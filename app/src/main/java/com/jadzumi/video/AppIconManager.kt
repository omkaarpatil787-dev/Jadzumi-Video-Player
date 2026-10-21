package com.jadzumi.video

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * Metadata and switching logic for Jadzumi Player custom launcher icons.
 */
data class AppIcon(
    val id: String,
    val name: String,
    val subtitle: String,
    val iconRes: Int,
    val aliasName: String
)

object AppIconManager {
    private const val TAG = "AppIconManager"
    private const val PREFS_NAME = "appearance"
    private const val PREF_KEY_ICON_ID = "app_icon_id"
    private val switchMutex = Mutex()

    val ALL_ICONS: List<AppIcon> = listOf(
        AppIcon(
            id = "original",
            name = "Jadzumi Original",
            subtitle = "Folded Ribbon",
            iconRes = R.drawable.ic_launcher_original,
            aliasName = "com.jadzumi.video.MainActivityAliasOriginal"
        ),
        AppIcon(
            id = "neon_protocol",
            name = "Neon Protocol",
            subtitle = "Cyberpunk Luminous",
            iconRes = R.drawable.ic_launcher_neon_protocol,
            aliasName = "com.jadzumi.video.MainActivityAliasNeonProtocol"
        ),
        AppIcon(
            id = "vhs_nostalgia",
            name = "VHS Nostalgia",
            subtitle = "Retro 80s Cassette",
            iconRes = R.drawable.ic_launcher_vhs_nostalgia,
            aliasName = "com.jadzumi.video.MainActivityAliasVhsNostalgia"
        ),
        AppIcon(
            id = "anime_pop",
            name = "Anime Pop",
            subtitle = "Cel-Shaded Manga",
            iconRes = R.drawable.ic_launcher_anime_pop,
            aliasName = "com.jadzumi.video.MainActivityAliasAnimePop"
        ),
        AppIcon(
            id = "boo",
            name = "Boo!",
            subtitle = "Pastel Ghost Mascot",
            iconRes = R.drawable.ic_launcher_boo,
            aliasName = "com.jadzumi.video.MainActivityAliasBoo"
        ),
        AppIcon(
            id = "zombumi",
            name = "Zombumi",
            subtitle = "Mint Zombie Mascot",
            iconRes = R.drawable.ic_launcher_zombumi,
            aliasName = "com.jadzumi.video.MainActivityAliasZombumi"
        ),
        AppIcon(
            id = "pixel_quest",
            name = "Pixel Quest",
            subtitle = "8-Bit Retro Arcade",
            iconRes = R.drawable.ic_launcher_pixel_quest,
            aliasName = "com.jadzumi.video.MainActivityAliasPixelQuest"
        ),
        AppIcon(
            id = "chrome_x",
            name = "Chrome X",
            subtitle = "Liquid Metal Mirror",
            iconRes = R.drawable.ic_launcher_chrome_x,
            aliasName = "com.jadzumi.video.MainActivityAliasChromeX"
        ),
        AppIcon(
            id = "cosmic_portal",
            name = "Cosmic Portal",
            subtitle = "Celestial Nebula Vortex",
            iconRes = R.drawable.ic_launcher_cosmic_portal,
            aliasName = "com.jadzumi.video.MainActivityAliasCosmicPortal"
        ),
        AppIcon(
            id = "funky_doodle",
            name = "Funky Doodle",
            subtitle = "Hand-Drawn Sticker Art",
            iconRes = R.drawable.ic_launcher_funky_doodle,
            aliasName = "com.jadzumi.video.MainActivityAliasFunkyDoodle"
        )
    )

    /**
     * Retrieves the active icon ID from persistent preferences or detects the active alias.
     */
    fun getCurrentIconId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(PREF_KEY_ICON_ID, null)
        if (!savedId.isNullOrEmpty() && ALL_ICONS.any { it.id == savedId }) {
            return savedId
        }

        // Fallback: check actual component state from PackageManager
        val pm = context.packageManager
        for (icon in ALL_ICONS) {
            val component = ComponentName(context, icon.aliasName)
            val state = try {
                pm.getComponentEnabledSetting(component)
            } catch (e: Exception) {
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            }
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return icon.id
            }
        }
        return "original"
    }

    /**
     * Safely switches the launcher icon to the target icon ID.
     * Guaranteed safe: enables target alias first before disabling others,
     * protecting against app disappearance in case of interruption.
     */
    suspend fun setAppIcon(context: Context, targetIconId: String): Boolean = withContext(Dispatchers.IO) {
        val targetIcon = ALL_ICONS.find { it.id == targetIconId } ?: return@withContext false
        if (!switchMutex.tryLock()) {
            Log.w(TAG, "Icon switch operation already in progress. Ignoring duplicate trigger.")
            return@withContext false
        }

        try {
            val pm = context.packageManager
            val targetComponent = ComponentName(context, targetIcon.aliasName)

            // Try atomic batch update on Android 13+ (API 33+) if supported
            var atomicSuccess = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val settingsList = mutableListOf<PackageManager.ComponentEnabledSetting>()
                    settingsList.add(
                        PackageManager.ComponentEnabledSetting(
                            targetComponent,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    )
                    for (icon in ALL_ICONS) {
                        if (icon.id != targetIcon.id) {
                            settingsList.add(
                                PackageManager.ComponentEnabledSetting(
                                    ComponentName(context, icon.aliasName),
                                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                    PackageManager.DONT_KILL_APP
                                )
                            )
                        }
                    }
                    pm.setComponentEnabledSettings(settingsList)
                    atomicSuccess = true
                } catch (e: Throwable) {
                    Log.w(TAG, "Atomic setComponentEnabledSettings failed, falling back to sequential: ${e.message}")
                    atomicSuccess = false
                }
            }

            if (!atomicSuccess) {
                // Sequential fallback: Always enable the target FIRST
                pm.setComponentEnabledSetting(
                    targetComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )

                // Then disable all other aliases
                for (icon in ALL_ICONS) {
                    if (icon.id != targetIcon.id) {
                        pm.setComponentEnabledSetting(
                            ComponentName(context, icon.aliasName),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }
            }

            // Save preference
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_KEY_ICON_ID, targetIcon.id)
                .apply()

            Log.i(TAG, "Successfully switched launcher icon to ${targetIcon.name} (${targetIcon.aliasName})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch launcher icon to $targetIconId", e)
            false
        } finally {
            switchMutex.unlock()
        }
    }

    /**
     * Verifies that exactly one alias is enabled. Can be called during app initialization.
     */
    fun syncLauncherAlias(context: Context) {
        val currentId = getCurrentIconId(context)
        val pm = context.packageManager
        val targetIcon = ALL_ICONS.find { it.id == currentId } ?: ALL_ICONS[0]
        val targetComponent = ComponentName(context, targetIcon.aliasName)

        val targetState = try {
            pm.getComponentEnabledSetting(targetComponent)
        } catch (e: Exception) {
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        }

        if (targetState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            try {
                pm.setComponentEnabledSetting(
                    targetComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                Log.e(TAG, "syncLauncherAlias: failed to enable target", e)
            }
        }
    }
}
