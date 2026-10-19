# M3 core implementation notes

## Playback intent

The UI now distinguishes actual isPlaying from requested playWhenReady. During loading or transient suppression, a pending play request still needs a Pause action. Idle/ended/error states show Play and prepare/retry as needed. onEvents persists position after playback-intent/discontinuity events, while the existing periodic checkpoint remains.

Reference: https://developer.android.com/reference/androidx/media3/common/Player

## Errors

The app's retry path rebuilds its local player/session while preserving the last position. A separately stopped idle player is prepared before Play. Released instances cannot update state through late listener callbacks.

Reference: https://developer.android.com/media/media3/exoplayer/listening-to-player-events

## PiP

Automatic entry stays conditioned on active fullscreen playback. Bounds are measured in window coordinates, adjusted to the visible fitted video content, and refreshed when aspect/resize state changes. PiP's supported aspect limits apply only to system parameters, not intrinsic video geometry. Native seek actions are disabled when unavailable. This needs real-device transition verification.

Reference: https://developer.android.com/develop/ui/compose/system/pip-enter

These are engineering decisions and source-level checks, not evidence of Android runtime behavior. API documentation was consulted on 2026-10-19. No new media-format claim follows from these changes.
