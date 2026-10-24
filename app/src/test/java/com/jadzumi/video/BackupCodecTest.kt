package com.jadzumi.video

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class BackupCodecTest {
    private fun rejects(block: () -> Unit) {
        try { block(); fail("Expected invalid backup to be rejected") } catch (_: IllegalArgumentException) {} catch (_: IllegalStateException) {}
    }
    @Test fun bookmarksAndSeriesPreferencesRoundTripInVersionTwo() {
        val scenes = SceneBookmarkCodec.encode(listOf(SceneBookmark("scene-1", 12000, "Moment", "Private note")))
        val data = mapOf("scene_bookmarks" to mapOf("a".repeat(64) to scenes),
            "playback" to mapOf("series_${"b".repeat(64)}_audio" to "en", "series_${"b".repeat(64)}_subtitle" to "none", "autoplay_next" to false))
        assertEquals(data, BackupCodec.decode(BackupCodec.encode(data)))
        assertFalse(BackupCodec.accepts("scene_bookmarks", "a".repeat(64), "invalid notes"))
    }
    @Test fun legacyBackupVersionOneStillImports() {
        val old = "format=JadzumiBackup\nversion=1\nplayback.YmdfYXVkaW8=b:true"
        assertEquals(mapOf("playback" to mapOf("bg_audio" to true)), BackupCodec.decode(old))
    }

    @Test fun historyAndTypedSettingsRoundTrip() {
        val data = mapOf(
            "playback" to mapOf("a".repeat(64) to 12345L, "a".repeat(64)+".done" to true, "seek_seconds" to 10, "preferred_audio_lang" to "日本語", "night_mode" to false),
            "appearance" to mapOf("glassIntensity" to 0.65f, "theme" to "Dark"),
            "subtitle_prefs" to mapOf("size" to 1.25f)
        )
        assertEquals(data, BackupCodec.decode(BackupCodec.encode(data)))
    }
    @Test fun exportOmitsConsentPermissionsAndUnknownValues() {
        val backup = BackupCodec.decode(BackupCodec.encode(mapOf("playback" to mapOf("online_metadata" to true, "folder_uri" to "content://private", "secret" to "nope", "bg_audio" to true))))
        assertEquals(mapOf("bg_audio" to true), backup["playback"])
    }
    @Test fun importedConsentCannotEnableNetworkAccess() {
        val key = Base64.getUrlEncoder().withoutPadding().encodeToString("online_metadata".toByteArray())
        rejects { BackupCodec.decode("format=JadzumiBackup\nversion=1\nplayback.$key=b:true") }
    }
    @Test fun invalidFilesAndUnsafeSettingValuesAreRejected() {
        rejects { BackupCodec.decode("not a backup") }
        rejects { BackupCodec.decode("format=JadzumiBackup\nversion=99") }
        rejects { BackupCodec.decode("x".repeat(BackupCodec.MAX_BYTES + 1)) }
        val key = Base64.getUrlEncoder().withoutPadding().encodeToString("seek_seconds".toByteArray())
        rejects { BackupCodec.decode("format=JadzumiBackup\nversion=1\nplayback.$key=i:-100") }
        rejects { BackupCodec.decode("format=JadzumiBackup\nversion=1\nplayback.$key=s:wrong type") }
        assertFalse(BackupCodec.accepts("appearance", "glassIntensity", Float.NaN))
    }
}
