# Recovery verification

Recovered from recorded source-writing commands; not byte-for-byte certified against the lost M3 ZIP. Baseline obtained from saved M2 archive and left intact.

Source/config changes relative to M2:

- app/build.gradle.kts
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/jadzumi/video/CinemaPlayer.kt
- app/src/main/java/com/jadzumi/video/JadzumiApp.kt
- app/src/main/java/com/jadzumi/video/LocalLibraryUi.kt
- app/src/main/java/com/jadzumi/video/MainActivity.kt
- app/src/main/java/com/jadzumi/video/PlayPauseMorph.kt
- app/src/main/java/com/jadzumi/video/PlaybackController.kt
- app/src/main/java/com/jadzumi/video/PlaybackModel.kt
- app/src/main/java/com/jadzumi/video/ScrubPreview.kt
- app/src/test/java/com/jadzumi/video/PlaybackModelTest.kt
- build.gradle.kts

SHA256SUMS.json records every packaged file except itself. ZIP CRC and source hash consistency checked during creation. No compilation claim.

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

