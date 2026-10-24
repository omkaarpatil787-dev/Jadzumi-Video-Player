package com.jadzumi.video

import org.junit.Assert.*
import org.junit.Test

class MediaInspectionTest {
    @Test fun reportedFrameRatesKeepFractionalPrecisionWithoutDefaults() {
        assertEquals("23.976 fps", inspectionFrameRate(23.976))
        assertEquals("30 fps", inspectionFrameRate(30.0))
        listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { assertNull(inspectionFrameRate(it)) }
    }
    @Test fun colorsReflectOnlyReportedFields() {
        assertNull(inspectionColor(VideoInspection()))
        assertNull(inspectionColor(null))
        assertEquals("BT.2020 · PQ / ST 2084 · Limited range",
            inspectionColor(VideoInspection(colorStandard = 6, colorTransfer = 6, colorRange = 2)))
        assertEquals("BT.709", inspectionColor(VideoInspection(colorStandard = 1)))
        assertEquals("HLG", inspectionColor(VideoInspection(colorTransfer = 7)))
    }
    @Test fun currentFormatComplementsFileHeadersWithoutInventingBitDepth() {
        val file = VideoInspection(mime = "video/hevc", width = 1920, height = 1080, frameRate = 23.976)
        val live = VideoInspection(width = 1280, height = 720, colorStandard = 1)
        val result = requireNotNull(mergeVideoInspection(live, file))
        assertEquals(1280, result.width)
        assertEquals(23.976, result.frameRate!!, 0.0001)
        assertEquals("H.265 / HEVC", inspectionVideoCodec(result.mime))
        assertNull(result.bitDepth)
        assertEquals(file, mergeVideoInspection(null, file))
        assertNull(mergeVideoInspection(null, null))
    }
    @Test fun decoderDetailsCannotLeakAcrossDifferentFiles() {
        val decoder = DecoderInspection("c2.vendor.hevc.decoder", DecoderHardware.HARDWARE)
        assertEquals(decoder, decoderForInspectedFile("content://video/1", "content://video/1", decoder))
        assertNull(decoderForInspectedFile("content://video/2", "content://video/1", decoder))
        assertNull(decoderForInspectedFile(null, null, decoder))
        assertNull(decoderForInspectedFile("content://video/1", "content://video/1", null))
    }
    @Test fun audioChannelsAndSubtitleFormatsAreNotGuessedFromCodec() {
        assertEquals("AAC · 5.1 Surround (6 Ch) · 48000 Hz · en",
            inspectionAudio(AudioInspection("audio/mp4a-latm", 6, 48000, "en")))
        assertEquals("AAC", inspectionAudio(AudioInspection("audio/mp4a-latm", null, null)))
        assertEquals("PGS · en", inspectionSubtitle(SubtitleInspection("application/pgs", "en")))
        assertEquals("WebVTT", inspectionSubtitle(SubtitleInspection("text/vtt", "und")))
        assertEquals("application/custom", inspectionSubtitle(SubtitleInspection("application/custom")))
    }
}
