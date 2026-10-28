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
The floating dock implementation is compared byte-for-byte with that v1.6.0 baseline.
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
  a consistent restrained finish. Material corner presets remain effective. Dock styling,
  labels, minimization and customization logic are untouched.
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
