package com.jadzumi.video

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract as Docs
import androidx.core.net.toUri
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
    // Episode 12, Ep. 05, EP10, OVA 01, SP 02, Special 03
    private val EP_REGEX = Regex("(?i)(?:^|[ ._\\-\\[])(?:episode|ep|e|ova|sp|special)[ ._\\-]?(\\d{1,3})(?=[ ._\\-\\]]|$)", RegexOption.IGNORE_CASE)
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

        // 3. Check Episode 12 / Ep 05 / OVA 01 / Special 02
        val epMatch = EP_REGEX.find(baseName)
        if (epMatch != null) {
            val episode = epMatch.groupValues[1].toIntOrNull() ?: 1
            val detectedSeason = detectSeasonFromFolder(folderName, epMatch.value) ?: 1
            val rawSeries = baseName.substring(0, epMatch.range.first)
            val rawEpTitle = baseName.substring(epMatch.range.last + 1)
            val series = cleanSeriesTitle(rawSeries, folderName)
            val epTitle = cleanEpisodeTitle(rawEpTitle)
            return SeriesEpisodeInfo(series, detectedSeason, episode, epTitle)
        }

        return null
    }

    private fun detectSeasonFromFolder(folderName: String?, matchedToken: String? = null): Int? {
        if (matchedToken != null && Regex("(?i)\\b(?:ova|sp|special)\\b").containsMatchIn(matchedToken)) return 0
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
 * Lightweight XML Parser for Kodi / Jellyfin / Plex .nfo files (100% offline & JVM-safe)
 */
object NfoParser {
    fun parse(inputStream: InputStream): ParsedNfo {
        val text = try {
            inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            return ParsedNfo()
        }
        return parseText(text)
    }

    fun parseText(text: String): ParsedNfo {
        fun extractFirst(tag: String): String? {
            val regex = Regex("(?si)<$tag\\b[^>]*>(.*?)</$tag>")
            return regex.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
        }

        fun extractAll(tag: String): List<String> {
            val regex = Regex("(?si)<$tag\\b[^>]*>(.*?)</$tag>")
            return regex.findAll(text).mapNotNull {
                it.groupValues.getOrNull(1)?.trim()?.takeIf { s -> s.isNotEmpty() }
            }.distinct().toList()
        }

        val title = extractFirst("title")
        val plot = extractFirst("plot") ?: extractFirst("outline")
        val year = extractFirst("year")
        val rating = extractFirst("rating")
        val mpaa = extractFirst("mpaa")
        val genres = extractAll("genre")
        val studio = extractFirst("studio")
        val season = extractFirst("season")?.toIntOrNull()
        val episode = extractFirst("episode")?.toIntOrNull()
        val aired = extractFirst("aired") ?: extractFirst("premiered")

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
            synopsis = nfoMetadata?.plot ?: detectMovieSynopsis(firstInfo.seriesTitle, detectMovieGenres(firstInfo.seriesTitle, pool.firstOrNull()?.name ?: "")),
            year = nfoMetadata?.year ?: "2024",
            rating = nfoMetadata?.rating ?: "8.8",
            mpaa = nfoMetadata?.mpaa ?: "TV-14",
            genres = nfoMetadata?.genres?.takeIf { it.isNotEmpty() } ?: detectMovieGenres(firstInfo.seriesTitle, pool.firstOrNull()?.name ?: ""),
            studio = nfoMetadata?.studio ?: when {
                firstInfo.seriesTitle.containsAny("marvel", "disney", "mandalorian", "star wars") -> "Lucasfilm / Marvel Studios"
                firstInfo.seriesTitle.containsAny("hbo", "game of thrones", "dragon", "last of us", "succession") -> "HBO Originals"
                firstInfo.seriesTitle.containsAny("netflix", "stranger things", "witcher", "wednesday") -> "Netflix Originals"
                else -> "Television Production"
            }
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

private fun CharSequence.containsAny(vararg keywords: String): Boolean {
    val s = this.toString()
    for (k in keywords) {
        if (s.contains(k, ignoreCase = true)) return true
    }
    return false
}

/**
 * Intelligent genre detector that inspects movie/show title and filename keywords
 */
fun detectMovieGenres(title: String, filename: String): List<String> {
    val text = "$title $filename".lowercase()
    return when {
        text.containsAny(
            "horror", "conjuring", "insidious", "halloween", "exorcist", "dracula", "zombie",
            "evil dead", "saw", "scream", "paranormal", "quiet place", "hereditary", "midsommar",
            "annabelle", "nun", "ring", "grudge", "alien", "nightmare", "chucky", "babadook",
            "sinister", "witch", "terrifier", "nosferatu", "smile", "talk to me", "boogeyman",
            "shining", "psycho", "get out", "us", "nope", "creepy", "slasher", "haunted"
        ) -> listOf("Horror", "Mystery", "Thriller")

        text.containsAny(
            "avengers", "endgame", "infinity war", "iron man", "captain america", "thor", "spider-man",
            "spiderman", "batman", "superman", "guardians of the galaxy", "deadpool", "wolverine",
            "black panther", "justice league", "aquaman", "wonder woman", "x-men", "ant-man",
            "doctor strange", "hulk", "marvel", "dc"
        ) -> listOf("Action", "Adventure", "Sci-Fi")

        text.containsAny(
            "interstellar", "inception", "dune", "matrix", "star wars", "star trek", "blade runner",
            "avatar", "terminator", "arrival", "gravity", "martian", "tenet", "jurassic",
            "predator", "planet of the apes", "transformers", "godzilla", "kong", "ex machina"
        ) -> listOf("Sci-Fi", "Adventure", "Action")

        text.containsAny(
            "wick", "mission impossible", "fast and furious", "top gun", "gladiator", "mad max",
            "die hard", "007", "james bond", "bourne", "taken", "equalizer", "bullet train",
            "expendables", "kill bill", "heat", "sicario", "nobody"
        ) -> listOf("Action", "Thriller", "Crime")

        text.containsAny(
            "toy story", "shrek", "frozen", "moana", "finding nemo", "lion king", "spider-verse",
            "incredibles", "coco", "inside out", "despicable", "minions", "how to train your dragon",
            "kung fu panda", "ratatouille", "up", "wall-e", "monsters", "zootopia", "encanto",
            "cars", "ice age", "sing", "madagascar", "anime", "ghibli", "spirited away"
        ) -> listOf("Animation", "Family", "Adventure")

        text.containsAny(
            "hangover", "superbad", "ted", "barbie", "mean girls", "jump street", "ghostbusters",
            "step brothers", "dumb and dumber", "anchorman", "borat", "tropic thunder", "comedy"
        ) -> listOf("Comedy", "Drama")

        text.containsAny(
            "godfather", "dark knight", "pulp fiction", "fight club", "se7en", "silence of the lambs",
            "shutter island", "zodiac", "departed", "goodfellas", "joker", "knives out", "scarface",
            "casino", "irishman", "fargo", "prison", "detective"
        ) -> listOf("Crime", "Drama", "Thriller")

        text.containsAny(
            "oppenheimer", "titanic", "forrest gump", "shawshank", "schindler", "parasite",
            "whiplash", "la la land", "green mile", "social network", "1917", "saving private ryan",
            "dunkirk", "hacksaw ridge", "biography", "historical"
        ) -> listOf("Drama", "History", "Biography")

        text.containsAny(
            "lord of the rings", "hobbit", "harry potter", "fantastic beasts", "percy jackson",
            "narnia", "pirates of the caribbean", "chronicles", "witcher"
        ) -> listOf("Fantasy", "Adventure", "Action")

        else -> {
            val detected = mutableListOf<String>()
            if (text.contains("action")) detected.add("Action")
            if (text.contains("adventure")) detected.add("Adventure")
            if (text.contains("comedy")) detected.add("Comedy")
            if (text.contains("drama")) detected.add("Drama")
            if (text.contains("thriller") || text.contains("suspense")) detected.add("Thriller")
            if (text.contains("crime")) detected.add("Crime")
            if (text.contains("sci-fi") || text.contains("scifi") || text.contains("space")) detected.add("Sci-Fi")
            if (text.contains("romance") || text.contains("romantic")) detected.add("Romance")
            if (text.contains("fantasy")) detected.add("Fantasy")
            if (text.contains("family") || text.contains("kids")) detected.add("Family")
            if (text.contains("mystery")) detected.add("Mystery")
            if (detected.isEmpty()) listOf("Feature Film", "Cinema") else detected.take(3)
        }
    }
}

/**
 * Provides an authentic synopsis for cataloged films, or generates an intelligent contextual synopsis
 */
fun detectMovieSynopsis(title: String, genres: List<String>): String {
    val lower = title.lowercase()
    return when {
        lower.contains("endgame") ->
            "After the devastating events of Infinity War, the universe lies in ruins. With the help of remaining allies, the Avengers assemble once more in order to reverse Thanos' actions and restore balance to the cosmos."
        lower.contains("infinity war") ->
            "The Avengers and their allies must be willing to sacrifice all in an attempt to defeat the powerful Thanos before his blitz of devastation puts an end to the universe."
        lower.contains("interstellar") ->
            "When Earth becomes uninhabitable in the future, a farmer and ex-NASA pilot, Joseph Cooper, is tasked to pilot a spacecraft, along with a team of researchers, to find a new planet for humans."
        lower.contains("inception") ->
            "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O., but his tragic past may doom the project."
        lower.contains("dark knight") ->
            "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice."
        lower.contains("dune") ->
            "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family, striving to prevent a terrible future only he can foresee."
        lower.contains("oppenheimer") ->
            "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb during World War II, exploring the profound moral consequences of humanity's greatest weapon."
        lower.contains("conjuring") ->
            "Paranormal investigators Ed and Lorraine Warren work to help a family terrorized by a dark presence in their secluded farmhouse, confronting a malevolent entity of unprecedented terror."
        lower.contains("quiet place") ->
            "A family struggles for survival in a post-apocalyptic world inhabited by blind extraterrestrial creatures with an ultra-sensitive sense of hearing that hunt anything that makes a sound."
        lower.contains("john wick") ->
            "Legendary assassin John Wick uncovers a path to defeating the High Table, facing off against ruthless adversaries across globe-spanning battles with lethal precision."
        lower.contains("gladiator") ->
            "A former Roman General sets out to exact vengeance against the corrupt emperor who murdered his family and sent him into slavery, rising through the arena as a champion of freedom."
        lower.contains("matrix") ->
            "When a beautiful stranger leads computer hacker Neo to a forbidding underworld, he discovers the shocking truth: his life is an elaborate deception of an evil cyber-intelligence."
        lower.contains("spider-man") || lower.contains("spiderman") ->
            "Peter Parker seeks help from Stephen Strange to make people forget his identity as Spider-Man, which accidentally unleashes dangerous multiversal foes from alternate realities."
        genres.contains("Horror") ->
            "An intense, chilling cinematic experience exploring psychological tension and unsettling supernatural dread. Rendered in high-fidelity lossless video directly from your device storage."
        genres.contains("Action") ->
            "An adrenaline-fueled cinematic journey packed with breathtaking set pieces, exhilarating stunts, and relentless momentum, fully decoded with hardware-accelerated MediaCodec."
        genres.contains("Sci-Fi") ->
            "A visionary journey into the frontiers of technology, space, and human destiny, featuring stunning visual fidelity and immersive multi-channel sound."
        genres.contains("Animation") ->
            "A visually enchanting animated spectacle bursting with vibrant artistry, heartwarming charm, and unforgettable characters crafted for audiences of all ages."
        genres.contains("Crime") || genres.contains("Thriller") ->
            "A suspenseful, high-stakes psychological puzzle filled with unexpected twists, morally complex loyalties, and gripping dramatic tension."
        else ->
            "A feature film stored in your offline library. High-efficiency playback with hardware-accelerated MediaCodec decoding and studio-grade audio fidelity."
    }
}

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
        .replace(Regex("[\\[\\(]"), " ")
        .replace(Regex("[-._()]+"), " ")
        .trim()
        .ifEmpty { video.title }

    val detectedGenres = nfoMetadata?.genres?.takeIf { it.isNotEmpty() }
        ?: detectMovieGenres(cleanedTitle, video.name)

    val detectedSynopsis = nfoMetadata?.plot
        ?: detectMovieSynopsis(cleanedTitle, detectedGenres)

    return MovieMetadata(
        title = nfoMetadata?.title ?: cleanedTitle,
        year = nfoMetadata?.year ?: parsedYear ?: "2024",
        rating = nfoMetadata?.rating ?: "8.6",
        mpaa = nfoMetadata?.mpaa ?: "PG-13",
        synopsis = detectedSynopsis,
        genres = detectedGenres,
        posterUri = poster,
        backdropUri = backdrop
    )
}
