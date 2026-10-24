# Jadzumi Video Player

> | Universal Android (Phones, Foldables & Tablets) | Offline-First Cinema Player | Zero Tracking | Jetpack Compose & Media3

---

## Overview

**Jadzumi Video Player** is a high-performance, private offline cinema and TV player built for Android. Powered by **Jetpack Compose** and **NextLib Media3 (ExoPlayer + bundled FFmpeg JNI decoders)**, Jadzumi combines fluid gesture controls, intelligent display calibration, audiophile audio DSP, and an adaptive frosted glassmorphic interface tailored for phones and tablets.

---

## What's New in v1.3.3

- Online metadata and poster downloads now require the default-off privacy toggle.
- HTTP requests use a shared versioned User-Agent with no personal email.
- Release signing uses private credentials; the development key is for debug builds only.
- About includes the public source link and bundled licenses for offline viewing.
- GPL-3.0 and verified native dependency notices are included. Artwork provenance and
  outstanding legal review remain unresolved; see THIRD_PARTY_LICENSES.md.
- GitHub Actions builds versioned APK artifacts and publishes signed releases from main.

### Previous changes in v1.3.2

- **Optimized Mobile Dock Alignment**: Balanced floating bottom bar with equal destination distribution (`Modifier.weight(1f)`) so Home, Movies, TV Shows, and Settings are always fully visible, centered, and accessible on any screen width.
- **Dynamic System Default Appearance**: Frosted search capsules, bottom docks, and cards dynamically match device light and dark modes in real time with authentic frosted glass surfaces and zero color clashing.
- **Stutter-Free Movie & TV Details Transitions**: Eliminated top bar layout thrash and thumbnail texture scaling hitches for buttery 60/120fps card opening and closing.
- **Mobile Portrait Glassify Controls**: Streamlined single-line frosted blur scale markers (`Matte 20%`, `Balanced 65%`, `Frosted 100%`) for clean mobile portrait display.
- **Subtle 0.75dp Specular Borders**: Refined hairline frosted borders across cards, dialogs, and dock capsules, eliminating harsh outlines while preserving crisp edge definition.
- **Hold-to-2x Speed Fast-Forward**: Press and hold anywhere on the playback screen to instantly boost speed to 2.0x with haptic feedback; release to restore normal playback.
- **Intelligent End-Credit Detection**: Automatically identifies credit sequences and presents a non-intrusive 12-second Next Episode countdown with a direct "Watch Credits" option.
- **Haptic Timeline Scrubbing & Chapter Markers**: Visual chapter tick marks along the timeline with tactile magnetic snapping during seeking.
- **Direct Voice Clarity / Dialogue Boost**: One-tap HUD button to enhance quiet dialogue and compress harsh audio spikes.
- **External Subtitle Loader**: Integrated system file picker supporting loose `.srt`, `.vtt`, and `.ass` subtitles.
- **Poster Grid & Detailed List Toggle**: One-tap toggle between a multi-column visual poster grid and rich metadata rows.
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
- **10 Handcrafted Custom App Icons**: Native dynamic launcher icon switching with 10 distinct art profiles (Original, Neon Protocol, VHS Nostalgia, Anime Pop, Boo!, Zombumi, Pixel Quest, Chrome X, Cosmic Portal, and Funky Doodle).
- **Scroll Blend-Fade**: Hardware-accelerated edge fades during list scrolling.

### 4. Library & Series Organization
- **Automated Series Parsing**: Intelligent detection of season and episode patterns (`S01E01`, `1x04`).
- **Continue Watching Shelf**: Quick-resume carousel with progress indicators and remaining time badges.
- **Dual Metadata Pipeline**: Offline `.nfo` sidecars with Wikipedia REST & TVMaze online fallback caching.

---

## Technical Specifications

| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.2.10 (JVM 17 Target) |
| **UI Toolkit** | Jetpack Compose (Material 3 Expressive) |
| **Media Framework** | AndroidX Media3 (ExoPlayer 1.11.1) |
| **Native Codecs** | NextLib Media3 FFmpeg 9.0.1 JNI |
| **Minimum SDK** | Android 8.0 (API 26) |
| **Target SDK** | Android 15 (API 35) / Compile SDK 36 |
| **Architecture** | ARM64 (`arm64-v8a`) |
| **Minification** | R8 Code Shrinking + Resource Optimization |

---

## Build & Verification

```bash
# Run unit tests and generate release bundle
./gradlew test assembleDebug --no-daemon
# With release signing configured (see below):
./gradlew assembleRelease --no-daemon
```

---

## License & Privacy

- **Privacy First**: 100% offline-capable, zero analytics, zero telemetric beacons.
- **Open Source**: Licensed under [GPL-3.0](LICENSE). See [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) for decoder compliance.

## Online metadata privacy

Online lookups default to **off**. The Privacy settings toggle (`playback` preferences,
`online_metadata`) gates Wikipedia/TVMaze lookups, fallbacks, and remote image requests.
Cached metadata and posters remain readable while disabled. Enabling lookups sends
search titles to those services and requests artwork from their image hosts; it does
not send playback history. Opening the About screen's source link explicitly launches
an external browser; licenses are bundled for offline viewing.

## Release signing

Debug builds alone use the checked-in development key. Release builds read these
environment variables, or the corresponding keys in an ignored root `keystore.properties`:

| Environment variable | Property |
| --- | --- |
| `ANDROID_KEYSTORE_FILE` | `storeFile` |
| `ANDROID_KEYSTORE_PASSWORD` | `storePassword` |
| `ANDROID_KEY_ALIAS` | `keyAlias` |
| `ANDROID_KEY_PASSWORD` | `keyPassword` |

`storeFile` is absolute or relative to the repository root. Missing credentials produce
an **unsigned** release, never a release signed with the debug key. Keep keys and
passwords outside Git. Create a key locally with interactive password prompts:

```sh
keytool -genkeypair -v -keystore "$HOME/jadzumi-release.jks" -storetype JKS -alias jadzumi-release -keyalg RSA -keysize 4096 -validity 10000
```

In GitHub Actions, add `RELEASE_KEYSTORE_BASE64` (the base64-encoded keystore),
`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` as
repository secrets. CI builds and publishes a signed release only when all four are
present; otherwise it runs tests and produces the debug APK. Artifacts use `versionName`.
For GNU/Linux, `base64 -w 0 "$HOME/jadzumi-release.jks" > "$HOME/jadzumi-release.base64"`
writes the secret input to a local file without printing it. Protect and remove that
file after secure secret entry, and back up the keystore securely.

**Existing installations:** a new signing key cannot update APKs signed with the old
development key. Plan signing continuity or a reinstall/migration before replacing
the public APK. Increment `versionCode` for the next release. Publishing this source
does not by itself establish complete Corresponding Source for native dependencies;
see the remaining review tasks in [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).
