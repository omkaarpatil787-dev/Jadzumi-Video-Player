# Official Jadzumi Video Player Roadmap

> **Target Platform:** Xiaomi Pad 5 (*nabu*, 11-inch 16:10 2560×1600 120Hz, Android 16 custom ROM, ARM64-v8a)  
> **Core Philosophy:** 100% Offline-First by default, Native Jetpack Compose (no WebViews), Material 3 Expressive UI, Hardware-Accelerated Playback.

---

## Completed & Verified Milestones

### Milestone 1 — Native Foundation & UI Preview (Approved)
- Native Android architecture with Kotlin & Jetpack Compose.
- Tablet 16:10 landscape navigation rail, library grids, and video cards.
- Pale-blue/white Material 3 Expressive theme with dynamic palette support.

### Milestone 2 — Real Offline Library & Storage Framework (Approved)
- Android SAF (Storage Access Framework) persistent directory permissions.
- Local video indexing, recursive directory scanning, and video metadata extraction.
- Offline thumbnail generation, folder grouping, and empty state handling.

### Milestone 3 — Core Cinema Playback Engine (Approved)
- Media3 / ExoPlayer hardware-accelerated playback (`SurfaceView` direct pipeline).
- Full gesture controls (horizontal seek swipe, vertical brightness and volume sliders).
- Video aspect ratio toggle (Fit, Fill, 16:9, 16:10, 4:3, Zoom), playback speed selector (0.5x–2.0x).
- Lock controls, screen orientation lock, and background resume persistence.

### Milestone 4 — Cinema Audio, Native PiP, Expressive UI & Aurora Splash (Pushed in Build 5)
- **FFmpeg Audio Decoding**: Native C++ FFmpeg decoders for EAC3, Dolby Atmos, AC3, TrueHD, DTS, FLAC, and 5.1/7.1 multichannel downmixing.
- **YouTube-Style Native PiP**: Stripped Compose mini-player in favor of OS hardware-accelerated transforms (`setSourceRectHint`) for zero-stutter enter/exit transitions.
- **Swipeable Hero Carousel**: Compose `HorizontalPager` with smooth pill indicators on the Home dashboard.
- **Customizable Subtitles**: Real-time preview card, typography presets (Roboto Sans, Condensed, Monospace, System Default), font scale (80%–150%), text colors, and background opacity.
- **Display & Appearance**: Corner style presets (Rounded 24dp, Compact 14dp, Sharp 0dp) and smooth 320ms crossfade theme blend.
- **Aurora Splash Screen**: Authentic Jadzumi curved bezier play symbol, animated aurora ambient glow, frosted glass badge, and dynamic loading timeline.

---

## Upcoming Milestones

### Milestone 5A — Smart Episode Continuity & TV Series Organization (Immediate Next)
1. **Series & Seasons Grouping**:
   - Automatic detection of episode naming patterns:
     - `S01E01`, `S02E05`
     - `1x04`, `2x12`
     - `Episode 12`, `EP10`
   - Clustered under dedicated TV Series screens matching `design-references/jadzumi_tablet_tv_show_details_refined.html`.
   - Tabbed season switcher: `Season 1`, `Season 2`, `OVAs & Specials`, with watched count badges.
2. **"Up Next" Highlighting & Watched Badges**:
   - Per-episode progress bars, remaining runtime, and watched status checkmarks (`Watched`, `In Progress`, `Unwatched`).
   - Home dashboard hero card automatically spotlights the next unwatched episode in line.
3. **10-Second Next-Episode Autoplay Countdown**:
   - In `CinemaPlayer.kt`, detects when an episode enters its last ~15 seconds.
   - Sleek frosted-glass banner slides in: *"Next episode starts in 10s"* with **Play Now** and **Cancel** buttons.
   - Fluid auto-advance to the next sequential episode when the timer reaches 0.

---

### Milestone 5B — Local Art & Optional Metadata
1. **100% Offline-First by Default**:
   - Zero network permissions and zero unsolicited web calls.
2. **Local Sidecar Scraping**:
   - Automatically reads local poster and backdrop art (`poster.jpg`, `fanart.jpg`, `cover.png`, `folder.jpg`).
   - Parses local `.nfo` XML metadata already sitting inside movie and TV series folders (synopsis, cast, studio, rating, release date).
3. **Optional Online Metadata (Opt-In)**:
   - User-toggleable in Settings (strictly disabled by default).
   - Fetches missing metadata and backdrop art, caching everything strictly locally on tablet storage for offline viewing.

---

### Milestone 7 — Comprehensive Design Parity Audit & Polish
1. **Pixel-for-Pixel Audit**:
   - Side-by-side QA of all native Jetpack Compose screens against the 6 approved HTML reference designs in `design-references/`:
     - `jadzumi_tablet_home_library_refined.html`
     - `jadzumi_tablet_tv_show_details_refined.html`
     - `jadzumi_tablet_movie_details_refined.html`
     - `jadzumi_tablet_folders_storage_consistent_data.html`
     - `jadzumi_tablet_cinema_player_refined.html`
     - `jadzumi_tablet_settings_customization_about_me.html`
2. **Tablet Ergonomics & 120Hz QA**:
   - Xiaomi Pad 5 120Hz high-refresh rate verification for stutter-free scrolling and animations.
   - Seamless responsive adaptation across 16:10 landscape, 10:16 portrait, and Android split-screen multitasking.

---

### Milestone 8 — Production Hardening & v1.0 Release
1. **R8 / ProGuard Code Shrinking**:
   - Optimization and dead-code stripping to minimize APK size.
   - Comprehensive keep-rules for Media3, FFmpeg JNI bindings, and Jetpack Compose runtime.
2. **Production Build Verification**:
   - Memory leak analysis, native heap inspection, and battery consumption profiling.
   - Official v1.0 signed release packaging.
