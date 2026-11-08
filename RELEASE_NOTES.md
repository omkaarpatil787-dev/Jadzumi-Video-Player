# Jadzumi v2.0.1 (Hotfix) — "Omniverse Cinema Suite & Gimmick-Free Player" 🌌🎬🚀

A major hotfix and feature expansion release delivering the complete *Omniverse Cinema* suite, pruning non-essential gimmicks (Ambilight glow, Eco mode, and 4D subwoofer rumble), and resolving AMOLED HDR10+ playback routing.

### 👥 1. Dual Subtitles Studio & Acoustic Subtitle Auto-Sync
- **Simultaneous Bilingual Subtitles**: Render two independent subtitle tracks at the same time (e.g., English primary dialogue with Japanese or Spanish secondary learning subtitles) with custom color overlays.
- **⚡ Acoustic Subtitle Auto-Sync**: 1-Tap intelligent auto-synchronization aligning subtitle cue timestamps with dialogue peaks, eliminating drift and sync errors without tedious manual offset adjustments.

### 📚 2. Curated Franchises & Smart Box Sets Shelf
- **Automated Saga Detection**: Automatically clusters and groups film sagas (Harry Potter, Resident Evil, Marvel Cinematic Universe, Star Wars, The Lord of the Rings & Hobbit, The Dark Knight, Fast & Furious, John Wick, Mission Impossible, Matrix, Alien, and Jurassic) in strict chronological order.
- **Watched Progress & Next-Up Film**: Displays real-time completion progress (`✓ X / Y Watched`) and 1-tap playback of the next unwatched movie in the saga.

### 🎨 3. Movie Aesthetic Color Harmony & Instant Scene Maker
- **Aesthetic Color Harmony Extractor**: Extracts a 5-swatch harmonic color palette with hex codes directly from the current video frame using intelligent luminance and chroma clustering.
- **Instant GIF / WebP Scene Maker**: 1-tap export of 2–6 second high-fidelity animated scene clips with instant Android system share sheet dispatch.

### 🌐 4. Universal LAN & Network Streaming (SMB / WebDAV / DLNA)
- **High-Throughput Wi-Fi Streaming**: Direct playback from Windows PC shared folders (SMB v2/v3), NAS units (Synology, QNAP), WebDAV servers, and UPnP/DLNA media servers with zero device storage consumed.
- **Integrated Storage Management**: Add, browse, and organize network endpoints directly from the Folders & Storage page.

### 🌟 5. Robust HDR Profile Switcher & AMOLED Display Match
- **Intelligent MediaCodec Routing**: Resolves purple/green color shifts and black crush on AMOLED HDR10+ displays (such as Samsung Galaxy phones & tablets) by routing Dolby Vision hybrid profiles directly to hardware 10-bit HEVC BT.2020 decoders.
- **Manual HDR Profile Switcher**: Choose explicitly between `Auto (Display Match)`, `Dolby Vision Master`, `HDR10+ Dynamic Metadata`, `HDR10 / Base Layer (BT.2020)`, and `SDR Tone-Mapped`.

### 🧹 6. Complete Gimmick Removal & Battery Optimization
- **Cleaned Non-Essential Simulations**: Fully removed *Screen Ambilight Glow*, *Cinema Eco Mode*, and *4D Cinema Subwoofer Engine*, eliminating the `VIBRATE` permission and reducing background CPU/GPU thread contention for peak 120Hz/144Hz fluidity.

---

# Jadzumi v2.0.0 — "The Omniverse Cinema" (Iteration 1: Audiophile Studio & Visual Immersion) 🌌🎧✨

Welcome to **Jadzumi 2.0**! This major milestone kicks off the locked *Omniverse Cinema* roadmap, bringing physical 4D immersion, binaural 3D acoustics, dynamic OLED edge glow, and multitasking in-app mini player docking.

### 🎧 1. Spatial Cinema 3D & Binaural Virtualizer
- **Hardware-Accelerated 3D Acoustic Field**: Integrated Android's native DSP `Virtualizer` to model multi-channel 3D acoustic room reflections for headphones and stereo speaker setups.
- **Selectable Acoustic Profiles**: One-tap toggle between `Off (Stereo Direct)`, `Subtle (Club Acoustic)`, `Studio 3D (Reference Room)`, and `Wide IMAX 3D (Acoustic Theater)`.
- **Global & Quick Menu Access**: Accessible both within the video player Quick Menu and under **Settings → Audio**.

### 📳 2. 4D Tactile Cinema Subwoofer (Haptic Rumble Engine)
- **Low-Frequency Action Rumbles**: Live sub-bass FFT analysis (20Hz–120Hz) converts explosions, thunder, and heavy engine rumbles into authentic tactile haptic pulses in your hands.
- **Intensity Presets**: Choose between `Off`, `Gentle (Subtle Rumble)`, `Balanced (4D Cinema)`, and `High-Octane (Subwoofer Punch)`.
- **Haptic Safety**: Debounced pulse interval prevents continuous motor strain and honors battery efficiency.

### 🌈 3. Atmospheric Screen Ambilight (OLED Edge Glow)
- **Beyond-the-Bezel Canvas Expansion**: Projects soft, dynamic ambient gradient halos radiating from the active video content onto top/bottom letterbox and side pillarbox black bars.
- **Lighting Dynamics**: Select between `Off`, `Subtle (Calm Ambient)`, and `Expressive (Dynamic Halo)` with smooth 600ms animated color blending.

### 📱 4. In-App Docked Mini Player (YouTube / Spotify Style)
- **Seamless Multitasking**: Swipe down on `CinemaPlayer` or tap the new header minimize button to dock playback into an interactive floating bottom card while continuing to browse your library, search movies, or adjust settings.
- **Integrated Controls**: Features top linear progress indicator, mini video glyph, title/duration timecode, jump +10s forward, play/pause toggle, and quick dismiss.
- **1-Tap Re-Expansion**: Tap anywhere on the docked card body to return seamlessly to full-screen cinema mode with zero audio interruption.

---

# Jadzumi v1.9.1.2 (Hotfix) — "Universal Seeking & Unindexed Media Engine" ⏩🚀

A critical playback enhancement enabling smooth fast-forward, rewind, and timeline scrubbing on video files with missing, corrupted, or fragmented seek indexes, and streamlining the core media library.

### ⏩ 1. Universal Constant-Bitrate & Fallback Seeking Engine
- **Unindexed Video Rescue**: Configured Media3 `DefaultExtractorsFactory` with `.setConstantBitrateSeekingEnabled(true)` and `.setConstantBitrateSeekingAlwaysEnabled(true)`, allowing ExoPlayer to accurately calculate seek offsets based on average bitrate and duration whenever a video file is missing its container index table (`Cues` in MKV, `moov`/`stss` in MP4, `idx1` in AVI, or TS transport streams).
- **Edit List Workaround**: Enabled `Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS` to prevent corrupted seeking on non-standard MP4 edits.
- **Adaptive Seekable Fallback**: Decoupled UI timeline scrubbing and gesture seeking from rigid seekable flags, ensuring jump buttons and scrubbers remain active on all local files with known durations (`duration > 0 && !isCurrentMediaItemLive`).

### 🧹 2. App Vault Removal & Performance Decluttering
- **Complete App Vault Removal**: Fully decommissioned the Private Vault and biometric prompt locker across the app, removing background SharedPreferences listeners, UI lock sheets, and card overlays for pure offline video player performance.

---

# Jadzumi v1.9.1.1 (Hotfix) — "Precision HDR Profile Router & Core Player Stabilization" 🚀✨

A critical hotfix and refinement release addressing AMOLED HDR10+ playback, biometric vault repairs, binge automation accuracy, and codebase decluttering.

### 🌟 1. Interactive HDR Profile Switcher & Real AMOLED Shadow Clarifier
- **Manual HDR Profile Router**: Instant toggle between `Auto (Display Match)`, `Dolby Vision Master`, `HDR10+ Dynamic Metadata`, `HDR10 / Base Layer (BT.2020)`, and `SDR Tone-Mapped (Full Color Studio)`.
- **AMOLED HDR10+ Decoding Fix**: Resolves severe black-crush on Samsung Galaxy, OnePlus, Xiaomi, and other AMOLED panels when playing hybrid Dolby Vision Profile 7 & 8 files by enabling one-tap hardware HEVC base-layer routing.
- **Physical Shadow Clarifier Control**: Integrated real Shadow Clarifier options (`Off`, `Mild +25%`, `High +50%`) directly into the Enhancer menu, boosting near-black EOTF toe levels and display driving voltage to rescue dark night scenes without highlight clipping.

### 🛡️ 2. Biometric Private Vault Restoration & Quick-Lock Action
- **1-Tap Lock / Cloak**: Added direct "Move to Private Vault" / "Remove from Vault" buttons to all video library cards and movie detail pages.
- **Modernized BiometricPrompt Flags**: Updated Android 11+ (API 30+) authentication flags using `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` with seamless fallback for un-enrolled devices.
- **True Library Cloaking**: Vaulted media files are completely invisible from public library, search, and recents until unlocked with fingerprint or PIN.

### 🍿 3. Smart Binge Automation Alignment & Accurate Skip Timers
- **Series Key Normalization**: Unified series identification across episode details and playback controller using `resolveSeriesKey()`, ensuring custom intro and outro skip timers apply across all episodes in a show.
- **Dynamic "Skip Intro (Xs)" Pill**: Reads custom series intro timestamps (e.g. 85s, 90s) dynamically and seeks directly past the intro.
- **Reliable Next Episode Card**: Automatically presents the Next Episode autoplay countdown card during the credits window.

### 🧹 4. Gimmick Pruning & Fluidity Optimization
- **Cleaned Non-Essential Features**: Removed *Cinema DNA & Viewer Persona Studio*, *Dialogue Replay*, and *Scene Bookmarks & Notes*, reducing background memory footprint and boosting player UI fluidity and responsiveness.

---

# Jadzumi v1.9.1 — "The Pure HDR & Dolby Vision Color Engine Release": Smart Fallback Decoder, Universal 10-Bit Window Pipeline, HDR Shadow Clarifier & Dynamic Badging 🌟🎞️

A critical playback fidelity update delivering full hardware HDR10, HDR10+, and Dolby Vision compatibility, eliminating purple/green color distortion, resolving dark/crushed shadow detail, and unlocking the full luminance potential of your display.

### 🌈 1. Smart Dolby Vision & HDR10+ Fallback Architecture
- **Hardware Base-Layer Fallback (`HdrCodecSelector`)**: On devices lacking native Dolby Vision hardware licenses (or displays without DV support, such as Samsung Galaxy phones/tablets, Google Pixel, and standard HDR10/HDR10+ panels), hybrid DV Profile 7 (UHD Blu-ray) and Profile 8 (Web-DL) files are automatically routed to native SoC hardware HEVC decoders.
- **Elimination of Purple/Green Artifacts**: Resolves the infamous IPT/ICtCp color mismatch where Dolby Vision streams were misdecoded as YUV, restoring true 10-bit BT.2020 colorimetry and pristine dynamic range.
- **Native DV Prioritization**: On certified Dolby Vision panels, native DV decoders take top priority with zero-overhead HEVC fallback.

### ☀️ 2. Universal 10-Bit HDR Window Mode (`COLOR_MODE_HDR`)
- **Unrestricted High Dynamic Range Pipeline**: Removed panel heuristics that restricted `COLOR_MODE_HDR` to hardcoded OLED devices. Now engages Android's native 10-bit wide-color-gamut composer (`ActivityInfo.COLOR_MODE_HDR`) across all compatible OLED, AMOLED, Mini-LED, and IPS displays.
- **SurfaceFlinger Dynamic Tone Mapping**: Prevents Android's hardware composer from squashing 1,000-nit SMPTE ST 2084 / HLG curves into an 8-bit SDR container, eliminating murky dark scenes and crushed blacks.

### 🌑 3. Dynamic "HDR Shadow Clarifier & EOTF Toe Boost"
- **Night Scene & Ambient Light Rescue**: Dedicated "HDR Shadow Clarifier" toggle in the Video Enhancer Quick Menu that gently lifts near-black shadows (0.01–5 nits) so actors and dark environments remain clearly visible without blowing out specular highlights.
- **Wide Gamut Preservation**: GPU-accelerated OpenGL ES processing strictly preserves native BT.2020 chrominance while lifting shadow details.
- **HDR Peak Brightness Auto-Boost**: Automatically ensures adequate display luminance headroom when HDR content is active, while seamlessly restoring user brightness upon exit.

### 🏷️ 4. Comprehensive Format Detection & Golden Status Badging
- **Full Spectrum Detection**: Comprehensive inspection across container MIME types, SMPTE ST 2084, HLG, BT.2020 colorimetry, and Dolby Vision codec profiles (`dvh1`, `dvhe`, `dva1`, `dav1`).
- **Dynamic Status Pill Badges**: The in-player status pill now proudly highlights the active format in radiant gold: `Dolby Vision`, `HDR10+`, `HDR10`, or `HDR10 (DV Hybrid)`.

---

# Jadzumi v1.9.0 — "The Pure Cinema & Intelligence Release": True Auto Frame Rate, Biometric Vault, Lossless Scene Clipper, Storage Space Saver, Binge Automation & Cinema DNA Persona Studio 🎬🛡️⚡

A monumental feature release introducing precision hardware display matching, private biometric vault storage, instant lossless scene clipping, automated binge watching, intelligent storage cleaning, and an offline Cinema DNA persona analytics studio.

### 🎯 1. True Auto Frame Rate (AFR) Display Mode Switching
- **Judder-Free 24fps Cinema Cadence**: Queries `Display.supportedModes` to negotiate exact refresh rates (24.00Hz, 23.976Hz) or integer multiples (120Hz for 5:5 cadence, 144Hz for 6:6 cadence), completely eliminating 3:2 pull-down stutter.
- **Hardware Surface API Integration**: Applies `Surface.setFrameRate(fps, FRAME_RATE_COMPATIBILITY_FIXED_SOURCE)` on Android 11+ (API 30+) for zero-flicker rate transitions.
- **Fluid UI Restoration**: Automatically restores maximum device refresh rate (120Hz/144Hz) upon leaving cinema playback for ultra-responsive navigation.

### 🛡️ 2. Biometric Private Vault (Secure Cinema Locker)
- **Hardware-Backed Protection**: Lock sensitive films or personal recordings behind device fingerprint, Face Unlock, or device PIN using Android `BiometricPrompt`.
- **Absolute Library Cloaking**: Vaulted videos are immediately hidden from Home, Movies, TV Shows, search queries, and recent carousels.
- **Auto-Relock Security**: Vault automatically relocks whenever leaving the sheet, minimizing the app, or locking the device.

### ✂️ 3. Instant Lossless Video Clip Trimmer & Share
- **Zero Re-Encoding Stream Copy**: Extracts clean 1080p and 4K MP4 clips in 1–2 seconds using Android's native `MediaExtractor` and `MediaMuxer`.
- **Interactive Dual-Slider Trim Bar**: Fine-tune clip start and end points with live preview timecodes.
- **One-Tap Direct Share**: Launches standard Android share sheet to send clips straight to WhatsApp, Discord, Telegram, or device storage.

### 🍿 4. Smart Per-Series Binge Automation
- **Custom Intro & Outro Timers**: Save custom intro skip lengths (10s–180s) and credits windows per TV series.
- **Auto-Skip Intro**: Automatically fast-forwards through opening themes within the first 12 seconds of playback.
- **Auto-Skip Credits & Next**: Automatically starts the next episode when credits begin rolling, eliminating manual scrubbing.

### 💾 5. Storage Analyzer & Space Saver
- **Watched Media Cleanup**: Scans library for completed movies and episodes and calculates total recoverable space.
- **Safe Batch Reclaim**: One-tap "Clean All" to safely purge completed files via Storage Access Framework, reclaiming gigabytes of device storage.
- **Heavyweight File Inspector**: Instantly identifies massive files (> 3 GB) taking up local space.

### 🧬 6. Cinema DNA & Viewer Persona Studio
- **Offline Viewing Intelligence**: Analyzes local SQLite watch logs to compute total hours, prime-time viewing heatmap, and 4K ratios.
- **Authentic Viewer Archetypes**: Unlocks fun cinema personas including *The Midnight Binge Architect*, *The 4K HDR Purist*, *The Hyper-Velocity Scholar*, *The Franchise Marathoner*, and *The Authentic Cinephile*.
- **Holographic Frosted Persona Card**: Generates an aesthetic shareable card with your archetype motto and top stats. 100% offline and private.

### 🔖 7. Interactive Chapter Drawer & Timeline Notches
- **Narrative Act & Chapter Navigation**: Slide-up drawer displaying all video chapters and acts with live playing indicators.
- **Timeline Notches**: Subtle vertical divider marks rendered directly on the scrub bar with haptic snap during scrubbing.

### 🔋 8. Smart Cinema Battery Estimator & Eco Mode
- **Traveler's Companion**: Monitors battery level and real-time discharge to project remaining watch time (e.g., *"~4h 15m left ≈ 2.5 episodes"*).
- **Cinema Eco Mode**: Enforces 24Hz display mode and optimizes hardware decoders to stretch battery life during flights and travel.

### ✅ 9. Watched / Unwatched State Badges & Filter Chip
- **Visual Checkmark Badges**: Distinct emerald checkmark badges on watched movie posters, TV cards, and episode rows.
- **Library Filter Parity**: Added dedicated "Watched" filter chip alongside "In Progress", "Unwatched", "4K UHD", and "Recently Added".

---

# Jadzumi v1.8.0.3 — "The Project Integrity & Regulatory Alignment Release": Documentation Parity, Repository Hygiene & Privacy Disclosures 🛡️✨

A project governance and repository refinement release addressing documentation parity, clarifying privacy endpoint disclosures, removing legacy binary artifacts, decluttering historic checkpoint logs, and updating compliance records.

### 📚 1. Documentation Parity & Version Synchronization
- **Eliminated Documentation Drift**: Updated all root guides (`README.md`, `RESUME.md`, `THIRD_PARTY_LICENSES.md`, `CODEC_LICENSE_AUDIT.md`, `RELEASE_COMPLIANCE_CHECKLIST.md`) to point consistently to active release versions and build identifiers.
- **Bundled Licenses Consistency**: Ensured the offline in-app license file (`THIRD_PARTY_LICENSES.md` bundled via `generateLicenseAssets`) matches the distributed build metadata.
- **Toned Down Absolute Claims**: Refined promotional and compliance terminology across documentation, acknowledging realistic scraper edge cases and clarifying that network lookups operate with explicit user consent.

### 🔒 2. Transparent Network Endpoint & Privacy Disclosures
- **Full Privacy Disclosure for Cinemeta**: Added explicit disclosures for **Cinemeta** (`v3-cinemeta.strem.io`) in `README.md` and `RELEASE_COMPLIANCE_CHECKLIST.md` alongside Wikipedia and TVMaze.
- **Permission Justification Clarity**: Documented that `android.permission.INTERNET` is exclusively used for user-directed metadata searches (Cinemeta, Wikipedia, TVMaze) and optional GitHub update checks, remaining completely inactive when lookups are disabled.

### 🧹 3. Repository Hygiene & Tree Decluttering
- **Removed Committed Binaries**: Safely purged 52 MB of legacy debug APKs from `artifacts/` and added `artifacts/` and `*.apk` to `.gitignore`.
- **Eliminated Inaccurate Checksums**: Dropped outdated `SHA256SUMS.json` to prevent checksum confusion.
- **Historic Docs Archival**: Moved 20+ legacy session notes and recovery checkpoint logs into `docs/archive/`, leaving `docs/` clean with core developer guides and roadmap documentation.
- **Corresponding Source & Asset Provenance**: Formally recorded upstream NextLib/FFmpeg build coordinates and AI-synthesized demo image provenance (`hero.jpg`, `cosmos.jpg`, `anime.jpg`) in `LEGAL_REVIEW_REQUIRED.md`.

---

# Jadzumi v1.8.0.2 — "The Universal Expressive Motion & Update Engine Update": 4-Part Semantic Version In-App Updater & Fluid Touch Micro-Interactions 🚀✨🧈

A dedicated fluidity and maintenance release resolving in-app update detection for 4-component semantic releases (`v1.8.0.x`) and expanding Material 3 Expressive spring physics, tactile touch-lighting, and elastic micro-interactions app-wide.

### 🔄 1. In-App Updater 4-Part Semantic Version Support
- **Full Support for 4-Component Version Tags**: Updated `UpdatePolicy.kt`'s `newerStableRelease` comparator to parse and compare 4-component release tags (e.g. `v1.8.0.1`, `v1.8.0.2`) alongside standard 3-component tags (`x.y.z`).
- **Zero Missed Releases**: Eliminates the issue where hotfix or refinement builds (`v1.8.0.x`) were previously rejected by the strict 3-token regex as "not newer". Tapping "Check for updates" now accurately detects any newer production release from GitHub Releases.

### 🧈 2. App-Wide Material 3 Expressive Micro-Interactions
- **Expressive Springs Enabled by Default**: Calibrated default damping ratio to `0.60f` with snappy `600f` stiffness for lively, organic feedback on touches, buttons, chips, and selection toolbars.
- **Enhanced Elastic Press Physics**: Pressing cards and action buttons now delivers responsive 4.5% elastic compression with luminous radial ripple lighting.
- **Shared-Axis Screen & Section Transitions**: Extended fluid directional slide-and-crossfade transitions to folder selectors, multi-select batch bars, and action dialogs.

---

# Jadzumi v1.8.0.1 — "The Persistent Art & Adaptive Splash Release": Disk Poster Cache Persistence, Adaptive Splash Screen & Universal 0.5s Continuous Morph 🎬✨🧈

A critical persistence and polish update resolving the poster loading bug on app relaunch, introducing the new clean Adaptive Splash Screen with glass morphism and ambient lighting, and strictly unifying the App-Wide Continuous Shared-Element Morph Engine to 0.5s.

### 🖼️ 1. Permanent Poster & Metadata Disk Persistence (Zero Blank Cards on Reopen)
- **Resolved False Cache Eviction**: Fixed an issue where cached movie and TV show artwork was mistakenly flagged by Wikipedia text-validation heuristics (`isFilmOrTvWork`) on app relaunch and deleted from disk cache. Verified Cinemeta (IMDb), TVMaze, and manual Fix Match entries are now permanently safeguarded.
- **Unified Cache Keys**: Standardized title parsing and year normalization across library extraction (`LocalSeries.kt`) and scraper lookup (`OnlineMetadataFetcher.kt`), preventing key mismatches on relaunch.
- **Asynchronous Card Prefetcher**: Added automatic background prefetchers to `MovieSummaryCard` and `TvSeriesSummaryCard` in `LocalLibraryUi.kt`. Posters load instantly (0ms) from disk cache on app relaunch without requiring the user to tap into details pages.

### 🌟 2. New Signature Adaptive Splash Screen (Spec-Accurate Studio Implementation)
- **Clean Adaptive Logo**: Implemented the glass-morphic squircle icon from `jadzumi_adaptive_splash_clean.html` featuring quad-stop gradient tiles, sliding dual-layer 3D wings, specular front glass play symbol, and diagonal light sheen sweep.
- **Seamless Appearance Theming**: Dynamic adaptation for **Light** (`#F3F9FF`), **Dark** (`#0B1426`), and **AMOLED** (`#000000`) appearances with matching ambient radial glow and progress tracks.
- **Progressive App Entry**: Smooth 1800ms timeline with progressive fill bar and unhurried scale/fade handoff directly into the media library.

### 🧈 3. Unified 0.5s App-Wide Continuous Shared-Element Morph Engine
- **Standardized 500ms Motion Engine**: Locked all screen, details card, and dialog geometry transitions to a deliberate **0.5s (500ms)** duration with `cubic-bezier(0.19, 1.0, 0.22, 1.0)`.
- **Fix Match Dialog Geometry Morph**: `FixMatchDialog` smoothly expands physical geometry (width, height from 60% to 85%, rounding from 38dp to 26dp, and depth scale) without popping or layout blinking.

---

# Jadzumi v1.8.0 — "The Smoothness & Refinement Milestone": Fix Match & Artwork Matcher, App-Wide 500ms Shared-Element Morph, Ambient Cinema Backdrop & YouTube Cascading Seek Chevrons 🎬✨🧈

A landmark refinement update introducing manual "Fix Match" & gallery poster matching, automated scraper guardrails against encyclopedic entries, universal 500ms continuous shared-element geometry morphing across navigation, an airy pastel horizon in Light mode / cinema ambient glow in Dark mode, and sleek YouTube-style cascading chevron seek pills.

### 🔍 1. "Fix Match" & Artwork Matcher (Manual & Online Disambiguation)
- **Interactive Fix Match Dialog**: Added a prominent yet elegant "✏️ Fix Match" action button on `MovieDetailsPage` (also accessible by directly tapping the poster thumbnail) to correct any metadata or poster mismatch in seconds.
- **Cinemeta & IMDb Streaming Catalog Search**: Search by title or paste an IMDb ID (`tt...`) to view top verified candidate cards with official posters, release years, IMDb star ratings, and synopses.
- **Custom Local Gallery Poster Picker**: Pick any local image from your device's photo gallery (`image/*`) to use as custom cover art.
- **Automated Scraper Guardrails**: Added strict filters rejecting non-film encyclopedic concepts (colors, dictionaries, sociological articles, and disambiguations) so files like *Blue*, *Apocalypse Z*, and *Sapiosexual* never match conceptual articles.
- **Persistent Local Overrides**: Manual matches are stored permanently in local storage and update the movie details immediately with zero app restarts.

### ✦ 2. Universal 500ms Continuous Shared-Element Geometry Morph
- **Calibrated Motion Curve**: Standardized screen and card transitions to a deliberate **500ms spring curve** (`cubic-bezier(0.19, 1, 0.22, 1)`).
- **Continuous Geometry Interpolation**: When navigating between Library Cards and Movie Details, card boundaries (width, height, rounding, depth) physically stretch and collapse to their coordinates at 60 FPS without blank screen flashes or layout cuts.

### ✨ 3. Ambient Cinema Backdrop & Airy Light Horizon
- **Light Mode Porcelain Perfection**: Replaced muddy dark color smudges with an airy, feather-soft pastel horizon wash (`#FEF3C7` ➔ `#FEF08A` ➔ transparent) and high-depth colored poster drop shadows, preserving 100% contrast on deep slate text.
- **Dark & AMOLED Ambient Glow**: Diffused, atmospheric cinema radial glow behind the hero banner on `MovieDetailsPage`.

### ⚡ 4. YouTube-Style Cascading Chevrons Seek Pill & Elastic Boundary Bump
- **Compact Chevron Seek Capsule**: Replaced oversized circular overlays with a sleek YouTube-style cascading chevron pill (`‹‹‹ 10s` and `10s ›››`) with staggered animated alphas that floats compactly and disappears without obstructing playback.
- **Elastic Rubber-Band Squash & Stretch**: Volume and Brightness HUD now performs an elastic rubber-band squash & stretch (`scaleX = 1.07f, scaleY = 0.94f`) with physical boundary haptic feedback upon reaching the 100% ceiling.

---

# Jadzumi v1.7.9 — Full Theme Adaptive Playback Controls & Quick Menu (Light / Dark / AMOLED) 🎨✨🧈

A dedicated visual consistency and theme optimization release ensuring every playback HUD overlay, quick menu option, status pill, and skip-intro button dynamically and harmoniously matches the active display appearance (Light mode, Dark mode, or AMOLED mode).

### ☀️ Light Mode Harmonic Playback Controls
- **Theme-Adaptive Quick Options Menu**: The right-side quick menu (`RightSideQuickMenu`) now renders a clean, frosted light glass container (`Color(0xF6F8FAFD)`) with crisp slate borders (`Color(0xFFD0D7DE)`), rich deep slate typography (`Color(0xFF183654)`), muted subtexts (`Color(0xFF526B85)`), and vivid primary blue accents (`Color(0xFF0061A4)`).
- **Zero Color Inversion / White-on-White Bugs**: Every option row, telemetry card, slider, stepper button (`-` and `+`), and sub-menu adapts dynamically with 100% legibility and contrast across all three modes.
- **Harmonized Playback Status Pill**: The top-right battery/clock pill now dynamically inherits the active frosted theme colors (`dynamicFrostedCircleBg`, `dynamicFrostedCircleRim`, `playerContentColor`), eliminating the visual clash of a dark pill amidst light controls.
- **Theme-Adaptive Skip Intro Pill**: Replaced the hardcoded dark capsule with dynamic frosted theme styling and responsive text/accent coloring.

### 🖤 Pure AMOLED & Dark Mode Fidelity
- **AMOLED Mode**: Quick menu and status capsules render with true deep black backgrounds (`Color(0xFC000000)` and `Color(0xEE000000)`) with crisp 1.dp high-contrast white hairlines, reducing battery consumption on OLED displays.
- **Dark Mode**: Cinematic frosted dark glass (`Color(0xF00D1520)`) with subtle specular highlights and crisp white typography.

---

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
