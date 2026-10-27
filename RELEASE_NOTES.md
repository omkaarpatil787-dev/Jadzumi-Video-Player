# Jadzumi v1.5.0 — GPU Video Enhancer & Audio Studio 🎨🔊

A major media processing update bringing a hardware-accelerated **GPU Video Enhancer & Color Studio** and an **Expanded Audio Studio** with real-time stream inspection and channel processing modes.

### 🎨 GPU Video Enhancer & Color Studio
- **Hardware-Accelerated Color Pipeline**: Powered by Media3 native `RgbMatrix` and OpenGL ES shader effects running directly between the hardware video decoder and the display surface.
- **SurfaceView Efficiency**: Retains zero-copy `SurfaceView` output for maximum efficiency, smooth frame delivery, and low thermal footprint.
- **Isolated Overlay & Subtitle Preservation**: Color enhancement operates strictly on video pixels; SRT and ASS subtitles, HUD controls, and gestures remain 100% crisp, unaltered, and rendered on separate window layers.
- **HDR Master Passthrough Safety**: When 10-bit HDR (HDR10, HDR10+, Dolby Vision, HLG) media is active, enhancements are locked out with an informative badge to strictly preserve the native BT.2020 wide color gamut and ST 2084 transfer curve without tone clipping or artificial compression.
- **Precision Color Controls**:
  - **Contrast (0.6x to 1.6x)**: Balanced contrast pivoting around middle-gray (0.5).
  - **Shadow Lift / Gamma (-25% to +25%)**: Lifts dark scene shadows via an exponential tonal curve without washing out highlight details.
  - **Saturation (0% to 200%)**: Scales from 0% pure monochrome/grayscale to 200% rich saturation.
  - **Vibrance (-25% to +25%)**: Selectively boosts muted colors while protecting natural skin tones and already vivid colors.
  - **AMOLED Deep Black (Experimental)**: Optional near-black noise clamp with shadow-clipping disclosure.
  - **One-Tap Reset**: Instantly returns all parameters to neutral.
- **Color Profiles**:
  - **Natural (Studio Master)**: 100% untouched source stream with direct zero-copy playback bypass (`setVideoEffects(emptyList())`).
  - **Cinema**: Subtle contrast with gentle shadow clarity and natural film tones.
  - **Vivid**: Deeper dynamics with selective vibrance protection.
  - **Anime**: High vibrance and edge contrast optimized for cell shading and animation.
  - **Custom**: User-adjusted sliders with persistent settings.

### 🔊 Expanded Audio Studio: Stream Inspection & Channel Modes
- **Active Audio Stream Card**: Real-time telemetry updating on track/decoder change without polling:
  - Container detection (MKV, MP4, WebM, etc.).
  - Source codec (AAC, Dolby Digital AC-3, E-AC-3, TrueHD, DTS, Opus, FLAC).
  - Source channels and verified layout (5.1 Surround, Stereo, Mono).
  - Source sample rate (e.g. 48,000 Hz) and bit depth (only when reported).
  - Active decoder and engine (distinguishing Platform MediaCodec hardware/software from NextLib/FFmpeg software engine).
  - Player audio output (channels, sample rate, PCM encoding).
  - Active processing chips.
- **Channel Processing Modes**:
  - **Default / Automatic**: Source track pass-through and device-negotiated channel output.
  - **Stereo Downmix**: Mixes multichannel (5.1/7.1) tracks into stereo for headphones or phone speakers with balanced dialogue headroom.
  - **Dialogue Clarity Focus**: Prioritizes center speech channel in cinema mixes for crisp, audible voices.
- **Lip-Sync Adjustment (Bluetooth Delay)**:
  - Preserved −50ms / +50ms adjustment with Reset to 0ms.
  - Explanatory direction guide: *Negative (−) advances audio; Positive (+) delays audio to sync with Bluetooth latency*.
- **Night Mode (Dynamic Compression)**:
  - Preserved dynamic compression to lift quiet dialogue and suppress loud spikes.

### 📦 Installation
Download **Jadzumi-Player-v1.5.0.apk** below. Android 8.0+ / ARM64.
Same-production-key updates preserve settings and watch history.
