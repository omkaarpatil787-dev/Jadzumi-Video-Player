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
}
