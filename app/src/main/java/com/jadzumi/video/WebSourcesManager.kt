package com.jadzumi.video

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

enum class WebSourceType {
    WEBSITE,
    DIRECT_STREAM,
    IPTV_PLAYLIST
}

data class WebSource(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val type: WebSourceType,
    val description: String = "",
    val iconUrl: String? = null,
    val isEnabled: Boolean = true,
    val addedTimestamp: Long = System.currentTimeMillis()
)

data class LiveChannel(
    val id: String = UUID.randomUUID().toString(),
    val sourceId: String = "",
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val group: String? = null,
    val isFavorite: Boolean = false
) {
    fun toLocalVideo(): LocalVideo {
        val cleanName = name.ifBlank { "Live TV Channel" }
        return LocalVideo(
            uri = streamUrl,
            folder = "Web Sources",
            name = "$cleanName.m3u8",
            mime = if (streamUrl.contains(".mpd", ignoreCase = true)) "application/dash+xml" else "application/x-mpegURL",
            bytes = -1L,
            modified = System.currentTimeMillis(),
            duration = -1L,
            width = 1920,
            height = 1080
        )
    }
}

class WebSourcesManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jadzumi_web_sources", Context.MODE_PRIVATE)

    private val _sources = MutableStateFlow<List<WebSource>>(emptyList())
    val sources: StateFlow<List<WebSource>> = _sources.asStateFlow()

    private val _channels = MutableStateFlow<List<LiveChannel>>(emptyList())
    val channels: StateFlow<List<LiveChannel>> = _channels.asStateFlow()

    private val _isFeatureEnabled = MutableStateFlow(prefs.getBoolean("web_sources_enabled", false))
    val isFeatureEnabled: StateFlow<Boolean> = _isFeatureEnabled.asStateFlow()

    init {
        loadPersistedData()
    }

    fun setFeatureEnabled(enabled: Boolean) {
        _isFeatureEnabled.value = enabled
        prefs.edit { putBoolean("web_sources_enabled", enabled) }
    }

    private fun loadPersistedData() {
        val sourcesJson = prefs.getString("sources_list", null)
        val loadedSources = mutableListOf<WebSource>()
        if (!sourcesJson.isNullOrBlank()) {
            try {
                val array = JSONArray(sourcesJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    loadedSources.add(
                        WebSource(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            url = obj.getString("url"),
                            type = WebSourceType.valueOf(obj.optString("type", WebSourceType.WEBSITE.name)),
                            description = obj.optString("description", ""),
                            iconUrl = if (obj.has("iconUrl") && !obj.isNull("iconUrl")) obj.getString("iconUrl") else null,
                            isEnabled = obj.optBoolean("isEnabled", true),
                            addedTimestamp = obj.optLong("addedTimestamp", System.currentTimeMillis())
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // Populate verified public test sources if list is empty
        if (loadedSources.isEmpty()) {
            loadedSources.addAll(getInitialDefaultSources())
            persistSources(loadedSources)
        }
        _sources.value = loadedSources

        // Load channels
        val channelsJson = prefs.getString("channels_list", null)
        val loadedChannels = mutableListOf<LiveChannel>()
        if (!channelsJson.isNullOrBlank()) {
            try {
                val array = JSONArray(channelsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    loadedChannels.add(
                        LiveChannel(
                            id = obj.getString("id"),
                            sourceId = obj.optString("sourceId", ""),
                            name = obj.getString("name"),
                            streamUrl = obj.getString("streamUrl"),
                            logoUrl = if (obj.has("logoUrl") && !obj.isNull("logoUrl")) obj.getString("logoUrl") else null,
                            group = if (obj.has("group") && !obj.isNull("group")) obj.getString("group") else null,
                            isFavorite = obj.optBoolean("isFavorite", false)
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        if (loadedChannels.isEmpty()) {
            loadedChannels.addAll(getInitialDefaultChannels())
            persistChannels(loadedChannels)
        }
        _channels.value = loadedChannels
    }

    fun addSource(source: WebSource) {
        val updated = _sources.value.filter { it.id != source.id } + source
        _sources.value = updated
        persistSources(updated)

        if (source.type == WebSourceType.DIRECT_STREAM) {
            val channel = LiveChannel(
                sourceId = source.id,
                name = source.name,
                streamUrl = source.url,
                group = "Direct Streams"
            )
            addChannel(channel)
        }
    }

    fun removeSource(sourceId: String) {
        val updatedSources = _sources.value.filter { it.id != sourceId }
        _sources.value = updatedSources
        persistSources(updatedSources)

        val updatedChannels = _channels.value.filter { it.sourceId != sourceId }
        _channels.value = updatedChannels
        persistChannels(updatedChannels)
    }

    fun addChannel(channel: LiveChannel) {
        val updated = _channels.value.filter { it.id != channel.id } + channel
        _channels.value = updated
        persistChannels(updated)
    }

    fun toggleFavorite(channelId: String) {
        val updated = _channels.value.map {
            if (it.id == channelId) it.copy(isFavorite = !it.isFavorite) else it
        }
        _channels.value = updated
        persistChannels(updated)
    }

    private fun persistSources(list: List<WebSource>) {
        try {
            val array = JSONArray()
            for (s in list) {
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("name", s.name)
                obj.put("url", s.url)
                obj.put("type", s.type.name)
                obj.put("description", s.description)
                s.iconUrl?.let { obj.put("iconUrl", it) }
                obj.put("isEnabled", s.isEnabled)
                obj.put("addedTimestamp", s.addedTimestamp)
                array.put(obj)
            }
            prefs.edit { putString("sources_list", array.toString()) }
        } catch (_: Exception) {}
    }

    private fun persistChannels(list: List<LiveChannel>) {
        try {
            val array = JSONArray()
            for (c in list) {
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("sourceId", c.sourceId)
                obj.put("name", c.name)
                obj.put("streamUrl", c.streamUrl)
                c.logoUrl?.let { obj.put("logoUrl", it) }
                c.group?.let { obj.put("group", it) }
                obj.put("isFavorite", c.isFavorite)
                array.put(obj)
            }
            prefs.edit { putString("channels_list", array.toString()) }
        } catch (_: Exception) {}
    }

    /**
     * Parse standard M3U / M3U8 IPTV Playlist format
     */
    fun parseM3u(content: String, sourceId: String): List<LiveChannel> {
        val channels = mutableListOf<LiveChannel>()
        var currentName = ""
        var currentLogo: String? = null
        var currentGroup: String? = null

        val logoRegex = Regex("""tvg-logo="([^"]+)"""", RegexOption.IGNORE_CASE)
        val groupRegex = Regex("""group-title="([^"]+)"""", RegexOption.IGNORE_CASE)

        for (rawLine in content.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                logoRegex.find(line)?.let { currentLogo = it.groupValues[1] }
                groupRegex.find(line)?.let { currentGroup = it.groupValues[1] }
                currentName = line.substringAfterLast(',').trim()
            } else if (!line.startsWith("#")) {
                if (line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true)) {
                    val channelName = currentName.ifBlank { "Channel ${channels.size + 1}" }
                    channels.add(
                        LiveChannel(
                            sourceId = sourceId,
                            name = channelName,
                            streamUrl = line,
                            logoUrl = currentLogo,
                            group = currentGroup
                        )
                    )
                    currentName = ""
                    currentLogo = null
                    currentGroup = null
                }
            }
        }
        return channels
    }

    suspend fun fetchAndImportM3u(url: String, sourceId: String): List<LiveChannel> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 12000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "JadzumiPlayer/2.0.9 (Android; Media3)")
            val content = connection.inputStream.bufferedReader().use { it.readText() }
            val parsed = parseM3u(content, sourceId)
            withContext(Dispatchers.Main) {
                val updated = _channels.value + parsed
                _channels.value = updated
                persistChannels(updated)
            }
            parsed
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getInitialDefaultSources(): List<WebSource> = listOf(
        WebSource(
            id = "global_free_tv",
            name = "Global Free TV",
            url = "https://www.globalfreetv.com/",
            type = WebSourceType.WEBSITE,
            description = "Public directory of free-to-air international live television streams"
        ),
        WebSource(
            id = "nasa_tv_feed",
            name = "NASA TV Live",
            url = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
            type = WebSourceType.DIRECT_STREAM,
            description = "Official NASA public educational live stream in 1080p HLS"
        ),
        WebSource(
            id = "red_bull_tv",
            name = "Red Bull TV",
            url = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
            type = WebSourceType.DIRECT_STREAM,
            description = "Official live action sports and documentary broadcast"
        )
    )

    private fun getInitialDefaultChannels(): List<LiveChannel> = listOf(
        LiveChannel(
            id = "ch_nasa_tv",
            sourceId = "nasa_tv_feed",
            name = "NASA TV HD Live",
            streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
            group = "Science & Space",
            isFavorite = true
        ),
        LiveChannel(
            id = "ch_sky_news",
            sourceId = "global_free_tv",
            name = "Sky News 24/7",
            streamUrl = "https://skynews-live.akamaized.net/hls/live/2012678/skynews/master.m3u8",
            group = "News",
            isFavorite = true
        ),
        LiveChannel(
            id = "ch_euronews",
            sourceId = "global_free_tv",
            name = "Euronews English HD",
            streamUrl = "https://rakuten-euronews-1-gb.samsung.wurl.tv/manifest/playlist.m3u8",
            group = "News",
            isFavorite = false
        ),
        LiveChannel(
            id = "ch_bloomberg",
            sourceId = "global_free_tv",
            name = "Bloomberg Global Markets",
            streamUrl = "https://liveproduseast.akamaized.net/us/Channel-HD-AWS-virginia-1/master.m3u8",
            group = "Financial News",
            isFavorite = false
        ),
        LiveChannel(
            id = "ch_red_bull",
            sourceId = "red_bull_tv",
            name = "Red Bull TV Live",
            streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
            group = "Sports",
            isFavorite = false
        ),
        LiveChannel(
            id = "ch_akamai_test",
            sourceId = "direct_test",
            name = "Akamai Adaptive Multi-Rate HLS",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            group = "Test Streams",
            isFavorite = false
        )
    )
}
