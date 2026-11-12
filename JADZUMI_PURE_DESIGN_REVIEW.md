# JADZUMI PURE — DESIGN REVIEW & APPROVAL GATE

## 1. Prototype Entry Point & Launch Instructions

The standalone, interactive prototype has been created in an isolated directory without modifying any Android production files:

- **Local File Path**:  
  [`design-prototypes/jadzumi-pure/index.html`](file:///C:/Users/onkar/OneDrive/Documents/GitHub/Jadzumi-Video-Player/design-prototypes/jadzumi-pure/index.html)
- **Direct Browser URL**:  
  `file:///C:/Users/onkar/OneDrive/Documents/GitHub/Jadzumi-Video-Player/design-prototypes/jadzumi-pure/index.html`

Double-click `index.html` or open it in Google Chrome / Microsoft Edge.

---

## 2. Visual Architecture & Design Language: Jadzumi Pure

Inspired by editorial typography and minimalism ("Less interface. More cinema."):

- **Typography-First**: Clean, heavy contrast (`Inter` / `Roboto`), bold headlines (`font-weight: 800`), refined subheads, and discreet metadata pill labels.
- **Surface Hierarchy**:
  - `AMOLED True Black` (`#000000` / `#050505` background) ensures zero backlight bleed on OLED panels.
  - `Surface 1` (`#101010`) and `Surface 2` (`#181818`) for elevated glass cards.
- **Color Accent Exploration**:
  - **Pure Yellow** (`#FFD600`): Inspired by the reference editorial cinema layout.
  - **Pure Lavender** (`#BB86FC`): Retaining Jadzumi's established brand heritage.
  - *Both are toggleable live in the prototype top control bar for direct comparison.*
- **Branding Preservation**: The original Jadzumi logo geometry and Living Emblem vector are 100% preserved. The ambient breathing halo pulse surrounds the original emblem without touching its vector paths.

---

## 3. Responsive Adaptations

### A. Tablet-First (Xiaomi Pad 5 11" 16:10)
- **Persistent Left Navigation Rail (76dp)**:
  - Top: Jadzumi Emblem with breathing ambient halo.
  - Destinations: Home, Library (Grid), Folders, Audio Studio, Settings.
  - Bottom: Quick access to Storage Cleaner.
- **Hero Continue Watching**: Cinematic 320dp backdrop card with prominent typography, progress indicator, and instant "▶ Resume" action.
- **Poster Rows & Grid**: High-density horizontal scroller and responsive poster grid.

### B. Android Phone Adaptation (390×820 Portrait & 820×390 Landscape)
- Automatically switches from the left rail to a sleek **Phone Bottom Navigation Bar** (`Home`, `Library`, `Folders`, `Settings`).
- Single-column editorial layout preserving thumb-zone ergonomics.

---

## 4. Continuous Spatial Navigation System

The navigation experience functions as one continuous physical environment:

$$\text{Library} \xrightarrow{420\text{ ms}} \text{Movie Details} \xrightarrow{450\text{ ms}} \text{Immersive Player} \xrightarrow{380\text{ ms}} \text{Movie Details} \xrightarrow{350\text{ ms}} \text{Library (Scroll Preserved)}$$

1. **Library $\rightarrow$ Movie Details**: Clicking the hero card or a library poster triggers a smooth cubic-bezier (`0.19, 1, 0.22, 1`) expansion.
2. **Details $\rightarrow$ Immersive Player**: Tapping "Play" morphs the backdrop directly into the active video canvas without disruptive black flashes.
3. **Player $\rightarrow$ Details**: Tapping the Back button smoothly returns to Details with updated timestamp and progress.
4. **Details $\rightarrow$ Library**: Returning back to the library restores the **exact scroll position** and originating card context.
5. **Reduced Motion**: Full toggle in the prototype to switch to instant zero-motion accessibility mode.

---

## 5. Playback & Gesture Ergonomics

- **Side-Docked Minimal Pills**:
  - **Left Edge** (`CenterStart`, 28dp): Minimal horizontal pill for **Brightness** and double-tap **Rewind** (`−10s`).
  - **Right Edge** (`CenterEnd`, 28dp): Minimal horizontal pill for **Volume** and double-tap **Forward** (`+10s`).
  - **Center Area**: 100% clean and unobstructed.
- **Interactive Timeline**: Draggable progress bar with real-time time indicators and aspect ratio cycling (`2.40:1 Scope`, `16:9 Fit`, `Stretch`).
- **Modals**: Minimalist sheets for Audio Track selection (E-AC-3, DDP 5.1), Subtitle selection (Dual-subtitle mode), Speed selection (`0.75×` to `2.0×`), and Video Enhancer.

---

## 6. Existing vs Proposed UI Comparison

| Screen / Feature | Current Implementation (v2.0.4) | Proposed Jadzumi Pure Redesign |
| :--- | :--- | :--- |
| **Home Layout** | Chunked rows with standard headers | Full-bleed editorial Continue Watching hero + minimalist poster rows |
| **Navigation** | Standard top bar + drawer/tabs | Tablet Left Rail (76dp) with Emblem + Phone Bottom Bar |
| **Movie Details** | Standard card with technical inspector list | Cinematic hero backdrop with high-contrast typography & spec pill grid |
| **Player HUD** | Standard floating controls | Immersive Cinema Mode with side-docked gesture pills and minimal timeline |
| **Audio Studio** | Standard dialog | Dedicated Studio screen with vocal clarity boost & night mode DRC |
| **Accent Theme** | Single lavender accent | Choice of Pure Yellow (`#FFD600`) or Pure Lavender (`#BB86FC`) |
| **Transitions** | Standard fade/slide | Continuous Spatial Navigation with card-to-details geometry morphing |

---

## 7. Known Prototype Limitations & Anticipated Implementation Risks

### Prototype Limitations
- The HTML prototype simulates video playback using high-resolution video stills rather than live hardware `MediaCodec` streams.
- File system access and actual SAF folder scanning are represented with sample data.

### Implementation Risks & Mitigations
1. **SurfaceView vs TextureView for Shared Element Transitions**:
   - *Risk*: Animating a live `SurfaceView` during Compose shared-element morphing can cause buffer flashing on older Qualcomm decoders.
   - *Mitigation*: Transition into playback by animating the poster backdrop first, then seamlessly swapping the underlying `SurfaceView` once the destination viewport geometry stabilizes.
2. **Recomposition Overhead**:
   - *Mitigation*: Use `@Stable` data models and `derivedStateOf` for gesture pills and timeline scrubbing to maintain steady 60/120 FPS on Xiaomi Pad 5.

---

## 8. Rollback Readiness & Verified Baseline

- **Checkpoint Tag**: `jadzumi-pure-pre-overhaul-20261112-1140`
- **Baseline Commit**: `11c89200ed302fc37b220ec0a604e0dd8db4f68d` on `main`
- **Backup Archive**: `C:\Users\onkar\OneDrive\Desktop\Jadzumi_Pure_Baseline_Backup_20261112.zip`
- **Working Tree**: Completely clean; 0 production files modified.
- **Rollback File**: [`JADZUMI_PURE_CHECKPOINT.md`](file:///C:/Users/onkar/OneDrive/Documents/GitHub/Jadzumi-Video-Player/JADZUMI_PURE_CHECKPOINT.md)

---

## 9. Approval Gate

> [!IMPORTANT]
> **Antigravity has reached the Phase 5 Approval Gate and stopped.**
> No Android production source files, resources, or configurations have been altered.
>
> Please test the interactive prototype at [`design-prototypes/jadzumi-pure/index.html`](file:///C:/Users/onkar/OneDrive/Documents/GitHub/Jadzumi-Video-Player/design-prototypes/jadzumi-pure/index.html).
>
> **Production implementation will not begin without your explicit instruction** (e.g. *"Approve Jadzumi Pure implementation"*).
