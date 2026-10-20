package com.jadzumi.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalGlassIntensity = compositionLocalOf { 0.75f }

/**
 * Adaptive Glassmorphic Theme Engine — Cinema Flow v1.3 Enhanced
 * Powers frosted glass depth, translucency, and specular rim borders across:
 * - Floating Bottom Navigation Dock & Tablet Navigation Rail
 * - CinemaPlayer Pills, Docks, and Skip Controls
 * - Search Capsules & Top Headers
 * - Settings Panels & Dialogs
 * Compatible across Light, Dark, and AMOLED themes.
 */
object GlassTheme {
    /**
     * Resolves the complete CinemaFlowColors token suite based on theme string and system dark mode.
     */
    fun colors(theme: String, isSystemDark: Boolean = true): CinemaFlowColors {
        return when (theme) {
            "AMOLED" -> CinemaFlowAmoledColors
            "Light" -> CinemaFlowLightColors
            "Dark" -> CinemaFlowDarkColors
            "System" -> if (isSystemDark) CinemaFlowDarkColors else CinemaFlowLightColors
            else -> CinemaFlowDarkColors
        }
    }

    /**
     * Compute background surface color based on active theme and glass intensity (0.0f - 1.0f).
     * Backwards-compatible with all existing references.
     */
    fun surfaceColor(theme: String, glassIntensity: Float): Color {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        return when (theme) {
            "AMOLED" -> {
                val alpha = (0.95f - 0.25f * intensity).coerceIn(0.65f, 0.98f)
                CinemaFlowAmoledColors.glassSurface.copy(alpha = alpha)
            }
            "Light" -> {
                val alpha = (0.90f - 0.35f * intensity).coerceIn(0.45f, 0.95f)
                CinemaFlowLightColors.glassSurface.copy(alpha = alpha)
            }
            else -> {
                val alpha = (0.88f - 0.30f * intensity).coerceIn(0.50f, 0.92f)
                CinemaFlowDarkColors.glassSurface.copy(alpha = alpha)
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
                val alpha = (0.10f + 0.20f * intensity).coerceIn(0.08f, 0.35f)
                CinemaFlowAmoledColors.glassBorder.copy(alpha = alpha)
            }
            "Light" -> {
                val alpha = (0.50f + 0.45f * intensity).coerceIn(0.40f, 0.95f)
                CinemaFlowLightColors.glassBorder.copy(alpha = alpha)
            }
            else -> {
                val alpha = (0.16f + 0.30f * intensity).coerceIn(0.12f, 0.48f)
                CinemaFlowDarkColors.glassBorder.copy(alpha = alpha)
            }
        }
    }

    /**
     * Frosted background for video overlay controls (CinemaPlayer time pills, docks, buttons).
     */
    fun playerFrostedBg(theme: String, isOled: Boolean, glassIntensity: Float): Color {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        val alpha = (0.65f - 0.35f * intensity).coerceIn(0.25f, 0.85f)
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
            Color(0xFF8AC3F7).copy(alpha = alpha)
        } else {
            Color(0xFF76B5EF).copy(alpha = alpha)
        }
    }
}
