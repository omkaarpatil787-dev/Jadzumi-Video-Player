package com.jadzumi.video

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class WatchProgressRecord(
    val mediaKey: String,
    val positionMs: Long,
    val durationMs: Long,
    val isCompleted: Boolean,
    val lastWatchedAt: Long
)

/**
 * Native SQLite Watch Intelligence Database.
 * Fulfills Section 6 of the Locked Master Blueprint.
 * Replaces flat SharedPreferences with reliable, indexed SQLite tables for watch progress and history.
 */
class WatchIntelligenceDatabase(context: Context) : SQLiteOpenHelper(context, "jadzumi_watch_intelligence.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS watch_progress (
                media_key TEXT PRIMARY KEY,
                position_ms INTEGER NOT NULL,
                duration_ms INTEGER NOT NULL,
                is_completed INTEGER NOT NULL,
                last_watched_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_watch_progress_last_watched ON watch_progress(last_watched_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Reserved for future schema migrations
    }

    fun saveProgress(mediaKey: String, positionMs: Long, durationMs: Long, isCompleted: Boolean) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("media_key", mediaKey)
                put("position_ms", positionMs)
                put("duration_ms", durationMs)
                put("is_completed", if (isCompleted) 1 else 0)
                put("last_watched_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict("watch_progress", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (_: Exception) {}
    }

    fun getProgress(mediaKey: String): WatchProgressRecord? {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                "watch_progress",
                arrayOf("media_key", "position_ms", "duration_ms", "is_completed", "last_watched_at"),
                "media_key = ?",
                arrayOf(mediaKey),
                null, null, null
            )
            cursor.use { c ->
                if (c.moveToFirst()) {
                    WatchProgressRecord(
                        mediaKey = c.getString(0),
                        positionMs = c.getLong(1),
                        durationMs = c.getLong(2),
                        isCompleted = c.getInt(3) == 1,
                        lastWatchedAt = c.getLong(4)
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun clearAll() {
        try {
            writableDatabase.delete("watch_progress", null, null)
        } catch (_: Exception) {}
    }

    fun removeRecord(mediaKey: String) {
        try {
            writableDatabase.delete("watch_progress", "media_key = ?", arrayOf(mediaKey))
        } catch (_: Exception) {}
    }
}
