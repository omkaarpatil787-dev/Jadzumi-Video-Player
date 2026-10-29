package com.jadzumi.video

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalGlassIntensity = compositionLocalOf { 0.75f }
val LocalThemeName = compositionLocalOf { "Dark" }

/**
 * Adaptive Frosted Glass & Embossed Depth Theme Engine
 * Powers frosted glass depth, translucency, and subtle embossed beveled borders across:
 * - Floating Frosted Bottom Navigation Dock & Minimized Pill
 * - Adaptive Embossed Cards throughout Library, Movies, Series & Episodes
 * - CinemaPlayer Pills, Docks, and Skip Controls
 * - Search Capsules & Top Headers
 * - Settings Panels & Dialogs
 * Compatible across Light, Dark, and AMOLED themes.
 */
object GlassTheme {
    /**
     * Compute authentic frosted surface color based on active theme and glass intensity (0.0f - 1.0f).
     * Provides subtle translucent frosted glass for floating docks, overlay pills, and search capsules.
     */
    fun frostedSurfaceColor(theme: String, glassIntensity: Float): Color {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        return when (theme) {
            "AMOLED" -> {
                val alpha = (0.96f - 0.20f * intensity).coerceIn(0.72f, 0.98f)
                Color(0xFF040914).copy(alpha = alpha)
            }
            "Light" -> {
                val alpha = (0.95f - 0.18f * intensity).coerceIn(0.75f, 0.98f)
                Color.White.copy(alpha = alpha)
            }
            else -> {
                val alpha = (0.94f - 0.20f * intensity).coerceIn(0.72f, 0.96f)
                Color(0xFF0E1726).copy(alpha = alpha)
            }
        }
    }

    /**
     * Backwards-compatible alias for surfaceColor.
     */
    fun surfaceColor(theme: String, glassIntensity: Float): Color = frostedSurfaceColor(theme, glassIntensity)

    /**
     * Compute subtle frosted specular border rim color.
     * Uniform monochromatic light catch that prevents two-tone color clashing.
     */
    fun frostedBorderColor(theme: String, glassIntensity: Float): Color {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        return when (theme) {
            "AMOLED" -> {
                val alpha = (0.12f + 0.15f * intensity).coerceIn(0.10f, 0.32f)
                Color.White.copy(alpha = alpha)
            }
            "Light" -> {
                val alpha = (0.40f + 0.25f * intensity).coerceIn(0.35f, 0.75f)
                Color.White.copy(alpha = alpha)
            }
            else -> {
                val alpha = (0.12f + 0.16f * intensity).coerceIn(0.10f, 0.35f)
                Color.White.copy(alpha = alpha)
            }
        }
    }

    /**
     * Backwards-compatible alias for borderColor.
     */
    fun borderColor(theme: String, glassIntensity: Float): Color = frostedBorderColor(theme, glassIntensity)

    /**
     * Clean, subtle specular border brush.
     */
    fun embossedBorderBrush(theme: String, glassIntensity: Float): Brush {
        val color = frostedBorderColor(theme, glassIntensity)
        return Brush.verticalGradient(listOf(color, color))
    }

    /**
     * Clean specular rim border stroke for frosted glass elements.
     */
    fun embossedBorderStroke(theme: String, glassIntensity: Float, strokeWidth: Dp = 1.dp): BorderStroke {
        return BorderStroke(strokeWidth, frostedBorderColor(theme, glassIntensity))
    }

    /**
     * Frosted background for video overlay controls (CinemaPlayer time pills, docks, buttons).
     */
    fun playerFrostedBg(theme: String, isOled: Boolean, glassIntensity: Float): Color {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        val alpha = (0.86f - 0.10f * intensity).coerceIn(0.74f, 0.88f)
        return if (isOled || theme == "AMOLED") {
            Color(0xFF050811).copy(alpha = alpha)
        } else {
            Color(0xFF0F172A).copy(alpha = alpha)
        }
    }

    /**
     * Specular rim highlight for video overlay controls.
     */
    fun playerFrostedRim(theme: String, isOled: Boolean, glassIntensity: Float): Color {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        val alpha = (0.16f + 0.10f * intensity).coerceIn(0.14f, 0.26f)
        return Color.White.copy(alpha = alpha)
    }

    /**
     * Subtle hairline border color for cards across Library, Movies, TV Shows, and Settings.
     * Replaces harsh/thick outlines with a delicate 0.75dp specular rim:
     * - Dark: soft white specular rim (alpha = 0.12f) that catches ambient light gracefully
     * - AMOLED: deep dark rim (alpha = 0.10f)
     * - Light: soft dark hairline (alpha = 0.08f) creating clean separation
     */
    fun subtleCardBorderColor(theme: String = "Dark"): Color {
        return when (theme) {
            "Light" -> Color(0xFF000000).copy(alpha = 0.08f)
            "AMOLED" -> Color(0xFFFFFFFF).copy(alpha = 0.10f)
            else -> Color(0xFFFFFFFF).copy(alpha = 0.12f)
        }
    }

    /**
     * Subtle hairline border stroke for cards.
     */
    fun subtleCardBorder(theme: String = "Dark", strokeWidth: Dp = 0.75.dp): BorderStroke {
        return BorderStroke(strokeWidth, subtleCardBorderColor(theme))
    }

    // Aliases for backwards compatibility with any callers
    fun liquidSurfaceBrush(theme: String, glassIntensity: Float): Brush = embossedBorderBrush(theme, glassIntensity)
    fun liquidBorderBrush(theme: String, glassIntensity: Float): Brush = embossedBorderBrush(theme, glassIntensity)
    fun liquidBorderStroke(theme: String, glassIntensity: Float, strokeWidth: Dp = 1.1.dp): BorderStroke = embossedBorderStroke(theme, glassIntensity, strokeWidth)
    fun liquidCardBrush(theme: String, glassIntensity: Float): Brush = embossedBorderBrush(theme, glassIntensity)
}

/**
 * Top-level convenience accessors
 */
@Composable
fun subtleCardBorder(strokeWidth: Dp = 0.75.dp): BorderStroke {
    val theme = LocalThemeName.current
    return GlassTheme.subtleCardBorder(theme, strokeWidth)
}

@Composable
fun frostedSurfaceColor(glassIntensity: Float = LocalGlassIntensity.current): Color =
    GlassTheme.frostedSurfaceColor(LocalThemeName.current, glassIntensity)

@Composable
fun frostedBorderColor(glassIntensity: Float = LocalGlassIntensity.current): Color =
    GlassTheme.frostedBorderColor(LocalThemeName.current, glassIntensity)

@Composable
fun surfaceColor(glassIntensity: Float = LocalGlassIntensity.current): Color =
    GlassTheme.frostedSurfaceColor(LocalThemeName.current, glassIntensity)

@Composable
fun borderColor(glassIntensity: Float = LocalGlassIntensity.current): Color =
    GlassTheme.frostedBorderColor(LocalThemeName.current, glassIntensity)

fun subtleCardBorderColor(theme: String = "Dark"): Color = GlassTheme.subtleCardBorderColor(theme)
fun subtleCardBorder(theme: String = "Dark", strokeWidth: Dp = 0.75.dp): BorderStroke = GlassTheme.subtleCardBorder(theme, strokeWidth)
fun frostedSurfaceColor(theme: String, glassIntensity: Float): Color = GlassTheme.frostedSurfaceColor(theme, glassIntensity)
fun frostedBorderColor(theme: String, glassIntensity: Float): Color = GlassTheme.frostedBorderColor(theme, glassIntensity)
fun embossedBorderStroke(theme: String, glassIntensity: Float, strokeWidth: Dp = 1.1.dp): BorderStroke = GlassTheme.embossedBorderStroke(theme, glassIntensity, strokeWidth)

// Deprecated liquid aliases delegating to embossed frosted methods
fun liquidSurfaceBrush(theme: String, glassIntensity: Float): Brush = GlassTheme.embossedBorderBrush(theme, glassIntensity)
fun liquidBorderBrush(theme: String, glassIntensity: Float): Brush = GlassTheme.embossedBorderBrush(theme, glassIntensity)
fun liquidBorderStroke(theme: String, glassIntensity: Float, strokeWidth: Dp = 1.1.dp): BorderStroke = GlassTheme.embossedBorderStroke(theme, glassIntensity, strokeWidth)
fun liquidCardBrush(theme: String, glassIntensity: Float): Brush = GlassTheme.embossedBorderBrush(theme, glassIntensity)

/** Edge scrims retain the theme blend without allocating a viewport-sized offscreen texture. */
@Composable
fun Modifier.animatedScrollBlendFade(
    listState: LazyListState,
    topFadeHeight: Dp = 32.dp,
    bottomFadeHeight: Dp = 56.dp
): Modifier {
    val motion = LocalMotionStyle.current
    val top = animateFloatAsState(if (listState.canScrollBackward) 1f else 0f, motion.fadeSpec(), label = "Scroll top blend")
    val bottom = animateFloatAsState(if (listState.canScrollForward) 1f else 0f, motion.fadeSpec(), label = "Scroll bottom blend")
    val background = MaterialTheme.colorScheme.background
    return drawWithCache {
        val topHeight = topFadeHeight.toPx().coerceIn(1f, size.height.coerceAtLeast(1f))
        val bottomHeight = bottomFadeHeight.toPx().coerceIn(1f, size.height.coerceAtLeast(1f))
        val topBrush = Brush.verticalGradient(listOf(background, background.copy(alpha = 0f)), endY = topHeight)
        val bottomStart = (size.height - bottomHeight).coerceAtLeast(0f)
        val bottomBrush = Brush.verticalGradient(listOf(background.copy(alpha = 0f), background), startY = bottomStart, endY = size.height.coerceAtLeast(1f))
        onDrawWithContent {
            drawContent()
            if (top.value > 0f) drawRect(topBrush, size = Size(size.width, topHeight), alpha = top.value)
            if (bottom.value > 0f) drawRect(bottomBrush, topLeft = Offset(0f, bottomStart), size = Size(size.width, bottomHeight), alpha = bottom.value)
        }
    }
}
