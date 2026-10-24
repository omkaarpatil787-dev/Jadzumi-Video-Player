# Jadzumi v1.4.0 — A smoother experience ✨

A major round of playback, library and interaction improvements, with the existing
Media3 / NextLib codec support preserved.

### ✨ Motion that feels more natural

- Spring transitions for tabs and detail cards, elastic feedback at scroll edges,
  gentle card compression and touch-position lighting.
- Scrubbing lights up the playback timeline; player menus and dock items respond to touch.
- Customize gentle or expressive springs and touch lighting in **Display & Appearance**.
  Reduced Motion and Android's animation setting override the new effects.

### 🎬 More control over playback

- Dialogue replay, offline SRT/VTT subtitle search, and named scene bookmarks with notes
  and plain-text timestamp export.
- Sleep timer with a final audio fade, stop at the end of a video, or after several episodes.
- Episode continuation starts after the file ends. Gaps, season changes and uncertain
  episode numbering require confirmation. Audio/subtitle language choices can follow a series.
- Optional **Match display refresh rate**, off by default, requests seamless matching
  where Android and the display support it. No frame interpolation or forced mode changes.

### 🔧 Under the hood

- Local technical inspection reads actual file headers and runtime playback information.
  Values align consistently; information Android does not expose stays clearly qualified.
- Local settings/history backup and restore now include scene bookmarks and per-series
  preferences. Online consent, credentials and folder grants remain excluded.
- History clearing preserves settings and consent; completion and resume state are separate.
  Metadata displays avoid invented ratings and other unsupported details.
- Interaction effects use graphics layers and draw-phase updates without a continuous glow loop.
  Frame-rate gains have not been benchmarked across devices.

### 🔄 Updates, on your terms

**Privacy & About → App updates** now has its own card. Check manually, or opt into
at-most-daily checks when opening the app. Automatic checks are off by default and
have separate consent from online metadata.

Newer stable releases show release notes and offer Update, Later and Skip version.
Downloads show progress and verify the APK package, version, increasing build code
and signing certificate. Tap Install after download; Android requires installation
approval and may first ask you to allow Jadzumi to install unknown apps.

### Installation and validation

Download **Jadzumi-Player-v1.4.0.apk** below. Android 8.0+ / ARM64.
Same-production-key updates preserve settings and history. Older development-key or
debug installations cannot update to a production-key APK; uninstalling can erase data.

The preceding preview's motion and playback changes were reported working by the
user. This release also adds the requested separate update card. Local checks cover
unit tests, debug assembly and lint; GitHub builds the signed release APK. Device
refresh matching and a full newer-same-key update installation still need device testing.

Free software under GPL-3.0. Source is included with this tag; offline notices are in
About → Licenses. Artwork redistribution evidence, complete native Corresponding Source
and other legal review items remain open in THIRD_PARTY_LICENSES.md and
LEGAL_REVIEW_REQUIRED.md; no legal clearance is claimed.
