package com.jadzumi.video

import org.junit.Assert.*
import org.junit.Test

class AppIconTest {
    @Test
    fun appIconListContainsExactlyTenProfiles() {
        assertEquals(10, AppIconManager.ALL_ICONS.size)
    }

    @Test
    fun appIconProfilesHaveUniqueIdsAndAliases() {
        val ids = AppIconManager.ALL_ICONS.map { it.id }
        assertEquals(10, ids.toSet().size)

        val aliases = AppIconManager.ALL_ICONS.map { it.aliasName }
        assertEquals(10, aliases.toSet().size)

        aliases.forEach { alias ->
            assertTrue(alias.startsWith("com.jadzumi.video.MainActivityAlias"))
        }
    }

    @Test
    fun defaultIconIsOriginal() {
        val first = AppIconManager.ALL_ICONS.first()
        assertEquals("original", first.id)
        assertEquals("Jadzumi Original", first.name)
        assertEquals("com.jadzumi.video.MainActivityAliasOriginal", first.aliasName)
    }

    @Test
    fun allExpectedIconIdsArePresent() {
        val expectedIds = listOf(
            "original",
            "neon_protocol",
            "vhs_nostalgia",
            "anime_pop",
            "boo",
            "zombumi",
            "pixel_quest",
            "chrome_x",
            "cosmic_portal",
            "funky_doodle"
        )
        val actualIds = AppIconManager.ALL_ICONS.map { it.id }
        assertEquals(expectedIds, actualIds)
    }
}
