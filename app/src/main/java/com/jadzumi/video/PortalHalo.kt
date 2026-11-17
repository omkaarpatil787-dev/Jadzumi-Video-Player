package com.jadzumi.video

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Jadzumi Portal Halo Visual & Motion System (v1.7.0)
 *
 * Provides soft radial-gradient atmospheric backdrop illumination inspired by
 * the approved jadzumi-halo-explorer specification.
 *
 * Features:
 * - 3 Curated Palettes: Glacier Blue, Peach Dusk, Violet Cinema
 * - Adaptive brightness scaling for Light, Dark, and AMOLED (pitch black preservation)
 * - Pure GPU radial brush rendering with zero expensive full-screen blurs
 * - Organic 10-second idle breathing drift when motion physics are active
 */
enum class HaloPalette(
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    GlacierBlue(
        title = "Glacier Blue",
        subtitle = "Sky Blue & Lavender",
        primaryColor = Color(0xFF84BAFF),
        secondaryColor = Color(0xFFB39CE8)
    ),
    PeachDusk(
        title = "Peach Dusk",
        subtitle = "Warm Apricot & Orchid",
        primaryColor = Color(0xFFFFC49F),
        secondaryColor = Color(0xFFDF9AC4)
    ),
    VioletCinema(
        title = "Violet Cinema",
        subtitle = "Royal Violet & Aqua Teal",
        primaryColor = Color(0xFFAC9AFF),
        secondaryColor = Color(0xFF76CDDD)
    ),
    CinemaNoir(
        title = "Cinema Noir",
        subtitle = "Studio Shadow & Micro-Rim",
        primaryColor = Color(0xFF2E2744),
        secondaryColor = Color(0xFF1B1728)
    );

    companion object {
        fun fromName(name: String?): HaloPalette = when (name) {
            "Peach Dusk", "peach", "Peach" -> PeachDusk
            "Violet Cinema", "violet", "Violet" -> VioletCinema
            "Cinema Noir", "noir", "Noir" -> CinemaNoir
            else -> GlacierBlue
        }
    }
}

/**
 * Direction A: Adaptive Cinematic Atmospheric Canvas.
 * A calm, refined, non-distracting atmospheric canvas:
 * - Light: Clean architectural paper (#F8F7FC) with subtle top micro-vignette.
 * - Dark: Deep obsidian (#0E0C15) with subtle top-to-bottom depth falloff.
 * - AMOLED: Pure #000000 black for infinite contrast and power saving.
 */
@Composable
fun AdaptiveAtmosphericCanvas(
    theme: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        when (theme) {
            "AMOLED" -> {
                drawRect(color = Color.Black)
            }
            "Light" -> {
                // Architectural paper base
                drawRect(color = Color(0xFFF8F7FC))
                // Subtle top elevation vignette
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEDEBF5).copy(alpha = 0.5f),
                            Color(0xFFF8F7FC).copy(alpha = 0f)
                        ),
                        startY = 0f,
                        endY = size.height * 0.35f
                    )
                )
                // Soft corner vignette for depth
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE4E1EF).copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.95f, size.height * 0.05f),
                        radius = size.width * 0.55f
                    )
                )
            }
            else -> { // Dark
                // Deep obsidian base
                drawRect(color = Color(0xFF0E0C15))
                // Subtle depth falloff from top
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF181524),
                            Color(0xFF0E0C15)
                        ),
                        startY = 0f,
                        endY = size.height * 0.45f
                    )
                )
                // Gentle horizon micro-ambient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF261F38).copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, 0f),
                        radius = size.width * 0.75f
                    )
                )
            }
        }
    }
}

/**
 * Atmospheric Portal Halo Backdrop Composable (Redirected to Direction A Adaptive Atmospheric Canvas).
 */
@Composable
fun PortalHaloBackdrop(
    theme: String,
    palette: HaloPalette = HaloPalette.GlacierBlue,
    enabled: Boolean = true,
    intensity: Float = 0.30f,
    spread: Float = 0.70f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    AdaptiveAtmosphericCanvas(theme = theme, modifier = modifier)
}

/**
 * Dedicated Portal Halo Settings Card for Display & Appearance preferences.
 */
@Composable
fun HaloSettingsCard(
    theme: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    palette: HaloPalette,
    onPaletteChange: (HaloPalette) -> Unit,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    spread: Float,
    onSpreadChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialExpressive.cardBg(theme),
        border = MaterialExpressive.cardBorder(theme),
        shadowElevation = MaterialExpressive.cardElevation(theme)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Portal Halo Backdrop",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Atmospheric radial aura backdrops behind app chrome and library cards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            if (enabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Palette Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Halo Palette",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HaloPalette.values().forEach { p ->
                            val isSelected = palette == p
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPaletteChange(p) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Dual color dots representing primary and secondary halo colors
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(p.primaryColor)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(p.secondaryColor)
                                        )
                                    }
                                    Text(
                                        p.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Halo Glow Intensity Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Halo Glow Intensity",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                "${(intensity * 100).toInt()}%",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Slider(
                        value = intensity,
                        onValueChange = onIntensityChange,
                        valueRange = 0.05f..0.65f,
                        steps = 11,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtle 10%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Balanced 30%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Luminous 65%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Halo Spread Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Halo Spread Radius",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                "${(spread * 100).toInt()}%",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Slider(
                        value = spread,
                        onValueChange = onSpreadChange,
                        valueRange = 0.30f..1.20f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Compact 30%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Standard 70%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Expansive 120%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
