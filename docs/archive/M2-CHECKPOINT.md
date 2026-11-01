# Jadzumi Video checkpoint

Date: 2026-10-18. Active milestone: M2 — real offline library preview.
Status: implemented, build successful, 7 unit tests passed, lint 0 errors / 1 intentional ARM64-versus-ChromeOS warning, final debug APK signed and verified. Awaiting Pad 5 tests and M2 approval. Do not begin M3 without approval.

## User context

M1 was installed on Xiaomi Pad 5 and visually approved. This does not establish exhaustive M1 runtime or playback testing. User requested resume; M2 work is now implemented. Six visual reference files remain unchanged. Full Expressive morphs, Theme Bloom and playback controls remain future implementation work.

## Resume entry point

1. Read README.md, docs/TEST-REPORT.md and docs/M2-DEVICE-REVIEW.md.
2. Collect M2 device feedback and fix defects before advancing. Preserve app ID and development signing identity.
3. Test actual folder import, restart persistence, nested/overlapping folders, thumbnails, cancel/refresh, revoked permissions, USB disconnect/reconnect and source-file safety on Pad 5.
4. After explicit M2 approval, start M3 playback with the approved minimal player design. AC-3/E-AC-3, surround downmix and subtitle work remain within stable v1.0 scope (M3/M4), not deferred outside v1.0.
5. Save source, evidence, APK and next steps to the same latest-checkpoint item after changes.

## Architecture

- JadzumiApp: navigation, themes and separate sample/real library entry points.
- LocalLibrary: Application-owned IO coroutine scope, StateFlow, read-only DocumentsContract traversal, app-private SQLite database, transactional per-folder refresh, metadata extraction and conservative episode hints. No new dependencies were required.
- LocalLibraryUi: system folder picker, local library/search, two-pane folders, file details and background thumbnails. Memory cache 12 MiB; two concurrent thumbnail jobs. No media write/delete operations.
- Process death can interrupt scans; refresh after relaunch. Cancellation is cooperative between provider/metadata calls. 50,000 traversed-entry cap per selected folder. API 26 uses thumbnail placeholders; API 27+ extracts scaled frames.
- Application registered as JadzumiApplication in manifest. No network permission. Backup/transfer exclusions retained.

## Build and release state

Version 0.2.0-m2 / versionCode 2. ARM64-v8a filter. Development signing key unchanged from M1; same signer digest verified. APK should update M1. Keep source checkpoint and development key private; testers only need the APK. Stable release needs a private production signing identity and release hardening later.

Use JDK 17, SDK 35/build-tools 35.0.0, wrapper Gradle 8.11.1, AGP 8.9.2, Kotlin 2.0.21. Toolchain/proxy/SDK paths are workspace-specific and not part of the source handoff. Do not depend on scratch surviving. README has portable build instructions.
