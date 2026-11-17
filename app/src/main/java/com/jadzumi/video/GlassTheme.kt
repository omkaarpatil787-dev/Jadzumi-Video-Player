package com.jadzumi.video

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
     * Always maintains cinematic frosted obsidian acrylic so controls never glare white over video footage.
     */
    fun playerFrostedBg(theme: String, isOled: Boolean, glassIntensity: Float): Color {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        val alpha = (0.84f - 0.12f * intensity).coerceIn(0.70f, 0.85f)
        return if (isOled || theme == "AMOLED") Color(0xFF07050E).copy(alpha = alpha) else Color(0xFF13101F).copy(alpha = alpha)
    }

    /**
     * Specular rim highlight for video overlay controls.
     */
    fun playerFrostedRim(theme: String, isOled: Boolean, glassIntensity: Float): Color {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        val alpha = (0.18f + 0.08f * intensity).coerceIn(0.16f, 0.28f)
        return Color.White.copy(alpha = alpha)
    }

    /**
     * Clean card border: null/transparent to eliminate any corner emboss or bevel effect.
     * Leaves corner arcs completely smooth, uninterrupted, and natural while preserving the layout.
     */
    fun subtleCardBorderColor(theme: String = "Dark"): Color = Color.Transparent

    /**
     * BorderStroke? returning null to remove border outlines completely from cards.
     */
    fun subtleCardBorder(theme: String = "Dark", strokeWidth: Dp = 0.dp): BorderStroke? = null

    // Aliases for backwards compatibility with any callers
    fun liquidSurfaceBrush(theme: String, glassIntensity: Float): Brush = embossedBorderBrush(theme, glassIntensity)
    fun liquidBorderBrush(theme: String, glassIntensity: Float): Brush = embossedBorderBrush(theme, glassIntensity)
    fun liquidBorderStroke(theme: String, glassIntensity: Float, strokeWidth: Dp = 1.1.dp): BorderStroke = embossedBorderStroke(theme, glassIntensity, strokeWidth)
    fun liquidCardBrush(theme: String, glassIntensity: Float): Brush = embossedBorderBrush(theme, glassIntensity)
}

/**
 * Spatial Glass UI & Tactile Micro-Optics System
 * Replaces flat blurry glassmorphism with high-clarity dimensional optical surfaces,
 * specular hairline rim gradients, and tactile micro-press physics.
 */
object SpatialGlass {
    val Accent = Color(0xFFB79AF7)
    val Obsidian = Color(0xFF120E20)
    val LightPorcelainBg = Color(0xF5FAF9FE)
    val LightPorcelainSubBg = Color(0xEEF3F0FA)
    val LightPrimaryText = Color(0xFF161224)
    val LightSecondaryText = Color(0xFF645C75)
    val LightAccent = Color(0xFF6D28D9)

    /**
     * Optical lens background for video overlay controls (CinemaPlayer time pills, docks, buttons).
     * Always maintains deep obsidian-smoke optical clarity so controls float without milky haze.
     */
    fun pillBg(isOled: Boolean = false): Color {
        return if (isOled) Color(0xF2080611) else Color(0xCC141122)
    }

    /**
     * Optical sub-pill background for secondary actions (Lock, Subtitles, Audio, Speed, Fit).
     */
    fun subPillBg(isOled: Boolean = false): Color {
        return if (isOled) Color(0xA6181427) else Color(0x99231E35)
    }

    /**
     * Theme-adaptive card background color.
     */
    fun cardBg(theme: String): Color {
        return when (theme) {
            "Light" -> LightPorcelainBg
            "AMOLED" -> Color(0xFC06050A)
            else -> Color(0xF0161224)
        }
    }

    /**
     * Theme-adaptive sub-card or inset background color.
     */
    fun subCardBg(theme: String): Color {
        return when (theme) {
            "Light" -> LightPorcelainSubBg
            "AMOLED" -> Color(0x99181427)
            else -> Color(0x80231E35)
        }
    }

    /**
     * Specular hairline rim gradient that catches dimensional light from above.
     */
    fun specularRimBrush(alphaBoost: Float = 1.0f): Brush {
        return Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = (0.28f * alphaBoost).coerceIn(0f, 1f)),
                Color.White.copy(alpha = (0.12f * alphaBoost).coerceIn(0f, 1f)),
                Color.White.copy(alpha = (0.04f * alphaBoost).coerceIn(0f, 1f))
            )
        )
    }

    /**
     * Theme-adaptive specular hairline rim brush for cards.
     */
    fun cardBorderBrush(theme: String, alphaBoost: Float = 1.0f): Brush {
        return if (theme == "Light") {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = (0.95f * alphaBoost).coerceIn(0f, 1f)),
                    Color(0xFFE5DFEE).copy(alpha = (0.75f * alphaBoost).coerceIn(0f, 1f)),
                    Color(0x33544878)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = (0.24f * alphaBoost).coerceIn(0f, 1f)),
                    Color.White.copy(alpha = (0.10f * alphaBoost).coerceIn(0f, 1f)),
                    Color.White.copy(alpha = (0.04f * alphaBoost).coerceIn(0f, 1f))
                )
            )
        }
    }

    /**
     * Specular rim border stroke for pills.
     */
    fun pillBorder(strokeWidth: Dp = 1.dp, alphaBoost: Float = 1.0f): BorderStroke {
        return BorderStroke(strokeWidth, specularRimBrush(alphaBoost))
    }

    /**
     * Theme-adaptive border stroke for 20dp cards.
     */
    fun cardBorder(theme: String, strokeWidth: Dp = 1.dp, alphaBoost: Float = 1.0f): BorderStroke {
        return BorderStroke(strokeWidth, cardBorderBrush(theme, alphaBoost))
    }
}

/**
 * Tactile Spatial Glass Pill Composable.
 * Features 3D optical refraction, specular rim highlight, and tactile micro-press physics.
 */
@Composable
fun SpatialTactilePill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    selected: Boolean = false,
    theme: String = LocalThemeName.current,
    isOled: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val active = isActive || selected
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tactilePressScale"
    )
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val isLight = theme == "Light"
    val effectiveOled = isOled || theme == "AMOLED"

    val bgColor = when {
        active && isLight -> SpatialGlass.LightAccent
        active -> SpatialGlass.Accent
        isLight -> SpatialGlass.LightPorcelainSubBg
        else -> SpatialGlass.subPillBg(effectiveOled)
    }

    val borderStroke = when {
        active && isLight -> BorderStroke(1.dp, Color(0xFF551CA8))
        active -> BorderStroke(1.dp, Color(0xFFD4C2FA))
        else -> SpatialGlass.cardBorder(if (isLight) "Light" else "Dark", 1.dp, if (isPressed) 1.5f else 1.0f)
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
        border = borderStroke,
        shadowElevation = if (isPressed) 2.dp else (if (isLight) 4.dp else 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            content = content
        )
    }
}

/**
 * Tactile Spatial Glass Card Composable (20.dp rounded corners with specular hairline rim).
 */
@Composable
fun SpatialTactileCard(
    modifier: Modifier = Modifier,
    theme: String = LocalThemeName.current,
    onClick: (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "spatialCardPressScale"
    )
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(isPressed) {
        if (onClick != null && isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = shape,
        color = SpatialGlass.cardBg(theme),
        border = SpatialGlass.cardBorder(theme, 1.dp, if (isPressed) 1.4f else 1.0f),
        shadowElevation = if (theme == "Light") 6.dp else 10.dp
    ) {
        Column(content = content)
    }
}

/**
 * Top-level convenience accessors
 */
@Composable
fun subtleCardBorder(strokeWidth: Dp = 0.dp): BorderStroke? {
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
fun subtleCardBorder(theme: String = "Dark", strokeWidth: Dp = 0.dp): BorderStroke? = GlassTheme.subtleCardBorder(theme, strokeWidth)
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

/**
 * High-performance animated shimmer skeleton placeholder for media cards & thumbnails
 */
@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 450f, translateAnim - 450f),
        end = Offset(translateAnim, translateAnim)
    )
    Box(modifier = modifier.background(brush))
}
