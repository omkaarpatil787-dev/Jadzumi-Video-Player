# Approved scope and incremental implementation gates

Current status (2026-10-19): M2 is user-approved; M3A.1 core playback passes internal build/tests/lint/signature checks and awaits Pad 5 testing/user approval. The six subdivisions below are historical working notes, not a replacement for the subsequently approved eight-main-milestone roadmap. Preserve those main milestones. M3A internal gates are Core Playback (3A.1), Cinema Formats (3A.2), and System Integration (3A.3); current source review remains within unfinished core playback and its existing PiP integration. Recover the exact current master roadmap before advancing to later work.

## Binding decisions recovered 2026-10-18

Jadzumi Video is the active project. Kotlin + Jetpack Compose, native Android, no WebView. Xiaomi Pad 5 11-inch 16:10 landscape is first; phone support follows stable tablet work. Six attached approved tablet HTML files are the visual references. Keep pale-blue/white Material 3 Expressive direction, rounded surfaces, restrained glass, fluid motion. Remove duplicate/technical chrome where appropriate. USB appears only when attached; Private Folder only when enabled.

Offline playback and library, no accounts/ads/tracking. Online metadata lookup is not authorized for this offline-first build. V1.0 includes compatible MP4/MKV, hardware/software decoding strategy, HDR where supported, AC-3/E-AC-3, surround downmix, subtitles/audio tracks, sync, gestures, resume, fullscreen, PiP and lock. Atmos/output and HDR promises require actual device/format verification; design mockup labels are not evidence of capabilities. Windows interpolation is separate; Android motion enhancement is later.

Workflow: build → test → fix → retest → user approval → approved checkpoint → next milestone. Preserve provisional checkpoints throughout. `build`, `resume`, `save` refer to this project by default.

## Working subdivisions of the approved foundation phase

These small implementation gates make the broader recovered roadmap executable; they are not claimed to be an exact recovered numbered roadmap.

1. **Native foundation/UI preview (M1 approved visually on Pad 5)**: buildable Android project, navigation, preview library/details, folder structure, saved appearance, honest placeholder handling. Compile/unit tests/lint; user device/UI review.
2. **Real offline library (M2 user-approved)**: SAF folder grants, removable storage handling, indexing and persistence, local artwork and metadata, empty/error/revoked-permission handling, actual details.
3. **Core cinema playback**: Media3 integration, lifecycle/release, MP4/MKV, seeking/resume, mini-player/fullscreen, PiP, lock, aspect ratio/speed and gestures. Real-file device tests.
4. **Cinema audio/subtitles**: verify AC-3/E-AC-3/downmix, investigate FFmpeg software extensions and packaging, subtitle/audio tracks and synchronization. Styled ASS may require a separate renderer. Build a format/device matrix before claims.
5. **Tablet polish and release**: remaining settings, expressive motion, accessibility, performance profiling, regression/security checks, release signing and installation validation. Stable tablet v1.0 gate.
6. **Phone phase**: adaptive layouts/player/navigation, Samsung and other phone tests.

Later version direction: 1.1 performance/subtitle refinement; 1.2 smarter local library with local metadata cache; 1.3 experimental motion enhancement; 2.0 further premium refinement.

Never advance a gate without user approval. Do not fabricate APKs, runtime tests, storage statistics, codecs or completed features.
