# JADZUMI PURE — FEATURE & SETTINGS AUDIT

This document maps all existing capabilities, settings, preferences, and navigation destinations of Jadzumi Player v2.0.4 to their exact destinations in the **Jadzumi Pure** editorial redesign.

---

## 1. Feature Mapping Table

| Capability / Setting | Current Location (v2.0.4) | Proposed Jadzumi Pure Location | Status | Implementation Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Media3 / ExoPlayer Playback Engine** | `PlaybackController.kt` | Engine layer unchanged | **Preserved** | No changes to playback core or MediaCodec decoders. |
| **Hardware Codec Support (H.264, HEVC, AV1, VP9)** | `DeviceHardwareInspector.kt` | Background inspector unchanged | **Preserved** | Direct GPU decoding retained. |
| **FFmpeg Extension (AC-3, E-AC-3, TrueHD, DTS)** | Native C++ / Media3 bridge | Native layer unchanged | **Preserved** | Retains all multichannel audio decoders. |
| **Stream-Driven HDR Pipeline (PQ, HLG, DV, HDR10)** | `PlaybackController.kt` | Playback HUD & Tech Inspector | **Preserved** | Precision stream metadata detection preserved. |
| **Side-Docked Minimal Gesture Pills** | `CinemaPlayer.kt` | Edge overlays (Left: Brightness, Right: Volume) | **Preserved** | 38dp pill geometry, edge docking retained. |
| **Double-Tap Seek (Left -10s, Right +10s)** | `CinemaPlayer.kt` | Left / Right side HUD pulse | **Preserved** | Unobstructed center screen retained. |
| **Interactive Timeline Scrubbing & Thumbnails** | `ScrubPreview.kt` | Playback timeline with scrub bubble | **Preserved** | Minimalistic yellow/lavender timeline track. |
| **Smart Control Visibility (3.2s auto-hide)** | `CinemaPlayer.kt` | Playback HUD overlay | **Preserved** | Unchanged timing, tap-to-reveal. |
| **Audio Track Selection** | `CinemaPlayer.kt` quick menu | Top bar Audio icon $\rightarrow$ Pure Audio Modal | **Visually relocated** | Modern editorial modal with codec/channel badges. |
| **Subtitle Selection & Dual Subtitles** | `DualSubtitleStudio.kt` | Top bar Subtitle icon $\rightarrow$ Pure Subtitle Modal | **Visually relocated** | Minimalist list with font size/sync offsets. |
| **Aspect Ratio Modes (Fit, Crop, Stretch, 16:9, 21:9)**| `CinemaPlayer.kt` | Bottom controls Aspect icon $\rightarrow$ Cycle Pill | **Preserved** | In-place pill notification on aspect change. |
| **Playback Speed (0.5x to 3.0x)** | `CinemaPlayer.kt` | Bottom controls Speed icon $\rightarrow$ Pure Speed Sheet| **Visually relocated** | Elegant horizontal stepper selector. |
| **Picture-in-Picture (PiP)** | `MainActivity.kt` | Bottom controls PiP icon & backgrounding | **Preserved** | Android system PiP integration intact. |
| **Background Audio Playback** | `PlaybackController.kt` | Playback settings & lockscreen controls | **Preserved** | MediaSession integration maintained. |
| **Auto Frame Rate (AFR) Matching** | `AutoFrameRateManager.kt` | Playback settings toggle | **Preserved** | Display refresh rate switching preserved. |
| **Audio Studio: Dialogue Clarity** | `AudioStudioModel.kt` | Navigation Rail $\rightarrow$ Audio Studio or Quick Menu| **Visually relocated** | Clean editorial slider with real-time dB preview. |
| **Audio Studio: Night Mode (DRC)** | `AudioStudioModel.kt` | Audio Studio & Quick Settings | **Visually relocated** | Pure minimalist switch. |
| **Audio Studio: A/V Sync Delay (-500ms to +500ms)**| `AudioStudioModel.kt` | Audio Studio & Track selector | **Visually relocated** | Precision 10ms step slider. |
| **Audio Studio: Downmix (Stereo, 5.1 Matrix)** | `AudioStudioModel.kt` | Audio Studio & Quick Settings | **Visually relocated** | Pill segment selector. |
| **Video Enhancer: Contrast / Saturation / Gamma** | `VideoEnhancerModel.kt` | Settings $\rightarrow$ Video Enhancer & Player Quick Menu| **Visually relocated** | Clean monochromatic slider controls. |
| **Video Enhancer: OLED True Black Protection** | `VideoEnhancerModel.kt` | Settings $\rightarrow$ Video Enhancer | **Preserved** | Retained toggle. |
| **Library Grid View & List View Modes** | `LocalLibraryUi.kt` | Library top toolbar mode toggle | **Preserved** | Fluid toggle between poster grid and dense list. |
| **Library Sorting & Filtering** | `LocalLibraryUi.kt` | Top toolbar Sort / Filter dropdown | **Visually relocated** | Clean editorial filter pills (All, Movies, TV). |
| **Hero Continue Watching Card** | `LocalLibraryUi.kt` | Home screen prominent header card | **Visually relocated** | Full-width editorial poster with prominent typography.|
| **Movie Details Screen** | `MovieDetailsPage.kt` | Expanded destination screen | **Visually relocated** | Large cinematic backdrop, editorial title, tech pills.|
| **TV Show / Episode Browser** | `TvShowDetailsPage.kt` | Series Details screen | **Visually relocated** | Season dropdown, horizontal episode selector. |
| **Folder Browser** | `LocalLibraryUi.kt` | Tablet Rail Folders tab / Phone Bottom Folders | **Visually relocated** | Dedicated folder navigation with item count. |
| **Universal Search & Fast Filtering** | `JadzumiApp.kt` | Top bar Search icon $\rightarrow$ Pure Search Overlay | **Visually relocated** | Instant filter with live match highlighting. |
| **Storage Cleaner / Media Analyzer** | `StorageAnalyzer.kt` | Settings $\rightarrow$ Storage / Folders header icon | **Visually relocated** | Clean breakdown card with duplicate finder. |
| **Library Rescan (Incremental & Full)** | `LocalLibrary.kt` | Top bar Refresh icon / Settings Library | **Preserved** | Animated sync indicator. |
| **Online Metadata & Poster Matching** | `OnlineMetadataFetcher.kt` | Movie Details $\rightarrow$ "Fix Match" action | **Preserved** | Search & replace TMDB poster and synopsis. |
| **Appearance: Accent Exploration (Lavender vs Yellow)**| New feature | Prototype header switcher & Settings $\rightarrow$ Theme | **Requires implementation work** | Toggle between Pure Lavender and Pure Yellow. |
| **Appearance: Theme (AMOLED, Dark, Light)** | `JadzumiApp.kt` | Settings $\rightarrow$ Appearance | **Preserved** | AMOLED True Black is the default hero look. |
| **Living Emblem & Logo Preservation** | `JadzumiApp.kt` | Rail top emblem / Splash screen | **Preserved** | Strict requirement: geometry & colors untouched. |
| **Reduced Motion & Lively Animation Toggles** | `MotionEffects.kt` | Settings $\rightarrow$ Appearance | **Preserved** | Respects system accessibility and app setting. |
| **Continuous Spatial Navigation** | New animation system | Card $\leftrightarrow$ Details $\leftrightarrow$ Player transitions | **Requires implementation work** | Shared physical geometry morphing. |

---

## 2. Navigation Architecture

### Tablet Landscape & Portrait (Xiaomi Pad 5 11" 16:10)
- **Persistent Left Rail Navigation**:
  - `Jadzumi Emblem`: App identity with subtle idle breathing glow (preserved asset).
  - `Home`: Editorial Continue Watching hero card, Quick Picks, Recently Added.
  - `Library`: Grid / List browser with Filter Pills (Movies, Series, All), Sort controls.
  - `Folders`: Direct directory explorer with folder hierarchy and media counters.
  - `Audio Studio`: Instant access to Dialogue Clarity, Night Mode, and Audio Delay.
  - `Settings`: Grouped categorization with zero clutter.
- **Top Context Bar**: Search icon, Rescan button, Storage Analyzer, Overflow menu.

### Phone Portrait & Landscape
- **Bottom Navigation Bar**:
  - `Home` · `Library` · `Folders` · `Settings`
- **Top App Bar**:
  - Jadzumi title with editorial typography, Universal Search button, and contextual actions.

---

## 3. Summary of Status Flags
- **Preserved**: 23 items (core playback, codecs, HDR, gestures, engine, settings, privacy)
- **Visually Relocated**: 11 items (editorial refinement, cleaner modals, modern typography)
- **Requires Implementation Work**: 2 items (Continuous Spatial Navigation and Live Accent Color Switching)
- **Requires Clarification**: 0 items (all requirements are explicitly specified)
