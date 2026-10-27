# Jadzumi v1.4.1 — Android 16/17 Audio Fix & 200% Volume Boost 🔊

A targeted audio quality, loudness, and DSP compatibility release resolving Android 16/17 spatializer volume attenuation and adding hardware-accelerated Volume Boost.

### 🔊 Android 16 & 17 Spatializer Attenuation Fix
- Explicitly sets `SPATIALIZATION_BEHAVIOR_NEVER` on audio attributes to stop Android 16/17 from forcing automatic spatializer downmixing and -14 dB dynamic range attenuation on 5.1 / Atmos multichannel movie tracks.
- Restores full-strength volume, bass, and punch for ViPER4FX, Dolby Atmos, and system equalizers.

### 🎛️ ViPER4FX & System Equalizer Session Hook
- Broadcasts `AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION` on playback start/session change and `AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION` on release.
- Enables ViPER4FX, Wavelet, and system DSPs to properly detect Jadzumi and apply full equalizer, clarity, and master gain settings.

### ⚡ Volume Boost up to 200%
- Swipe up on the right side of the screen beyond 100% to seamlessly engage **Volume Boost (105% → 200%)** powered by Android's hardware `LoudnessEnhancer`.
- Glowing mint-green HUD capsule shows real-time boost percentage, amplifying quiet cinema dialogue without audio distortion.

### 📦 Installation
Download **Jadzumi-Player-v1.4.1.apk** below. Android 8.0+ / ARM64.
Same-production-key updates preserve settings and watch history.
