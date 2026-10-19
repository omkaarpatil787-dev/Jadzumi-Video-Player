Current checkpoint: M3A.1 internal build gate passed; Pad 5 device tests and user approval pending. See RESUME.md and docs/M3A1-BUILD-REPORT-2026-10-19.md.

# Jadzumi Video — unfinished Milestone 3

Native Kotlin/Jetpack Compose offline video player for Omkar Patil, targeting Xiaomi Pad 5 first. M1 visuals and M2 local library were user-approved. M3 core playback source is recovered and under active development; it has not passed compilation or device tests. Read RESUME.md first.

## Preserved and current work

M2 folder import, read-only SAF access, SQLite library, search/details, scaled thumbnails, all six locked design references, development signing key and M1/M2 APKs are preserved. M3 adds local Media3 playback source, resume, seek, fullscreen/mini player, gestures/lock, speed/fit controls, PiP actions, scrub previews and play/pause morph. Source implementation is not proof that these features work on device.

## Build gate

Use JDK 17, Gradle wrapper 8.11.1, Android SDK platform 36/build-tools 35.0.0, AGP 8.10.1 and Kotlin/Compose plugin 2.2.10. Media3 is pinned to 1.11.1. Set ANDROID_HOME to your installed SDK. Dependency access is needed on first build; the app requests no network permission.

From this project directory on Linux/macOS:

```sh
python3 tools/verify_m3.py --source-only
python3 tools/verify_m3.py
```

The first command checks preserved hashes and source manifest only. The second checks prerequisites, invokes Gradle once for assembleDebug/testDebugUnitTest/lintDebug, inspects test/lint output, verifies APK signing matches M2, checks network/broad-storage permissions and copies an M3 APK only after those checks pass. Evidence goes to a unique docs/verification/m3 run directory. Status `blocked` or `source_checks_passed` does not mean compilation succeeded. The successful-build branch is not yet exercised in this environment.

Manual build on Windows: configure JDK/SDK in Android Studio, then run `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`; review and preserve equivalent evidence. Build-only proxy configuration stays outside source.

No M3 APK is currently included. Existing artifacts are historical M1/M2 builds. Use docs/M3-DEVICE-REVIEW.md after a verified M3 APK exists. Do not advance milestones until user approval.

## Scope and privacy

Codec/HDR capability, cinema audio/downmix, subtitle handling and further system integration still require their implementation/validation gates. Full motion polish, episode continuity, production signing and phone adaptation remain later work. This checkpoint includes the private development key; distribute only APKs to testers. Production signing remains separate.

## Saving and resuming

Source, validation evidence, issues and next steps belong together in a checkpoint. CHECKPOINT.md and RESUME.md distinguish recovered source, later edits and actual test results. Older saved versions remain recovery points. Do not depend on a temporary workspace or infer successful saving from a local ZIP alone.
