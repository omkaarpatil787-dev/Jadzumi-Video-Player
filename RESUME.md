<h1 align="center">Jadzumi · Project Summary</h1>
<p align="center">An overview of the app, its architecture, and release progress.</p>

<p align="center">
  <a href="https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player/releases/latest"><img src="https://img.shields.io/badge/version-1.7.0-2563eb?style=flat-square" alt="Version 1.7.0"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-16a34a?style=flat-square" alt="Android 8.0 and later">
  <img src="https://img.shields.io/badge/Kotlin-2.2.10-7c3aed?style=flat-square" alt="Kotlin 2.2.10">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-0891b2?style=flat-square" alt="GPL-3.0 license"></a>
</p>

<p align="center"><a href="README.md">App overview</a> · <a href="https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player/releases/latest">Latest release</a> · <a href="docs/DEVELOPMENT.md">Developer guide</a></p>

## 🎬 At a glance

**Jadzumi Video Player** is a free, offline-first Android app for local movies and TV
shows. It brings together a Compose interface, Media3 playback with hardware video effects,
FFmpeg decoder fallbacks, subtitles, and on-device viewing progress.

| Release | Platform | Approach |
| --- | --- | --- |
| **v1.7.0 · build 24** | Android 8.0+ · ARM64 | Portal Halo Backdrops · Snappy Controls · Immersive Telemetry · Pure Floating Cards |

## 📸 Screenshots

<p align="center">
  <a href="docs/screenshots/home.jpg"><img src="docs/screenshots/home.jpg" alt="Jadzumi tablet home screen with continue watching and navigation" width="1000"></a>
</p>
<p align="center"><sub>Home · Continue watching · Your local video library</sub></p>

<details>
<summary><strong>Explore the library, player, and settings</strong></summary>

<table>
  <tr>
    <td width="50%" align="center"><strong>Movies</strong><br><a href="docs/screenshots/movies.jpg"><img src="docs/screenshots/movies.jpg" alt="Movie library with poster cards" width="100%"></a></td>
    <td width="50%" align="center"><strong>TV shows</strong><br><a href="docs/screenshots/tv-shows.jpg"><img src="docs/screenshots/tv-shows.jpg" alt="TV show library and continue watching" width="100%"></a></td>
  </tr>
  <tr>
    <td align="center"><strong>Series details</strong><br><a href="docs/screenshots/series-details.jpg"><img src="docs/screenshots/series-details.jpg" alt="Series metadata, seasons, and viewing progress" width="100%"></a></td>
    <td align="center"><strong>Episodes &amp; inspector</strong><br><a href="docs/screenshots/episodes.jpg"><img src="docs/screenshots/episodes.jpg" alt="Episode list and technical inspector" width="100%"></a></td>
  </tr>
  <tr>
    <td align="center"><strong>Playback</strong><br><a href="docs/screenshots/playback.jpg"><img src="docs/screenshots/playback.jpg" alt="Video player with subtitle and playback controls" width="100%"></a></td>
    <td align="center"><strong>Video settings</strong><br><a href="docs/screenshots/video-settings.jpg"><img src="docs/screenshots/video-settings.jpg" alt="Video decoding, scaling, and display settings" width="100%"></a></td>
  </tr>
  <tr>
    <td colspan="2" align="center"><strong>Privacy &amp; About</strong><br><a href="docs/screenshots/about.jpg"><img src="docs/screenshots/about.jpg" alt="Privacy and About screen from an earlier build" width="800"></a></td>
  </tr>
</table>

</details>

<sub>Tablet screenshots from v1.3.1. Some labels and version details differ from the current release.</sub>


## 🧩 Architecture

| Area | Main components | Purpose |
| --- | --- | --- |
| App & settings | `JadzumiApp.kt`, `MainActivity.kt` | Navigation, themes, preferences, and About |
| Library | `LocalLibrary.kt`, `LocalSeries.kt` | Local media scanning and series organization |
| Playback | `PlaybackController.kt`, `CinemaPlayer.kt` | Media3 player, FFmpeg fallbacks, and playback controls |
| Details & artwork | `MovieDetailsPage.kt`, `TvShowDetailsPage.kt` | Movie/series screens, artwork, and viewing progress |
| Online metadata | `OnlineMetadataFetcher.kt` | Opt-in metadata and poster requests with local caching |

**Playback:** double-tap seek, hold-to-2× speed, chapter markers, external subtitles,
audio adjustments, background playback, and picture-in-picture.

**Library & appearance:** movie/series grouping, continue watching, grid/list views,
batch actions, adaptive layouts, themes, frosted surfaces, and custom icons.

<details>
<summary><strong>Technology stack</strong></summary>

| Component | Version / technology |
| --- | --- |
| Language | Kotlin 2.2.10 · JVM 17 |
| Interface | Jetpack Compose · Material 3 Expressive |
| Playback & Effects | AndroidX Media3 (ExoPlayer, Effect, UI) 1.11.1 · NextLib 1.11.1-0.16.0 |
| Native dependencies | FFmpeg 9.0.1 · dav1d 1.5.4 |
| Build tools | Gradle 8.11.1 · AGP 8.10.1 · R8 |
| Android SDK | Minimum 26 · Target 35 · Compile 36 |
| Media access | Android Storage Access Framework |

</details>

<details>
<summary><strong>What’s new · v1.7.0 🌌🎬</strong></summary>

- **Portal Halo Atmospheric Backdrops**: Atmospheric radial aura backdrops behind app chrome and library cards with customizable palettes (Glacier Blue, Peach Dusk, Violet Cinema), intensity, and spread.
- **Snappy Micro-Animated Player Controls**: Snappy 54dp circular seek buttons with bouncy rotational impulse (-24° / +24°), scale punch (1.18x), and gentle floating skip accumulator badges.
- **Persistent Immersive Telemetry**: Full-screen immersive playback HUD overlays for Clock, Battery Percentage, and Remaining Time.
- **Pure Borderless Floating Cards**: Clean, uninterrupted rounded corner arcs with genuine Material 3 floating depth (`tonalElevation` & `shadowElevation`), eliminating chalky emboss or bevel artifacts.
- **Unified Settings with Scroll Blur Fade**: Consistent surface styling matching all tabs, dynamic top/bottom edge blur on scroll, and tablet two-pane layout.
- **Snappy Cinema Portal Launch**: Accelerated, natural launch sequence (1.4s) seamlessly revealing your video library.

See [release notes](RELEASE_NOTES.md).

</details>

## 🚀 Release progress

**v1.7.0** introduces the Portal Halo UI & motion architecture, micro-animated player controls with zero distortion, persistent immersive telemetry status pill, unified settings with scroll blur fade, pure borderless floating cards, and an accelerated 1.4s Cinema Portal splash screen. See [release notes](RELEASE_NOTES.md).

<details>
<summary><strong>View project milestones</strong></summary>

| Milestone | Deliverables & Scope | Status |
| :--- | :--- | :---: |
| **Milestone 1 — Responsive Core** | Adaptive wide-screen layouts, responsive navigation, glassmorphic UI | Complete |
| **Milestone 2 — Media Indexing** | Scoped Storage / SAF integration, asynchronous library scanner, disk cache | Complete |
| **Milestone 3 — Playback Engine** | Hardware SurfaceView, multi-ratio scaling, variable speeds (0.5x–2.0x) | Complete |
| **Milestone 4 — Privacy & Audio DSP** | Zero telemetry, night mode voice clarifier, Bluetooth delay compensation | Complete |
| **Milestone 5 — TV Continuity** | Season/Episode grouping, autoplay continuity, offline metadata sidecars | Complete |
| **Milestone 6 — Production Hardening** | R8 code shrinking, resource stripping, ProGuard rules, reproducible signing | Complete |
| **Milestone 7 — Hardware & Display** | OLED-exclusive HDR peak luminance, LCD backlight protection, hardware inspector | Complete |
| **Milestone 8 — Mobile Ergonomics** | Floating frosted dock, Gemini-style glowing brand emblem, alive greeting | Complete |
| **Milestone 9 — CineFlow Fluidity** | Natural 340ms cubic bezier navigation transitions, frosted glass depth, blend-fade scrolling | Complete |
| **Milestone 10 — Flagship 1.3 Overhaul** | Hold-to-2x speed, smart end-credit autoplay countdown, chapter tick marks & snapping, external subtitle loader, poster grid toggle, smart filter chips, batch multi-select | Complete |
| **Milestone 11 — Polishing & Fluid Exit (v1.3.1)** | Subtle 0.75dp specular card borders, decluttered header with grid/list toggle, fluid cubic-bezier exit transitions | Complete |
| **Milestone 12 — Mobile Dock & Ambient Polish (v1.3.2)** | Universal mobile dock alignment (equal destination weighting), stutter-free card opening transitions, dynamic System default ambient theme matching for frosted glass surfaces, and portrait-optimized Glassify controls | Complete |
| **Milestone 13 — Privacy, Signing & License Corrections (v1.3.3)** | Default-off online requests, shared email-free User-Agent, external release credentials, offline license viewer, versioned CI APKs | Released; CI verified |
| **Milestone 14 — Motion, Continuity & Tools (v1.4.0)** | Configurable spring animations, touch lighting, elastic scroll edges, technical inspector, local backup/restore, dialogue replay, subtitle search, episode continuity, sleep controls, scene bookmarks, optional refresh matching | Released; CI verified |
| **Milestone 15 — Video Enhancer & Audio Studio (v1.5.0)** | Hardware OpenGL ES color studio (Contrast, Shadow Lift, Saturation, Vibrance, AMOLED Deep Black, 5 profiles), zero-copy SurfaceView bypass, HDR master passthrough safety, subtitle isolation, real-time Audio Stream Telemetry Card, and Channel Processing Modes (Stereo Downmix, Dialogue Clarity Focus) | Released; CI verified |
| **Milestone 16 — Universal Views & Playback HUD (v1.6.0)** | Universal Grid/List view toggle across all primary tabs (Home, Movies, TV Shows, Folders), Playback HUD status pill with live clock & battery telemetry, Files settings with folder blacklisting, and emblem relocation to About | Complete |


</details>

## ✅ Validation

- **Unit tests:** 69 tests passing across all test suites; zero failures or skips.
- **Signed CI build and publication:** [GitHub Actions CI passed](https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player/actions/runs/40605491370).
- **Public APK inspected:** v1.5.0 / build 22, valid production signing key, and all native NextLib libraries verified.

These checks verify the build and package. They do not replace device playback tests
or establish legal clearance.

## 📌 Release notes

- Metadata requests default to off; cached metadata and posters remain usable offline.
- Release credentials stay outside Git. Build/signing instructions live in the
  [developer guide](docs/DEVELOPMENT.md).
- The new key cannot update older development-signed installations in place.
  Reinstallation can erase local settings and viewing history.
- The owner reports ChatGPT / Google Stitch origins for the three bundled JPGs;
  per-file exports and redistribution terms remain unverified.
- Upstream configures mbedTLS 3.6.7, but retained TLS code/version in this AAR was
  not independently established. Native Corresponding Source review remains open.

[GPL-3.0](LICENSE) · [Third-party notices](THIRD_PARTY_LICENSES.md) ·
[Outstanding legal review](LEGAL_REVIEW_REQUIRED.md)
