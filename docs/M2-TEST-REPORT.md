# Milestone 2 validation — 2026-10-18

Result: debug APK ready for Pad 5 testing. M2 is not device-approved; do not advance to M3 yet.

| Check | Result |
|---|---|
| Android/Kotlin/Java build | PASS — final Gradle BUILD SUCCESSFUL |
| JVM unit tests | PASS — 7 tests, 0 failures/errors/skips |
| Android lint | PASS with 0 errors and 1 warning: ChromeOS x86_64 ABI support omitted intentionally for the approved ARM64 tablet target |
| Final APK signature | PASS; same SHA-256 signer certificate as M1 |
| Final APK permissions | No internet or broad media/storage permission. Only AndroidX app-private receiver signature permission. SAF grants are requested through the picker. |
| Packaged native ABI | ARM64-v8a only |
| Reference integrity | PASS — all 6 original HTML hashes unchanged |
| Device/emulator/USB testing | NOT RUN — required Pad 5 checklist provided |
| Playback/codec/subtitle testing | NOT IMPLEMENTED; no compatibility claims |

Unit tests cover the four existing preview-model cases plus episode filename recognition, rejection of ambiguous numeric patterns, and video file classification. They do not exercise the Android provider, database, thumbnail decoding, Compose rendering, or lifecycle on a device.

Source review addressed background I/O, transactional replacement, failed/cancelled scan retention, overlapping tree membership and document identity, read-only permissions, absence of media deletion, application-scoped scan ownership, provider loading/error states and bounded thumbnail memory/concurrency. This is not a comprehensive security audit.

Fixed before final build: Android 8.0 thumbnail API mismatch (uses a placeholder below API 27); replaced a static context singleton with an Application-owned instance; adopted KTX URI and transaction helpers. Initial build noted unstripped androidx graphics-path native symbols because an NDK is not installed; this is a development APK, not a hardened release.

Evidence: docs/verification contains final build log, unit XML, lint, signature and permissions. Earlier M1 evidence is under docs/verification/m1. See M2-DEVICE-REVIEW.md for runtime gates and known limits.
