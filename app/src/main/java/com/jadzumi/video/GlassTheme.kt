package com.jadzumi.video

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalGlassIntensity = compositionLocalOf { 0.75f }

/**
 * Adaptive Glassmorphic Theme Engine
 * Powers frosted glass depth, translucency, and specular rim borders across:
 * - Floating Bottom Navigation Dock & Tablet Navigation Rail
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
