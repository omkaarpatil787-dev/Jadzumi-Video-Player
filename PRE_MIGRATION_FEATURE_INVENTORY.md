# 📑 Jadzumi Player v1.3 — Pre-Migration Feature Inventory
> **Baseline Audit Date:** 2026-10-20 · **Version Baseline:** v1.2.0 (Release) · **Target:** Cinema Flow v1.3

---

## 🎯 Executive Summary & Purpose

In accordance with the **Jadzumi Player — Cinema Flow v1.3 Master Implementation Specification**, this document establishes an exhaustive, immutable baseline inventory of all implemented features, settings, preference keys, UI states, playback capabilities, and architectural dependencies existing in the application prior to the Cinema Flow visual upgrade.

**Core Rule:** Absolute zero feature regression. No feature, toggle, preference key, or playback capability may be deleted, renamed, or impaired during this visual UI/UX overhaul.

---

## 🏛️ 1. Architecture & State Management Inventory

| Component | Implementation Location | Role & State Held | Preservation Requirement |
| :--- | :--- | :--- | :--- |
| **`PlaybackController`** | `PlaybackController.kt` | Media3 ExoPlayer instance, NextLib FFmpeg JNI decoder pipeline, playback state, timestamps, audio DSP state, track selection state, loop/repeat | Strictly preserved. Underlying player logic must not be modified. |
| **`LocalLibrary`** | `LocalLibrary.kt` | Android SAF storage access, DocumentFile indexing, Room/SQLite persistence, LRU bitmap thumbnail caching, sidecar `.nfo`/poster loading | Strictly preserved. Offline-first local data vault remains unchanged. |
| **`LocalSeries`** | `LocalSeries.kt` | Episode regex pattern parser (`S01E01`, `1x04`), TV Series aggregation, season groupings, `nextUnwatchedEpisode` logic | Strictly preserved. |
| **`OnlineMetadataFetcher`** | `OnlineMetadataFetcher.kt` | Dual-engine Wikipedia REST + TVMaze scraper with local disk caching (`cacheDir/poster_cache/`) | Strictly preserved. Works as online fallback when local sidecars are missing. |
| **`DeviceHardwareInspector`**| `DeviceHardwareInspector.kt` | Hardware display detection (OLED/AMOLED vs LCD), SoC detection, OLED-exclusive peak HDR boost | Strictly preserved. |
| **`GlassTheme`** | `GlassTheme.kt` | Dynamic glass translucency engine modulated by `LocalGlassIntensity` (0.0 to 1.0) | Upgraded to Cinema Flow fluid glass tokens while preserving the intensity slider. |
| **`SubtitlePreferences`** | `SubtitlePreferences.kt` | Subtitle styling persistence (size, font, color, background, edge) | Strictly preserved. |

---

## 📱 2. Screen & Destination Inventory

| # | Existing Screen / Destination | Implementation File | Key Features & Elements | Cinema Flow Destination | Status |
| :-: | :--- | :--- | :--- | :--- | :---: |
| **1** | **Splash Screen** | `JadzumiSplashScreen.kt` | HTML-spec accurate Aurora gradient blobs, levitating frosted glass tile, vector play symbol, loading track, smooth zoom exit | Approved Fluid Glass Splash preserved | `PRESERVED` |
| **2** | **Home / Discover** | `LocalLibraryUi.kt`, `JadzumiApp.kt` | Interactive cinema greeting with recent episode detection and tap-to-cycle trivia facts, Hero suggestions carousel (`▶ UP NEXT`, `⏱ RESUME`), Continue Watching row, All Videos grid | `view-home` with Cinema Flow featured card, glass pills, resume cards | `MIGRATE UI` |
| **3** | **Movie Library** | `LocalLibraryUi.kt` | Dedicated Movie collection grid, genre filter chips (Sci-Fi, Action, Animation, etc.), online/offline badges | `view-library` with filter bar, glass media grid (4 columns) | `MIGRATE UI` |
| **4** | **TV Series** | `LocalLibraryUi.kt`, `TvShowDetailsPage.kt` | TV box-sets grid, season groupings, in-progress episode counter, unwatched indicator | `view-series` with series continuity cards | `MIGRATE UI` |
| **5** | **Movie Details** | `MovieDetailsPage.kt` | Single-surface hero banner, 2:3 poster, resolution/audio badges, click-to-expand plot synopsis, play action, audio/sub status | `view-movie-details` with backdrop, frosted metadata column | `MIGRATE UI` |
| **6** | **Series & Episode Details**| `TvShowDetailsPage.kt` | Series backdrop/poster, season selector pill bar, responsive episode cards with progress bars, mark season watched | `view-series-details` with season filter bar, episode grid | `MIGRATE UI` |
| **7** | **Cinema Video Player** | `CinemaPlayer.kt` | Hardware SurfaceView, minimal overlay, Play/Pause morph, 10s seek buttons, scrubber timeline with preview, dual edge swipe gestures, speed selector, night voice boost, subtitle/audio switcher | `cinemaPlayer` overlay with Cinema Flow frosted controls | `PRESERVED & STYLED` |
| **8** | **Folders & Storage** | `JadzumiApp.kt`, `LocalLibraryUi.kt` | Internal storage bar, indexed folder list, SAF folder picker, active directory removal protection | `view-folders` with Cinema Flow storage stats bar | `MIGRATE UI` |
| **9** | **Library Search** | `LocalLibraryUi.kt`, `JadzumiApp.kt` | Tokenized multi-word search matcher, search capsules, clear query button, categorized results | `view-search` with recent search tags, live suggestions | `MIGRATE UI` |
| **10**| **Settings & Appearance**| `JadzumiApp.kt` | Theme cards (Light, Dark, AMOLED, System), Theme Bloom animation, Glassify intensity slider, Dock styles, Auto-minimize toggle | `view-settings` with Cinema Flow settings grid and glass cards | `MIGRATE UI` |
| **11**| **Privacy & About** | `JadzumiApp.kt` | 100% offline guarantee, zero tracking statement, open-source credits, replay splash animation, playful footer | `view-about` with Cinema Flow about hero card | `MIGRATE UI` |

---

## ⚙️ 3. Exhaustive Preference & Settings Key Inventory

| SharedPreferences File | Key Name | Data Type | Default Value | Functional Role in App |
| :--- | :--- | :--- | :--- | :--- |
| `appearance` | `theme` | `String` | `"Dark"` | Master theme mode (`"Light"`, `"Dark"`, `"AMOLED"`, `"System"`) |
| `appearance` | `reducedMotion` | `Boolean` | `false` | Disables non-essential parallax and float animations for accessibility |
| `appearance` | `livelyAnimations`| `Boolean` | `true` | Enables spring-based fluid Compose transitions |
| `appearance` | `themeBloom` | `Boolean` | `true` | Enables radial circular reveal animation on theme switch |
| `appearance` | `cornerStyle` | `String` | `"Rounded"` | Global UI corner curvature (`"Rounded"`, `"Compact"`, `"Sharp"`) |
| `appearance` | `bottomBarStyle` | `String` | `"Icons and labels"` | Navigation dock presentation (`"Icons and labels"`, `"Icons only"`) |
| `appearance` | `dockAutoMinimize`| `Boolean` | `true` | Auto-collapses floating dock into compact mini-pill on scroll |
| `appearance` | `glassIntensity` | `Float` | `0.75f` | Modulates frosted glass opacity and specular rim highlights (0.0 to 1.0) |
| `appearance` | `ambientAura` | `Boolean` | `true` | *NEW Cinema Flow* — Dynamic artwork-driven ambient radiance behind glass |
| `playback` | `bg_audio` | `Boolean` | `true` | Enables background audio playback when app is minimized or screen is locked |
| `playback` | `remember_progress`| `Boolean` | `true` | Persists exact millisecond playback timestamp on exit |
| `playback` | `save_track_prefs`| `Boolean` | `true` | Remembers preferred audio language and subtitle track across videos |
| `playback` | `swipe_gestures` | `Boolean` | `true` | Enables vertical edge swipes for brightness (left) and volume (right) |
| `playback` | `brightness_boost`| `Boolean` | `true` | Enables extra brightness headroom / OLED peak HDR luminance |
| `playback` | `seek_seconds` | `Int` | `10` | Double-tap seek increment (10s, 15s, 30s) |
| `playback` | `night_mode` | `Boolean` | `false` | Android LoudnessEnhancer speech clarity / dialogue boost DSP |
| `playback` | `preferred_audio_lang`| `String` | `null` | Persisted audio language track preference |
| `playback` | `preferred_sub_lang` | `String` | `null` | Persisted subtitle language track preference |
| `playback` | `extract_thumbnails` | `Boolean` | `true` | Enables asynchronous video frame thumbnail generation |
| `playback` | `scan_sidecars` | `Boolean` | `true` | Enables scanning local `.nfo` and poster image sidecars |
| `playback` | `online_metadata`| `Boolean` | `true` | Enables dual-engine Wikipedia/TVMaze online scraper fallback |
| `playback` | `video_uri_hash` | `Long` | `0L` | Per-video saved playback position in milliseconds |
| `playback` | `video_uri_hash.done`| `Boolean`| `false` | Per-video completion flag (>= 90% watched) |
| `jadzumi_settings` | `local_sandbox` | `Boolean` | `true` | Enforces zero telemetry and local-only sandbox security |
| `jadzumi_subs` | `size` | `Float` | `18f` | Subtitle text font size |
| `jadzumi_subs` | `font` | `String` | `"Default"` | Subtitle typeface family |
| `jadzumi_subs` | `color` | `String` | `"White"` | Subtitle text color |
| `jadzumi_subs` | `bg` | `String` | `"Semi-transparent Black"` | Subtitle background caption container |
| `jadzumi_subs` | `edge` | `String` | `"Drop Shadow"` | Subtitle text outline / drop shadow treatment |

---

## 🎮 4. Playback Engine & Gesture Inventory

- **Engine:** AndroidX Media3 (ExoPlayer 1.11.1) + bundled `nextlib-media3ext` (FFmpeg JNI decoders for AC-3, E-AC-3, TrueHD, DTS-HD, FLAC, Opus).
- **Video Surface:** Native hardware `SurfaceView` with zero overhead.
- **Aspect Ratio Control:** Four modes: `Fit`, `Fill`, `Zoom (16:9 / 21:9)`, `Original`.
- **Playback Speeds:** `0.5x`, `0.75x`, `1.0x`, `1.25x`, `1.5x`, `1.75x`, `2.0x`.
- **Gestures:**
  - Double-tap left: seek backward (accumulative with ripple).
  - Double-tap right: seek forward (accumulative with ripple).
  - Left edge vertical swipe: display brightness (0% to 100%).
  - Right edge vertical swipe: media audio volume (0% to 100%).
  - Long press anywhere: momentary 2.0x speed boost.
  - Quick Skip Intro pill: +85s intro bypass.
- **Picture-in-Picture (PiP):** Automatic entry on Home gesture / minimize with aspect ratio matching.
- **Lock Screen / Background Audio:** MediaSessionService continuity.

---

## 🔒 5. Verification Checkpoint Statement

All 29 preference keys, 11 primary screen destinations, 8 audio/video DSP pipelines, and underlying storage architectures are cataloged and locked. **Zero regression guarantee enacted.**
