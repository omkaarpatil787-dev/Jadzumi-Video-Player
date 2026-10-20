# 📑 Jadzumi Video — Project Resume & Development Checkpoint

> **Status:** Production Ready · **Version:** 1.2.0 (Release) · **Target:** Universal Android Devices (Phones, Foldables, Tablets) · **Updated:** 2026-10-20

---

## 🎯 Executive Summary

**Jadzumi Video** is a modern, privacy-first cinema and TV media player engineered for Android. Built from the ground up with **Kotlin**, **Jetpack Compose**, and **NextLib Media3 (ExoPlayer + FFmpeg JNI C++ decoders)**, the application delivers an offline-first cinema experience with adaptive ergonomics, hardware inspection, audiophile DSP enhancements, and fluid gesture navigation.

---

## 🏆 Milestone Accomplishment Matrix

| Milestone | Scope & Deliverables | Status |
| :--- | :--- | :---: |
| **Milestone 1 — Tablet & Large-Screen UI** | Responsive wide-screen layouts, vertical Navigation Rail (AppRail), frosted glassmorphism | `COMPLETED` |
| **Milestone 2 — Media Indexing Engine** | Scoped Storage / SAF integration, asynchronous indexing, SQLite room persistence | `COMPLETED` |
| **Milestone 3A — Cinema Playback Core** | Hardware `SurfaceView`, multi-ratio scaling (Fit, Fill, Zoom), variable speed (0.5x–2.0x) | `COMPLETED` |
| **Milestone 3B — Video Cache & History** | LRU bitmap thumbnail caching, resume playback timestamps, per-video preferences | `COMPLETED` |
| **Milestone 3C — Storage Management** | In-app disk inspection, active directories manager, single-video deletion workflows | `COMPLETED` |
| **Milestone 4 — Privacy & Unified Settings** | Local-first guarantee, telemetry zeroing, 3 display modes (Light, Dark, AMOLED) | `COMPLETED` |
| **Milestone 5A & 5B — TV Continuity & Sidecars** | Season/Episode auto-grouping, 10s autoplay countdown, offline `.nfo` & poster scraper | `COMPLETED` |
| **Milestone 7 — 120Hz Polish & Audit** | High refresh rate negotiation (120Hz/144Hz), Picture-in-Picture (PiP), multi-window | `COMPLETED` |
| **Milestone 8 — Production Hardening (v1.0.0)** | R8 code shrinking, resource shrinking, FFmpeg ProGuard rules, reproducible signing | `COMPLETED` |
| **Milestone 9 — Phone Ergonomics (v1.1.0)** | Floating frosted glass bottom bar, adaptive 2-row mobile headers, in-player rotation | `COMPLETED` |
| **Milestone 10 — Hardware & DSP Overhaul (v1.2.0)** | Dynamic hardware inspector, Audiophile DSP, seek ripples, unified storage in settings | `COMPLETED` |
| **Milestone 11 — Mobile Refinements & HDR (v1.2.0+)** | Centered portrait player pills, auto-minimizing dock, movie page redesign, auto HDR peak brightness, non-black thumbnails | `COMPLETED` |

---

## 🚀 Key Features in Current Version (v1.2.0)

### 📱 1. Universal Phone & Tablet Ergonomics
- 🫧 **Auto-Minimizing Floating Dock**: Bouncy spring animation (`Spring.DampingRatioMediumBouncy`) smoothly collapses the bottom navigation into a sleek 52dp floating capsule while scrolling movies and TV shows; tap to expand.
- 🎛️ **Bottom Dock Style Toggle**: Select between **Icons and labels** (default) or **Icons only** (ultra-sleek 54dp bar) in Settings › Display.
- 📐 **Centered Mobile Portrait Player Pills**: Balanced, centered layout for both the glassy timecode pill and player actions dock on phone portrait screens.
- 🎬 **Mobile-Optimized Movie Details**: Full-width title header (no bracket truncation), genre subtitle, side-by-side 2:3 poster with responsive metadata pills (`MovieBadgesFlow`), expandable synopsis, and symmetrical 44dp pill action buttons.
- 🔄 **In-Player Orientation Switcher**: One-tap rotation between **Landscape**, **Portrait**, and **Auto-Sensor**.

### ☀️ 2. Automatic HDR Peak Brightness & Display Probing
- 🌈 **Auto HDR Detection**: Real-time detection of **HDR10**, **HDR10+**, **Dolby Vision** (`dv`/`dovi`), and **HLG** content.
- 💡 **Peak Luminance Boost**: Automatically raises screen brightness to peak (`1.0f`) with `COLOR_MODE_HDR`, restoring original brightness on exit.
- 🔍 **Dynamic SoC & Hardware Inspection**: Detects processor chipset and device model (`Build.SOC_MODEL` / `Build.HARDWARE` / `Build.BOARD`) with native FFmpeg fallback.
- 📏 **Universal Aspect Ratio Metric**: Real-time calculation of screen ratios (`19.5:9`, `20:9`, `21:9` for phones; `16:10` / `4:3` for tablets) alongside resolution.

### 🖼️ 3. Studio-Grade Poster Generation & Intelligent Metadata
- 🎞️ **Anti-Black Frame Sampling**: Samples frames at 12% into the film (`10s`–`180s`) with Android 10+ `loadThumbnail` support to eliminate black lead-in thumbnails.
- 🎨 **Cinematic Fallback Artwork**: Genre-matched color gradients (crimson for Horror, deep space cyan for Sci-Fi, navy for Action, royal purple for Animation).
- 🧠 **Intelligent Genre Detector**: Detects Horror, Sci-Fi, Action, Animation, Comedy, and Crime keywords automatically.
- 📖 **Authentic & Dynamic Synopses**: Authentic summaries for major franchises (*Avengers, Interstellar, The Dark Knight, Dune, The Conjuring, etc.*) plus smart contextual descriptions for uncataloged films.

### 🔊 4. Audiophile & Late-Night Cinema DSP
- 🌙 **Dialogue Voice Boost / Night Mode**: Speech clarity enhancement via Android `LoudnessEnhancer` dynamic range compression.
- 📶 **Bluetooth Lip-Sync Calibration**: Wireless audio delay offset calibration (`±500ms` in 50ms increments).
- ⏱️ **Live Subtitle Sync Calibration**: On-the-fly subtitle timing adjustments (`±10s` in 100ms increments).
- 🎼 **Bundled NextLib FFmpeg Decoders**: Lossless decoding for **Dolby Atmos**, **E-AC-3**, **AC-3**, **TrueHD**, **DTS-HD**, **FLAC**, and **AAC**.
- 🔒 **Screen-Off Background Playback**: Seamless background audio when screen is turned off or device is locked.

### 🎮 5. Premium Player Gestures & Delights
- 💫 **YouTube-Style Seek Ripples**: Expanding frosted arc ripples with dynamic chevron arrows (`«««` / `»»»`) and tactile haptics.
- 🎚️ **Dual-Side Edge Swiping**: Left-side vertical brightness control and right-side volume control with deadzone touch protection.
- ⏩ **Long-Press 2x Speed**: Press and hold anywhere on the player surface to temporarily accelerate playback to 2.0x.
- ⏭️ **Contextual "Skip Intro (+85s)" Capsule**: One-tap intro skipper in the opening minutes of episodes and movies.

### 📁 6. Streamlined Navigation & Folder Management
- 🧭 **4 Core Destinations**: Simplified navigation across **Home**, **Movies**, **TV Shows**, and **Settings**.
- ⚙️ **Settings-Integrated Storage Hub**: Comprehensive folder management under **Settings → Storage & Folders** featuring storage breakdown bars, active directory lists, and deletion protection dialogs.
- 🗂️ **Clean Media Grids**: Removed redundant category filter cards on Movies/TV pages for immediate media immersion.

### 🎨 7. Clean Material 3 Display Modes & Aurora Splash
- 🌌 **Cinematic Dark Aurora Splash**: Deep `#09101d` palette, floating sapphire/cyan blurred energy blobs, frosted glass logo, and glowing loading track.
- ☀️ **Light Mode**: Airy Pale-Blue aesthetic for bright daytime environments.
- 🌙 **Dark Mode**: Deep Night Slate styling for cinema immersion.
- 🖤 **AMOLED Mode**: Pure Black background for maximum OLED battery savings and infinite contrast.
- 📐 **Corner Curvature Presets**: Choose between **Rounded** (24dp curve), **Compact** (14dp curve), or **Sharp** (0dp crisp).
- 🌺 **Theme Transition Blend**: Smooth fade in / fade out blend when switching themes.
- 🎬 **Lively UI Animations**: Fluid slide & spring transitions between tabs, details, and folders.

### 🔒 8. 100% Offline-First Privacy Architecture
- 📴 **Local Sidecar Scraping**: Reads local `poster.jpg`, `fanart.jpg`, and `.nfo` files without network calls.
- 🚫 **Zero Telemetry**: No analytics, no ads, no trackers, and no unsolicited network calls.
- 💾 **Strictly On-Device**: Playback positions, resume markers, and audio/subtitle preferences are kept strictly on-device.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin 2.0 (JVM 17 Target)
- **UI Toolkit:** Jetpack Compose (Material 3 Expressive Design)
- **Media Engine:** AndroidX Media3 (ExoPlayer 1.4.x) + NextLib FFmpeg native JNI bindings
- **Storage:** Android Storage Access Framework (SAF) + DocumentFile + SQLite
- **Architecture Pattern:** Offline-First Unidirectional Data Flow (UDF) with Kotlin StateFlow & Coroutines
- **Build System:** Gradle 8.11.1 + Android Gradle Plugin 8.7.2
- **Compiler Optimization:** R8 Code Shrinking + Resource Optimization with ProGuard rules

---

## 📦 Binary & Build Specifications

| Property | Value |
| :--- | :--- |
| **Package ID** | `com.jadzumi.video` |
| **Version Name** | `1.2.0` |
| **Version Code** | `11` |
| **Min SDK** | API 26 (Android 8.0 Oreo) |
| **Target SDK** | API 35 (Android 15 Vanilla Ice Cream) |
| **ABI Support** | `arm64-v8a` |
| **Source Archive** | `Jadzumi-Video-Player-v1.2.0-source.zip` |

---

## 👤 Project Leadership

- **Creator & Product Direction:** Omkar Patil  
- **Repository:** `https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player`
