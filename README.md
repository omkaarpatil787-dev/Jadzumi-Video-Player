<h1 align="center">Jadzumi Video Player</h1>
<p align="center"><strong>Your movies. Your space.</strong><br>A free, offline-first Android video player for phones, foldables, and tablets.</p>

<p align="center">
  <a href="https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player/releases/latest"><img src="https://img.shields.io/badge/version-1.4.0-2563eb?style=flat-square" alt="Version 1.4.0"></a>
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
<summary><strong>What’s new · v1.4.0 ✨</strong></summary>

Configurable spring animations, touch lighting, elastic scroll edges, a measured
technical inspector, local backup/restore, dialogue replay, subtitle search,
episode continuity, sleep controls, and scene bookmarks. Optional seamless refresh
matching and a separate App updates card round out this release.

See [release notes](RELEASE_NOTES.md) and [validation details](docs/FEATURE-PROGRESS.md).

</details>

## ✨ Highlights

| Playback | Your library | Make it yours |
| --- | --- | --- |
| Hardware decoding with FFmpeg fallback | Continue watching and resume positions | Light, dark, and AMOLED themes |
| Double-tap seek and hold-to-2× speed | Movies, series, seasons, and episodes | Adjustable frosted-glass appearance |
| External subtitles and timing adjustments | Local artwork and metadata sidecars | Custom app icons |
| Background audio and picture-in-picture | Poster grid, list view, and batch actions | Brightness, volume, and scaling controls |

## 📦 Latest release · v1.4.0

- ✨ Gentler motion, expressive springs, and optional touch lighting.
- 🎬 More playback and library tools, with codecs preserved.
- ⚙️ Optional refresh matching and verified in-app update downloads.

[Download v1.4.0](https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player/releases/tag/v1.4.0) · [Release notes](RELEASE_NOTES.md)

**Upgrading from an older development-signed APK?** The new signing key requires
reinstallation. Uninstalling can erase app settings and playback history.

## 🔒 Privacy

No ads, analytics, or background telemetry. Your playback history stays on your device.

Online lookups are **off by default**. Enable them in **Settings → Privacy & About**
to request movie/TV information from Wikipedia and TVMaze, plus artwork from their
image hosts. This sends search titles to those services. Cached information and
images remain available offline when lookups are disabled.

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
