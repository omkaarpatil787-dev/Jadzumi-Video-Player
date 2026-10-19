# Milestone 1 validation — 2026-10-18

Result: candidate ready for real-device review. User approval is pending.

| Check | Result |
|---|---|
| Kotlin/Android debug build | PASS — Gradle BUILD SUCCESSFUL |
| JVM unit tests | PASS — 4 tests, 0 failures, 0 errors |
| Android lint | PASS — no issues found |
| APK signature | PASS — apksigner verification, v2 scheme |
| APK permission inspection | No internet/media/storage permissions; only AndroidX app-private signature permission |
| Approved reference preservation | PASS — six originals match SHA-256 manifest and uploaded files |
| Bundled JPEG decoding | PASS — three images decoded successfully |
| Gradle distribution integrity | PASS — official SHA-256 matches downloaded Gradle 8.11.1 |
| Device/emulator launch and visual rendering | NOT RUN — no connected device/emulator used |
| Smoothness, rotation, accessibility and appearance persistence on Pad 5 | PENDING user review |
| Playback/codecs/HDR/surround/subtitles | NOT IMPLEMENTED in M1; no compatibility claims |

Tests cover consistent progress/remaining duration, clamped invalid progress, combined title/category filters, and long/negative time formatting. They do not cover Compose interaction or rendering.

Fixed before this checkpoint: settings-block syntax error; deprecated non-mirrored playlist icon; missing explicit backup/transfer exclusions; duplicate bitmap resources during relocation; bitmap density placement; preference-edit style warnings; missing switch descriptions. Build environment also required Android SDK, a full JDK and environment-specific dependency proxy settings. Kotlin compilation runs in-process for this workspace's stability.

Security checks are limited to manifest/permission inspection, backup policy, signature and lint; this is not a comprehensive security audit. Production signing, release optimization, dependency audit and device regression testing remain later gates.

Evidence files are in docs/verification. See DEVICE-REVIEW.md for the required user tests. Do not advance to M2 until feedback is resolved and Omkar explicitly approves.
