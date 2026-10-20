# Jadzumi Video / Jadzumi Player — Active Development Checkpoint

Updated: 2026-10-20  
Version: 1.2.0 (Release)  
Target: Universal Android Devices — Handheld Phones, Tablets & Large Screens (16:10, 16:9, 19.5:9, 20:9, 4:3, Foldables; Android 8.0–16, ARM64-v8a)  

## Milestone Status Summary

- **Milestone 1 — Tablet UI Foundation:** [COMPLETED & VERIFIED]
- **Milestone 2 — Local Video Library:** [COMPLETED & VERIFIED]
- **Milestone 3A — Cinema Playback:** [COMPLETED & VERIFIED]
- **Milestone 3B — Library Refresh:** [COMPLETED & VERIFIED]
- **Milestone 3C — Internal Storage Management:** [COMPLETED & VERIFIED]
- **Combined Milestone 4 + 6 — Privacy, Settings, Branding, Theme Bloom & Responsive UI:** [COMPLETED & VERIFIED]
- **Milestone 5A & 5B — TV Continuity, Dedicated Movies, Local Art & Sidecar Engine:** [COMPLETED & VERIFIED]
  - TV series & seasons grouping (`TvShowDetailsPage.kt`) with autoplay countdown banner.
  - Dedicated **Movies** tab (`MovieCardGrid`, `MovieDetailsPage.kt`) with strict media type isolation.
  - Offline-first local sidecar scraping (`poster.jpg`, `fanart.jpg`, `cover.png`, `.nfo` parsing) with SQLite v3 schema and `SidecarMemory` LRU caching.
- **Milestone 7 — Comprehensive Design Parity Audit & Polish:** [COMPLETED & VERIFIED]
  - Universal tablet compatibility: removed all device-specific strings.
  - 120Hz/144Hz high refresh rate display mode negotiation.
  - Fluid adaptive layouts across 16:10 landscape, portrait, and split-screen modes.
  - Cleaned up demo/template triggers for production-ready standalone UI.
- **Milestone 8 — Production Hardening, Binary Security & v1.0 Official Release:** [COMPLETED & PACKAGED]
  - R8 code shrinking and resource shrinking enabled for release builds.
  - Complete ProGuard keep-rules for NextLib FFmpeg native JNI decoders, Media3 ExoPlayer, Jetpack Compose, and domain models.
  - Continuous keystore signature preservation for seamless tablet update without watched history or settings loss.
  - Upgraded to `versionCode = 9`, `versionName = "1.0.0"`.
  - Modernized Kotlin compilerOptions DSL (`JvmTarget.JVM_17`).
  - Automated CI build for both Release and Debug APKs.
- **Milestone 9 — Universal Phone Adaptation & Glassmorphic Polish (v1.1.0):** [COMPLETED & PACKAGED]
  - Floating frosted glass bottom navigation bar with animated indicator pill for mobile displays (`< 600dp`).
  - Adaptive 2-row header with full-width search capsule and 2x2 responsive category quick cards.
  - Mobile Cinema Player: one-tap orientation toggle (Landscape/Portrait/Sensor) and responsive bottom sheet Quick Menu in phone portrait.
  - Thumb-friendly bottom padding and universal "device's internal storage" dialog cleanups.
  - Upgraded to `versionCode = 10`, `versionName = "1.1.0"`.
- **Milestone 10 — Expressive Themes, App Icon Studio, Audiophile DSP, Double-Tap Ripples & Phone Portrait Overhaul (v1.2.0):** [COMPLETED & PACKAGED]
  - Eradicated empty-state mockup cards; added first-run `WelcomeOnboardingCard` SAF folder picker.
  - Fully adapted Cinema Player, Movie Details, TV Show Details, and Storage Cards for phone portrait mode.
  - Implemented 5 Expressive Themes: Aura Flow, Frost Crystal, Neo Cinema, Soft Motion, and Prism across Light, Dark, and AMOLED modes.
  - Added Custom App Icon Studio under Settings → Display with live vector preview and dynamic launcher switching via Android `<activity-alias>`.
  - Audiophile DSP: Night Mode / Dialogue Voice Boost (`LoudnessEnhancer`), Bluetooth lip-sync audio delay offset (`±500ms`), and in-player subtitle sync offset calibration.
  - Gestures & Delights: YouTube-style double-tap seeking ripples with chevrons and haptics, plus "Skip Intro (+85s)" frosted pill.
  - Upgraded to `versionCode = 11`, `versionName = "1.2.0"`.
