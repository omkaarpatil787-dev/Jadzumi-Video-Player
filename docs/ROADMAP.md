# Official Jadzumi Video Player Roadmap

> **Target Platform:** Universal Android Devices — Handheld Phones, Tablets & Foldables (16:10, 16:9, 19.5:9, 20:9, 4:3; optimized for 60Hz/90Hz/120Hz/144Hz, Android 8.0–16, ARM64-v8a)  
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

### Milestone 8 — Production Hardening, Binary Security & v1.0 Official Release (Approved)
- **R8 / ProGuard Code Shrinking & Obfuscation**: Enabled minification, resource shrinking, and native JNI keep rules for Release builds.
- **Keystore Alignment**: Maintained development keystore compatibility for seamless in-place updates without data wipe.
- **Toolchain Modernization**: Migrated to `JvmTarget.JVM_17` compilerOptions DSL.

---

## Current Milestone

### Milestone 9 (v1.1.0) — Universal Phone Adaptation & Glassmorphic Polish (Approved)
- **Floating Frosted Glass Bottom Navigation Bar**:
  - Dynamically activates for phone form factors (`< 600dp`), floating gracefully with a translucent frosted container, specular white border, and animated indicator pill.
  - Seamlessly keeps the 96dp navigation rail on tablets and foldables.
- **Phone Ergonomics & Layout Optimization**:
  - Adaptive two-row phone top bar: Greeting/Title header + full-width frosted search capsule.
  - Responsive 2×2 quick category filter cards on phones (1×4 on tablets).
  - Scaled hero banner card height (220dp on phones, 260dp on tablets) with thumb-safe bottom content padding.
- **Phone Cinema Player Upgrades**:
  - One-tap screen orientation toggle (Landscape / Portrait / Sensor Auto).
  - Quick Menu automatically transforms into a frosted bottom sheet on portrait phone displays.
  - Automatic orientation reset to sensor preference upon closing player.
  - Centered bottom alignment for the 10-second next-episode autoplay banner.
- **Refined Glassmorphism & UI Consistency**:
  - Polished specular highlights, frosted borders, and high-translucency surfaces.
  - Universal "device's internal storage" localization across all confirmation dialogs.
- **Version Tag**: `versionCode = 10`, `versionName = "1.1.0"`.

---

## Current Milestone

### Milestone 10 (v1.2.0) — Expressive Themes, App Icon Studio, Audiophile DSP, Double-Tap Ripples & Phone Portrait Overhaul (Completed)
- **Eradication of "Mockup" Feel & First-Run SAF Onboarding**:
  - Identified and removed legacy fallback cards with inert no-op click listeners that rendered on empty library state.
  - Introduced `WelcomeOnboardingCard` with direct, zero-friction "Select Video Folder" Storage Access Framework picker.
- **Mobile Phone Portrait Adaptations**:
  - **Cinema Player**: Adaptive 2-row layout on phone portrait (< 600dp) cleanly separating the Time Capsule and Actions Dock to eliminate clipping and overlap.
  - **MovieDetailsPage & TvShowDetailsPage**: Stacked poster/header, `FlowRow` metadata chips, full-width primary action buttons, and responsive secondary docks preventing horizontal overflow.
  - **StorageSummaryCard**: Adaptive vertical stacking on compact phone displays.
- **Audiophile & Late-Night Cinema Enhancements**:
  - **Night Mode / Dialogue Voice Boost**: Native Android `LoudnessEnhancer` dynamic range compression DSP bringing quiet dialogue forward while softening jarring explosions for midnight viewing.
  - **Bluetooth Audio Delay (Lip-Sync Compensation)**: Real-time audio timing offset (`±500ms` in 50ms steps) accessible both from Settings → Audio and directly inside the player Quick Menu.
  - **In-Player Subtitle Delay Adjustment**: Instant subtitle synchronization offset calibration (`±10s` in 100ms steps) in Subtitle Settings and player Quick Menu.
- **Premium Gestures & Player Delights**:
  - **YouTube-Style Animated Seeking Ripples**: Double-tap left/right seek with expanding frosted arc ripples, dynamic chevrons (`«««` / `»»»`), and haptic feedback.
  - **"Skip Intro (+85s)" Frosted Capsule Pill**: Appears dynamically in opening minutes of episodes/films with smooth fade-out and haptics.
- **Signature Expressive Themes (Jadzumi Design Lab Parity)**:
  - 5 distinctive visual personalities: **Aura Flow** (🩵), **Frost Crystal** (❄️), **Neo Cinema** (🎞️), **Soft Motion** (🌸), and **Prism** (💜).
  - 3 independent appearance modes: **Light**, **Dark**, and **AMOLED** (Pure Black).
- **Custom App Icon Studio & Dynamic Launcher Switcher**:
  - Integrated App Icon Studio under **Settings → Display** with interactive folded-play vector preview.
  - Silhouette selectors (`Squircle`, `Circle`, `Soft square`) and finish styles (`Frosted`, `Solid`, `Outlined`).
  - "Apply to Home Screen Launcher" button dynamically switching Android `<activity-alias>` (`LauncherAura`, `LauncherFrost`, `LauncherNeo`, `LauncherSoft`, `LauncherPrism`) via `PackageManager`.
- **Version Tag**: `versionCode = 11`, `versionName = "1.2.0"`.
