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
- Next: apply local backup/restore implementation. No device playback checks performed.

## Validation and continuation

Use /workspace/.cloud-setup/activate.sh in this environment, then ./gradlew test assembleDebug --no-daemon.
Record exact test/build outcomes and commit after each stage. Do not describe unrun device checks as passed.
Keep codec dependencies, signing credentials and released v1.3.3 unchanged.
Feature branches produce CI artifacts; pushing main automatically publishes a release. Do not merge unfinished features into main.
Save final device-check instructions for backup/restore, replay cleanup and subtitle timing before release.
