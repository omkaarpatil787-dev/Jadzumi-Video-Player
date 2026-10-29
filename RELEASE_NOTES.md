# Jadzumi v1.6.0 — Universal Library Views & Playback HUD 🎬📱

An incremental usability and media management update bringing **Universal Grid / List View switching across all tabs**, **Playback HUD status overlays (Clock, Battery %, Remaining Time)**, **Advanced File & Blacklist Controls**, and refined **Brand Aesthetics**.

### ⏱️ Playback HUD Display & Status Pill
- **Display During Playback Multi-Option Dialog**:
  - Located in Video Settings: customizable checkboxes to toggle overlays.
  - **Remaining Time**: Shows live countdown (`−42:15`) in the top status pill and scrubber timeline.
  - **System Clock**: Shows current time in the top-right corner during playback.
  - **Battery Percentage**: Real-time battery indicator with charging state detection.
- **Top-Right HUD Status Pill (Persistent in Immersive Playback)**:
  - Elegant frosted pill rendering live system time, battery percentage, and remaining duration directly in **immersive mode** while watching movies, as well as when controls are visible.
  - Ultra-efficient updates using Android's sticky `ACTION_BATTERY_CHANGED` intent with zero CPU overhead or battery drain.

### 📱 Streamlined Home & Universal Views
- **Clean Cinematic Home Dashboard**:
  - Home now focuses cleanly on your Hero spotlight banner and in-progress Continue Watching queue, removing the cluttered raw video list from the home screen.
  - Full video collections remain accessible in Movies, TV Shows, and Connected Folders tabs.
- **Universal Grid & List View Toggle**:
  - Seamlessly switch between compact list rows and rich poster grid cards in Movies, TV Shows, and Connected Folders with persistent preferences.

### 🌊 Fluid Motion & Polished Layout
- **iOS & ColorOS Fluid Dynamics**:
  - Tuned physical spring damping (`0.85f` standard, `0.70f` expressive) and transition easing curves (`CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)`) across all screen, tab, and player transitions for rich, weighted momentum.
- **De-cluttered Tablet & Phone Settings**:
  - Re-architected Motion & Touch settings with clean typography, generous row spacing, and individual dividers, eliminating stacked or touching switches on tablets and phones.

### 📁 Advanced Files & Storage Settings
- **Folder Blacklist Management**:
  - Control which connected directories are visible in your library.
  - Blacklist specific folders without revoking SAF folder permissions or re-scanning storage.
- **Show .nomedia Files**:
  - Optional switch to display and index video files inside directories containing a `.nomedia` file.
- **Show Hidden Files**:
  - Optional switch to reveal files and directories starting with a dot (`.`).

### ✨ Refined Brand Aesthetics
- **Floating Bottom Navigation Dock Polish**:
  - Streamlined the mobile floating dock into a sleek, minimalist pill holding the active destination icon.
- **Living Brand Emblem Relocated to About**:
  - Moved the animated breathing / glowing Gemini-style Jadzumi emblem from the floating dock to the **About** screen for a focused, uncluttered navigation experience.

### 📦 Installation
Download **Jadzumi-Player-v1.6.0.apk** below. Android 8.0+ / ARM64.
Same-production-key updates preserve settings and watch history.
