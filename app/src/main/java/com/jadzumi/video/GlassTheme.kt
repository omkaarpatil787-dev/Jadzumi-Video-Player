package com.jadzumi.video

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalGlassIntensity = compositionLocalOf { 0.75f }
val LocalThemeName = compositionLocalOf { "Dark" }

/**
 * Adaptive Liquid Glass & Glassmorphic Theme Engine
 * Powers liquid refractive depth, specular meniscus rim borders, and adaptive translucency across:
 * - Floating Liquid Navigation Dock & Minimized Pill
 * - Adaptive Liquid Cards throughout Library, Series & Episodes
 * - CinemaPlayer Pills, Docks, and Skip Controls
 * - Search Capsules & Top Headers
 * - Settings Panels & Dialogs
 * Compatible across Light, Dark, and AMOLED themes.
 */
object GlassTheme {
    /**
     * Compute background surface color based on active theme and glass intensity (0.0f - 1.0f).
     */
    fun surfaceColor(theme: String, glassIntensity: Float): Color {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        return when (theme) {
            "AMOLED" -> {
                // Pitch black OLED energy saver at 0% scaling to deep sapphire translucent glass at 100%
                val alpha = (0.98f - 0.50f * intensity).coerceIn(0.45f, 1.0f)
                Color(0xFF040914).copy(alpha = alpha)
            }
            "Light" -> {
                // Crisp luminous white surface at 0% scaling to translucent frosted crystal at 100%
                val alpha = (0.96f - 0.52f * intensity).coerceIn(0.38f, 0.98f)
                Color.White.copy(alpha = alpha)
            }
            else -> {
                // Deep obsidian navy surface at 0% scaling to sleek frosted obsidian glass at 100%
                val alpha = (0.95f - 0.50f * intensity).coerceIn(0.42f, 0.96f)
                Color(0xFF0E1726).copy(alpha = alpha)
            }
        }
    }

    /**
     * Compute specular highlight rim border color based on active theme and glass intensity.
     */
    fun borderColor(theme: String, glassIntensity: Float): Color {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        return when (theme) {
            "AMOLED" -> {
                val alpha = (0.12f + 0.50f * intensity).coerceIn(0.10f, 0.62f)
                Color(0xFF188AF5).copy(alpha = alpha)
            }
            "Light" -> {
                val alpha = (0.32f + 0.60f * intensity).coerceIn(0.28f, 0.95f)
                Color.White.copy(alpha = alpha)
            }
            else -> {
                val alpha = (0.12f + 0.42f * intensity).coerceIn(0.10f, 0.55f)
                Color(0xFF64DCFF).copy(alpha = alpha)
            }
        }
    }

    /**
     * Liquid Glass Surface Brush: Refractive gradient capturing ambient light across the upper meniscus
     * down to deep glass body.
     */
    fun liquidSurfaceBrush(theme: String, glassIntensity: Float): Brush {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        return when (theme) {
            "AMOLED" -> {
                val alphaTop = (0.86f - 0.40f * intensity).coerceIn(0.40f, 0.92f)
                val alphaBottom = (0.98f - 0.48f * intensity).coerceIn(0.50f, 1.0f)
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0B172E).copy(alpha = alphaTop),
                        Color(0xFF040810).copy(alpha = alphaBottom)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            }
            "Light" -> {
                val alphaTop = (0.98f - 0.35f * intensity).coerceIn(0.60f, 0.98f)
                val alphaBottom = (0.92f - 0.45f * intensity).coerceIn(0.45f, 0.94f)
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alphaTop),
                        Color(0xFFE9F1FC).copy(alpha = alphaBottom)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            }
            else -> {
                val alphaTop = (0.90f - 0.40f * intensity).coerceIn(0.42f, 0.95f)
                val alphaBottom = (0.96f - 0.45f * intensity).coerceIn(0.48f, 0.98f)
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF16253D).copy(alpha = alphaTop),
                        Color(0xFF0D1726).copy(alpha = alphaBottom)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            }
        }
    }

    /**
     * Liquid Glass Border Meniscus Brush: Specular directional light catching the top rim
     * and fading to subtle glass occlusion at the base.
     */
    fun liquidBorderBrush(theme: String, glassIntensity: Float): Brush {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        val topAlpha = (0.35f + 0.50f * intensity).coerceIn(0.30f, 0.92f)
        val bottomAlpha = (0.10f + 0.25f * intensity).coerceIn(0.08f, 0.45f)

        val topColor = when (theme) {
            "AMOLED" -> Color(0xFF38BDF8)
            "Light" -> Color.White
            else -> Color(0xFF64DCFF)
        }
        val bottomColor = when (theme) {
            "AMOLED" -> Color(0xFF0A192F)
            "Light" -> Color(0xFFB0CDEE)
            else -> Color(0xFF1E293B)
        }

        return Brush.verticalGradient(
            colors = listOf(
                topColor.copy(alpha = topAlpha),
                bottomColor.copy(alpha = bottomAlpha)
            )
        )
    }

    /**
     * Ready-to-use liquid glass border stroke for surfaces, panels, and floating navigation bars.
     */
    fun liquidBorderStroke(theme: String, glassIntensity: Float, strokeWidth: Dp = 1.2.dp): BorderStroke {
        return BorderStroke(strokeWidth, liquidBorderBrush(theme, glassIntensity))
    }

    /**
     * Refined liquid glass card brush for movie, series, and episode cards.
     */
    fun liquidCardBrush(theme: String, glassIntensity: Float): Brush {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        return when (theme) {
            "AMOLED" -> {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1628).copy(alpha = (0.75f - 0.35f * intensity).coerceIn(0.35f, 0.85f)),
                        Color(0xFF02060E).copy(alpha = (0.95f - 0.40f * intensity).coerceIn(0.50f, 1.0f))
                    )
                )
            }
            "Light" -> {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (0.98f - 0.35f * intensity).coerceIn(0.60f, 0.98f)),
                        Color(0xFFF1F6FD).copy(alpha = (0.92f - 0.40f * intensity).coerceIn(0.50f, 0.95f))
                    )
                )
            }
            else -> {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF18263E).copy(alpha = (0.85f - 0.38f * intensity).coerceIn(0.40f, 0.92f)),
                        Color(0xFF0F1B2D).copy(alpha = (0.95f - 0.42f * intensity).coerceIn(0.50f, 0.98f))
                    )
                )
            }
        }
    }

    /**
     * Frosted background for video overlay controls (CinemaPlayer time pills, docks, buttons).
     */
    fun playerFrostedBg(theme: String, isOled: Boolean, glassIntensity: Float): Color {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        val alpha = (0.75f - 0.50f * intensity).coerceIn(0.18f, 0.85f)
        return if (isOled || theme == "AMOLED") {
            Color(0xFF040A14).copy(alpha = alpha)
        } else {
            Color(0xFF0D1624).copy(alpha = alpha)
        }
    }

    /**
     * Specular rim highlight for video overlay controls.
     */
    fun playerFrostedRim(theme: String, isOled: Boolean, glassIntensity: Float): Color {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        val alpha = (0.18f + 0.35f * intensity).coerceIn(0.14f, 0.60f)
        return if (isOled || theme == "AMOLED") {
            Color(0xFF188AF5).copy(alpha = alpha)
        } else {
            Color.White.copy(alpha = alpha)
        }
    }
}
