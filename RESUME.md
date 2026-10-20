# 📑 Jadzumi Video — Project Resume & Development Checkpoint

> **Status:** Production Ready · **Version:** 1.2.0 (Release) · **Target:** Universal Android Devices (Phones, Foldables, Tablets) · **Updated:** 2026-10-20

---

## 🎯 Executive Summary

**Jadzumi Video** is a modern, privacy-first cinema and TV media player engineered for Android. Built from the ground up with **Kotlin**, **Jetpack Compose**, and **NextLib Media3 (ExoPlayer + FFmpeg JNI C++ decoders)**, the application provides an offline-first cinema experience with adaptive ergonomics, hardware inspection, audiophile DSP enhancements, and fluid gesture navigation.

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

---

## 🚀 Key Features in Current Version (v1.2.0)

### ⚡ 1. Dynamic Hardware & Display Inspection
- **SoC & Processor Detection**: Dynamically inspects device model and processor chipset (`Build.SOC_MODEL` / `Build.HARDWARE` / `Build.BOARD`) via `DeviceHardwareInspector.kt`.
- **Universal Aspect Ratio Measurement**: Calculates real-time display metrics (`19.5:9`, `20:9`, `21:9` for phones, `16:10` / `4:3` for tablets) alongside resolution metrics.
- **Hardware HDR Probing**: Queries `Display.hdrCapabilities` for **HDR10**, **HDR10+**, **HLG**, and **Dolby Vision** rendering capabilities with SDR tone mapping.
- **Universal Audio Downmixing**: Multi-speaker downmixing optimized universally for device stereo speakers, earpieces, and headphones.

### 🔊 2. Audiophile & Late-Night Cinema DSP
- **Night Mode / Dialogue Voice Boost**: Real-time speech clarity enhancement via Android `LoudnessEnhancer` dynamic range compression.
- **Bluetooth Lip-Sync Calibration**: Fine-tune wireless audio synchronization offset (`±500ms` in 50ms increments).
- **In-Player Subtitle Delay Calibration**: Instant on-the-fly subtitle timing adjustments (`±10s` in 100ms increments).
- **Bundled NextLib FFmpeg Decoders**: High-fidelity playback for **Dolby Atmos**, **E-AC-3**, **AC-3**, **TrueHD**, **DTS-HD**, **FLAC**, and **AAC**.
- **Screen-Off Background Playback**: Seamless background audio when screen is turned off or device is locked.

### 🎮 3. Premium Player Gestures & Delights
- **YouTube-Style Double-Tap Seek Ripples**: Expanding frosted arc ripples with dynamic chevron arrows (`«««` / `»»»`) and tactile haptics.
- **Dual-Side Edge Swiping**: Left-side vertical brightness control and right-side volume control with deadzone touch protection.
- **Long-Press 2x Speed**: Press-and-hold player surface for temporary 2.0x playback speed acceleration.
- **Contextual "Skip Intro (+85s)" Capsule**: One-tap intro skipper in the opening minutes of episodes and movies.
- **Adaptive 2-Row Cinema Dock**: Segregated action dock on phone portrait layouts to eliminate button crowding.

### 📁 4. Streamlined Navigation & Folder Management
- **4 Main Destinations**: Simplified bottom navigation bar and AppRail to **Home**, **Movies**, **TV Shows**, and **Settings**.
- **Settings-Integrated Storage Hub**: Comprehensive folder management relocated into **Settings → Storage & Folders** featuring storage breakdown bars, active folder lists, and directory removal confirmation.
- **Clean Full-Width Content**: Removed redundant category filter cards on Movies/TV pages for immediate media immersion.

### 🎨 5. Clean Material 3 Display Modes
- **3 Display Modes**: **Light** (Airy Pale-Blue), **Dark** (Night Slate), and **AMOLED** (Pure Black).
- **Corner Curvature Presets**: Rounded (24dp curve), Compact (14dp curve), Sharp (0dp crisp).
- **Theme Bloom Transition**: Smooth fade in / fade out animations during appearance transitions.

### 🔒 6. 100% Offline-First Privacy Architecture
- **Offline Sidecar Scraper**: Reads local `poster.jpg`, `fanart.jpg`, and `.nfo` files without network calls.
- **Zero Telemetry**: No analytics, no ads, no trackers, and no unsolicited network calls.
- **Local Persistence**: Playback positions, resume markers, and audio/subtitle preferences are kept strictly on-device.

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
