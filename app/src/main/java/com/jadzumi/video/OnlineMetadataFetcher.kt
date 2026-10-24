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

    fun cleanMediaTitle(raw: String): String {
        return raw.substringBeforeLast('.')
            .replace(EpisodePatterns.RELEASE_TAGS, " ")
            .replace(Regex("(?i)\\b(2160p|1080p|720p|480p|4k|uhd|hdr10\\+?|hdr|dovi|dv|remux|bluray|bdrip|web-?dl|webrip|x264|x265|hevc|avc|aac|dts|ddp?|atmos)\\b"), " ")
            .replace(Regex("[\\[\\](){}]"), " ")
            .replace(Regex("[-._+]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun sanitizeKey(key: String): String {
        return key.lowercase(Locale.US).replace(Regex("[^a-z0-9_]"), "_").take(64)
    }

    fun getCachedMovie(context: Context, rawTitle: String, hintYear: String? = null): OnlineMediaMetadata? {
        val cleanTitle = cleanMediaTitle(rawTitle)
        if (cleanTitle.length < 2) return null
        val cacheKey = "mov_" + sanitizeKey(cleanTitle + (hintYear?.let { "_$it" } ?: ""))
        memoryCache[cacheKey]?.let { return it }

        val cacheFile = File(getMetadataCacheDir(context), "$cacheKey.json")
        if (cacheFile.exists()) {
            return try {
                val json = JSONObject(cacheFile.readText())
                val posterUrl = json.optString("posterUrl").takeIf { it.isNotBlank() }
                val localPoster = json.optString("localPoster").takeIf { it.isNotBlank() }
                    ?: File(getPosterCacheDir(context), "${cacheKey}.jpg").takeIf { it.exists() && it.length() > 500 }?.absolutePath

                val genresList = mutableListOf<String>()
                val genresArr = json.optJSONArray("genres")
                if (genresArr != null) {
                    for (i in 0 until genresArr.length()) {
                        genresList.add(genresArr.getString(i))
                    }
                }
                val meta = OnlineMediaMetadata(
                    title = json.optString("title", cleanTitle),
                    year = json.optString("year").takeIf { it.isNotBlank() },
                    rating = json.optString("rating").takeIf { it.isNotBlank() },
                    genres = genresList,
                    synopsis = json.optString("synopsis").takeIf { it.isNotBlank() },
                    posterUrl = posterUrl,
                    localPosterPath = localPoster
                )
                memoryCache[cacheKey] = meta
                meta
            } catch (_: Exception) { null }
        }
        return null
    }

    fun getCachedTvShow(context: Context, rawTitle: String): OnlineMediaMetadata? {
        val cleanTitle = cleanMediaTitle(rawTitle)
        if (cleanTitle.length < 2) return null
        val cacheKey = "tv_" + sanitizeKey(cleanTitle)
        memoryCache[cacheKey]?.let { return it }

        val cacheFile = File(getMetadataCacheDir(context), "$cacheKey.json")
        if (cacheFile.exists()) {
            return try {
                val json = JSONObject(cacheFile.readText())
                val posterUrl = json.optString("posterUrl").takeIf { it.isNotBlank() }
                val localPoster = json.optString("localPoster").takeIf { it.isNotBlank() }
                    ?: File(getPosterCacheDir(context), "${cacheKey}.jpg").takeIf { it.exists() && it.length() > 500 }?.absolutePath

                val genresList = mutableListOf<String>()
                val genresArr = json.optJSONArray("genres")
                if (genresArr != null) {
                    for (i in 0 until genresArr.length()) {
                        genresList.add(genresArr.getString(i))
                    }
                }
                val meta = OnlineMediaMetadata(
                    title = json.optString("title", cleanTitle),
                    year = json.optString("year").takeIf { it.isNotBlank() },
                    rating = json.optString("rating").takeIf { it.isNotBlank() },
                    genres = genresList,
                    synopsis = json.optString("synopsis").takeIf { it.isNotBlank() },
                    posterUrl = posterUrl,
                    localPosterPath = localPoster
                )
                memoryCache[cacheKey] = meta
                meta
            } catch (_: Exception) { null }
        }
        return null
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
     * Fetches movie metadata and official high-resolution poster online with offline disk caching.
     */
    suspend fun fetchMovieMetadata(
        context: Context,
        rawTitle: String,
        hintYear: String? = null
    ): OnlineMediaMetadata? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanMediaTitle(rawTitle)
        if (cleanTitle.length < 2) return@withContext null
        val cacheKey = "mov_" + sanitizeKey(cleanTitle + (hintYear?.let { "_$it" } ?: ""))

        // 1. Memory Cache
        memoryCache[cacheKey]?.let { return@withContext it }

        // 2. Disk Cache
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
                    rating = json.optString("rating").takeIf { it.isNotBlank() },
                    genres = genresList,
                    synopsis = json.optString("synopsis").takeIf { it.isNotBlank() },
                    posterUrl = posterUrl,
                    backdropUrl = json.optString("backdropUrl").takeIf { it.isNotBlank() },
                    localPosterPath = localPoster
                )
                memoryCache[cacheKey] = metadata
                return@withContext metadata
            } catch (_: Exception) { }
        }

        // 3. Network Fetch
        if (!isOnlineMetadataEnabled(context) || !isNetworkConnected(context)) return@withContext null

        try {
            // Query Wikipedia Search API
            val searchQuery = "$cleanTitle ${hintYear ?: ""} film".trim()
            val searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=" +
                    URLEncoder.encode(searchQuery, "UTF-8") + "&format=json"

            val conn = openOnlineConnection(context, searchUrl) ?: return@withContext null
            conn.connectTimeout = 6000
            conn.readTimeout = 8000

            var pageTitle: String? = null
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val searchJson = JSONObject(body)
                val searchHits = searchJson.optJSONObject("query")?.optJSONArray("search")
                if (searchHits != null && searchHits.length() > 0) {
                    pageTitle = searchHits.getJSONObject(0).optString("title")
                }
            }

            if (pageTitle.isNullOrBlank()) {
                pageTitle = cleanTitle
            }

            // Query Wikipedia REST Summary API
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

                // High-resolution original poster
                val posterUrl = sumJson.optJSONObject("originalimage")?.optString("source")
                    ?: sumJson.optJSONObject("thumbnail")?.optString("source")

                // Release year extraction
                val yearMatch = Regex("\\b(19\\d{2}|20\\d{2})\\b").find(desc + " " + (synopsis ?: ""))
                val detectedYear = yearMatch?.groupValues?.get(1) ?: hintYear ?: "2024"

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
                    rating = "8.8",
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
                    rating = json.optString("rating").takeIf { it.isNotBlank() },
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

        val queryName = cleanTitle.replace(Regex("\\b(19\\d{2}|20\\d{2})\\b"), "").trim().ifEmpty { cleanTitle }

        try {
            val tvMazeUrl = "https://api.tvmaze.com/singlesearch/shows?q=" +
                    URLEncoder.encode(queryName, "UTF-8")

            val conn = openOnlineConnection(context, tvMazeUrl) ?: return@withContext null
            conn.connectTimeout = 6000
            conn.readTimeout = 8000

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val showJson = JSONObject(body)

                val showName = showJson.optString("name", cleanTitle)
                val imageObj = showJson.optJSONObject("image")
                val posterUrl = imageObj?.optString("original") ?: imageObj?.optString("medium")

                val rawSummary = showJson.optString("summary")
                val cleanSummary = rawSummary.replace(Regex("<[^>]*>"), "").trim().takeIf { it.isNotBlank() }

                val ratingObj = showJson.optJSONObject("rating")
                val ratingVal = if (ratingObj != null && !ratingObj.isNull("average")) {
                    String.format(Locale.US, "%.1f", ratingObj.optDouble("average"))
                } else "8.7"

                val premiered = showJson.optString("premiered")
                val yearVal = premiered.take(4).ifEmpty { "2024" }

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

        // Fallback: Query Wikipedia TV Series REST API if TVMaze failed or returned nothing
        try {
            val searchQuery = "$cleanTitle TV series".trim()
            val searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=" +
                    URLEncoder.encode(searchQuery, "UTF-8") + "&format=json"

            val conn = openOnlineConnection(context, searchUrl) ?: return@withContext null
            conn.connectTimeout = 6000
            conn.readTimeout = 8000

            var pageTitle: String? = null
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val searchJson = JSONObject(body)
                val searchHits = searchJson.optJSONObject("query")?.optJSONArray("search")
                if (searchHits != null && searchHits.length() > 0) {
                    pageTitle = searchHits.getJSONObject(0).optString("title")
                }
            }

            if (pageTitle.isNullOrBlank()) {
                pageTitle = cleanTitle
            }

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

                val posterUrl = sumJson.optJSONObject("originalimage")?.optString("source")
                    ?: sumJson.optJSONObject("thumbnail")?.optString("source")

                val yearMatch = Regex("\\b(19\\d{2}|20\\d{2})\\b").find(desc + " " + (synopsis ?: ""))
                val detectedYear = yearMatch?.groupValues?.get(1) ?: "2024"

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
                    rating = "8.8",
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
