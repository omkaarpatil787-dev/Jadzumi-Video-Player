package com.jadzumi.video

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

data class PersonaStat(val label: String, val value: String, val subtext: String)

data class CinemaPersona(
    val archetypeTitle: String,
    val tagLine: String,
    val badgeEmoji: String,
    val description: String,
    val stats: List<PersonaStat>,
    val gradientColors: List<Color>
)

object CinemaDnaEngine {

    fun generatePersona(
        videos: List<LocalVideo>,
        playback: PlaybackController
    ): CinemaPersona {
        var totalWatchedSeconds = 0L
        var completedCount = 0
        var uhdCount = 0
        var episodeCount = 0

        videos.forEach { v ->
            val pos = playback.savedPosition(v) / 1000
            totalWatchedSeconds += pos
            if (playback.isWatched(v)) completedCount++
            if (v.width >= 3840 || v.height >= 2160 || v.name.contains("4k", true) || v.name.contains("2160", true)) {
                uhdCount++
            }
            if (v.episode != null) episodeCount++
        }

        val totalHours = (totalWatchedSeconds / 3600.0).coerceAtLeast(0.1)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNightTime = currentHour in 22..23 || currentHour in 0..4
        val speed = playback.speed

        return when {
            speed >= 1.25f -> CinemaPersona(
                archetypeTitle = "The Hyper-Velocity Scholar",
                tagLine = "Life is short; narrative arcs are long.",
                badgeEmoji = "⚡",
                description = "You consume cinema at superhuman velocity. Your brain processes subtitles and plot twists at high frame rates.",
                stats = listOf(
                    PersonaStat("Cinema Mileage", String.format(java.util.Locale.US, "%.1f Hours", totalHours), "Total canvas watch time"),
                    PersonaStat("Pace Multiplier", "${speed}x Speed", "Active playback tempo"),
                    PersonaStat("Conquered Media", "$completedCount Titles", "Completed files"),
                    PersonaStat("Library Scale", "${videos.size} Videos", "Total indexed collection")
                ),
                gradientColors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
            )

            uhdCount >= 3 || (videos.isNotEmpty() && uhdCount.toDouble() / videos.size >= 0.3) -> CinemaPersona(
                archetypeTitle = "The 4K HDR Purist",
                tagLine = "Never compromise on bitrates or color gamut.",
                badgeEmoji = "🎬",
                description = "You demand maximum visual fidelity. True master color profiles, wide dynamics, and pristine pixel precision are your standard.",
                stats = listOf(
                    PersonaStat("Cinema Mileage", String.format(java.util.Locale.US, "%.1f Hours", totalHours), "Total canvas watch time"),
                    PersonaStat("UHD Ratio", "$uhdCount 4K Master Rips", "High-bitrate cinema files"),
                    PersonaStat("Completed", "$completedCount Titles", "Fully watched"),
                    PersonaStat("Color Profile", playback.videoEnhancerProfile.displayName.substringBefore(" ("), "Active GPU shader")
                ),
                gradientColors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
            )

            isNightTime -> CinemaPersona(
                archetypeTitle = "The Midnight Binge Architect",
                tagLine = "Sleep is temporary; cliffhangers are forever.",
                badgeEmoji = "🌙",
                description = "Your peak cinema hours happen under starry skies. Midnight marathons and late-night TV series binges are your sanctuary.",
                stats = listOf(
                    PersonaStat("Cinema Mileage", String.format(java.util.Locale.US, "%.1f Hours", totalHours), "Total canvas watch time"),
                    PersonaStat("Golden Hour", "${currentHour}:00 Peak", "Active midnight window"),
                    PersonaStat("Series Binge", "$episodeCount Episodes", "Clustered TV installments"),
                    PersonaStat("Completed", "$completedCount Titles", "Conquered stories")
                ),
                gradientColors = listOf(Color(0xFF2E0854), Color(0xFF180B22))
            )

            episodeCount >= 10 -> CinemaPersona(
                archetypeTitle = "The Franchise Marathoner",
                tagLine = "Just one more episode...",
                badgeEmoji = "🍿",
                description = "You dive headfirst into sprawling fictional universes, devouring entire seasons in epic marathons.",
                stats = listOf(
                    PersonaStat("Cinema Mileage", String.format(java.util.Locale.US, "%.1f Hours", totalHours), "Total canvas watch time"),
                    PersonaStat("TV Episodes", "$episodeCount Episodes", "Indexed installments"),
                    PersonaStat("Watched", "$completedCount Completed", "Finished files"),
                    PersonaStat("Library Scale", "${videos.size} Videos", "Total media pool")
                ),
                gradientColors = listOf(Color(0xFFFF512F), Color(0xFFDD2476))
            )

            else -> CinemaPersona(
                archetypeTitle = "The Authentic Cinephile",
                tagLine = "Dedicated to the art of offline visual storytelling.",
                badgeEmoji = "✨",
                description = "You appreciate cinema in its purest offline form — private, zero-tracking, hardware-accelerated, and unhurried.",
                stats = listOf(
                    PersonaStat("Cinema Mileage", String.format(java.util.Locale.US, "%.1f Hours", totalHours), "Total canvas watch time"),
                    PersonaStat("Conquered Titles", "$completedCount Titles", "Finished files"),
                    PersonaStat("Collection Size", "${videos.size} Videos", "Offline index"),
                    PersonaStat("Cinema Speed", "${speed}x", "Natural pace")
                ),
                gradientColors = listOf(Color(0xFF11998E), Color(0xFF38EF7D))
            )
        }
    }
}

@Composable
fun CinemaDnaSheet(
    videos: List<LocalVideo>,
    playback: PlaybackController,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val persona = remember(videos, playback.historyVersion) {
        CinemaDnaEngine.generatePersona(videos, playback)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(persona.badgeEmoji, fontSize = 20.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Cinema DNA & Persona",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Offline viewing intelligence & viewing archetypes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Holographic Frosted Persona Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(persona.gradientColors)
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "OFFICIAL JADZUMI ARCHETYPE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.75f),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        persona.archetypeTitle,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        "\"${persona.tagLine}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.90f),
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.20f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(persona.badgeEmoji, fontSize = 26.sp)
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(
                                persona.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.92f),
                                lineHeight = 20.sp
                            )

                            Spacer(Modifier.height(20.dp))

                            // 4 Key Stats Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                persona.stats.take(2).forEach { stat ->
                                    StatChip(stat, Modifier.weight(1f))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                persona.stats.drop(2).take(2).forEach { stat ->
                                    StatChip(stat, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Privacy Commitment Note
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Security,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "100% Private & Device-Only: Cinema DNA is computed entirely from your local SQLite history. Zero tracking, zero telemetry, zero cloud logging.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Share Button
                Button(
                    onClick = {
                        val shareText = "🎬 My Jadzumi Cinema Persona: ${persona.archetypeTitle} ${persona.badgeEmoji}\n\n" +
                                "\"${persona.tagLine}\"\n\n" +
                                persona.stats.joinToString("\n") { "• ${it.label}: ${it.value}" } +
                                "\n\n100% Offline-First Cinema Player • Jadzumi v1.9"

                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Cinema Persona"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Cinema Persona")
                }
            }
        }
    }
}

@Composable
private fun StatChip(stat: PersonaStat, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
        modifier = modifier
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(stat.label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.70f))
            Text(stat.value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            Text(stat.subtext, fontSize = 9.sp, color = Color.White.copy(alpha = 0.60f), maxLines = 1)
        }
    }
}
