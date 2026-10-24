package com.jadzumi.video

import org.junit.Assert.*
import org.junit.Test

class LocalSeriesTest {

    @Test
    fun missingMetadataIsNotInventedFromTheFilename() {
        val movie = LocalVideo("uri_movie", "folder", "UnknownFilm.mkv", "video/mp4", 1000, 0, 1000, 0, 0)
        val meta = extractMovieMetadata(movie)
        assertNull(meta.rating)
        assertNull(meta.year)
        assertNull(meta.mpaa)
        assertNull(meta.synopsis)
        assertTrue(meta.genres.isEmpty())
        val episode = LocalVideo("uri_episode", "folder", "UnknownShow S01E01.mkv", "video/mp4", 1000, 0, 1000, 0, 0)
        val series = clusterVideosIntoSeries(listOf(episode)).single()
        assertNull(series.rating)
        assertNull(series.year)
        assertNull(series.mpaa)
        assertNull(series.studio)
        assertNull(series.synopsis)
    }

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

    @Test
    fun extractsMovieMetadataFromFilename() {
        val movie = LocalVideo("uri_mov", "folder1", "Interstellar.2014.1080p.BluRay.x264.mkv", "video/x-matroska", 5_000_000_000, 100, 7200_000, 1920, 1080)
        assertNull("Movie must not have episode info", movie.episode)

        val meta = extractMovieMetadata(movie)
        assertEquals("Interstellar", meta.title)
        assertEquals("2014", meta.year)
    }

    @Test
    fun isolatesTvEpisodesFromMovies() {
        val ep = LocalVideo("uri_ep", "folder1", "Chrono.Shift.S01E03.1080p.mkv", "video/mp4", 1000, 100, 1400_000, 1920, 1080)
        val mov = LocalVideo("uri_mov", "folder1", "Inception.2010.1080p.mkv", "video/mp4", 1000, 100, 1400_000, 1920, 1080)

        assertNotNull(ep.episode)
        assertNull(mov.episode)

        val all = listOf(ep, mov)
        val moviesOnly = all.filter { it.episode == null }
        val tvOnly = all.filter { it.episode != null }

        assertEquals(listOf(mov), moviesOnly)
        assertEquals(listOf(ep), tvOnly)
    }

    @Test
    fun matchesMoviePosterFromDedicatedSubfolder() {
        val movie = LocalVideo("uri_mov", "folder1", "Avatar (2009).mkv", "video/mp4", 1000, 100, 1000, 1920, 1080, parentDir = "dir_avatar")
        val sidecars = listOf(
            LocalSidecar("folder1", "poster.jpg", "image/jpeg", "uri_poster_avatar", parentDir = "dir_avatar"),
            LocalSidecar("folder1", "poster.jpg", "image/jpeg", "uri_poster_other", parentDir = "dir_other")
        )
        val meta = extractMovieMetadata(movie, mapOf("folder1" to sidecars))
        assertEquals("uri_poster_avatar", meta.posterUri)
    }

    @Test
    fun matchesTvShowPosterFromShowFolder() {
        val ep1 = LocalVideo("uri_ep1", "folder1", "Arcane S01E01.mkv", "video/mp4", 1000, 100, 1000, 1920, 1080, parentDir = "dir_arcane/season1")
        val sidecars = listOf(
            LocalSidecar("folder1", "poster.jpg", "image/jpeg", "uri_arcane_poster", parentDir = "dir_arcane"),
            LocalSidecar("folder1", "poster.jpg", "image/jpeg", "uri_other_poster", parentDir = "dir_other")
        )
        val seriesList = clusterVideosIntoSeries(listOf(ep1), mapOf("folder1" to sidecars))
        assertEquals(1, seriesList.size)
        assertEquals("uri_arcane_poster", seriesList.first().posterUri)
    }
}
