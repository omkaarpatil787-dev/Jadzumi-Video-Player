# Jadzumi Video Player

> | Universal Android (Phones, Foldables & Tablets) | Offline-First Cinema Player | Zero Tracking | Jetpack Compose & Media3

---

## Overview

**Jadzumi Video Player** is a high-performance, private offline cinema and TV player built for Android. Powered by **Jetpack Compose** and **NextLib Media3 (ExoPlayer + bundled FFmpeg JNI decoders)**, Jadzumi combines fluid gesture controls, intelligent display calibration, audiophile audio DSP, and an adaptive frosted glassmorphic interface tailored for phones and tablets.

---

## What's New in v1.3.0

- **Hold-to-2x Speed Fast-Forward**: Press and hold anywhere on the playback screen to instantly boost speed to 2.0x with haptic feedback; release to restore normal playback.
- **Intelligent End-Credit Detection**: Automatically identifies credit sequences and presents a non-intrusive 12-second Next Episode countdown with a direct "Watch Credits" option.
- **Haptic Timeline Scrubbing & Chapter Markers**: Visual chapter tick marks along the timeline with tactile magnetic snapping during seeking.
- **Direct Voice Clarity / Dialogue Boost**: One-tap HUD button to enhance quiet dialogue and compress harsh audio spikes.
- **External Subtitle Loader**: Integrated system file picker supporting loose `.srt`, `.vtt`, and `.ass` subtitles.
- **Poster Grid & Detailed List Toggle**: One-tap toggle between a multi-column visual poster grid and rich metadata rows.
- **Smart Filter & Sort Chips**: Quick filters (*All*, *In Progress*, *Unwatched*, *4K UHD*, *Recently Added*) and flexible sorting (*Date*, *Title*, *Duration*, *Size*).
- **Batch Video Management**: Long-press selection mode for marking items watched/unwatched or clearing playback progress in bulk.
- **Ambient Blurred Backdrop Glow**: Detail pages softly sample video artwork into a luminous, theatrical background glow.
- **TV Season Progress & Up Next Spotlight**: Visual season completion gauges and prominent next-unwatched episode action cards.

---

## Core Capabilities

### 1. Playback & Gesture Ergonomics
- **Fluid Gestures**: Vertical swipes for brightness and volume with deadzone touch protection; double-tap ripple seek with accumulator.
- **Hold-to-2x Speed**: Seamless acceleration on press down, instant restore on release.
- **Haptic Timeline Bar**: Periwinkle accent bar with chapter markers and tactile snapping.
- **Quick Audio & Subtitle Adjustments**: On-the-fly subtitle sync (`±10s`) and Bluetooth lip-sync compensation (`±500ms`).
- **Aspect Ratio Control**: Fit to screen (letterbox), Crop to fill (zoom), and Stretch modes.
- **Picture-in-Picture (PiP)**: Smooth background playback preserving source aspect ratios.

### 2. Audio Architecture & Decoders
- **Bundled FFmpeg Decoders**: Lossless decoding for Dolby Atmos, AC-3, E-AC-3, TrueHD, DTS-HD, FLAC, and AAC.
- **Dialogue Voice Boost**: Dynamic range compression via Android `LoudnessEnhancer`.
- **Background Audio**: Lock-screen and screen-off playback.

### 3. Display Calibration & Visual Polish
- **OLED / AMOLED Peak Luminance**: Automatically applies peak HDR brightness exclusively on OLED/AMOLED panels while protecting LCD backlights.
- **Frosted Glass Engine**: Directional embossed border strokes, frosted surface diffusion, and customizable glass intensity (0% to 100%).
- **Tri-Theme Fidelity**: Calibrated for Light, Dark (obsidian navy), and AMOLED (true pitch black).
- **Scroll Blend-Fade**: Hardware-accelerated edge fades during list scrolling.

### 4. Library & Series Organization
- **Automated Series Parsing**: Intelligent detection of season and episode patterns (`S01E01`, `1x04`).
- **Continue Watching Shelf**: Quick-resume carousel with progress indicators and remaining time badges.
- **Dual Metadata Pipeline**: Offline `.nfo` sidecars with Wikipedia REST & TVMaze online fallback caching.

---

## Technical Specifications

| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.0 (JVM 17 Target) |
| **UI Toolkit** | Jetpack Compose (Material 3 Expressive) |
| **Media Framework** | AndroidX Media3 (ExoPlayer 1.11.1) |
| **Native Codecs** | NextLib Media3 FFmpeg 6.1 JNI |
| **Minimum SDK** | Android 8.0 (API 26) |
| **Target SDK** | Android 15 (API 35) / Compile SDK 36 |
| **Architecture** | ARM64 (`arm64-v8a`) |
| **Minification** | R8 Code Shrinking + Resource Optimization |

---

## Build & Verification

```bash
# Run unit tests and generate release bundle
./gradlew test assembleRelease assembleDebug --no-daemon
```

---

## License & Privacy

- **Privacy First**: 100% offline-capable, zero analytics, zero telemetric beacons.
- **Open Source**: Licensed under the MIT License. See [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) for decoder compliance.
