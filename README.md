# Jadzumi Video — Modern Cinema & TV Player for Android Tablets

**Version 1.0.0 (Release)**  
Target: Universal Android Tablets & Large Screens (16:10, 16:9, 4:3, Foldables; Android 8.0–16, ARM64-v8a)  
Architecture: 100% Offline-First, Native Jetpack Compose, Media3/ExoPlayer + FFmpeg C++ Decoders, Material 3 Expressive UI.

---

## Key Features

- **Hardware Cinema Playback**:
  - Direct `SurfaceView` video rendering with multi-format aspect ratio controls (Fit, Fill, 16:9, 16:10, 4:3, Zoom) and playback speed (0.5x–2.0x).
  - Native gesture controls for smooth horizontal seek swipes, dual-side vertical brightness & volume sliders.
  - Instant resume with `SeekParameters.CLOSEST_SYNC` keyframe alignment and low-latency buffer management.

- **Advanced Audio & Subtitle Engine**:
  - Native FFmpeg decoders for Dolby Digital (AC-3), Dolby Digital Plus (E-AC-3), Dolby Atmos, TrueHD, DTS-HD, and FLAC.
  - Multi-speaker matrix stereo downmix optimized for tablet stereo speaker arrays and headphones.
  - Full support for embedded and external subtitles (Styled ASS/SSA, SubRip SRT, WebVTT) with real-time typography scaling, color palette selection, and background opacity controls.

- **Smart TV Series & Dedicated Movies Organization**:
  - Automatic episode detection (`S01E01`, `1x04`, `Episode 12`) and grouping into dedicated TV Series and Seasons pages.
  - Dedicated **Movies** tab with strict media type isolation, grid cards, and detailed movie view (`MovieDetailsPage`).
  - Sleek 10-second next-episode autoplay countdown banner with instant "Play Now" and "Cancel" buttons.

- **100% Offline-First Local Sidecar Engine**:
  - Scans and caches local folder art (`poster.jpg`, `fanart.jpg`, `cover.png`) and `.nfo` metadata with memory-safe downsampling (`SidecarMemory`).
  - Zero unsolicited network requests or external tracking.

- **Fluid 120Hz Tablet Ergonomics & Native PiP**:
  - Programmatic high refresh rate synchronization requesting up to 120Hz/144Hz displays.
  - True hardware Picture-in-Picture with seamless aspect ratio transitions (`setSourceRectHint`).
  - Adaptive 16:10 landscape, portrait, and split-screen responsive layouts.
  - Ambient Aurora splash screen with animated bezier play glyph.

- **Production Hardening (v1.0.0)**:
  - R8 code shrinking and resource optimization enabled.
  - Precision ProGuard keep-rules for native FFmpeg JNI bindings and Media3 ExoPlayer renderers.
  - Consistent signing key for zero-friction in-place updates without data wipe.

---

## Building from Source

```bash
# Clone the repository
git clone https://github.com/onkar/Jadzumi-Video-Player.git
cd Jadzumi-Video-Player

# Build Release APK (Minified & Shrunk)
./gradlew assembleRelease

# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew test
```

Artifacts are produced in `app/build/outputs/apk/release/` and `app/build/outputs/apk/debug/`.
