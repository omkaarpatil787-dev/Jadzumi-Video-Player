package com.jadzumi.video

import org.junit.Assert.*
import org.junit.Test

class DialogueReplayTest {
    @Test fun replayCoversPreviousTenSecondsAndStopsAtOriginalPosition() {
        val window = requireNotNull(dialogueReplayWindow(45_000, 90_000))
        assertEquals(35_000L, window.startMs)
        assertEquals(45_000L, window.endMs)
        assertFalse(window.reachedEnd(44_999))
        assertTrue(window.reachedEnd(45_000))
        assertTrue(window.reachedEnd(45_500))
    }
    @Test fun beginningAndCorruptPositionsAreBounded() {
        assertNull(dialogueReplayWindow(0, 50_000))
        assertNull(dialogueReplayWindow(-100, 50_000))
        assertEquals(DialogueReplayWindow(0, 2500), dialogueReplayWindow(2500, 50_000))
        assertEquals(DialogueReplayWindow(40_000, 50_000), dialogueReplayWindow(100_000, 50_000))
    }
}
