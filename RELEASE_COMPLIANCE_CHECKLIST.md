# ✅ Jadzumi Player — Pre-Release Compliance & Safety Checklist

> **Application:** Jadzumi Player (Android) · Version: `1.2.0` · Package: `com.jadzumi.video`  
> **Target Release Environment:** GitHub Releases / F-Droid / Direct Free APK Distribution  
> **Audit Status:** Completed Pre-Release Verification · **Date:** 2026-10-20

---

## 1. Executive Compliance Dashboard

| Pillar | Status | Summary & Verification |
| :--- | :---: | :--- |
| **1. Binary Security & Signing** | ⚠️ **ACTION REQUIRED** | Release build is non-debuggable with R8 shrinking active, but currently signs with `development.keystore`. Dedicated release keystore must be configured before official public store release. |
| **2. Open-Source GPLv3 Compliance** | ✅ **PASSED** | Full corresponding source code is maintained on public GitHub repository. GPLv3 notice and license text included in project. |
| **3. Third-Party Notices & Attribution** | ✅ **PASSED** | Comprehensive registry and attribution documented in `THIRD_PARTY_LICENSES.md` covering NextLib, FFmpeg, dav1d, Media3, Compose, and Kotlin. |
| **4. Trademark & Certification Boundaries** | ✅ **PASSED** | Zero claims of "Dolby Certified" or "DTS Certified". Explicit trademark disclaimers added in `THIRD_PARTY_LICENSES.md`. |
| **5. Privacy & Permission Scopes** | ✅ **PASSED** | 100% offline-first playback. Network permission restricted strictly to opt-in Wikipedia/TVMaze poster scraping with local caching. Zero analytics or ads. |
| **6. Codec & Playback Integrity** | ✅ **PASSED** | 100% of approved media capabilities preserved (H.264, HEVC Main10, AC-3, E-AC-3, TrueHD, DTS, AV1, VP9, Subtitles, Downmixing). Zero codec removal. |

---

## 2. Detailed Verification Checklist

### Pillar 1: Binary Security & Signing Credentials
- [x] **Non-Debuggable Release Build:** In [`app/build.gradle.kts`](file:///C:/Users/onkar/OneDrive/Documents/GitHub/Jadzumi-Video-Player/app/build.gradle.kts#L43-L48), `isDebuggable` is `false` for `release` builds. `isMinifyEnabled = true` and `isShrinkResources = true` are active.
- [x] **No Hardcoded Secrets or Passwords in Source Code:** Verified zero API keys, tokens, or personal credentials committed in Kotlin source files.
- [ ] **Dedicated Production Release Keystore (Action Required):**
  - **Current State:** `signingConfigs.release` references `signing/development.keystore` with default development credentials (`"android"` / `"androiddebugkey"`).
  - **Required Action:** For official public distribution (e.g. F-Droid, Google Play, or GitHub signed releases), generate a dedicated 4096-bit RSA or EC production release keystore, store it securely outside of version control (or via encrypted CI/CD secrets), and inject credentials via environment variables (`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_PASSWORD`).

### Pillar 2: Open-Source GPL-3.0 License Compliance
- [x] **Source Code Availability (GPL-3.0 § 6):** Complete corresponding source code is publicly hosted and version-controlled at `https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player.git`.
- [x] **GPL-3.0 License Text Included:** Complete license text included in [`THIRD_PARTY_LICENSES.md`](file:///C:/Users/onkar/OneDrive/Documents/GitHub/Jadzumi-Video-Player/THIRD_PARTY_LICENSES.md).
- [x] **No Closed-Source Binary Blobs:** Verified that all bundled C/C++ libraries (`libavcodec.so`, `libavutil.so`, `libswresample.so`, `libswscale.so`, `libmedia3ext.so`) originate from open-source repositories with publicly inspectable build scripts.

### Pillar 3: Third-Party Notices & Attribution
- [x] **NextLib & Anil Kumar Beesetti Attribution:** Explicitly recognized for `nextlib-media3ext` in `THIRD_PARTY_LICENSES.md` and `CODEC_LICENSE_AUDIT.md`.
- [x] **The FFmpeg Developers Attribution:** Complete copyright notice for FFmpeg 6.x/7.x included.
- [x] **VideoLAN & dav1d Attribution:** BSD 2-Clause copyright notice for `dav1d` retained.
- [x] **Google & AndroidX Attribution:** Apache 2.0 notices for Media3 and Jetpack Compose included.

### Pillar 4: Trademark & Certification Protection
- [x] **No Unauthorized Dolby Branding:** Verified that Jadzumi Player does NOT use the Dolby Double-D symbol or claim official certification. Format descriptions are purely technical format identifiers.
- [x] **Trademark Disclaimer Included:** Explicit disclaimer published:
  > *"Dolby, Dolby Digital, Dolby Digital Plus, Dolby TrueHD, and Dolby Atmos are registered trademarks of Dolby Laboratories Licensing Corporation. Jadzumi Player is an independent open-source application not affiliated with or certified by Dolby Laboratories."*
- [x] **DTS Disclaimer Included:** Explicit disclaimer published regarding DTS, Inc. trademarks.

### Pillar 5: Privacy, Permissions & Network Transparency
- [x] **Strictly Local Media Storage:** Playback history, resume positions, subtitle delay settings, and bookmarks remain 100% on-device in private app storage.
- [x] **Permission Justification:**
  - `android.permission.POST_NOTIFICATIONS`: Required for Android 13+ background media playback controls.
  - `android.permission.INTERNET` & `ACCESS_NETWORK_STATE`: Required strictly for fetching public movie posters and synopses from Wikipedia REST and TVMaze.
- [x] **Offline Resilience:** If Wi-Fi/Internet is disconnected or unavailable, the application functions 100% offline using local sidecars (`poster.jpg`, `.nfo`) and sampled video frames.

### Pillar 6: Playback Engine & Codec Integrity
- [x] **HEVC Main10 & 10-bit HDR:** Preserved with automatic OLED peak luminance boost.
- [x] **H.264 / AVC:** Preserved with hardware acceleration primary.
- [x] **AC-3 & E-AC-3:** Preserved with hardware acceleration primary and FFmpeg software fallback.
- [x] **TrueHD & DTS:** Preserved via FFmpeg software decoders.
- [x] **Surround-to-Stereo Downmix:** Preserved via `libswresample.so` for headphones and mobile speakers.
- [x] **Subtitle Engine:** Preserved with pure Media3 Java/Kotlin parsers (SRT, VTT, SSA/ASS).

---

## 3. Recommended Release Distribution Strategy

1. **GitHub Releases:**
   - Attach the compiled APK (`app-release.apk`) alongside the source code zip.
   - Include direct links to `CODEC_LICENSE_AUDIT.md` and `THIRD_PARTY_LICENSES.md` in the release notes.
2. **F-Droid Inclusion:**
   - Because Jadzumi Player is 100% open-source under GPLv3 with zero proprietary tracking SDKs, it is an ideal candidate for inclusion in the official F-Droid repository.
3. **Google Play Store (Optional):**
   - Ensure the Play Store listing references the open-source license and avoids trademarked certification terms in screenshots or promotional copy.
