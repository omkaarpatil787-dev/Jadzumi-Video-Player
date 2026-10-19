package com.jadzumi.video

import org.junit.Assert.*
import org.junit.Test

class LocalLibraryTest {
    @Test fun recognizesSeasonEpisodePatterns() {
        assertEquals(EpisodeHint(2,4),parseEpisode("Show.S02E04.1080p.mkv"))
        assertEquals(EpisodeHint(2,4),parseEpisode("Show 2x04.mp4"))
        assertEquals(EpisodeHint(0,1),parseEpisode("Show_S00E01_special.mkv"))
    }
    @Test fun avoidsAmbiguousNumbers() {
        assertNull(parseEpisode("Movie.2024.1080p.mkv"))
        assertNull(parseEpisode("assets02e04.mkv"))
        assertNull(parseEpisode("Show S02E0400.mkv"))
        assertNull(parseEpisode("Show 1920x1080.mp4"))
    }
    @Test fun acceptsVideoMimeOrKnownExtension() {
        assertTrue(isVideoFile("holiday.MKV","application/octet-stream"))
        assertTrue(isVideoFile("no-extension","video/mp4"))
        assertFalse(isVideoFile("cover.jpg","image/jpeg"))
        assertFalse(isVideoFile("film.mp4.srt","text/plain"))
    }
}
