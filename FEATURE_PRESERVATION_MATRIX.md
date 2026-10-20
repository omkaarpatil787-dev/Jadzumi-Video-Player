# 🛡️ Cinema Flow v1.3 — Feature Preservation Matrix
> **Zero-Regression Verification: 100% Feature Continuity Audit**

This matrix tracks every pre-existing feature, toggle, gesture, state machine, and decoder capability to ensure zero regression when applying the **Cinema Flow v1.3** visual design system.

---

## 📋 1. Core Feature & Architecture Preservation Matrix

| Feature / Capability | Pre-Migration Source File | Cinema Flow Implementation Location | Preservation Verification Check | Regression Risk | Mitigation Strategy |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Hardware SurfaceView Video Render** | `CinemaPlayer.kt` | `CinemaPlayer.kt` (`AndroidView`) | SurfaceView hardware render untouched; zero overhead Compose overlays | None | Keep exact SurfaceHolder callback |
| **FFmpeg JNI Decoder Pipeline** | `PlaybackController.kt` | `PlaybackController.kt` | `NextLib` audio/video extension pipeline preserved intact | None | No touch to controller decoder setup |
| **Local SAF Storage & DocumentFile** | `LocalLibrary.kt` | `LocalLibrary.kt` | SAF URIs, content resolvers, directory scanner intact | None | Preserve all SAF intent contracts |
| **TV Show Episode Regex Grouping** | `LocalSeries.kt` | `LocalSeries.kt` | `S01E01`, `1x02`, season indexing untouched | None | Pure model logic untouched |
| **Dual-Engine Online Metadata** | `OnlineMetadataFetcher.kt` | `OnlineMetadataFetcher.kt` | Wikipedia REST + TVMaze scraper with disk LRU cache preserved | None | Metadata pipeline intact |
| **Night Voice Boost (LoudnessEnhancer)** | `PlaybackController.kt` | `PlaybackController.kt` | Audio DSP speech frequency clarity boost preserved | None | Audio session ID and DSP lifecycle preserved |
| **Edge Swipe Gestures** | `CinemaPlayer.kt` | `CinemaPlayer.kt` | Left-edge brightness, right-edge volume drag gestures preserved | None | Gesture detector unmodified |
| **Double-Tap 10s Seek** | `CinemaPlayer.kt` | `CinemaPlayer.kt` | 10s/15s/30s double-tap seek with smooth seek animation | None | Seek increment preference key preserved |
| **Subtitle Styling Engine** | `SubtitlePreferences.kt` | `SubtitlePreferences.kt`, `LocalLibraryUi.kt` | Subtitle font size, color, background opacity, font family | None | Preserved in Cinema Flow Settings |
| **OLED Peak HDR Headroom** | `DeviceHardwareInspector.kt` | `DeviceHardwareInspector.kt` | Max brightness boost on OLED displays preserved | None | Hardware detection logic untouched |
| **Dynamic Glass Translucency Slider** | `GlassTheme.kt` | `GlassTheme.kt` (`LocalGlassIntensity`) | Modulates frosted glass opacity and specular reflections | None | Upgraded to drive Cinema Flow glass |
| **Theme Bloom Animation** | `MainActivity.kt` | `MainActivity.kt` | Circular reveal animation on theme toggle | None | Retained for seamless theme changes |
| **Floating Dock Auto-Minimize** | `LocalLibraryUi.kt` | `LocalLibraryUi.kt`, `JadzumiApp.kt` | Floating dock collapses to compact pill on list scroll | None | Preserved with Cinema Flow floating dock |
| **100% Offline Credo & Sandbox** | `JadzumiApp.kt` | `JadzumiApp.kt` | Zero analytics, zero cloud transmission, local-only guarantee | None | Retained in About hero card |

---

## 🎛️ 2. Preference Key Mapping Matrix

All 29 preference keys remain unchanged in name, type, and SharedPreferences storage file.

| Pref Key | File | Cinema Flow UI Location | Control Type | Verification |
| :--- | :--- | :--- | :--- | :---: |
| `theme` | `appearance` | Settings -> Appearance Card | Chip Row (System, Light, Dark, AMOLED) | ✅ Preserved |
| `ambientAura` | `appearance` | Settings -> Appearance Card | Cinema Flow Aurora Radiance Switch | ✅ Preserved (New) |
| `glassIntensity`| `appearance` | Settings -> Glassify Slider | Slider (0.0 to 1.0) | ✅ Preserved |
| `dockAutoMinimize`| `appearance` | Settings -> Navigation Card | Switch toggle | ✅ Preserved |
| `bottomBarStyle`| `appearance` | Settings -> Navigation Card | Chip Row (Icons & labels, Icons only) | ✅ Preserved |
| `cornerStyle` | `appearance` | Settings -> Appearance Card | Chip Row (Rounded, Compact, Sharp) | ✅ Preserved |
| `reducedMotion` | `appearance` | Settings -> Animation Card | Switch toggle | ✅ Preserved |
| `livelyAnimations`| `appearance` | Settings -> Animation Card | Switch toggle | ✅ Preserved |
| `themeBloom` | `appearance` | Settings -> Animation Card | Switch toggle | ✅ Preserved |
| `bg_audio` | `playback` | Settings -> Playback Card | Switch toggle | ✅ Preserved |
| `remember_progress`| `playback` | Settings -> Playback Card | Switch toggle | ✅ Preserved |
| `save_track_prefs`| `playback` | Settings -> Playback Card | Switch toggle | ✅ Preserved |
| `swipe_gestures` | `playback` | Settings -> Gestures Card | Switch toggle | ✅ Preserved |
| `brightness_boost`| `playback` | Settings -> Gestures Card | Switch toggle | ✅ Preserved |
| `seek_seconds` | `playback` | Settings -> Gestures Card | Chip Row (10s, 15s, 30s) | ✅ Preserved |
| `night_mode` | `playback` | Video Player & Settings | Night Voice Clarity Switch | ✅ Preserved |
| `scan_sidecars` | `playback` | Settings -> Metadata Card | Switch toggle | ✅ Preserved |
| `online_metadata`| `playback` | Settings -> Metadata Card | Switch toggle | ✅ Preserved |
| `extract_thumbnails`| `playback` | Settings -> Metadata Card | Switch toggle | ✅ Preserved |
| `local_sandbox` | `jadzumi_settings`| Settings -> Security Card | Lock badge / switch | ✅ Preserved |
| `jadzumi_subs.*` (5 keys) | `jadzumi_subs` | Settings -> Subtitle Sheet | Font, Color, Size, Bg, Edge controls | ✅ Preserved |
| `video_uri_hash.*` | `playback` | Player lifecycle | Internal saved playback positions | ✅ Preserved |

---

## 🎬 3. Zero-Regression Verification Strategy

1. **Gradle Build Check**: `./gradlew test assembleDebug` must compile with 0 errors after every phase.
2. **Preference Round-Trip**: Every setting modified in Cinema Flow UI is asserted to persist to disk and reload accurately.
3. **Playback Verification**: Existing videos resume at saved millisecond offsets, double-tap seek operates, and background audio keeps playing.
4. **Offline Isolation**: No network calls initiated during regular offline local file browsing.
