# Jadzumi Video Player — Project Resume & Architecture Summary

> | Universal Android (Phones, Foldables & Tablets) | Offline-First Cinema Player | Zero Tracking | Jetpack Compose & Media3

---

## Executive Summary

**Jadzumi Video Player** is a modern, privacy-first multimedia player engineered for Android. Built using **Kotlin**, **Jetpack Compose**, and **NextLib Media3 (ExoPlayer + bundled FFmpeg JNI C++ decoders)**, Jadzumi delivers an uncompromised cinema viewing experience across form factors. Highlights include universal phone and tablet ergonomics, OLED-exclusive HDR peak luminance calibration, intelligent end-credit detection, tactile haptic scrubbing, multi-column poster browsing, and audiophile-grade audio DSP.

---

## Milestone Evolution Matrix

| Milestone | Deliverables & Scope | Status |
| :--- | :--- | :---: |
| **Milestone 1 — Responsive Core** | Adaptive wide-screen layouts, responsive navigation, glassmorphic UI | `COMPLETED` |
| **Milestone 2 — Media Indexing** | Scoped Storage / SAF integration, asynchronous library scanner, disk cache | `COMPLETED` |
| **Milestone 3 — Playback Engine** | Hardware SurfaceView, multi-ratio scaling, variable speeds (0.5x–2.0x) | `COMPLETED` |
| **Milestone 4 — Privacy & Audio DSP** | Zero telemetry, night mode voice clarifier, Bluetooth delay compensation | `COMPLETED` |
| **Milestone 5 — TV Continuity** | Season/Episode grouping, autoplay continuity, offline metadata sidecars | `COMPLETED` |
| **Milestone 6 — Production Hardening** | R8 code shrinking, resource stripping, ProGuard rules, reproducible signing | `COMPLETED` |
| **Milestone 7 — Hardware & Display** | OLED-exclusive HDR peak luminance, LCD backlight protection, hardware inspector | `COMPLETED` |
| **Milestone 8 — Mobile Ergonomics** | Floating frosted dock, Gemini-style glowing brand emblem, alive greeting | `COMPLETED` |
| **Milestone 9 — CineFlow Fluidity** | Natural 340ms cubic bezier navigation transitions, frosted glass depth, blend-fade scrolling | `COMPLETED` |
| **Milestone 10 — Flagship 1.3 Overhaul** | Hold-to-2x speed, smart end-credit autoplay countdown, chapter tick marks & snapping, external subtitle loader, poster grid toggle, smart filter chips, batch multi-select | `COMPLETED` |
| **Milestone 11 — Polishing & Fluid Exit (v1.3.1)** | Subtle 0.75dp specular card borders, decluttered header with grid/list toggle, fluid cubic-bezier exit transitions | `COMPLETED` |
| **Milestone 12 — Mobile Dock & Ambient Polish (v1.3.2)** | Universal mobile dock alignment (equal destination weighting), stutter-free card opening transitions, dynamic System default ambient theme matching for frosted glass surfaces, and portrait-optimized Glassify controls | `COMPLETED` |

---

## Architecture & Technical Highlights (v1.3.2)

### 1. In-Player Gesture Ergonomics & Fluid Transitions
- **Symmetrical Fluid Opening & Exit Transitions**: CinemaPlayer scales gracefully (0.93 ↔ 1.0) with cubic-bezier easing (`(0.4, 0.0, 1.0, 1.0)`) on exit and (`(0.16, 1.0, 0.3, 1.0)`) on entry across back gesture, top-bar back, close buttons, and quick menu.
- **Stutter-Free Movie & TV Details Navigation**: Eliminates outer top-bar height jumping on phones when inspecting cards, paired with hardware-accelerated 260ms Material decelerate curves and zero thumbnail grid scale thrash.
- **Hold-to-2x Fast-Forward**: Press-and-hold anywhere on the screen temporarily accelerates playback to 2.0x with haptic feedback; releasing returns to the previous speed.
- **Haptic Scrubbing & Chapter Snapping**: Real-time chapter marker extraction and visualization with tactile snapping within a 1.4% seek threshold.
- **Intelligent End-Credit Detection**: Automatically calculates the credit lead-in boundary based on media runtime, triggering a smooth 12-second countdown with an unobtrusive "Watch Credits" option.
- **HUD Voice Clarity Access**: Direct one-tap toggle for dialogue enhancement via `LoudnessEnhancer`.
- **External Subtitle Loader**: Storage Access Framework picker allowing external `.srt`, `.vtt`, or `.ass` file attachment during playback.

### 2. Adaptive Library & Dock Ergonomics
- **Universal Mobile Dock Architecture**: Floating bottom bar with equal destination distribution (`Modifier.weight(1f)`) ensuring Home, Movies, TV Shows, and Settings remain balanced, properly centered, and visible across all mobile aspect ratios.
- **Streamlined Header & Grid/List Toggle**: Unified section header with a dedicated grid/list view toggle and clean visual balance without redundant filter chips.
- **Poster Grid vs. Detailed List**: Responsive view mode adapting column density from 2 columns (phone portrait) to 3-4 columns (tablets/landscape).
- **Batch Multi-Select Operations**: Long-press selection mode enabling batch status toggling (*Mark Watched*, *Mark Unwatched*, *Clear History*).
- **Continue Watching Shelf**: Frosted horizontal shelf displaying in-progress thumbnails, remaining time, and progress bars.

### 3. Display Intelligence & Visual Aesthetics
- **Dynamic System Default Appearance**: Derives `effectiveTheme` in real time from `isSystemInDarkTheme()`, ensuring frosted search capsules, floating bottom docks, and cards render authentic light frosted surfaces on light devices and dark frosted surfaces on dark devices with zero color clashing.
- **10 Handcrafted Custom App Icons**: Native dynamic launcher icon switching with 10 distinct art profiles (Original, Neon Protocol, VHS Nostalgia, Anime Pop, Boo!, Zombumi, Pixel Quest, Chrome X, Cosmic Portal, and Funky Doodle) integrated into Settings → Appearance.
- **Portrait-Optimized Glassify Controls**: Adaptive single-line scale indicators (`Matte 20%`, `Balanced 65%`, `Frosted 100%`) for mobile portrait displays without text wrapping.
- **Subtle 0.75dp Specular Card Rim**: Delicate, refined hairline borders across cards and panels (`0.75.dp` with soft `0.12f` alpha in dark mode and `0.08f` in light mode), replacing heavy/bold outlines while maintaining crisp edge definition.
- **Ambient Blurred Backdrop Glow**: Samples video artwork into a luminous, theatrical background gradient on movie and show detail pages.
- **TV Season Progress Gauges**: Real-time season completion percentages and "Up Next" spotlight cards.
- **OLED / AMOLED Peak Luminance**: Calibrated HDR10, Dolby Vision, and HLG peak luminance engagement restricted to true OLED panels.

---

## Core Technology Stack

- **Language:** Kotlin 2.0 (JVM 17 Target)
- **UI Framework:** Jetpack Compose, Material 3 Expressive
- **Media Engine:** AndroidX Media3 (ExoPlayer 1.11.1) + NextLib Media3 FFmpeg 6.1
- **Storage & Security:** Android Storage Access Framework (SAF), zero network telemetry
- **Build Tooling:** Gradle 8.11.1, AGP 8.7.2, R8 full-mode shrinking
