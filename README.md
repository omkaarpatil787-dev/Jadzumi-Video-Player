<h1 align="center">Jadzumi Video Player</h1>
<p align="center"><strong>Your movies. Your space.</strong><br>A free, offline-first Android video player for phones, foldables, and tablets.</p>

<p align="center">
  <a href="https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player/releases/latest"><img src="https://img.shields.io/badge/version-1.9.1-2563eb?style=flat-square" alt="Version 1.9.1"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-16a34a?style=flat-square" alt="Android 8.0 and later">
  <img src="https://img.shields.io/badge/Kotlin-2.2.10-7c3aed?style=flat-square" alt="Kotlin 2.2.10">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-0891b2?style=flat-square" alt="GPL-3.0 license"></a>
</p>

<p align="center">
  <a href="https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player/releases/latest"><strong>Download APK</strong></a> ·
  <a href="RESUME.md">Project summary</a> ·
  <a href="docs/DEVELOPMENT.md">Developer guide</a>
</p>

Watch your local movies and TV shows with gesture controls, subtitles, and a library
that remembers where you left off. Built with Jetpack Compose and AndroidX Media3,
with NextLib FFmpeg software decoders alongside Android's platform decoders.

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


<details>
<summary><strong>What’s new · v1.8.0 🎬✨</strong></summary>

- **Fix Match & Artwork Matcher**: Disambiguate films and TV series against Cinemeta and IMDb streaming catalogs, or choose custom artwork directly from your device's photo gallery.
- **Universal 500ms Shared-Element Morph**: Screen and card transitions smoothly stretch physical geometry over a calibrated 0.5s spring curve (`cubic-bezier(0.19, 1.0, 0.22, 1.0)`).
- **YouTube-Style Cascading Seek Chevrons**: Sleek, compact chevron indicators (`‹‹‹ 10s` and `10s ›››`) with staggered animated alphas replace oversized circular overlays.
- **Ambient Cinema Horizon**: Diffused atmospheric glow behind hero banners in dark themes and feather-soft porcelain horizon washes in light theme.

See [release notes](RELEASE_NOTES.md) for full details.

</details>

## ✨ Highlights

| Playback | Your library | Make it yours |
| --- | --- | --- |
| Hardware decoding with FFmpeg fallback | Continue watching and resume positions | Light, dark, and AMOLED themes |
| GPU Video Enhancer & Color Studio | Movies, series, seasons, and episodes | Portal Halo atmospheric backdrops |
| Expanded Audio Studio & Channel Modes | Local artwork and metadata sidecars | Pure flat modern cards (zero highlights) |
| Double-tap seek and hold-to-2× speed | Poster grid, list view, and batch actions | Subtitle typography suite & 3D glows |
| External subtitles and timing adjustments | Scene bookmarks, notes, and search | Theme-reactive playback controls |
| Background audio and picture-in-picture | Episode continuity and sleep timer | Refresh rate matching |

## 📦 Latest release · v1.9.1

- 🌟 **Smart Dolby Vision & HDR10+ Fallback Architecture**: Automatically routes hybrid Dolby Vision Profile 7 & 8 files with HDR10/HDR10+ base layers to SoC hardware HEVC decoders on non-DV certified panels (Samsung, Pixel, standard panels), completely eliminating purple/green color distortion.
- ☀️ **Universal 10-Bit HDR Window Mode**: Unlocks native `COLOR_MODE_HDR` wide-color-gamut composer across all compatible OLED, AMOLED, Mini-LED, and IPS displays, preventing SurfaceFlinger from crushing 1000-nit HDR curves into 8-bit SDR.
- 🌑 **HDR Shadow Clarifier & Night Scene Boost**: Dedicated Quick Menu control that lifts near-black shadows (0.01–5 nits) in dark scenes (such as nighttime interiors) while preserving native BT.2020 color accuracy and specular highlights.
- 🏷️ **Dynamic Golden Status Badging**: Live status pill highlights the exact active stream format: `Dolby Vision`, `HDR10+`, `HDR10`, or `HDR10 (DV Hybrid)`.
- 🎬 **v1.9 Core Features**: Includes True Auto Frame Rate (AFR) matching, Biometric Private Vault, Cinema DNA Studio, Lossless Scene Clipper, Storage Space Saver, and Per-Series Binge Automation.

[Download v1.9.1](https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player/releases/tag/v1.9.1) · [Release notes](RELEASE_NOTES.md)

**Upgrading from an older development-signed APK?** The new signing key requires
reinstallation. Uninstalling can erase app settings and playback history.

## 🔒 Privacy

No ads, analytics, or background telemetry. Your playback history stays on your device.

Online lookups are **off by default**. Enable them in **Settings → Privacy & About**
to request movie/TV information and artwork from **Cinemeta** (`v3-cinemeta.strem.io`),
**Wikipedia** (`en.wikipedia.org`), and **TVMaze** (`api.tvmaze.com`), plus official
posters from their image delivery networks. This sends search titles to those services.
Cached information and downloaded artwork remain available offline when lookups are disabled.

GitHub update checks are available in **Privacy & About**.
Manual checks contact GitHub when requested; automatic checks have their own
**off-by-default** switch. APK downloads and installation require your action.

## 🛠️ Development

Requires **Android 8.0+** and an **ARM64** device. The project uses Kotlin 2.2.10,
Jetpack Compose, Media3 1.11.1, and NextLib with FFmpeg 9.0.1.

See the [developer guide](docs/DEVELOPMENT.md) for building and release setup, or the
[project summary](RESUME.md) for architecture and validation details.

## 📄 License

Jadzumi is free and open source under [GPL-3.0](LICENSE). Open-source notices are
included in **About → Licenses** and [Third-party licenses](THIRD_PARTY_LICENSES.md).

Artwork redistribution terms, native Corresponding Source completeness, and
[legal review items](LEGAL_REVIEW_REQUIRED.md) remain under review; this project
does not claim legal clearance.
