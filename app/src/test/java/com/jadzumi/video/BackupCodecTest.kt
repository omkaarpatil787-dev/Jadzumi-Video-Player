package com.jadzumi.video

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class BackupCodecTest {
    private fun rejects(block: () -> Unit) {
        try { block(); fail("Expected invalid backup to be rejected") } catch (_: IllegalArgumentException) {} catch (_: IllegalStateException) {}
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
