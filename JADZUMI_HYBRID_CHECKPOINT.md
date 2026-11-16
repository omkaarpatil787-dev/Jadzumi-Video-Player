# JADZUMI PURE (CINEMATIC HYBRID) — ROLLBACK CHECKPOINT

## 1. Verified Baseline Information
- **Original Branch**: `main`
- **Baseline Commit Hash**: `9983c3757fa0d2e5bda5926ec03ddc2c0ce645c7`
- **Checkpoint Tag**: `jadzumi-hybrid-pre-overhaul-20261116-1305`
- **Backup Archive Location**: `C:\Users\onkar\OneDrive\Desktop\Jadzumi_Baseline_Backup_20261116.zip`
- **Original Working-Tree Status**: Clean (0 uncommitted changes, 0 untracked files)
- **Original Application Version**: `2.0.6` (`versionCode = 48`, `versionName = "2.0.6"`)
- **Verification Status**:
  - Local validation: `compileDebugKotlin testDebugUnitTest` passed (0 errors)
  - Remote CI validation: GitHub Actions Workflow Run `43209034185` succeeded
  - GitHub Release: Tag `v2.0.6` published with signed production APK `Jadzumi-Player-v2.0.6.apk`
- **Checkpoint Timestamp**: `2026-11-16T13:05:00+01:00`

---

## 2. Integrity Verification
- **Emblem Asset**: `app/src/main/res/drawable/ic_jadzumi_logo.xml` and launcher drawables are intact and unmodified.
- **Signing Credentials**: Unexposed; keystore environment references preserved.
- **Functional Architecture**: Media3 / ExoPlayer 1.8.0, FFmpeg audio decoder, HDR dynamic metadata pipeline, Audio Studio, Video Enhancer, and Subtitle Studio are in verified functional state.

---

## 3. Exact Non-Destructive Restoration Procedure
In the event that a rollback is requested:
```powershell
cd "C:\Users\onkar\OneDrive\Documents\GitHub\Jadzumi-Video-Player"
git checkout main
git reset --hard jadzumi-hybrid-pre-overhaul-20261116-1305
git status
git log -n 1 --oneline
```
