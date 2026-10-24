package com.jadzumi.video

import org.junit.Assert.*
import org.junit.Test

class SubtitleSearchTest {
    @Test fun srtHandlesBomMultilineFormattingAndCommas() {
        val cues = SubtitleSearch.parse("\uFEFF1\r\n00:00:01,250 --> 00:00:03,000\r\n<i>Hello</i> &amp; welcome\r\nback.\r\n\r\n2\r\n00:00:05,000 --> 00:00:06,000\r\nGoodbye")
        assertEquals(2, cues.size)
        assertEquals(1250L, cues[0].startMs)
        assertEquals("Hello & welcome back.", cues[0].text)
        assertEquals(listOf(cues[0]), SubtitleSearch.search(cues, " WELCOME "))
    }
    @Test fun webVttHandlesShortTimestampsIdsAndCueSettings() {
        val cues = SubtitleSearch.parse("WEBVTT\n\nNOTE not dialogue\n00:01.000 --> 00:02.000\nignore me\n\nscene-1\n01:02.500 --> 01:04.000 align:start\nThe <b>door</b> opens")
        assertEquals(listOf(SubtitleSearchCue(62500, 64000, "The door opens")), cues)
    }
    @Test fun invalidAndUnsupportedInputDoesNotCreateBogusSeekTargets() {
        assertTrue(SubtitleSearch.parse("[Events]\nDialogue: ASS is not supported").isEmpty())
        assertTrue(SubtitleSearch.parse("1\n00:99:00,000 --> 01:00:00,000\nbad timestamp").isEmpty())
        assertTrue(SubtitleSearch.parse("1\n00:00:05,000 --> 00:00:01,000\nbackwards").isEmpty())
        assertTrue(SubtitleSearch.search(emptyList(), "anything").isEmpty())
    }
    @Test fun resultsAreSortedAndBounded() {
        val cues = (200 downTo 1).joinToString("\n\n") { "$it\n00:00:01,000 --> 00:00:02,000\nHello $it" }
        val parsed = SubtitleSearch.parse(cues)
        assertEquals(100, SubtitleSearch.search(parsed, "hello").size)
        assertTrue(SubtitleSearch.search(parsed, "  ").isEmpty())
        val ordered = SubtitleSearch.parse("1\n00:00:09,000 --> 00:00:10,000\nlate\n\n2\n00:00:01,000 --> 00:00:02,000\nearly")
        assertEquals("early", ordered.first().text)
    }
}
