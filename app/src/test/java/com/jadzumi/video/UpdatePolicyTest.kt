package com.jadzumi.video

import org.junit.Assert.*
import org.junit.Test

class UpdatePolicyTest {
    @Test fun stableOnlyAndOrderedNumerically() {
        assertTrue(newerStableRelease("v1.10.0", "1.9.9"))
        assertTrue(newerStableRelease("v1.4.0", "1.4.0-alpha.3"))
        assertFalse(newerStableRelease("v1.3.3", "1.4.0-alpha.3"))
        assertFalse(newerStableRelease("1.4.0", "1.4.0"))
        assertFalse(newerStableRelease("v1.5.0-beta.1", "1.4.0"))
        assertFalse(newerStableRelease("malformed", "1.4.0"))
    }
    @Test fun installRequiresMatchingIdentityAndIncreasingBuild() {
        val current = UpdateIdentity("com.jadzumi.video", "1.4.0", 20, setOf("production"))
        val next = current.copy(versionName = "1.4.1", versionCode = 21)
        requireUpdateIdentity(next, current, "v1.4.1")
        for (bad in listOf(next.copy(packageName = "other"), next.copy(signers = setOf("debug")),
            next.copy(signers = emptySet()), next.copy(versionCode = 20), next.copy(versionName = "1.4.2"))) {
            try { requireUpdateIdentity(bad, current, "v1.4.1"); fail("Invalid APK identity accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun updateConsentCannotBeImportedFromBackup() {
        assertFalse(BackupCodec.accepts("updates", "automatic", true))
        assertFalse(BackupCodec.accepts("playback", "automatic", true))
        assertTrue(BackupCodec.accepts("playback", "match_refresh_rate", true))
    }
}
