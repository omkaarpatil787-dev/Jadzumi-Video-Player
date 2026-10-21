package com.jadzumi.video

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
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
     * Compute authentic frosted Gaussian blur surface color based on active theme and glass intensity (0.0f - 1.0f).
     * Maintains high diffused opacity (0.88f - 0.98f) to prevent transparent see-through wash or two-tone background split.
     */
    fun frostedSurfaceColor(theme: String, glassIntensity: Float): Color {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        return when (theme) {
            "AMOLED" -> {
                // Pure deep OLED black frosted depth without transparent washout
                val alpha = (0.99f - 0.05f * intensity).coerceIn(0.92f, 1.0f)
                Color(0xFF030712).copy(alpha = alpha)
            }
            "Light" -> {
                // Crisp luminous frosted white surface
                val alpha = (0.98f - 0.08f * intensity).coerceIn(0.88f, 0.98f)
                Color(0xFFFFFFFF).copy(alpha = alpha)
            }
            else -> {
                // Refined dark slate frosted Gaussian blur foundation
                val alpha = (0.97f - 0.08f * intensity).coerceIn(0.88f, 0.98f)
                Color(0xFF111827).copy(alpha = alpha)
            }
        }
    }

    /**
     * Backwards-compatible alias for surfaceColor.
     */
    fun surfaceColor(theme: String, glassIntensity: Float): Color = frostedSurfaceColor(theme, glassIntensity)

    /**
     * Compute frosted border outline color.
     * Uses a subtle monochromatic light catch to prevent two-tone color clashing.
     */
    fun frostedBorderColor(theme: String, glassIntensity: Float): Color {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        return when (theme) {
            "AMOLED" -> {
                val alpha = (0.11f + 0.08f * intensity).coerceIn(0.09f, 0.20f)
                Color.White.copy(alpha = alpha)
            }
            "Light" -> {
                val alpha = (0.45f + 0.25f * intensity).coerceIn(0.40f, 0.70f)
                Color(0xFFCBD5E1).copy(alpha = alpha)
            }
            else -> {
                val alpha = (0.09f + 0.08f * intensity).coerceIn(0.08f, 0.18f)
                Color.White.copy(alpha = alpha)
            }
        }
    }

    /**
     * Backwards-compatible alias for borderColor.
     */
    fun borderColor(theme: String, glassIntensity: Float): Color = frostedBorderColor(theme, glassIntensity)

    /**
     * Monochromatic Embossed Border Brush:
     * Generates a delicate, tactile 3D frosted glass bevel along card perimeters:
     * - Top edge catches subtle diffused white specular light.
     * - Bottom edge tapers to a softer neutral highlight.
     * - Strictly avoids colored tints (e.g. cyan/blue vs black) to eliminate two-tone banding.
     */
    fun embossedBorderBrush(theme: String, glassIntensity: Float): Brush {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        return when (theme) {
            "AMOLED" -> {
                val topAlpha = (0.18f + 0.10f * intensity).coerceIn(0.14f, 0.28f)
                val midAlpha = (0.08f + 0.05f * intensity).coerceIn(0.06f, 0.14f)
                val bottomAlpha = (0.04f + 0.04f * intensity).coerceIn(0.03f, 0.09f)
                Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = topAlpha),
                    0.40f to Color.White.copy(alpha = midAlpha),
                    1.0f to Color.White.copy(alpha = bottomAlpha)
                )
            }
            "Light" -> {
                val topAlpha = (0.80f + 0.15f * intensity).coerceIn(0.70f, 0.95f)
                val midAlpha = (0.45f + 0.15f * intensity).coerceIn(0.35f, 0.60f)
                val bottomAlpha = (0.28f + 0.12f * intensity).coerceIn(0.22f, 0.42f)
                Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = topAlpha),
                    0.40f to Color(0xFFE2E8F0).copy(alpha = midAlpha),
                    1.0f to Color(0xFFCBD5E1).copy(alpha = bottomAlpha)
                )
            }
            else -> { // Dark / Midnight
                val topAlpha = (0.16f + 0.10f * intensity).coerceIn(0.12f, 0.26f)
                val midAlpha = (0.07f + 0.05f * intensity).coerceIn(0.05f, 0.13f)
                val bottomAlpha = (0.04f + 0.04f * intensity).coerceIn(0.03f, 0.09f)
                Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = topAlpha),
                    0.40f to Color.White.copy(alpha = midAlpha),
                    1.0f to Color.White.copy(alpha = bottomAlpha)
                )
            }
        }
    }

    /**
     * Ready-to-use embossed border stroke with subtle tactile depth bevel.
     */
    fun embossedBorderStroke(theme: String, glassIntensity: Float, strokeWidth: Dp = 1.1.dp): BorderStroke {
        return BorderStroke(strokeWidth, embossedBorderBrush(theme, glassIntensity))
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

    // Aliases for backwards compatibility with any callers
    fun liquidSurfaceBrush(theme: String, glassIntensity: Float): Brush = embossedBorderBrush(theme, glassIntensity)
    fun liquidBorderBrush(theme: String, glassIntensity: Float): Brush = embossedBorderBrush(theme, glassIntensity)
    fun liquidBorderStroke(theme: String, glassIntensity: Float, strokeWidth: Dp = 1.1.dp): BorderStroke = embossedBorderStroke(theme, glassIntensity, strokeWidth)
    fun liquidCardBrush(theme: String, glassIntensity: Float): Brush = embossedBorderBrush(theme, glassIntensity)
}

/**
 * Top-level convenience accessors
 */
fun frostedSurfaceColor(theme: String, glassIntensity: Float): Color = GlassTheme.frostedSurfaceColor(theme, glassIntensity)
fun frostedBorderColor(theme: String, glassIntensity: Float): Color = GlassTheme.frostedBorderColor(theme, glassIntensity)
fun embossedBorderStroke(theme: String, glassIntensity: Float, strokeWidth: Dp = 1.1.dp): BorderStroke = GlassTheme.embossedBorderStroke(theme, glassIntensity, strokeWidth)

// Deprecated liquid aliases delegating to embossed frosted methods
fun liquidSurfaceBrush(theme: String, glassIntensity: Float): Brush = GlassTheme.embossedBorderBrush(theme, glassIntensity)
fun liquidBorderBrush(theme: String, glassIntensity: Float): Brush = GlassTheme.embossedBorderBrush(theme, glassIntensity)
fun liquidBorderStroke(theme: String, glassIntensity: Float, strokeWidth: Dp = 1.1.dp): BorderStroke = GlassTheme.embossedBorderStroke(theme, glassIntensity, strokeWidth)
fun liquidCardBrush(theme: String, glassIntensity: Float): Brush = GlassTheme.embossedBorderBrush(theme, glassIntensity)

/**
 * Scroll blend fade modifier:
 * Applies a smooth vertical fading edge on top and bottom of scrollable containers.
 * Top edge fades when content is scrolled down; bottom edge fades when more content remains below.
 */
@Composable
fun Modifier.animatedScrollBlendFade(
    listState: LazyListState,
    topFadeHeight: Dp = 32.dp,
    bottomFadeHeight: Dp = 56.dp
): Modifier {
    val animatedTopPx by animateDpAsState(
        targetValue = if (listState.canScrollBackward) topFadeHeight else 0.dp,
        animationSpec = tween(240, easing = LinearOutSlowInEasing),
        label = "scrollFadeTop"
    )
    val animatedBottomPx by animateDpAsState(
        targetValue = if (listState.canScrollForward) bottomFadeHeight else 0.dp,
        animationSpec = tween(240, easing = LinearOutSlowInEasing),
        label = "scrollFadeBottom"
    )
    return this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val height = size.height
            val topPx = animatedTopPx.toPx()
            val bottomPx = animatedBottomPx.toPx()

            if (topPx > 1f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        1.0f to Color.Black,
                        startY = 0f,
                        endY = topPx
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
            if (bottomPx > 1f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Black,
                        1.0f to Color.Transparent,
                        startY = (height - bottomPx).coerceAtLeast(0f),
                        endY = height
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
        }
}
