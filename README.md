<h1 align="center">Jadzumi Video Player</h1>
<p align="center"><strong>Your movies. Your space.</strong><br>A free, offline-first Android video player for phones, foldables, and tablets.</p>

<p align="center">
  <a href="https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player/releases/latest"><img src="https://img.shields.io/badge/version-2.0.6-2563eb?style=flat-square" alt="Version 2.0.6"></a>
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


<details open>
<summary><strong>What’s new · v2.0.4 🎬✨</strong></summary>

- **Precision 1080p Scope Resolution Mapping**: Direct physical resolution detection prevents widescreen scope 1080p movies (e.g. 1920x800) from being misclassified as 4K.
- **Strict Word-Boundary Token Matching (`isMedia4K`)**: Release group and uploader website names (such as `4kHdHub`) no longer trigger false 4K flags.
- **Strict Media3 HDR/SDR Pipeline**: Stream color metadata drives HDR categorization; standard dynamic range files are preserved as clean SDR, and false `HDR10+ (DV Hybrid)` display overrides are eliminated.
- **Non-Intrusive Side-Docked Gesture Capsules**: Replaced the centered HUD with minimal horizontal pills docked on the left (Brightness & Rewind seeking) and right (Volume & Forward seeking), keeping the screen center and Play/Pause controls completely unobstructed.
- **High-Framerate 90/120Hz Library Performance**: Dynamic thumbnail LRU caching (32MB–96MB) and recomposition decoupling across timeline and player HUDs.

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

## 📦 Latest release · v2.0.1 (Hotfix) — "Omniverse Cinema Suite & Gimmick-Free Player"

- 👥 **Dual Subtitles Studio**: Simultaneous bilingual subtitle rendering with custom secondary styling and independent track selection.
- ⚡ **Acoustic Subtitle Auto-Sync**: 1-Tap intelligent peak auto-synchronization aligning subtitle cues with dialogue.
- 📚 **Curated Franchises & Smart Box Sets**: Chronological saga grouping (Harry Potter, Resident Evil, MCU, Star Wars, LotR, etc.) with completion progress tracking.
- 🎨 **Movie Aesthetic Palette & Scene Clipper**: 5-swatch color harmony extraction with hex codes and 2–6s animated WebP scene sharing.
- 🌐 **LAN & Network Streaming**: Direct streaming from Windows SMB v2/v3, NAS, and WebDAV/DLNA endpoints without local storage overhead.
- 🌟 **HDR Profile Router & AMOLED Display Match**: Explicit manual switching between DV, HDR10+, HDR10 Base, and SDR Tone-Mapped, fixing tint issues on AMOLED HDR10+ devices.
- 🧹 **Pure Gimmick-Free Player**: Removed ambient glow, eco mode, and subwoofer rumble to ensure pure 120Hz/144Hz smoothness.

[Download v2.0.1](https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player/releases/tag/v2.0.1) · [Release notes](RELEASE_NOTES.md)

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
