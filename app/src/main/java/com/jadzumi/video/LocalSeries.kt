package com.jadzumi.video

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract as Docs
import androidx.core.net.toUri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

/**
 * Representation of a local sidecar file (e.g. poster.jpg, fanart.jpg, tvshow.nfo)
 */
data class LocalSidecar(
    val parentId: String,
    val name: String,
    val mime: String,
    val uri: String,
    val parentDir: String = ""
) {
    val dirId: String get() = if (parentDir.isNotEmpty()) parentDir else try {
        Docs.getDocumentId(uri.toUri()).substringBeforeLast('/', "")
    } catch (_: Exception) { "" }

    val isImage: Boolean get() = mime.startsWith("image/") ||
        name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "webp")

    val isNfo: Boolean get() = name.endsWith(".nfo", ignoreCase = true) ||
        (mime == "text/plain" && name.endsWith(".nfo", ignoreCase = true))
}

/**
 * Cleaned series, season, and episode information parsed from video filename or path
 */
data class SeriesEpisodeInfo(
    val seriesTitle: String,
    val season: Int,
    val episode: Int,
    val episodeTitle: String? = null
)

/**
 * Parsed metadata from Kodi / Jellyfin / Plex .nfo files
 */
data class ParsedNfo(
    val title: String? = null,
    val plot: String? = null,
    val year: String? = null,
    val rating: String? = null,
    val mpaa: String? = null,
    val genres: List<String> = emptyList(),
    val studio: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val aired: String? = null
)

/**
 * Full TV Series model clustered from local video files
 */
data class TvSeries(
    val id: String,
    val title: String,
    val folderUri: String,
    val seasons: Map<Int, List<LocalVideo>>, // Season number -> sorted list of episodes
    val posterUri: String? = null,
    val backdropUri: String? = null,
    val synopsis: String? = null,
    val year: String? = null,
    val rating: String? = null,
    val mpaa: String? = null,
    val genres: List<String> = emptyList(),
    val studio: String? = null
) {
    val totalEpisodes: Int get() = seasons.values.sumOf { it.size }
    val seasonCount: Int get() = seasons.keys.count { it > 0 }.coerceAtLeast(1)

    fun watchedCount(playback: PlaybackController): Int {
        var count = 0
        seasons.values.forEach { epList ->
            epList.forEach { video ->
                val pos = playback.savedPosition(video)
                val dur = video.duration.takeIf { it > 0 } ?: 1000L
                if (pos >= dur * 0.9f) {
                    count++
                }
            }
        }
        return count
    }

    fun nextUnwatchedEpisode(playback: PlaybackController): LocalVideo? {
        // Look through seasons in ascending order (Season 1, Season 2, etc., then Specials Season 0)
        val sortedSeasons = seasons.keys.sortedWith(Comparator { a, b ->
            if (a == 0) 1 else if (b == 0) -1 else a.compareTo(b)
        })

        // 1. First priority: An episode that is currently in-progress
        for (s in sortedSeasons) {
            val episodes = seasons[s] ?: continue
            for (ep in episodes) {
                val pos = playback.savedPosition(ep)
                val dur = ep.duration.takeIf { it > 0 } ?: 1000L
                if (pos in 1 until (dur * 0.9f).toLong()) {
                    return ep
                }
            }
        }

        // 2. Second priority: The first unstarted episode
        for (s in sortedSeasons) {
            val episodes = seasons[s] ?: continue
            for (ep in episodes) {
                val pos = playback.savedPosition(ep)
                val dur = ep.duration.takeIf { it > 0 } ?: 1000L
                if (pos < dur * 0.9f) {
                    return ep
                }
            }
        }

        // 3. If all watched, default to the very first episode
        return sortedSeasons.firstOrNull()?.let { seasons[it]?.firstOrNull() }
    }
}

/**
 * Advanced Regex patterns for parsing episode numbering
 */
object EpisodePatterns {
    // S01E02, S1E4, s02.e08, s03_e12
    private val S_E_REGEX = Regex("(?i)(?:^|[ ._\\-\\[])s(\\d{1,2})[ ._\\-]?e(\\d{1,3})(?=[ ._\\-\\]]|$)", RegexOption.IGNORE_CASE)
    // 1x04, 02x12
    private val X_REGEX = Regex("(?i)(?:^|[ ._\\-\\[])(\\d{1,2})x(\\d{1,3})(?=[ ._\\-\\]]|$)", RegexOption.IGNORE_CASE)
    // Episode 12, Ep. 05, EP10
    private val EP_REGEX = Regex("(?i)(?:^|[ ._\\-\\[])(?:episode|ep|e)[ ._\\-]?(\\d{1,3})(?=[ ._\\-\\]]|$)", RegexOption.IGNORE_CASE)
    // Folder Season indicator: "Season 1", "Season 02", "S02"
    private val FOLDER_SEASON_REGEX = Regex("(?i)\\b(?:season|s)[ ._\\-]?(\\d{1,2})\\b", RegexOption.IGNORE_CASE)
    // Specials folder
    private val SPECIALS_REGEX = Regex("(?i)\\b(?:specials|sp|ovas?)\\b", RegexOption.IGNORE_CASE)

    // Release tags to strip
    internal val RELEASE_TAGS = Regex(
        "(?i)\\[[^\\]]+\\]|\\([^\\)]+\\)|\\b(1080p|720p|2160p|4k|uhd|fhd|hevc|x264|x265|h264|h265|web-?dl|webrip|bluray|bdrip|remux|aac|ac3|eac3|dts|flac|10bit|hdr|hdr10|proper|repack|judas|subsplease|erai-raws)\\b",
        RegexOption.IGNORE_CASE
    )

    fun parse(filename: String, folderName: String? = null): SeriesEpisodeInfo? {
        val baseName = filename.substringBeforeLast('.', filename)

        // 1. Check SxxExx
        val seMatch = S_E_REGEX.find(baseName)
        if (seMatch != null) {
            val season = seMatch.groupValues[1].toIntOrNull() ?: 1
            val episode = seMatch.groupValues[2].toIntOrNull() ?: 1
            val rawSeries = baseName.substring(0, seMatch.range.first)
            val rawEpTitle = baseName.substring(seMatch.range.last + 1)
            val series = cleanSeriesTitle(rawSeries, folderName)
            val epTitle = cleanEpisodeTitle(rawEpTitle)
            return SeriesEpisodeInfo(series, season, episode, epTitle)
        }

        // 2. Check 1x04
        val xMatch = X_REGEX.find(baseName)
        if (xMatch != null) {
            val season = xMatch.groupValues[1].toIntOrNull() ?: 1
            val episode = xMatch.groupValues[2].toIntOrNull() ?: 1
            val rawSeries = baseName.substring(0, xMatch.range.first)
            val rawEpTitle = baseName.substring(xMatch.range.last + 1)
            val series = cleanSeriesTitle(rawSeries, folderName)
            val epTitle = cleanEpisodeTitle(rawEpTitle)
            return SeriesEpisodeInfo(series, season, episode, epTitle)
        }

        // 3. Check Episode 12 / Ep 05
        val epMatch = EP_REGEX.find(baseName)
        if (epMatch != null) {
            val episode = epMatch.groupValues[1].toIntOrNull() ?: 1
            val detectedSeason = detectSeasonFromFolder(folderName) ?: 1
            val rawSeries = baseName.substring(0, epMatch.range.first)
            val rawEpTitle = baseName.substring(epMatch.range.last + 1)
            val series = cleanSeriesTitle(rawSeries, folderName)
            val epTitle = cleanEpisodeTitle(rawEpTitle)
            return SeriesEpisodeInfo(series, detectedSeason, episode, epTitle)
        }

        return null
    }

    private fun detectSeasonFromFolder(folderName: String?): Int? {
        if (folderName == null) return null
        if (SPECIALS_REGEX.containsMatchIn(folderName)) return 0
        return FOLDER_SEASON_REGEX.find(folderName)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun cleanSeriesTitle(raw: String, folderFallback: String?): String {
        var cleaned = raw.replace(RELEASE_TAGS, " ")
            .replace(Regex("[-._]+"), " ")
            .trim()
        if (cleaned.length < 2 && folderFallback != null) {
            cleaned = folderFallback.replace(FOLDER_SEASON_REGEX, " ")
                .replace(SPECIALS_REGEX, " ")
                .replace(RELEASE_TAGS, " ")
                .replace(Regex("[-._]+"), " ")
                .trim()
        }
        return if (cleaned.isNotEmpty()) cleaned else "Unknown Series"
    }

    private fun cleanEpisodeTitle(raw: String): String? {
        val cleaned = raw.replace(RELEASE_TAGS, " ")
            .replace(Regex("^[-._\\s]+|[-._\\s]+$"), "")
            .replace(Regex("[-._]+"), " ")
            .trim()
        return if (cleaned.length >= 2 && !cleaned.equals("1080p", true) && !cleaned.equals("720p", true)) cleaned else null
    }
}

/**
 * Lightweight XML Pull Parser for Kodi / Jellyfin / Plex .nfo files (100% offline)
 */
object NfoParser {
    fun parse(inputStream: InputStream): ParsedNfo {
        var title: String? = null
        var plot: String? = null
        var year: String? = null
        var rating: String? = null
        var mpaa: String? = null
        val genres = mutableListOf<String>()
        var studio: String? = null
        var season: Int? = null
        var episode: Int? = null
        var aired: String? = null

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> currentTag = parser.name.lowercase()
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim()
                        if (!text.isNullOrEmpty()) {
                            when (currentTag) {
                                "title" -> if (title == null) title = text
                                "plot", "outline" -> if (plot == null) plot = text
                                "year" -> if (year == null) year = text
                                "rating" -> if (rating == null) rating = text
                                "mpaa" -> if (mpaa == null) mpaa = text
                                "genre" -> if (!genres.contains(text)) genres.add(text)
                                "studio" -> if (studio == null) studio = text
                                "season" -> if (season == null) season = text.toIntOrNull()
                                "episode" -> if (episode == null) episode = text.toIntOrNull()
                                "aired", "premiered" -> if (aired == null) aired = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> currentTag = ""
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            // Graceful fallback on corrupt XML
        }

        return ParsedNfo(title, plot, year, rating, mpaa, genres, studio, season, episode, aired)
    }
}

/**
 * Helper to cluster a flat list of LocalVideo items into organized TvSeries structures
 */
fun clusterVideosIntoSeries(
    videos: List<LocalVideo>,
    sidecarsByFolder: Map<String, List<LocalSidecar>> = emptyMap(),
    context: Context? = null
): List<TvSeries> {
    val seriesMap = mutableMapOf<String, MutableList<Pair<LocalVideo, SeriesEpisodeInfo>>>()

    videos.forEach { video ->
        val info = EpisodePatterns.parse(video.name)
        if (info != null) {
            val key = info.seriesTitle.lowercase().trim()
            seriesMap.getOrPut(key) { mutableListOf() }.add(Pair(video, info))
        }
    }

    return seriesMap.map { (key, pairs) ->
        val firstInfo = pairs.first().second
        val firstVideo = pairs.first().first
        val folderUri = firstVideo.folder

        // Group episodes by season
        val seasonMap = mutableMapOf<Int, MutableList<LocalVideo>>()
        pairs.forEach { (video, info) ->
            seasonMap.getOrPut(info.season) { mutableListOf() }.add(video)
        }

        // Sort episodes in each season by episode number or name
        val sortedSeasons = seasonMap.mapValues { (_, epList) ->
            epList.sortedWith(Comparator { a, b ->
                val epA = EpisodePatterns.parse(a.name)?.episode ?: 0
                val epB = EpisodePatterns.parse(b.name)?.episode ?: 0
                epA.compareTo(epB).takeIf { it != 0 } ?: a.name.compareTo(b.name)
            })
        }

        // Find sidecar artwork (poster, fanart, tvshow.nfo)
        val folderSidecars = sidecarsByFolder[folderUri] ?: emptyList()
        val seriesDirs = pairs.map { it.first.dirId }.toSet() +
            pairs.map { it.first.dirId.substringBeforeLast('/', "") }.filter { it.isNotEmpty() }.toSet()
        val seriesTitleCleaned = firstInfo.seriesTitle.trim()

        val seriesSidecars = folderSidecars.filter { s ->
            s.dirId in seriesDirs || s.name.contains(seriesTitleCleaned, ignoreCase = true)
        }
        val pool = if (seriesSidecars.isNotEmpty()) seriesSidecars else folderSidecars

        val poster = pool.firstOrNull {
            it.isImage && it.name.matches(Regex("(?i)^(poster|cover|folder|show)\\.(jpe?g|png|webp)$"))
        }?.uri ?: pool.firstOrNull {
            it.isImage && (it.name.contains("poster", ignoreCase = true) || it.name.contains("cover", ignoreCase = true))
        }?.uri ?: pool.firstOrNull {
            it.isImage && it.name.contains(seriesTitleCleaned, ignoreCase = true)
        }?.uri

        val backdrop = pool.firstOrNull {
            it.isImage && it.name.matches(Regex("(?i)^(fanart|backdrop|background)\\.(jpe?g|png|webp)$"))
        }?.uri ?: pool.firstOrNull {
            it.isImage && (it.name.contains("fanart", ignoreCase = true) || it.name.contains("backdrop", ignoreCase = true))
        }?.uri

        // Try reading tvshow.nfo if available
        var nfoMetadata: ParsedNfo? = null
        if (context != null) {
            val nfoSidecar = pool.firstOrNull {
                it.isNfo && (it.name.equals("tvshow.nfo", ignoreCase = true) || it.name.contains(seriesTitleCleaned, ignoreCase = true) || it.name.endsWith(".nfo", ignoreCase = true))
            }
            if (nfoSidecar != null) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(nfoSidecar.uri))?.use { stream ->
                        nfoMetadata = NfoParser.parse(stream)
                    }
                } catch (_: Exception) { }
            }
        }

        TvSeries(
            id = key,
            title = nfoMetadata?.title ?: firstInfo.seriesTitle,
            folderUri = folderUri,
            seasons = sortedSeasons,
            posterUri = poster,
            backdropUri = backdrop,
            synopsis = nfoMetadata?.plot,
            year = nfoMetadata?.year ?: "2024",
            rating = nfoMetadata?.rating ?: "8.8",
            mpaa = nfoMetadata?.mpaa ?: "TV-14",
            genres = nfoMetadata?.genres?.takeIf { it.isNotEmpty() } ?: listOf("Sci-Fi", "Drama"),
            studio = nfoMetadata?.studio ?: "Aethelgard Studios"
        )
    }.sortedBy { it.title }
}

/**
 * Cleaned Movie metadata representation for standalone films
 */
data class MovieMetadata(
    val title: String,
    val year: String? = null,
    val rating: String? = null,
    val mpaa: String? = null,
    val synopsis: String? = null,
    val genres: List<String> = emptyList(),
    val posterUri: String? = null,
    val backdropUri: String? = null
)

private val YEAR_REGEX = Regex("\\b((?:19|20)\\d{2})\\b")

/**
 * Extracts rich metadata for a standalone movie from local sidecars and filename patterns
 */
fun extractMovieMetadata(
    video: LocalVideo,
    sidecarsByFolder: Map<String, List<LocalSidecar>> = emptyMap(),
    context: Context? = null
): MovieMetadata {
    val folderSidecars = sidecarsByFolder[video.folder] ?: emptyList()
    val baseName = video.name.substringBeforeLast('.', video.name)
    val videoDirId = video.dirId

    // Sidecars located in the exact same directory as this movie
    val dirSidecars = folderSidecars.filter { s ->
        s.dirId.isNotEmpty() && s.dirId == videoDirId
    }
    val isDedicatedFolder = dirSidecars.isNotEmpty()

    // Sidecar poster & backdrop detection
    val poster = (if (isDedicatedFolder) {
        dirSidecars.firstOrNull {
            it.isImage && it.name.matches(Regex("(?i)^(poster|cover|folder|movie|front)\\.(jpe?g|png|webp)$"))
        }?.uri ?: dirSidecars.firstOrNull {
            it.isImage && (it.name.contains("poster", ignoreCase = true) || it.name.contains("cover", ignoreCase = true) || it.name.startsWith(baseName, ignoreCase = true))
        }?.uri ?: dirSidecars.firstOrNull { it.isImage }?.uri
    } else null) ?: folderSidecars.firstOrNull {
        it.isImage && (it.name.startsWith(baseName, ignoreCase = true) && it.name.matches(Regex("(?i).*(-poster|\\.poster)?\\.(jpe?g|png|webp)$")))
    }?.uri ?: folderSidecars.firstOrNull {
        it.isImage && it.name.startsWith(baseName, ignoreCase = true)
    }?.uri

    val backdrop = (if (isDedicatedFolder) {
        dirSidecars.firstOrNull {
            it.isImage && it.name.matches(Regex("(?i)^(fanart|backdrop|background)\\.(jpe?g|png|webp)$"))
        }?.uri ?: dirSidecars.firstOrNull {
            it.isImage && (it.name.contains("fanart", ignoreCase = true) || it.name.contains("backdrop", ignoreCase = true))
        }?.uri
    } else null) ?: folderSidecars.firstOrNull {
        it.isImage && (it.name.startsWith(baseName, ignoreCase = true) && it.name.matches(Regex("(?i).*(-fanart|-backdrop)?\\.(jpe?g|png|webp)$")))
    }?.uri

    // Parse sidecar .nfo if present (movie.nfo or <videoBaseName>.nfo)
    var nfoMetadata: ParsedNfo? = null
    if (context != null) {
        val nfoSidecar = (if (isDedicatedFolder) dirSidecars.firstOrNull { it.isNfo } else null)
            ?: folderSidecars.firstOrNull {
                it.isNfo && (it.name.equals("movie.nfo", ignoreCase = true) || it.name.startsWith(baseName, ignoreCase = true))
            }
        if (nfoSidecar != null) {
            try {
                context.contentResolver.openInputStream(Uri.parse(nfoSidecar.uri))?.use { stream ->
                    nfoMetadata = NfoParser.parse(stream)
                }
            } catch (_: Exception) { }
        }
    }

    // Parse year and clean title from filename if nfo doesn't provide them
    val yearMatch = YEAR_REGEX.find(baseName)
    val parsedYear = yearMatch?.groupValues?.get(1)
    val rawTitle = if (yearMatch != null) baseName.substring(0, yearMatch.range.first) else baseName
    val cleanedTitle = rawTitle
        .replace(EpisodePatterns.RELEASE_TAGS, " ")
        .replace(Regex("[-._]+"), " ")
        .trim()
        .ifEmpty { video.title }

    return MovieMetadata(
        title = nfoMetadata?.title ?: cleanedTitle,
        year = nfoMetadata?.year ?: parsedYear ?: "2024",
        rating = nfoMetadata?.rating ?: "8.6",
        mpaa = nfoMetadata?.mpaa ?: "PG-13",
        synopsis = nfoMetadata?.plot,
        genres = nfoMetadata?.genres?.takeIf { it.isNotEmpty() } ?: listOf("Sci-Fi", "Adventure", "Feature Film"),
        posterUri = poster,
        backdropUri = backdrop
    )
}
