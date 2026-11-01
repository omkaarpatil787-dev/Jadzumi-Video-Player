package com.jadzumi.video

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class CandidateMatch(
    val id: String,
    val title: String,
    val year: String? = null,
    val rating: String? = null,
    val genres: List<String> = emptyList(),
    val synopsis: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val provider: String = "Cinemeta / IMDb"
)

data class OnlineMediaMetadata(
    val title: String,
    val year: String? = null,
    val rating: String? = null,
    val genres: List<String> = emptyList(),
    val synopsis: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val localPosterPath: String? = null
)

object OnlineMetadataFetcher {
    val USER_AGENT = "JadzumiPlayer/" + BuildConfig.VERSION_NAME + " (Android; Media3)"

    fun isOnlineMetadataEnabled(context: Context): Boolean =
        context.getSharedPreferences("playback", Context.MODE_PRIVATE)
            .getBoolean("online_metadata", false)

    // Check before EVERY request (including fallbacks and images). Opt-out never opens
    // a connection; existing memory/disk metadata and posters remain readable offline.
    fun openOnlineConnection(context: Context, url: String): HttpURLConnection? {
        if (!isOnlineMetadataEnabled(context)) return null
        return (URL(url).openConnection() as HttpURLConnection).apply {
            setRequestProperty("User-Agent", USER_AGENT)
        }
    }

    private const val TAG = "OnlineMetadata"
    private val memoryCache = ConcurrentHashMap<String, OnlineMediaMetadata>()

    private fun isNetworkConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    private fun getMetadataCacheDir(context: Context): File {
        val dir = File(context.cacheDir, "metadata_cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getPosterCacheDir(context: Context): File {
        val dir = File(context.cacheDir, "poster_cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private val YEAR_EXTRACTION_REGEX = Regex("\\b((?:19[2-9]\\d|20[0-3]\\d))\\b")

    private val RELEASE_AND_AUDIO_TAGS = Regex(
        "(?i)\\b(" +
        // Indian / Asian / Foreign languages & dubs
        "hindi|tamil|telugu|malayalam|kannada|bengali|marathi|punjabi|gujarati|urdu|" +
        "english|eng|japanese|jap|korean|kor|chinese|french|german|spanish|italian|" +
        "dual[ ._-]?audio|multi[ ._-]?audio|clean[ ._-]?audio|org[ ._-]?audio|original[ ._-]?audio|" +
        "dubbed|dub|subbed|subs?|esubs?|msubs?|softsubs?|hardsubs?|" +
        // Source rips, qualities, cams
        "predvd|dvdscr|dvdrip|cam|camrip|hdcam|hdtc|tc|ts|telesync|hdts|" +
        "bluray|bdrip|brrip|remux|web-?dl|webrip|web|hdtv|pdtv|dsr|tvrip|" +
        "2160p|1080p|720p|480p|360p|4k|uhd|fhd|hd|sd|" +
        // Video codecs & color standards
        "hevc|x264|x265|h\\.?264|h\\.?265|avc|av1|xvid|divx|" +
        "hdr10\\+?|hdr|dovi|dv|dolby[ ._-]?vision|sdr|10bit|8bit|" +
        // Audio codecs
        "aac|ac3|eac3|ddp?5\\.1|ddp?|dd5\\.1|dts-?hd|dts|truehd|atmos|flac|mp3|opus|" +
        // Editions & packaging
        "extended|director'?s[ ._-]?cut|directors[ ._-]?cut|unrated|imax|" +
        "remastered|restored|criterion|edition|proper|repack|complete|" +
        "amzn|nf|dsnp|hmax|atvp|zee5|hotstar|sonyliv|" +
        // Piracy release domains / tags
        "4khdhub|vegamovies|bolly4u|yify|yts|rarbg|psa|pahe|galaxytv|cinevood|filmyzilla|mkvcinemas|katmoviehd|luxmovies|hdhub4u|extramovies|worldfree4u|ssrmovies" +
        ")\\b",
        RegexOption.IGNORE_CASE
    )

    fun extractCleanTitleAndYear(raw: String): Pair<String, String?> {
        val base = raw.substringBeforeLast('.')
        val yearMatches = YEAR_EXTRACTION_REGEX.findAll(base).toList()
        var extractedYear: String? = null
        var titlePart = base

        if (yearMatches.isNotEmpty()) {
            val bestYearMatch = yearMatches.lastOrNull { it.range.first > 0 } ?: yearMatches.last()
            extractedYear = bestYearMatch.value
            if (bestYearMatch.range.first > 0) {
                titlePart = base.substring(0, bestYearMatch.range.first)
            }
        }

        val cleaned = titlePart
            .replace(RELEASE_AND_AUDIO_TAGS, " ")
            .replace(EpisodePatterns.RELEASE_TAGS, " ")
            .replace(Regex("[\\[\\](){}]"), " ")
            .replace(Regex("[-._+]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return Pair(cleaned.ifEmpty { base.trim() }, extractedYear)
    }

    fun cleanMediaTitle(raw: String): String {
        return extractCleanTitleAndYear(raw).first
    }

    /**
     * Checks whether a Wikipedia page summary corresponds to a real film or television work.
     * Prevents encyclopedia concepts (e.g. 'Sexual identity' or the colour 'Blue') from
     * being mistakenly matched to local movie files.
     */
    fun isFilmOrTvWork(title: String, desc: String?, extract: String?): Boolean {
        val tLower = title.lowercase(Locale.US)
        if (tLower.endsWith("(film)") || tLower.contains("(film series)") ||
            tLower.endsWith("(miniseries)") || tLower.endsWith("(tv series)") ||
            (tLower.contains("(19") && tLower.contains("film)")) ||
            (tLower.contains("(20") && tLower.contains("film)"))) {
            return true
        }
        val combined = ((desc ?: "") + " " + (extract ?: "")).lowercase(Locale.US)
        val hasFilmIndicator = combined.containsAny(
            " film", "film ", "film,", "film.", "films", "film directed",
            "movie", "motion picture", "directed by",
            "television series", "tv series", "miniseries",
            "web series", "anime series", "animated series"
        )
        val isNonFilmTopic = combined.containsAny(
            "primary colour", "colour between", "color between", "visible spectrum",
            "sexual orientation", "sexual identity", "sexuality", "attraction",
            "soundtrack", "album by", "song by", "ep by", "single by",
            "village in", "city in", "district in", "commune in", "town in",
            "genus of", "species of", "plant", "asteroid",
            "philosophy", "psychological", "sociological", "medical condition"
        )
        return hasFilmIndicator && !isNonFilmTopic
    }

    fun purgeBogusCache(context: Context) {
        try {
            val dir = getMetadataCacheDir(context)
            if (dir.exists()) {
                dir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".json")) {
                        try {
                            val json = JSONObject(file.readText())
                            val title = json.optString("title")
                            val synopsis = json.optString("synopsis")
                            val desc = json.optString("description")
                            val keyRaw = file.nameWithoutExtension.removePrefix("mov_").removePrefix("tv_").replace("_", " ")

                            val ratingSource = json.optString("ratingSource")
                            val isVerifiedSource = ratingSource == "imdb" || ratingSource == "tvmaze" || ratingSource == "manual"
                            if (isVerifiedSource) continue

                            val isBogus = !isFilmOrTvWork(title, desc, synopsis) ||
                                title.equals("Sexual identity", ignoreCase = true) ||
                                (title.equals("Blue", ignoreCase = true) && synopsis.contains("colour", ignoreCase = true)) ||
                                title.contains("List of ", ignoreCase = true) ||
                                title.contains("Rajesh Kava", ignoreCase = true) ||
                                title.contains("Zombie Massacre", ignoreCase = true) ||
                                (keyRaw.length > 3 && scoreCandidate(keyRaw, title, synopsis, null) < 0.20f)

                            if (isBogus) {
                                file.delete()
                                val base = file.nameWithoutExtension
                                File(getPosterCacheDir(context), "$base.jpg").takeIf { it.exists() }?.delete()
                            }
                        } catch (_: Exception) { }
                    }
                }
            }
        } catch (_: Exception) { }
    }

    /**
     * Scores a candidate page title against the search query to strictly prevent mismatches
     * (e.g. matching 'World War Z' for 'Apocalypse Z', or 'List of American films' for 'Jawan').
     * Returns a score from 0.0 to 1.5, or negative value if candidate must be discarded.
     */
    fun scoreCandidate(
        query: String,
        candTitle: String,
        candDesc: String? = null,
        hintYear: String? = null
    ): Float {
        if (candTitle.contains(Regex("(?i)^(List of|Category:|Index of|Timeline of|Draft:)|(\\(soundtrack\\)|\\(album\\)|\\(disambiguation\\)|\\(character\\))"))) {
            return -1f
        }

        val stopWords = setOf("the", "a", "an", "of", "and", "in", "on", "for", "film", "movie", "to", "at")
        val normQuery = query.replace(Regex("(?i)\\b(film|movie)\\b"), "")
            .replace(Regex("[^a-zA-Z0-9 ]"), " ")
            .lowercase(Locale.US)
            .trim()
        val normCand = candTitle.replace(Regex("(?i)\\b(film|movie|\\d{4} film|\\d{4} indian film)\\b"), "")
            .replace(Regex("[^a-zA-Z0-9 ]"), " ")
            .lowercase(Locale.US)
            .trim()

        val qTokensList = normQuery.split(Regex("\\s+")).filter { it.isNotEmpty() && it !in stopWords }
        val cTokensList = normCand.split(Regex("\\s+")).filter { it.isNotEmpty() && it !in stopWords }

        if (qTokensList.isEmpty() || cTokensList.isEmpty()) return 0f

        // Anchor word check: The first significant word of the query (e.g. "Apocalypse", "Stree", "Jawan")
        // MUST have a matching root/stem in candidate title, UNLESS at least 2 significant words (length >= 4)
        // match across both titles (which supports foreign translation prefixes like "Kitab Sijjin" -> "The Book of Sijjin").
        val anchor = qTokensList[0]
        val prefixLen = minOf(4, anchor.length)
        val anchorPrefix = anchor.take(prefixLen)
        val hasAnchor = cTokensList.any { it == anchor || it.startsWith(anchorPrefix) || anchor.startsWith(it.take(prefixLen)) }

        val qLongTokens = qTokensList.filter { it.length >= 4 }
        val cLongTokens = cTokensList.filter { it.length >= 4 }
        val multiWordOverlap = qLongTokens.count { qt -> cLongTokens.any { ct -> qt == ct || ct.startsWith(qt) || qt.startsWith(ct) } }

        if (!hasAnchor && multiWordOverlap < 2) {
            return 0f
        }

        val qTokens = qTokensList.toSet()
        val cTokens = cTokensList.toSet()

        var intersect = 0
        for (qt in qTokens) {
            for (ct in cTokens) {
                if (qt == ct || ct.startsWith(qt) || qt.startsWith(ct)) {
                    intersect++
                    break
                }
            }
        }

        val dice = (2.0f * intersect) / (qTokens.size + cTokens.size)
        var score = dice

        if (candTitle.contains(Regex("(?i)\\((?:\\d{4} )?(?:indian )?film\\)"))) {
            score += 0.20f
        }
        if (candDesc != null && candDesc.contains(Regex("(?i)\\b(film|movie|directed by|starring|cinema)\\b"))) {
            score += 0.15f
        }
        if (!hintYear.isNullOrBlank()) {
            if (candTitle.contains(hintYear) || (candDesc != null && candDesc.contains(hintYear))) {
                score += 0.20f
            }
        }

        return score
    }

    fun sanitizeKey(key: String): String {
        return key.lowercase(Locale.US).replace(Regex("[^a-z0-9_]"), "_").take(64)
    }

    fun getManualOverride(context: Context, videoIdentity: String): OnlineMediaMetadata? {
        return try {
            val prefs = context.getSharedPreferences("metadata_manual_overrides", Context.MODE_PRIVATE)
            val raw = prefs.getString(videoIdentity, null) ?: return null
            val json = JSONObject(raw)
            val genresList = mutableListOf<String>()
            json.optJSONArray("genres")?.let { arr ->
                for (i in 0 until arr.length()) genresList.add(arr.getString(i))
            }
            OnlineMediaMetadata(
                title = json.getString("title"),
                year = json.optString("year").takeIf { it.isNotBlank() },
                rating = json.optString("rating").takeIf { it.isNotBlank() },
                genres = genresList,
                synopsis = json.optString("synopsis").takeIf { it.isNotBlank() },
                posterUrl = json.optString("posterUrl").takeIf { it.isNotBlank() },
                backdropUrl = json.optString("backdropUrl").takeIf { it.isNotBlank() },
                localPosterPath = json.optString("localPosterPath").takeIf { it.isNotBlank() }
            )
        } catch (_: Exception) { null }
    }

    fun setManualOverride(context: Context, videoIdentity: String, meta: OnlineMediaMetadata) {
        try {
            val prefs = context.getSharedPreferences("metadata_manual_overrides", Context.MODE_PRIVATE)
            val json = JSONObject().apply {
                put("title", meta.title)
                put("year", meta.year ?: "")
                put("rating", meta.rating ?: "")
                put("genres", JSONArray(meta.genres))
                put("synopsis", meta.synopsis ?: "")
                put("posterUrl", meta.posterUrl ?: "")
                put("backdropUrl", meta.backdropUrl ?: "")
                put("localPosterPath", meta.localPosterPath ?: "")
            }
            prefs.edit().putString(videoIdentity, json.toString()).apply()
            memoryCache[videoIdentity] = meta
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save manual override: ${e.message}")
        }
    }

    fun copyCustomPosterToCache(context: Context, imageUri: android.net.Uri, key: String): String? {
        return try {
            val destFile = File(getPosterCacheDir(context), "custom_${sanitizeKey(key)}.jpg")
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy custom poster: ${e.message}")
            null
        }
    }

    suspend fun searchCandidates(
        context: Context,
        query: String,
        isSeries: Boolean = false
    ): List<CandidateMatch> = withContext(Dispatchers.IO) {
        val results = mutableListOf<CandidateMatch>()
        val cleanQ = query.trim()
        if (cleanQ.isBlank()) return@withContext results
        if (!isNetworkConnected(context) || !isOnlineMetadataEnabled(context)) return@withContext results

        try {
            val type = if (isSeries) "series" else "movie"
            val searchUrl = "https://v3-cinemeta.strem.io/catalog/$type/top/search=" +
                    URLEncoder.encode(cleanQ, "UTF-8") + ".json"
            val conn = openOnlineConnection(context, searchUrl)
            if (conn != null) {
                conn.connectTimeout = 6000
                conn.readTimeout = 8000
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(body)
                    val metas = root.optJSONArray("metas")
                    if (metas != null) {
                        for (i in 0 until metas.length().coerceAtMost(10)) {
                            val item = metas.getJSONObject(i)
                            val id = item.optString("id")
                            val name = item.optString("name")
                            val yearStr = item.optString("year").takeIf { it.isNotBlank() }
                            val poster = item.optString("poster").takeIf { it.isNotBlank() }
                            val syn = item.optString("description").takeIf { it.isNotBlank() }
                            val rStr = item.optString("imdbRating").takeIf { it.isNotBlank() }
                            val rVal = rStr?.toDoubleOrNull()?.let { displayRating(it) }
                            val genres = mutableListOf<String>()
                            item.optJSONArray("genres")?.let { gArr ->
                                for (g in 0 until gArr.length().coerceAtMost(3)) {
                                    genres.add(gArr.getString(g))
                                }
                            }
                            if (name.isNotBlank()) {
                                results.add(
                                    CandidateMatch(
                                        id = id,
                                        title = name,
                                        year = yearStr,
                                        rating = rVal,
                                        genres = genres,
                                        synopsis = syn,
                                        posterUrl = poster,
                                        backdropUrl = item.optString("background").takeIf { it.isNotBlank() },
                                        provider = "Cinemeta / IMDb"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return@withContext results
    }

    fun getCachedMovie(context: Context, rawTitle: String, hintYear: String? = null, videoIdentity: String? = null): OnlineMediaMetadata? {
        if (videoIdentity != null) {
            getManualOverride(context, videoIdentity)?.let { return it }
        }
        val (cleanTitle, extractedYear) = extractCleanTitleAndYear(rawTitle)
        val effectiveYear = hintYear ?: extractedYear
        if (cleanTitle.length < 2) return null
        val cacheKey = "mov_" + sanitizeKey(cleanTitle + (effectiveYear?.let { "_$it" } ?: ""))
        memoryCache[cacheKey]?.let { return it }

        val cacheDir = getMetadataCacheDir(context)
        val candidateFiles = listOfNotNull(
            File(cacheDir, "$cacheKey.json").takeIf { it.exists() },
            if (effectiveYear != null) File(cacheDir, "mov_${sanitizeKey(cleanTitle)}.json").takeIf { it.exists() } else null,
            File(cacheDir, "mov_${sanitizeKey(rawTitle)}.json").takeIf { it.exists() }
        )

        val cacheFile = candidateFiles.firstOrNull() ?: return null
        return try {
            val json = JSONObject(cacheFile.readText())
            val cachedTitle = json.optString("title", cleanTitle)
            val cachedSynopsis = json.optString("synopsis")
            val cachedDesc = json.optString("description")
            val ratingSource = json.optString("ratingSource")

            val isVerifiedSource = ratingSource == "imdb" || ratingSource == "tvmaze" || ratingSource == "manual"
            val isBogus = !isVerifiedSource && (
                cachedTitle.contains("List of ", ignoreCase = true) ||
                cachedTitle.contains("Rajesh Kava", ignoreCase = true) ||
                cachedTitle.equals("Sexual identity", ignoreCase = true) ||
                (cachedTitle.equals("Blue", ignoreCase = true) && cachedSynopsis.contains("colour", ignoreCase = true)) ||
                cachedTitle.contains("Zombie Massacre", ignoreCase = true) ||
                scoreCandidate(cleanTitle, cachedTitle, cachedSynopsis, effectiveYear) < 0.20f
            )

            if (isBogus) {
                cacheFile.delete()
                val posterFile = File(getPosterCacheDir(context), "${cacheFile.nameWithoutExtension}.jpg")
                if (posterFile.exists()) posterFile.delete()
                return null
            }

            val posterUrl = json.optString("posterUrl").takeIf { it.isNotBlank() }
            val localPoster = json.optString("localPoster").takeIf { it.isNotBlank() && File(it).exists() }
                ?: File(getPosterCacheDir(context), "${cacheFile.nameWithoutExtension}.jpg").takeIf { it.exists() && it.length() > 500 }?.absolutePath
                ?: posterUrl

            val genresList = mutableListOf<String>()
            val genresArr = json.optJSONArray("genres")
            if (genresArr != null) {
                for (i in 0 until genresArr.length()) {
                    genresList.add(genresArr.getString(i))
                }
            }
            val meta = OnlineMediaMetadata(
                title = cachedTitle,
                year = json.optString("year").takeIf { it.isNotBlank() },
                rating = if (ratingSource == "tvmaze" || ratingSource == "imdb") displayRating(json.optString("rating").toDoubleOrNull()) else null,
                genres = genresList,
                synopsis = json.optString("synopsis").takeIf { it.isNotBlank() },
                posterUrl = posterUrl,
                backdropUrl = json.optString("backdropUrl").takeIf { it.isNotBlank() },
                localPosterPath = localPoster
            )
            memoryCache[cacheKey] = meta
            meta
        } catch (_: Exception) { null }
    }

    fun getCachedTvShow(context: Context, rawTitle: String): OnlineMediaMetadata? {
        val cleanTitle = cleanMediaTitle(rawTitle)
        if (cleanTitle.length < 2) return null
        val cacheKey = "tv_" + sanitizeKey(cleanTitle)
        memoryCache[cacheKey]?.let { return it }

        val cacheDir = getMetadataCacheDir(context)
        val candidateFiles = mutableListOf<File>()
        val exactFile = File(cacheDir, "$cacheKey.json")
        if (exactFile.exists()) candidateFiles.add(exactFile)
        val rawFile = File(cacheDir, "tv_${sanitizeKey(rawTitle)}.json")
        if (rawFile.exists()) candidateFiles.add(rawFile)
        if (candidateFiles.isEmpty()) {
            cacheDir.listFiles { _, name -> name.startsWith(cacheKey) && name.endsWith(".json") }?.let {
                candidateFiles.addAll(it)
            }
        }

        val cacheFile = candidateFiles.firstOrNull() ?: return null
        return try {
            val json = JSONObject(cacheFile.readText())
            val cachedTitle = json.optString("title", cleanTitle)
            val ratingSource = json.optString("ratingSource")
            val isVerifiedSource = ratingSource == "imdb" || ratingSource == "tvmaze" || ratingSource == "manual"

            if (!isVerifiedSource && scoreCandidate(cleanTitle, cachedTitle, null, null) <= 0f) {
                cacheFile.delete()
                return null
            }

            val posterUrl = json.optString("posterUrl").takeIf { it.isNotBlank() }
            val localPoster = json.optString("localPoster").takeIf { it.isNotBlank() && File(it).exists() }
                ?: File(getPosterCacheDir(context), "${cacheFile.nameWithoutExtension}.jpg").takeIf { it.exists() && it.length() > 500 }?.absolutePath
                ?: posterUrl

            val genresList = mutableListOf<String>()
            val genresArr = json.optJSONArray("genres")
            if (genresArr != null) {
                for (i in 0 until genresArr.length()) {
                    genresList.add(genresArr.getString(i))
                }
            }
            val meta = OnlineMediaMetadata(
                title = cachedTitle,
                year = json.optString("year").takeIf { it.isNotBlank() },
                rating = if (ratingSource == "tvmaze" || ratingSource == "imdb") displayRating(json.optString("rating").toDoubleOrNull()) else null,
                genres = genresList,
                synopsis = json.optString("synopsis").takeIf { it.isNotBlank() },
                posterUrl = posterUrl,
                backdropUrl = json.optString("backdropUrl").takeIf { it.isNotBlank() },
                localPosterPath = localPoster
            )
            memoryCache[cacheKey] = meta
            meta
        } catch (_: Exception) { null }
    }

    /**
     * Downloads and caches a remote image to local disk, returning the local file path.
     */
    suspend fun downloadAndCachePoster(context: Context, imageUrl: String, key: String): String? = withContext(Dispatchers.IO) {
        if (imageUrl.isBlank()) return@withContext null
        try {
            val safeKey = sanitizeKey(key)
            val posterFile = File(getPosterCacheDir(context), "${safeKey}.jpg")
            if (posterFile.exists() && posterFile.length() > 500) {
                return@withContext posterFile.absolutePath
            }

            val conn = openOnlineConnection(context, imageUrl) ?: return@withContext null
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.instanceFollowRedirects = true
            if (conn.responseCode in 200..299) {
                conn.inputStream.use { input ->
                    FileOutputStream(posterFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (posterFile.exists() && posterFile.length() > 500) {
                    return@withContext posterFile.absolutePath
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to download poster $imageUrl: ${e.message}")
        }
        null
    }

    /**
     * Fetches metadata & official high-resolution artwork directly from Cinemeta (IMDb & OTT catalog).
     * Covers global cinema, Hollywood, Bollywood, and all major streaming platforms (Prime, Netflix, Hotstar).
     */
    suspend fun fetchCinemetaMetadata(
        context: Context,
        cleanTitle: String,
        effectiveYear: String?,
        isSeries: Boolean = false
    ): OnlineMediaMetadata? = withContext(Dispatchers.IO) {
        if (!isOnlineMetadataEnabled(context) || !isNetworkConnected(context)) return@withContext null
        try {
            val type = if (isSeries) "series" else "movie"
            val searchQueries = mutableListOf(cleanTitle.trim())
            if (cleanTitle.contains(' ')) {
                val words = cleanTitle.split(' ').filter { it.isNotBlank() }
                if (words.size > 2) {
                    searchQueries.add(words.take(2).joinToString(" "))
                }
            }

            for (q in searchQueries) {
                val searchUrl = "https://v3-cinemeta.strem.io/catalog/$type/top/search=" +
                        URLEncoder.encode(q, "UTF-8") + ".json"
                val conn = openOnlineConnection(context, searchUrl) ?: continue
                conn.connectTimeout = 5000
                conn.readTimeout = 7000
                if (conn.responseCode != 200) continue

                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)
                val metas = root.optJSONArray("metas") ?: continue
                if (metas.length() == 0) continue

                var bestId: String? = null
                var bestScore = 0f
                var bestName: String? = null
                var bestYear: String? = null
                var bestPoster: String? = null

                for (i in 0 until metas.length().coerceAtMost(6)) {
                    val item = metas.getJSONObject(i)
                    val id = item.optString("id")
                    val name = item.optString("name")
                    val yearStr = item.optString("year").takeIf { it.isNotBlank() }
                    val score = scoreCandidate(q, name, null, effectiveYear ?: yearStr)
                    if (score > bestScore && score >= 0.25f) {
                        bestScore = score
                        bestId = id
                        bestName = name
                        bestYear = yearStr
                        bestPoster = item.optString("poster").takeIf { it.isNotBlank() }
                    }
                }

                if (bestId != null) {
                    // Query full details from Cinemeta meta endpoint
                    val detailUrl = "https://v3-cinemeta.strem.io/meta/$type/$bestId.json"
                    val detConn = openOnlineConnection(context, detailUrl)
                    if (detConn != null) {
                        detConn.connectTimeout = 5000
                        detConn.readTimeout = 7000
                        if (detConn.responseCode == 200) {
                            val detBody = detConn.inputStream.bufferedReader().use { it.readText() }
                            val detMeta = JSONObject(detBody).optJSONObject("meta")
                            if (detMeta != null) {
                                val officialTitle = detMeta.optString("name", bestName ?: cleanTitle)
                                val year = detMeta.optString("year").takeIf { it.isNotBlank() } ?: bestYear ?: effectiveYear
                                val posterUrl = detMeta.optString("poster").takeIf { it.isNotBlank() } ?: bestPoster
                                val backdropUrl = detMeta.optString("background").takeIf { it.isNotBlank() }
                                val synopsis = detMeta.optString("description").takeIf { it.isNotBlank() }
                                val ratingStr = detMeta.optString("imdbRating").takeIf { it.isNotBlank() }
                                val ratingVal = ratingStr?.toDoubleOrNull()?.let { displayRating(it) }

                                val genresList = mutableListOf<String>()
                                val genresArr = detMeta.optJSONArray("genres")
                                if (genresArr != null) {
                                    for (j in 0 until genresArr.length()) {
                                        genresList.add(genresArr.getString(j))
                                    }
                                }

                                val cacheKey = (if (isSeries) "tv_" else "mov_") + sanitizeKey(cleanTitle + (effectiveYear?.let { "_$it" } ?: ""))
                                val localPoster = if (!posterUrl.isNullOrBlank()) {
                                    downloadAndCachePoster(context, posterUrl, cacheKey)
                                } else null

                                return@withContext OnlineMediaMetadata(
                                    title = officialTitle,
                                    year = year,
                                    rating = ratingVal,
                                    genres = genresList.take(3),
                                    synopsis = synopsis,
                                    posterUrl = posterUrl,
                                    backdropUrl = backdropUrl,
                                    localPosterPath = localPoster
                                )
                            }
                        }
                    }

                    if (!bestPoster.isNullOrBlank()) {
                        val cacheKey = (if (isSeries) "tv_" else "mov_") + sanitizeKey(cleanTitle + (effectiveYear?.let { "_$it" } ?: ""))
                        val localPoster = downloadAndCachePoster(context, bestPoster, cacheKey)
                        return@withContext OnlineMediaMetadata(
                            title = bestName ?: cleanTitle,
                            year = bestYear ?: effectiveYear,
                            rating = null,
                            genres = listOf("Cinema", "Drama"),
                            synopsis = null,
                            posterUrl = bestPoster,
                            backdropUrl = null,
                            localPosterPath = localPoster
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cinemeta lookup failed for $cleanTitle: ${e.message}")
        }
        null
    }

    /**
     * Fetches movie metadata and official high-resolution poster online with offline disk caching.
     */
    suspend fun fetchMovieMetadata(
        context: Context,
        rawTitle: String,
        hintYear: String? = null,
        videoIdentity: String? = null
    ): OnlineMediaMetadata? = withContext(Dispatchers.IO) {
        if (videoIdentity != null) {
            getManualOverride(context, videoIdentity)?.let { return@withContext it }
        }
        val (cleanTitle, extractedYear) = extractCleanTitleAndYear(rawTitle)
        val effectiveYear = hintYear ?: extractedYear
        if (cleanTitle.length < 2) return@withContext null
        val cacheKey = "mov_" + sanitizeKey(cleanTitle + (effectiveYear?.let { "_$it" } ?: ""))

        // 1. Memory Cache
        memoryCache[cacheKey]?.let { return@withContext it }

        // 2. Disk Cache
        val cacheFile = File(getMetadataCacheDir(context), "$cacheKey.json")
        if (cacheFile.exists()) {
            try {
                val jsonStr = cacheFile.readText()
                val json = JSONObject(jsonStr)
                val cachedTitle = json.optString("title", cleanTitle)
                val cachedSynopsis = json.optString("synopsis")
                val cachedDesc = json.optString("description")

                val ratingSource = json.optString("ratingSource")
                val isVerifiedSource = ratingSource == "imdb" || ratingSource == "tvmaze" || ratingSource == "manual"
                val isBogus = !isVerifiedSource && (
                    cachedTitle.contains("List of ", ignoreCase = true) ||
                    cachedTitle.contains("Rajesh Kava", ignoreCase = true) ||
                    cachedTitle.equals("Sexual identity", ignoreCase = true) ||
                    (cachedTitle.equals("Blue", ignoreCase = true) && cachedSynopsis.contains("colour", ignoreCase = true)) ||
                    cachedTitle.contains("Zombie Massacre", ignoreCase = true) ||
                    scoreCandidate(cleanTitle, cachedTitle, cachedSynopsis, effectiveYear) < 0.20f
                )

                if (isBogus) {
                    cacheFile.delete()
                    val posterFile = File(getPosterCacheDir(context), "${cacheKey}.jpg")
                    if (posterFile.exists()) posterFile.delete()
                } else {
                    val posterUrl = json.optString("posterUrl").takeIf { it.isNotBlank() }
                    val localPoster = json.optString("localPoster").takeIf { it.isNotBlank() && File(it).exists() }
                        ?: File(getPosterCacheDir(context), "${cacheKey}.jpg").takeIf { it.exists() && it.length() > 500 }?.absolutePath
                        ?: posterUrl?.let { downloadAndCachePoster(context, it, cacheKey) }

                    val genresList = mutableListOf<String>()
                    val genresArr = json.optJSONArray("genres")
                    if (genresArr != null) {
                        for (i in 0 until genresArr.length()) {
                            genresList.add(genresArr.getString(i))
                        }
                    }

                    val metadata = OnlineMediaMetadata(
                        title = cachedTitle,
                        year = json.optString("year").takeIf { it.isNotBlank() },
                        rating = if (json.optString("ratingSource") == "tvmaze" || json.optString("ratingSource") == "imdb") displayRating(json.optString("rating").toDoubleOrNull()) else null,
                        genres = genresList,
                        synopsis = json.optString("synopsis").takeIf { it.isNotBlank() },
                        posterUrl = posterUrl,
                        backdropUrl = json.optString("backdropUrl").takeIf { it.isNotBlank() },
                        localPosterPath = localPoster
                    )
                    memoryCache[cacheKey] = metadata
                    return@withContext metadata
                }
            } catch (_: Exception) { }
        }

        // 3. Network Fetch
        if (!isOnlineMetadataEnabled(context) || !isNetworkConnected(context)) return@withContext null

        // Tier 1: Query Cinemeta (IMDb & OTT Platform Catalog with official artwork)
        val cinemeta = fetchCinemetaMetadata(context, cleanTitle, effectiveYear, isSeries = false)
        if (cinemeta != null) {
            try {
                val cacheJson = JSONObject().apply {
                    put("title", cinemeta.title)
                    put("year", cinemeta.year)
                    put("rating", cinemeta.rating)
                    put("ratingSource", "imdb")
                    put("synopsis", cinemeta.synopsis)
                    put("posterUrl", cinemeta.posterUrl)
                    put("backdropUrl", cinemeta.backdropUrl)
                    put("localPoster", cinemeta.localPosterPath)
                    val arr = JSONArray()
                    cinemeta.genres.forEach { arr.put(it) }
                    put("genres", arr)
                }
                cacheFile.writeText(cacheJson.toString())
            } catch (_: Exception) { }
            memoryCache[cacheKey] = cinemeta
            return@withContext cinemeta
        }

        // Tier 2: Wikipedia Fallback with Strict Film Validation

        try {
            val candidateTitles = LinkedHashSet<String>()

            fun searchOpenSearch(query: String) {
                try {
                    val url = "https://en.wikipedia.org/w/api.php?action=opensearch&search=" +
                            URLEncoder.encode(query, "UTF-8") + "&limit=6&namespace=0&format=json"
                    val conn = openOnlineConnection(context, url) ?: return
                    conn.connectTimeout = 5000
                    conn.readTimeout = 7000
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val arr = JSONArray(body)
                        if (arr.length() > 1) {
                            val titles = arr.optJSONArray(1)
                            if (titles != null) {
                                for (i in 0 until titles.length()) {
                                    val t = titles.getString(i)
                                    val s = scoreCandidate(cleanTitle, t, null, effectiveYear)
                                    if (s >= 0.25f || t.contains(cleanTitle, ignoreCase = true) || cleanTitle.contains(t, ignoreCase = true)) {
                                        candidateTitles.add(t)
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) { }
            }

            // Step 1: OpenSearch for exact clean title
            searchOpenSearch(cleanTitle.trim())

            // Step 2: OpenSearch with explicit film tag for short or ambiguous titles
            val words = cleanTitle.split(' ').filter { it.isNotBlank() }
            if (words.size <= 2) {
                searchOpenSearch("${cleanTitle.trim()} (film)")
            }

            // Step 3: If multi-word title (e.g. "Apocalypse Z New World"), try with first 2 words
            if (candidateTitles.isEmpty() && words.size > 2) {
                val shortened = words.take(2).joinToString(" ")
                searchOpenSearch(shortened)
            }

            // Step 4: Fallback to Wikipedia Full-Text Search
            if (candidateTitles.isEmpty()) {
                try {
                    val searchQuery = "$cleanTitle ${effectiveYear ?: ""} film".trim()
                    val searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=" +
                            URLEncoder.encode(searchQuery, "UTF-8") + "&format=json"
                    val conn = openOnlineConnection(context, searchUrl)
                    if (conn != null) {
                        conn.connectTimeout = 5000
                        conn.readTimeout = 7000
                        if (conn.responseCode == 200) {
                            val body = conn.inputStream.bufferedReader().use { it.readText() }
                            val searchJson = JSONObject(body)
                            val searchHits = searchJson.optJSONObject("query")?.optJSONArray("search")
                            if (searchHits != null) {
                                for (i in 0 until searchHits.length()) {
                                    val hit = searchHits.getJSONObject(i)
                                    val hitTitle = hit.optString("title")
                                    val hitSnippet = hit.optString("snippet")
                                    val s = scoreCandidate(cleanTitle, hitTitle, hitSnippet, effectiveYear)
                                    if (s >= 0.25f) {
                                        candidateTitles.add(hitTitle)
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) { }
            }

            // Inspect candidates in order until a verified film work is found
            for (candidateTitle in candidateTitles.take(5)) {
                try {
                    val summaryUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" +
                            URLEncoder.encode(candidateTitle.replace(' ', '_'), "UTF-8")
                    val sumConn = openOnlineConnection(context, summaryUrl) ?: continue
                    sumConn.connectTimeout = 5000
                    sumConn.readTimeout = 7000

                    if (sumConn.responseCode != 200) continue
                    val sumBody = sumConn.inputStream.bufferedReader().use { it.readText() }
                    val sumJson = JSONObject(sumBody)

                    val type = sumJson.optString("type")
                    if (type == "disambiguation") continue

                    val officialTitle = sumJson.optString("title", candidateTitle)
                    val synopsis = sumJson.optString("extract").takeIf { it.isNotBlank() }
                    val desc = sumJson.optString("description")

                    // Verify this page is truly a movie or TV film work
                    if (!isFilmOrTvWork(officialTitle, desc, synopsis)) {
                        continue
                    }

                    // Candidate must have a reasonable relevance score
                    val score = scoreCandidate(cleanTitle, officialTitle, synopsis, effectiveYear)
                    if (score < 0.25f && !officialTitle.contains(cleanTitle, ignoreCase = true) && !cleanTitle.contains(officialTitle, ignoreCase = true)) {
                        continue
                    }

                    // High-resolution poster
                    val posterUrl = sumJson.optJSONObject("originalimage")?.optString("source")
                        ?: sumJson.optJSONObject("thumbnail")?.optString("source")

                    // Release year extraction
                    val yearMatch = Regex("\\b(19\\d{2}|20\\d{2})\\b").find(desc + " " + (synopsis ?: ""))
                    val detectedYear = yearMatch?.groupValues?.get(1) ?: effectiveYear

                    // Accurate genre detection from plot summary
                    val combinedText = (desc + " " + (synopsis ?: "") + " " + officialTitle).lowercase(Locale.US)
                    val detectedGenres = mutableListOf<String>()

                    when {
                        combinedText.containsAny("horror", "slasher", "haunted", "supernatural", "exorcist", "paranormal", "gore", "demonic", "creepy", "witch", "ghost", "vampire", "zombie") -> {
                            detectedGenres.add("Horror")
                            if (combinedText.containsAny("mystery", "thriller", "suspense")) detectedGenres.add("Mystery")
                            detectedGenres.add("Thriller")
                        }
                        combinedText.containsAny("superhero", "marvel", "dc comics", "avengers", "sci-fi", "space", "interstellar", "science fiction", "galaxy") -> {
                            detectedGenres.add("Sci-Fi")
                            detectedGenres.add("Action")
                            detectedGenres.add("Adventure")
                        }
                        combinedText.containsAny("animated", "animation", "anime", "pixar", "disney", "cartoon") -> {
                            detectedGenres.add("Animation")
                            detectedGenres.add("Family")
                            detectedGenres.add("Adventure")
                        }
                        combinedText.containsAny("action", "martial arts", "chase", "combat", "thriller") -> {
                            detectedGenres.add("Action")
                            detectedGenres.add("Thriller")
                            if (combinedText.contains("crime")) detectedGenres.add("Crime")
                        }
                        combinedText.containsAny("comedy", "satire", "parody", "hilarious") -> {
                            detectedGenres.add("Comedy")
                            if (combinedText.contains("romance") || combinedText.contains("romantic")) detectedGenres.add("Romance")
                        }
                        combinedText.containsAny("documentary", "historical", "history", "biography", "biographical") -> {
                            detectedGenres.add("Documentary")
                            detectedGenres.add("History")
                        }
                        else -> {
                            detectedGenres.add("Cinema")
                            detectedGenres.add("Drama")
                        }
                    }

                    // Download and cache poster to disk immediately
                    val localPoster = if (!posterUrl.isNullOrBlank()) {
                        downloadAndCachePoster(context, posterUrl, cacheKey)
                    } else null

                    val metadata = OnlineMediaMetadata(
                        title = officialTitle,
                        year = detectedYear,
                        rating = null,
                        genres = detectedGenres.distinct().take(3),
                        synopsis = synopsis,
                        posterUrl = posterUrl,
                        localPosterPath = localPoster
                    )

                    // Persist to Disk Cache
                    try {
                        val cacheJson = JSONObject().apply {
                            put("title", metadata.title)
                            put("year", metadata.year)
                            put("rating", metadata.rating)
                            if (metadata.rating != null) put("ratingSource", "tvmaze")
                            put("synopsis", metadata.synopsis)
                            put("description", desc)
                            put("posterUrl", metadata.posterUrl)
                            put("localPoster", metadata.localPosterPath)
                            val arr = org.json.JSONArray()
                            metadata.genres.forEach { arr.put(it) }
                            put("genres", arr)
                        }
                        cacheFile.writeText(cacheJson.toString())
                    } catch (_: Exception) { }

                    memoryCache[cacheKey] = metadata
                    return@withContext metadata
                } catch (_: Exception) { }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Online movie metadata lookup failed for $cleanTitle: ${e.message}")
        }

        null
    }

    /**
     * Fetches TV show metadata and official high-resolution poster from TVMaze API.
     */
    suspend fun fetchTvShowMetadata(
        context: Context,
        rawTitle: String
    ): OnlineMediaMetadata? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanMediaTitle(rawTitle)
        if (cleanTitle.length < 2) return@withContext null
        val cacheKey = "tv_" + sanitizeKey(cleanTitle)

        memoryCache[cacheKey]?.let { return@withContext it }

        val cacheFile = File(getMetadataCacheDir(context), "$cacheKey.json")
        if (cacheFile.exists()) {
            try {
                val jsonStr = cacheFile.readText()
                val json = JSONObject(jsonStr)
                val posterUrl = json.optString("posterUrl").takeIf { it.isNotBlank() }
                val localPoster = json.optString("localPoster").takeIf { it.isNotBlank() }
                    ?: posterUrl?.let { downloadAndCachePoster(context, it, cacheKey) }

                val genresList = mutableListOf<String>()
                val genresArr = json.optJSONArray("genres")
                if (genresArr != null) {
                    for (i in 0 until genresArr.length()) {
                        genresList.add(genresArr.getString(i))
                    }
                }

                val metadata = OnlineMediaMetadata(
                    title = json.optString("title", cleanTitle),
                    year = json.optString("year").takeIf { it.isNotBlank() },
                    rating = if (json.optString("ratingSource") == "tvmaze") displayRating(json.optString("rating").toDoubleOrNull()) else null,
                    genres = genresList,
                    synopsis = json.optString("synopsis").takeIf { it.isNotBlank() },
                    posterUrl = posterUrl,
                    localPosterPath = localPoster
                )
                memoryCache[cacheKey] = metadata
                return@withContext metadata
            } catch (_: Exception) { }
        }

        if (!isOnlineMetadataEnabled(context) || !isNetworkConnected(context)) return@withContext null

        // Tier 1: Query Cinemeta (IMDb & OTT Series Catalog with official artwork)
        val cinemetaTv = fetchCinemetaMetadata(context, cleanTitle, null, isSeries = true)
        if (cinemetaTv != null) {
            try {
                val cacheJson = JSONObject().apply {
                    put("title", cinemetaTv.title)
                    put("year", cinemetaTv.year)
                    put("rating", cinemetaTv.rating)
                    put("ratingSource", "imdb")
                    put("synopsis", cinemetaTv.synopsis)
                    put("posterUrl", cinemetaTv.posterUrl)
                    put("backdropUrl", cinemetaTv.backdropUrl)
                    put("localPoster", cinemetaTv.localPosterPath)
                    val arr = JSONArray()
                    cinemetaTv.genres.forEach { arr.put(it) }
                    put("genres", arr)
                }
                cacheFile.writeText(cacheJson.toString())
            } catch (_: Exception) { }
            memoryCache[cacheKey] = cinemetaTv
            return@withContext cinemetaTv
        }

        val queryName = cleanTitle.replace(Regex("\\b(19\\d{2}|20\\d{2})\\b"), "").trim().ifEmpty { cleanTitle }

        try {
            var selectedShowJson: JSONObject? = null

            // 1. Try TVMaze singlesearch
            val tvMazeUrl = "https://api.tvmaze.com/singlesearch/shows?q=" +
                    URLEncoder.encode(queryName, "UTF-8")
            val conn = openOnlineConnection(context, tvMazeUrl)
            if (conn != null) {
                conn.connectTimeout = 6000
                conn.readTimeout = 8000
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val single = JSONObject(body)
                    val sName = single.optString("name")
                    if (scoreCandidate(cleanTitle, sName, null, null) >= 0.35f) {
                        selectedShowJson = single
                    }
                }
            }

            // 2. If singlesearch was rejected or failed, query TVMaze multi-search
            if (selectedShowJson == null) {
                val multiUrl = "https://api.tvmaze.com/search/shows?q=" +
                        URLEncoder.encode(queryName, "UTF-8")
                val multiConn = openOnlineConnection(context, multiUrl)
                if (multiConn != null) {
                    multiConn.connectTimeout = 6000
                    multiConn.readTimeout = 8000
                    if (multiConn.responseCode == 200) {
                        val body = multiConn.inputStream.bufferedReader().use { it.readText() }
                        val arr = JSONArray(body)
                        var bestShowScore = 0f
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val showObj = item.optJSONObject("show") ?: continue
                            val sName = showObj.optString("name")
                            val sDesc = showObj.optString("summary")
                            val sc = scoreCandidate(cleanTitle, sName, sDesc, null)
                            if (sc > bestShowScore && sc >= 0.35f) {
                                bestShowScore = sc
                                selectedShowJson = showObj
                            }
                        }
                    }
                }
            }

            if (selectedShowJson != null) {
                val showJson = selectedShowJson
                val showName = showJson.optString("name", cleanTitle)
                val imageObj = showJson.optJSONObject("image")
                val posterUrl = imageObj?.optString("original") ?: imageObj?.optString("medium")

                val rawSummary = showJson.optString("summary")
                val cleanSummary = rawSummary.replace(Regex("<[^>]*>"), "").trim().takeIf { it.isNotBlank() }

                val ratingObj = showJson.optJSONObject("rating")
                val ratingVal = if (ratingObj != null && !ratingObj.isNull("average")) {
                    displayRating(ratingObj.optDouble("average"))
                } else null

                val premiered = showJson.optString("premiered")
                val yearVal = premiered.take(4).takeIf { it.matches(Regex("[0-9]{4}")) }

                val genresList = mutableListOf<String>()
                val genresArr = showJson.optJSONArray("genres")
                if (genresArr != null) {
                    for (i in 0 until genresArr.length()) {
                        genresList.add(genresArr.getString(i))
                    }
                }
                if (genresList.isEmpty()) {
                    genresList.addAll(listOf("Drama", "Series"))
                }

                val localPoster = if (!posterUrl.isNullOrBlank()) {
                    downloadAndCachePoster(context, posterUrl, cacheKey)
                } else null

                val metadata = OnlineMediaMetadata(
                    title = showName,
                    year = yearVal,
                    rating = ratingVal,
                    genres = genresList,
                    synopsis = cleanSummary,
                    posterUrl = posterUrl,
                    localPosterPath = localPoster
                )

                try {
                    val cacheJson = JSONObject().apply {
                        put("title", metadata.title)
                        put("year", metadata.year)
                        put("rating", metadata.rating)
                        if (metadata.rating != null) put("ratingSource", "tvmaze")
                        put("synopsis", metadata.synopsis)
                        put("posterUrl", metadata.posterUrl)
                        put("localPoster", metadata.localPosterPath)
                        val arr = org.json.JSONArray()
                        metadata.genres.forEach { arr.put(it) }
                        put("genres", arr)
                    }
                    cacheFile.writeText(cacheJson.toString())
                } catch (_: Exception) { }

                memoryCache[cacheKey] = metadata
                return@withContext metadata
            }
        } catch (e: Exception) {
            Log.w(TAG, "TVMaze lookup failed for $cleanTitle: ${e.message}")
        }

        // Fallback: Query Wikipedia TV Series with OpenSearch & candidate scoring
        try {
            var bestPageTitle: String? = null
            var bestScore = 0f

            val openSearchQuery = "$cleanTitle TV series"
            val openSearchUrl = "https://en.wikipedia.org/w/api.php?action=opensearch&search=" +
                    URLEncoder.encode(openSearchQuery, "UTF-8") + "&limit=6&namespace=0&format=json"

            val openConn = openOnlineConnection(context, openSearchUrl)
            if (openConn != null) {
                openConn.connectTimeout = 6000
                openConn.readTimeout = 8000
                if (openConn.responseCode == 200) {
                    val body = openConn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONArray(body)
                    if (arr.length() > 1) {
                        val titles = arr.optJSONArray(1)
                        if (titles != null) {
                            for (i in 0 until titles.length()) {
                                val t = titles.getString(i)
                                val s = scoreCandidate(cleanTitle, t, null, null)
                                if (s > bestScore && s >= 0.35f) {
                                    bestScore = s
                                    bestPageTitle = t
                                }
                            }
                        }
                    }
                }
            }

            if (bestPageTitle.isNullOrBlank() || bestScore < 0.35f) {
                return@withContext null
            }

            val pageTitle = bestPageTitle

            val summaryUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" +
                    URLEncoder.encode(pageTitle.replace(' ', '_'), "UTF-8")

            val sumConn = openOnlineConnection(context, summaryUrl) ?: return@withContext null
            sumConn.connectTimeout = 6000
            sumConn.readTimeout = 8000

            if (sumConn.responseCode == 200) {
                val sumBody = sumConn.inputStream.bufferedReader().use { it.readText() }
                val sumJson = JSONObject(sumBody)

                val officialTitle = sumJson.optString("title", cleanTitle)
                val synopsis = sumJson.optString("extract").takeIf { it.isNotBlank() }
                val desc = sumJson.optString("description")

                if (!isFilmOrTvWork(officialTitle, desc, synopsis)) return@withContext null

                val posterUrl = sumJson.optJSONObject("originalimage")?.optString("source")
                    ?: sumJson.optJSONObject("thumbnail")?.optString("source")

                val yearMatch = Regex("\\b(19\\d{2}|20\\d{2})\\b").find(desc + " " + (synopsis ?: ""))
                val detectedYear = yearMatch?.groupValues?.get(1)

                val combinedText = (desc + " " + (synopsis ?: "") + " " + officialTitle).lowercase(Locale.US)
                val detectedGenres = mutableListOf<String>()

                when {
                    combinedText.containsAny("horror", "supernatural", "mystery", "thriller", "creepy", "stephen king") -> {
                        detectedGenres.add("Horror")
                        detectedGenres.add("Mystery")
                        detectedGenres.add("Drama")
                    }
                    combinedText.containsAny("sci-fi", "science fiction", "space") -> {
                        detectedGenres.add("Sci-Fi")
                        detectedGenres.add("Drama")
                    }
                    combinedText.containsAny("animated", "anime", "animation") -> {
                        detectedGenres.add("Animation")
                        detectedGenres.add("Fantasy")
                    }
                    combinedText.containsAny("comedy", "sitcom") -> {
                        detectedGenres.add("Comedy")
                    }
                    else -> {
                        detectedGenres.add("Drama")
                        detectedGenres.add("Series")
                    }
                }

                val localPoster = if (!posterUrl.isNullOrBlank()) {
                    downloadAndCachePoster(context, posterUrl, cacheKey)
                } else null

                val metadata = OnlineMediaMetadata(
                    title = officialTitle,
                    year = detectedYear,
                    rating = null,
                    genres = detectedGenres.distinct().take(3),
                    synopsis = synopsis,
                    posterUrl = posterUrl,
                    localPosterPath = localPoster
                )

                try {
                    val cacheJson = JSONObject().apply {
                        put("title", metadata.title)
                        put("year", metadata.year)
                        put("rating", metadata.rating)
                        if (metadata.rating != null) put("ratingSource", "tvmaze")
                        put("synopsis", metadata.synopsis)
                        put("posterUrl", metadata.posterUrl)
                        put("localPoster", metadata.localPosterPath)
                        val arr = org.json.JSONArray()
                        metadata.genres.forEach { arr.put(it) }
                        put("genres", arr)
                    }
                    cacheFile.writeText(cacheJson.toString())
                } catch (_: Exception) { }

                memoryCache[cacheKey] = metadata
                return@withContext metadata
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wikipedia TV fallback lookup failed for $cleanTitle: ${e.message}")
        }

        null
    }

    private fun String.containsAny(vararg candidates: String): Boolean {
        return candidates.any { this.contains(it, ignoreCase = true) }
    }
}
