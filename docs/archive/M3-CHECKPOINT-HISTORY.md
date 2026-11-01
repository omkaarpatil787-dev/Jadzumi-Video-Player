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
