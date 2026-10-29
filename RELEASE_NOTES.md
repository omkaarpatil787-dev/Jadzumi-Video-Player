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
