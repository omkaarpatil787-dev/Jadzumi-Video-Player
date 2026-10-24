package com.jadzumi.video

import java.io.StringReader
import java.io.StringWriter
import java.util.Base64
import java.util.Properties

/** Versioned local backup. No media, folder permissions, cached artwork or credentials. */
object BackupCodec {
    const val MAX_BYTES = 1_048_576
    val namespaces = setOf("playback", "appearance", "subtitle_prefs", "jadzumi_settings")
    private val booleans = setOf("bg_audio", "brightness_boost", "remember_progress", "save_track_prefs", "swipe_gestures", "night_mode", "extract_thumbnails", "scan_sidecars")
    private val appearanceBooleans = setOf("reducedMotion", "livelyAnimations", "themeBloom", "dockAutoMinimize", "pad5_welcome_done")
    private val appearanceStrings = setOf("theme", "cornerStyle", "bottomBarStyle")
    fun accepts(namespace: String, key: String, value: Any): Boolean = when (namespace) {
        "playback" -> when {
            isWatchHistoryKey(key) -> if (key.endsWith(".done")) value is Boolean else value is Long && value >= 0
            key in booleans -> value is Boolean
            key == "seek_seconds" -> value is Int && value in 5..60
            key == "audio_delay" -> value is Long && value in -500L..500L
            key == "sub_delay" -> value is Long && value in -10_000L..10_000L
            key in setOf("preferred_audio_lang", "preferred_sub_lang") -> value is String && value.length <= 128
            else -> false // Consent (online_metadata) must never be enabled by an imported file.
        }
        "appearance" -> when (key) {
            in appearanceBooleans -> value is Boolean
            in appearanceStrings -> value is String && value.length <= 128
            "glassIntensity" -> value is Float && value.isFinite() && value in 0f..1f
            else -> false
        }
        "subtitle_prefs" -> when (key) {
            "size" -> value is Float && value.isFinite() && value in 0.5f..2f
            "font", "color", "bg", "edge" -> value is String && value.length <= 128
            else -> false
        }
        "jadzumi_settings" -> key == "local_sandbox" && value is Boolean
        else -> false
    }

    fun encode(data: Map<String, Map<String, Any>>): String {
        val properties = Properties().apply { setProperty("format", "JadzumiBackup"); setProperty("version", "1") }
        data.forEach { (namespace, values) -> values.forEach { (key, value) ->
            if (accepts(namespace, key, value)) {
                val type = when (value) { is Boolean -> "b"; is Int -> "i"; is Long -> "l"; is Float -> "f"; else -> "s" }
                val encodedKey = Base64.getUrlEncoder().withoutPadding().encodeToString(key.toByteArray(Charsets.UTF_8))
                properties.setProperty("$namespace.$encodedKey", "$type:$value")
            }
        } }
        return StringWriter().also { properties.store(it, "Jadzumi local settings and viewing history") }.toString()
            .also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds 1 MB" } }
    }

    /** Parse and validate the ENTIRE file before the caller changes preferences. */
    fun decode(text: String): Map<String, Map<String, Any>> {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds 1 MB" }
        val p = Properties().apply { load(StringReader(text)) }
        require(p.getProperty("format") == "JadzumiBackup" && p.getProperty("version") == "1") { "Unsupported Jadzumi backup" }
        require(p.size <= 10_002) { "Too many backup entries" }
        val result = mutableMapOf<String, MutableMap<String, Any>>()
        p.stringPropertyNames().filter { it != "format" && it != "version" }.forEach { entry ->
            val namespace = entry.substringBefore('.')
            require(namespace in namespaces && '.' in entry) { "Unknown backup section" }
            val key = String(Base64.getUrlDecoder().decode(entry.substringAfter('.')), Charsets.UTF_8)
            val raw = p.getProperty(entry)
            require(raw.length in 2..4096 && raw[1] == ':') { "Invalid backup entry" }
            val value: Any = when (raw[0]) {
                'b' -> raw.substring(2).toBooleanStrict()
                'i' -> raw.substring(2).toInt()
                'l' -> raw.substring(2).toLong()
                'f' -> raw.substring(2).toFloat()
                's' -> raw.substring(2)
                else -> error("Unknown backup value type")
            }
            require(accepts(namespace, key, value)) { "Invalid backup preference" }
            result.getOrPut(namespace) { mutableMapOf() }[key] = value
        }
        return result
    }
}
