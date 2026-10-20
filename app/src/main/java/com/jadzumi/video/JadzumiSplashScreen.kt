package com.jadzumi.video

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Authentic Jadzumi Play Symbol from jadzumi_player_splash_final.html
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
            quadraticTo(20f * sx, 30f * sx, 30f * sx, 36f * sy)
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
 * Ambient background matching jadzumi_player_splash_final.html:
 * Features ambient radial gradients, 3 floating blobs, and specular sheen.
 * Adaptive to Light, Dark, and AMOLED themes.
 */
@Composable
private fun AuroraAmbientBackground(
    theme: String,
    float1: Float,
    float2: Float,
    float3: Float,
    modifier: Modifier = Modifier
) {
    val isAmoled = theme == "AMOLED"
    val isLight = theme == "Light"
    val isDark = !isAmoled && !isLight

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when {
            isAmoled -> {
                // AMOLED: True Pitch Black for OLED display power efficiency
                drawRect(Color.Black)

                // Very soft accent floating blobs (.12f opacity) without destroying true black levels
                val b1X = w * (-0.10f + 0.22f * float1)
                val b1Y = h * (-0.05f + 0.10f * float2)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF188AF5).copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(b1X, b1Y),
                        radius = w * 0.45f
                    )
                )

                val b2X = w * (1.10f - 0.18f * float2)
                val b2Y = h * (0.40f - 0.12f * float3)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1659D9).copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(b2X, b2Y),
                        radius = w * 0.45f
                    )
                )
            }

            isDark -> {
                // Dark theme base: #091628
                drawRect(Color(0xFF091628))

                // Dark ambient gradient: #09101d -> #0b2445 52% -> #171e40
                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF09101D),
                            0.52f to Color(0xFF0B2445),
                            1.0f to Color(0xFF171E40)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )

                // Soft radial ellipse at 30% 28%
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF172E53).copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(w * 0.30f, h * 0.28f),
                        radius = w * 0.65f
                    )
                )

                // Soft radial ellipse at 74% 64%
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF154A8B).copy(alpha = 0.40f), Color.Transparent),
                        center = Offset(w * 0.74f, h * 0.64f),
                        radius = w * 0.70f
                    )
                )

                // Floating Blobs (soft opacity .18f for smooth background integration)
                val b1X = w * (-0.10f + 0.25f * float1)
                val b1Y = h * (-0.05f + 0.12f * float2)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF80C6FF).copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(b1X, b1Y),
                        radius = w * 0.50f
                    )
                )

                val b2X = w * (1.10f - 0.20f * float2)
                val b2Y = h * (0.40f - 0.15f * float3)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF8CAEFF).copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(b2X, b2Y),
                        radius = w * 0.50f
                    )
                )

                val b3X = w * (-0.05f + 0.18f * float3)
                val b3Y = h * (0.95f - 0.15f * float1)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFC2F1FF).copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(b3X, b3Y),
                        radius = w * 0.45f
                    )
                )

                // Sheen: linear-gradient(128deg, #ffffff0a, transparent 65%)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.04f), Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(w * 0.65f, h * 0.65f)
                    )
                )
            }

            else -> {
                // Light theme base: #eaf4ff
                drawRect(Color(0xFFEAF4FF))

                // Ambient linear gradient: #eff9ff -> #cde6ff 52% -> #e9e5ff
                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFFEFF9FF),
                            0.52f to Color(0xFFCDE6FF),
                            1.0f to Color(0xFFE9E5FF)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )

                // Ellipse at 30% 28%: #fff
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.90f), Color.Transparent),
                        center = Offset(w * 0.30f, h * 0.28f),
                        radius = w * 0.65f
                    )
                )

                // Ellipse at 74% 64%: #acd9ff
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFACD9FF).copy(alpha = 0.80f), Color.Transparent),
                        center = Offset(w * 0.74f, h * 0.64f),
                        radius = w * 0.70f
                    )
                )

                // Blob 1: #80c6ff (opacity .55)
                val b1X = w * (-0.10f + 0.25f * float1)
                val b1Y = h * (-0.05f + 0.14f * float2)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF80C6FF).copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(b1X, b1Y),
                        radius = w * 0.58f
                    )
                )

                // Blob 2: #8caeff (opacity .55)
                val b2X = w * (1.10f - 0.20f * float2)
                val b2Y = h * (0.42f - 0.16f * float3)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF8CAEFF).copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(b2X, b2Y),
                        radius = w * 0.58f
                    )
                )

                // Blob 3: #c2f1ff (opacity .55)
                val b3X = w * (-0.05f + 0.18f * float3)
                val b3Y = h * (0.95f - 0.15f * float1)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFC2F1FF).copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(b3X, b3Y),
                        radius = w * 0.52f
                    )
                )

                // Sheen: linear-gradient(128deg, #ffffff8a, transparent 35%, #ffffff3a 65%, transparent)
                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color.White.copy(alpha = 0.54f),
                            0.35f to Color.Transparent,
                            0.65f to Color.White.copy(alpha = 0.22f),
                            1.0f to Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )
            }
        }
    }
}

/**
 * Final Jadzumi Player Splash Screen
 * Pixel-perfect adaptation of jadzumi_player_splash_final.html:
 * - Adaptive layout for small phones, large phones, landscape, and tablets
 * - Compatible with Light, Dark, and AMOLED themes
 * - Glass tile with spec-accurate gradient, border, shadows, breathe aura, and levitation
 * - JADZUMI PLAYER letter-spaced typography
 * - Dynamic loading track with "Preparing your cinema…" -> "Loading your library…"
 */
@Composable
fun JadzumiSplashScreen(
    theme: String,
    onFinished: () -> Unit
) {
    val isAmoled = theme == "AMOLED"
    val isLight = theme == "Light"
    val isDark = !isAmoled && !isLight

    // Spec colors from CSS
    val inkColor = when {
        isLight -> Color(0xFF16346B)
        else -> Color(0xFFE9F2FF)
    }
    val mutedColor = when {
        isLight -> Color(0xFF6682A9)
        else -> Color(0xFF9CB9D9)
    }

    // Infinite transitions for atmospheric animations
    val infiniteTransition = rememberInfiniteTransition(label = "splashAtmosphere")

    // Ambient floating blob movements
    val float1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse),
        label = "float1"
    )
    val float2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "float2"
    )
    val float3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse),
        label = "float3"
    )

    // Logo levitate animation: translateY(-6px)
    val levitateY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "levitate"
    )

    // Entrance and Exit Animators
    val entryAlpha = remember { Animatable(0f) }
    val entryScale = remember { Animatable(0.76f) }
    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        entryAlpha.animateTo(1f, tween(1000, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
    }
    LaunchedEffect(Unit) {
        entryScale.animateTo(1f, tween(1000, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
    }

    LaunchedEffect(Unit) {
        // Simulated loading progress across ~2.4s matching HTML tick
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = FastOutSlowInEasing)
        )
        delay(250)
        // Exit transition: zoom to 1.09x and fade to 0
        exitScale.animateTo(1.09f, tween(600, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = FastOutSlowInEasing)
        )
        delay(250)
        exitAlpha.animateTo(0f, tween(550, easing = FastOutSlowInEasing))
        onFinished()
    }

    BackHandler { }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .alpha(exitAlpha.value)
            .scale(exitScale.value)
    ) {
        val screenHeight = maxHeight
        val isCompact = screenHeight < 680.dp

        // Adaptive sizes per HTML media queries:
        // @media(max-height: 680px) { .logo-glass { width: 140px; height: 140px; border-radius: 38px } .logo { width: 96px } }
        val glassSize = if (isCompact) 140.dp else 172.dp
        val glassRadius = if (isCompact) 38.dp else 46.dp
        val logoSize = if (isCompact) 96.dp else 116.dp

        // 1. Ambient Background Layer
        AuroraAmbientBackground(
            theme = theme,
            float1 = float1,
            float2 = float2,
            float3 = float3,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Main Splash Content Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glass Logo Tile matching jadzumi_player_splash_final.html
            Box(
                modifier = Modifier
                    .offset(y = levitateY.dp)
                    .alpha(entryAlpha.value)
                    .scale(entryScale.value),
                contentAlignment = Alignment.Center
            ) {
                // Frosted Glass Rounded Container
                // HTML:
                // Light: linear-gradient(145deg,#ffffffa8,#ffffff30 42%,#6db7ff36); border: 1px solid #ffffffcf; box-shadow: 0 20px 52px #3379c044
                // Dark: linear-gradient(145deg,#a4d6ff2b,#ffffff09); border-color: #ffffff35
                // Amoled: linear-gradient(145deg,#2d69a34d,#071c34a0); border-color: #ffffff24
                Surface(
                    modifier = Modifier
                        .size(glassSize)
                        .shadow(
                            elevation = if (isLight) 14.dp else 0.dp,
                            shape = RoundedCornerShape(glassRadius),
                            spotColor = if (isLight) Color(0x333379C0) else Color.Transparent
                        ),
                    shape = RoundedCornerShape(glassRadius),
                    color = Color.Transparent,
                    border = BorderStroke(
                        width = 1.dp,
                        color = when {
                            isLight -> Color(0xCFFFFFFF)
                            isAmoled -> Color(0x24FFFFFF)
                            else -> Color(0x35FFFFFF)
                        }
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                when {
                                    isAmoled -> Brush.linearGradient(
                                        colors = listOf(Color(0x4D2D69A3), Color(0xA0071C34)),
                                        start = Offset(0f, 0f),
                                        end = Offset(glassSize.value, glassSize.value)
                                    )
                                    isDark -> Brush.linearGradient(
                                        colors = listOf(Color(0x2BA4D6FF), Color(0x09FFFFFF)),
                                        start = Offset(0f, 0f),
                                        end = Offset(glassSize.value, glassSize.value)
                                    )
                                    else -> Brush.linearGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color(0xA8FFFFFF),
                                            0.42f to Color(0x30FFFFFF),
                                            1.0f to Color(0x366DB7FF)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(glassSize.value, glassSize.value)
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        JadzumiPlaySymbol(modifier = Modifier.size(logoSize))
                    }
                }
            }

            Spacer(Modifier.height(if (isCompact) 24.dp else 33.dp))

            // Spec Brand Typography: <h1>JADZUMI PLAYER</h1>
            Text(
                text = "JADZUMI PLAYER",
                color = inkColor,
                fontSize = 19.sp,
                fontWeight = FontWeight(650),
                letterSpacing = 0.29.em,
                modifier = Modifier.alpha(entryAlpha.value)
            )

            Spacer(Modifier.height(if (isCompact) 36.dp else 52.dp))

            // Dynamic Loading Track & Status Text
            val currentProgress = progress.value
            val statusText = if (currentProgress < 0.40f) "Preparing your cinema…" else "Loading your library…"

            Column(
                modifier = Modifier
                    .widthIn(max = 250.dp)
                    .fillMaxWidth(0.70f)
                    .alpha(entryAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 3px Progress Track with gradient fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isLight -> Color(0x466598C3)
                                isAmoled -> Color(0x2564DCFF)
                                else -> Color(0x25FFFFFF)
                            }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(currentProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1687F7), Color(0xFF71D8FF))
                                )
                            )
                    )
                }

                Spacer(Modifier.height(15.dp))

                Text(
                    text = statusText,
                    color = mutedColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
