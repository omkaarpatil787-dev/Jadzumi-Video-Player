# Jadzumi Video — active Milestone 3 checkpoint

## SDK installation continuation — 2026-10-19, 12:02 Europe/Warsaw

User authorized attempting SDK/Gradle installation and resuming only after success.

- Android command-line tools ZIP downloaded successfully (164760899 bytes); ZIP CRC passed, sdkmanager reports version 19.0.
- A single sdkmanager invocation installed Android platform 36, build-tools 35.0.0 and platform-tools with exit code 0. Required android.jar, aapt, apksigner and adb files exist. JDK 17 remains available.
- SDK location in this session: /workspace/scratch/a2b20457baed/toolchain/android-sdk. Set ANDROID_HOME to this path when building here. Tool binaries are environment prerequisites, not included in this source checkpoint; a future environment may require reinstalling them.
- A single official Gradle 8.11.1 GET returned HTTP 307 to GitHub. Redirect was not followed. Gradle is not installed. Earlier no-GitHub instruction remains in force; explicit authorization for the official Gradle GitHub release download is needed unless a verified local distribution is supplied.
- No Kotlin/Gradle source or design changes. No compile, unit tests, lint, M3 APK or device tests. Source-only checks from the preceding continuation still apply. M3A.1 remains the active gate; no M3A.2 advancement.

Evidence: docs/verification/m3/20261019T100154Z/.


## Controlled continuation — 2026-10-19, 11:59 Europe/Warsaw

- Resumed from the user-attached latest checkpoint; ZIP CRC passed and all 82 manifest hashes matched before changes. Reviewed RESUME.md and the existing PlaybackController, PlaybackModel and MainActivity fixes. No Kotlin, Gradle, design, signing key or existing APK changes were made.
- JDK 17.0.20 is present. No local Gradle, sdkmanager, adb, configured SDK or checked cached Gradle launcher/android.jar was found.
- One bounded parallel HEAD check per endpoint, without following redirects: Android command-line tools, Google Maven, Maven Central and Gradle plugin marker returned HTTP 200. Responses do not prove download/install or full dependency resolution.
- Both services.gradle.org and downloads.gradle.org for pinned Gradle 8.11.1 returned HTTP 307 to github.com/gradle/gradle-distributions. No GitHub requests, repeated blocked attempts, alternate mirror experiments or dependency/version changes were made. SDK installation was not started because Gradle remains blocked under the no-GitHub constraint.
- Source-only gate passed (preserved artifact/design hashes and source manifest checks). No Kotlin compile, Gradle unit tests, lint or device testing ran; no M3 APK exists. M2 remains the last verified APK, preserved as historical output only.
- Active gate remains M3A.1. No M3A.2 advancement. Continue only when pinned build prerequisites can be supplied within the authorized constraints, then run tools/verify_m3.py and address real build results before Pad 5 testing.

Evidence: docs/verification/m3/20261019T095903Z/access-check.json and docs/verification/m3/20261019T095903100926Z/result.json.


## Latest controlled continuation check — 2026-10-19, 11:52 Europe/Warsaw

Active gate remains M3A.1 Core Playback. No UI redesign, milestone advancement, source rollback or GitHub use occurred.

- Both saved M3 ZIP copies are byte-identical, SHA-256 b1b21b2eedd76d79808ea43ff216f5bcded97505404158b39f1d65d777e20ca1. Isolated extraction, CRC and all 79 manifest entries passed. The existing working tree matched every extracted file, with no extra or newer source differences.
- Reviewed RESUME.md and the existing PlaybackController, MainActivity/PiP and PlaybackModel fixes. No Kotlin or Gradle source was changed.
- One bounded access check per endpoint: Android command-line tools, Google Maven AGP POM, Maven Central JUnit POM and Gradle plugin-marker POM returned HTTP 200. These are access responses, not proof of successful installation or complete dependency resolution.
- Gradle 8.11.1 official distribution returned HTTP 307. A single non-following download request confirmed the redirect destination is github.com/gradle/gradle-distributions. No request to GitHub was made. No blocked retry loop or alternate mirror experiments were attempted.
- No local Gradle or sdkmanager was found, no SDK is configured, and no cached Gradle launcher/distribution or android.jar was found in the checked tool/workspace locations. Java is present.
- Current blocker is obtaining the pinned Gradle distribution under the user's explicit no-GitHub constraint. The previous blanket network-timeout diagnosis is historical; the required repository endpoints now respond. Android SDK installation was not started once the Gradle blocker was confirmed.
- No M3 compile, tests, lint or APK export was performed. The 16 JUnit methods remain unexecuted against M3; M2 remains the last verified APK. Do not call this a successful or failed Kotlin build.

Evidence: docs/verification/m3/20261019T095227Z/access-check.json and gradle-download-result.json. Stop after preserving this checkpoint. Future continuation must respect the no-GitHub instruction or obtain explicit revised authorization; never follow a prohibited redirect silently. Do not advance to M3A.2 without approval.


Updated 2026-10-19, Warsaw morning. M2 is user-approved. M3 core playback is unfinished; do not revert to M2 or advance the milestone gate. Latest source has NOT passed Kotlin compilation, Gradle tests, lint or device testing. No M3 APK is included.

## Current source progress

The saved recovery source includes Media3 content-URI playback, audio focus/MediaSession, seek/resume, completion, fullscreen/mini player, PiP actions, gestures/lock, speed/fit controls, scrub previews and play/pause morph.

Subsequent review fixes now included:
- Preserve saved position on early initialization failure; reject released-player callbacks; retry playback errors.
- Restart a player stopped into STATE_IDLE; synchronize speed from player events.
- Separate requested playback from actual playback so buffering transport icons/PiP actions represent the operation they perform; keep screen awake while playback is requested.
- Save on pause/play-intent changes and position discontinuities, not only periodic polling; show live resume progress in details and load each new video's own persisted position.
- Reset transient menus/scrub/gesture state on video change; clear obsolete preview frames; use integer seek arithmetic with saturation.
- Calculate PiP bounds in window coordinates and exclude fit-mode letterboxing/pillarboxing; keep intrinsic aspect separate from PiP platform limits; disable unavailable seek actions; clear stale PiP actions/bounds on close.
- Restore previous window brightness when leaving the player.
- Rename speed mutation to changeSpeed, avoiding collision with the delegated speed property's generated setter.

## Actual verification

- Preserved design-reference and APK SHA-256 checks: PASS.
- Source manifest XML/no requested permissions/backup disabled/PiP declaration: PASS.
- Python build-verification script parses and its source-only and missing-SDK branches were exercised: PASS.
- Current-source diff reviewed; no new dependencies or media write/delete operations added.
- 16 JUnit methods are present (7 baseline, 9 playback). NONE ran against this M3 source. New cases cover exact seek arithmetic and PiP bounds for landscape, portrait, crop/stretch and invalid dimensions.
- Gradle compilation, unit execution, lint, merged-APK permissions/signatures, dependency security review and Pad 5 runtime testing: PENDING.
- Build-gate evidence: docs/verification/m3/20261019T044708962725Z (source checks), 20261019T044709033573Z (blocked prerequisites). Older M1/M2 evidence is historical only.

## Build blocker and next action

JDK 17 is available. Android SDK 36/build-tools 35.0.0 and Gradle/Kotlin dependencies are unavailable. Official Android SDK and Gradle download attempts returned proxy CONNECT timeouts. This is an environment blocker, not a successful or failed Kotlin compile.

When permitted dependency access is available:
1. Configure JDK 17 and ANDROID_HOME with platform 36/build-tools 35.0.0. Preserve wrapper 8.11.1, AGP 8.10.1, Kotlin/Compose 2.2.10, Media3 1.11.1.
2. Run `python3 tools/verify_m3.py` from the project root. It runs one Gradle invocation for assembleDebug/testDebugUnitTest/lintDebug, then checks reports, APK signing against M2 and key permissions before copying artifacts/Jadzumi-Video-M3-debug.apk. Its success branch remains untested here.
3. Fix actual compilation/test/lint issues and rerun the gate. Keep source and evidence together; never label source checks as runtime tests.
4. Run docs/M3-DEVICE-REVIEW.md on Xiaomi Pad 5 with real internal/USB files. Prioritize resume across video switches, buffering controls, failure/retry, PiP continuity/dismissal, lifecycle, audio focus and source-file safety.
5. Save checkpoint plus verified M3 APK if one exists; obtain user milestone approval before advancing.

## Provenance and preserved files

The original durable latest-checkpoint was M2 version 1; the earlier M3 upload had not completed. The first recovered M3 source was reconstructed from recorded source-writing commands, not proven byte-for-byte against the lost temporary ZIP. It was saved before continuing development. This checkpoint contains later review edits as described above; older versions remain recovery points.

M2 library/index/import/search/details/thumbnails, all six locked design references, application ID, development signing identity and M1/M2 APKs remain preserved. Do not share the source/signing key with APK testers. Do not imply codec/HDR support from Media3 integration. Cinema-format validation and further system integration remain gated work. The historical working roadmap is not a replacement for the approved eight main milestones.

## Preservation pause — 2026-10-19 06:58 Europe/Warsaw

User requested preservation only with 7% usage remaining. Stop and wait for explicit resume. Do not retry SDK downloads or start another milestone. All source changes through the latest core playback/PiP fixes are retained.

Latest blocker diagnosis: the configured session network allowlist excludes Android/Gradle download hosts. The proxy was reachable and a permitted host responded, while SDK requests timed out. The final official SDK/Gradle download retry produced no files; checking processes also encountered an automatic approval-service capacity failure. No SDK installation or M3 compilation succeeded. Future continuation needs permitted build dependency access; the source ZIP is complete but does not include the missing SDK or dependency binaries.

Preservation checks: archive existed, CRC passed, every manifest hash matched, and extraction into a separate temporary directory succeeded with matching extracted hashes. All packaged files matched the current working source before this documentation-only update. No playback source, Gradle configuration, design reference, signing key or APK was changed during this preservation pass.
