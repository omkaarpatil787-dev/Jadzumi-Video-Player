# Historical recovery and first source review notes

The current status is in ../RESUME.md. The following records earlier snapshots, including their then-current test counts.

# Jadzumi Video — Milestone 3 recovery checkpoint

## Latest resumed progress — 2026-10-19, source review pass

M3 remains active and unfinished. The recovered source was saved before these edits.

Changes since the recovery snapshot:
- PlaybackController: preserve resume position when initialization fails before assigning a media item; ignore callbacks from a released player; retry errors through Play; reset aspect for a newly opened video; clear completion when playback resumes.
- CinemaPlayer: reset menus, scrubbing and gesture state when video identity changes; refresh PiP parameters and clear obsolete bounds when the player UI closes.
- ScrubPreview: clear the previous preview while a new target loads.
- PlaybackModel: replace floating-point seek addition with saturating integer arithmetic.
- PlaybackModelTest: add exact-integer and negative-overflow regression cases.

Validation actually performed: reviewed source diff against the saved recovery ZIP; parsed AndroidManifest.xml; verified no requested permissions; compared all design references, APKs and signing files byte-for-byte with the saved checkpoint. All these checks passed. The project has 12 JUnit methods (7 baseline and 5 playback); NONE were executed in this environment. Kotlin compilation, Gradle tests, lint, dependency checks and Pad 5 runtime checks remain pending. No M3 APK exists.

Build blocker: both official Gradle and Android SDK download probes failed with proxy CONNECT timeouts. JDK 17 exists; Android SDK, Gradle distribution and Kotlin compiler are unavailable. Resume by restoring permitted dependency access and running assembleDebug, testDebugUnitTest and lintDebug sequentially. Do not infer functional success from source review.

Before M3 approval, specifically exercise: revoked URI during open/retry without losing saved position; close and immediately open another video; switching videos during scrub; completed-video replay from MediaSession/PiP; closing fullscreen then pressing Home (no stale auto-PiP); fullscreen/mini/PiP continuity and rotation. Cinema-format and device validation remain required.


Saved: 2026-10-19. Active milestone: M3, unfinished and not build-verified.
M2 was approved by the user. Continue M3; do not revert to M2.

The previous save did NOT complete its durable upload. The verified saved archive was M2 version 1, last modified 2026-10-18T19:23:05Z. The temporary M3 archive was lost when scratch reset. M3 source was reconstructed from the source-writing commands retained in the conversation, over a separate copy of the verified M2 archive. This is not a proven byte-for-byte recovery of the lost ZIP. Original M2 files remain separately preserved.

## Recovered progress

- PlaybackController.kt: Media3 local content-URI playback, audio focus, MediaSession, position persistence, completion handling, speed/seek, lifecycle release and restore.
- CinemaPlayer.kt: fullscreen/mini player, immersive controls, gestures, lock, speed and fit menu, thin seek bar, errors and retry, PiP integration.
- MainActivity.kt: native PiP with playback actions and lifecycle handling.
- ScrubPreview.kt: debounced scaled keyframe previews.
- PlayPauseMorph.kt: animated play/pause icon with reduced-motion support.
- PlaybackModel.kt and PlaybackModelTest.kt: bounded seek/resume helpers and four new tests (not yet run).
- JadzumiApp.kt and LocalLibraryUi.kt: player integration and Play/Resume entry point.
- AndroidManifest.xml: PiP and configuration handling.
- Gradle configuration: Media3 1.11.1, Kotlin/Compose plugin 2.2.10, AGP 8.10.1, compile SDK 36; app 0.3.0-m3/code 3, target 35/min 26, ARM64.

## Preserved baseline

M2 offline folder import, SQLite library, search/details/thumbnails, persisted grants, existing tests, all six locked design references, development signing identity and M1/M2 APKs remain included. No M3 APK exists. Included APKs and historical build/test evidence are M1/M2 only. Original M2 report is docs/M2-TEST-REPORT.md.

## Verification limits

Archive CRC and file hashes are checked when packaging. Source reconstruction has not yet passed compilation, lint or device testing. The prior M3 build failed on Kotlin metadata compatibility; the toolchain upgrades were made but the next build was interrupted. Do not treat UI/features as validated or codec support as guaranteed. HEVC Main10, AC-3/E-AC-3, SRT/complex ASS and HDR fallback remain validation/integration work.

## Resume

1. Preserve this recovery snapshot before further edits.
2. Use JDK 17, Gradle wrapper 8.11.1 and Android SDK 36; restore build tools outside source.
3. Run a single clean build, unit tests and lint; fix compile/lifecycle failures, record real evidence.
4. Validate local playback, resume, completion, seek, rotation, mini player, PiP actions and revoked URI behavior on Pad 5.
5. Save a new checkpoint with actual validation status before stopping. Keep existing signing identity and design references. M3 approval is required before advancing.

## Resume attempt after recovery was saved

2026-10-19: recovery ZIP was successfully saved before further work. Reviewed PlaybackController, MainActivity and CinemaPlayer source. JDK 17 is available, but Android SDK and Gradle are absent. Android command-line tools download timed out after 20 seconds; Gradle distribution probe failed with proxy CONNECT timeout after 8 seconds. No build or tests could run. No source changes were made after recovery. Next action: restore permitted access to Android/Gradle dependencies, compile/test/lint the recovered M3 source, then investigate runtime issues on Pad 5.
