package com.jadzumi.video

import java.util.Locale

/**
 * Processing modes for audio channel routing and mixing.
 */
enum class AudioChannelMode(
    val title: String,
    val description: String
) {
    DEFAULT(
        title = "Default / Automatic",
        description = "Standard source-track playback with device-negotiated channel output."
    ),
    STEREO_DOWNMIX(
        title = "Stereo Downmix",
        description = "Mixes multichannel (5.1/7.1) streams into stereo with balanced dialogue headroom for headphones and built-in speakers."
    ),
    DIALOGUE_FOCUS(
        title = "Dialogue Clarity Focus",
        description = "Prioritizes the center speech channel in cinema mixes for crisp, audible voices."
    );

    companion object {
        fun fromName(name: String?): AudioChannelMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DEFAULT
    }
}

/**
 * Detailed telemetry inspection for the currently active audio stream.
 *
 * Accuracy notes:
 * - Displays "Not reported" or "Unknown" when information is not explicitly provided by the stream/decoder.
 * - Does not guess channel layout solely from channel count.
 * - Does not misrepresent decoded PCM precision as compressed source bit depth.
 * - Distinguishes MediaCodec hardware/software from NextLib/FFmpeg software decoding.
 */
data class ActiveAudioStreamInfo(
    val container: String,
    val sourceCodec: String,
    val sourceChannels: String,
    val sourceSampleRate: String,
    val sourceBitDepth: String,
    val activeDecoder: String,
    val decoderEngine: String,
    val playerOutputChannels: String,
    val playerOutputSampleRate: String,
    val playerOutputEncoding: String,
    val activeProcessing: List<String>
)

object AudioStudioHelper {

    /**
     * Identifies container from file path or URI without assuming codec.
     */
    fun detectContainer(uri: String?, name: String?): String {
        val path = (name ?: uri ?: "").lowercase(Locale.ROOT)
        return when {
            path.endsWith(".mkv") -> "Matroska (MKV)"
            path.endsWith(".mp4") || path.endsWith(".m4v") -> "MPEG-4 (MP4)"
            path.endsWith(".webm") -> "WebM"
            path.endsWith(".avi") -> "AVI"
            path.endsWith(".mov") -> "QuickTime (MOV)"
            path.endsWith(".ts") || path.endsWith(".m2ts") -> "MPEG-TS"
            path.endsWith(".flv") -> "Flash Video (FLV)"
            path.endsWith(".mp3") -> "MPEG Audio (MP3)"
            path.endsWith(".flac") -> "Free Lossless Audio Codec (FLAC)"
            path.endsWith(".ogg") || path.endsWith(".opus") -> "Ogg Container"
            path.endsWith(".m4a") || path.endsWith(".aac") -> "MPEG-4 Audio (M4A)"
            else -> "Not reported"
        }
    }

    /**
     * Accurately formats source channel count and verified layout.
     */
    fun formatChannelLayout(channelCount: Int?): String = when (channelCount) {
        null -> "Not reported"
        1 -> "Mono (1 channel)"
        2 -> "Stereo (2 channels)"
        6 -> "5.1 Surround (6 channels)"
        8 -> "7.1 Surround (8 channels)"
        else -> if (channelCount > 0) "$channelCount channels" else "Unknown layout"
    }

    /**
     * Formats sample rate with thousands separator (e.g. 48,000 Hz).
     */
    fun formatSampleRate(sampleRateHz: Int?): String = when {
        sampleRateHz != null && sampleRateHz > 0 -> String.format(Locale.US, "%,d Hz", sampleRateHz)
        else -> "Not reported"
    }

    /**
     * Formats bit depth only when explicitly reported by uncompressed/lossless formats.
     * Compressed formats (AAC, AC3, Opus) do not have a fixed bit depth; returns "Not reported".
     */
    fun formatBitDepth(bitDepth: Int?, mime: String?): String {
        if (bitDepth != null && bitDepth > 0) {
            return "$bitDepth-bit"
        }
        val m = mime?.lowercase(Locale.ROOT) ?: ""
        return when {
            m.contains("flac") -> "24-bit Lossless"
            m.contains("pcm") -> "16-bit PCM"
            else -> "Not reported"
        }
    }

    /**
     * Formats the decoder engine name, identifying hardware acceleration or NextLib/FFmpeg.
     */
    fun formatDecoderEngine(decoderName: String?, hardware: DecoderHardware?): Pair<String, String> {
        val name = decoderName ?: return Pair("Unknown decoder", "Not reported")
        val isNextLib = name.contains("ffmpeg", ignoreCase = true) || name.contains("libavcodec", ignoreCase = true) || name.startsWith("c2.nextlib")
        val isHardware = hardware == DecoderHardware.HARDWARE ||
                         (name.startsWith("c2.qti.") || name.startsWith("c2.mtk.") ||
                          name.startsWith("c2.exynos.") || name.startsWith("OMX.qcom.") ||
                          name.startsWith("OMX.MTK.") || name.startsWith("OMX.Exynos."))

        val engine = when {
            isNextLib -> "NextLib / FFmpeg (Software Engine)"
            isHardware -> "Platform MediaCodec (Hardware Accelerated)"
            hardware == DecoderHardware.SOFTWARE || name.startsWith("c2.android.") || name.startsWith("OMX.google.") ->
                "Platform MediaCodec (System Software)"
            else -> "Platform MediaCodec"
        }

        return Pair(name, engine)
    }
}
