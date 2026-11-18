# Jadzumi v3.4.6 - Enforced 2-Column Mobile Portrait Grid, Slim Nav Capsule & Desktop Delivery

A targeted mobile experience refinement enforcing a 2-column portrait grid across all mobile devices, slimming the top navigation capsule to 165dp for optimal button clearance, and ensuring complete Desktop artifact delivery for immediate local installation.

### 1. Enforced 2-Column Phone Grid
- Guaranteed 2-Column Architecture: Locked phone portrait grid layouts strictly to 2 columns, overriding any legacy compact density settings from SharedPreferences that previously compressed mobile cards into 3 narrow columns.
- Maximum Poster Width & Readability: Each card now consistently occupies at least 150dp-170dp of horizontal width, providing generous room for 2-line title wrapping and unclipped timestamps.

### 2. Slimmed Navigation Capsule (165dp)
- Clearance for Action Buttons: Slimmed JadzumiSegmentedNavCapsule width from 190dp to 165dp, providing generous breathing space for Search, Storage Cleaner, and Library Sync buttons on Row 1 across all mobile phone screen widths.

---

# Jadzumi v3.4.5 - Mobile UI Overhaul: Clean Two-Row Header, 2-Column Responsive Grid & Inset Protection

A comprehensive mobile layout optimization release resolving top header pill collisions and text bleeding, implementing an uncrowded two-row phone header architecture, establishing an adaptive 2-column mobile portrait card grid with 2-line title wrapping, and enhancing system gesture navigation insets.

### 1. Header Collision Resolution & Two-Row Phone Architecture
- Header Overlap Elimination: On mobile portrait screens, placing both the Home/Settings navigation capsule (190dp) and the category switcher (225dp) alongside action icons in a single row forced 540dp of layout into 360dp-400dp viewports, causing 'ttings' text to bleed directly through category tabs and cramming search/action buttons.
- Device Orientation & Width Detection: Integrated LocalConfiguration screen width and orientation checks (`screenWidthDp < 600 && !isLandscape`) to ensure smartphones reliably activate the dedicated two-row layout regardless of display density scaling.
- Uncongested Layout Hierarchy: Row 1 hosts the Home/Settings capsule on the left and circular action buttons (Search, Storage, Sync) on the right; Row 2 neatly seats the category pills (Movies | Series | Anime) with 8dp vertical breathing room and zero overlapping text.

### 2. Mobile Responsive 2-Column Grid & 2-Line Titles
- 2-Column Default on Mobile Phones: Set the default comfortable library density on phone portrait viewports to 2 columns instead of 3, expanding usable card width from ~100dp to ~150dp+.
- 2-Line Title Wrapping: Upgraded movie, series, and anime poster cards to support up to 2 lines of text (`maxLines = 2`), preventing long film titles, series titles, and personal camera recordings from abruptly truncating into ellipses.
- Preserved Metadata Readability: Durations and release years (`2017 · 01:42:25`) now display with full fidelity without clipping.

### 3. Gesture Bar Inset Protection
- System Bar Insets: Increased mobile bottom padding to 36dp across library views to prevent bottom cards from colliding with the Android system navigation gesture pill.

---

# Jadzumi v3.4.4 - Dolby Vision Profile 5 Native Decoders, ICtCp Color Fix & Direct HWC Composition

A targeted media engine and video pipeline release resolving purple and magenta color distortion on Dolby Vision Profile 5 content, prioritizing SoC hardware Dolby Vision decoders across all AMOLED and HDR panels, correcting HDR stream classification, and optimizing Direct Hardware Composer SurfaceView pass-through.

### 1. Dolby Vision Profile 5 Native Hardware Decoding & ICtCp Color Fix
- Root Cause Resolution: Dolby Vision Profile 5 (common in streaming releases from Disney+, Netflix, and Apple TV+) uses single-layer IPTPQc2 (ICtCp) color space without an HDR10 fallback base layer. Previously, Jadzumi checked whether the display panel explicitly declared Dolby Vision support. On devices whose displays do not declare Dolby Vision (such as HDR10+ AMOLED panels), the player bypassed the SoC's hardware Dolby Vision decoder and routed the bitstream to a generic HEVC decoder. The generic HEVC decoder interpreted ICtCp as standard YCbCr, causing severe purple and green posterization in shadows and highlights.
- Hardware Decoder Prioritization: In HdrCodecSelector, Dolby Vision decoders (e.g. c2.qti.dv.decoder, c2.exynos.dv.decoder) are now strictly prioritized whenever available on the chipset, regardless of display panel flags. The hardware decoder decodes ICtCp correctly and outputs accurate color to the display.

### 2. Stream Classification Fix for SDR-Flagged Containers
- Container Tag Handling: Certain Dolby Vision Profile 5 MP4 and MKV containers declare colorTransfer as SDR (transfer = 1) in their container metadata.
- Pre-Emptive DV Inspection: PlaybackController now inspects Dolby Vision codec identifiers (dvh, dvhe, dva, dav) and MIME types before checking SDR color transfer tags, ensuring Dolby Vision streams are never misidentified as SDR or routed into inappropriate rendering pipelines.

### 3. Direct Hardware Composer SurfaceView Pass-Through
- Zero-Copy Display Composition: CinemaPlayer now selectively bypasses Modifier.graphicsLayer on SurfaceView when zoom scale is 1.0 and pan offset is zero.
- Native 10-Bit Output: SurfaceView renders directly to the Hardware Composer (HWC) overlay without intermediate Compose render node allocations, preserving pristine 10-bit HDR output and eliminating compositing latency.

---

# Jadzumi v3.4.3 - Hyper-Interactive Watch Party Dock, Persistent WebP Disk Cache & Compose Stability

A performance and interactivity upgrade featuring an actionable collapsed Watch Party pill with direct mic and reaction triggers, a tabbed host command center with guest management and live VU meters, two-tier persistent WebP disk thumbnail caching for butter-smooth library scrolling, and Compose immutability optimizations.

### 1. Actionable Shrunk Watch Party Capsule
- **Direct Mic Toggle on Capsule**: Tap the microphone button directly on the collapsed floating pill to mute or unmute the host voice without expanding the full card.
- **Direct Quick Reactions Strip**: Fire reaction bursts (Cinema, Epic, Bravo) directly from the collapsed capsule, immediately rendering floating animations on the cinema canvas while keeping the movie view completely unobstructed.
- **Live Snippet Ticker & Counter**: Displays the latest guest message preview alongside an unread counter badge.

### 2. Tabbed Host Command Center
- **Voice VU Meter & Broadcasting Status**: Live animated audio VU meter bars visualizing speech activity and confirming the active -25% movie volume ducking.
- **Tabbed Experience (Chat vs Guests)**: Switch seamlessly between the live message feed and the connected guests manager.
- **Guest Management**: View connected peer IP addresses, sub-second sync status, and selectively disconnect participants if needed.

### 3. Two-Tier Persistent Disk & RAM Thumbnail Engine
- **Persistent WebP Disk Cache**: Video frame thumbnails are now automatically compressed and cached to persistent WebP files on local disk (`cacheDir/thumbnails/`).
- **Instantaneous 120fps Library Scrolling**: Loading cached thumbnails from disk takes under 2ms, completely bypassing heavy native MediaMetadataRetriever decoders on subsequent launches.
- **Disk Cache Clearing**: Dedicated clear cache controls wipe both RAM and disk caches cleanly upon user request.

### 4. Compose Compiler Stability & Recomposition Pruning
- **Immutable Data Models**: Annotated `LocalVideo`, `LocalFolder`, `LocalLibraryState`, `EpisodeHint`, `LocalSidecar`, and `TvSeries` with `@Immutable`.
- **Zero-Drop List Performance**: Informs the Compose runtime that list items are structurally stable, enabling Compose to skip 95% of recomposition passes during rapid library scrolling.

---

# Jadzumi v3.4.2 - Host Watch Party Mode, Collapsible Live Chat Dock & Content Picker

A major experience upgrade transforming the player into a comprehensive Watch Party command center on host tablets and devices, featuring a shrinkable/collapsible live reaction chat card, in-player video library switching, floating reaction bursts, and butter-smooth card list performance.

### 1. Dedicated Host Watch Party Mode
- **Player HUD Transformation**: Activating a Watch Party instantly transitions the player into Watch Party Mode, displaying a streamlined top control capsule with connected guest count, session status, and quick host actions.
- **Floating Live Reaction Bursts**: Animated reaction bubbles and emojis sent by either host or guests gently float upward across the video canvas without blocking playback visibility.
- **Clean One-Tap Session Teardown**: Tapping "End Party" cleanly shuts down background HTTP and WebRTC stream servers, disconnects peer clients, and seamlessly restores normal solo playback HUD.

### 2. Shrinkable & Collapsible Live Chat Card
- **Normal Docked View**: Anchored cleanly on the side with a 330dp acrylic blur card offering full live message history, host microphone mute toggle with automatic movie ducking indicator (-25%), quick reaction chips, and text composer.
- **Compact Floating Pill Capsule**: With a single tap on the minimize button, the chat card morphs into a floating frosted capsule showing unread count badges and incoming message snippet ticker. Tapping the capsule instantly expands back to full view.

### 3. In-Party Content Selector ("Choose What to Watch Next")
- **Live Video Switching**: Host can browse local device videos, series, and movie episodes inside an acrylic bottom drawer without disconnecting or leaving the watch party.
- **Synchronized Playback Transition**: Selecting a new title immediately changes media playback for the host and synchronizes the new stream to all connected guests.

### 4. Zero-Lag Card List & Drawer Scrolling
- **Stable Identity Keys**: Enhanced EpisodeDrawerSheet and card collections with unique stable item keys (`ep.identity`), preventing expensive layout remeasurements and eliminating scroll stutter.

---

# Jadzumi v3.4.1 - WebRTC Watch Party Audio Gate, Live Voice Controls & Reaction Bursts

A targeted maintenance and refinement release resolving browser audio playback restrictions, enabling bidirectional WebRTC live voice calling controls for guests, optimizing dynamic viewport layouts for mobile devices, and adding instant reaction bursts.

### 1. Web Audio Unlock Gate & Browser Audio Activation
- **Guaranteed Unmuted Audio Playback**: Implemented an interactive "Tap to Unmute & Connect" audio gate overlay that instantiates and resumes the browser AudioContext on first tap, overcoming mobile browser autoplay mute restrictions.
- **Top Bar Mute Toggle**: Added an instant Unmute/Mute button in the guest browser header bar for direct volume control without interrupting synchronization.

### 2. Live WebRTC Voice Calling in Browser Client
- **Join Call Action Bar**: Exposed WebRTC live voice channel controls directly in the browser client with hardware Acoustic Echo Cancellation (AEC) and noise suppression.
- **Microphone Mute & Push-to-Talk**: Guests can easily toggle their microphone or mute on demand.
- **Smart Voice Ducking Integration**: The browser client dynamically responds to host movie audio ducking (-25%), clearly prioritizing voice speech over background action scenes.

### 3. Dynamic Mobile Viewport (100dvh) & Reaction Bursts
- **Mobile-Responsive Dock**: Rebuilt the web layout using dynamic viewport height (100dvh) and flexible side-dock styling for landscape orientation, preventing virtual keyboards and mobile browser address bars from obscuring the chat composer.
- **Quick Reaction Emoji Strip**: Integrated one-tap reaction chips (Love, Epic, Laugh, Bravo, Cinema, Mindblown) triggering animated floating reaction bursts rising across the cinema canvas.
- **Real-Time Client Count Tracking**: Enhanced host session server to monitor active heartbeats and display the exact number of connected guests in real-time.

---

# Jadzumi v3.4.0 - Global WebRTC Watch Party, Audio Cinema Bridge, Episode Drawer & Micro-Scrubbing

A major feature release introducing Global WebRTC Watch Party with zero-install browser streaming and real-time voice call, Audio Cinema Bridge for dual-headphone synchronization, In-Player "Up Next" Episode Drawer, Variable Micro-Scrubbing precision, and Letterbox Subtitle Placement.

### 1. Global WebRTC Watch Party with Live Voice Calling & Reaction Chat
- **Zero-Install Browser Streaming**: Generate an instant shareable link and QR code from the player HUD. Remote friends on PC, Mac, iPhone, or Android can join by simply opening the link in their web browser without installing any app.
- **Bi-directional WebRTC Voice Call**: Integrated real-time Opus 48 kHz voice calling with hardware Acoustic Echo Cancellation (AEC) and noise suppression, allowing you to speak with friends while watching.
- **Smart Movie Audio Ducking**: The player DSP automatically dips the movie volume by 25% when voice speech is detected, ensuring conversations remain completely clear during action scenes.
- **Frame-Accurate Playback Synchronization**: Bi-directional WebRTC DataChannel clock syncs play, pause, seek, and buffer states with sub-second accuracy across all connected guests.
- **Live Reaction Chat**: In-player floating reaction feed and guest web chat bar for instant messaging without obstructing the video frame.

### 2. Audio Cinema Bridge (Dual-Headphone Synchronization)
- **Private Headphone Relay**: Stream uncompressed, zero-latency stereo audio directly over local Wi-Fi or mobile hotspot to a partner's phone and headphones.
- **Hardware Limitation Bypass**: Overcomes Android's dual-Bluetooth audio connection bottlenecks by broadcasting synchronized audio packets that any partner can receive via phone and AirPods.
- **Instant QR Pairing**: Partners connect in seconds by scanning the Audio Bridge QR code directly off the tablet or phone screen.

### 3. In-Player "Up Next" Episode Drawer
- **Seamless Episode Browsing**: Access the season episode queue directly from the player HUD utility row or Cinephile Tools menu without navigating back to the library.
- **Visual Episode Cards**: Displays episode thumbnails, season and episode tags (e.g. S01E03), running times, and an active glowing playback indicator.

### 4. Variable Micro-Scrubbing Precision
- **Multi-Tier Seek Velocity**: Dragging downward vertically while scrubbing along the timeline downshifts seek velocity through 4 discrete precision rates (100% at under 25px, 50% at over 25px, 25% at over 60px, and Fine 1-second ticks at over 110px).
- **Tactile Feedback & Visual Badge**: Provides tactile haptic feedback ticks on precision gear shifts and displays a floating micro-scrubbing rate pill directly above the seeker.

### 5. Letterbox Subtitle Placement
- **Unobstructed 2.39:1 Cinema Viewing**: Added a toggle in Subtitle Settings that dynamically reparents subtitles into the lower black letterbox bars on widescreen films, preventing text from overlapping video action.

---

# Jadzumi v3.3.8 — Unified Category Segmented Capsule, Diffused Acrylic Canvas & Purified Player HUD

A fidelity and interface refinement release delivering the unified category segmented capsule, fixing background circular gradient banding with true seamless acrylic diffusion, and purifying the in-player Quick Menu per videophile reference standards.

### 1. Unified Category Segmented Capsule
- **Single Segmented Control**: Replaced individual floating category pills with a single unified 38dp frosted milk-glass capsule housing Movies, Series, and Anime tabs.
- **Physics-Based Sliding Thumb**: Integrated an animated selection pill thumb using Jetpack Compose spring physics (damping ratio 0.85, stiffness 380), gliding seamlessly between segments with zero text jump.
- **Translucent Milk Glass Styling**: Styled with frosted milk-glass background (`0xF2FFFFFF` in Light, `0xFF1E1A29` in Dark), hairline lilac contour rim (`0x286750A4`), and dynamic spot elevation.

### 2. Diffused Acrylic Canvas & Anti-Banding Architecture
- **Eliminated Hard Circle Clipping**: Replaced geometric `drawCircle` primitives with full-bleed `drawRect` covering the entire viewport, preventing visible perimeter arcs and circle boundaries on wide displays.
- **Hue-Preserving Zero-Alpha Falloff**: Fixed the dark grayish banding ring caused by Compose `Color.Transparent` black interpolation by fading each gradient color to its own hue at alpha 0.0f (`Color.copy(alpha = 0f)`).
- **4-Stop Gaussian Multi-Mesh**: Layered subtle lilac, glacier cyan, and warm champagne gradients with 4-stop Gaussian falloffs across Light and Dark themes for genuine physical frosted glass depth.

### 3. Purified Videophile Player Quick Menu (Option A)
- **Eliminated Distortive Post-Processing**: Purged the "Video & Color Studio" shader controls to prevent artificial highlight clipping, crushed shadow detail, and color space distortion over master SDR and HDR video streams.
- **Removed Legacy Audio Virtualizer**: Removed "Spatial Cinema 3D" to prevent phase smearing, comb filtering, and muffled dialogue caused by Android's legacy audio effect, preserving clean stereo and multichannel downmix fidelity.
- **Streamlined Cinephile Tools**: Removed in-playback frame palette extraction clutter while keeping core media playback essentials: Subtitle Track & Search, Audio Track & Language, Dialogue Voice Clarity (Vocal Boost), Playback Diagnostics (Nerd Stats), Aspect Ratio, Playback Speed, Chapters & Acts, and Lossless Scene Clipper.

---

# Jadzumi v3.3.7 — Living Light Acrylic Canvas, Dynamic Poster Monet & Precision HUD Alignment

A visual atmosphere and ergonomics release delivering the Living Light Acrylic Canvas architecture, system-wide Material Expressive frosted glass surfaces, dynamic film poster Monet color extraction, upper-right screen orientation rotation in the player HUD, and duplicate control cleanup.

### 1. Living Light Acrylic Canvas Architecture
- **Multi-Mesh Iridescent Canvas**: Replaced flat, stark white paper backgrounds with a living multi-mesh optical canvas composed of soft Lilac (`#D8B4FE` at 28%), Glacier Sky Cyan (`#BAE6FD` at 32%), and Warm Peach (`#FED7AA` at 22%) on an architectural base (`#F6F5FA`).
- **Dark & AMOLED Parity**: Refined the Dark theme multi-mesh with Muted Iris and Oceanic Cyan ambient gradients, while AMOLED remains pitch-black (`#000000`) for infinite contrast.
- **Material Expressive Frosted Milk Glass**: Updated cards, segmented navigation capsules, and category chips to translucent milk glass (`0xF0FFFFFF` and `0xE0FFFFFF`) with hairline specular contours (`0x286750A4`) floating with zero elevation drop shadows.

### 2. Dynamic Poster Monet Palette Adaptation
- **Deterministic 144-Point Chroma Sampler**: Implemented zero-dependency poster color extraction with saturation scoring and luminosity bounding, filtering out near-blacks and washed-out highlights.
- **Cinema Details Aura**: Movie details and TV show hero banners project an ambient aura matching the film's artwork (e.g. Solar Amber for Dune: Part Two, Gothic Cyan for Corpse Bride, Emerald for Beware Boiuna).
- **Adaptive Primary CTA**: Primary Play and Resume buttons automatically inherit the poster's dominant chroma with high-contrast text typography.

### 3. Screen Orientation Rotation & Player HUD Alignment
- **HUD Orientation Control**: Added a dedicated 42dp frosted circular button on the upper-right utility row of the player HUD, cycling between Landscape Locked, Portrait Locked, and Sensor Auto with spring-animated rotation.
- **Control Deduplication**: Removed redundant Picture-in-Picture and 3-dots Quick Menu buttons from the bottom deck, ensuring clean alignment across the four 42dp frosted circular controls at the top right.
- **Circular Play Button Interaction**: Fixed the squarish gray box bounding artifact during center play button clicks by using a bounded Surface container and radial circular touch bursts.

### 4. Uncongested Mobile Phone Top Deck
- **Fluid Collapsible Search**: Standardized an uncongested two-row mobile header with a segmented Home/Settings capsule on the left, compact 38dp circular actions on the right, and thumb-friendly horizontal category chips on row two.
- **Full-Width Expansion**: Search input smoothly expands full-width across row one upon tap, with clear and close microinteractions.

---

# Jadzumi v3.3.6 — HUD Alignment, Collapsible Search Bar & Material Expressive Spec Sheet

A precision UX refinement release delivering symmetric 42dp player HUD controls, a fluid collapsible search bar resolving library header congestion, and the complete Material Expressive 2.0 Media Inspector spec sheet.

### 1. Symmetrical Player HUD & Control Alignment
- **Top-Right 42dp Utility Bar**: Standardized the upper-right utility buttons (PiP, Quick Menu, Screen Lock) to 42dp circular frosted controls, achieving visual balance and vertical baseline alignment with the 42dp Back button.
- **Deduplication of Controls**: Completely removed redundant PiP and 3-dots More buttons from the bottom deck, leaving the bottom deck clean and focused on stream chips (Subtitles, Audio, Speed, Aspect Ratio).
- **Circular Play Button Interaction**: Fixed the squarish gray box bounding artifact during center play button clicks by enforcing circular clip masks before touch interactions.

### 2. Fluid Collapsible Search Bar
- **Uncongested Header Layout**: Transformed the fixed search bar into a compact 38dp circular button when idle, giving ample breathing room to category pills and the segmented Home/Settings capsule.
- **Fluid Expansion**: Smoothly expands horizontally across the top bar upon tap or active query with full text input, clear text button, and dismiss action.

### 3. Material Expressive Media Inspector 2.0
- **Compact Spec Sheet Architecture**: Replaced the bulky four-card bento grid with a single unified Material Expressive spec sheet (Concept 4A).
- **Categorized Sections**: Clean key-value breakdown covering Video Stream, Audio & Acoustics, Subtitle Studio, and Container & File metrics separated by subtle 0.75dp hairline dividers.
- **Design Token Compliance**: Fully conforms to GlassTheme design tokens with 0dp flat elevation, high-contrast readable typography, and dynamic resolution quality badges.

---

# Jadzumi v3.3.5 — Integrated Bottom Deck Architecture & Redesigned Display Settings

A player ergonomics and customization release delivering the approved Approach B Integrated Unified Bottom Deck, eliminating top action pill clutter, and providing full telemetry customization options under settings.

### 1. Approach B: Integrated Unified Bottom Deck
- **Top Bar De-Cluttering**: Eliminated the crowded top-right action capsule. The top bar is now completely clean and video-centric, displaying only the back button and title details on the left, with minimal circular PiP and screen lock buttons on the right.
- **Unified Bottom Control Deck**: Merged media track and playback controls directly into the bottom scrubber deck inside a cohesive, two-row frosted glass deck with 22dp corner radius, 0dp elevation, and 0.75dp hairline border.
- **Integrated Action Chips**: Quick Subtitle, Audio, Speed, and Aspect chips sit directly below the timeline scrubber, providing natural thumb access in landscape orientation without reaching to the top of the screen.
- **Connected Track Sheets**: Tapping Subtitles or Audio seamlessly opens the bottom track picker sheet with instant selection and smooth transition physics.

### 2. Redesigned "Display During Playback" Customization Screen
- **Telemetry HUD Opacity**: Live 20% to 100% slider with real-time percentage readout and descriptive caption.
- **Items to Display**: Dedicated switch toggles for Remaining Playback Time, Current Clock Time, and Battery Percentage & Level.
- **HUD Style Presets**: Choose between Frosted Glass (rounded translucent capsule), Pure Text (borderless text with drop shadow), and Hairline (subtle rectangular notch).
- **Placement Position**: Position the telemetry overlay in Top Right, Top Left, or Bottom Left with live alignment and safe drawing padding.

---

# Jadzumi v3.3.4 — Pure Borderless Action Pill, In-Player Telemetry Control, Flat Quick Menu & Dynamic OTA

A refinement release delivering the strict borderless Variation 3B action pill with luminescent accents, direct in-player telemetry HUD controls, a flat 0dp elevation quick menu drawer, and real-time dynamic OTA changelog parsing.

### 1. Pure Borderless Variation 3B Action Pill
- **Zero Inner Containers**: Removed all inner background boxes and individual button borders inside the upper-right capsule.
- **Luminescent Accents**: Borderless pure typography and vector icons floating directly on glass with active lavender glow (`#D0BCFF`).
- **Hairline Dividers**: Slim 15dp vertical hairline dividers (`0.75dp` stroke) providing clean separation.

### 2. In-Player Telemetry HUD Controls
- **Direct Quick Menu Integration**: Added a dedicated "Display during playback" row into the player's quick menu drawer.
- **Live Live Opacity & Indicator Tuning**: Adjust HUD opacity (20%-100%) and toggle Remaining Time, Clock, and Battery directly from within the video player without exiting to settings.

### 3. Pure Flat Quick Menu Drawer
- **Flat Living Elevation**: Standardized the quick menu drawer to 0dp elevation and 0.75dp hairline borders across all categories and sub-menus.

### 4. Dynamic Real-Time OTA Changelog
- **Live GitHub Release Parsing**: Replaced static release cards with a real-time Markdown parser that extracts key changes directly from GitHub release notes into clean, categorized bullet tags.

### 5. Concept A Laser Trace Splash Screen
- **App Logo Fusion**: Integrated the official Jadzumi Adaptive Glass Playhead Icon with the Concept A dynamic vector laser stroke trace and breathing ambient aura.

---

# Jadzumi v3.3.3 — Luminescent Morphing Pill, Telemetry Opacity & PiP Desync Fix

A major player ergonomics, visual elegance, and system stability release introducing the Variation 3B Luminescent Action Pill with dynamic downward in-pill menu morphing, telemetry HUD adjustable opacity slider, and robust Android Picture-in-Picture window-manager desync resolution.

### 1. Variation 3B Luminescent Action Pill & Dynamic In-Pill Morphing
- **Variation 3B Luminescent Typography & Accents**: Replaced filled solid chip backgrounds in the upper-right capsule with sleek, borderless Luminescent lavender typography and active icon accents (`#D0BCFF`). Keeps the top player HUD airy, non-distracting, and pristine in both dark and light modes.
- **Dynamic In-Pill Morphing Expansion**: Eliminated detached right-side flyout cards for Audio and Subtitles. Selecting Audio or Subtitles smoothly morphs the upper-right capsule directly downward into a rounded 20dp track picker using system-wide living motion springs (`dampingRatio = 0.85f`, `stiffness = 400f`), collapsing seamlessly upon track selection.
- **Direct Track Selection**: Instant track switching with soft lavender checkmark indicators and dedicated subtitle toggle ("Off").

### 2. Picture-in-Picture (PiP) Window Desync Fix
- **Stale Video Bounds Guard**: Resolved the bug where reopening or switching videos and launching PiP caused the window to collapse to 0x0 and vanish into a background notification. Added strict dimension validation (`width > 100 && height > 100`) to prevent invalid window-manager source rect hints.
- **PiP State Reset on Open**: Explicitly reset `inPip = false` whenever initiating playback of a new or reopened video in `PlaybackController`.
- **Instant Control Overlay Dismissal**: Automatically hides on-screen overlay controls (`controls = false`) immediately before invoking `enterCinemaPip()` to avoid transition collisions.

### 3. Display During Playback Redesign & Adjustable Opacity
- **Live Opacity Slider**: Added an interactive 20% to 100% opacity slider with live percentage readout in `DisplayDuringPlaybackDialog`, persisted across sessions via SharedPreferences.
- **Clean Essential Telemetry**: Streamlined telemetry to the essential indicators: Remaining Time, Current Clock, and Battery Percentage (Format and HDR badges cleanly separated from the telemetry HUD).
- **System-Wide Flat Living Geometry**: Updated dialog container and telemetry status pill to 20dp curvature, hairline 0.75dp border, and 0dp elevation.

### 4. Player Micro-Details Polish
- **Volume & Brightness Gestures (`FluidGestureCapsule`)**: Updated vertical swipe capsules to 0.75dp hairline border and 0dp elevation.
- **Hold-to-Fast-Forward 2.0x Micro-Pill**: Flat 0dp elevation with 0.75dp border.
- **Timeline Scrubbing Thumbnail Tooltip (`ScrubPreview`)**: Aligned to flat 0dp elevation and 0.75dp border.

---

# Jadzumi v3.3.2 — System-Wide Flat Card Architecture, Symmetrical 32dp Action Pill & Light Theme Typography Contrast

A design system harmonization and visual fluidity release extending the pure flat Material 3 Expressive architecture across all library rails, continue-watching shelves, and player controls, ensuring strict alignment, clean typography, and zero embossed artifacts across Light, Dark, and AMOLED themes.

### 1. System-Wide Flat Clean Architecture on Homepage
- **Rail Card Standardization (`CleanRailPosterCard`, `CleanRailSeriesCard`, `CleanRailAnimeCard`)**: Upgraded to 20dp corner radius, hairline 0.75dp muted contours (8% alpha in Dark/AMOLED, 16% stone in Light), and zero shadow elevation (elevation = 0dp), perfectly matching the Settings tab cards.
- **Intelligent Title Sanitization**: Replaced raw, dot-separated filenames (e.g. `Beware.Boiuna.2026.1080p.1...`) with cleanly parsed media titles (e.g. `Beware Boiuna (2026)`) and high-contrast dual-tier typography (`onSurface` primary titles, `onSurfaceVariant` secondary metadata).
- **Continue Watching Shelf Alignment (`CleanContinueWatchingCard`)**: Fully unified Continue Watching cards to 20dp rounded geometry, 0.75dp hairline borders, zero elevation, and clean title parsing.
- **Top Segmented Capsule & Material Pills**: Zeroed out shadow elevation on the Home/Settings segmented navigation capsule (`JadzumiSegmentedNavCapsule`) and global tactile pills for a cohesive, flat aesthetic.

### 2. Symmetrical 32dp Player Dynamic Action Pill
- **Uniform Height & Touch Targets**: Locked all player top-right action buttons (`Audio`, `Subtitles`, `Speed`, `Aspect Ratio`, `PiP`, `More`, and `Screen Lock`) to an exact 32dp height.
- **Circular Icon Touch Targets**: Standardized icon-only buttons (`PiP`, `More`, `Screen Lock`) to symmetrical 32dp x 32dp circular shapes (`CircleShape`), eliminating oblong stretching.
- **Harmonized Typography & Dividers**: Aligned chip typography to 11.5sp semi-bold text with balanced 10dp horizontal padding and subtle 16dp hairline vertical dividers.

---

# Jadzumi v3.3.1 — Clean Flat Material 3 Expressive Architecture, Upper-Right Dynamic Action Pill & Subtitle Unobstructed Baseline

A design system refinement release delivering a pure flat Material 3 Expressive architecture (elevation = 0dp), an upper-right dynamic action pill, an edge-to-edge baseline hairline scrubber with zero subtitle collision, and hairline 0.75dp muted contours across Dark, AMOLED, and Light themes.

### 1. Pure Flat Material 3 Expressive Architecture
- **Global Elevation Zero**: Eliminated artificial Android drop shadows across cards, panels, and surfaces in Dark and AMOLED modes. Surface depth is communicated purely through flat tonal luminance shifts (`surfaceContainerLowest` to `surfaceContainerLow`).
- **Muted Hairline Contours (0.75dp)**: Replaced high-contrast specular white outlines with subtle, uniform hairline strokes (8% alpha in Dark/AMOLED, 16% stone in Light) to remove 3D beveled rims and embossed artifacts.
- **Unified Corner Geometry Scale**: Standardized corner radii across the app (`8dp` for badges/switches, `20dp` for cards and panels, `CircleShape` for action pills).

### 2. Upper-Right Dynamic Action Pill & Baseline Scrubber
- **Upper-Right Consolidation**: Relocated all primary stream actions (`Audio`, `Subtitles`, `Speed`, `Aspect Ratio`, `PiP`, `Settings`, and `Screen Lock`) into a sleek, non-intrusive floating micro-pill in the upper-right corner.
- **Hairline Baseline Scrubber**: Replaced bulky split bottom capsules with a minimal edge-to-edge baseline timeline track.
- **Uncompromised Subtitle Freedom**: Decoupled the bottom deck from subtitles, ensuring subtitles have 100% vertical headroom with zero overlap or border collision.
- **Center Ghost Seeking Preserved**: Maintained the user-approved 48dp ghost outline play/pause ring flanked by 10s back and forward steppers.

---

# Jadzumi v3.3.0 — Precision Audio & Subtitle Sync Studio, Sonic Pitch Preservation, Refined HUD 3 & SQLite Watch Intelligence

A focused playback fidelity, audio-video synchronization, and data layer release delivering real-time lip-sync calibration, NextLib live subtitle delay, natural Sonic pitch preservation, refined HUD 3 compact pills, and native SQLite watch intelligence.

### 1. Precision Audio & Subtitle Sync Studio
- **Real-Time Audio Lip-Sync Calibration**: Dedicated circular delay audio processor (`LipSyncAudioProcessor`) providing instant `+/- 50ms` tactile nudges to eliminate Bluetooth headset and soundbar latency without stream reloads.
- **Live Subtitle Offset Steppers**: Built-in NextLib timing synchronization (`subtitleDelayMilliseconds`) with tactile `+/- 100ms` nudges and instant offset readout.
- **Sonic Audio Pitch Preservation**: Guaranteed natural voice pitch across all playback speeds (0.25x to 3.0x) via Media3 Sonic processor configuration (`PlaybackParameters(speed, 1.0f)`).

### 2. Refined HUD 3 Cinephile Deck & Uncluttered Header
- **Concise Audio & Subtitle Badges**: Compact industry format tags (`E-AC-3 5.1`, `TrueHD 7.1`, `DTS 5.1`, `AAC 2.0`) eliminating oversized pill chips.
- **Uncluttered Top Bar**: Purged redundant decoder chips for an elegant, distraction-free live stream telemetry deck.
- **Strict Text Anti-Wrapping**: Hardened horizontal layout ensuring action labels (`Fit`, `Zoom`, `1.0x`) never vertically wrap into letters.

### 3. Native SQLite Watch Intelligence (Data Layer)
- **Structured Database Migration**: Replaced flat SharedPreferences storage with an indexed native Android SQLite database (`WatchIntelligenceDatabase`), providing robust millisecond resume points, completion tracking, and watch history records.

---

# Jadzumi v3.2.0 — Cinephile Command Deck HUD 3, Pinch-to-Zoom, OkHttp Engine, Frame Stepping & Morphing Updater Capsule

A major performance, playback controls, and UI fluidity release delivering HUD 3 Cinephile Command Deck, continuous pinch-to-zoom with 2D pan dragging, frame-by-frame pause stepping, OkHttp network streaming engine, Smart Binge pre-buffering, and Concept 2A Morphing Updater Capsule.

### 1. Playback HUD 3: Cinephile Command Deck
- **Style C Ghost Minimal Seeking**: 48dp ghost outline play/pause ring flanked by 10s back and forward steppers, complemented by dedicated single-frame stepping controls on pause.
- **Top Live Stream Telemetry Deck**: Real-time video codec identification (HEVC Main 10, H.264 High, AV1 Main, VP9), native resolution, exact fractional frame rate (24.000 fps), and HDR flavor badge (HDR10+, Dolby Vision, BT.709 SDR).
- **Decoder Hardware/Software Chip**: Instant status pill indicating hardware acceleration (`MediaCodec HW+`) or software fallback (`NextLib SW`).
- **Precision Split Capsules**: Left Scrubber Timeline Capsule (elapsed, total, percentage, precision scrubber) paired with Right Stream Actions Capsule (audio track selector, subtitle track selector, playback speed, aspect ratio mode, screen orientation lock).

### 2. Continuous Pinch-to-Zoom & Pan
- **Fluid 1.0x to 5.0x Scaling**: Continuous two-finger pinch-to-zoom with real-time 2D pan dragging and boundary clamping.
- **Double-Tap Fit Reset**: Double-tapping the viewport while zoomed immediately resets the canvas to 1.0x Fit.
- **Transient Zoom Feedback**: Sleek on-screen zoom indicator chip displaying active magnification percentage.

### 3. Precision Frame-by-Frame Stepping
- **Exact Pause Stepping**: Dedicated previous and next frame step buttons rendered upon pausing playback, leveraging `SeekParameters.EXACT` calculated from runtime frame rate.

### 4. High-Performance OkHttp Network Engine
- **Media3 OkHttp DataSource**: Replaced standard HTTP data source with dedicated OkHttp client featuring HTTP/2 multiplexing, connection pooling, and optimized timeouts for local NAS and remote streaming.

### 5. Smart Binge Pre-Buffering
- **Zero-Latency Next Episode Queue**: Proactively preloads the queued episode stream during the credits countdown to achieve seamless, pause-free episode transitions.

### 6. Concept 2A: Morphing Cinema Capsule & Background Updater
- **Non-Intrusive Cinema Capsule**: Collapsed bottom floating island that keeps video playback completely interactive and unobstructed.
- **Expandable Frosted Bottom Sheet**: Rich changelog with categorized feature badges, live transfer speed telemetry (MB/s), linear download progress bar, SHA-256 package verification, and seamless background APK downloading.

---

# Jadzumi v3.1.0 — Multi-Tier Artwork Resolver, Atmospheric Canvas, Unified Cards, Cleaned Settings & Smart Binge Engine

A major feature and design system release delivering the Multi-Tier Poster Artwork Resolver, Direction A Adaptive Atmospheric Canvas, universal Material 3 Expressive surfaces across all screens, a streamlined 4-group Settings Hub, and the Smart Hybrid Binge Engine.

### 1. Multi-Tier Artwork Resolution Engine
- **Universal Poster Scraper**: Solved the missing artwork issue where cards rendered blank gray gradient rectangles on the library screen. Library cards now resolve posters immediately without requiring users to tap into details pages.
- **Asynchronous 4-Tier Pipeline**:
  - Tier 1: Local sidecar image scraping (`poster.jpg`, `cover.jpg`, `folder.jpg`, `.nfo`).
  - Tier 2: Instant in-memory and disk cache checks for TMDB/Cinemeta and AniList online posters.
  - Tier 3: Non-blocking background coroutine prefetching for indexed library media.
  - Tier 4: Hardware thumbnail extraction fallback, backed by `CinematicFallbackPoster` with dynamic palette detection and styled typography so no card ever remains blank.
- **Opt-Out Online Scraper**: Online metadata and poster scraping is now active out of the box with user privacy controls.

### 2. Direction A: Adaptive Cinematic Atmospheric Canvas
- **Calm Architectural Backdrop**: Replaced the floating `PortalHalo` radial gradient aura blobs with an adaptive, non-distracting atmospheric canvas:
  - Light Mode: Crisp architectural paper (`#F8F7FC`) with subtle top micro-vignette.
  - Dark Mode: Deep obsidian charcoal (`#0E0C15`) with gentle vertical depth falloff.
  - AMOLED Mode: True pitch black (`#000000`) for infinite contrast and zero OLED power consumption.

### 3. Universal Material 3 Expressive Design System
- **Unified Card Hierarchy**: Replaced legacy `SpatialGlass` card implementations across `MovieDetailsPage` and `TvShowDetailsPage` with unified `MaterialExpressive.cardBg`, `MaterialExpressive.subCardBg`, and `MaterialExpressive.cardBorder` tokens.
- **Harmonized Geometry**: Standardized 16dp corner radius across all library rail and grid cards, and 20dp corner radius on details hero banners, dialogs, and panels.

### 4. Reorganized 4-Group Settings Hub & Gimmick Purge
- **Clean 4-Group Architecture**: Consolidated fragmented settings categories into a clean, predictable hierarchy:
  1. Playback & Decoders: MediaCodec hardware acceleration, Auto Frame Rate (AFR) display matching, resume playback rules, and background playback.
  2. Audio & Subtitles Studio: Multi-track audio management, night mode voice boost, Spatial Audio virtualizer, dual subtitle studio, typography styling, and OpenSubtitles API integration.
  3. Appearance & Controls: Curated theme engine (Light, Dark, AMOLED, Dynamic Material You), 6% shared-axis motion physics, touch gestures, and orientation lock.
  4. Storage & About: Storage breakdown analyzer, indexed media folder management, blacklist/excluded paths, cache cleanup, app identity, and GitHub repository links.
- **Gimmick Removal**: Purged halo sliders, halo palette selectors, splash replay button, theme bloom switch, corner style picker, and bottom bar style picker.

### 5. Smart Hybrid Binge Engine (Up Next Auto-Queue)
- **Deterministic Multi-Tier Detection**: Automated credit detection without heavy AI model overhead:
  - Tier 1: Embedded chapter metadata inspection (`credits`, `ending`, `outro`, `ed`, `staff`).
  - Tier 2: 90-second Anime ED rule anchoring credit detection at `duration - 90s` for TV anime.
  - Tier 3: Real-time subtitle cue scanner monitoring production credit keywords (`directed by`, `starring`, `written by`, `executive producer`, `theme song`, `cast`, `produced by`, `music by`, `created by`) during the final 10% of playback.
  - Tier 4: Dynamic runtime fallback at 92% duration or custom outro window.
- **Minimal Floating Corner Prompt**: Replaced intrusive full-screen countdown overlays with a sleek floating corner chip (*"Up Next: S01E03 starts in 10s · [Play Now]"*) with an instant dismiss action.

---

# Jadzumi v3.0.1 — Hotfix: Series Navigation, Category Stutter Elimination & Clean Rails Catalog Activation

A targeted performance and navigation hotfix resolving Series tab interaction, eliminating category switching jank, and fully activating the locked v3.0 Clean Rails catalog layout.

### 1. Series Tab Navigation Fix
- **Identifier Mismatch Resolution**: Resolved state normalization discrepancy between Series and TV Shows tab identifiers in navigation capsule and library page listeners.
- **Bi-Directional State Synchronization**: Tapping the Series tab now consistently switches to and maintains focus on TV Series collections without resetting to Movies.

### 2. Category Transition Performance Optimization
- **Virtualized Lazy Content**: Eliminated non-lazy chunked item measurement during shared-axis slide transitions. All categories now utilize memoized chunked rows virtualized directly by Compose.
- **Pre-Computed Category Slices**: Hoisted and memoized independent video subsets for Movies, TV Shows, and Anime outside animation blocks, eliminating heavy re-filtering and GC churn on the main thread during tab transitions.
- **Scoped Animation Keys**: Switched animated transition evaluation to scoped category target keys, ensuring outgoing and incoming views render smoothly without layout thrashing.

### 3. Clean Rails Catalog Flow (Direction 1A) Activation
- **Top Continue Watching Shelf**: Top shelf now features clean 16:9 widescreen cards with progress percentages, remaining duration timecodes, instant play capsules, and purple accent progress tracks.
- **Universal 2:3 Poster Grid**: Replaced legacy grid components with clean 2:3 vertical poster cards styled with Google Pixel Material 3 Expressive tokens, 16dp rounded corners, and subtle borders.
- **First-Class Anime Support**: Added animation studio chips (MAPPA, Ufotable, Wit Studio, etc.) and dual-title support.
- **TV Series Rails**: Displays dedicated season counts and total episode counts across collections.

---

# Jadzumi v3.0.0 — Ground-Up Architecture Refresh & Unified Living Motion

A landmark release transforming Jadzumi Video Player from the ground up: unifying the app under a single 6% Shared-Axis Spring Motion signature, introducing the Clean Rails Flow library catalog, the Cinephile Stream Deck details page, the hierarchical Material 3 Expressive Settings Hub, and the refined Style C Ghost Seeking playback HUD.

### 1. Unified Living Motion Engine
- **Universal 6% Shared-Axis Spring Drift**: Standardized navigation transitions across the entire app on a 6% horizontal spring drift with cubic-bezier easing (`0.22, 1.0, 0.36, 1.0`).
- **Seamless Surfaces**: Governs Home to Settings navigation, tap-to-switch category browsing across Movies, TV Shows, and Anime, and drill-down navigation into sub-settings pages.
- **Gesture Decoupling**: Replaced full-screen horizontal page dragging with pure tap-driven spring motion, eliminating conflicts with vertical scrolling.

### 2. Unified Library Catalog: Clean Rails Flow
- **Zero Resolution Fragmentation**: Completely eliminated arbitrary 4K, 1080p, and 720p quality divider tabs. All media resides together naturally in its designated category.
- **Direct Continue Watching Shelf**: Opens directly with an interactive Continue Watching shelf with exact watch progress lines, remaining duration, and instant play capsules, removing redundant hero resume banners.
- **Universal 2:3 Poster Grid**: Standardized 2:3 vertical poster cards with 16dp rounded corners and consistent typography across Movies, Shows, and Anime.
- **First-Class Anime Support**: Authentic dual-title presentation with English and Japanese Romaji/Kanji, animation studio tags, and dual-audio format chips.
- **Purged Offline Clutter**: Removed all redundant "100% offline" and sidecar warning notices from browsing catalog cards.

### 3. Media Details: The Cinephile Stream Deck
- **Purged Clutter**: Removed cast rosters, external YouTube trailer links, and generic bookmarks.
- **Offline Audio & Subtitle Track Pre-Selector**: Inspect and pre-select audio channels (e.g. 5.1 Surround vs. Commentary) and subtitle streams (ASS Styled vs. Off) before starting playback.
- **Technical Stream Specs Bento Grid**: Real-time readout of MediaCodec HW+ status, video profile, resolution, color space (HDR10+ / BT.2020), and bitrate.
- **Dual Play Dock**: High-contrast primary Resume capsule with integrated progress bar alongside a dedicated Start Over button.
- **100% Adaptive Light & Dark Engine**: Pixel-perfect Material 3 Expressive semantic tokens with deep contrast across both themes.

### 4. Settings & Sub-Settings: Hierarchical Hub
- **Grouped Category Cards**: Replaced the 1,500-line monolithic filter-chip row with clean grouped cards: Playback & Engine, Controls & Interface, and System & About.
- **6% Shared-Axis Drill-Down**: Silky forward slide into focused sub-settings pages and reverse slide on back navigation.
- **Device & Storage Meter**: Real-time storage meter breaking down media consumption across Movies, TV Shows, and Anime.
- **Dedicated Offline Privacy Documentation**: Official offline architecture information centralized in Settings > About.

### 5. Now Playing HUD: Style C Ghost Minimal Seeking
- **Ghost Outline Seeking Controls**: Replaced bulky chapter buttons with borderless, minimalist vector seek icons and a delicate translucent outline play/pause ring.
- **Top Stream Telemetry**: Header displays video codec profile, bitrate, HDR10+, and MediaCodec HW+ acceleration status.
- **Bottom Split Capsules**: Distinct Spatial Glass pods for timeline scrubbing and direct 1-tap audio/subtitle switching.
- **Spatial Glass 2.0**: Retains 100% frosted acrylic optics exclusively inside the video player HUD.

---

# Jadzumi v2.3.7 — Option A: Full Continuous Spring Slide Navigation

A motion physics release implementing Option A continuous spring sliding across Movies, Series, and Anime, perfectly matching the fluid shared-axis transitions of the Home and Settings tabs.

### 1. Full Continuous Spring Slide (`AnimatedContent`)
- **Directional Spring Translation**: Replaced page carousel paging with native `AnimatedContent` executing full screen-width directional spring transitions (`dampingRatio = 0.86f`, `StiffnessMediumLow`) combined with smooth fade curves.
- **Natural Directional Axis**: Navigating forward (*Movies -> Series -> Anime*) slides content cleanly from right to left; navigating backward (*Anime -> Series -> Movies*) slides smoothly from left to right.

### 2. Intuitive Swipe & Drag Gestures
- **Fluid Horizontal Gesture Recognition**: Integrated `detectHorizontalDragGestures` allowing you to swipe horizontally anywhere on the catalog to trigger directional spring sliding between categories.
- **Bi-Directional Pill Sync**: The top category pills instantly synchronize their active indicator when swiping between pages, and tapping any pill glides the catalog with spring velocity.

### 3. Preserved List Scroll Memory
- **Independent State Retention**: Each category (*Movies*, *Series*, *Anime*) continues to retain its own dedicated `LazyListState`, ensuring your scroll position in one section is never lost when sliding across to another.

---

# Jadzumi v2.3.6 — Google Pixel Material 3 Expressive UI & Spatial Glass Player Isolation

A major architectural and design refinement introducing Google Pixel Flagship Material 3 Expressive surfaces across all catalog, library, details, inspector, and settings screens, while keeping Spatial Glass optics dedicated exclusively to the Now Playing video player HUD.

### 1. Dual-Metaphor Architecture
- **Dedicated Player Spatial Glass**: Retained 100% of translucent Spatial Glass optics, specular hairline rims, and live video ambilight in the Now Playing playback HUD (Floating Island Scrubber, Center Control Capsule, Quick Island Menu, Gesture HUD, and Lock Pill).
- **Grounded Browsing Experience**: Replaced translucent glass outside the player with grounded, high-contrast Material 3 Expressive surfaces, eliminating poster glare, reflection artifacts, and box-corner clashing across the browsing experience.

### 2. Google Pixel Material 3 Expressive Design System
- **Tonal Container Hierarchy**: Implemented structured tonal containers (`surfaceContainerLowest` through `surfaceContainerHighest`) across Dark, Light, and AMOLED modes for clean, natural elevation stepping.
- **Pixel Capsule Pills (`MaterialExpressivePill`)**: Introduced 999dp full-capsule pill components with spring micro-press kinetics, tactile haptic feedback, and vibrant primary-container active fills with high-contrast typography.
- **20dp Squircle Container Cards (`MaterialExpressiveCard`)**: Standardized all media cards, hero cards, and settings panels on 20dp squircle geometry with subtle 0.75dp hairline contours and soft tonal elevation.

### 3. Screen-by-Screen Enhancements
- **Main Library & Search**: Search bar upgraded to a clean Material Expressive capsule with subtle outline contour; category pills (*Movies, Series, Anime*) powered by `MaterialExpressivePill`.
- **Movie & TV Show Details**: Subtabs (*Overview, Cast, Details, Related* / *Episodes, Cast, Specs*) upgraded to Material Expressive capsules; season selectors and episode cards upgraded to grounded tonal cards.
- **Media Inspector 2.0**: The 4 Bento technical cards now render in solid tonal containers with high-contrast spec chips, maximizing legibility for 4K HDR, audio channels, and bitrate metadata.
- **Settings & Dialogs**: App configuration panels upgraded to clean M3 Expressive surfaces with subtle 1dp/2dp elevation.

---

# Jadzumi v2.3.5 — Curved Specular Optics, Sharp Box Elimination & Continuous Horizontal Pager

A dedicated optical refinement and interaction release curving specular edge reflections naturally around pill/card arcs, eliminating child clipping bleed across all media containers, and introducing continuous swipe and sliding motion between Movies, Series, and Anime.

### 1. Curved Specular Rim Optics
- **Diagonal 45-Degree Refraction Vector**: Replaced flat perpendicular vertical gradients with an angled linear refraction vector, enabling specular catch-lights to wrap naturally around curved 20dp and 999dp corner arcs instead of cutting across horizontally.
- **Subtle Depth Profile**: Softened shadow elevations to a clean, subtle 2dp–4dp across all cards and pills, achieving a modern, unbloated glass aesthetic.

### 2. Elimination of Sharp Box Bleed
- **Strict Shape Clipping**: Enforced explicit shape clipping and hardware layer clipping across all card surfaces, posters, thumbnails, and hero banners, permanently preventing inner images and container backgrounds from bleeding square 90-degree corners past the rounded border stroke.

### 3. Continuous Sliding Carousel for Movies, Series & Anime
- **HorizontalPager Architecture**: Migrated the library catalog to a continuous HorizontalPager with native 1:1 finger swipe gestures between Movies, Series (TV Shows), and Anime.
- **Bi-Directional Pill Synchronization**: Tapping any top category pill smoothly scrolls the carousel to the selected category, and swiping between pages dynamically updates the active category indicator in real time.
- **Independent Scroll Position Retention**: Each category maintains its own dedicated list state, ensuring your scroll position in one category is never lost when swiping across others.

---

# Jadzumi v2.3.4 — Universal Spatial Glass UI & Tactile Micro-Optics

A comprehensive design language rollout elevating every card, capsule, subtab, and panel across the entire app with VisionOS-inspired Spatial Glass UI, tactile micro-press physics, and dual-tone optical parity in both Light and Dark modes.

### 1. Universal Spatial Glass & Tactile Micro-Optics
- **Tactile Micro-Press Kinetics**: Integrated physical spring micro-press compression and haptic tick feedback into interactive pills and cards via SpatialTactilePill and SpatialTactileCard.
- **Top Navigation Island**: Transformed the search bar into a floating Spatial Glass capsule (999dp) flanked by tactile circular Spatial Glass utility capsules for Storage Cleaner and Library Sync.
- **Pure Pill Architecture**: Standardized all filter chips, subtabs, and category selectors on pure 999dp pill geometry with dynamic specular hairline rims.

### 2. Dual-Tone Light & Dark Optical Parity
- **Light Mode Porcelain Acrylic**: Formulated light surfaces with Alabaster Porcelain (#F5FAF9FE) and high-contrast obsidian ink (#161224) achieving over 12:1 WCAG AAA legibility.
- **Dual-Tone Specular Borders**: Integrated top catch-light gradient to bottom subtle ambient refraction contour, eliminating washed-out edges in light themes while preserving deep obsidian clarity in dark/AMOLED modes.

### 3. Movie Details & TV Series Polish
- **Movie Details Page**: Upgraded the Hero Banner, Offline Sidecar badge, Fix Match pill, Overview/Related subtabs, and Related title cards to 20dp Spatial Glass cards with hairline specular borders.
- **TV Show Details Page**: Elevated ShowHeroCard, Episodes/Details/Inspector subtabs, Binge Automation pill, SeasonProgressCard, and UpNextSpotlightCard to unified 20dp Spatial Glass components.
- **Universal Media Inspector 2.0**: All four Bento engine cards upgraded with theme-adaptive specular rims and elevated tactile depth.

### 4. Local Library & Settings Elevation
- **Card Hierarchy Standardization**: Elevated WelcomeOnboardingCard, LocalPanel, CategoryQuickCard, StorageSummaryCard, LocalPosterGridCard, and FranchiseCard to 20dp rounded corners with specular rims.
- **Settings Panels**: Enforced 20dp Spatial Glass elevation across all settings panels, maintaining visual harmony between player HUD, library, and configuration pages.

---

# Jadzumi v2.3.3 — Spatial Glass UI, Tactile Micro-Optics & Player Refinements

A major visual and experiential upgrade bringing VisionOS-style Spatial Glass UI, tactile micro-press physics, unified pill architecture, and player usability refinements.

### 1. Spatial Glass UI & Tactile Micro-Optics
- **Central Control Island**: Grouped the central Rewind (-10s), Play/Pause, and Forward (+10s) controls into a unified floating Spatial Glass Capsule with specular hairline gradient rims, deep obsidian optical clarity, and spring-based micro-press compression.
- **Micro-Pills Unification**: Upgraded top metadata tags (4K HDR, HDR flavor) and Player Status Pill (Clock, Battery, Time Remaining) to pure 999dp micro-pills with optical specular rims.
- **Vertical Gesture HUD**: Replaced vertical cards with a VisionOS-style Spatial Optical Capsule with live percentage readout and glowing liquid lavender meter fill.

### 2. Color Integrity & Ambilight Purge
- **Zero Color Wash in Fit Mode**: Completely removed artificial ambilight/letterbox radial tints. Fit mode now renders 100% natural, deep, unwashed reference cinema colors identical to Zoom mode.
- **White Glare Bug Fixed**: Locked all player overlay controls to permanent dark optical acrylic (#13101F / #07050E on OLED), preventing library light theme leaks from blinding users during playback.

### 3. Usability & Player Flow Refinements
- **Instant Track Selection**: Selecting subtitle or audio tracks now immediately dismisses the drawer, letting users resume viewing without interruption.
- **Spatial Glass Unlock Pill**: Upgraded the lock screen overlay to a floating bottom pill with LockOpen icon and "Tap to Unlock" prompt.
- **Portrait Overflow Protection**: Added horizontal scroll safety to bottom action pills to guarantee zero button crowding or clipping on narrow portrait displays.
- **Quick Menu Polish**: Enforced permanent dark optical acrylic across the quick menu drawer, eliminated duplicate Brightness Boost entries, and upgraded the Diagnostics (Nerd Stats) capsule.

### 4. Icon Restoration & Shape System
- **Classic App Icon Restored**: Reverted launcher icon to the signature cobalt folded ribbon wing with front frosted play triangle.
- **Unified Corner Geometry**: Standardized app cards on 16dp and 20dp smooth corners, eliminating 0dp sharp rectangular bricks.

---

# Jadzumi v2.3.2 — Floating Pill Bottom HUD, Continuous PiP Flow & Acrylic UI

A landmark release refining the in-player experience with standalone floating pill controls, uninterrupted Picture-in-Picture playback, strict zero-emoji interface polish, and modern Acrylic glass styling.

### 💊 1. Separated & Near Pill-Shaped Bottom HUD
- **Standalone Scrubber Pill**: Replaced the bulky monolithic card with an independent floating capsule (`RoundedCornerShape(999.dp)`) housing the timeline scrubber, chapter markers, and precise timecodes.
- **Separated & Near Action Dock**: Directly below the scrubber, floating pill capsules provide 1-tap tactile control:
  - **Left Pill**: `[ Lock ]` screen protection.
  - **Right Pill Cluster**: `[ Subtitles ]`, `[ Audio ]`, `[ Speed ]`, and `[ Aspect Ratio ]` pills with active accent glows.
- **Lightweight Viewport**: Content breathes through between and around the floating pills, maximizing video visibility.

### 🖼️ 2. Continuous Picture-in-Picture (Zero Playback Interruption)
- **Lifecycle Protection**: Guarded `onStop()` lifecycle callback to prevent ExoPlayer instance destruction during background task switches and PiP transitions.
- **Seamless Flow**: Ensured playback never stops or stutters when entering or expanding back to fullscreen from PiP mode.
- **Stable Configuration**: Registered `keyboard|keyboardHidden|uiMode` configuration changes in `AndroidManifest.xml` to eliminate surface rebuilding on orientation and system flips.

### 💎 3. Enhanced Acrylic UI & Theatrical Backdrop
- **Dual-Specular Rims**: Border gradients with soft top-edge specular highlights catching scene light over frosted glass.
- **Theme Adaptability**: High-diffusion frosted surfaces harmonized across Light, Dark, and AMOLED (pitch-black obsidian) modes.
- **Atmospheric Horizon Wash**: Upgraded `PortalHalo` backdrop with top-down cinematic light wash and introduced the `Cinema Noir` palette for distraction-free pure OLED viewing.

### 🚫 4. Strict Zero-Emoji UI System
- Fully purged emoji prefixes across all Settings Bento headers, Media Inspector statuses, toasts, and trivia greetings in favor of clean Material 3 vector icons and polished typography.

---

# Jadzumi v2.3.1 — Universal Media Inspector 2.0 & Full App Unification

The comprehensive architectural unification release ensuring complete visual cohesion, single-source-of-truth data presentation, and zero duplication across all Movie and TV Show details pages and Settings.

### 🔍 1. Universal Media Inspector 2.0 (Strict Zero Duplication)
- **Universal Shared Bento Component**: Created `UnifiedMediaInspectorCard` shared identically across both Movie Details and TV Show Details screens.
- **Dedicated 4-Card Bento Grid**:
  - **1. Video Engine & Codec**: Resolution (`1916 × 808`), Quality badge (`1080p FHD`), Codec (`HEVC / H.265`), and Hardware GPU Acceleration status.
  - **2. Audio Engine & Channels**: Audio stream format (`E-AC-3 Surround`), Channel configuration (`5.1 / 6CH`), Sample rate, and active tracks.
  - **3. Subtitle Studio**: Active tracks, AndroidX Media3 TextRenderer engine, and Dual Subtitle Studio status.
  - **4. Storage & Container**: Container format (`MKV / MP4`), File size (`4.48 GB`), Folder location, and exact filename.
- **Strict Single-Source-of-Truth**: Each data point lives in exactly one card:
  - Genre is kept strictly in the Hero subtitle (duplicate `GENRE` glance card eliminated).
  - Rating is kept strictly in the Hero badge chips (duplicate `RATING` glance card eliminated).
  - Duration is kept strictly in the Hero badge chips (removed from file overview).
  - Redundant floating audio/subtitle pills in TV Shows eliminated; all media streams unified into the Bento Inspector.

### 🏛️ 2. Subtab Architecture & Feature Parity
- **Movies**: Unified subtabs `[ Overview | Related ]`.
- **TV Shows**: Unified subtabs `[ Episodes | Media Inspector | Related ]`.
- **TV Shows Fix Match Added**: Added `[ Fix Match ]` action button to the TV Show Details top bar, allowing users to search online databases (TVMaze / AniList) and immediately apply official poster artwork and metadata corrections.

### ⚙️ 3. Settings Bento Polish & Developer Clutter Removal
- **Developer Documentation Removed**: Removed static architecture explainer cards (*Bento UI Layout*, *Soft Neumorphism*, *M3 Expressive*) from Display & Appearance, keeping settings strictly focused on user-actionable controls.
- **Audio Settings Bento Overhaul**: Reorganized legacy monolithic 8-divider list into 3 elevated Bento panels:
  - *⚡ Audio Output & Spatial 3D* (NextLib FFmpeg status, Stereo Downmix, Spatial Cinema 3D chips).
  - *🌙 Playback Audio Enhancements* (Screen-off background audio, Night mode voice boost).
  - *🎧 Wireless Lip-Sync Calibration* (Bluetooth delay compensation with tactile `[-50 ms]`, `[+50 ms]`, `[Reset]` chips).
- **Subtitle Settings Grouped**: Segmented into *Live Studio & Typography*, *Colors & Shading*, and *Subtitle Timing Offset* Bento panels.

### 🎛️ 4. In-Player HUD Toast Alignment
- Unified aspect ratio toggle feedback toast messages with Settings naming: `Fit (Original)` and `Crop / Fill (Zoom)`.

---

# Jadzumi v2.3.0 — Master UI/UX Overhaul, Subtle Acrylic & FLOW LOOP 🌌🎬✨

The definitive master release transforming Jadzumi with the unified Single-Row Cinema Bar, space-optimized Above-the-Fold Library, Bento Settings 2.0, zero-duplication Details architecture, Cinema Ambilight with full 4-theme adaptability, and the new **FLOW LOOP** official app icon.

### 🏛️ 1. Unified Single-Row Cinema Bar
- **Logo Replaced with Navigation**: Category pills `[Movies | Series | Anime]` moved directly into the top-left slot where "Jadzumi" was, flanked by the centered sliding `[Home | Settings]` capsule and floating Search + utilities.
- **Row 2 Completely Eliminated**: Reclaims ~52 dp of vertical viewport height and eliminates the large blank gap across the screen.

### 🎬 2. Space-Optimized Above-the-Fold Library
- **Hero Banner Removed**: Eliminates the redundant 200 dp recommendation banner.
- **Immediate Content Visibility**: **Continue Watching** moves directly under the top bar, with the full **Movie & TV Series Poster Grid (2 to 3 rows)** commanding the screen immediately upon launching without scrolling.
- **Settings Toggle**: Optional preference `Show Featured Hero Banner (On / Off)` in Settings (defaulting to Off).

### 🔍 3. Unified Details & Media Inspector 2.0 (Zero Duplication Policy)
- **Zero Redundant Boxes**: In TV Series, "Up Next" is strictly the first, highlighted card in the episode list (no standalone duplicate mini-card).
- **Consolidated Season Progress**: Season watch progress is embedded directly into the Season Selector Pill (`Season 1 · 2/5 Watched (40%)`), eliminating the separate progress card.
- **Media Inspector 2.0 Bento Grid**: Replaces raw SQL database text dumps with clean, structured Bento cards (`Video Stream`, `Audio Stream`, `Container & Storage`, `Hardware Diagnostics`).

### ⚙️ 4. Bento Settings Overhaul (Useful Details Only)
- **1-Tap Tactile Chips**: Replaces clunky sub-dialogs with direct in-card tactile chips for Video Scaling (`[ Fit | Zoom / Fill | Stretch ]`) and Decoder Mode (`[ Auto | Hardware Only ]`).
- **Zero Developer Jargon**: Stripped out raw device codenames (`Xiaomi nabu`, `SM8150`) and raw library names; replaced with clean glowing status badges (`● GPU Active (60/120 FPS)`).
- **Elevated Bento Cards**: Groups settings into 3 focused cards (*Hardware Acceleration & Engine*, *Screen & Aspect Ratio*, *Luminance, Ambilight & Overlays*).

### 🌌 5. Subtle Acrylic UI & Cinema Ambilight (4-Theme Adaptable)
- **In-Player Cinema Ambilight**: For 2.39:1 widescreen films on 16:10 / 16:9 displays, upper and lower letterbox bars softly radiate a gentle, dynamic ambient glow (8–12% opacity) matching the active video scene.
- **Full 4-Theme Adaptability**:
  - **Dark (Default)**: Deep Obsidian base with 10% ambient color bleed.
  - **AMOLED**: True `#000000` pitch-black preservation where pixels physically shut off to maximize battery.
  - **Light**: Soft tonal lavender/cream surfaces (`#F8F6FB`, `#EDE7F6`) and dark violet typography.
  - **System Default**: Automatically synchronizes with OS day/night mode transitions.
- **Hardware Performance**: GPU-only radial shaders with 0% CPU consumption and locked 60/120 FPS.

### 🎨 6. Official App Icon: FLOW LOOP
- **Approved Icon Emblem**: Symmetrical continuous Möbius ribbon loop wrapping a central acrylic play core on an adaptive soft squircle container.

---

# Jadzumi v2.2.3 — Living Motion Physics & Appearance Adaptability 🌌🎬✨

A major visual cohesion and motion release introducing the approved Jadzumi Living Motion animation system, fixing light-theme visual adaptivity, and ensuring permanent luminous high-contrast controls across Cinema HUD.

### ⚡ 1. Jadzumi Living Motion Animation System
- **Spring Physics & Timing Engine**: Implemented the twelve living motion rules and animation specifications:
  - **Button Press & Release**: 120 ms press compression ($1.0 \to 0.96$) with spring release ($0.96 \to 1.0$, Damping `0.75`, Stiffness `650`).
  - **Connected Sliding Navigation**: 380 ms target continuous spring physics (Damping `0.85`, Stiffness `350`).
  - **Cinema HUD Transitions**: Expressive 220 ms entrance (`fadeIn` + 8 dp slide) and 180 ms exit (`fadeOut` + 4 dp slide).
  - **Selectable Motion Presets**: Added **Subtle** (`1.00 / 600`), **Balanced** (Default `0.85 / 400`), and **Expressive** (`0.70 / 300`) presets with full support for Reduced Motion system override.

### 🎨 2. Light Theme Appearance Adaptability Fixed
- **Tonal Navigation & Category Surfaces**: In Light theme, `[Home | Settings]` and `[Movies | Series | Anime]` pills now dynamically adopt soft tonal lavender surfaces (`#ECE6F4`, `#EDE7F6`) and dark violet typography (`#5A526B`) instead of hardcoded dark charcoal containers.
- **Harmonious M3 Expressive Integration**: Seamless visual blending across light, dark, and AMOLED themes without awkward dark ink-blotches.

### 🎬 3. Permanent High-Contrast Luminous Cinema HUD
- **Theme-Decoupled Playback Text**: Video title ("Chandramukhi"), technical metadata, elapsed/duration timecodes, and bottom capsule control buttons are permanently luminous white (`#FFFFFF` / `#B79AF7`), ensuring 100% legibility over any video scene regardless of system Light/Dark settings.

---

# Jadzumi v2.2.2 — Refined Cinema HUD Capsule, Editorial Bento & Gestures 🌌🎬✨

A comprehensive polish update introducing the unified Cinema HUD floating capsule, floating modal bottom sheets for audio and subtitles, modern gesture controls, editorial movie details styling, and complete removal of Direct Stream.

### 🎬 1. Unified Cinema HUD Floating Bottom Capsule
- **Integrated Floating Dock**: Combines the precision timeline scrubber, time elapsed / total, and tactile essential action pills (`[🔒 Lock]`, `[💬 Subtitles]`, `[🎧 Audio]`, `[▶ 1.0x Speed]`, `[⛶ Aspect]`) into a single, elegant floating capsule with soft specular lavender depth (`Color(0xD91C1A25)`).
- **Floating Modal Sheets**: Subtitles and Audio now open as focused floating bottom cards hovering gracefully above the player controls rather than covering a third of the screen with a heavy side drawer.
- **Top Bar Polish**: Simplified the top-right toolbar to Picture-in-Picture (`PiP`) and More Options (`...`), eliminating redundant audio/subtitle icons.

### 👆 2. Modern Gesture Controls & Double-Tap Aspect Toggle
- **Double-Tap Aspect Toggle**: Double-tapping the center 30% of the screen instantly toggles between `Fit` (original aspect ratio) and `Zoom / Fill` (fills ultrawide/tablet displays without black bars), mirroring YouTube and modern media players.
- **Double-Tap Seeking**: Double-tapping the left 35% seeks backward; double-tapping the right 35% seeks forward.

### 🎨 3. Movie Details Page Bento Refinement
- **Color Consistency**: Removed the yellow horizon gradient and blue buttons. Replaced with the approved **Moonlit Lavender** design system (`LavenderAccent #B79AF7`, `Obsidian #101016`).
- **Clean Editorial Layout**: Play Movie buttons now prominently feature Moonlit Lavender with obsidian text. Removed all trace of the "Episodes" tab on standalone movies.

### 🧹 4. Complete Removal of Direct Stream
- Completely removed all direct stream network inputs, dialogs, and dependencies to preserve Jadzumi's focused identity as an elite local cinema player.

---

# Jadzumi v2.2.1 — Direct Stream Resolution & Clutter-Free HUD 🌌🎬✨

A precision fix and refinement update resolving Direct Stream network playback and eliminating non-functional gimmicks and clutter from the Cinema HUD playback experience.

### 🌐 1. Direct Stream Native Network Playback Fixed
- **Root Cause Resolved**: Replaced strict `ContentDataSource` with `DefaultDataSource.Factory` backed by `DefaultHttpDataSource` with cross-protocol redirect support. Direct Stream now reliably streams HTTP/HTTPS network URLs (HLS `.m3u8`, MPEG-DASH `.mpd`, progressive MP4/MKV) instead of failing with `ERROR_CODE_IO_FILE_NOT_FOUND`.
- **HLS & DASH Native Decoders**: Integrated `media3-exoplayer-hls` and `media3-exoplayer-dash` for seamless adaptive bitrate streaming.
- **Cleartext Traffic Support**: Enabled `android:usesCleartextTraffic="true"` to support non-SSL/LAN streams (e.g. home servers and local IPTV streams).

### 🎬 2. Gimmick & Clutter Removal from Cinema HUD
- **Cast Gimmick Removed**: Removed the dummy top-right Cast icon that only showed a placeholder message.
- **Redundant Bottom Pills Removed**: Eliminated the cluttered bottom pill row (`Chapters`, `Audio`, `Subtitles`, `Speed`, `Aspect`, `Fullscreen`).
- **Integrated Lock Control**: The `Lock` button is now seamlessly integrated into the timeline row, leaving the entire lower screen clean and 100% focused on video content.
- **Top-Right Audio Settings**: Linked the top-right tool icon directly to the comprehensive Audio drawer (track selector, audio sync delay, audio boost).
- **Codebase Cleanliness**: Removed obsolete prototype sample preview code from the codebase.

---

# Jadzumi v2.2.0 — Final Master Evolution: Cinema HUD 4.0 & Direct Stream 🌌🎬✨

The definitive master release transforming Jadzumi Player with the pixel-perfect **Cinema HUD 4.0**, native **Jadzumi Direct Stream** engine, streamlined 3-tab library navigation, and unified Reactive Matte architecture.

### 🎬 1. Cinema HUD 4.0 — Pixel-Aligned Playback Experience
- **Aligned with Approved Visual Design**: Matches the reference specification with pixel precision.
- **Top Bar**: Minimal floating toolbar with Back navigation, video title with vertical lavender accent indicator, metadata info pills (`2024 · PG-13 · 2h 46m`, `4K HDR`, `Dolby Vision`, `Dolby Atmos`), `Cast`, `Subtitles/PiP`, `Equalizer`, and `More Options`.
- **Center Controls**: Soft circular `↺ 10s` and `↻ 10s` tactile seek buttons flanking the morphing circular Play/Pause button with soft specular lavender rim and glow.
- **Vertical Gesture Pills**: Left Brightness capsule (Sun icon) and right Volume capsule (Speaker icon) with smooth reactive lavender fill.
- **Floating Bottom Capsule**:
  - Ultra-thin scrubbable timeline with chapter ticks and thumbnail scrubber tooltip.
  - Tactile action pills: `[🔒 Lock]` `[|◄ Chapters]` `[((·)) Audio]` `[💬 Subtitles]` `[▶ Speed 1.0x]` `[🔲 Aspect]` `[⛶ Fullscreen]`.
- **Cinema Focus Mode**: Graceful control timeout leaving the video 100% unobstructed.

### 🌐 2. Jadzumi Direct Stream (Native Media3 Engine)
- **Direct Network Stream Input**: Discreet toolbar button in the Home bar allowing instant playback of direct HTTP/HTTPS URLs (HLS `.m3u8`, MPEG-DASH `.mpd`, Progressive MP4/MKV) inside Cinema HUD 4.0.
- **Local History & Privacy**: Includes clipboard 1-tap paste, stream validation, and local recent stream history with zero telemetry.

### 🗂️ 3. Streamlined 3-Tab Library Navigation
- Category pills on Home now strictly feature **`Movies`**, **`Series`**, and **`Anime`** (with "Folders" tab cleanly removed from the home bar and managed via toolbar sync and Settings).
- State-hoisted category persistence guarantees you remain on your selected category when navigating into details and pressing Back.

---

# Jadzumi v2.1.1 — Bento UI, Soft Neumorphic Surfaces & Category Persistence 🌌🎬✨

A refinement release delivering category persistence across detail navigation, complete removal of the legacy "All" tab, and synthesis of Bento UI, Soft Neumorphism, and Material 3 Expressive motion.

### 📌 1. Category Tab Persistence Fixed
- **State Hoisting**: Fixed the issue where navigating into Movie or Series details and pressing Back jumped to the default tab. The active category is now hoisted as a persistent state in `JadzumiApp`, ensuring you stay on `Movies`, `Series`, `Anime`, or `Folders` when returning from any details screen.

### 🗂️ 2. "All" Tab Completely Removed
- Category navigation now features 4 clean, focused tabs: **`Movies`**, **`Series`**, **`Anime`**, and **`Folders`**.
- The **Movies** tab serves as the primary Home showcase, featuring the Bento Hero banner, real-time Continue Watching carousel, and standalone movie collection.
- Removed obsolete "Show all" buttons and unused category filters.

### 🍱 3. Bento UI + Soft Neumorphism + M3 Expressive Architecture
- **Bento UI Layout & Organization**: Asymmetrical, rounded content panels (`20.dp` curvature) for Featured Hero, Continue Watching, shortcuts, and Details cards; efficient grids for poster collections.
- **Soft Neumorphic Tactile Surfaces**: Gentle elevation (`3.dp` to `6.dp`) and soft monochromatic rims (`BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))`) on interactive controls, Play/Pause, switches, and sliders. No heavy Gaussian blur, glossy glares, or recessed text.
- **Material 3 Expressive Motion**: Physics-based spring curves (`Spring.StiffnessLow`, `dampingRatio = 0.82f`), morphing lavender selection capsules, responsive touch feedback (`.touchMotion()`), and smooth shared-axis directional transitions.
- Replaced the obsolete "Frosted Glass Depth & Curves" panel in Settings with the unified **Cinematic Hybrid Architecture** panel.

---

# Jadzumi v2.1.0 — Moonlit Lavender UI/UX & Cinema HUD 3.0 Refinement 🌌🎬✨

A major visual and interaction design release introducing the approved **Moonlit Lavender** design system, artwork-driven library browsing, streamlined movie details, and pixel-aligned Cinema HUD 3.0 playback controls.

### 🎨 1. Moonlit Lavender Cinematic Design System
- **Approved Palette Integration**: Replaced cold blues and harsh greys with the mature **Moonlit Lavender** palette:
  - `Obsidian` (`#101016`) & `Charcoal` (`#1C1A25`) atmospheric deep canvas surfaces.
  - `Surface` (`#292535`) elevated containers with soft neumorphic depth.
  - `LavenderAccent` (`#B79AF7`) & `MoonlitHighlight` (`#E8DDFB`) for responsive interactive accents.
  - `Primary Text` (`#F4F1FA`) & `Secondary Text` (`#A7A3B4`) for clean, effortless readability.
- **Subtle Neumorphic Tactile Depth**: Restrained soft lighting and gentle borders on interactive buttons and controls without excessive blur or heavy glassmorphism.

### 🖼️ 2. Artwork-First Featured Banner & Continue Watching
- **Featured Hero Banner**: Replaced empty grey gradient backdrops with full cinematic poster/backdrop artwork and subtle dark gradient overlays for maximum contrast and legibility.
- **Refined Continue Watching**: Thinner 2.5dp progress bars, zero empty grey dead space, 16:9 artwork-focused thumbnail cards, and clean typography.
- **Compact Category Navigation**: Ergonomic Material 3 Expressive pills (`All`, `Movies`, `Series`, `Anime`, `Folders`) replacing oversized segmented bars with smooth horizontal scrolling.

### 📽️ 3. Movie Details & Inconsistency Cleanup
- **Elimination of Erroneous Episode Tabs**: Standalone movies strictly show dedicated movie details without confusing "Episodes" capsules.
- **De-Duplication of Rating Badges**: Removed duplicated rating badges from the header badge row, centering official ratings cleanly inside the Neumorphic Info Glance card (`Rating`, `Genre`, `Audio Stream`).

### 🎮 4. Pixel-Aligned Cinema HUD 3.0 & De-Duplication
- **De-Duplicated Playback Controls**: Removed duplicate Subtitles and Audio buttons from the top bar (keeping Top Bar focused on `Cast`, `Equalizer`, and `More Options`), and removed the redundant `^` Up Arrow from the bottom bar. Subtitles and Audio are exclusively accessed via their tactile bottom bar pills.
- **Hybrid Neumorphic Controls**:
  - Center Play/Pause button styled with a vibrant soft lavender vertical gradient, crisp white icon, and tactile elevation shadow.
  - Secondary circular skip buttons with soft raised neumorphic depth.
  - Bottom bar pills (`Chapters`, `Subtitles`, `Audio`, `Speed`, `PiP`, `Fullscreen`) with soft translucent recessed surfaces and glowing lavender activation.
- **Top Cinema Bar**: Back button, movie/episode title highlighted by a vertical lavender accent indicator, clean metadata info (`2024 · 4K HDR · Dolby Vision · E-AC-3 5.1`), `Cast`, `Equalizer`, and `More Options`.
- **Vertical Gesture Pills**: Minimal translucent vertical capsules on the left (Brightness with sun icon) and right (Volume with speaker icon) with smooth lavender fill.

### ⚡ 5. Silky-Smooth Tab Switching & Fluid Motion
- **Isolated LazyListStates**: Implemented independent scroll states per category tab (`All`, `Movies`, `Series`, `Anime`), completely eliminating measurement conflicts, relayout hitching, and scroll jumps during animated transitions.
- **Shared-Axis Directional Parallax**: Category switching now matches the exact physical feel of `Home` → `Settings` navigation with subtle 5% directional parallax shift (`it * 0.05f`), damped spring physics (`Spring.StiffnessLow`, `dampingRatio = 0.82f`), and cubic-bezier crossfade (`CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)`).
- **Non-Blocking Background Thumbnails**: Pruned synchronous file extraction loops on the UI thread, ensuring zero-stutter 90/120Hz library navigation.

### 🔒 5. Pure Offline-First Cinema Experience
- **Web Sources Pruning**: Completely removed web streaming and live TV experiments to keep Jadzumi 100% focused on offline-first, distraction-free local cinema playback.

---

# Jadzumi v2.0.6 (UI/UX Rollback & Motion System Retention) — "Baseline Restored, Premium Motion Preserved" 🌌🎬✨

A precision restoration release rolling back the UI/UX changes from the Pure overhaul to the verified baseline checkpoint (`11c8920`), while fully preserving the cohesive Apple/M3 fluid spring animations, continuous spatial transitions, and gesture responsiveness.

### 🔄 1. UI/UX Baseline Restoration
- **Restored 4 Navigation Destinations**: Restored the classic navigation destinations on the floating frosted dock: **Home**, **Movies**, **TV Shows**, and **Settings**.
- **Living Emblem & Ambient Light Sweep**: Restored the approved 5-second gentle idle breathing glow pulse and specular light sweep on `GeminiGlowingJadzumiLogo`.
- **Classic Cinema Palette**: Restored the signature Lavender (`#8B9DFF`), Glacier Blue, and AMOLED Dark themes, along with original button and timeline styling.

### 🎭 2. Pure Motion System Retention
- **Cohesive Spring Dynamics**: Preserved the damped organic Apple/M3 spring specifications across dock sizing, pill minimizing/expanding, icon scaling, and tab switching.
- **Continuous Spatial Navigation**: Maintained the 500ms physical-geometry shared-element morphing (`CubicBezierEasing(0.19f, 1.0f, 0.22f, 1.0f)`) between library items and detail screens, silky-smooth dismissals, and touch feedback.
- **Leaf Recomposition Decoupling & LRU Scrub Previews**: Retained high-performance leaf-decoupled playback HUD and zero-flash thumbnail scrubbing.

---

# Jadzumi v2.0.5 (Pure UI/UX Overhaul) — "Less interface. More cinema." 🌌🎬⚡

A milestone editorial overhaul redesigning Jadzumi Player with the approved **Jadzumi Pure** visual direction, strictly decluttering navigation chrome, eliminating glowing emblem artifacts, and adopting high-contrast Pure Yellow cinema styling.

### 🖤 1. Navigation Dock Reductions & Logo Removal
- **Strictly 3 Navigation Destinations**: The floating frosted dock has been streamlined down to **Home**, **Library**, and **Settings**, removing unnecessary clutter and distraction while retaining quick access to the entire movie and television catalog.
- **Zero Logo on Dock**: The emblem and branding elements have been removed from the navigation dock, maximizing touch targets and presenting an unobstructed, content-first navigation experience.

### 🌟 2. Glowing Artifact Purge & Crisp Matte Emblem
- **Elimination of Glowing Halos & Specular Light Sweeps**: Removed continuous idle breathing glows, outer radial gradient boxes, and specular light sweeps from `GeminiGlowingJadzumiLogo`.
- **Pure Matte Surface Identity**: Preserved the original vector geometry of `JadzumiPlaySymbol` inside a refined, static, matte tile that respects true AMOLED black backgrounds without visual distraction.

### 🎨 3. Pure Yellow Cinema Palette & AMOLED True Black
- **Signature Cinema Accent (`#FFD600`)**: Integrated Pure Yellow as the primary visual accent across player scrubbers, hero call-to-actions, seek progress indicators, and active badges.
- **Pristine Dark Surfaces**: Backgrounds and surfaces default to True Black (`#000000`) and Obsidian (`#0E0E0E`), disabling distracting background ambient halos on AMOLED profiles for pure cinema immersion.

---

# Jadzumi v2.0.4 (Hotfix & Interaction Evolution) — "Precision Resolution, Side-Docked HUD & Strict HDR" 🌌🎬✨

A precision hotfix and interaction refinement release perfecting 1080p scope resolution parsing, eliminating false HDR10+ badge claims on standard releases, and replacing intrusive center gesture overlays with non-intrusive minimal horizontal pills docked to their respective screen sides.

### 🎯 1. Precision 1080p / 4K Resolution Classification
- **Physical Dimension Priority**: Native track resolution (e.g. 1920x800 or 1920x1080 for 2.40:1 scope cinematic releases) now takes absolute precedence over filename tokens, correctly identifying 1080p full HD films.
- **Word-Boundary Token Matching (`isMedia4K`)**: Eliminated false 4K detections caused by uploader and site tags (such as `4kHdHub`) using strict word-boundary regular expressions (`\b(2160p|4k|uhd)\b`), preventing regular 1080p files from incorrectly displaying 4K badges.

### 🌈 2. Strict HDR & Dolby Vision Hybrid Pipeline
- **Media-Driven HDR Detection**: Corrected `detectHdrFlavor` so it strictly checks video stream color metadata rather than the tablet's screen capabilities. The app no longer falsely claims `HDR10+ (DV Hybrid)` simply because the device display supports HDR10+.
- **SDR Integrity Preservation**: Standard dynamic range files (BT.709 transfer or explicit SDR) are never misclassified as HDR from file naming artifacts.

### 🧭 3. Non-Intrusive Side-Docked Gesture Capsules
- **Minimal Horizontal Side Pills**: Replaced the large centered gesture overlay with sleek, compact horizontal pills docked to their respective sides:
  - **Left Side**: Minimal Brightness pill with icon, micro progress bar, and percentage.
  - **Right Side**: Minimal Volume pill with icon, micro progress bar, and percentage.
  - **Respective Side Double-Tap Seeking**: Backward double-tap seeking docks cleanly on the left (`−10s`), while forward double-tap seeking docks cleanly on the right (`+10s`).
- **100% Unobscured Center Viewport**: The center of the screen, Play/Pause controls, dialogue, and subtitles remain completely clear and visible during volume, brightness, or seeking interactions.

---

# Jadzumi v2.0.3 (Performance & Polish Evolution) — "High-Framerate Motion & Resilient Updater" 🌌🎬⚡

A comprehensive performance engineering, UI/UX polish, and stability release delivering silky-smooth 90 Hz / 120 Hz library scrolling, eliminated playback recomposition bottlenecks, cached zero-flash timeline scrubbing, and resilient multi-stage update delivery.

### ⚡ 1. Performance Engineering & Recomposition Isolation
- **Root Timeline Decoupling**: Completely isolated high-frequency playback position updates (100–250ms) to leaf composables in `CinemaPlayer`. Eliminated redundant full-screen recompositions of HUD controls, gesture detectors, and overlays during active video playback.
- **Zero-Flash Scrubbing Previews**: Introduced in-memory LRU thumbnail caching (`previewCache`) and smooth frame retention during scrubbing in `ScrubPreview`, eliminating black flashes and disk I/O lag when seeking across timelines.
- **Dynamic Thumbnail Memory Cache**: Dynamically scaled `ThumbnailMemory.cache` to allocate 1/7th of available heap (32 MB to 96 MB) with 4 concurrent decoding permits, providing stutter-free poster grid scrolling on 90 Hz and 120 Hz displays.
- **Optimized Lazy List Keys**: Replaced O(N) string concatenation keys in chunked grid rows with O(1) stable identifiers and memoized row allocations (`remember(displayVideos, colCount)`).
- **Background Halo Overdraw Prevention**: Suspended ambient `PortalHaloBackdrop` drift animations during fullscreen video playback, conserving GPU cycles and extending battery life.

### 🌐 2. Resilient Update Downloader & CDN Fallback
- **Direct CDN Asset Delivery**: Prioritized direct CDN download paths with multi-hop redirect handling, preventing rate-limiting on unauthenticated GitHub REST API endpoints.
- **HTTP 504 Gateway Timeout Resilience**: Added intelligent exponential backoff retry handling (up to 4 attempts) for transient CDN edge timeouts.
- **1-Tap Browser Fallback**: Prominently integrated a direct "Download directly via browser ↗" surface pill inside the update dialog whenever an edge timeout or network error occurs.

### 🎭 3. Cohesive Motion System Polish
- **Standardized Motion Tokens (`JadzumiMotion`)**: Coordinated timing tokens across the entire interface (Touch feedback 120ms, Tab navigation 210ms, Dialogs/Panels 280ms, Poster morph 500ms, and Player dismissal 420ms).
- **Physical Geometry Morph**: Polished player dismissal transition with `CubicBezierEasing(0.19f, 1.0f, 0.22f, 1.0f)` physics, supporting seamless interruption and respecting system reduced-motion accessibility settings.

---

# Jadzumi v2.0.2 (Hotfix & UI Evolution) — "Immersive Now Playing & HDR Master Pipeline" 🌌🎬✨

A precision hotfix and interface refinement release implementing the approved Jadzumi Player v2.0 UI specification, eliminating Picture-in-Picture (PiP) judder, decluttering player settings, perfecting the 10-bit HDR/SDR pipeline, and introducing real-time read-only playback diagnostics.

### 🖼️ 1. Approved Jadzumi Player v2.0 UI Refinements
- **Living Cards & Adaptive Density**:
  - Implemented 3 user-selectable grid density presets (`Compact` @ 104dp min-width / 10dp gap / 12dp radius, `Comfortable` @ 142dp min-width / 14dp gap / 18dp radius, `Spacious` @ 194dp min-width / 18dp gap / 22dp radius) with responsive column computation based on available container width, persisted in SharedPreferences (`library_density`).
  - Living cards with artwork-first rest states, subtle 4dp bottom progress bars for in-progress videos, and light compression press microinteractions (`touchMotion`).
- **Immersive Now Playing UI**:
  - Top HUD with floating Back button, video title, and real-time codec/resolution line at left; Subtitles, Playback Diagnostics, and Quick Menu shortcuts at right.
  - Symmetrical center controls featuring a 56dp primary Play/Pause button in Lavender (`#ddc8ff`) with deep ink glyph (`#241a31`), flanked by balanced ±10s seek buttons (`#32303e`).
  - Vignette vertical veil matching specification (`rgba(0,0,0,0.53)` top to `rgba(0,0,0,0.68)` bottom).

### 🚀 2. Smooth Picture-in-Picture (PiP) Transitions
- **Zero Surface Recreation**: Eliminated redundant `PlayerView` composition switching across `inPip` states, ensuring the native hardware `SurfaceView` and MediaCodec pipeline remain continuously mounted without dropped frames or stutter.
- **Android 12+ (API 31+) Smooth OS Morph**: Avoided collision between `onUserLeaveHint()` manual calls and `setAutoEnterEnabled(true)` framework morph animations.

### 🌈 3. HDR Playback Pipeline & Brightness Restoration
- **Strict 10-Bit SDR vs. HDR Distinction**: Completely audited and corrected Media3 `ColorInfo` handling: videos declaring explicit `C.COLOR_TRANSFER_SDR` (such as 10-bit SDR anime encodes) are never falsely classified as HDR from bit depth or file tags alone.
- **Auto Specular Headroom**: Eradicated unexpected screen brightness jumps. System screen brightness is preserved during HDR playback; 100% boost is only applied if explicitly toggled by the user.
- **AMOLED HDR10+ / Dolby Vision Hybrid Compatibility**: Robust decoder selection and display matching for AMOLED HDR10+ displays (such as Xiaomi Pad 5 and Samsung Galaxy devices), prioritizing native SoC hardware HEVC decoders for clean BT.2020 PQ rendering without purple/green tinting.
- **Persistent HDR Profile Selector**: Freely switch between `Auto (Display Match)`, `Dolby Vision Master`, `HDR10+ Dynamic Metadata`, `HDR10 / Base Layer (BT.2020)`, and `SDR Tone-Mapped`.

### 📊 4. Real-Time Read-Only Playback Diagnostics
- 1-Tap access to live stream metrics: Resolution, Frame Rate, Container Aspect Ratio, Video Codec, Hardware/Software Decoder Name, Color Primaries (BT.2020 vs BT.709), Color Transfer (ST 2084 PQ vs HLG vs SDR), Bit Depth, Display HDR Capabilities, and Audio Stream Details.

### 🗂️ 5. Decluttered Quick Menu
- Reorganized the crowded Quick Menu into 5 clean, categorized sections with clear headers and dividers:
  1. *Playback & Display* (Speed, Aspect Ratio, Minimalist HUD, Sleep Controls, Continuity)
  2. *Video & Color Studio* (Video Enhancer, HDR Profile, Playback Diagnostics)
  3. *Audio & Soundstage* (Audio Tracks, Spatial Cinema 3D, Voice Clarity)
  4. *Subtitles & Captions* (Subtitle Tracks, Dual Subtitles Studio)
  5. *Cinephile Studio* (Chapters & Acts, Scene Clipper, 5-Swatch Palette, Binge Automation)

---

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
