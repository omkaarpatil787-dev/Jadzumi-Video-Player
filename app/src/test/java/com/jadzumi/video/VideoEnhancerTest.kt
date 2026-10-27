package com.jadzumi.video

import androidx.media3.common.util.UnstableApi
import org.junit.Assert.*
import org.junit.Test

@androidx.annotation.OptIn(UnstableApi::class)
class VideoEnhancerTest {

    @Test
    fun defaultParametersAreStrictlyNeutral() {
        val neutral = ColorStudioParams.NEUTRAL
        assertEquals(1.0f, neutral.contrast, 0.0001f)
        assertEquals(0.0f, neutral.shadowLift, 0.0001f)
        assertEquals(1.0f, neutral.saturation, 0.0001f)
        assertEquals(0.0f, neutral.vibrance, 0.0001f)
        assertFalse(neutral.amoledDeepBlack)
        assertTrue(neutral.isNeutral())
    }

    @Test
    fun profilesMapToExpectedPresets() {
        assertTrue(ColorEnhancementProfile.NATURAL.params.isNeutral())
        assertEquals(ColorEnhancementProfile.NATURAL, ColorEnhancementProfile.fromName("UNKNOWN_INVALID"))
        assertEquals(ColorEnhancementProfile.CINEMA, ColorEnhancementProfile.fromName("cinema"))
        assertEquals(ColorEnhancementProfile.VIVID, ColorEnhancementProfile.fromName("VIVID"))
        assertEquals(ColorEnhancementProfile.ANIME, ColorEnhancementProfile.fromName("anime"))
        assertEquals(ColorEnhancementProfile.CUSTOM, ColorEnhancementProfile.fromName("Custom"))

        val cinema = ColorEnhancementProfile.CINEMA.params
        assertEquals(1.06f, cinema.contrast, 0.001f)
        assertEquals(0.04f, cinema.shadowLift, 0.001f)

        val anime = ColorEnhancementProfile.ANIME.params
        assertEquals(1.08f, anime.contrast, 0.001f)
        assertEquals(1.15f, anime.saturation, 0.001f)
        assertEquals(0.18f, anime.vibrance, 0.001f)
    }

    @Test
    fun hdrStrictlyPreservesIdentityMatrixWithoutClipping() {
        // Even if aggressive adjustments are requested, when useHdr is true,
        // VideoEnhancerEffect MUST return identity to protect 10-bit color space!
        val aggressive = ColorStudioParams(
            contrast = 1.5f,
            shadowLift = 0.2f,
            saturation = 1.8f,
            vibrance = 0.25f,
            amoledDeepBlack = true
        )
        val effect = VideoEnhancerEffect(aggressive)
        val matrix = effect.getMatrix(0L, useHdr = true)

        val identity = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
        assertArrayEquals(identity, matrix, 0.0001f)
    }

    @Test
    fun neutralParamsReturnIdentityMatrix() {
        val effect = VideoEnhancerEffect(ColorStudioParams.NEUTRAL)
        val matrix = effect.getMatrix(0L, useHdr = false)
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
        assertArrayEquals(identity, matrix, 0.0001f)
    }

    @Test
    fun containerDetectionIsAccurate() {
        assertEquals("Matroska (MKV)", AudioStudioHelper.detectContainer("content://media/external/1", "Interstellar.mkv"))
        assertEquals("MPEG-4 (MP4)", AudioStudioHelper.detectContainer("content://media/external/2", "sample.mp4"))
        assertEquals("WebM", AudioStudioHelper.detectContainer("content://media/external/3", "clip.webm"))
        assertEquals("Not reported", AudioStudioHelper.detectContainer("content://media/external/4", "stream_without_ext"))
    }

    @Test
    fun channelLayoutFormattingDoesNotGuess() {
        assertEquals("Mono (1 channel)", AudioStudioHelper.formatChannelLayout(1))
        assertEquals("Stereo (2 channels)", AudioStudioHelper.formatChannelLayout(2))
        assertEquals("5.1 Surround (6 channels)", AudioStudioHelper.formatChannelLayout(6))
        assertEquals("7.1 Surround (8 channels)", AudioStudioHelper.formatChannelLayout(8))
        assertEquals("4 channels", AudioStudioHelper.formatChannelLayout(4))
        assertEquals("Not reported", AudioStudioHelper.formatChannelLayout(null))
        assertEquals("Unknown layout", AudioStudioHelper.formatChannelLayout(-1))
    }

    @Test
    fun sampleRateAndBitDepthFormattingPreservesAccuracy() {
        assertEquals("48,000 Hz", AudioStudioHelper.formatSampleRate(48000))
        assertEquals("44,100 Hz", AudioStudioHelper.formatSampleRate(44100))
        assertEquals("Not reported", AudioStudioHelper.formatSampleRate(null))
        assertEquals("Not reported", AudioStudioHelper.formatSampleRate(0))

        assertEquals("24-bit", AudioStudioHelper.formatBitDepth(24, "audio/flac"))
        assertEquals("16-bit", AudioStudioHelper.formatBitDepth(16, "audio/raw"))
        assertEquals("Not reported", AudioStudioHelper.formatBitDepth(null, "audio/mp4a-latm"))
        assertEquals("Not reported", AudioStudioHelper.formatBitDepth(null, "audio/ac3"))
    }

    @Test
    fun decoderEngineIdentificationDistinguishesHardwareAndSoftware() {
        val (nameHw, engineHw) = AudioStudioHelper.formatDecoderEngine("c2.qti.aac.decoder", DecoderHardware.HARDWARE)
        assertEquals("c2.qti.aac.decoder", nameHw)
        assertEquals("Platform MediaCodec (Hardware Accelerated)", engineHw)

        val (nameSw, engineSw) = AudioStudioHelper.formatDecoderEngine("c2.android.aac.decoder", DecoderHardware.SOFTWARE)
        assertEquals("c2.android.aac.decoder", nameSw)
        assertEquals("Platform MediaCodec (System Software)", engineSw)

        val (nameFfmpeg, engineFfmpeg) = AudioStudioHelper.formatDecoderEngine("libavcodec.so (FFmpeg eac3)", DecoderHardware.SOFTWARE)
        assertEquals("libavcodec.so (FFmpeg eac3)", nameFfmpeg)
        assertEquals("NextLib / FFmpeg (Software Engine)", engineFfmpeg)

        val (nameUnknown, engineUnknown) = AudioStudioHelper.formatDecoderEngine(null, null)
        assertEquals("Unknown decoder", nameUnknown)
        assertEquals("Not reported", engineUnknown)
    }
}
