package com.jadzumi.video

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cinema Flow v1.3 — Design Tokens System
 * Extracted directly from stitch_jadzumi_cinema_player_design_system (code.html & screen.png)
 */

@Immutable
data class CinemaFlowColors(
    val bgPrimary: Color,
    val bgSecondary: Color,
    val accentPrimary: Color,
    val accentSoft: Color,
    val accentLavender: Color,
    val accentGlow: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    val glassSurface: Color,
    val glassCard: Color,
    val glassBorder: Color,
    val glassSpecular: Color,
    val pillBg: Color,
    val pillActiveBg: Color,
    val pillActiveText: Color,
    val auraColor1: Color,
    val auraColor2: Color,
    val auraColor3: Color,
    val auraOpacity: Float
)

val CinemaFlowLightColors = CinemaFlowColors(
    bgPrimary = Color(0xFFF7FAFF),
    bgSecondary = Color(0xFFEAF4FF),
    accentPrimary = Color(0xFF458AC9),
    accentSoft = Color(0xFF8DC8F8),
    accentLavender = Color(0xFFD9D5FF),
    accentGlow = Color(0x598DC8F8),
    inkPrimary = Color(0xFF1C2B3D),
    inkSecondary = Color(0xFF586C82),
    inkMuted = Color(0xFF859BB0),
    glassSurface = Color(0xB8FFFFFF), // rgba(255, 255, 255, 0.72)
    glassCard = Color(0xA6FFFFFF),    // rgba(255, 255, 255, 0.65)
    glassBorder = Color(0xD9FFFFFF),  // rgba(255, 255, 255, 0.85)
    glassSpecular = Color(0xF2FFFFFF),// rgba(255, 255, 255, 0.95)
    pillBg = Color(0xD1FFFFFF),       // rgba(255, 255, 255, 0.82)
    pillActiveBg = Color(0xFFE1F0FF),
    pillActiveText = Color(0xFF2C689C),
    auraColor1 = Color(0xFF91C9FF),
    auraColor2 = Color(0xFFD9D5FF),
    auraColor3 = Color(0xFFF4F8FF),
    auraOpacity = 0.75f
)

val CinemaFlowDarkColors = CinemaFlowColors(
    bgPrimary = Color(0xFF0E1724),
    bgSecondary = Color(0xFF142032),
    accentPrimary = Color(0xFF76B5EF),
    accentSoft = Color(0xFF4D8BC7),
    accentLavender = Color(0xFFB5ADFF),
    accentGlow = Color(0x4076B5EF),
    inkPrimary = Color(0xFFF8FAFC),
    inkSecondary = Color(0xFFCBD5E1),
    inkMuted = Color(0xFFA0B3C6),
    glassSurface = Color(0xBF162234), // rgba(22, 34, 52, 0.75)
    glassCard = Color(0xA61A283C),    // rgba(26, 40, 60, 0.65)
    glassBorder = Color(0x298CB9EB),  // rgba(140, 185, 235, 0.16)
    glassSpecular = Color(0x1FFFFFFF),// rgba(255, 255, 255, 0.12)
    pillBg = Color(0xCC1E2E44),       // rgba(30, 46, 68, 0.80)
    pillActiveBg = Color(0x4D4D8BC7), // rgba(77, 139, 199, 0.30)
    pillActiveText = Color(0xFFBFE0FF),
    auraColor1 = Color(0xFF28568A),
    auraColor2 = Color(0xFF383372),
    auraColor3 = Color(0xFF142338),
    auraOpacity = 0.35f
)

val CinemaFlowAmoledColors = CinemaFlowColors(
    bgPrimary = Color(0xFF000000),
    bgSecondary = Color(0xFF08080A),
    accentPrimary = Color(0xFF8AC3F7),
    accentSoft = Color(0xFF396FA1),
    accentLavender = Color(0xFF948BFF),
    accentGlow = Color(0x408AC3F7),
    inkPrimary = Color(0xFFFFFFFF),
    inkSecondary = Color(0xFFE2E8F0),
    inkMuted = Color(0xFFB0B7C3),
    glassSurface = Color(0xE0121216), // rgba(18, 18, 22, 0.88)
    glassCard = Color(0xC716161A),    // rgba(22, 22, 26, 0.78)
    glassBorder = Color(0x1CFFFFFF),  // rgba(255, 255, 255, 0.11)
    glassSpecular = Color(0x14FFFFFF),// rgba(255, 255, 255, 0.08)
    pillBg = Color(0xFF16161A),
    pillActiveBg = Color(0xFF272832),
    pillActiveText = Color(0xFF96CBFA),
    auraColor1 = Color(0xFF1A3352),
    auraColor2 = Color(0xFF221E47),
    auraColor3 = Color(0xFF080E17),
    auraOpacity = 0.12f
)

val LocalCinemaFlowColors = staticCompositionLocalOf { CinemaFlowDarkColors }

/**
 * Geometric corner radii specified by Cinema Flow design tokens
 */
object CinemaCorners {
    val Sm = RoundedCornerShape(14.dp)
    val Md = RoundedCornerShape(20.dp)
    val Lg = RoundedCornerShape(28.dp)
    val Xl = RoundedCornerShape(36.dp)
    val Pill = RoundedCornerShape(percent = 50)
}

/**
 * Extension modifiers for Cinema Flow glassmorphism
 */
@Composable
fun Modifier.cinemaGlassCard(
    shape: Shape = CinemaCorners.Lg,
    borderWidth: Dp = 1.dp,
    customAlpha: Float? = null
): Modifier {
    val colors = LocalCinemaFlowColors.current
    val glassIntensity = LocalGlassIntensity.current.coerceIn(0f, 1f)
    val baseAlpha = colors.glassCard.alpha
    val effectiveAlpha = customAlpha ?: (baseAlpha * (0.55f + 0.45f * glassIntensity)).coerceIn(0.15f, 0.95f)
    val fill = colors.glassCard.copy(alpha = effectiveAlpha)

    val specularBrush = Brush.linearGradient(
        colors = listOf(
            colors.glassSpecular,
            colors.glassBorder,
            colors.glassBorder.copy(alpha = colors.glassBorder.alpha * 0.4f)
        )
    )

    return this
        .clip(shape)
        .background(fill, shape)
        .border(borderWidth, specularBrush, shape)
}

@Composable
fun Modifier.cinemaGlassSurface(
    shape: Shape = CinemaCorners.Xl,
    borderWidth: Dp = 1.dp,
    customAlpha: Float? = null
): Modifier {
    val colors = LocalCinemaFlowColors.current
    val glassIntensity = LocalGlassIntensity.current.coerceIn(0f, 1f)
    val baseAlpha = colors.glassSurface.alpha
    val effectiveAlpha = customAlpha ?: (baseAlpha * (0.6f + 0.4f * glassIntensity)).coerceIn(0.2f, 0.98f)
    val fill = colors.glassSurface.copy(alpha = effectiveAlpha)

    val specularBrush = Brush.linearGradient(
        colors = listOf(
            colors.glassSpecular,
            colors.glassBorder,
            colors.glassBorder.copy(alpha = colors.glassBorder.alpha * 0.5f)
        )
    )

    return this
        .clip(shape)
        .background(fill, shape)
        .border(borderWidth, specularBrush, shape)
}

@Composable
fun Modifier.cinemaPill(
    isActive: Boolean = false,
    active: Boolean = isActive,
    shape: Shape = CinemaCorners.Pill
): Modifier {
    val colors = LocalCinemaFlowColors.current
    val isPillActive = isActive || active
    val fill = if (isPillActive) colors.pillActiveBg else colors.pillBg
    val borderCol = if (isPillActive) colors.accentPrimary.copy(alpha = 0.5f) else colors.glassBorder

    return this
        .clip(shape)
        .background(fill, shape)
        .border(1.dp, borderCol, shape)
}
