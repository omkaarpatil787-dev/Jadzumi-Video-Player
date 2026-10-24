# Development checkpoint — library and playback tools

Updated: 2026-10-24. Stable public baseline: v1.3.3 (build 16).
Branch: feature/library-playback-tools. Changes remain separate from the published release.

## Authorized work

1. Accuracy and targeted reliability fixes: real ratings/metadata; remove fabricated technical facts; preserve settings when clearing history.
2. Local export/import of viewing history and app settings using Android's file picker.
3. “What did they say?”: replay the previous ten seconds with temporary voice boost, restoring the user's audio preference.
4. Offline subtitle search for locally selected SRT/VTT files, with timestamp jumps.

## Progress

- Planning checkpoint saved. Repository inspected; older M3 checkpoint preserved in docs/M3-CHECKPOINT-HISTORY.md.
- Found hardcoded 8.8 Wikipedia ratings, fixed codec/format claims in inspectors, and a settings export button that only displays a status message.
- Found clearWatchHistory clears unrelated playback/privacy preferences. Fix before adding backup.
- Stage 1 complete: removed fabricated Wikipedia scores and unverified cached ratings; TVMaze scores require a valid finite 0–10 value. Inspectors no longer claim fixed frame rate, color space, decoder or libass use. Codec badges use measured dimensions/MIME/HDR transfer.
- Clear Watch History now removes only hashed viewing records, preserving privacy and other preferences.
- Stage 1 validation: ./gradlew test assembleDebug passed; 33 tests per variant, zero failures/errors/skips. New MediaFacts tests cover missing/invalid ratings, codec accuracy and history-key isolation.
- Stage 2 complete: Export backup / Restore backup replace the placeholder. Versioned local file stores typed, allowlisted settings and hashed viewing history. Import validates all entries and size before mutation, asks for confirmation, merges data and attempts rollback on storage failure. Online consent, folder permissions, media and caches are excluded. Restart applies restored settings; reselect original folders after reinstall.
- Stage 2 validation: ./gradlew test assembleDebug passed; 37 tests per variant, zero failures/errors/skips. Backup tests cover round trips, type/range validation, unsupported/corrupt files, limits and consent exclusion.
- Stage 3 complete: player HUD now offers “What did they say?” (previous ten seconds, bounded at the beginning). Temporary voice boost never changes the saved preference. Precise seek mode is restored afterward; manual seek, pause, playback error and release cancel the temporary effect. If replay started paused, it returns to pause at the original position.
- Stage 3 validation: ./gradlew test assembleDebug passed; 39 tests per variant, zero failures/errors/skips. Replay-window tests cover duration bounds and the original-position endpoint. Audio effects/lifecycle still need device checks.
- Stage 4 complete: player subtitle settings can search a local UTF-8 SRT/VTT file (2 MB / 50,000 cue limit); case-insensitive results are capped at 100, and tapping a result seeks to its timestamp. Embedded/ASS/bitmap subtitle search is not supported. File display names now select the correct MIME when adding VTT/ASS via content providers.
- Stage 4 validation: ./gradlew test assembleDebug passed; 43 tests per variant, zero failures/errors/skips. Parser tests cover SRT/VTT timing, BOM/CRLF, tags, malformed/unsupported cues, search bounds and ordering.
- Preview version is 1.4.0-alpha.1 (build 17); public v1.3.3 is unchanged.
- Final review complete: library-level fabricated scores, years, classifications, studios and synopses removed; unknown values remain unavailable. A regression test checks missing metadata stays missing. Subtitle-result jumps now use exact seeking.
- Corrected existing Media3 opt-in declarations and the folder animation's ignored target state; no lint rules were disabled.
- Final validation: ./gradlew test assembleDebug lintDebug passed; 45 tests per debug/release variant, zero failures/errors/skips; lint has zero errors/fatal issues (24 warnings and 2 hints). Debug APK signature, version 1.4.0-alpha.1/build 17, five NextLib native libraries and six bundled license files verified.
- Evidence: docs/verification/features-1.4.0-alpha.1/result.json. Progress and next device checks: docs/FEATURE-PROGRESS.md.
- Ready for device evaluation, not a stable release. Device playback/file-picker/audio-effect checks remain NOT RUN. Next: collect results from the documented device checklist and fix feedback on this branch. Do not merge main just to save progress.

## Validation and continuation

Use /workspace/.cloud-setup/activate.sh in this environment, then ./gradlew test assembleDebug --no-daemon.
Record exact test/build outcomes and commit after each stage. Do not describe unrun device checks as passed.
Keep codec dependencies, signing credentials and released v1.3.3 unchanged.
Feature branches produce CI artifacts; pushing main automatically publishes a release. Do not merge unfinished features into main.
Save final device-check instructions for backup/restore, replay cleanup and subtitle timing before release.

## Watched-history follow-up

Completed flags are now read separately from resume positions: finished titles remain
watched when resume is zero, and watched/in-progress states cannot overlap. Unknown
durations no longer invent a completion threshold. Regression test added.

Latest local command: ./gradlew test assembleDebug lintDebug --no-daemon --console=plain.
Passed: 45 tests per variant, zero failures/errors/skips; lint zero errors, 24 warnings,
2 hints; debug APK rebuilt with native libraries and current license assets verified.

The preceding full preview at 05af368 passed signed GitHub CI run 40127402354 and
produced debug/release artifacts. The watched-history follow-up requires its own CI
result; do not describe the prior run as validation of this later commit.

## Technical inspector follow-up — 1.4.0-alpha.2 (build 18)

User reports the other preview features work well and requests inspector completion.
The former placeholders were removed without an accompanying media probe, leaving
empty fields. New local header inspection and playback analytics now populate real
information; fields the file/OS does not expose stay explicitly unreported.

New files: MediaInspection.kt (models/formatting), LocalMediaProbe.kt (off-thread
header reads with resource cleanup/cache), TechnicalInspector.kt (UI/loading/refresh),
and MediaInspectionTest.kt. Runtime decoder data is guarded by player/file identity.

First implementation pass: test/build/lint passed with 50 tests per variant.
Final alpha.2 validation passed: 50 unit tests per variant, zero failures/errors/skips;
debug APK and lint passed with zero errors/fatal issues, 25 warnings and 2 hints.
APK version/signature, five NextLib native libraries and six license files verified.
Evidence: docs/verification/features-1.4.0-alpha.2/result.json. Device probe/playback checks
remain NOT RUN. Source and new helpers are checkpointed in commit 410b724. The completed batch is pushed to the feature branch; signed CI validation is pending.

The earlier alpha.1 watched-history follow-up at b3bb02a also passed signed GitHub CI
run 40128340965, producing debug and release artifacts. This is historical evidence,
not signed-build verification of the inspector follow-up.

## Push batching / scope

User requests one push when the agreed feature set is complete. The initial agreed
sequence (accuracy/reliability, backup/restore, dialogue replay, subtitle search, plus
the inspector correction) is complete in code and locally validated. Batch these
completed changes on the feature branch; keep main/stable unchanged for device review.
An optional scope question asks whether to include the remaining ideas too. Default
is to finish the agreed set first; incorporate any later reply as further steering.
Keep saving local checkpoint commits for additional work.

## Expanded batch — explicit user scope

User explicitly requests all remaining ideas: episode continuity, sleep controls,
travel check, private handoff and scene bookmarks. Save local checkpoint commits
throughout; HOLD PUSH until the entire expanded batch is implemented and verified.
Keep stable/main unchanged.

Implementation choices:
- Continuity: per-series language preferences, explicit missing/uncertain episode confirmation,
  and end-of-file continuation without skipping content on guessed credit timestamps.
- Sleep: timed fade/pause, end-current-episode and stop-after-N-episodes controls.
- Travel: local access/subtitle/episode-gap checks and clearly distinguished previous
  playback evidence; no claim that header inspection proves a file will play.
- Handoff: portable progress via a user-exported file, without accounts or an app server.
  Match a local media fingerprint, confirm before applying; no automatic network transfer.
- Bookmarks: named timestamps and personal notes saved locally, with timestamp/notes export.

Next: implement continuity/sleep/bookmark foundations, test and checkpoint locally,
then wire travel checks and portable handoff. Do not push partial stages.

## Latest scope and push request — alpha.3 (build 19)

User explicitly deferred travel check and private handoff and requested pushing this
build. Completed additions: episode continuity/per-series language preferences, sleep
controls and named scene bookmarks. Deferred files are outside the repository under
/workspace/.cloud-setup/deferred-travel-handoff; none are shipped or staged.

Final frozen-source validation PASSED: 57 tests per variant, zero failures/errors/skips;
debug APK and lint passed (zero errors, 26 warnings, 2 hints). Version/build/signature,
five NextLib libraries and six bundled license files verified. Evidence:
docs/verification/features-1.4.0-alpha.3/result.json. Resumed after the battery pause;
fixed explicit expression grouping/braces flagged by lint before this successful run.
Push the completed batch to feature/library-playback-tools after checks, not main.
Stable v1.3.3 remains unchanged. Device validation of the new controls is still NOT RUN.

Prior alpha.2 signed CI run 40130879855 succeeded and produced both APK artifacts.
This is historical evidence; alpha.3 requires its own signed CI build.
