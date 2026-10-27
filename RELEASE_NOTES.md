# Jadzumi v1.5.1 — Auto Adaptive Color Engine & Display Profiles 🌟🎨

An intelligent display optimization release introducing the Auto Adaptive Video Engine and One-Tap Display Profiles, eliminating manual slider tweaking while preserving the rock-solid audio foundation.

### 🌟 Auto Adaptive Color Engine (Default)
- **Zero-Effort Automatic Optimization**: Analyzes incoming video stream metadata (HDR vs SDR, color gamut, tags, subtitle format) and display hardware (OLED vs LCD) to compute the mathematically optimal color grading curve on the fly.
- **HDR Tonemap Recovery**: Automatically engages when playing 10-bit HDR or Dolby Vision video on SDR displays, eliminating the washed-out "gray fog" look by dynamically restoring cinema contrast (+14%) and natural saturation (+18%).
- **Anime & Animation Mode**: Automatically detects anime and animated films (via ASS subtitles or file naming), boosting vibrance (+22%) and edge contrast for punchy, vibrant cell shading.
- **Dark Film & Night Owl Lift**: Automatically lifts crushed dark shadows in dark cinematography (*The Batman*, horror movies, night sequences) by +8% so hidden details become clearly visible.
- **Display-Aware AMOLED Mode**: On OLED/AMOLED displays, automatically clamps near-black pixel noise into pure pitch-black (`#000000`) for infinite contrast and zero battery waste.

### 🎭 1-Tap Display Profiles & Fine-Tuning
- **Auto (Smart Adaptive)**: Automatically applies the optimal calibration.
- **Cinema (Neutral)**: 100% untouched studio reference master for purists.
- **Anime & Animation (Vivid)**: High vibrance and deep contrast.
- **Shadow Clarity (Night Owl)**: Lifts dark shadows without washing out highlights.
- **AMOLED Pitch-Black**: Extreme contrast with true-black OLED pixel shutoff.
- **Custom**: Opens fine-tuning sliders if you ever want to dial in exact manual values.

### 🔊 Audio & Hardware Foundation Preserved
- 100% maintains the earpiece-prevention hardware loudspeaker lock (`setPreferredAudioDevice(TYPE_BUILTIN_SPEAKER)`), `MODE_NORMAL` telecom resets, and ViPER4FX session hooks.
- Maintains the automatic 5.1/7.1 Surround Downmix Normalizer (+7.5dB dialogue lift).

### 📦 Installation
Download **Jadzumi-Player-v1.5.1.apk** below. Android 8.0+ / ARM64.
Same-production-key updates preserve settings and watch history.
