# Jadzumi v1.4.2 — Speaker Routing Fix & Audio Output Selector 🔊

A critical audio pipeline and hardware routing update resolving an Android 16/17 and ViPER4FX audio bug where sound output misrouted into the phone call earpiece instead of the loud stereo speakers.

### 📢 Explicit Loudspeaker Hardware Routing
- Directly binds ExoPlayer's output HAL via `setPreferredAudioDevice(TYPE_BUILTIN_SPEAKER)` when listening on internal phone audio, completely preventing Android 17 / ViPER4FX / VoIP policies from misrouting media playback into the tiny telephone call earpiece.
- Automatically handles seamless handoff to Bluetooth / USB / Wired headphones when connected via dynamic `AudioDeviceCallback`.

### 🔄 Audio Communication & Telecom State Reset
- Ensures `AudioManager.mode` resets cleanly to `MODE_NORMAL` on playback start, resume, and track change, eliminating stuck communication states left behind by voice assistants, VoIP calls, or audio mod drivers.
- Automatically clears communication device locks via `clearCommunicationDevice()` on Android 12+ (API 31+).
- Explicitly locks `volumeControlStream = AudioManager.STREAM_MUSIC` in `MainActivity`.

### 🎛️ Audio Content Type Alignment (`CONTENT_TYPE_MUSIC`)
- Updates ExoPlayer `AudioAttributes` to `AUDIO_CONTENT_TYPE_MUSIC` (matching the system DSP session broadcast), preventing carrier/OEM movie voice routing rules from diverting media audio to the call speaker.

### 🔀 New "Audio Output Route" Menu
- Added an **Audio Output Route** selector in the player's Audio Settings sheet:
  - **Auto (Headphones / Loudspeaker)**: Default intelligent routing with hardware loudspeaker protection.
  - **Phone Loudspeaker (Force Stereo)**: Forcefully locks output strictly to the loud main stereo speakers.
  - **Phone Earpiece (Private Audio)**: In-call receiver mode for discrete, private listening without headphones.

### 📦 Installation
Download **Jadzumi-Player-v1.4.2.apk** below. Android 8.0+ / ARM64.
Same-production-key updates preserve settings and watch history.
