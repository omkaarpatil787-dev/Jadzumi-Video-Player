# 🎬 Jadzumi Player — Modern Cinema & TV Player for Android

> **Version 1.2.0 (Release)** · 📱 Universal Android: Phones, Foldables & Tablets · 📴 Offline-First with Smart Artwork · 🛡️ Zero Tracking · ⚡ Jetpack Compose & NextLib Media3 Engine

---

## 🌟 Overview

**Jadzumi Player** is a high-performance, private, modern cinema and TV video player engineered for all Android devices. Built with **Jetpack Compose** and **NextLib Media3 (ExoPlayer + bundled FFmpeg C++ decoders)**, Jadzumi delivers fluid gesture controls, intelligent hardware & display detection, audiophile audio DSP, online album artwork scraping with local disk caching, and an adaptive glassmorphic interface that scales seamlessly from handheld phones to foldables and large-screen tablets.

---

## ✨ Key Features & Highlights

### 📱 1. Universal Phone & Tablet Ergonomics
- 🫧 **Snapchat-Style Pure Dock & Tablet Rail**: Ultra-clean, alive navigation icons with natural spring scaling (`1.15x`) and vibrant primary tints. No intrusive bounding shapes or squished glow capsules when auto-minimizing on tablets or phones!
- 🎛️ **Dock Style Toggle**: Choose between **Icons & labels** (default) or **Icons only** (ultra-sleek 54dp dock / 64dp tablet rail) under Settings › Display.
- 📐 **Balanced Mobile Portrait Cinema Player**: Symmetrically centered timecode capsule pill and player actions dock in portrait orientation. Streamlined top bar keeps focus on immersion with subtitles directly accessible via Cinema Settings.
- 🎬 **Unified Design Parity (Home, Movies & TV Shows)**: Pixel-perfect parity across hero cards, genre subtitles, click-to-expand synopses, rating stars, and single-surface styling without elevation tinting or two-tone borders.
- 🏷️ **FlowRow Audio & Subtitle Chips**: Modern badge chips for Multi-channel 5.1 passthrough, FFmpeg 6.1 libavcodec, libass, and PGS/SRT formats that never clip or wrap awkwardly on portrait phones.
- 🔄 **One-Tap Orientation Switcher**: In-player button instantly cycles between **Landscape**, **Portrait**, and **Auto-Sensor** rotation.
- ✨ **Shared-Axis Premium Tab Transitions**: Replaces fast flickering crossfades with directional horizontal parallax (`slideIn/Out` + overlapping fade) for a continuous, silky-smooth 120 FPS navigation feel.

---

### ☀️ 2. OLED / AMOLED Exclusive Auto HDR Peak Luminance
- 🌈 **Auto HDR Detection**: Real-time detection of **HDR10**, **HDR10+**, **Dolby Vision** (`dv`/`dovi`), and **HLG** content.
- 🖤 **OLED-Exclusive Peak Luminance**: Automatically boosts display luminance to peak (`1.0f`) with `COLOR_MODE_HDR` **only on OLED / AMOLED displays** for piercing highlights with infinite true blacks.
- 🛡️ **LCD Backlight Protection**: Bypasses auto peak luminance on LCD tablets (Xiaomi Pad 5/6, OnePlus Pad, Galaxy Tab S7/S8 base, Lenovo Tab) to eliminate backlight bleeding and preserve natural contrast.
- 🔍 **Hardware & Panel Inspector**: Live inspection of display panel technology, SoC chipset model, and screen aspect ratios (`19.5:9`, `20:9`, `21:9` for phones; `16:10` / `4:3` for tablets).

---

### 🖼️ 3. Online Poster Album & Rich Metadata Scraping (Movies & TV Series)
- 🌐 **Dual-Engine Online Metadata Scraper**: When local sidecars are missing, Jadzumi fetches official high-resolution posters, official plot synopses, critic ratings, and release years via **Wikipedia REST** and **TVMaze** for both feature films and TV shows with automatic fallback.
- 💾 **Local Disk Caching**: Downloaded posters and metadata are cached locally on disk (`cacheDir/poster_cache/`) so they load instantly and remain available 100% offline with verified `☁ Online Art` status pills.
- 🎯 **Accurate Genre Detection**: Intelligently categorizes movies and shows into Horror, Sci-Fi, Action, Animation, Crime, Thriller, and Comedy.
- 🎞️ **Anti-Black Frame Fallback**: Samples local video frames at 12% runtime (`10s`–`180s`) to ensure local thumbnails never display black intros.

---

### 🎛️ 4. Glassify UI Elements Intensity Slider
- 🧪 **Adjustable Glass Translucency**: Interactive slider in **Settings › Display** adjusting frosted glass intensity from **0% (Solid)** to **100% (Deep Glass)**.
- 💎 **Universal Live UI Engine (`GlassTheme`)**: Dynamically modulates background opacity and specular frosted rims across the floating dock, tablet rail, search capsules, settings panels, and CinemaPlayer overlay pills and controls.
- 🌓 **Full Tri-Theme Fidelity**: Calibrated for **Light** (frosted white crystal), **Dark** (frosted obsidian navy), and **AMOLED** (pure pitch black scaling into sapphire dark glass).

---

### 🌌 5. Final Aurora Splash Screen
- 🎨 **HTML-Spec Accurate Aurora**: Ethereal liquid aurora based on `jadzumi_player_splash_final.html`, blending Electric Cyan (`#64DCFF`), Sapphire Azure (`#188AF5`), and Royal Blue (`#1659D9`).
- 📱 **Adaptive Screen Sizing**: Dynamically scales glass tile and logo dimensions across compact phones (<680dp), standard phones, and large-screen tablets.
- 🌓 **Adaptive Display Palettes**:
  - 🌙 **Dark Mode**: Deep obsidian navy (`#091628`) with ambient radial ellipses and screen-blend floating blobs.
  - 🖤 **AMOLED Mode**: Pure `#000000` pitch black background for zero OLED battery consumption with a soft ambient sapphire aura.
  - ☀️ **Light Mode**: Luminous morning sky gradient (`#EAF4FF`) with airy azure floating blobs and diagonal specular sheen.
- ✨ **Levitating Glass Tile & SVG Logo**: Levitating frosted tile (`translateY(-6px)`) with breathing radial aura, 120x120 vector play symbol, and 3dp gradient loading track with dynamic status text (*"Preparing your cinema…"* ➔ *"Loading your library…"*) and 1.09x smooth zoom exit.

---

### 🎮 6. Studio Player Gestures & Seek Controls
- 💫 **YouTube-Style 54dp Seek Ripples**: Proportioned double-tap seek ripple overlay (`54dp` matching seek control buttons) with dynamic chevrons (`«««` / `»»»`), seek accumulator, and smooth circular clipping.
- 🎚️ **Dual-Side Edge Swipes**: Left side controls screen brightness; right side controls audio volume with deadzone touch protection.
- ⏩ **Long-Press 2x Speed**: Press and hold anywhere on the player surface to temporarily accelerate playback to 2.0x.
- ⏭️ **Contextual "Skip Intro (+85s)" Capsule**: One-tap intro skipper in the opening minutes of episodes and movies.
- 🖼️ **Hardware Picture-in-Picture (PiP)**: Smooth background playback with source aspect ratio matching.

---

### 🔊 7. Audiophile Cinema DSP & NextLib FFmpeg
- 🌙 **Dialogue Voice Boost / Night Mode**: Speech clarity enhancement via Android `LoudnessEnhancer` dynamic range compression.
- 📶 **Bluetooth Lip-Sync Calibration**: Wireless audio delay offset calibration (`±500ms` in 50ms steps).
- ⏱️ **Live Subtitle Sync Calibration**: On-the-fly subtitle timing adjustments (`±10s` in 100ms increments).
- 🎼 **Bundled NextLib FFmpeg Decoders**: Lossless decoding for **Dolby Atmos**, **Dolby Digital (AC-3)**, **Dolby Digital Plus (E-AC-3)**, **TrueHD**, **DTS-HD**, **FLAC**, and **AAC**.
- 🔒 **Screen-Off Background Audio**: Continue listening to concerts, commentary, and podcasts with your device locked.

---

### 📁 8. Smart Library, Search & Folder Management
- 🔍 **Tokenized Smart Search Engine**: Multi-word normalized search matcher across movies, TV series, episodes, and folder paths. Handles dots, underscores, hyphens, and brackets (`"the dark knight"`, `"batman 2022"`, `"spider man"`), with automatic detail view reset, keyboard IME actions, and dedicated categorized search result sections.
- 🏷️ **Automated Series & Seasons Grouping**: Automatically groups episode patterns (`S01E01`, `1x04`) into organized series hubs and season carousels.
- ⏳ **Next-Episode Autoplay Countdown**: 10-second autoplay countdown banner with instant "Play Now" and "Cancel" buttons.
- ⚙️ **Settings-Integrated Storage Hub**: Comprehensive folder management under **Settings → Storage & Folders** featuring storage breakdown bars, active directory lists, and deletion protection dialogs.

---

## 🛠️ Tech Stack

| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.0 (JVM 17 Target) |
| **UI Toolkit** | Jetpack Compose (Material 3 Expressive Design) |
| **Media Engine** | AndroidX Media3 (ExoPlayer) + NextLib FFmpeg JNI decoders |
| **Image Loading** | High-performance sampled bitmap pipeline + local disk LRU cache |
| **Metadata** | 100% Offline sidecars (`.nfo`, `poster.jpg`) + Wikipedia REST & TVMaze |
| **Build System** | Gradle 8.11.1 + Android Gradle Plugin 8.7.2 |
| **Optimization** | R8 code shrinking + resource optimization with ProGuard rules |

---

## 📦 Building from Source

```bash
# 1. Clone the repository
git clone https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player.git
cd Jadzumi-Video-Player

# 2. Build Debug APK
./gradlew assembleDebug

# 3. Build Production Release APK
./gradlew assembleRelease
```

---

## 📄 License

Licensed under the Apache License, Version 2.0.
