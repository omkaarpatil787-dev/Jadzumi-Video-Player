# 📑 Jadzumi Player — Project Resume & Development Checkpoint

> **Status:** Production Ready · **Version:** 1.2.0 (Release) · **Target:** Universal Android Devices (Phones, Foldables, Tablets) · **Updated:** 2026-10-20

---

## 🎯 Executive Summary

**Jadzumi Player** is a modern, privacy-first cinema and TV media player engineered for Android. Built from the ground up with **Kotlin**, **Jetpack Compose**, and **NextLib Media3 (ExoPlayer + FFmpeg JNI C++ decoders)**, the application delivers a premium cinema experience with universal device ergonomics, OLED-exclusive HDR peak luminance, online poster and metadata scraping with local disk caching, audiophile DSP enhancements, and fluid gesture navigation.

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
| **Milestone 11 — Modern Refinements & Artwork (v1.2.0+)** | Jadzumi Player rebranding, OLED-exclusive HDR, online poster scraper & disk cache, fluid dock/rail minimize, glassify slider, glowing aurora splash, Glass Playback Motion v2 HUD, unified single-surface movie details | `COMPLETED` |

---

## 🚀 Key Features in Current Version (v1.2.0)

### 📱 1. Universal Phone & Tablet Ergonomics
- 🫧 **Auto-Minimizing Floating Dock & Tablet Rail**: Silky-smooth fluid physics (`0.88` damping ratio) smoothly collapse the bottom dock (phone) and side rail (tablet) into sleek compact pills during scrolling. Tap to expand instantly.
- 🎛️ **Dock Style Toggle**: Choose between **Icons & labels** (default) or **Icons only** (ultra-sleek 54dp dock / 64dp rail) under Settings › Display.
- 🎬 **Unified Single-Surface Movie Details Page**: Pure single-surface card layout matching TV show ergonomics without elevation tinting or two-tone boundaries. Side-by-side poster and info on all devices, with responsive synopsis and symmetrical action buttons.
- 🏷️ **FlowRow Audio & Subtitle Chips**: Modern badge chips for Multi-channel 5.1 passthrough, FFmpeg 6.1 libavcodec, libass, and PGS/SRT formats that never clip or wrap awkwardly on portrait phones.
- 🔄 **In-Player Orientation Switcher**: One-tap rotation between **Landscape**, **Portrait**, and **Auto-Sensor**.

### 🌊 2. Minimal & Immersive Glass Playback Motion v2 Cinema Controls
- 💎 **Universal Glass Motion v2 UX across All Buttons**: Back navigation, Caption/Subtitles, Cinema Settings, and bottom actions (Rotate, Lock, PiP, Close) all feature interactive spring physics (`scale` 0.88 on press), dynamic frosted glass backgrounds, and glowing specular frosted rims responsive to the Glassify slider.
- 🌈 **Minimal & Immersive Floating Scrubber**: Removed the enclosed border rectangle around the progress bar; the fluid cyan gradient timeline (`#359EFF` ➔ `#83E5FF`) and glowing cyan-rimmed circular thumb now float cleanly and edge-to-edge over the subtle cinematic scrim.
- 💫 **Symmetrical 58dp Seek & 78dp Morphing Play Controls**: 58dp frosted glass circular seek buttons (±10s) with numeric indicators and a central 78dp play/pause button featuring radiant gradient rims and fluid icon morphing.
- 🎬 **Distraction-Free Minimal Top Bar**: Completely removed the refresh button for an uncluttered, pure cinema view with clean title and `"JADZUMI PLAYER"` micro-badge tracking.
- ⚡ **YouTube-Style Seek Ripples**: Proportioned 58dp double-tap seek ripple overlays with dynamic chevrons (`«««` / `»»»`) and clean circular geometry eliminating hexagonal artifacts.

### ☀️ 2. OLED / AMOLED Exclusive Auto HDR Peak Luminance
- 🌈 **Auto HDR Detection**: Real-time detection of **HDR10**, **HDR10+**, **Dolby Vision** (`dv`/`dovi`), and **HLG** content.
- 🖤 **OLED-Exclusive Peak Luminance**: Automatically engages maximum brightness (`1.0f`) with `COLOR_MODE_HDR` **only on true OLED / AMOLED displays** for piercing highlights and infinite black levels.
- 🛡️ **LCD Backlight Bleed Protection**: Tone-maps naturally on LCD tablets (Xiaomi Pad 5/6, OnePlus Pad, Tab S7/S8 base, Lenovo Tab) without forcing maximum backlight, preventing contrast washout and eye strain.
- 🔍 **Hardware Display & SoC Inspector**: Real-time inspection of display panel technology, chipset model, and screen aspect ratios (`19.5:9`, `20:9`, `21:9` on phones; `16:10` / `4:3` on tablets).

### 🖼️ 3. Online Poster Album & Rich Metadata Scraping
- 🌐 **Online Artwork & Metadata Scraper**: When local sidecars are missing, Jadzumi fetches official high-resolution posters, official plot synopses, critic ratings, and release years via Wikipedia REST and TVMaze.
- 💾 **Local Disk Caching**: Downloaded posters and metadata are cached locally on disk (`cacheDir/poster_cache/`) so they load instantly and remain available 100% offline.
- 🎯 **Accurate Genre Detection**: Intelligently categorizes movies and shows into Horror, Sci-Fi, Action, Animation, Crime, Thriller, and Comedy.
- 🎞️ **Anti-Black Frame Fallback**: Samples local video frames at 12% runtime (`10s`–`180s`) to ensure local thumbnails never display black intros.

### 🎛️ 4. Glassify UI Elements Intensity Slider
- 🧪 **Adjustable Glass Translucency**: Interactive slider in **Settings › Display** adjusting frosted glass intensity from **0% (Solid)** to **100% (Deep Glass)**.
- 💎 **Universal Live UI Engine (`GlassTheme`)**: Dynamically modulates background opacity and specular frosted rims across the floating dock, tablet rail, search capsules, settings panels, and CinemaPlayer overlay pills and controls.
- 🌓 **Full Tri-Theme Fidelity**: Calibrated for **Light** (frosted white crystal), **Dark** (frosted obsidian navy), and **AMOLED** (pure pitch black scaling into sapphire dark glass).

### 🌌 5. Final Aurora Splash Screen
- 🎨 **HTML-Spec Accurate Aurora**: Ethereal liquid aurora based on `jadzumi_player_splash_final.html`, blending Electric Cyan (`#64DCFF`), Sapphire Azure (`#188AF5`), and Royal Blue (`#1659D9`).
- 📱 **Adaptive Screen Sizing**: Dynamically scales glass tile and logo dimensions across compact phones (<680dp), standard phones, and large-screen tablets.
- 🌓 **Adaptive Display Palettes**:
  - 🌙 **Dark Mode**: Deep obsidian navy (`#091628`) with ambient radial ellipses and screen-blend floating blobs.
  - 🖤 **AMOLED Mode**: Pure `#000000` pitch black background for zero OLED battery consumption with a soft ambient sapphire aura.
  - ☀️ **Light Mode**: Luminous morning sky gradient (`#EAF4FF`) with airy azure floating blobs and diagonal specular sheen.
- ✨ **Levitating Glass Tile & SVG Logo**: Levitating frosted tile (`translateY(-6px)`) with breathing radial aura, 120x120 vector play symbol, and 3dp gradient loading track with dynamic status text (*"Preparing your cinema…"* ➔ *"Loading your library…"*) and 1.09x smooth zoom exit.

### 📱 6. Mobile Portrait TV Show & Episode Card Ergonomics
- 🏷️ **Adaptive Audio & Subtitle Badges**: FlowRow layout preventing horizontal compression or single-letter vertical stretching on narrow phone displays.
- 🎞️ **Responsive Episode Cards**: Responsive thumbnail dimensions (`116dp` on narrow phones vs `156dp` on tablets) with wrapped specs and status text for clean readability.
- ⚡ **Zero-Flash Dock Selection**: Fluid single-action tap selection with unified bounded ripple, smooth indicator fade/scale animations, and instant 120 FPS tab crossfades.

### 🎮 6. Studio Player Gestures & Seek Controls
- 💫 **YouTube-Style 54dp Seek Ripples**: Proportioned double-tap seek ripple overlay (`54dp` matching seek control buttons) with dynamic chevrons (`«««` / `»»»`), seek accumulator, and smooth circular clipping.
- 🎚️ **Dual-Side Edge Swipes**: Left side controls screen brightness; right side controls audio volume with deadzone touch protection.
- ⏩ **Long-Press 2x Speed**: Press and hold anywhere on the player surface to temporarily accelerate playback to 2.0x.
- ⏭️ **Contextual "Skip Intro (+85s)" Capsule**: One-tap intro skipper in the opening minutes of episodes and movies.
- 🖼️ **Hardware Picture-in-Picture (PiP)**: Smooth background playback with source aspect ratio matching.

### 🔊 7. Audiophile Cinema DSP & NextLib FFmpeg
- 🌙 **Dialogue Voice Boost / Night Mode**: Speech clarity enhancement via Android `LoudnessEnhancer` dynamic range compression.
- 📶 **Bluetooth Lip-Sync Calibration**: Wireless audio delay offset calibration (`±500ms` in 50ms steps).
- ⏱️ **Live Subtitle Sync Calibration**: On-the-fly subtitle timing adjustments (`±10s` in 100ms increments).
- 🎼 **Bundled NextLib FFmpeg Decoders**: Lossless decoding for **Dolby Atmos**, **Dolby Digital (AC-3)**, **Dolby Digital Plus (E-AC-3)**, **TrueHD**, **DTS-HD**, **FLAC**, and **AAC**.
- 🔒 **Screen-Off Background Audio**: Continue listening to concerts, commentary, and podcasts with your device locked.

### 📁 8. Smart Library, Search & Folder Management
- 🔍 **Tokenized Smart Search Engine**: Multi-word normalized search matcher across movies, TV series, episodes, and folder paths. Handles dots, underscores, hyphens, and brackets (`"the dark knight"`, `"batman 2022"`, `"spider man"`), with automatic detail view reset, keyboard IME actions, and dedicated categorized search result sections.
- 🏷️ **Automated Series & Seasons Grouping**: Automatically groups episode patterns (`S01E01`, `1x04`) into organized series hubs and season carousels.
- ⏳ **Next-Episode Autoplay Countdown**: 10-second autoplay countdown banner with instant "Play Now" and "Cancel" buttons.
- ⚙️ **Settings-Integrated Storage Hub**: Comprehensive folder management under **Settings → Storage & Folders** featuring storage breakdown bars, active directory lists, and deletion protection dialogs.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin 2.0 (JVM 17 Target)
- **UI Toolkit:** Jetpack Compose (Material 3 Expressive Design)
- **Media Engine:** AndroidX Media3 (ExoPlayer) + NextLib FFmpeg native JNI bindings
- **Storage:** Android Storage Access Framework (SAF) + DocumentFile + SQLite
- **Architecture Pattern:** Offline-First Unidirectional Data Flow (UDF) with Kotlin StateFlow & Coroutines
- **Build System:** Gradle 8.11.1 + Android Gradle Plugin 8.7.2
- **Compiler Optimization:** R8 Code Shrinking + Resource Optimization with ProGuard rules

---

## 📦 Binary & Build Specifications

- **Package ID:** `com.jadzumi.video`
- **Application Label:** `Jadzumi Player`
- **Target SDK:** 35 (Android 15) · **Min SDK:** 26 (Android 8.0 Oreo)
- **ABI Architectures:** `arm64-v8a` (Primary Cinema Target)
- **Release Signing:** Production development keystore with reproducible credentials
