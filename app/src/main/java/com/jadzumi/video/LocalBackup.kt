package com.jadzumi.video

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import java.io.ByteArrayOutputStream

object LocalBackup {
    private fun preferences(context: Context, name: String) = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    fun export(context: Context, uri: Uri) {
        val data = BackupCodec.namespaces.associateWith { name ->
            preferences(context, name).all.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
        }
        val text = BackupCodec.encode(data)
        requireNotNull(context.contentResolver.openOutputStream(uri, "wt")) { "Cannot open backup destination" }
            .bufferedWriter(Charsets.UTF_8).use { it.write(text) }
    }

    fun read(context: Context, uri: Uri): Map<String, Map<String, Any>> {
        val bytes = ByteArrayOutputStream()
        requireNotNull(context.contentResolver.openInputStream(uri)) { "Cannot open backup file" }.use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(bytes.size() + count <= BackupCodec.MAX_BYTES) { "Backup exceeds 1 MB" }
                bytes.write(buffer, 0, count)
            }
        }
        return BackupCodec.decode(bytes.toString("UTF-8"))
    }

    private fun put(editor: SharedPreferences.Editor, key: String, value: Any) {
        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is String -> editor.putString(key, value)
            is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
        }
    }

    /** Merge records; preserve folder grants and online consent. Roll back on write failure. */
    fun restore(context: Context, data: Map<String, Map<String, Any>>) {
        require(data.all { (name, values) -> name in BackupCodec.namespaces && values.all { (key, value) -> BackupCodec.accepts(name, key, value) } })
        val before = data.keys.associateWith { preferences(context, it).all.toMap() }
        try {
            data.forEach { (name, values) ->
                val editor = preferences(context, name).edit()
                values.forEach { (key, value) -> put(editor, key, value) }
                check(editor.commit()) { "Could not save restored settings" }
            }
        } catch (e: Exception) {
            before.forEach { (name, values) ->
                val editor = preferences(context, name).edit().clear()
                values.forEach { (key, value) -> if (value != null) put(editor, key, value) }
                editor.commit()
            }
            throw e
        }
    }
}
