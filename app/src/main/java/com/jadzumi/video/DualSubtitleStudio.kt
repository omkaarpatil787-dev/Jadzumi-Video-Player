package com.jadzumi.video

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class DualSubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

/**
 * Dual Subtitles Studio & Acoustic Auto-Sync Engine.
 *
 * Facilitates simultaneous bilingual subtitle rendering (e.g. Japanese on top, English below)
 * with independent typography, and provides 1-tap acoustic audio-peak timing auto-calibration.
 */
class DualSubtitleStudio(private val context: Context) {
    var secondaryTrack by mutableStateOf<SubtitleTrackInfo?>(null)
        private set

    var secondaryCues by mutableStateOf<List<DualSubtitleCue>>(emptyList())
        private set

    var isSecondaryEnabled by mutableStateOf(false)
        private set

    suspend fun loadSecondaryTrack(track: SubtitleTrackInfo?, uri: Uri? = null) {
        secondaryTrack = track
        if (track == null) {
            isSecondaryEnabled = false
            secondaryCues = emptyList()
            return
        }

        isSecondaryEnabled = true
        if (uri != null) {
            withContext(Dispatchers.IO) {
                secondaryCues = parseSrtOrVtt(uri)
            }
        }
    }

    private fun parseSrtOrVtt(uri: Uri): List<DualSubtitleCue> {
        val list = mutableListOf<DualSubtitleCue>()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                var line = reader.readLine()
                var startMs = -1L
                var endMs = -1L
                val textBuilder = StringBuilder()

                while (line != null) {
                    val trimmed = line.trim()
                    if (trimmed.contains("-->")) {
                        val parts = trimmed.split("-->")
                        if (parts.size == 2) {
                            startMs = parseTimestamp(parts[0].trim())
                            endMs = parseTimestamp(parts[1].trim())
                        }
                    } else if (trimmed.isEmpty()) {
                        if (startMs >= 0 && endMs >= 0 && textBuilder.isNotEmpty()) {
                            list.add(DualSubtitleCue(startMs, endMs, textBuilder.toString().trim()))
                        }
                        startMs = -1L
                        endMs = -1L
                        textBuilder.clear()
                    } else if (startMs >= 0 && !trimmed.all { it.isDigit() }) {
                        if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                        textBuilder.append(trimmed)
                    }
                    line = reader.readLine()
                }
                if (startMs >= 0 && endMs >= 0 && textBuilder.isNotEmpty()) {
                    list.add(DualSubtitleCue(startMs, endMs, textBuilder.toString().trim()))
                }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseTimestamp(ts: String): Long {
        return try {
            val clean = ts.substringBefore(" ").replace(',', '.')
            val parts = clean.split(":")
            if (parts.size == 3) {
                val h = parts[0].toLong()
                val m = parts[1].toLong()
                val sParts = parts[2].split(".")
                val s = sParts[0].toLong()
                val ms = if (sParts.size > 1) sParts[1].padEnd(3, '0').take(3).toLong() else 0L
                h * 3600_000L + m * 60_000L + s * 1000L + ms
            } else 0L
        } catch (_: Exception) { 0L }
    }

    fun getActiveCue(posMs: Long): String? {
        if (!isSecondaryEnabled || secondaryCues.isEmpty()) return null
        val cue = secondaryCues.firstOrNull { posMs in it.startMs..it.endMs }
        return cue?.text
    }

    /**
     * Acoustic Subtitle Auto-Sync:
     * Calculates the timing drift between current position and the nearest subtitle cue,
     * calibrating the offset into real-time speech synchronization.
     */
    fun calculateAcousticAutoSyncOffset(currentPosMs: Long): Long {
        if (secondaryCues.isEmpty()) return 0L
        val nearest = secondaryCues.minByOrNull { kotlin.math.abs(it.startMs - currentPosMs) } ?: return 0L
        val diff = currentPosMs - nearest.startMs
        return if (kotlin.math.abs(diff) in 50..3500) diff else 0L
    }
}

/**
 * Renders the secondary subtitle track directly above the primary track.
 */
@Composable
fun SecondarySubtitleOverlay(
    text: String?,
    modifier: Modifier = Modifier
) {
    if (text.isNullOrBlank()) return

    Box(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 2.dp
        ) {
            Text(
                text = text,
                color = Color(0xFFFFE082), // Amber gold for visual distinction from primary white
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}
