package com.jadzumi.video

import org.junit.Assert.*
import org.junit.Test

class LibraryToolsModelTest {
    @Test fun onlyContiguousSameSeriesEpisodesAutoContinue() {
        assertNull(episodeContinuationWarning("Arcane S01E01.mkv", "Arcane S01E02.mkv"))
        assertNotNull(episodeContinuationWarning("Arcane S01E01.mkv", "Arcane S01E03.mkv"))
        assertNotNull(episodeContinuationWarning("Arcane S01E09.mkv", "Arcane S02E01.mkv"))
        assertNotNull(episodeContinuationWarning("Arcane S01E01.mkv", "Other S01E02.mkv"))
        assertNotNull(episodeContinuationWarning("unknown.mkv", "unknown2.mkv"))
    }
    @Test fun languageTagsNeverUseTranslatedDisplayNames() {
        assertEquals("en-US", preferredLanguageTag("en-US"))
        assertEquals("eng", preferredLanguageTag("eng"))
        assertNull(preferredLanguageTag("English"))
        assertNull(preferredLanguageTag("none"))
        assertNull(preferredLanguageTag(null))
    }
    @Test fun sleepUsesMonotonicBoundedDeadlinesAndRestorableFade() {
        assertEquals(1_801_000L, sleepDeadline(1000, 30))
        assertEquals(Long.MAX_VALUE, sleepDeadline(Long.MAX_VALUE - 10, 1))
        assertEquals(1f, sleepFadeFactor(100_000), 0f)
        assertEquals(0.5f, sleepFadeFactor(5000), 0f)
        assertEquals(0f, sleepFadeFactor(-1), 0f)
        assertEquals(0, episodesAfterEnd(SleepPlan(SleepMode.END_CURRENT)))
        assertEquals(1, episodesAfterEnd(SleepPlan(SleepMode.EPISODES, episodes = 2)))
    }
    @Test fun sceneNotesRoundTripAndKeepTimestampOrdering() {
        val entries = listOf(SceneBookmark("b", 12000, "Later", "line 1\n日本語"), SceneBookmark("a", 0, "Opening"))
        val decoded = SceneBookmarkCodec.decode(SceneBookmarkCodec.encode(entries))
        assertEquals(entries.sortedBy { it.positionMs }, decoded)
        assertTrue(SceneBookmarkCodec.notesExport("Movie", decoded).contains("00:00:12"))
    }
    @Test fun invalidSceneRecordsAreRejected() {
        try { SceneBookmarkCodec.decode("not a bookmark file"); fail() } catch (_: IllegalArgumentException) {}
        try { SceneBookmarkCodec.encode(listOf(SceneBookmark("x", -1, "Bad"))); fail() } catch (_: IllegalArgumentException) {}
        try { SceneBookmarkCodec.encode(List(201) { SceneBookmark("$it", 0, "Scene") }); fail() } catch (_: IllegalArgumentException) {}
    }
}
