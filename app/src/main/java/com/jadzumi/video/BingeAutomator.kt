package com.jadzumi.video

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

data class SeriesBingeSettings(
    val seriesKey: String,
    val introSkipSeconds: Int = 85,
    val outroSkipSeconds: Int = 60,
    val autoSkipIntro: Boolean = false,
    val autoSkipOutro: Boolean = false
)

object BingeManager {
    private const val PREFS_NAME = "jadzumi_binge_settings"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSettings(context: Context, seriesKey: String): SeriesBingeSettings {
        val prefs = getPrefs(context)
        val normalized = seriesKey.trim().lowercase()
        return SeriesBingeSettings(
            seriesKey = normalized,
            introSkipSeconds = prefs.getInt("${normalized}_intro", 85),
            outroSkipSeconds = prefs.getInt("${normalized}_outro", 60),
            autoSkipIntro = prefs.getBoolean("${normalized}_auto_intro", false),
            autoSkipOutro = prefs.getBoolean("${normalized}_auto_outro", false)
        )
    }

    fun saveSettings(context: Context, settings: SeriesBingeSettings) {
        val prefs = getPrefs(context)
        val normalized = settings.seriesKey.trim().lowercase()
        prefs.edit {
            putInt("${normalized}_intro", settings.introSkipSeconds)
            putInt("${normalized}_outro", settings.outroSkipSeconds)
            putBoolean("${normalized}_auto_intro", settings.autoSkipIntro)
            putBoolean("${normalized}_auto_outro", settings.autoSkipOutro)
        }
    }
}

@Composable
fun BingeSettingsDialog(
    seriesName: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var settings by remember(seriesName) {
        mutableStateOf(BingeManager.getSettings(context, seriesName))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.FastForward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text("Binge Automation", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Customize automatic intro & outro skipping specifically for '$seriesName'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // Intro Auto Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto-Skip Intro", fontWeight = FontWeight.SemiBold)
                        Text("Jumps ahead ${settings.introSkipSeconds}s on episode start", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.autoSkipIntro,
                        onCheckedChange = {
                            settings = settings.copy(autoSkipIntro = it)
                            BingeManager.saveSettings(context, settings)
                        }
                    )
                }

                if (settings.autoSkipIntro) {
                    Column {
                        Text("Intro Length: ${settings.introSkipSeconds} seconds", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = settings.introSkipSeconds.toFloat(),
                            onValueChange = {
                                settings = settings.copy(introSkipSeconds = it.toInt())
                                BingeManager.saveSettings(context, settings)
                            },
                            valueRange = 10f..180f,
                            steps = 16
                        )
                    }
                }

                HorizontalDivider()

                // Outro Auto Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto-Skip Credits & Next", fontWeight = FontWeight.SemiBold)
                        Text("Starts next episode when ${settings.outroSkipSeconds}s left", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.autoSkipOutro,
                        onCheckedChange = {
                            settings = settings.copy(autoSkipOutro = it)
                            BingeManager.saveSettings(context, settings)
                        }
                    )
                }

                if (settings.autoSkipOutro) {
                    Column {
                        Text("Credits Window: ${settings.outroSkipSeconds} seconds", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = settings.outroSkipSeconds.toFloat(),
                            onValueChange = {
                                settings = settings.copy(outroSkipSeconds = it.toInt())
                                BingeManager.saveSettings(context, settings)
                            },
                            valueRange = 15f..180f,
                            steps = 10
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
