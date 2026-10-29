# Jadzumi v1.7.2 — Cinema Cards & Media Layout Refinement 🎬✨

A comprehensive UI polish update decluttering Movie and TV series cards, introducing instant "Up Next" playback chips, dual aspect ratio view modes (2:3 Portrait Posters & 16:9 Cinematic Backdrops), and buttery 120Hz shimmer loading.

### 🎬 Decluttered Movie & TV Show Cards
- **Unified Single Glass Capsule Pill**:
  - Combined resolution/codec (`4K`, `1080p`, `HD`, `HEVC`) and community rating (`★ 8.2`) into a single, compact frosted glass capsule on the top-right corner (`4K · ★ 8.2`).
  - Cleared the entire top-left artwork canvas for unobstructed poster views.
- **Streamlined Cinema Typography**:
  - Replaced noisy file size labels on browse cards with clean cinema metadata (`2024 · 2h 15m · Action`).
  - Added a gentle bottom gradient scrim behind card titles for high contrast and readability across bright poster art.
- **One-Tap Quick Play Button**:
  - Embedded a subtle circular floating play/resume button directly on the bottom-right of movie and TV show cards for immediate playback without having to navigate into details first.

### 📺 TV Series "Up Next" One-Tap Quick Launch
- **Interactive "Up Next" Chip**:
  - Automatically identifies the next unwatched (or in-progress) episode across seasons and displays an interactive pill (`Up Next: S01E03`).
  - Tapping the "Up Next" chip launches playback immediately with full season queue continuity.
  - Tapping the card body opens the full series details page.
- **Watched Progress & Status**:
  - Displays a clean episode completion badge (`8 of 10 watched` or `Completed · 12 eps` with green checkmark).

### 📐 Aspect Ratio Grid Toggle (2:3 Posters vs 16:9 Backdrops)
- Added an instant layout aspect ratio toggle in Movies and TV Shows headers:
  - **Portrait Poster View (2:3)**: Classic cinema posters (2 columns on phone, 3–6 on tablets) showcasing key art.
  - **Cinematic Backdrop View (16:9)**: Widescreen landscape stills with backdrop imagery.
  - Remembers your preferred aspect view in settings.

### ⚡ 120Hz Scrolling Polish & Shimmer Loading
- **Fluid Shimmer Skeleton Placeholders**:
  - Integrated animated shimmer gradient placeholders during asynchronous thumbnail and poster decoding, eliminating jarring pop-ins.
- **Recomposition Optimization**:
  - Memoized video and series list filtering with `remember` for sustained 120 FPS scrolling across tablets and phones.

---

# Jadzumi v1.7.1 — Pure Flat Modern Cards & Highlight-Free Surfaces 🎨✨

A targeted aesthetic refinement release eliminating unwanted bottom edge highlight/shadow lips and elevation lines across all card surfaces in the app.

### 🎨 Pure Flat Modern Card Architecture
- **Complete Elimination of Bottom Edge Lines & Highlights**:
  - Removed artificial elevation shadows (`shadowElevation` and `tonalElevation`) from all cards and panels across the app.
  - Fixes visible horizontal shadow lips, chalky borders, and bottom highlight lines on cards in Light, Dark, and AMOLED themes.
  - Delivers a 100% clean, modern flat aesthetic matching Apple Settings and Material 3 Expressive guidelines where card surfaces cleanly separate from the background naturally without artificial skeuomorphic lines.
- **Unified Clean Selection States**:
  - Inner preset selection cards (Corner Style Presets, Navigation Dock Style, Theme Options, App Icon Options) now render flat with zero borders when unselected, drawing a clean primary accent border only when selected.
  - Storage summary cards, Folder cards, Movie/TV overview cards, Technical Inspector cards, and Episode cards now render completely flat with zero corner emboss or bottom elevation lips.
- **Preserved Geometry & Layout**:
  - Zero changes to card padding, corner radii, content layouts, typography, or interaction performance.

---

# Jadzumi v1.7.0 — Portal Halo UI & Motion System 🌌🎬

A major aesthetic and motion architecture release introducing the **Portal Halo Visual System**, **Snappy Micro-Animated Playback Controls**, **Seamless Immersive Playback Telemetry**, **Unified Settings with Scroll Blur Fade**, and **Pure Borderless Floating Cards**.

### 🌌 Portal Halo Atmospheric Backdrop System
- **Curated Halo Palettes**:
  - **Glacier Blue (Default)**: Sky Blue (`#84BAFF`) and Lavender Rose (`#B39CE8`).
  - **Peach Dusk**: Warm Apricot (`#FFC49F`) and Orchid Peach (`#DF9AC4`).
  - **Violet Cinema**: Royal Violet (`#AC9AFF`) and Aqua Teal (`#76CDDD`).
- **Adaptive Appearance & AMOLED Optimization**:
  - **Light Mode**: Airy, soft translucent floating surfaces with luminous atmospheric tints.
  - **Dark Mode**: Deep night-slate background (`#0C1422`) with glowing cosmic auras behind interface chrome.
  - **AMOLED Mode**: Pitch-black (`#000000`) power-efficient background preservation with subtle, restrained ethereal auras.
- **Customizable Halo Controls in Display & Appearance**:
  - Enable/disable Portal Halos toggle.
  - Interactive Palette picker with dual-color preview badges.
  - Halo Glow Intensity slider (0% to 65%).
  - Halo Spread Radius slider (30% to 120%).
- **Unhurried Organic Breathing Drift**:
  - Subtle 10-second ease-in-out breathing float when motion physics are active, respecting reduced-motion preferences.
  - Zero-overhead GPU radial brush gradients with zero expensive full-screen Gaussian blurs.
  - 100% video-surface isolation: videos remain completely crystal-clear and unadulterated by ambient washes.

### ⏱️ Immersive Playback Telemetry & HUD Overlays
- **Persistent Top-Right Status Pill in Immersive Mode**:
  - Clock, Battery Percentage, and Remaining Time overlay displays gracefully in full-screen immersive playback when controls are hidden.
  - Single-line format with smart dot separators (`11:05 AM · 84% · −1:42:15`) that never duplicate or misalign.
  - Perfectly anchored top-right coordinates with safe inset padding that prevents notch or cutout collisions.
  - Seamless handover: when player controls are toggled on/off, the status pill remains fixed in position without jumping or shifting horizontally.

### 🎯 Snappy Micro-Animated Player Controls (Zero Morphing Distortion)
- **Animated Circular Seek Buttons**:
  - Replaced button shape-morphing with clean, fixed 54dp circular buttons featuring snappy spring rotation impulse (-24° / +24°) and scale punch (1.18x).
  - Floating skip accumulator badge pill (`+10s`, `+20s` / `-10s`, `-20s`) appears directly above the button during rapid seeking without changing button geometry under your thumb.
- **Fixed Voice Clarity Button**:
  - Symmetrical 44dp circular frosted button with smooth spring icon scale, active glow highlight, and instant toast feedback (`Voice Clarity · On`).
- **Play / Pause Spring Transition**:
  - Replaced vector morphing with smooth `AnimatedContent` scale and crossfade between play and pause states.

### 🪟 Pure Borderless Floating Cards & Scroll Blur Fade
- **Eliminated Corner Emboss & Bevel Artifacts**:
  - Removed artificial border strokes from cards, eliminating chalky or stepped emboss artifacts on rounded corners.
  - Natural `tonalElevation` (1.0dp–1.5dp) and `shadowElevation` (2.5dp–3.5dp) provide genuine, polished floating depth that is noticeable and refined.
- **Top & Bottom Scroll Blur Fade**:
  - Dynamic gradient edge blur fade (`animatedScrollBlendFade`) when scrolling up and down through the Settings screen.
- **Unified Settings Color Harmony**:
  - Settings panels use the exact solid surface tone of Home and Library cards, eliminating murky translucent grey-blue tints.

### 🚀 Snappy Cinema Portal Launch Sequence
- Accelerated launch sequence from 3.6s down to **1.4s** total (reduced motion: 250ms).
- Organic unhurried portal expansion seamlessly revealing your video library without artificial dragging delay.

### 📦 Installation
Download **Jadzumi-Player-v1.7.0.apk** below. Android 8.0+ / ARM64.
Same-production-key updates preserve settings and watch history.
