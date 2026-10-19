# Milestone 3 core playback — Xiaomi Pad 5 review

Status: device tests NOT RUN. M3A.1 debug APK now passes build, all 16 unit tests, lint (zero errors/two warnings), and signature/permission checks. This checklist is a procedure, not evidence of runtime success. Install the exported M3 APK as an update; its signer matches M2. Do not uninstall M2 first.

Record OS/ROM, app version, source checkpoint, file container/codecs, storage provider, steps and result for each case. Use owned test media. No source video should be altered by any player action.

| Case | Procedure | Expected result | Result |
|---|---|---|---|
| Baseline MP4 | Play H.264/AAC MP4 from internal SAF folder for 10 minutes | Picture/audio progress normally; no drift, crash or unexpected navigation | NOT RUN |
| Baseline MKV | Play a known compatible H.264/AAC MKV | Correct duration, seeks and playback; container support assessed separately from codecs | NOT RUN |
| Pause/buffering | Toggle during initial loading and while ready | Pause icon means playback is requested, including buffering; Play means paused/ended/error | NOT RUN |
| Seek limits | Back 10s near beginning; forward near end; scrub backward/forward | No negative position; end handled cleanly; preview approximate and not stale | NOT RUN |
| Resume/relaunch | Play 60s, pause, close video, reopen; then background/relaunch | Resume near saved position; details label updates when paused/closed | NOT RUN |
| Completion | Finish video, close, reopen; replay from player controls | Finished video begins at zero; resumed playback clears completion | NOT RUN |
| Restart after stop | Stop via connected media controller, then press Play in app | Player prepares and resumes instead of remaining idle | NOT RUN |
| Mini/fullscreen | Toggle mini/fullscreen repeatedly during playback | Same player, uninterrupted audio, stable position; no duplicate sessions | NOT RUN |
| PiP entry/exit | Enter by button and Home gesture; restore fullscreen repeatedly | Correct system-limited aspect, smooth content bounds; playback continuity | NOT RUN |
| PiP controls | Pause/resume/back/forward; test unavailable seeks | Labels match action; unavailable seeking disabled; no restart from ordinary transition | NOT RUN |
| PiP dismissal | Close PiP; reopen app; close fullscreen then press Home | No hidden playback or stale auto-PiP window | NOT RUN |
| Rotation/window size | Rotate and change window size during play, pause, mini, PiP | No unexpected restart, lost position or leaked session | NOT RUN |
| Audio focus/headset | Play another media app; disconnect headphones/Bluetooth | Focus is respected; noisy output pauses; pause position persists | NOT RUN |
| Revoked access | Revoke test-folder grant; open/resume video; restore grant and retry | Actionable error; saved position retained; retry opens media when accessible | NOT RUN |
| USB loss | Play from USB, disconnect, reconnect and retry | Error remains recoverable; no source corruption or indefinite hidden audio | NOT RUN |
| Rapid switching | Open A, scrub, open B, repeat and close quickly | Menus/preview/position belong to current video; old callbacks ignored | NOT RUN |
| Controls/lock | Lock, tap, unlock, Back; wait for controls to hide | Lock prevents gestures; unlock remains available; Back does not navigate beneath fullscreen | NOT RUN |
| Brightness/volume | Adjust each side; exit player; change system volume | Correct channel changes; previous window brightness restored | NOT RUN |
| Aspect/speed | Fit/crop/stretch, portrait/ultrawide video, 0.5–2x | Current video aspect used; PiP hint excludes fit letterboxing; speed stays synchronized | NOT RUN |
| Reduced motion/accessibility | Reduced motion on/off, TalkBack labels, large text | Clear controls/labels; no incorrect Play/Pause semantics or inaccessible recovery action | NOT RUN |

## Format gate — not certified by core playback

HEVC Main10, AC-3/E-AC-3/downmix, HDR10/HDR10+ and fallback, SRT and complex ASS require separate actual-device validation and any needed decoder/renderer implementation. No guarantees follow from Media3 integration. Native media-session/PiP source does not mean notification/background-audio integration is complete.

## Stop conditions

Do not approve M3 if any baseline playback, resume, data safety, lifecycle or accessibility recovery case fails. Attach the exact failure, file characteristics and reproducible steps; save a new checkpoint after fixes. No milestone advancement until user approval.
