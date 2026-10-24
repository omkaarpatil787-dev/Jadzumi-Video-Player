package com.jadzumi.video

import org.junit.Assert.*
import org.junit.Test

class MediaFactsTest {
    @Test fun unavailableRatingsStayUnavailable() {
        listOf(null, Double.NaN, Double.POSITIVE_INFINITY, -1.0, 11.0).forEach { assertNull(displayRating(it)) }
        assertEquals("8.8", displayRating(8.8))
        assertEquals("0.0", displayRating(0.0))
    }
    @Test fun codecBadgeDoesNotInventResolutionOrBitDepth() {
        assertEquals("640 × 360 · AVC", measuredVideoBadge("video/avc", 640, 360, false))
        assertEquals("HEVC", measuredVideoBadge("video/hevc", -1, -1, false))
        assertEquals("3840 × 2160 · HEVC · HDR", measuredVideoBadge("video/hevc", 3840, 2160, true))
        assertNull(measuredVideoBadge(null, 0, 0, false))
    }
    @Test fun historyRemovalCannotMatchPrivacyOrOtherSettings() {
        assertTrue(isWatchHistoryKey("a".repeat(64)))
        assertTrue(isWatchHistoryKey("f".repeat(64) + ".done"))
        listOf("online_metadata", "night_mode", "preferred_audio_lang", "a".repeat(63), "a".repeat(64)+".other").forEach {
            assertFalse(isWatchHistoryKey(it))
        }
    }
}
