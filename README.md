# 🎬 Jadzumi Player — Modern Cinema & TV Player for Android

> **Version 1.2.0 (Release)** · 📱 Universal Android: Phones, Foldables & Tablets · 📴 Offline-First with Smart Artwork · 🛡️ Zero Tracking · ⚡ Jetpack Compose & NextLib Media3 Engine

---

## 🌟 Overview

**Jadzumi Player** is a high-performance, private, modern cinema and TV video player engineered for all Android devices. Built with **Jetpack Compose** and **NextLib Media3 (ExoPlayer + bundled FFmpeg C++ decoders)**, Jadzumi delivers fluid gesture controls, intelligent hardware & display detection, audiophile audio DSP, online album artwork scraping with local disk caching, and an adaptive glassmorphic interface that scales seamlessly from handheld phones to foldables and large-screen tablets.

---

## ✨ Key Features & Highlights

### 📱 1. Universal Phone & Tablet Ergonomics
- 🫧 **Auto-Minimizing Floating Dock & Tablet Rail**: Silky-smooth fluid physics (`0.88` damping ratio) automatically collapse the navigation bar into a sleek compact pill when scrolling through media. A single tap pops it right back open!
- 🎛️ **Dock Style Toggle**: Choose between **Icons & labels** (default) or **Icons only** (ultra-sleek 54dp dock / 64dp tablet rail) under Settings › Display.
- 📐 **Balanced Mobile Portrait Cinema Player**: Symmetrically centered timecode capsule pill and player actions dock in portrait orientation.
- 🎬 **Mobile-Optimized Movie Details**: Full-width title layout, genre subtitle, side-by-side poster and metadata pill flow (`MovieBadgesFlow`), expandable synopsis, and symmetrical 44dp pill buttons.
- 🔄 **One-Tap Orientation Switcher**: In-player button instantly cycles between **Landscape**, **Portrait**, and **Auto-Sensor** rotation.

---

### ☀️ 2. OLED / AMOLED Exclusive Auto HDR Peak Luminance
- 🌈 **Auto HDR Detection**: Real-time detection of **HDR10**, **HDR10+**, **Dolby Vision** (`dv`/`dovi`), and **HLG** content.
- 🖤 **OLED-Exclusive Peak Luminance**: Automatically boosts display luminance to peak (`1.0f`) with `COLOR_MODE_HDR` **only on OLED / AMOLED displays** for piercing highlights with infinite true blacks.
- 🛡️ **LCD Backlight Protection**: Bypasses auto peak luminance on LCD tablets (Xiaomi Pad 5/6, OnePlus Pad, Galaxy Tab S7/S8 base, Lenovo Tab) to eliminate backlight bleeding and preserve natural contrast.
- 🔍 **Hardware & Panel Inspector**: Live inspection of display panel technology, SoC chipset model, and screen aspect ratios (`19.5:9`, `20:9`, `21:9` for phones; `16:10` / `4:3` for tablets).

---

### 🖼️ 3. Online Poster Album & Rich Metadata Scraping
- 🌐 **Online Artwork & Metadata Scraper**: When local sidecars are missing, Jadzumi fetches official high-resolution posters, official plot synopses, critic ratings, and release years via Wikipedia REST and TVMaze.
- 💾 **Local Disk Caching**: Downloaded posters and metadata are cached locally on disk (`cacheDir/poster_cache/`) so they load instantly and remain available 100% offline.
- 🎯 **Accurate Genre Detection**: Intelligently categorizes movies and shows into Horror, Sci-Fi, Action, Animation, Crime, Thriller, and Comedy.
- 🎞️ **Anti-Black Frame Fallback**: Samples local video frames at 12% runtime (`10s`–`180s`) to ensure local thumbnails never display black intros.

---

### 🎛️ 4. Glassify UI Elements Intensity Slider
- 🧪 **Adjustable Glass Translucency**: Interactive slider in **Settings › Display** adjusting frosted glass intensity from **20% (Solid)** to **100% (Deep Glass)**.
- 💎 **Live UI Adaptation**: Dynamically modulates background opacity and specular frosted rims across the floating dock, sheets, and player seek ripples.

---

### 🌌 5. Adaptive Glowing Aurora Splash Screen
- 🎨 **App Icon Color Gradient**: Ethereal liquid aurora blending Electric Cyan (`#64DCFF`), Sapphire Azure (`#188AF5`), and Royal Blue (`#1659D9`).
- 🌓 **Adaptive Display Palettes**:
  - 🌙 **Dark Mode**: Deep obsidian navy with diffused aurora waves and soft ambient halo.
  - 🖤 **AMOLED Mode**: Pure `#000000` pitch black background for zero power consumption on OLED panels.
  - ☀️ **Light Mode**: Luminous morning sky gradient with airy azure lighting and dark ink typography.
- ✨ **Zero Harsh Artifacts**: Seamless multi-stop radial dissipation with zero circular edge cutoffs.

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

### 📁 8. Smart Library & Folder Management
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
