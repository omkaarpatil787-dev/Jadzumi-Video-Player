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
     * Delicate whisper hairline border color for cards across Library, Movies, TV Shows, and Settings.
     * Eliminates the harsh chalk-like corner emboss while providing a clean specular separation:
     * - Dark: refined soft hairline (alpha = 0.07f) that blends organically with dark backgrounds
     * - AMOLED: pure deep edge (alpha = 0.08f)
     * - Light: soft subtle edge (alpha = 0.05f)
     */
    fun subtleCardBorderColor(theme: String = "Dark"): Color {
        return when (theme) {
            "Light" -> Color(0xFF000000).copy(alpha = 0.05f)
            "AMOLED" -> Color(0xFFFFFFFF).copy(alpha = 0.08f)
            else -> Color(0xFFFFFFFF).copy(alpha = 0.07f)
        }
    }

    /**
     * Subtle hairline border stroke for cards with whisper 0.5dp thickness.
     */
    fun subtleCardBorder(theme: String = "Dark", strokeWidth: Dp = 0.5.dp): BorderStroke {
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
fun subtleCardBorder(strokeWidth: Dp = 0.5.dp): BorderStroke {
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
fun subtleCardBorder(theme: String = "Dark", strokeWidth: Dp = 0.5.dp): BorderStroke = GlassTheme.subtleCardBorder(theme, strokeWidth)
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
    val hasFade = animatedTopPx > 0.5.dp || animatedBottomPx > 0.5.dp
    if (!hasFade) return this

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
