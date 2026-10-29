package com.jadzumi.video

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Authentic Jadzumi Play Symbol from jadzumi_player_splash_final.html / Cinema Portal
 * ViewBox 120x120:
 * 1. Blue Wing: M41 18Q41 12 49 16L103 48Q111 53 111 62V93Q111 102 102 98L41 64Z
 *    Gradient: #64DCFF (0%) -> #188AF5 (55%) -> #1659D9 (100%)
 * 2. Front White Play: M20 40Q20 30 30 36L76 62Q86 68 76 75L30 104Q20 110 20 99Z
 *    Gradient: #FFFFFF (0%) -> #D8EFFF (100%)
 */
@Composable
fun JadzumiPlaySymbol(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sx = w / 120f
        val sy = h / 120f

        // 1. Blue Wing Path
        val blueWing = Path().apply {
            moveTo(41f * sx, 18f * sy)
            quadraticTo(41f * sx, 12f * sy, 49f * sx, 16f * sy)
            lineTo(103f * sx, 48f * sy)
            quadraticTo(111f * sx, 53f * sy, 111f * sx, 62f * sy)
            lineTo(111f * sx, 93f * sy)
            quadraticTo(111f * sx, 102f * sy, 102f * sx, 98f * sy)
            lineTo(41f * sx, 64f * sy)
            close()
        }
        drawPath(
            path = blueWing,
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFF64DCFF),
                    0.55f to Color(0xFF188AF5),
                    1.0f to Color(0xFF1659D9)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // 2. Front White Play Path
        val whitePlay = Path().apply {
            moveTo(20f * sx, 40f * sy)
            quadraticTo(20f * sx, 30f * sy, 30f * sx, 36f * sy)
            lineTo(76f * sx, 62f * sy)
            quadraticTo(86f * sx, 68f * sy, 76f * sx, 75f * sy)
            lineTo(30f * sx, 104f * sy)
            quadraticTo(20f * sx, 110f * sy, 20f * sx, 99f * sy)
            close()
        }
        drawPath(
            path = whitePlay,
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to Color.White,
                    1.0f to Color(0xFFD8EFFF)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
    }
}

/**
 * Jadzumi Player · Cinema Portal · Unhurried
 * Spec-accurate Jetpack Compose implementation of Cinema Portal motion study:
 * - Phase 1: Glass Frame arrives (0-1100ms) with scale 0.96 -> 1, soft fade, and 10px rise.
 * - Phase 2: Emblem & Wordmark arrive (180-1380ms) with separate gentle fades.
 * - Phase 3: Breathing Room (1380-1900ms) quiet brand hold.
 * - Phase 4: Portal Unfolds (1900-3600ms) continuous rounded expansion with CubicBezierEasing(0.4f, 0f, 0.2f, 1f).
 * - Phase 5: Brand dissolves (1900-2850ms) with upward drift and seamless handoff to Library.
 */
@Composable
fun JadzumiSplashScreen(
    theme: String,
    reducedMotion: Boolean = false,
    onFinished: () -> Unit
) {
    val isAmoled = theme == "AMOLED"
    val isLight = theme == "Light"

    // Colors matching Cinema Portal specification
    val bgColor = when {
        isLight -> Color(0xFFF4F7FC)
        isAmoled -> Color(0xFF000000)
        else -> Color(0xFF0F1724)
    }
    val glassColor = when {
        isLight -> Color(0xFFE5F0FE)
        isAmoled -> Color(0xFF071222)
        else -> Color(0xFF20344C)
    }
    val inkColor = when {
        isLight -> Color(0xFF16346B)
        else -> Color(0xFFF1F5F9)
    }
    val mutedColor = when {
        isLight -> Color(0xFF526B8C)
        else -> Color(0xFFA4B8D1)
    }
    val edgeColor = when {
        isLight -> Color(0xDEFFFFFF)
        isAmoled -> Color(0x25FFFFFF)
        else -> Color(0x28FFFFFF)
    }
    val haloColor = when {
        isLight -> Color(0x558FC7FF)
        isAmoled -> Color.Transparent
        else -> Color(0x26358EF5)
    }

    // Master Timeline in Milliseconds (0 to 3600 ms)
    val timelineMs = remember { Animatable(0f) }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            timelineMs.animateTo(3600f, tween(400, easing = LinearEasing))
            onFinished()
        } else {
            timelineMs.animateTo(
                targetValue = 3600f,
                animationSpec = tween(durationMillis = 3600, easing = LinearEasing)
            )
            onFinished()
        }
    }

    // Dismiss early on back press or tap
    BackHandler { onFinished() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onFinished() }
    ) {
        val w = maxWidth
        val h = maxHeight
        val isPhone = w < 600.dp

        val t = timelineMs.value

        // Helper easing calculations matching CSS / cubic-bezier specs:
        // entry: 0 to 1100ms
        val entry = (t / 1100f).coerceIn(0f, 1f)
        val entryEase = remember(entry) {
            val p = entry
            3f * (1f - p) * (1f - p) * p * 0.4f + 3f * (1f - p) * p * p * 0.2f + p * p * p
        }

        // brandEntry: 180 to 1380ms
        val brandP = ((t - 180f) / 1200f).coerceIn(0f, 1f)
        val brandEase = remember(brandP) {
            val p = brandP
            3f * (1f - p) * (1f - p) * p * 0.42f + 3f * (1f - p) * p * p * 0.58f + p * p * p
        }

        // portal unfold exit: 1900 to 3500ms (1600ms unfolding)
        val exitP = ((t - 1900f) / 1600f).coerceIn(0f, 1f)
        val exitEase = remember(exitP) {
            val p = exitP
            // CubicBezier(0.4, 0.0, 0.2, 1.0)
            3f * (1f - p) * (1f - p) * p * 0.4f + 3f * (1f - p) * p * p * 0.2f + p * p * p
        }

        // brand dissolve: 1900 to 2800ms
        val dissolveP = ((t - 1900f) / 900f).coerceIn(0f, 1f)
        val dissolveEase = remember(dissolveP) {
            val p = dissolveP
            3f * (1f - p) * (1f - p) * p * 0.42f + 3f * (1f - p) * p * p * 0.58f + p * p * p
        }

        // Resting portal dimensions
        val restingPw = if (isPhone) w * 0.84f else w * 0.76f
        val restingPh = if (isPhone) (h * 0.54f).coerceAtMost(310.dp) else (h * 0.62f).coerceAtMost(275.dp)

        // Interpolated portal size & corner radius
        val portalWidth = restingPw + (w - restingPw) * exitEase
        val portalHeight = restingPh + (h - restingPh) * exitEase
        val portalRadius = 42.dp * (1f - exitEase)
        val portalScale = 0.96f + 0.04f * entryEase
        val portalY = 10.dp * (1f - entryEase)
        val portalAlpha = if (reducedMotion) 1f - (t / 400f).coerceIn(0f, 1f) else (entryEase * (1f - exitEase * 0.95f)).coerceIn(0f, 1f)

        // Brand alpha & offset
        val brandAlpha = if (reducedMotion) 1f - (t / 400f).coerceIn(0f, 1f) else (brandEase * (1f - dissolveEase)).coerceIn(0f, 1f)
        val brandY = 8.dp * (1f - brandEase) - 8.dp * dissolveEase

        // Sheen progress (-42% to +33%)
        val sheenProgress = ((t - 250f) / 2900f).coerceIn(0f, 1f)

        // 1. Atmospheric Ambient Halos
        if (!isAmoled && haloColor != Color.Transparent) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cw = size.width
                val ch = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(haloColor, Color.Transparent),
                        center = Offset(cw * 0.20f, ch * 0.18f),
                        radius = cw * 0.60f
                    ),
                    center = Offset(cw * 0.20f, ch * 0.18f),
                    radius = cw * 0.60f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(haloColor, Color.Transparent),
                        center = Offset(cw, ch),
                        radius = cw * 0.65f
                    ),
                    center = Offset(cw, ch),
                    radius = cw * 0.65f
                )
            }
        }

        // 2. The Cinema Portal Glass Frame
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = portalY)
                .scale(portalScale)
                .width(portalWidth)
                .height(portalHeight)
                .alpha(portalAlpha)
                .shadow(
                    elevation = if (isLight) 22.dp else 12.dp,
                    shape = RoundedCornerShape(portalRadius),
                    spotColor = if (isLight) Color(0x333379C0) else Color(0x33000000)
                )
                .clip(RoundedCornerShape(portalRadius))
                .background(
                    Brush.linearGradient(
                        colors = listOf(glassColor, bgColor),
                        start = Offset(0f, 0f),
                        end = Offset(portalWidth.value * 2f, portalHeight.value * 2f)
                    )
                )
        ) {
            // Subtle specular glass border
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(portalRadius),
                color = Color.Transparent,
                border = BorderStroke(1.dp, edgeColor.copy(alpha = edgeColor.alpha * (1f - exitEase)))
            ) {}

            // Sheen sweeping across the portal
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sw = size.width
                val sh = size.height
                val sheenX = (-0.42f + sheenProgress * 0.75f) * sw
                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.38f to Color.Transparent,
                            0.49f to Color(0x13AFD7FF),
                            0.50f to Color(0x1AFFFFFF),
                            0.61f to Color.Transparent,
                            1.0f to Color.Transparent
                        ),
                        start = Offset(sheenX, 0f),
                        end = Offset(sheenX + sw * 0.8f, sh)
                    )
                )
            }
        }

        // 3. Central Brand Identity (Logo + Wordmark)
        if (brandAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = brandY)
                    .alpha(brandAlpha),
                contentAlignment = Alignment.Center
            ) {
                if (isPhone) {
                    // Vertical stacked layout for phones
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        JadzumiPlaySymbol(modifier = Modifier.size(116.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "JADZUMI",
                                color = inkColor,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.15.em
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "PLAYER",
                                color = mutedColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.55.em
                            )
                        }
                    }
                } else {
                    // Side-by-side horizontal layout for tablet / landscape with vertical divider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        JadzumiPlaySymbol(modifier = Modifier.size(128.dp))

                        // Vertical dividing line
                        Box(
                            modifier = Modifier
                                .height(88.dp)
                                .width(1.dp)
                                .background(edgeColor)
                        )

                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = "JADZUMI",
                                color = inkColor,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.15.em,
                                lineHeight = 38.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "PLAYER",
                                color = mutedColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.55.em
                            )
                        }
                    }
                }
            }
        }
    }
}
