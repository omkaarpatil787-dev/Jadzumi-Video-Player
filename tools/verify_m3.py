#!/usr/bin/env python3
"""Linux/macOS build gate. Produces evidence; never treats source checks as a build."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
from datetime import datetime, timezone
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-only", action="store_true", help="No compilation or dependency downloads")
    args = parser.parse_args()
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    evidence = ROOT / "docs/verification/m3" / stamp
    evidence.mkdir(parents=True)
    result = {"timestamp_utc": stamp, "status": "running", "checks": [], "tests_executed": 0}

    def finish(status, message, code):
        result.update(status=status, message=message)
        (evidence / "result.json").write_text(json.dumps(result, indent=2) + "\n")
        print(f"{status}: {message}\nEvidence: {evidence.relative_to(ROOT)}")
        return code

    def run(command, name):
        with (evidence / name).open("w") as log:
            return subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=False).returncode

    try:
        for area in ("design-references", "artifacts"):
            hashes = json.loads((ROOT / area / "SHA256.json").read_text())
            for name, expected in hashes.items():
                if sha(ROOT / area / name) != expected:
                    raise ValueError(f"Preserved file hash mismatch: {area}/{name}")
            result["checks"].append(f"{area}: {len(hashes)} hashes match")
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        if manifest.findall("uses-permission"):
            raise ValueError("This core checkpoint is expected to request no manifest permissions; review changes")
        app = manifest.find("application")
        if app is None or app.get(ANDROID + "allowBackup") != "false":
            raise ValueError("Backup policy changed")
        activity = app.find("activity")
        if activity is None or activity.get(ANDROID + "supportsPictureInPicture") != "true":
            raise ValueError("PiP activity configuration missing")
        result["checks"].append("Source manifest parses; no requested permissions; backup disabled; PiP declared")
        source_hashes = {
            str(p.relative_to(ROOT)): sha(p)
            for p in sorted(ROOT.rglob("*"))
            if p.is_file() and (p.suffix in (".kt", ".kts") or p.name == "AndroidManifest.xml")
            and "build" not in p.relative_to(ROOT).parts and ".gradle" not in p.relative_to(ROOT).parts
        }
        (evidence / "source-sha256.json").write_text(json.dumps(source_hashes, indent=2) + "\n")
        if args.source_only:
            return finish("source_checks_passed", "Source checks only; Kotlin, Gradle tests, lint and runtime NOT executed", 0)

        java_home = os.environ.get("JAVA_HOME")
        java = str(Path(java_home) / "bin/java") if java_home else shutil.which("java")
        if not java or run([java, "-version"], "java.txt") != 0:
            return finish("blocked", "JDK 17 is required", 2)
        if not re.search(r'version "17[.\"]', (evidence / "java.txt").read_text()):
            return finish("blocked", "Use JDK 17 for this checkpoint", 2)
        javac = str(Path(java_home) / "bin/javac") if java_home else shutil.which("javac")
        if not javac or not Path(javac).is_file() or run([javac, "-version"], "javac.txt") != 0:
            return finish("blocked", "A full JDK 17 with javac is required; a Java runtime alone cannot compile the app", 2)
        if not re.search(r'javac 17(?:\.|\s|$)', (evidence / "javac.txt").read_text()):
            return finish("blocked", "Use the Java 17 compiler for this checkpoint", 2)
        sdk_value = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
        if not sdk_value:
            return finish("blocked", "Set ANDROID_HOME to an installed Android SDK with platform 36 and build-tools 35.0.0", 2)
        sdk = Path(sdk_value)
        if not (sdk / "platforms/android-36/android.jar").is_file():
            return finish("blocked", "Android SDK platform 36 is missing", 2)
        build_tools = sdk / "build-tools/35.0.0"
        if not all((build_tools / tool).is_file() for tool in ("apksigner", "aapt")):
            return finish("blocked", "Android build-tools 35.0.0 are missing", 2)

        tasks = [":app:assembleDebug", ":app:testDebugUnitTest", ":app:lintDebug"]
        result["gradle_tasks"] = tasks
        # One invocation, no parallel build processes; wrapper verifies its distribution checksum.
        code = run(["bash", str(ROOT / "gradlew"), "--no-daemon", "--console=plain", "--no-parallel", *tasks], "gradle.log")
        if code != 0:
            return finish("build_failed", f"Gradle exit code {code}; inspect gradle.log", 1)
        tests = list((ROOT / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
        if not tests:
            raise ValueError("Gradle returned success but unit-test XML is missing")
        for report in tests:
            suite = ET.parse(report).getroot()
            result["tests_executed"] += int(suite.get("tests", "0"))
            if int(suite.get("failures", "0")) or int(suite.get("errors", "0")):
                raise ValueError(f"Unit-test failures: {report.name}")
            shutil.copy2(report, evidence / report.name)
        if not result["tests_executed"]:
            raise ValueError("No tests executed")
        lint = ROOT / "app/build/reports/lint-results-debug.xml"
        lint_root = ET.parse(lint).getroot()
        shutil.copy2(lint, evidence / lint.name)
        if any(i.get("severity") in ("Error", "Fatal") for i in lint_root.findall("issue")):
            raise ValueError("Lint has errors")
        result["lint_warnings"] = sum(i.get("severity") == "Warning" for i in lint_root.findall("issue"))
        apk = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
        for name, path in (("m3", apk), ("m2", ROOT / "artifacts/Jadzumi-Video-M2-debug.apk")):
            if run([str(build_tools / "apksigner"), "verify", "--print-certs", str(path)], name + "-signature.txt") != 0:
                raise ValueError(f"Signature verification failed: {name}")
        def signer(name):
            return re.findall(r"Signer #\d+ certificate SHA-256 digest: (\S+)", (evidence / name).read_text())
        if not signer("m3-signature.txt") or signer("m3-signature.txt") != signer("m2-signature.txt"):
            raise ValueError("M3 signing identity does not match M2")
        if run([str(build_tools / "aapt"), "dump", "permissions", str(apk)], "apk-permissions.txt") != 0:
            raise ValueError("Could not inspect merged APK permissions")
        permissions = (evidence / "apk-permissions.txt").read_text()
        forbidden = ("android.permission.INTERNET", "android.permission.MANAGE_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE")
        if any(permission in permissions for permission in forbidden):
            raise ValueError("Unexpected network or broad-storage permission in merged APK")
        target = ROOT / "artifacts/Jadzumi-Video-M3-debug.apk"
        shutil.copy2(apk, target)
        hashes = json.loads((ROOT / "artifacts/SHA256.json").read_text())
        hashes[target.name] = sha(target)
        (ROOT / "artifacts/SHA256.json").write_text(json.dumps(hashes, indent=2) + "\n")
        result["apk_sha256"] = hashes[target.name]
        return finish("build_checks_passed", "Debug APK built, tests/lint/signature checks passed; Pad 5 runtime testing still required", 0)
    except (OSError, ValueError, ET.ParseError) as exc:
        return finish("failed", str(exc), 1)


if __name__ == "__main__":
    sys.exit(main())
