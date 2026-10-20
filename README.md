# 🎬 Jadzumi Video — Modern Cinema & TV Player for Android

> **Version 1.2.0 (Release)** · Universal Android Support: Handheld Phones, Tablets & Foldables · 100% Offline-First · Zero Tracking · Jetpack Compose & Media3 FFmpeg Engine

---

## 🌟 Overview

**Jadzumi Video** is a high-performance, private, offline-first cinema video player engineered for modern Android devices. Built with **Jetpack Compose** and **NextLib Media3 (ExoPlayer + bundled FFmpeg C++ decoders)**, Jadzumi delivers fluid gesture controls, dynamic hardware detection, audiophile late-night audio enhancements, and an adaptive glassmorphic interface that seamlessly scales from compact smartphones to 16:10 tablets and foldables.

---

## ✨ Current Features & Capabilities

### 📱 1. Universal Phone & Tablet Ergonomics
- 🌊 **Floating Frosted Glass Bottom Navigation Bar**: Deploys automatically on smartphones (`< 600dp`) featuring blurred translucent glass, specular border styling, and animated indicator pills across 4 core tabs: **Home**, **Movies**, **TV Shows**, and **Settings**.
- 🧭 **Vertical Navigation Rail (AppRail)**: Displays on tablets and wide screens (`≥ 640dp`) with quick navigation and branding.
- 📐 **Adaptive Cinema Player Dock**: Segregates Time Capsule and Action Dock into a two-row layout on mobile portrait screens to prevent button collisions, expanding into a unified single-row dock in landscape and tablet views.
- 🔄 **One-Tap Orientation Switcher**: In-player button instantly cycles between **Landscape**, **Portrait**, and **Auto-Sensor** rotation without leaving your video.
- 🗂️ **Clean Full-Width Library Pages**: Direct media flow without redundant category cards or empty filler blocks, letting your movie posters and episode art take center stage.

---

### ⚡ 2. Real-Time Dynamic Hardware & Display Inspector
- 🔍 **Dynamic SoC & Chipset Detection**: Automatically inspects device model and processor chipset (`Build.SOC_MODEL`, `Build.HARDWARE`, `Build.BOARD`, `Build.MODEL`) with NextLib FFmpeg native fallback.
- 📏 **Universal Display Aspect Ratio Metric**: Dynamically calculates screen aspect ratio (e.g. `19.5:9`, `20:9`, `21:9` on modern smartphones, `16:10` / `4:3` on tablets) along with exact hardware resolution.
- 🌈 **Hardware HDR Inspection**: Queries `Display.hdrCapabilities` to identify native **HDR10**, **HDR10+**, **HLG**, and **Dolby Vision** support, automatically enabling high-contrast tone mapping for SDR displays.
- 🎧 **Acoustic Optimization**: Universal stereo downmixing calibrated for device stereo speakers, earpieces, and headphones.

---

### 🔊 3. Audiophile & Late-Night Watching DSP
- 🌙 **Night Mode / Dialogue Voice Boost**: Native dynamic range compression via Android `LoudnessEnhancer`, bringing quiet whispered speech forward while smoothing sudden loud explosions for late-night viewing.
- 📶 **Bluetooth Wireless Audio Lip-Sync**: Fine-tune wireless audio delay calibration (`±500ms` in 50ms increments) from Settings or the Cinema Player quick menu.
- ⏱️ **In-Player Subtitle Delay Calibration**: Adjust subtitle timing offset on-the-fly (`±10s` in 100ms increments) for out-of-sync SRT or ASS tracks.
- 🎼 **Bundled NextLib FFmpeg Decoders**: High-fidelity hardware & software decoding for **Dolby Atmos**, **Dolby Digital (AC-3)**, **Dolby Digital Plus (E-AC-3)**, **TrueHD**, **DTS-HD**, **FLAC**, and **AAC**.
- 🔒 **Screen-Off Background Audio**: Continue listening to concerts, podcasts, and video commentary with your screen locked.

---

### 🎮 4. Premium Player Gestures & Delights
- 💫 **YouTube-Style Seek Ripples**: Double-tap left or right to seek with expanding frosted arc ripples, dynamic chevrons (`«««` / `»»»`), seek time accumulator, and tactile haptic ticks.
- 🎚️ **Dual-Side Edge Swipes**: Smooth vertical swipe gestures with deadzone protection — left side controls screen brightness; right side controls media volume.
- ⏩ **Long-Press 2x Speed**: Press and hold anywhere on the player surface to temporarily accelerate playback to 2.0x speed.
- ⏭️ **"Skip Intro (+85s)" Frosted Capsule**: Contextually appears in the opening minutes of TV episodes and films for instant one-tap intro skipping.
- 🖼️ **Hardware Picture-in-Picture (PiP)**: Seamless background playback with aspect ratio hints (`setSourceRectHint`).

---

### 📺 5. Smart TV Series & Movies Management
- 🏷️ **Automated Series & Seasons Grouping**: Automatically identifies series patterns (`S01E01`, `1x04`, `Episode 5`) into organized series hubs and season carousels.
- ⏳ **Next-Episode Autoplay Countdown**: 10-second autoplay countdown banner with instant "Play Now" and "Cancel" buttons.
- 🎬 **Dedicated Movies Tab**: Isolates standalone films with high-res poster grids and rich detail pages (`MovieDetailsPage.kt`) featuring codec badges and resume markers.
- 🚀 **First-Run Welcome Onboarding**: Clean Storage Access Framework (SAF) folder picker card guiding new users immediately.

---

### 📁 6. Integrated Storage & Folder Management
- ⚙️ **Direct Settings Integration**: Folder and disk management lives under **Settings → Storage & Folders**.
- 📊 **Storage Summary Card**: Live internal storage usage bar alongside indexed media byte calculations.
- 🛡️ **Active Directory Manager**: Add media directories with folder removal protection dialogs.
- 🔄 **Quick Management Tools**: One-tap **Rescan Folders** and **Clear Thumbnail Cache** buttons.

---

### 🎨 7. Clean Material 3 Display Modes
- ☀️ **Light Mode**: Airy Pale-Blue aesthetic for bright daytime environments.
- 🌙 **Dark Mode**: Deep Night Slate styling for cinema immersion.
- 🖤 **AMOLED Mode**: Pure Black background for maximum OLED power savings and infinite contrast.
- 📐 **Corner Curvature Presets**: Choose between **Rounded** (24dp curve), **Compact** (14dp curve), or **Sharp** (0dp crisp).
- 🌺 **Theme Bloom Transition**: Cinematic fade blend animation when switching appearance themes.

---

### 🔒 8. 100% Offline-First Privacy Guarantee
- 📴 **Local Sidecar Scraping**: Automatically discovers offline `poster.jpg`, `fanart.jpg`, `cover.png`, and Jellyfin/Kodi `.nfo` metadata.
- 🚫 **Zero External Tracking**: No telemetry, no ads, no trackers, and no background network sweeps.
- 💾 **Strictly On-Device**: Playback history, resume timestamps, and subtitle preferences are stored exclusively on your device.

---

## 🛠️ Building from Source

```bash
# 1. Clone the repository
git clone https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player.git
cd Jadzumi-Video-Player

# 2. Build Release APK (Minified & R8 Shrunk)
./gradlew assembleRelease

# 3. Build Debug APK
./gradlew assembleDebug

# 4. Run Unit Tests
./gradlew test
```

Compiled APK artifacts are generated in:
- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/apk/debug/app-debug.apk`

---

## 📋 System Requirements

| Specification | Requirement |
| :--- | :--- |
| **Operating System** | Android 8.0 (Oreo / API 26) through Android 16 (Baklava / API 35+) |
| **Architecture** | ARM64 (`arm64-v8a`) |
| **Form Factors** | Smartphones, Tablets, Foldables (16:10, 16:9, 19.5:9, 20:9, 21:9, 4:3) |
| **Media Decoders** | Media3 ExoPlayer + NextLib FFmpeg Native JNI |

---

## 👤 Author & Philosophy

- **Creator & Product Direction**: Omkar Patil  
- **Philosophy**: Crafted to deliver uncompromised video playback with fluid gesture controls, accurate color rendering, and complete privacy.  
- *"No popcorn included. Sorry."* 🍿
