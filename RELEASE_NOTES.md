# Jadzumi v1.5.0 — Video Equalizer (Hardware Shaders) & 5.1 Surround Downmix Normalizer 🎨🔊

A major visual and acoustic upgrade bringing GPU-accelerated Video Equalizer controls (Contrast, Shadow Lift, Saturation, AMOLED Pitch-Black) and an automatic 5.1/7.1 Surround Downmix Normalizer.

### 🎨 GPU Hardware Video Equalizer & Color Grading
- **Real-Time RenderThread Shaders**: Powered by hardware `ColorMatrix` & `RenderEffect` running directly on the GPU at up to 120Hz with zero video playback stutter or decoding stalls. Works on both SDR and HDR content.
- **Contrast Control (0.6x to 1.6x)**: Dial in cinematic depth and pop for flat or washed-out video encodings.
- **Shadow Lift / Gamma Boost (-25% to +25%)**: Elevates crushed shadow details in notoriously dark movie scenes (*The Batman*, nighttime sequences, horror films) without blowing out bright highlights.
- **Saturation / Vibrance (0% to 200%)**: Makes anime, animation, and cinema colors rich, punchy, and vivid; or slide down to 0% for pure Black & White film noir.
- **AMOLED Pitch-Black Filter**: Clamps near-black pixel noise directly into pure pitch black (`#000000`), turning off OLED display pixels to eliminate grayish artifacting and save battery.
- **1-Tap Reset**: Quick button to restore neutral calibration instantly.
- **Subtitle Preservation**: Color filters apply exclusively to the video surface; subtitle rendering and HUD overlays stay crisp, pure white, and pristine.

### 🔊 Automatic 5.1 & 7.1 Surround Downmix Normalizer (+7.5dB Dialogue Lift)
- **The Problem**: Standard multi-channel downmixing (5.1/7.1 to stereo) applies an automatic $-8\text{ dB}$ to $-12\text{ dB}$ attenuation penalty to prevent digital clipping, making Center-channel dialogue sound quiet, muffled, and pushed back compared to 2.0 stereo tracks.
- **The Fix**: Automatically senses multi-channel tracks ($> 2$ channels) and applies an acoustic $+7.5\text{ dB}$ clean downmix compensation curve via the hardware `LoudnessEnhancer` DSP.
- 5.1 cinema mixes now sound **just as loud, full, and punchy as 2.0 stereo tracks** at the exact same volume position.
- Real-time **ACTIVE +7.5dB** badge in Audio Settings shows when downmix compensation is actively engaged.

### 📦 Installation
Download **Jadzumi-Player-v1.5.0.apk** below. Android 8.0+ / ARM64.
Same-production-key updates preserve settings and watch history.
