# Cinema Portal — local test preview

User requested a native adaptation of the attached HTML, a testing APK, preservation of
all settings/features and the floating dock, and initially NO PUSH without further permission. The user subsequently authorized
a push to a test branch only: test/cinema-portal-ui. Main remains unchanged and no
release is published. A separate approval is required before merging into main.

## Baseline and feature preservation

The initial local checkout was v1.4.0. Before handoff, remote main was inspected and the
preview was brought forward to v1.6.0 commit 5718fbc using a fast-forward, then the local
UI changes reapplied. The earlier draft is backed up in the local Git stash and in an
external workspace patch. No user commits/history were overwritten.

All v1.6.0 features remain, including GPU Color Studio, expanded Audio Studio, universal
grid/list views, file blacklist controls, playback HUD clock/battery/remaining-time settings,
codecs, subtitles, gestures, bookmarks, sleep, episode continuity, update checks and backups.
The first preview preserved the floating dock byte-for-byte; the follow-up below changes its selection animation as requested.
The persisted preference-key set is compared before/after; no new preference is introduced
and no old preference is removed by the visual work.

## Native adaptation

- Bounded rounded-aperture opening onto the actual composed destination, not simulated
  library loading. Existing motion controls and disabled system animations are honored;
  Skip/Back can dismiss the opening. Original logo geometry is retained.
- First-video-frame portal mask observes Media3's actual first-frame callback and current
  player identity. It draws over the surface without clipping/recreating the hardware
  video surface, reconfiguring codecs or introducing a video-processing preset.
- Home hero uses real local artwork, stronger directional scrims, readable two-line titles,
  responsive action wrapping and actual saved-position progress. No fictional films,
  hardcoded HDR/audio claims or sample progress values are imported from the HTML.
- The hero's misleading bookmark-lookalike (which only played the file) is replaced by
  the actual Details action. Real player scene bookmarks remain intact.
- Poster cards, movie/TV header surfaces, library panels and existing settings cards get
  a consistent restrained finish. Material corner presets remain effective. Dock labels, minimization and customization remain; the follow-up adds a shared sliding selection capsule.
- Player options morph between menu/close, have a clear close action, slide from the bottom
  on portrait phones, and retain every existing option. Long labels can wrap and the main
  menu height is bounded. Existing play/pause geometry is retained with reused draw buffers.

The HTML stays only in docs/design. No WebView, reference JavaScript, new permission,
new persistent effect toggle, fake file property or background animation loop is added.
The native route transitions are retained rather than imitating the HTML's cloned-poster
DOM animation. No cross-device frame-rate improvement is claimed.

## Build/install

```bash
./gradlew test assembleDebug lintDebug -PportalPreview=true
```

The property applies only to the debug build. Normal release ID/version/signing remain
unchanged. Preview ID: com.jadzumi.video.portalpreview. Launcher label: Jadzumi Preview.
It is development-signed and installs alongside the production app. Existing production
settings/history remain in the production sandbox; choose media folders and preferred
appearance again in Preview. No automatic import or reset of production data occurs.
Production update APKs cannot replace this differently named test package; normal package/
certificate update verification is retained. Do not uninstall production just to test it.

## Device checklist — NOT RUN

- Phone and tablet, portrait/landscape, default and large fonts; verify hero actions,
  poster/list layouts, drawer height and all actual content remain readable.
- Original floating dock: icons/labels, minimize, glass intensity and corner/theme controls.
- Light/Dark/AMOLED/System, Reduced Motion, lively animations off and system animations off;
  opening/replay/Skip/Back should not block readiness or produce fake progress.
- Local 24/30/60fps, SDR/HDR and HEVC files: first-frame reveal, seeking, control lock,
  subtitles, hide/show controls, orientation, PiP, errors and background audio.
- Verify all Color Studio/Audio Studio settings, grid/list modes, blacklist and playback
  HUD settings survive changes and remain functional. No default is forcibly changed.
- Check real details, resume, watched state, episode continuation, sleep, bookmarks,
  subtitle search, external subtitles, and backup/update UI.

[Unverified] Physical-device rendering, HDR composition, frame pacing and native playback
regressions need the user's device testing. Unit/build/lint evidence is saved separately;
it does not certify the visual result or every vendor decoder.

## Test-branch CI

The test/cinema-portal-ui workflow builds/tests/lints with -PportalPreview=true, uploads
only the isolated development-signed Preview APK, and skips release-key decoding and
production-signed APK assembly. GitHub Release publication remains guarded by main.
The application source tested locally is unchanged; the branch-specific workflow adds
no app setting or permission. Device checks remain NOT RUN.

## Preview 2 — requested polish (2026-10-29)

Follow-up to a0227a5. The user has authorized committing and pushing these fixes to
`test/cinema-portal-ui` only. Main remains untouched; no stable release is authorized.

- Opening now follows the reference's 650 ms brand entrance, 220 ms hold and 720 ms
  aperture, then exits once the actual destination is ready. Changing readiness no longer
  restarts the entrance. Skip, Back, reduced motion and system-disabled animation remain.
- Dock retains its layout, destinations, labels and minimization. One spring-driven capsule
  stretches/slides between tabs; icons ease with it. No dock ripple or touch-compression
  overlay. Selection semantics remain available to accessibility services.
- Shared gentle/expressive spring and fade specifications coordinate navigation, dock,
  hero mode changes, player menus, play/pause, countdowns and gesture feedback.
- Movie and TV poster grids (including search) now emit individual lazy rows. Previously
  all rows were composed inside one list item. Cancelled thumbnail jobs check cancellation
  after acquiring a permit, before starting native extraction.
- Scroll edges use cached theme scrims rather than a viewport-sized offscreen DstIn
  texture. Removed the second custom viewport overscroll transform; Foundation scrolling
  owns edge handling. Dragging cancels card press feedback and does not move its light
  origin on every pointer event.
- The clock, battery and remaining-time switches now also control a compact information
  pill while transport controls are hidden or locked. No HUD is displayed in PiP/entry.
  Unknown duration does not produce fictional remaining time.
- Removed player-root scaling/fading around the hardware surface; only the bounded
  first-frame overlay is animated. PiP no longer resets orientation, shows system bars or
  restores brightness at every mode change. Source bounds are retained during PiP;
  Android 15+ transition callback hides overlays before entry. Older Android versions
  retain manual entry/mode callbacks and seamless-resize support where available.
- Removed the explicit Dark / Night Slate choice. System default, Light and AMOLED
  choices remain; System default continues following the existing system palette. An old
  explicit Dark preference is shown as AMOLED; no other preference is reset.
- Glass intensity, theme blending, reduced motion, lively animations, spring style, touch
  lighting and opening replay are grouped under one Motion & effects card. No new setting,
  dependency, network access or codec change.

Validation is recorded in docs/verification/cinema-portal-preview/polish-result.json.
Device frame pacing, visual quality, HDR/subtitle rendering and repeated PiP entry/exit
remain [Unverified] until tested on hardware; compilation and unit tests cannot establish
that the app is lag-free.

### Device checks for this preview

1. Cold launch and replay: observe full brand entrance/hold/aperture; try Skip/Back and
   Reduced Motion, Lively off and Android animator scale off.
2. Browse/search a large movie/TV library in list and grid, scroll quickly, change tabs
   repeatedly, minimize/expand the dock, try both dock styles and larger text.
3. Play a local video, wait for controls to fade, verify each HUD switch individually,
   then all off. Check lock controls, pause, seek, rotate and unknown-duration files.
4. Enter PiP by Home and player button; return several times in portrait/landscape.
   Check playback position, orientation, subtitles, brightness/HDR and absence of HUD
   inside PiP. Compare against stable using the same file and device settings.
5. Inspect Motion & effects and the three appearance choices. Check Color/Audio Studios,
   folders, blacklist, backups and existing settings remain available.
