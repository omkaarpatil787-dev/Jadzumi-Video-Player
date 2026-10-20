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
 * Authentic Jadzumi Play Symbol with app icon's signature cyan, sapphire, and royal blue gradient
 */
@Composable
fun JadzumiPlaySymbol(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sx = w / 120f
        val sy = h / 120f

        // 1. Blue Wing Path (App Icon signature color gradient: Cyan -> Azure -> Royal Blue)
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
                colors = listOf(Color.White, Color(0xFFE2F3FF)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
    }
}

/**
 * Adaptive Glowing Aurora Background: Seamlessly tailored to Light, Dark, and AMOLED palettes
 * using the app icon's iconic colors (#64DCFF, #188AF5, #1659D9) without harsh circular artifacts.
 */
@Composable
private fun AuroraAmbientBackground(
    theme: String,
    anim1: Float,
    anim2: Float,
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
                // Pure Pitch Black for OLED energy conservation and true zero black levels
                drawRect(Color.Black)

                // Ultra-soft, gentle specular aurora accent matching app icon colors
                val shiftX = w * (0.45f + 0.10f * anim1)
                val shiftY = h * (0.42f + 0.08f * anim2)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF188AF5).copy(alpha = 0.15f),
                            Color(0xFF1659D9).copy(alpha = 0.08f),
                            Color(0xFF64DCFF).copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        center = Offset(shiftX, shiftY),
                        radius = w * 0.75f
                    )
                )
            }

            isDark -> {
                // Premium deep obsidian navy base
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF090E1A), Color(0xFF0C1424), Color(0xFF0A0F1D)),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )

                // Flowing, diffused Aurora waves (blending icon's cyan, azure, and royal blues seamlessly)
                val a1X = w * (0.25f + 0.20f * anim1)
                val a1Y = h * (0.20f + 0.15f * anim2)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF188AF5).copy(alpha = 0.22f),
                            Color(0xFF1659D9).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(a1X, a1Y),
                        radius = w * 0.85f
                    )
                )

                val a2X = w * (0.75f - 0.20f * anim2)
                val a2Y = h * (0.65f - 0.15f * anim1)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF64DCFF).copy(alpha = 0.16f),
                            Color(0xFF188AF5).copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(a2X, a2Y),
                        radius = w * 0.90f
                    )
                )

                // Diagonal specular sheen
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent,
                            Color(0xFF64DCFF).copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )
            }

            else -> {
                // Luminous, pristine Light Aurora
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFF5FAFF), Color(0xFFEAF4FF), Color(0xFFF2F0FF)),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )

                val a1X = w * (0.25f + 0.18f * anim1)
                val a1Y = h * (0.22f + 0.12f * anim2)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF64DCFF).copy(alpha = 0.25f),
                            Color(0xFF188AF5).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(a1X, a1Y),
                        radius = w * 0.70f
                    )
                )

                val a2X = w * (0.78f - 0.18f * anim2)
                val a2Y = h * (0.62f - 0.14f * anim1)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFBCE5FF).copy(alpha = 0.40f),
                            Color(0xFFD6C8FF).copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(a2X, a2Y),
                        radius = w * 0.75f
                    )
                )
            }
        }
    }
}

/**
 * Full Aurora Splash Screen adaptive to Light, Dark, and AMOLED themes
 */
@Composable
fun JadzumiSplashScreen(
    theme: String,
    onFinished: () -> Unit
) {
    val isAmoled = theme == "AMOLED"
    val isLight = theme == "Light"
    val isDark = !isAmoled && !isLight

    val inkColor = when {
        isLight -> Color(0xFF0F1E36)
        else -> Color(0xFFF0F6FF)
    }
    val mutedColor = when {
        isLight -> Color(0xFF5A718F)
        isAmoled -> Color(0xFF88A6C8)
        else -> Color(0xFF9CB9D9)
    }

    // Ambient aurora motion
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
        initialValue = 0f, targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "levitate"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.40f, targetValue = 0.70f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowPulse"
    )

    // Progress and Entrance/Exit Animators
    val progress = remember { Animatable(0f) }
    val entryAlpha = remember { Animatable(0f) }
    val entryScale = remember { Animatable(0.85f) }
    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        entryAlpha.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        entryScale.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2100, easing = FastOutSlowInEasing)
        )
        delay(200)
        exitScale.animateTo(1.05f, tween(450, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2100, easing = FastOutSlowInEasing)
        )
        delay(200)
        exitAlpha.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
        onFinished()
    }

    BackHandler { }

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
            // Glass Logo Tile with Soft Multi-stop Icon Glow
            Box(
                modifier = Modifier
                    .offset(y = levitateY.dp)
                    .alpha(entryAlpha.value)
                    .scale(entryScale.value),
                contentAlignment = Alignment.Center
            ) {
                // Wide, gentle diffuse aura (matching app icon colors with zero harsh circular borders)
                Canvas(Modifier.size(280.dp)) {
                    val w = size.width
                    val h = size.height
                    val auraColors = when {
                        isLight -> listOf(
                            Color(0xFF64DCFF).copy(alpha = 0.28f * glowPulse),
                            Color(0xFF188AF5).copy(alpha = 0.16f * glowPulse),
                            Color(0xFF1659D9).copy(alpha = 0.05f * glowPulse),
                            Color.Transparent
                        )
                        isAmoled -> listOf(
                            Color(0xFF188AF5).copy(alpha = 0.20f * glowPulse),
                            Color(0xFF64DCFF).copy(alpha = 0.10f * glowPulse),
                            Color.Transparent
                        )
                        else -> listOf(
                            Color(0xFF64DCFF).copy(alpha = 0.22f * glowPulse),
                            Color(0xFF188AF5).copy(alpha = 0.16f * glowPulse),
                            Color(0xFF1659D9).copy(alpha = 0.06f * glowPulse),
                            Color.Transparent
                        )
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = auraColors,
                            center = Offset(w * 0.5f, h * 0.5f),
                            radius = w * 0.50f
                        )
                    )
                }

                // Frosted Glass Rounded Container
                Surface(
                    modifier = Modifier
                        .size(164.dp)
                        .shadow(
                            elevation = if (isLight) 12.dp else 20.dp,
                            shape = RoundedCornerShape(44.dp),
                            spotColor = if (isLight) Color(0xFF64DCFF).copy(alpha = 0.30f) else Color(0xFF188AF5).copy(alpha = 0.35f)
                        ),
                    shape = RoundedCornerShape(44.dp),
                    color = when {
                        isAmoled -> Color(0xFF061426).copy(alpha = 0.65f)
                        isLight -> Color.White.copy(alpha = 0.75f)
                        else -> Color(0xFF101B2E).copy(alpha = 0.55f)
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = when {
                            isLight -> Color.White.copy(alpha = 0.90f)
                            isAmoled -> Color(0xFF188AF5).copy(alpha = 0.35f)
                            else -> Color.White.copy(alpha = 0.22f)
                        }
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        JadzumiPlaySymbol(modifier = Modifier.size(110.dp))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

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
                    letterSpacing = 0.26.em
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Cinema & Offline Video Player",
                    color = mutedColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.05.em
                )
            }

            Spacer(Modifier.height(44.dp))

            // Loading Track & Dynamic Status Text
            val currentProgress = progress.value
            val statusText = if (currentProgress < 0.45f) "Preparing cinema engine…" else "Loading media library…"

            Column(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .fillMaxWidth(0.68f)
                    .alpha(entryAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Smooth pill progress track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isLight) Color(0xFF188AF5).copy(alpha = 0.15f)
                            else Color(0xFF64DCFF).copy(alpha = 0.18f)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(currentProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF188AF5), Color(0xFF64DCFF))
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
