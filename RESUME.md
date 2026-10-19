# Jadzumi Video / Jadzumi Player — Active Development Checkpoint

Updated: 2026-10-19 (Europe/Warsaw)
Target Device: Xiaomi Pad 5 (nabu, Android 16 Custom ROM, 16:10 display, ARM64-v8a)

## Milestone Status Summary

- **Milestone 1 — Tablet UI Foundation:** [USER-APPROVED]
- **Milestone 2 — Local Video Library:** [USER-APPROVED]
- **Milestone 3A — Cinema Playback:** [USER-APPROVED & DEVICE-VERIFIED]
  - **3A.1 Core Playback:** Media3/ExoPlayer content-URI playback, seek, 10s skips, per-video resume, mini-player/fullscreen, gestures, screen lock, speed/aspect controls, scrub previews, play/pause morph.
  - **3A.2 Cinema Formats & Instant Resume:** Audio track selection, Dolby 5.1/7.1 surround-to-stereo downmix for Pad 5 quad speakers, embedded subtitle track selection (ASS/SSA, SRT, VTT) with Turn Off option, HDR10/HEVC Main10 detection (moved to File Details), and instant keyframe-sync resume (`SeekParameters.CLOSEST_SYNC` + 100ms local buffer + 10s back-buffer).
  - **3A.3 System Integration:** Android 13–16 `POST_NOTIFICATIONS` support, `MediaStyle` lock-screen and notification shade player card with `-10s`/`+10s` custom commands, screen-off Background Audio toggle, `WAKE_MODE_LOCAL` CPU wake lock for Android 16 custom ROMs, headset/Bluetooth noisy-audio pause, and seamless Picture-in-Picture (PiP).
- **Milestone 3B — Library Refresh:** [USER-APPROVED & DEVICE-VERIFIED]
  - Native Material 3 `PullToRefreshBox` on Library screen, active top toolbar Sync button, debounced incremental auto-refresh on app resume, and Full Rescan option in Folders.
- **Milestone 3C — Internal Storage Management:** [USER-APPROVED & DEVICE-VERIFIED]
  - Duplicate Refresh button removed from Library card, folder video count and total GB storage footprint displayed, and user-initiated permanent file deletion via scoped SAF `DocumentsContract.deleteDocument` with explicit confirmation dialog and active-playback teardown.
- **Combined Milestone 4 + Milestone 6 — Privacy, Settings, Branding, Theme Bloom & Responsive UI:** [USER-APPROVED & DEVICE-VERIFIED]
  - **4A Unified Privacy & About:** Two-pane layout matching `unified_privacy_about.html`, Folder Access directory cards, functional *Remember Playback Progress* and *Save Audio & Subtitle Preferences* toggles, functional *Clear Watch History*, *Clear Cache*, and *Export App Settings* actions, Omkar Patil Creator & Product Direction card with *AI-assisted development* badge, Philosophy card, and *"No popcorn included. Sorry."* footer.
  - **4B & 4C Settings, Logo & First-Launch:** Layered folded-play Jadzumi icon (`ic_launcher.xml` and `JadzumiLogo` canvas), one-time 16:10 display detection welcome card, Light/Dark/AMOLED/System themes, customizable double-tap seek intervals (5s/10s/15s/30s), and swipe gesture toggles.
  - **M6 Expressive Motion & Responsive/Adaptive UI Correction:** 580ms Theme Bloom circular reveal transition, content-width-aware (`BoxWithConstraints` + `FlowRow`) layouts across all screens preventing badge/chip crushing in portrait and split-screen, 2×2 vs 1×4 adaptive theme grid, and stacked single-column Folders/Privacy views on narrower widths.

## Current Active Source Files Modified Since M2/M3A.1

- `app/src/main/AndroidManifest.xml` (Added `POST_NOTIFICATIONS`, uses `@android:style/Theme.Material.NoActionBar`)
- `app/src/main/res/drawable/ic_launcher.xml` (Official layered folded-play Jadzumi icon)
- `app/src/main/java/com/jadzumi/video/MainActivity.kt` (Android 13–16 runtime notification permission request & background audio lifecycle)
- `app/src/main/java/com/jadzumi/video/PlaybackModel.kt` (Audio/subtitle track models, channel layout & codec formatters, HDR transfer detection)
- `app/src/main/java/com/jadzumi/video/PlaybackController.kt` (Fast 100ms local load control, `CLOSEST_SYNC` resume, `WAKE_MODE_LOCAL`, `MediaStyle` notification channel, track switching, watch history clearing, configurable seek/gesture preferences)
- `app/src/main/java/com/jadzumi/video/CinemaPlayer.kt` (Clean player canvas, Audio & Subtitles side drawer, Background Audio menu toggle, configurable seek increments)
- `app/src/main/java/com/jadzumi/video/LocalLibrary.kt` (Incremental vs full rescan, auto-refresh on resume, scoped permanent file deletion)
- `app/src/main/java/com/jadzumi/video/LocalLibraryUi.kt` (Pull-to-refresh, adaptive Folders & Storage layout, storage footprint totals, permanent delete confirmation dialog, thumbnail cache clearing hook)
- `app/src/main/java/com/jadzumi/video/JadzumiApp.kt` (Folded-play logo, 580ms Theme Bloom reveal, welcome card, width-aware adaptive Unified Privacy & About + Settings screens)
- `app/src/test/java/com/jadzumi/video/PlaybackModelTest.kt` (20 unit tests covering seek clamping, bounds, channel layouts, codecs, subtitles, and HDR detection)

## Next Action on Resume

1. Begin **Milestone 5A — Smart Episode Continuity**:
   - Group recognized TV/Anime episodes (`S01E01`, `2x04`) into Series & Seasons (`Season 1`, `Season 2`, `Specials`).
   - Add per-episode watched/progress badges and "Up Next" highlighting.
   - Implement skip protection and the **10-second Next-Episode Autoplay countdown banner** (with *Play Now* and *Cancel* buttons).
2. Proceed to **Milestone 5B** (Optional Online Metadata, OFF by default), **Milestone 7** (Final HTML-to-Native Design Audit & QA), and **Milestone 8** (Stable v1.0 Production Release).