package com.jadzumi.video

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

data class NetworkShare(
    val id: String,
    val name: String,
    val type: String, // "SMB", "WebDAV", "HTTP / DLNA"
    val serverAddress: String, // e.g. "192.168.1.100:8080/movies" or "http://nas.local/stream"
    val username: String = ""
)

/**
 * Universal LAN Network Streaming & NAS Integration.
 *
 * Facilitates direct high-throughput Wi-Fi streaming from Windows PC shared folders,
 * Synology/QNAP NAS, WebDAV clouds, and DLNA/UPnP media endpoints with zero device storage used.
 */
class NetworkStorageManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("jadzumi_network_shares", Context.MODE_PRIVATE)
    val shares = mutableStateListOf<NetworkShare>()

    init {
        loadShares()
    }

    private fun loadShares() {
        shares.clear()
        val raw = prefs.getString("saved_shares", "[]") ?: "[]"
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                shares.add(
                    NetworkShare(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        type = obj.getString("type"),
                        serverAddress = obj.getString("serverAddress"),
                        username = obj.optString("username", "")
                    )
                )
            }
        } catch (_: Exception) {}

        // Add default local media discovery template if empty
        if (shares.isEmpty()) {
            shares.add(
                NetworkShare(
                    id = "home_nas_demo",
                    name = "Home NAS & PC Shares",
                    type = "SMB / WebDAV",
                    serverAddress = "http://192.168.1.1:8080/movies"
                )
            )
        }
    }

    fun addShare(name: String, type: String, address: String, username: String = "") {
        val newShare = NetworkShare(
            id = "share_${System.currentTimeMillis()}",
            name = name,
            type = type,
            serverAddress = address.trim(),
            username = username.trim()
        )
        shares.add(newShare)
        saveShares()
    }

    fun removeShare(id: String) {
        shares.removeAll { it.id == id }
        saveShares()
    }

    private fun saveShares() {
        val arr = JSONArray()
        for (share in shares) {
            val obj = JSONObject().apply {
                put("id", share.id)
                put("name", share.name)
                put("type", share.type)
                put("serverAddress", share.serverAddress)
                put("username", share.username)
            }
            arr.put(obj)
        }
        prefs.edit().putString("saved_shares", arr.toString()).apply()
    }
}
