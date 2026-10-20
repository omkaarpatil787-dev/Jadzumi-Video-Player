# Official Jadzumi Video Player Roadmap

> **Target Platform:** Universal Android Tablets & Large Screens (16:10, 16:9, 4:3, Foldables; optimized for 120Hz/144Hz, Android 8.0–16, ARM64-v8a)  
> **Core Philosophy:** 100% Offline-First by default, Native Jetpack Compose (no WebViews), Material 3 Expressive UI, Hardware-Accelerated Playback, Zero Tracking.

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

### Milestone 4 — Cinema Audio, Native PiP, Expressive UI & Aurora Splash (Approved)
- **FFmpeg Audio Decoding**: Native C++ FFmpeg decoders for EAC3, Dolby Atmos, AC3, TrueHD, DTS, FLAC, and 5.1/7.1 multichannel downmixing.
- **YouTube-Style Native PiP**: Stripped Compose mini-player in favor of OS hardware-accelerated transforms (`setSourceRectHint`) for zero-stutter enter/exit transitions.
- **Swipeable Hero Carousel**: Compose `HorizontalPager` with smooth pill indicators on the Home dashboard.
- **Customizable Subtitles**: Real-time preview card, typography presets (Roboto Sans, Condensed, Monospace, System Default), font scale (80%–150%), text colors, and background opacity.
- **Display & Appearance**: Corner style presets (Rounded 24dp, Compact 14dp, Sharp 0dp) and smooth 320ms crossfade theme blend.
- **Aurora Splash Screen**: Authentic Jadzumi curved bezier play symbol, animated aurora ambient glow, frosted glass badge, and dynamic loading timeline.

### Milestone 5A & 5B — TV Continuity, Dedicated Movies, Local Art & Sidecar Engine (Approved)
- **TV Series & Seasons Grouping**: Automatic clustering by SxxExx, 1x04, Episode naming with dedicated TV Series details screen (`TvShowDetailsPage.kt`).
- **Dedicated Movies Tab & Movie Details Screen**: Strict media type isolation between Movies and TV shows, responsive `MovieCardGrid`, and `MovieDetailsPage.kt` matching reference design.
- **Robust Local Sidecar Engine**: Directory-aware detection of `poster.jpg`, `cover.png`, `fanart.jpg`, and `.nfo` files with subfolder mapping and SQLite v3 unique URI schema.
- **10-Second Autoplay Banner**: Frosted-glass next-episode autoplay countdown banner in `CinemaPlayer.kt`.

### Milestone 7 — Comprehensive Design Parity Audit & Polish (Approved)
- **Universal Tablet Compatibility**: Stripped device-specific branding in favor of universal tablet multi-speaker and high-fidelity stereo downmix profiles.
- **120Hz / High Refresh Rate Optimization**: Programmatic display mode negotiation (`preferredDisplayModeId`) targeting 120Hz/144Hz with memory-bounded downsampled image decoding (`SidecarMemory`).
- **Responsive Layout & Ergonomics**: Adaptive 16:10 landscape, portrait, and split-screen layouts across all library, details, folder, and player screens.
- **Production Standalone UI**: Removed legacy "View original design samples" demo buttons and test triggers across all tabs.

### Milestone 8 — Production Hardening, Binary Security & v1.0 Official Release (Completed)
- **R8 / ProGuard Code Shrinking & Obfuscation**:
  - `isMinifyEnabled = true` and `isShrinkResources = true` enabled for production release builds.
  - Comprehensive keep-rules for NextLib FFmpeg native JNI methods, AndroidX Media3 / ExoPlayer, Jetpack Compose runtime, and Jadzumi model data classes.
- **Seamless Upgrade & Keystore Continuity**:
  - Configured `release` signing with development keystore, guaranteeing in-place updates over existing tablet installs with zero data loss or watched progress reset.
- **Modernized Toolchain**:
  - Migrated Kotlin Gradle compilerOptions to type-safe DSL (`JvmTarget.JVM_17`), eliminating deprecation warnings.
- **Multi-Build CI Automation**:
  - Updated GitHub Actions workflow to run unit tests and package both `v1.0.0-Release` (optimized & shrunk) and `v1.0.0-Debug` APKs as workflow artifacts.
- **Version Stamp**:
  - Officially tagged as `versionCode = 9`, `versionName = "1.0.0"`.
