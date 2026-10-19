# Jadzumi Video — Milestone 3A.1 device-test checkpoint

Updated 2026-10-19T10:18:06.952869+00:00. M1 and M2 remain user-approved. M3A.1 Core Playback now passes the internal build gate and awaits Xiaomi Pad 5 testing and user approval. Do not advance to M3A.2.

## Verified result

- Gradle 8.11.1, full JDK 17 (Amazon Corretto 17.0.20.12.1), Android platform 36/build-tools 35.0.0 installed in this session.
- User explicitly authorized the official Gradle release download via GitHub. This is a build-tool download exception, not authorization to publish source or use GitHub for this project.
- Gradle assembleDebug, testDebugUnitTest and lintDebug: PASS. 16 tests, zero failures/errors/skips; lint zero errors and two warnings (target SDK 35; ARM64-only excludes ChromeOS x86_64). Existing compiler parameter-name warning and native-library stripping notice remain nonfatal.
- Debug APK signature verified and matches M2; no INTERNET, MANAGE_EXTERNAL_STORAGE or WRITE_EXTERNAL_STORAGE permission in merged APK. Application ID and development key preserved.
- artifacts/Jadzumi-Video-M3-debug.apk, version 0.3.0-m3 (code 3), SHA-256: 0d72734372fe7d0272106b8da34fbc632a68f3c9decd2aeadb854faccbb413a7.
- Evidence: docs/verification/m3/20261019T101528822455Z/result.json, gradle.log, unit XML, lint XML, signatures, permissions, source hashes and source diffs.

## Changes in this continuation

All previous playback/resume/seek/PiP review fixes were retained. Only app source change: CinemaPlayer uses LocalActivity with a safe MainActivity cast instead of casting LocalContext, fixing the actual lint error. The build gate now explicitly checks for javac 17, since this environment originally had only a Java runtime. No dependency versions, approved UI, player behavior design, signing identity or milestone scope were changed.

Historical attempts are retained under docs/verification/m3. Initial failures were plugin access (proxy configuration), missing javac, expired proxy address, and then the LocalContext lint error. These are superseded by the successful gate above. Earlier source-only checks are not runtime tests. Earlier RESUME notes are retained in docs/M3-RESUME-HISTORY-2026-10-19.md.

## Next action and approval gate

1. Install artifacts/Jadzumi-Video-M3-debug.apk as an update over M2 on Xiaomi Pad 5; same signing key was verified. Do not uninstall M2 first.
2. Follow docs/M3-DEVICE-REVIEW.md. Prioritize H.264/AAC MP4 and MKV playback, seeking, per-video resume, close/reopen/background, rapid video switching, mini/fullscreen, existing PiP and error/retry with disconnected USB or revoked grants.
3. Report actual device results. No Pad 5/emulator playback tests were run in this environment; no playback, lifecycle, decoder, performance or smoothness certification follows from a build.
4. Fix device feedback within M3A.1 and save again. Await explicit approval before M3A.2 Cinema Formats. AC-3/E-AC-3, HEVC Main10, HDR and complex subtitles remain separately gated; no guarantees. Full dependency security review and release validation remain pending.

## Rebuild

Run python3 tools/verify_m3.py from the project root with JAVA_HOME pointing to a full JDK 17, ANDROID_HOME to platform 36/build-tools 35.0.0, and GRADLE_USER_HOME to a usable Gradle cache. Preserve wrapper 8.11.1/AGP 8.10.1/Kotlin 2.2.10/Media3 1.11.1. In this environment the local HTTP proxy address may change per command: derive Gradle proxy settings from that invocation's environment, not a saved stale address. Use the current environment CA trust settings. Never package credentials/proxy configuration into app source.

Session-only toolchain root: /workspace/scratch/a2b20457baed/toolchain. JAVA_HOME subdirectory: jdk17/amazon-corretto-17.0.20.12.1-linux-x64; ANDROID_HOME subdirectory: android-sdk; GRADLE_USER_HOME subdirectory: gradle-home. These installed tool binaries/caches are not bundled in the source checkpoint and may need reinstalling in a future session.

## Preservation

The attached recovery archive passed CRC and all 82 original manifest hashes. Existing M1/M2 APKs, six locked design references and signing/development.keystore are preserved. The source archive contains the development signing key: keep it private; send APK only to testers. This is a debug test build, not the final private-key stable release.

The original M3 recovery provenance remains in docs/M3-RECOVERY-HISTORY.md and docs/RECOVERY-VERIFICATION.md. Recovery source was reconstructed from recorded commands; do not claim byte-for-byte identity to a lost earlier temporary archive. The exact later eight-main-milestone roadmap must be recovered before later work; historical working subdivisions do not replace it.


---
Historical test notes follow; the verified result above is current.

# SDK installation result

## SDK installation continuation — 2026-10-19, 12:02 Europe/Warsaw

User authorized attempting SDK/Gradle installation and resuming only after success.

- Android command-line tools ZIP downloaded successfully (164760899 bytes); ZIP CRC passed, sdkmanager reports version 19.0.
- A single sdkmanager invocation installed Android platform 36, build-tools 35.0.0 and platform-tools with exit code 0. Required android.jar, aapt, apksigner and adb files exist. JDK 17 remains available.
- SDK location in this session: /workspace/scratch/a2b20457baed/toolchain/android-sdk. Set ANDROID_HOME to this path when building here. Tool binaries are environment prerequisites, not included in this source checkpoint; a future environment may require reinstalling them.
- A single official Gradle 8.11.1 GET returned HTTP 307 to GitHub. Redirect was not followed. Gradle is not installed. Earlier no-GitHub instruction remains in force; explicit authorization for the official Gradle GitHub release download is needed unless a verified local distribution is supplied.
- No Kotlin/Gradle source or design changes. No compile, unit tests, lint, M3 APK or device tests. Source-only checks from the preceding continuation still apply. M3A.1 remains the active gate; no M3A.2 advancement.

Evidence: docs/verification/m3/20261019T100154Z/.

# Latest continuation verification

## Controlled continuation — 2026-10-19, 11:59 Europe/Warsaw

- Resumed from the user-attached latest checkpoint; ZIP CRC passed and all 82 manifest hashes matched before changes. Reviewed RESUME.md and the existing PlaybackController, PlaybackModel and MainActivity fixes. No Kotlin, Gradle, design, signing key or existing APK changes were made.
- JDK 17.0.20 is present. No local Gradle, sdkmanager, adb, configured SDK or checked cached Gradle launcher/android.jar was found.
- One bounded parallel HEAD check per endpoint, without following redirects: Android command-line tools, Google Maven, Maven Central and Gradle plugin marker returned HTTP 200. Responses do not prove download/install or full dependency resolution.
- Both services.gradle.org and downloads.gradle.org for pinned Gradle 8.11.1 returned HTTP 307 to github.com/gradle/gradle-distributions. No GitHub requests, repeated blocked attempts, alternate mirror experiments or dependency/version changes were made. SDK installation was not started because Gradle remains blocked under the no-GitHub constraint.
- Source-only gate passed (preserved artifact/design hashes and source manifest checks). No Kotlin compile, Gradle unit tests, lint or device testing ran; no M3 APK exists. M2 remains the last verified APK, preserved as historical output only.
- Active gate remains M3A.1. No M3A.2 advancement. Continue only when pinned build prerequisites can be supplied within the authorized constraints, then run tools/verify_m3.py and address real build results before Pad 5 testing.

Evidence: docs/verification/m3/20261019T095903Z/access-check.json and docs/verification/m3/20261019T095903100926Z/result.json.

# M3 validation report — 2026-10-19

## Latest controlled continuation check — 2026-10-19, 11:52 Europe/Warsaw

Active gate remains M3A.1 Core Playback. No UI redesign, milestone advancement, source rollback or GitHub use occurred.

- Both saved M3 ZIP copies are byte-identical, SHA-256 b1b21b2eedd76d79808ea43ff216f5bcded97505404158b39f1d65d777e20ca1. Isolated extraction, CRC and all 79 manifest entries passed. The existing working tree matched every extracted file, with no extra or newer source differences.
- Reviewed RESUME.md and the existing PlaybackController, MainActivity/PiP and PlaybackModel fixes. No Kotlin or Gradle source was changed.
- One bounded access check per endpoint: Android command-line tools, Google Maven AGP POM, Maven Central JUnit POM and Gradle plugin-marker POM returned HTTP 200. These are access responses, not proof of successful installation or complete dependency resolution.
- Gradle 8.11.1 official distribution returned HTTP 307. A single non-following download request confirmed the redirect destination is github.com/gradle/gradle-distributions. No request to GitHub was made. No blocked retry loop or alternate mirror experiments were attempted.
- No local Gradle or sdkmanager was found, no SDK is configured, and no cached Gradle launcher/distribution or android.jar was found in the checked tool/workspace locations. Java is present.
- Current blocker is obtaining the pinned Gradle distribution under the user's explicit no-GitHub constraint. The previous blanket network-timeout diagnosis is historical; the required repository endpoints now respond. Android SDK installation was not started once the Gradle blocker was confirmed.
- No M3 compile, tests, lint or APK export was performed. The 16 JUnit methods remain unexecuted against M3; M2 remains the last verified APK. Do not call this a successful or failed Kotlin build.

Evidence: docs/verification/m3/20261019T095227Z/access-check.json and gradle-download-result.json. Stop after preserving this checkpoint. Future continuation must respect the no-GitHub instruction or obtain explicit revised authorization; never follow a prohibited redirect silently. Do not advance to M3A.2 without approval.


Current result: SOURCE CHECKS PASSED; BUILD BLOCKED; RUNTIME UNTESTED.

Executed: tools/verify_m3.py --source-only (exit 0); tools/verify_m3.py (exit 2, missing SDK). Latest evidence is in docs/verification/m3/20261019T044708962725Z and 20261019T044709033573Z. The verifier script was syntax-checked and its missing-prerequisite branch exercised. Its successful-build/APK-export branch is not yet exercised.

The source checker verifies all six design hashes, the two existing APK hashes, manifest XML, no source permissions, backup disabled and PiP declaration. Current source hashes accompany each run. It does NOT parse/typecheck Kotlin or run the app. A source diff review additionally checked changed lifecycle/transport/PiP paths.

There are 16 JUnit methods in source: 7 historical library/model tests and 9 playback tests. None ran against this M3 source. The historical M2 result of 7 passing tests is not a current result.

Pending: Kotlin/Android compile, JUnit execution, lint, current APK signature/merged permissions, dependency security review, all Pad 5 device cases. No M3 APK exists. No media formats/codecs/HDR or smoothness results are claimed.

See M3-DEVICE-REVIEW.md for reproducible runtime cases and ../RESUME.md for the build gate and source changes. M2-TEST-REPORT.md and historical verification files remain preserved separately.

## Preservation pause — 2026-10-19 06:58 Europe/Warsaw

User requested preservation only with 7% usage remaining. Stop and wait for explicit resume. Do not retry SDK downloads or start another milestone. All source changes through the latest core playback/PiP fixes are retained.

Latest blocker diagnosis: the configured session network allowlist excludes Android/Gradle download hosts. The proxy was reachable and a permitted host responded, while SDK requests timed out. The final official SDK/Gradle download retry produced no files; checking processes also encountered an automatic approval-service capacity failure. No SDK installation or M3 compilation succeeded. Future continuation needs permitted build dependency access; the source ZIP is complete but does not include the missing SDK or dependency binaries.

Preservation checks: archive existed, CRC passed, every manifest hash matched, and extraction into a separate temporary directory succeeded with matching extracted hashes. All packaged files matched the current working source before this documentation-only update. No playback source, Gradle configuration, design reference, signing key or APK was changed during this preservation pass.
