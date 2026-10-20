# 🎬 Jadzumi Video — Modern Cinema & TV Player for Android

> **Version 1.2.0 (Release)** · 📱 Universal Android Support: Handheld Phones, Foldables & Tablets · 📴 100% Offline-First · 🛡️ Zero Tracking · ⚡ Jetpack Compose & NextLib Media3 Engine

---

## 🌟 Overview

**Jadzumi Video** is a high-performance, private, offline-first cinema video player engineered for modern Android devices. Built with **Jetpack Compose** and **NextLib Media3 (ExoPlayer + bundled FFmpeg C++ decoders)**, Jadzumi delivers fluid gesture controls, real-time hardware detection, audiophile audio enhancements, and an adaptive glassmorphic interface that seamlessly adapts across smartphones in portrait and landscape, foldables, and large-screen tablets.

---

## ✨ Features & Capabilities

### 📱 1. Universal Phone & Tablet Ergonomics
- 🫧 **Auto-Minimizing Floating Dock**: Bouncy spring animation (`Spring.DampingRatioMediumBouncy`) smoothly collapses the bottom navigation into a sleek 52dp floating capsule while scrolling through movies and TV shows. A single tap pops it back open!
- 🎛️ **Customizable Dock Styles**: Choose between **Icons and labels** (default) or **Icons only** (ultra-sleek 54dp bar) directly under Settings › Display.
- 📐 **Balanced Mobile Portrait Cinema Player**: Timecode capsule pill and player actions dock are centered and symmetrically aligned in phone portrait mode to eliminate awkward staggered controls.
- 🎬 **Mobile-Optimized Movie Details**: Full-width title layout (no cutoffs or unclosed brackets), genre subtitle, side-by-side poster and metadata pill flow (`MovieBadgesFlow`), expandable synopsis, and symmetrical 44dp pill buttons.
- 🧭 **Tablet Navigation Rail (AppRail)**: Displays on tablets and wide screens (`≥ 640dp`) with direct access to library destinations.
- 🔄 **One-Tap Orientation Switcher**: In-player button instantly cycles between **Landscape**, **Portrait**, and **Auto-Sensor** rotation.
- 🗂️ **Clean Media Hubs**: Direct media grid layouts without clutter, putting your movie posters, TV series, and episode art front and center.

---

### ☀️ 2. Automatic HDR Peak Brightness & Display Optimization
- 🌈 **Auto HDR Detection**: Automatically detects **HDR10**, **HDR10+**, **Dolby Vision** (`dv`/`dovi`), and **HLG** content upon opening.
- 💡 **Peak Screen Luminance**: Automatically boosts display brightness to maximum (`1.0f`) with `COLOR_MODE_HDR` for vivid HDR highlights, seamlessly restoring your prior brightness level on player exit.
- 🔍 **Real-Time SoC & Chipset Probing**: Inspects device chipset and processor model (`Build.SOC_MODEL`, `Build.HARDWARE`, `Build.BOARD`) with native FFmpeg fallback.
- 📏 **Universal Display Metrics**: Calculates exact screen aspect ratio (`19.5:9`, `20:9`, `21:9` on phones; `16:10` / `4:3` on tablets) and native resolution.

---

### 🖼️ 3. Studio-Grade Poster Generation & Intelligent Metadata
- 🎞️ **Anti-Black Frame Thumbnail Extraction**: Intelligently samples frames at 12% into the runtime (`10s`–`180s`) with Android 10+ `loadThumbnail` integration, completely bypassing 0-second black intros.
- 🎨 **Cinematic Fallback Artwork**: Renders genre-matched atmospheric gradients (crimson for Horror, deep space cyan for Sci-Fi, navy for Action, royal purple for Animation) so missing posters are never blank or black.
- 🧠 **Intelligent Genre Detection**: Inspects film and show keywords to categorize Horror, Sci-Fi, Action, Animation, Comedy, and Crime accurately.
- 📖 **Authentic & Dynamic Synopses**: Built-in authentic plot summaries for major franchises (*Avengers, Interstellar, The Dark Knight, Dune, The Conjuring, etc.*) and intelligent contextual descriptions for uncataloged media.

---

### 🔊 4. Audiophile & Late-Night Cinema DSP
- 🌙 **Dialogue Voice Boost / Night Mode**: Android `LoudnessEnhancer` dynamic range compression brings whispered dialogue forward while smoothing loud explosions for comfortable late-night listening.
- 📶 **Bluetooth Wireless Audio Lip-Sync**: Calibrate wireless audio delay (`±500ms` in 50ms steps) from Settings or the Cinema Player quick menu.
- ⏱️ **Live Subtitle Sync Calibration**: Adjust subtitle timing on-the-fly (`±10s` in 100ms increments) for out-of-sync SRT or ASS tracks.
- 🎼 **Bundled NextLib FFmpeg Decoders**: Lossless decoding for **Dolby Atmos**, **Dolby Digital (AC-3)**, **Dolby Digital Plus (E-AC-3)**, **TrueHD**, **DTS-HD**, **FLAC**, and **AAC**.
- 🔒 **Screen-Off Background Audio**: Continue listening to concerts, commentary, and podcasts with your device locked.

---

### 🎮 5. Premium Player Gestures & Delights
- 💫 **YouTube-Style Seek Ripples**: Double-tap left or right to seek with expanding frosted arc ripples, dynamic chevrons (`«««` / `»»»`), seek accumulator, and tactile haptic ticks.
- 🎚️ **Dual-Side Edge Swipes**: Vertical edge swipes with deadzone touch protection — left side controls screen brightness; right side controls volume.
- ⏩ **Long-Press 2x Speed**: Press and hold anywhere on the player surface to temporarily accelerate playback to 2.0x.
- ⏭️ **Contextual "Skip Intro (+85s)" Capsule**: One-tap intro skipper in the opening minutes of episodes and movies.
- 🖼️ **Hardware Picture-in-Picture (PiP)**: Smooth background playback with source aspect ratio matching.

---

### 📺 6. Smart TV Series & Episode Management
- 🏷️ **Automated Series & Seasons Grouping**: Automatically identifies episode patterns (`S01E01`, `1x04`, `Episode 5`) into organized series hubs and season carousels.
- ⏳ **Next-Episode Autoplay Countdown**: 10-second autoplay countdown banner with instant "Play Now" and "Cancel" buttons.
- 🎬 **Dedicated Movies Tab**: Isolates standalone films with high-res poster grids and rich details (`MovieDetailsPage.kt`).
- 🚀 **First-Run Welcome Onboarding**: Simple Storage Access Framework (SAF) folder picker card guiding new users immediately.

---

### 📁 7. Integrated Storage & Folder Management
- ⚙️ **Direct Settings Integration**: Folder and disk management lives under **Settings → Storage & Folders**.
- 📊 **Storage Summary Card**: Live internal storage breakdown bar alongside indexed media byte metrics.
- 🛡️ **Active Directory Manager**: Add media folders with protected directory removal dialogs.
- 🔄 **Quick Maintenance**: One-tap **Rescan Folders** and **Clear Thumbnail Cache** buttons.

---

### 🎨 8. Clean Material 3 Display Modes & Aurora Splash
- 🌌 **Cinematic Dark Aurora Splash**: Deep `#09101d` palette, floating sapphire/cyan blurred energy blobs, frosted glass logo, and glowing loading track.
- ☀️ **Light Mode**: Airy Pale-Blue aesthetic for bright daytime viewing.
- 🌙 **Dark Mode**: Deep Night Slate styling for cinema immersion.
- 🖤 **AMOLED Mode**: Pure Black background for maximum OLED battery savings and infinite contrast.
- 📐 **Corner Curvature Presets**: Choose between **Rounded** (24dp curve), **Compact** (14dp curve), or **Sharp** (0dp crisp).
- 🌺 **Theme Transition Blend**: Smooth fade in / fade out animation when switching appearance themes.
- 🎬 **Lively UI Animations**: Fluid slide & spring transitions when navigating between folders, tabs, and detail pages.

---

### 🔒 9. 100% Offline-First Privacy Guarantee
- 📴 **Local Sidecar Scraping**: Reads offline `poster.jpg`, `fanart.jpg`, `cover.png`, and Jellyfin/Kodi `.nfo` metadata.
- 🚫 **Zero External Tracking**: No telemetry, no ads, no trackers, and no background network calls.
- 💾 **Strictly On-Device**: Playback history, resume markers, and subtitle preferences stay exclusively on your device.

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
