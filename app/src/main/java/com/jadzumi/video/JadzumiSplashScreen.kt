package com.jadzumi.video

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Authentic Jadzumi Play Symbol from Aurora Splash specification
 * Grid: 120 x 120 with quadratic bezier curves
 */
@Composable
fun JadzumiPlaySymbol(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sx = w / 120f
        val sy = h / 120f

        // 1. Blue Wing Path: M41 18Q41 12 49 16L103 48Q111 53 111 62V93Q111 102 102 98L41 64Z
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
                colors = listOf(Color(0xFF64DCFF), Color(0xFF188AF5), Color(0xFF1659D9)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // 2. White Front Play Path: M20 40Q20 30 30 36L76 62Q86 68 76 75L30 104Q20 110 20 99Z
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
                colors = listOf(Color.White, Color(0xFFD8EFFF)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
    }
}

/**
 * Aurora Ambient Background with floating blurred energy blobs
 */
@Composable
private fun AuroraAmbientBackground(
    theme: String,
    anim1: Float,
    anim2: Float,
    modifier: Modifier = Modifier
) {
    val isAmoled = theme == "AMOLED"
    // Cinema Aurora is always deep dark cinematic palette
    val isDark = true

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        if (isAmoled) {
            // Pure AMOLED deep background
            drawRect(Color.Black)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF152A4A).copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.7f
                )
            )
        } else if (isDark) {
            // Dark Aurora
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF09101D), Color(0xFF0B2445), Color(0xFF171E40)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF172E53).copy(alpha = 0.8f), Color.Transparent),
                    center = Offset(w * 0.30f, h * 0.28f),
                    radius = w * 0.65f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF154A8B).copy(alpha = 0.7f), Color.Transparent),
                    center = Offset(w * 0.74f, h * 0.64f),
                    radius = w * 0.7f
                )
            )

            // Animated subtle blobs
            val b1X = w * (0.2f + 0.15f * anim1)
            val b1Y = h * (0.15f + 0.10f * anim2)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1E4E8C).copy(alpha = 0.32f), Color.Transparent),
                    center = Offset(b1X, b1Y),
                    radius = w * 0.45f
                )
            )

            val b2X = w * (0.8f - 0.18f * anim2)
            val b2Y = h * (0.55f - 0.12f * anim1)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1E3A70).copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(b2X, b2Y),
                    radius = w * 0.48f
                )
            )
        } else {
            // Light Aurora
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFEFF9FF), Color(0xFFCDE6FF), Color(0xFFE9E5FF)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.9f), Color.Transparent),
                    center = Offset(w * 0.30f, h * 0.28f),
                    radius = w * 0.60f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFACD9FF).copy(alpha = 0.85f), Color.Transparent),
                    center = Offset(w * 0.74f, h * 0.64f),
                    radius = w * 0.65f
                )
            )

            // Animated blobs
            val b1X = w * (0.2f + 0.20f * anim1)
            val b1Y = h * (0.1f + 0.14f * anim2)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF80C6FF).copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(b1X, b1Y),
                    radius = w * 0.50f
                )
            )

            val b2X = w * (0.85f - 0.22f * anim2)
            val b2Y = h * (0.6f - 0.15f * anim1)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8CAEFF).copy(alpha = 0.50f), Color.Transparent),
                    center = Offset(b2X, b2Y),
                    radius = w * 0.52f
                )
            )

            val b3X = w * (0.35f + 0.15f * anim2)
            val b3Y = h * (0.85f - 0.12f * anim1)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFC2F1FF).copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(b3X, b3Y),
                    radius = w * 0.46f
                )
            )
        }

        // Sheen diagonal highlight
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDark) 0.04f else 0.45f),
                    Color.Transparent,
                    Color.White.copy(alpha = if (isDark) 0.02f else 0.20f),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
    }
}

/**
 * Full Aurora Splash Screen based on jadzumi_player_splash_final.html
 */
@Composable
fun JadzumiSplashScreen(
    theme: String,
    onFinished: () -> Unit
) {
    val isAmoled = theme == "AMOLED"
    // Cinematic Splash is always dark cinematic palette
    val isDark = true

    val inkColor = if (isDark) Color(0xFFE9F2FF) else Color(0xFF16346B)
    val mutedColor = if (isDark) Color(0xFF9CB9D9) else Color(0xFF6682A9)

    // Infinite ambient aurora movement
    val infiniteTransition = rememberInfiniteTransition(label = "auroraMotion")
    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7500, easing = LinearEasing), RepeatMode.Reverse),
        label = "anim1"
    )
    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9500, easing = LinearEasing), RepeatMode.Reverse),
        label = "anim2"
    )
    val levitateY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "levitate"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowPulse"
    )

    // Progress and Entrance/Exit Animators
    val progress = remember { Animatable(0f) }
    val entryAlpha = remember { Animatable(0f) }
    val entryScale = remember { Animatable(0.82f) }
    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Entrance animation
        entryAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        entryScale.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
    }

    // Progress timeline: ~2200ms
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing)
        )
        delay(250)
        // Smooth exit crossfade
        exitScale.animateTo(1.06f, tween(500, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing)
        )
        delay(250)
        exitAlpha.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        onFinished()
    }

    BackHandler {
        // Absorb back presses during initial splash
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(exitAlpha.value)
            .scale(exitScale.value)
    ) {
        // 1. Ambient Animated Background
        AuroraAmbientBackground(
            theme = theme,
            anim1 = anim1,
            anim2 = anim2,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Center Content: Logo + Brand + Loading Track
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glass Logo Tile with Ambient Pulse Glow
            Box(
                modifier = Modifier
                    .offset(y = levitateY.dp)
                    .alpha(entryAlpha.value)
                    .scale(entryScale.value),
                contentAlignment = Alignment.Center
            ) {
                // Outer Radial Glow Circle
                Canvas(Modifier.size(230.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6AB7FF).copy(alpha = glowPulse),
                                Color.Transparent
                            ),
                            radius = size.width * 0.48f
                        )
                    )
                }

                // Frosted Glass Rounded Container
                Surface(
                    modifier = Modifier
                        .size(172.dp)
                        .shadow(
                            elevation = if (isDark) 24.dp else 28.dp,
                            shape = RoundedCornerShape(46.dp),
                            spotColor = Color(0xFF3379C0).copy(alpha = 0.35f)
                        ),
                    shape = RoundedCornerShape(46.dp),
                    color = if (isAmoled) Color(0xFF071C34).copy(alpha = 0.55f)
                    else if (isDark) Color(0xFFA4D6FF).copy(alpha = 0.12f)
                    else Color.White.copy(alpha = 0.55f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.75f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        JadzumiPlaySymbol(modifier = Modifier.size(116.dp))
                    }
                }
            }

            Spacer(Modifier.height(34.dp))

            // Brand Typography: JADZUMI PLAYER
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(entryAlpha.value)
            ) {
                Text(
                    text = "JADZUMI PLAYER",
                    color = inkColor,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.28.em
                )
            }

            Spacer(Modifier.height(48.dp))

            // Loading Track & Dynamic Status Text
            val currentProgress = progress.value
            val statusText = if (currentProgress < 0.42f) "Preparing your cinema…" else "Loading your library…"

            Column(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .fillMaxWidth(0.68f)
                    .alpha(entryAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 3dp Pill Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isDark) Color(0xFF6598C3).copy(alpha = 0.20f)
                            else Color(0xFF6598C3).copy(alpha = 0.28f)
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

                Spacer(Modifier.height(14.dp))

                Text(
                    text = statusText,
                    color = mutedColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
