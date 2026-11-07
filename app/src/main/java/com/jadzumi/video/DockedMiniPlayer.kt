package com.jadzumi.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * In-App Docked Mini Player (YouTube / Spotify Style).
 *
 * Allows uninterrupted video listening/watching while browsing the library, search, or settings.
 * Swiping down or pressing minimize in [CinemaPlayer] docks the video into this floating bar.
 */
@Composable
fun DockedMiniPlayer(
    playback: PlaybackController,
    theme: String,
    modifier: Modifier = Modifier
) {
    val video = playback.video ?: return
    if (!playback.isDocked) return

    val isLight = theme == "Light"
    val cardBg = if (isLight) Color(0xF2FFFFFF) else Color(0xEB131E2E)
    val cardBorder = if (isLight) Color(0x330061A4) else Color(0x4082B1FF)
    val textColor = if (isLight) Color(0xFF0F1E2E) else Color.White
    val subtextColor = if (isLight) Color(0xFF556980) else Color(0xFF94A3B8)
    val accentColor = if (isLight) Color(0xFF0061A4) else Color(0xFF82B1FF)

    val progress = if (playback.duration > 0) {
        (playback.position.toFloat() / playback.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    AnimatedVisibility(
        visible = playback.isDocked,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clickable { playback.undock() }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {},
                        onDrag = { change, dragAmount ->
                            if (dragAmount.y > 15f) {
                                change.consume()
                                playback.close()
                            }
                        }
                    )
                }
        ) {
            Column {
                // Sleek Top Linear Progress Indicator
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = accentColor,
                    trackColor = cardBorder.copy(alpha = 0.2f),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mini Video Glyph / Indicator
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playback.playing) Icons.Rounded.Movie else Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // Video Title and Duration
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                    ) {
                        Text(
                            text = video.title.ifBlank { video.name },
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        val elapsed = formatTimestamp(playback.position)
                        val total = formatTimestamp(playback.duration.takeIf { it > 0 } ?: video.duration)
                        Text(
                            text = "$elapsed / $total",
                            color = subtextColor,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    // Jump Forward +10s
                    IconButton(
                        onClick = { playback.skip(10_000L) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = "Forward 10s",
                            tint = textColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play / Pause
                    IconButton(
                        onClick = { playback.toggle() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(accentColor.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (playback.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playback.playing) "Pause" else "Play",
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(2.dp))

                    // Close (Dismiss) Button
                    IconButton(
                        onClick = { playback.close() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = subtextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val totalSec = (millis / 1000).coerceAtLeast(0)
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}
