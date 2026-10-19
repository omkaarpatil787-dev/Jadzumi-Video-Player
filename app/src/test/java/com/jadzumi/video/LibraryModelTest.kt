package com.jadzumi.video
import org.junit.Assert.*
import org.junit.Test
class LibraryModelTest {
    @Test fun progressUsesOneConsistentDuration() {
        val movie = previewVideos.first()
        assertEquals("01:25:06", timecode(movie.remainingSeconds))
        assertEquals(0.448477f, movie.progress, 0.00001f)
    }
    @Test fun corruptPositionsStayBounded() {
        val v = PreviewVideo("x", "x", "Movies", 10, 25, "")
        assertEquals(1f, v.progress)
        assertEquals(0L, v.remainingSeconds)
        assertEquals(0f, v.copy(durationSeconds=0).progress)
        assertEquals(0f, v.copy(positionSeconds=-4).progress)
        assertEquals(10L, v.copy(positionSeconds=-4).remainingSeconds)
    }
    @Test fun searchAndCategoryWorkTogether() {
        assertEquals(listOf("show"), filterVideos(previewVideos,"  CHRONO ","TV & Anime").map { it.id })
        assertTrue(filterVideos(previewVideos,"Chrono","Movies").isEmpty())
        assertEquals(3,filterVideos(previewVideos,"","All videos").size)
    }
    @Test fun durationFormattingHandlesLongAndNegativeValues() {
        assertEquals("00:00:00", timecode(-1))
        assertEquals("27:00:01", timecode(97201))
    }
}
