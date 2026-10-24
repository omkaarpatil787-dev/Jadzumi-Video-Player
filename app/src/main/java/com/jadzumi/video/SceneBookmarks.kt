package com.jadzumi.video

import android.content.Context
import java.security.MessageDigest

object SceneBookmarkStore {
    private fun prefs(context: Context) = context.getSharedPreferences("scene_bookmarks", Context.MODE_PRIVATE)
    fun key(identity: String): String = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
        .joinToString("") { "%02x".format(it) }
    fun read(context: Context, identity: String): List<SceneBookmark> = runCatching {
        SceneBookmarkCodec.decode(prefs(context).getString(key(identity), SceneBookmarkCodec.HEADER) ?: SceneBookmarkCodec.HEADER)
    }.getOrDefault(emptyList())
    fun write(context: Context, identity: String, bookmarks: List<SceneBookmark>) {
        prefs(context).edit().putString(key(identity), SceneBookmarkCodec.encode(bookmarks)).apply()
    }
}
