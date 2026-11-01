# Jadzumi v1.7.8 — Clean Uncluttered Media Cards, Crisp Micro-Border Clarity, AOD Status Pill & Dark Cinema Quick Menu 🎬✨🧈

A refined visual ergonomics and playback UI release removing redundant card chips, eliminating blurry elevation halos with crisp 1.dp hairline definition, transforming the Now Playing status pill into a subtle Always-On Display (AOD) capsule, and ensuring the Quick Customization menu retains dark cinema glass contrast across all themes.

### 🧹 Clean, Uncluttered Media Cards
- **Removed Redundant Bottom Chips**: Eliminated unnecessary tags (`HDR Master`, `10-Bit Color`, `Horror`, `Thriller`, `Adventure`) from unwatched movie cards for a clean, streamlined 2-line layout (Title + Year · Duration · Genre).
- **Preserved Meaningful Status**: Maintained clean `✓ Watched` status badges and actionable `Resume · 42m left` indicators on in-progress titles.

### 📐 Crisp Micro-Border Surface Definition
- **Zero Blurry Elevation Shadows**: Replaced fuzzy drop-shadow elevation halos with razor-sharp 1.dp hairline surface boundaries (`Color(0xFFE2E8F0)` in Light mode, translucent white in Dark/AMOLED mode).
- **High-Clarity Card Canvas**: Pure crisp surface contrast across both phone and tablet form factors.

### 🌙 Always-On Display (AOD) Status Pill
- **Subtle, Non-Glaring HUD**: Replaced glowing white status pill with a subtle dark translucent capsule (`Color(0x66000000)`) with soft white hairline rim and muted AOD typography (`Color.White.copy(alpha = 0.88f)`).
- **Theme Independent Consistency**: Remains uniform and non-distracting during cinema playback regardless of whether the app theme is Light, Dark, or AMOLED.

### 🎛️ Dark Cinema Quick Menu (100% Legible Fonts)
- **High-Contrast Cinema Glass**: Fixed issue where the Right-Side Quick Menu had a white background in Light theme, which made white text and icons invisible. Quick Menu is now styled with dark cinema glass (`Color(0xF00D1520)`) unconditionally with crisp, high-contrast white text and icons.

### 🌐 Metadata Accuracy & Deep Translation Support
- **Multi-Word Overlap Matching**: Enhanced `scoreCandidate` to recognize translated and international film titles (e.g. *Kitab Sijjin and Illiyyin* accurately mapping to *The Book of Sijjin and Illiyyin*).
- **Deep Cache Sanitization**: Strengthened `purgeBogusCache` to proactively eliminate any mismatched or legacy poisoned cache entries.

---

# Jadzumi v1.7.7 — Ghost PiP Fix, Micro-Border Surface Definition & De-Cluttered Media Cards 🖼️🧈🎯

A targeted refinement addressing accidental PiP activations from the library/home screen, introducing elevated micro-border card surfaces, frosted glass action buttons, and a de-cluttered media card hierarchy.

### 🛑 Ghost PiP Revocation & State Safety
- **Explicit OS Auto-PiP Revocation**: Completely fixed the issue where returning home from the Movies or TV Shows library triggered Picture-in-Picture. Whenever video playback closes or stops, `updatePip()` explicitly sends `setAutoEnterEnabled(false)` to Android OS (API 31+).
- **Reactive Activity Inset Sync**: Added an active `LaunchedEffect` directly in `MainActivity` root content to ensure the OS never retains auto-enter PiP permission when the user is browsing the library.

### 🎨 Refined Movie & TV Series Cards
- **Micro-Border Surface Definition**: Added layered 1.dp micro-borders (`outlineVariant` with 2.dp elevation in Light mode, translucent white in Dark/AMOLED mode) eliminating flat card fade-out.
- **De-Cluttered Hierarchy (No Genre Repetition)**: Removed redundant genre labels on the card footer. Dynamically replaces them with clean `✓ Watched` badges, remaining playtime chips (`Resume · 42m left`), or technical format tags (`HDR Master`, `10-Bit Color`).
- **Tactile Frosted Action Buttons**: Quick play buttons now feature frosted glass rims, micro-shadows, and smooth touch-motion response.
- **High-Contrast Progress Tracks**: Added a dark underlay channel behind card playback progress indicators for crisp visibility across any artwork.

---

# Jadzumi v1.7.6 — Direct Cinemeta (IMDb & Streaming Catalog) Integration for 100% Accurate Artwork 🎬🔥🖼️

A major artwork and metadata accuracy release integrating direct access to the **Cinemeta entertainment catalog** (powered by IMDb and Metahub streaming indexes), delivering 100% accurate, high-definition official posters and backdrops for OTT releases, international movies, and regional cinema without API key limits.

### 🌐 Direct OTT & Entertainment Catalog Integration (Cinemeta)
- **Comprehensive Streaming & Theatrical Index**: Directly connects to the comprehensive IMDb/Metahub catalog indexing releases across Amazon Prime Video, Netflix, Apple TV+, Disney+ Hotstar, HBO, Zee5, and global cinema.
- **Accurate Resolution for Previously Troubled Titles**:
  - *Apocalypse Z: New World* (2026): Exact Amazon Prime movie poster, background, and horror overview.
  - *Sapiosexual* (2023): Official thriller/drama movie poster, IMDb rating (2.8), and synopsis (eliminates encyclopedia concept mismatches).
  - *Kitab Sijjin and Illiyyin* (2024): Exact Indonesian horror film poster and background ("The Book of Sijjin & Illiyyin") rather than falling back to offline.
  - *Blue*: Exact theatrical movie artwork and backdrop rather than encyclopedia colour articles.
- **Multi-Tier Robust Architecture**:
  - **Tier 1 (Cinemeta)**: Exact title and prefix matching against global streaming & theatrical database.
  - **Tier 2 (Wikipedia with Film Validation)**: Fallback to Wikipedia with strict non-film topic filtering (`isFilmOrTvWork`).
  - **Tier 3 (TVMaze)**: TV series single-search and multi-search fallback.
- **Permanent Poison-Cache Immune**: Always queries from original file specs and purges corrupt legacy entries automatically.

---

# Jadzumi v1.7.5 — Film Concept Validation, Permanent Cache Sanitization & YouTube-Grade Seamless PiP 🎬✨🧈

A targeted stability and smoothness update delivering concept-filtered movie artwork matching, permanent break from poisoned metadata cache loops, responsive Aspect Ratio re-layout triggers, and YouTube-grade smooth Picture-in-Picture transitions without UI overlay freeze.

### 🎯 Strict Film Concept Validation & Poison-Proof Artwork
- **Concept & Encyclopedia Exclusion**: Added `isFilmOrTvWork` validation to strictly filter out encyclopedia concept articles (such as sexual orientation, colors, scientific terms, or biology genera) from being mistakenly matched to local movie files.
- **Candidate Fallback & Progressive Pruning**: Iterates candidate titles until a verified film work is found with high confidence (e.g. accurately retrieving *Apocalypse Z: The Beginning of the End* instead of *Zombie Massacre* / *World War Z*).
- **Poisoned Cache Auto-Purge**: Evicts corrupt or non-film cache entries on startup (`purgeBogusCache`) and immediately on lookup, ensuring old misidentifications never persist.
- **Filename-Anchored Searches**: Decoupled Movie Details metadata lookups to always query from sanitized original filenames rather than cached titles, eliminating circular cache poisoning.

### 📐 Reactive Aspect Ratio Engine
- **Direct Layout Invalidation**: Added dedicated reactive layout triggers in `CinemaPlayer` (`LaunchedEffect(playback.aspectMode, playback.aspect)`) to immediately invalidate and measure `AspectRatioFrameLayout` (`exo_content_frame`) whenever the user changes Fit, Zoom, Stretch, 16:9, 4:3, or 21:9.
- **State Read Reactivity**: Guaranteed PlayerView recomposition by lifting aspect state bindings out of raw update lambdas into active composable scopes.

### 🧈 YouTube-Grade Seamless Picture-in-Picture
- **Clean PiP Subtree**: Isolated Picture-in-Picture rendering to an uncluttered, raw `PlayerView`—completely eliminating Top Bar, Scrubber Dock, Center seek/pause overlays, gestures, and status pills inside the floating PiP window.
- **Pre-Animation State Synchronization**: Sets `playback.inPip = true` synchronously prior to invoking `enterPictureInPictureMode`, ensuring the OS captures a pristine video surface snapshot without frozen control artifacts.
- **Solid Window Scrim & Inset Smoothness**: Enforces solid black window decor backgrounds to prevent launcher wallpaper bleed-through and layout jitter during PiP expand/contract transitions.

---

# Jadzumi v1.7.4 — Poster Artwork Accuracy, Responsive Aspect Ratios & Silky 120Hz PiP Transitions 🎬📐✨

A performance, accuracy, and smoothness update addressing metadata matching for regional & international titles, responsive aspect ratio modes with direct layout invalidation, and zero-stutter Picture-in-Picture transitions.

### 🎯 High-Precision Poster Artwork & Metadata Matching
- **Intelligent Media Query Sanitization**: Strips audio encoding, dubbed language markers (Hindi, Dual Audio, Multi Audio, Clean Audio, Tamil, Telugu, etc.), rip source tags (PreDVD, HDTC, HDCAM, WEBRip, BluRay), and release group suffixes before query execution.
- **Anchor Word Preservation & Candidate Scoring**: Prevents wrong movie poster fetches (e.g., *Apocalypse Z: The Beginning of the End* is no longer hijacked by *World War Z*). Candidate scoring enforces mandatory anchor keyword matching and Dice token similarity.
- **Multi-Tier Search Strategy**:
  - **Tier 1 (OpenSearch Title Matching)**: Direct Wikipedia exact title search, eliminating actor biography hits and unrelated full-text matches.
  - **Tier 2 (Progressive Multi-Word Fallback)**: Intelligent query pruning with year disambiguation.
  - **Tier 3 (Candidate Scoring)**: Full-text search with strict threshold verification (>= 0.35 similarity).
- **Poisoned Cache Auto-Eviction**: Automatically detects and purges previously misidentified metadata entries (such as actor biographies, voice credits, or generic film list pages) so accurate artwork is retrieved seamlessly.
- **TV Series Refinement**: TVMaze single-search title verification and Wikipedia TV series fallback.

### 📐 Fully Responsive Aspect Ratio & Video Scaling
- **ExoContentFrame Direct Reactivity**: Fixed aspect ratio modes by directly binding `AspectRatioFrameLayout` (`exo_content_frame`) inside `PlayerView` with programmatic `requestLayout()` triggers.
- **Expanded Aspect Ratio Modes**:
  - **Fit to screen (Letterbox)**: Preserves native video dimensions with letterboxing/pillarboxing.
  - **Crop to fill (Zoom)**: Scales to fill entire display without black bars.
  - **Stretch to fill**: Fills the viewport without cropping.
  - **16:9 Standard Widescreen**: Forces 16:9 widescreen presentation.
  - **4:3 Classic TV**: Forces standard definition academy / classic television ratio.
  - **21:9 Cinema UltraWide**: Forces anamorphic cinemascope presentation.
- **Dynamic Quick Menu & PiP Synchronization**: Changing aspect ratio dynamically updates Android OS Picture-in-Picture window aspect ratios in real time.

### 🧈 Butter-Smooth 120Hz PiP Transitions
- **Decoupled System Bars & Insets Lifecycle**: Keyed immersive fullscreen mode to video playback session rather than PiP state, eliminating insets recreation and orientation thrashing during the OS window transition.
- **Binder IPC Throttling**: Suppressed 60–120 fps `updatePip()` IPC calls during window scale animations; PiP parameters are only submitted when bounds change at rest.
- **Zero-Glitch Returning from PiP**: Re-applies immersive transient system bars smoothly without display flicker or status bar stutter.

---

# Jadzumi v1.7.3 — Subtitle Typography Suite, Theme-Reactive HUD & Decluttered Minimal Controls 🎨✨

A major aesthetic refinement and accessibility update introducing an expanded **Subtitle Typography & Edge Effects Suite**, dynamic **Theme-Reactive Playback UI**, and a **Decluttered Minimalist Player HUD**.

### 🔤 Subtitle Typography & 3D Edge Effects
- **Cinema Font Families**: Added *Cinema Serif*, *Heavy Sans*, *Rounded Sans*, and *Roboto Condensed* alongside classic system and monospace fonts.
- **Font Styles & Weights**: Choose between *Regular*, *Bold*, *Italic*, and *Bold Italic* for custom caption weight.
- **3D & Glow Edge Effects**:
  - *Raised 3D Bevel* and *Depressed 3D Inset* for crisp readability against textured movies.
  - *Soft Atmospheric Glow* for luminous subtitles without hard rectangular black boxes.
  - Classic *Drop Shadow* and *Outline Stroke*.
- **Palette & Contrast**: Expanded color tokens with *Amber Gold*, *Lavender*, and *Soft Orange*.
- **Vertical Margin Calibration**: Fine-tune caption bottom margin from standard (0.06) to high (0.16) for cinemascope letterbox positioning.
- **Live Interactive Preview Card**: Instant feedback in Subtitle Settings showing real-time styling, edge effects, and background box rendering.

### 🌓 Theme-Reactive Playback HUD
- **Adaptive Appearance Reaction**:
  - **Light Mode**: Frosted white milk-glass HUD pills (`#F0F4FC`) with dark navy typography (`#183654`) and deep-blue controls.
  - **Dark Mode**: Translucent slate-navy HUD (`#0F172A`) with crisp white text.
  - **AMOLED Mode**: Pure ink-black translucent surfaces (`#000000`) preserving OLED power and contrast.
- **Telemetry & Battery Pills**: Player status pill, speed pill, volume/brightness bars, and seek buttons all inherit theme-reactive color palettes seamlessly.

### 🎬 Decluttered Minimalist Player HUD
- **Single-Row Bottom Timeline**: Merged timecode, progress scrubber, remaining time pill, and essential action icons into a unified dock.
- **Quick Menu Integration**: Moved secondary buttons (*Voice Clarity*, *Dialogue Replay*, *HUD Mode Toggle*) into the Quick Menu drawer to keep video viewing uncluttered.
- **HUD Mode Toggle**: Quickly switch between *Minimalist HUD* and *Expressive HUD* on the fly.

---

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
