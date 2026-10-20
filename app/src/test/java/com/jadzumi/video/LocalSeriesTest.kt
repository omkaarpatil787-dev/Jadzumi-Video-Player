package com.jadzumi.video

import org.junit.Assert.*
import org.junit.Test

class LocalSeriesTest {

    @Test
    fun parsesStandardSeasonEpisode() {
        val info = EpisodePatterns.parse("Chrono Shift - S02E08 - Fractured Horizons.mkv")
        assertNotNull(info)
        assertEquals("Chrono Shift", info!!.seriesTitle)
        assertEquals(2, info.season)
        assertEquals(8, info.episode)
        assertEquals("Fractured Horizons", info.episodeTitle)
    }

    @Test
    fun parsesWithReleaseTagsAndXNotation() {
        val info = EpisodePatterns.parse("[Judas] Arcane - 01x04 - Happy Progress Day! [1080p][HEVC].mkv")
        assertNotNull(info)
        assertEquals("Arcane", info!!.seriesTitle)
        assertEquals(1, info.season)
        assertEquals(4, info.episode)
        assertEquals("Happy Progress Day!", info.episodeTitle)
    }

    @Test
    fun parsesEpisodeWordPatternWithFolderFallback() {
        val info = EpisodePatterns.parse("Episode 12.mkv", "Breaking Bad")
        assertNotNull(info)
        assertEquals("Breaking Bad", info!!.seriesTitle)
        assertEquals(1, info.season)
        assertEquals(12, info.episode)
    }

    @Test
    fun parsesSpecialsSeasonFolder() {
        val info = EpisodePatterns.parse("OVA 01.mkv", "Specials")
        assertNotNull(info)
        assertEquals(0, info!!.season)
        assertEquals(1, info.episode)
    }

    @Test
    fun clustersVideosIntoSeries() {
        val v1 = LocalVideo("uri1", "folder1", "Chrono Shift S01E01.mkv", "video/mp4", 1000, 100, 1400_000, 1920, 1080)
        val v2 = LocalVideo("uri2", "folder1", "Chrono Shift S01E02.mkv", "video/mp4", 1000, 100, 1400_000, 1920, 1080)
        val v3 = LocalVideo("uri3", "folder1", "Chrono Shift S02E01.mkv", "video/mp4", 1000, 100, 1400_000, 1920, 1080)

        val seriesList = clusterVideosIntoSeries(listOf(v1, v2, v3))
        assertEquals(1, seriesList.size)
        val series = seriesList.first()
        assertEquals("Chrono Shift", series.title)
        assertEquals(2, series.seasons.size)
        assertEquals(2, series.seasons[1]?.size)
        assertEquals(1, series.seasons[2]?.size)
        assertEquals(3, series.totalEpisodes)
    }

    @Test
    fun parsesNfoXmlProperly() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tvshow>
                <title>Chrono Shift: Fractured Horizons</title>
                <plot>In 2088, rogue chronologist Ren Vance navigates parallel timelines.</plot>
                <year>2024</year>
                <rating>8.8</rating>
                <mpaa>TV-14</mpaa>
                <genre>Sci-Fi</genre>
                <genre>Action</genre>
                <studio>Aethelgard Studios</studio>
            </tvshow>
        """.trimIndent()

        val parsed = NfoParser.parse(xml.byteInputStream())
        assertEquals("Chrono Shift: Fractured Horizons", parsed.title)
        assertEquals("In 2088, rogue chronologist Ren Vance navigates parallel timelines.", parsed.plot)
        assertEquals("2024", parsed.year)
        assertEquals("8.8", parsed.rating)
        assertEquals("TV-14", parsed.mpaa)
        assertEquals(listOf("Sci-Fi", "Action"), parsed.genres)
        assertEquals("Aethelgard Studios", parsed.studio)
    }
}
