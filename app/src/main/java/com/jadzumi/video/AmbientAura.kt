package com.jadzumi.video

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cinema Flow Ambient Aura System
 * Replicates the Stitch multi-blob radial gradient aura behind frosted glass.
 * Renders efficiently on Canvas with subtle organic breathing motion when animations are enabled.
 */
@Composable
fun CinemaAmbientAura(
    enabled: Boolean = true,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    val colors = LocalCinemaFlowColors.current
    val auraAlpha = colors.auraOpacity

    if (reducedMotion) {
        // Static aura rendering for battery efficiency and accessibility
        Canvas(modifier = modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Blob 1: Top-Left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.auraColor1.copy(alpha = auraAlpha * 0.8f), Color.Transparent),
                    center = Offset(w * 0.25f, h * 0.20f),
                    radius = w * 0.55f
                ),
                center = Offset(w * 0.25f, h * 0.20f),
                radius = w * 0.55f
            )

            // Blob 2: Middle-Right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.auraColor2.copy(alpha = auraAlpha * 0.7f), Color.Transparent),
                    center = Offset(w * 0.80f, h * 0.45f),
                    radius = w * 0.50f
                ),
                center = Offset(w * 0.80f, h * 0.45f),
                radius = w * 0.50f
            )

            // Blob 3: Bottom-Left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.auraColor3.copy(alpha = auraAlpha * 0.9f), Color.Transparent),
                    center = Offset(w * 0.20f, h * 0.80f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.20f, h * 0.80f),
                radius = w * 0.65f
            )
        }
        return
    }

    // Dynamic fluid floating aura
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientAuraMotion")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Blob1Phase"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Blob2Phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Blob 1 subtle orbital float
        val b1X = w * (0.25f + 0.04f * sin(phase1))
        val b1Y = h * (0.20f + 0.03f * cos(phase1))
        val b1Radius = w * (0.55f + 0.03f * sin(phase1 * 0.5f))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.auraColor1.copy(alpha = auraAlpha * 0.85f), Color.Transparent),
                center = Offset(b1X, b1Y),
                radius = b1Radius
            ),
            center = Offset(b1X, b1Y),
            radius = b1Radius
        )

        // Blob 2 counter float
        val b2X = w * (0.80f - 0.05f * cos(phase2))
        val b2Y = h * (0.45f + 0.04f * sin(phase2))
        val b2Radius = w * (0.50f + 0.04f * cos(phase2 * 0.5f))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.auraColor2.copy(alpha = auraAlpha * 0.75f), Color.Transparent),
                center = Offset(b2X, b2Y),
                radius = b2Radius
            ),
            center = Offset(b2X, b2Y),
            radius = b2Radius
        )

        // Blob 3 deep foundation float
        val b3X = w * (0.20f + 0.03f * cos(phase1 * 0.7f))
        val b3Y = h * (0.80f - 0.03f * sin(phase2 * 0.6f))
        val b3Radius = w * (0.65f + 0.03f * sin(phase2))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.auraColor3.copy(alpha = auraAlpha * 0.90f), Color.Transparent),
                center = Offset(b3X, b3Y),
                radius = b3Radius
            ),
            center = Offset(b3X, b3Y),
            radius = b3Radius
        )
    }
}
