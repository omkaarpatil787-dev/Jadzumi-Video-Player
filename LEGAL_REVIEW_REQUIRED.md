# ⚖️ Jadzumi Player — Matters Flagged for Qualified Legal Review

> **Document Type:** Unresolved Intellectual Property & Regulatory Disclosure  
> **Application:** Jadzumi Player (Android) · Version: `1.4.0-alpha.3` · Package: `com.jadzumi.video`
> **Purpose:** Identifies specific patent, trademark, and licensing questions that cannot be conclusively resolved by technical inspection alone and warrant qualified IP legal counsel.  
> **Date:** 2026-10-20

---

## 1. Executive Statement

In accordance with strict compliance engineering principles, technical auditors must not make unsupported legal guarantees or substitute engineering analysis for professional legal advice. 

The following items are flagged for review by qualified intellectual property legal counsel prior to high-volume commercial store distribution or international OEM pre-installation.

---

## 2. Itemized Legal Matters

### Matter 1: Secondary E-AC-3 Patent Claims Outside the United States
- **Technical Context:** Jadzumi Player supports audio playback of Enhanced AC-3 (E-AC-3 / ATSC A/52B) streams. The primary playback path utilizes the device's hardware `MediaCodec` (where the device manufacturer has already licensed the technology). A software fallback path utilizes the open-source FFmpeg decoder (`eac3dec.c`) bundled via NextLib.
- **Legal Consideration:** 
  - The original foundational US patents for core Dolby Digital (AC-3) expired in March 2017.
  - However, certain secondary extension patents associated with E-AC-3 (such as specific multi-channel substream multiplexing and spectral extension techniques) have contested or varying expiration timelines across international patent offices (e.g. European Patent Office, Germany, Japan).
- **Practical Risk Assessment:** Free, open-source media players (including VLC for Android, Kodi, Nova Video Player, and Next Player) have distributed FFmpeg-based software decoders to millions of users worldwide without active patent enforcement actions from rights holders. Nonetheless, formal legal review is recommended for targeted commercial distribution in jurisdictions with active secondary patent assertions.
- **Recommended Counsel Inquiry:** *"Does the non-commercial distribution of an open-source media player bundling FFmpeg's software E-AC-3 decoder create patent exposure under applicable patent laws in the EU and Germany?"*

---

### Matter 2: Software Patent Exclusions Across Geographic Jurisdictions
- **Technical Context:** Jadzumi Player is distributed globally via GitHub as an open-source APK.
- **Legal Consideration:** 
  - **United States:** Software patents on video compression algorithms (such as H.264 and HEVC) are recognized and enforced through patent pools (Via LA, Access Advance). However, both major pools maintain royalty-free tiers or commercial exemptions for free-to-end-user software applications where no subscription, advertising, or per-unit fee is collected.
  - **European Union:** Under Article 52(2)(c) and 52(3) of the European Patent Convention (EPC), computer programs "as such" are excluded from patentability, though computer-implemented inventions with technical contributions may be patented.
  - **Other Jurisdictions (e.g. India, Brazil):** National patent acts contain specific exclusions regarding pure software algorithms.
- **Recommended Counsel Inquiry:** *"Does free, non-monetized distribution of an open-source video player application qualify for patent pool royalty exemptions under Via LA and Access Advance guidelines across our intended distribution territories?"*

---

### Matter 3: GPL-3.0 Compatibility with Google Play Developer Terms
- **Technical Context:** Jadzumi Player integrates `io.github.anilbeesetti:nextlib-media3ext`, which is licensed under the GNU General Public License v3.0 (GPL-3.0). Consequently, Jadzumi Player is distributed under GPL-3.0.
- **Legal Consideration:**
  - GPL-3.0 requires that recipients have the right to modify and re-distribute the application and access the Corresponding Source Code without additional restrictions.
  - The Free Software Foundation (FSF) has historically raised questions regarding whether standard app store terms of service (such as Google Play's Developer Distribution Agreement or Apple App Store terms) impose "further restrictions" prohibited by GPL-3.0 Section 10.
  - Many prominent GPLv3 applications (e.g., VLC, Next Player, Termux) successfully distribute on Google Play by providing explicit public source repository links in their store listings and app interfaces.
- **Recommended Counsel Inquiry:** *"Does our proposed Google Play Store distribution model fully comply with GPL-3.0 Section 6 and Section 10 requirements?"*

---

### Matter 4: Nominative Fair Use of Format Identifiers
- **Technical Context:** Jadzumi Player's technical track inspector displays technical metadata to users, such as `"Dolby Digital (AC-3)"`, `"Dolby Digital Plus (E-AC-3)"`, and `"Dolby TrueHD"`.
- **Legal Consideration:**
  - Jadzumi Player does NOT use the Dolby double-D graphic trademark, does NOT claim certification, and does NOT imply sponsorship.
  - Under United States trademark law (*New Kids on the Block v. News America Publishing, Inc.*) and European trademark directives, nominative fair use permits using a trademark strictly necessary to identify a product or format, provided no affiliation or endorsement is implied.
  - Jadzumi Player has published an explicit trademark disclaimer in [`THIRD_PARTY_LICENSES.md`](THIRD_PARTY_LICENSES.md).
- **Recommended Counsel Inquiry:** *"Are the trademark disclaimers and nominative track labels in Jadzumi Player sufficient to prevent any trademark infringement or false endorsement claims under Lanham Act § 43(a) and EU trademark regulations?"*

---

## 3. Summary & Guidance

None of the flagged legal matters require disabling or removing any existing media codec from Jadzumi Player at this time. All existing playback capabilities remain active, stable, and preserved. These items are documented to ensure transparent, responsible governance as the project prepares for public distribution.
