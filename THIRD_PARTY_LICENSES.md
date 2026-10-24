# 📜 Jadzumi Player — Third-Party Software Licenses & Notices

> **Application:** Jadzumi Player (Android) · Package: `com.jadzumi.video` · Version: `1.3.2`
> **Distribution:** Free & Open Source · **Primary Project License:** GNU General Public License v3.0 (GPL-3.0)  
> **Updated:** 2026-10-20

---

## 🌟 Overview & Attribution

Jadzumi Player relies on world-class open-source software libraries to provide high-fidelity video decoding, modern Material 3 user interfaces, and audio DSP processing. We extend our sincere gratitude to the developers, maintainers, and open-source communities behind these projects.

In compliance with open-source license agreements (including **GNU GPL v3.0**, **Apache 2.0**, **BSD 2-Clause**, and **BSD 3-Clause**), this document records component notices and verified license evidence. It is not legal clearance or a claim that Corresponding Source obligations have been fully satisfied. Unresolved items are listed below.

---

## 📦 Third-Party Component Registry

| Component / Library | Version | Copyright Holder(s) | Primary License | Repository / Origin |
| :--- | :---: | :--- | :---: | :--- |
| **NextLib Media3 Extension (`nextlib-media3ext`)** | `1.11.1-0.16.0` | Copyright © 2023–2026 Anil Kumar Beesetti | **GPL-3.0** | [github.com/anilbeesetti/nextlib](https://github.com/anilbeesetti/nextlib) |
| **The FFmpeg Project (`libavcodec`, `libavutil`, `libswresample`, `libswscale`)** | `9.0.1` (bundled in NextLib) | Copyright © 2000–2026 The FFmpeg Developers | **LGPL-3.0-or-later** | [ffmpeg.org](https://ffmpeg.org) |
| **Mbed TLS** | `3.6.7` (upstream build; AAR retention unverified) | Mbed TLS contributors | **Apache-2.0 OR GPL-2.0-or-later** | [Mbed-TLS/mbedtls](https://github.com/Mbed-TLS/mbedtls/tree/mbedtls-3.6.7) |
| **dav1d AV1 Decoder** | `1.5.4` (embedded in `libavcodec`) | Copyright © 2018–2026 VideoLAN, Two Orioles, LLC | **BSD 2-Clause** | [code.videolan.org/videolan/dav1d](https://code.videolan.org/videolan/dav1d) |
| **AndroidX Media3 (ExoPlayer, UI, Session)** | `1.11.1` | Copyright © 2022–2026 The Android Open Source Project | **Apache 2.0** | [github.com/androidx/media](https://github.com/androidx/media) |
| **Android Jetpack Compose UI Suite** | BOM `2025.04.01` | Copyright © 2019–2026 The Android Open Source Project | **Apache 2.0** | [android.googlesource.com](https://android.googlesource.com) |
| **AndroidX Activity & Lifecycle Compose** | `1.10.1` | Copyright © 2018–2026 The Android Open Source Project | **Apache 2.0** | [android.googlesource.com](https://android.googlesource.com) |
| **AndroidX Graphics Path** | `1.0.1` (transitive) | Copyright © 2023–2026 The Android Open Source Project | **Apache 2.0** | [android.googlesource.com](https://android.googlesource.com) |
| **Google Guava / ListenableFuture** | `1.0` (transitive) | Copyright © 2014–2026 Google LLC | **Apache 2.0** | [github.com/google/guava](https://github.com/google/guava) |
| **Kotlin Standard Library & Coroutines** | `2.2.10` (stdlib; coroutines version resolved separately) | Copyright © 2010–2026 JetBrains s.r.o. | **Apache 2.0** | [github.com/JetBrains/kotlin](https://github.com/JetBrains/kotlin) |
| **JUnit Testing Framework** | `4.13.2` (test scope) | Copyright © 2002–2026 JUnit Contributors | **EPL 1.0** | [junit.org](https://junit.org) |

---

## ⚖️ Trademark Notices & Fair-Use Disclaimers

1. **Dolby Laboratories Disclaimers:**  
   *Dolby, Dolby Digital, Dolby Digital Plus, Dolby TrueHD, Dolby Atmos, and the double-D symbol are registered trademarks of Dolby Laboratories Licensing Corporation.*  
   *Jadzumi Player is an independent, free, open-source personal media player application. Jadzumi Player is **not** certified, approved, sponsored, or endorsed by Dolby Laboratories. All references to AC-3, E-AC-3, and TrueHD in this software are strictly descriptive for format identification purposes and rely upon open-source software decoder implementations.*

2. **DTS / Xperi Disclaimers:**  
   *DTS, DTS-HD, and the DTS symbol are registered trademarks of DTS, Inc. / Xperi. Jadzumi Player is an independent open-source player not affiliated with or endorsed by DTS, Inc.*

3. **Android & Google Disclaimers:**  
   *Android, Google Play, ExoPlayer, and the Android logo are trademarks of Google LLC.*

---

## 📄 License Texts

### 1. GNU General Public License, Version 3 (GPL-3.0)
*Applies to Jadzumi Player binary distribution, `io.github.anilbeesetti:nextlib-media3ext`, and the application linking to NextLib. The FFmpeg AAR binaries themselves identify as LGPL version 3 or later.*

The full, unmodified GPL-3.0 text is in [LICENSE](LICENSE), also bundled in the APK
and available through About → Licenses. The direct GNU download was blocked by the
cloud proxy (HTTP 403); the verbatim license was obtained from the GNU GCC mirror's
[COPYING3](https://github.com/gcc-mirror/gcc/blob/master/COPYING3).

#### Source Code Availability Statement (GPL-3.0 Section 6)
Application source and build scripts are publicly available at the confirmed public repository below. Release maintainers must also supply matching native dependency sources, patches, build scripts, and any other required Corresponding Source; completeness has not been certified:
**[https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player.git](https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player.git)**

---

### 2. Apache License, Version 2.0
*Applies to AndroidX Media3, Jetpack Compose, Kotlin Standard Library, AndroidX Lifecycle, and Guava.*

Full text: [Apache-2.0](licenses/Apache-2.0.txt), bundled in About → Licenses.

---

### 3. The 2-Clause BSD License
*Applies to `dav1d` AV1 video decoder (statically linked into `libavcodec.so`).*

Verbatim upstream notice: [dav1d 1.5.4 COPYING](licenses/dav1d-1.5.4-COPYING.txt), bundled in About → Licenses.


### 4. Mbed TLS 3.6.7

Upstream offers **Apache-2.0 OR GPL-2.0-or-later**. The complete, unmodified dual
license text from the `mbedtls-3.6.7` tag is retained in
[mbedTLS-3.6.7-LICENSE.txt](licenses/mbedTLS-3.6.7-LICENSE.txt) and bundled in About → Licenses.

### 5. FFmpeg 9.0.1

The ARM64 AAR binaries identify as **LGPL version 3 or later**, with `--enable-version3`
and no `--enable-gpl` in their embedded configuration. Keep the full
[LGPL-3.0 text](licenses/FFmpeg-LGPL-3.0.txt) together with [GPL-3.0](LICENSE).
NextLib's own POM/license remains GPL-3.0; the app is released under GPL-3.0.

## Native dependency verification (1.3.2)

Inspected the [published POM](https://repo.maven.apache.org/maven2/io/github/anilbeesetti/nextlib-media3ext/1.11.1-0.16.0/nextlib-media3ext-1.11.1-0.16.0.pom),
the Maven Central AAR, and upstream tag
[`v1.11.1-0.16.0`](https://github.com/anilbeesetti/nextlib/tree/v1.11.1-0.16.0)
(commit `2dc77cd2d9352bc308aa26782f9c77a320df510b`), including README and `ffmpeg/setup.sh`.

- AAR SHA-256: `998b4ff249fd3be3dfe2bf0757d47cb7cd903c4bf02a702efc4789574c679839`;
  matches the artifact resolved into the Gradle dependency cache.
- AAR contains `libavcodec.so`, `libavutil.so`, `libswresample.so`, `libswscale.so`,
  and `libmedia3ext.so` for ARM64, ARMv7, x86 and x86_64. This app selects ARM64 only.
- ARM64 binary strings confirm FFmpeg **9.0.1**, dav1d **1.5.4**, and
  `--enable-libdav1d --enable-mbedtls --enable-version3`. dav1d is statically linked.
- The tagged README/build script pins **mbedTLS 3.6.7** and builds it statically.
  However, the media3ext AAR does not ship `libavformat.so`, and ARM64 shipped
  binaries expose no mbedTLS version/symbol evidence. The build configuration
  establishes mbedTLS usage upstream, but actual retained TLS code/version in this
  AAR could not be independently established. Its license is included conservatively.
- No separate libvpx or libass library is listed in the AAR. VP8/VP9 use FFmpeg's
  built-in decoders per upstream README. A full source-level audit of every static
  object and vendored internal component has **not** been performed.

**TODO before release:** retain exact dependency sources/build scripts and verify
all native redistribution notices and Corresponding Source. Patent/trademark items
in [LEGAL_REVIEW_REQUIRED.md](LEGAL_REVIEW_REQUIRED.md) remain open.

## Bundled artwork provenance — TODO / UNKNOWN LICENSE

| File | What can be established | Required follow-up |
| --- | --- | --- |
| `app/src/main/res/drawable-nodpi/hero.jpg` | Present in initial project upload; no attribution or EXIF provenance | **TODO:** identify creator, original URL, license, and redistribution permission |
| `app/src/main/res/drawable-nodpi/cosmos.jpg` | Present in initial project upload; no attribution or EXIF provenance | **TODO:** identify creator, original URL, license, and redistribution permission |
| `app/src/main/res/drawable-nodpi/anime.jpg` | Present in initial project upload; no attribution or EXIF provenance | **TODO:** identify creator, original URL, license, and redistribution permission |

Files are retained unchanged. Their presence does not establish permission to distribute
them or that they are covered by the application's GPL license.
