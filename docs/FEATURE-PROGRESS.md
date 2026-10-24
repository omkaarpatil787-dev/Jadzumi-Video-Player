# Library and playback tools · development progress

Preview: **1.4.0-alpha.3 · build 19**. Stable public release: **v1.3.3**.
Branch: `feature/library-playback-tools`. This preview is not merged into main.

## Saved stages

| Stage | Saved commit | Validation at that checkpoint |
| --- | --- | --- |
| Plan and baseline | `71a1407` | Original M3 checkpoint archived; implementation plan recorded |
| Accuracy and history isolation | `d31c273` | 33 unit tests per variant; debug APK build passed |
| Local backup/restore | `3c8a7c9` | 37 unit tests per variant; debug APK build passed |
| Dialogue replay | `3fe21bd` | 39 unit tests per variant; debug APK build passed |
| Local subtitle search | `cda20e3` | 43 unit tests per variant; debug APK build passed |
| Accuracy review and lint | `05af368` | 44 tests per variant; debug build/lint passed; signed CI run 40127402354 passed |
| Watched-history follow-up | See latest checkpoint commit | 45 tests per variant; debug build/lint passed; CI pending |

The final review also removes library-level invented scores, years, classifications,
studios and synopses; missing facts stay unavailable. Search-result seeks use exact
positioning. Completed viewing records remain watched even though their resume position
is zero; watched and in-progress states no longer overlap. Final verification is recorded in CHECKPOINT.md.

## Where to find the additions

- **Settings → Privacy & About → App Data & Cache:** Export backup / Restore backup.
- **Player top bar → Replay 10:** “What did they say?” replays up to ten seconds with
  temporary voice boost, returning to the original position. It restores normal audio
  behavior without changing the saved voice-boost preference.
- **Player → Cinema settings → Subtitles → Search a local SRT / VTT file:** select a
  UTF-8 file, enter a phrase, and tap a result to jump to the line's timestamp.

Backups are versioned local files (up to 1 MB) containing allowlisted app settings and
hashed viewing-history records. They do not contain media, cached artwork, folder
permissions, signing material, or online-lookup consent. Imports merge matching records
only after full validation and user confirmation. Settings take effect after restarting.
After reinstalling, re-add the same media folders; moved files or a different document
provider may not match saved history. Keep backup files private.

Subtitle search supports text SRT/VTT only (up to 2 MB / 50,000 cues; first 100 search
results). It does not index embedded, bitmap, or ASS subtitle tracks. Existing playback
support for these formats is preserved. Search timestamps use the selected file's
original timeline, not an inferred transcript or cloud service.

## Device checks — NOT RUN

Use the signed **Release artifact from the feature-branch CI run** for a device already
running the production-signed app. The locally built debug APK uses the development key
and cannot update a production-signed installation. Do not uninstall an existing app
unless you have intentionally accepted the loss of its local data.

### Backup and restore

1. Save progress on two videos; change theme, subtitle appearance and a playback setting.
2. Export a backup through the system file picker, cancel a second export, then confirm
   the saved file exists. No success message should appear for cancellation.
3. Modify the settings/progress, open Restore backup and cancel the confirmation.
   Nothing should be changed.
4. Restore the saved file, restart Jadzumi, and confirm saved settings/progress return.
   Verify completed titles remain watched, resumed titles remain in progress, and online
   lookup consent remains exactly as it was before restore.
5. Try an invalid, unsupported-version, oversized, unreadable or permission-revoked file.
   Confirm no existing settings/history are modified.
6. Clear watch history and verify online consent, voice boost, delay and appearance
   settings remain intact. Folder access must still work.

### Dialogue replay

1. During normal playback, tap Replay 10. Confirm the previous ten seconds replay and
   temporary boost ends at the original position. Try within the first ten seconds too.
2. Start while paused; confirm playback returns to pause at the original position.
3. Repeat with saved voice boost on and off, and at different playback speeds.
4. Pause, seek, close, switch videos, background/reopen, and trigger a playback error
   during replay. Confirm boost and seek precision do not remain stuck.
5. Verify short clips, unseekable files, Bluetooth changes, audio-session changes and
   device audio-effect availability. Audio enhancement varies with device support.

### Subtitle search and playback regression

1. Pick UTF-8 SRT and VTT files (including VTT through a document provider). Search
   mixed-case phrases; tap results and check precise timestamp jumps.
2. Try no matches, malformed/oversized input, ASS input and cancellation. Confirm clear
   feedback, no unwanted seek and no change to the selected playback subtitle track.
3. Add external VTT/ASS tracks through the normal picker and confirm MIME/format works.
4. Recheck embedded/external subtitles, subtitle timing, audio delay, resume positions,
   seeking, background playback, audio focus, picture-in-picture and next-episode flow.
   Existing timing controls require real playback validation; no new timing pipeline is claimed.

## Continue later

Read CHECKPOINT.md, inspect the branch and working tree, then run the recorded build
command. Keep codecs and signing material intact. Resolve device feedback before a
stable release; merge to main triggers automatic publication, so do not merge simply
to save progress.


## Technical inspector follow-up · alpha.2

The earlier accuracy cleanup removed static values but did not yet read file headers.
This follow-up completes the inspector rather than restoring guessed values.

- Local file headers supply codec, dimensions, reported frame rate, color metadata,
  audio channels/sample rates/languages and embedded subtitle track formats when exposed.
- Reads run off the UI thread, are serialized and cached for up to 32 files, and have
  a refresh control. Unsupported fields and revoked access produce explanatory text.
- Playback analytics report actual audio/video decoder names and format information.
  Hardware/software classification uses Android's codec flags on Android 10+, or known
  FFmpeg software decoding. Older Android versions may not report hardware status.
- Runtime decoder details only apply to the same inspected URI. TV episode inspection
  follows the loaded episode within the selected season, otherwise the first episode.
- The movie audio/subtitle card now shows probed tracks instead of placeholder chips.
- No codec dependency, renderer-selection policy, or playback feature was removed.

Device checks (NOT RUN): inspect a known H.264 MP4 and HEVC MKV before playback; compare
resolution/fps/audio metadata against a trusted file report. Start/pause playback and
check decoder names/hardware status. Switch files rapidly and confirm no stale decoder
information. Try files with multiple audio/subtitle tracks, an unsupported container,
revoked access, and the refresh button. Confirm background inspection does not interrupt
playback. Recheck the other preview features after this patch.


## Delivery scope

The agreed first delivery is implemented: accuracy/reliability corrections, local
backup/restore, dialogue replay, subtitle search, and the inspector correction.
It remains a preview for device review. Episode continuity, sleep controls and scene bookmarks are now included in alpha.3.
Travel check and private handoff are deferred by the user’s latest instruction.


## Alpha.3 — continuity, sleep and scenes

The user expanded the scope, then explicitly deferred travel check and private handoff
and requested this build be pushed. Their incomplete implementation files are excluded.

New player menu entries:
- **Episode continuity:** autoplay only starts after the file ends. Adjacent numbered
  episodes in the same season may auto-continue after a 12-second cancelable countdown.
  Gaps, other series, season changes or unverified numbering always need confirmation.
  Per-series audio/subtitle language preferences use actual language tags; unlabeled
  tracks fall back to automatic selection. Subtitle-off preferences are remembered.
- **Sleep controls:** 15/30/60/90-minute stops with a final 10-second audio fade, stop at
  the end of the current video, or stop after two/three video endings. Canceling or
  closing restores volume; timer deadlines use the monotonic device clock. Sleep
  controls last for the current app/player session and are not restored after process death.
- **Scene bookmarks:** playback pauses for notes. Save up to 200 named moments per video,
  add personal notes, edit/delete, tap to seek precisely, or export a plain-text timestamp
  list without copying video. Total stored notes are limited to 64 KB per video.

Backup version 2 includes scene bookmarks and per-series language preferences, while
still accepting version-1 backups and preserving online consent. Device-specific
folder permissions and credentials remain excluded.

### Additional device checks — NOT RUN

1. Finish adjacent episodes and verify the countdown starts at EOF, not estimated credits.
   Cancel it, disable/re-enable autoplay, and verify missing/duplicate episodes and season
   changes show confirmation. Test the same series with different track languages.
2. Select a labeled audio/subtitle language, move to another episode, and confirm it is
   retained for that series. Verify another series can have its own choice, subtitle-off
   persists, and unlabeled tracks do not save translated language names as tags.
3. Use a one-minute timer through the controller/test setup or the normal 15-minute UI
   option. Verify fade/pause, cancel during fade, background audio, PiP, close/reopen,
   stop-at-current-end and stop-after-two across adjacent episodes. Confirm volume returns.
4. Add/edit/delete scene names and multiline notes, seek to beginning/end bookmarks,
   export text through the picker, cancel export, and restart to verify saved scenes persist.
   Back up/restore scene notes along with per-series preferences and online consent.
5. Repeat technical inspector and existing preview regression checks after these changes.

## Motion and inspector polish

- Inspector values consistently share a right-aligned column, wrapping naturally on
  narrow screens. Empty Android track scans no longer imply that an MKV has no tracks;
  playback may expose additional information. Color information remains measured.
- Display & Appearance → Motion & touch offers gentle/expressive springs and optional
  touch lighting. Reduced Motion, the existing lively-animation switch, and Android's
  disabled animation setting override the added effects.
- Library card presses, dock items and player menu rows have bounded spring compression
  and touch-position lighting. Navigation uses configurable springs. Unconsumed scroll
  edges stretch slightly and return without consuming child scroll distance.
- The playback timeline lights around the scrub position only while scrubbing.
- Effects use graphics layers/draw-phase updates, without continuous glow loops or blur.
  No measured frame-rate improvement is claimed: tablet/phone frame pacing, rapid taps,
  long lists, TalkBack, reduced motion, PiP and scrubbing still require device checks.
- These effects apply inside Jadzumi; Android system quick settings and call widgets
  are outside this app's UI.
