package com.jadzumi.video

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Authentic Jadzumi Play Symbol from Cinema Portal (backward-compatible)
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
 * Spec-accurate Jadzumi Adaptive Glass Icon from jadzumi_adaptive_splash_clean.html
 * 320x320 viewBox SVG layout:
 * - Rounded Squircle Tile (rx=86) with quad-stop linear gradient (#fff -> #d6f1ff -> #c6dfff -> #e9dfff)
 * - Rear 3D Wing: dual-layered paths with #side & #blue gradients, with animated glide/rotation
 * - Front Glass Play: with #glass gradient & 3px white stroke, with animated glide/scale
 * - Sheen: diagonal light beam sweeping across the clipped squircle
 */
@Composable
fun JadzumiAdaptiveSplashIcon(
    modifier: Modifier = Modifier,
    rearProgress: Float = 1f,
    frontProgress: Float = 1f,
    sheenProgress: Float = 0f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sx = w / 320f
        val sy = h / 320f

        // 1. Squircle Background Path (x=7, y=7, w=306, h=306, rx=86)
        val clipSquircle = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 7f * sx,
                    top = 7f * sy,
                    right = 313f * sx,
                    bottom = 313f * sy,
                    cornerRadius = CornerRadius(86f * sx, 86f * sy)
                )
            )
        }

        // Drop shadow for outer tile
        drawRoundRect(
            color = Color(0x352E78C9),
            topLeft = Offset(7f * sx, 14f * sy),
            size = Size(306f * sx, 306f * sy),
            cornerRadius = CornerRadius(86f * sx, 86f * sy)
        )

        // Squircle Fill
        drawPath(
            path = clipSquircle,
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFFFFFFFF),
                    0.46f to Color(0xFFD6F1FF),
                    0.80f to Color(0xFFC6DFFF),
                    1.0f to Color(0xFFE9DFFF)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // Squircle Stroke
        drawPath(
            path = clipSquircle,
            color = Color.White,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * sx)
        )

        // Rear Wing Enter Animation interpolation:
        // 0% -> (28px, 8px, rot 8deg) | 65% -> (-5px, -2px, rot -2deg) | 100% -> (0, 0, rot 0)
        val rearDx: Float
        val rearDy: Float
        val rearRot: Float
        if (rearProgress < 0.65f) {
            val p = (rearProgress / 0.65f).coerceIn(0f, 1f)
            rearDx = (28f + (-5f - 28f) * p) * sx
            rearDy = (8f + (-2f - 8f) * p) * sy
            rearRot = 8f + (-2f - 8f) * p
        } else {
            val p = ((rearProgress - 0.65f) / 0.35f).coerceIn(0f, 1f)
            rearDx = (-5f + (0f - (-5f)) * p) * sx
            rearDy = (-2f + (0f - (-2f)) * p) * sy
            rearRot = -2f + (0f - (-2f)) * p
        }

        // 2. Rear Wing Group
        translate(left = rearDx, top = rearDy) {
            rotate(degrees = rearRot, pivot = Offset(190f * sx, 155f * sy)) {
                // Path 1 (Side): M105 70 Q105 40 136 56 L260 130 Q275 139 275 159 L275 236 Q275 267 248 252 L125 181 Q105 170 105 146Z
                val sidePath = Path().apply {
                    moveTo(105f * sx, 70f * sy)
                    quadraticTo(105f * sx, 40f * sy, 136f * sx, 56f * sy)
                    lineTo(260f * sx, 130f * sy)
                    quadraticTo(275f * sx, 139f * sy, 275f * sx, 159f * sy)
                    lineTo(275f * sx, 236f * sy)
                    quadraticTo(275f * sx, 267f * sy, 248f * sx, 252f * sy)
                    lineTo(125f * sx, 181f * sy)
                    quadraticTo(105f * sx, 170f * sy, 105f * sx, 146f * sy)
                    close()
                }
                drawPath(
                    path = sidePath,
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF00AAFF),
                            1.0f to Color(0xFF7ACFFF)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )
                drawPath(
                    path = sidePath,
                    color = Color(0xFF86E6FF),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f * sx)
                )

                // Path 2 (Blue): M103 68 Q103 41 129 54 L218 107 Q233 116 233 138 L233 216 Q233 244 208 229 L116 176 Q103 169 103 148Z
                val bluePath = Path().apply {
                    moveTo(103f * sx, 68f * sy)
                    quadraticTo(103f * sx, 41f * sy, 129f * sx, 54f * sy)
                    lineTo(218f * sx, 107f * sy)
                    quadraticTo(233f * sx, 116f * sy, 233f * sx, 138f * sy)
                    lineTo(233f * sx, 216f * sy)
                    quadraticTo(233f * sx, 244f * sy, 208f * sx, 229f * sy)
                    lineTo(116f * sx, 176f * sy)
                    quadraticTo(103f * sx, 169f * sy, 103f * sx, 148f * sy)
                    close()
                }
                drawPath(
                    path = bluePath,
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.05f to Color(0xFF32AAFF),
                            0.52f to Color(0xFF0057F4),
                            1.0f to Color(0xFF0848DB)
                        ),
                        start = Offset(w * 0.05f, 0f),
                        end = Offset(w * 0.9f, h)
                    )
                )
                drawPath(
                    path = bluePath,
                    color = Color(0xFF9BE9FF),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f * sx)
                )
            }
        }

        // Front Glass Enter Animation interpolation:
        // 0% -> (-28px, 9px, scale 0.85) | 65% -> (5px, -3px, scale 1.04) | 100% -> (0, 0, scale 1.0)
        val frontDx: Float
        val frontDy: Float
        val frontSc: Float
        if (frontProgress < 0.65f) {
            val p = (frontProgress / 0.65f).coerceIn(0f, 1f)
            frontDx = (-28f + (5f - (-28f)) * p) * sx
            frontDy = (9f + (-3f - 9f) * p) * sy
            frontSc = 0.85f + (1.04f - 0.85f) * p
        } else {
            val p = ((frontProgress - 0.65f) / 0.35f).coerceIn(0f, 1f)
            frontDx = (5f + (0f - 5f) * p) * sx
            frontDy = (-3f + (0f - (-3f)) * p) * sy
            frontSc = 1.04f + (1.0f - 1.04f) * p
        }

        // 3. Front Glass Play Group
        // Path: M65 108 Q65 78 92 91 L182 143 Q212 161 184 182 L92 239 Q65 253 65 220Z
        translate(left = frontDx, top = frontDy) {
            scale(scale = frontSc, pivot = Offset(130f * sx, 165f * sy)) {
                val glassPath = Path().apply {
                    moveTo(65f * sx, 108f * sy)
                    quadraticTo(65f * sx, 78f * sy, 92f * sx, 91f * sy)
                    lineTo(182f * sx, 143f * sy)
                    quadraticTo(212f * sx, 161f * sy, 184f * sx, 182f * sy)
                    lineTo(92f * sx, 239f * sy)
                    quadraticTo(65f * sx, 253f * sy, 65f * sx, 220f * sy)
                    close()
                }

                // Front glass drop shadow
                drawPath(
                    path = glassPath,
                    color = Color(0x381E4F90),
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )

                drawPath(
                    path = glassPath,
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFAFFFFFF),
                            0.50f to Color(0xEBE7F7FF),
                            1.0f to Color(0xED91C6FF)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )
                drawPath(
                    path = glassPath,
                    color = Color.White,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * sx)
                )
            }
        }

        // 4. Sheen Sweep clipped to squircle
        // Sheen Path: M-50 -15 L-4 -15 L165 335 L119 335Z
        // Translated from -330px to 410px across 320 coordinate space
        val sheenX = (-330f + 740f * sheenProgress) * sx
        clipPath(clipSquircle) {
            val sheenPath = Path().apply {
                moveTo((-50f * sx) + sheenX, -15f * sy)
                lineTo((-4f * sx) + sheenX, -15f * sy)
                lineTo((165f * sx) + sheenX, 335f * sy)
                lineTo((119f * sx) + sheenX, 335f * sy)
                close()
            }
            drawPath(
                path = sheenPath,
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0x00FFFFFF),
                        0.5f to Color(0xBFFFFFFF),
                        1.0f to Color(0x00FFFFFF)
                    ),
                    start = Offset((-50f * sx) + sheenX, 0f),
                    end = Offset((165f * sx) + sheenX, 0f)
                )
            )
        }
    }
}

/**
 * Jadzumi Player · Adaptive Splash Studio
 * Spec-accurate Jetpack Compose implementation of jadzumi_adaptive_splash_clean.html:
 * - Light, Dark, and AMOLED appearances matching CSS variables
 * - Ambient radial glow / halo
 * - Signature 320x320 SVG glass morph animation
 * - Diagonal specular sheen sweep
 * - Expressive title typography & bottom progress bar
 * - Seamless exit transition into the Library
 */
@Composable
fun JadzumiSplashScreen(
    theme: String,
    reducedMotion: Boolean = false,
    onFinished: () -> Unit
) {
    val isAmoled = theme.equals("AMOLED", ignoreCase = true)
    val isLight = theme.equals("Light", ignoreCase = true)

    // Colors matching jadzumi_adaptive_splash_clean.html specifications
    val bgColor = when {
        isLight -> Color(0xFFF3F9FF)
        isAmoled -> Color(0xFF000000)
        else -> Color(0xFF0B1426)
    }
    val fgColor = when {
        isLight -> Color(0xFF153B77)
        isAmoled -> Color(0xFFF7FAFF)
        else -> Color(0xFFEEF6FF)
    }
    val haloColor = when {
        isLight -> Color(0x9C8EC9FF)
        isAmoled -> Color(0x5E1050A6)
        else -> Color(0x951757A8)
    }
    val trackColor = when {
        isLight -> Color(0xFFB9D6F5)
        isAmoled -> Color(0xFF252525)
        else -> Color(0xFF334A67)
    }

    // Master Timeline in Milliseconds (0 to 1800 ms)
    val timelineMs = remember { Animatable(0f) }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            timelineMs.animateTo(1800f, tween(300, easing = LinearEasing))
            onFinished()
        } else {
            timelineMs.animateTo(
                targetValue = 1800f,
                animationSpec = tween(durationMillis = 1800, easing = LinearEasing)
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
        val t = timelineMs.value

        // Animation Timings from jadzumi_adaptive_splash_clean.html:
        // logoEnter: 0 to 1250ms (scale 0.77 -> 1.04 -> 1.0, translateY 35dp -> -12dp -> -5dp)
        val logoP = (t / 1250f).coerceIn(0f, 1f)
        val logoScale: Float
        val logoY: Float
        val logoAlpha: Float
        if (logoP < 0.65f) {
            val p = (logoP / 0.65f).coerceIn(0f, 1f)
            logoScale = 0.77f + (1.04f - 0.77f) * p
            logoY = 35f + (-12f - 35f) * p
            logoAlpha = p
        } else {
            val p = ((logoP - 0.65f) / 0.35f).coerceIn(0f, 1f)
            logoScale = 1.04f + (1.0f - 1.04f) * p
            logoY = -12f + (-5f - (-12f)) * p
            logoAlpha = 1f
        }

        // rearEnter: 0 to 1200ms
        val rearProgress = (t / 1200f).coerceIn(0f, 1f)

        // frontEnter: 0 to 1200ms
        val frontProgress = (t / 1200f).coerceIn(0f, 1f)

        // sheen: 280ms to 1630ms (1350ms duration)
        val sheenProgress = ((t - 280f) / 1350f).coerceIn(0f, 1f)

        // title fadeRise: 400ms to 1200ms (800ms)
        val titleP = ((t - 400f) / 800f).coerceIn(0f, 1f)
        val titleAlpha = titleP
        val titleY = 13f * (1f - titleP)

        // progress: 0 to 1800ms
        val loadProgress = (t / 1800f).coerceIn(0f, 1f)

        // exit transition: 1400ms to 1800ms (400ms)
        val exitP = ((t - 1400f) / 400f).coerceIn(0f, 1f)
        val exitEase = exitP * exitP
        val masterAlpha = (1f - exitEase).coerceIn(0f, 1f)
        val masterScale = 1f - 0.08f * exitEase
        val masterY = -10f * exitEase

        // 1. Ambient Radial Glow (circle at 50% X, 43% Y)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cw = size.width
            val ch = size.height
            val haloRadius = if (isAmoled) cw * 0.45f else cw * 0.60f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(haloColor.copy(alpha = haloColor.alpha * (1f - exitEase)), Color.Transparent),
                    center = Offset(cw * 0.50f, ch * 0.43f),
                    radius = haloRadius
                ),
                center = Offset(cw * 0.50f, ch * 0.43f),
                radius = haloRadius
            )
        }

        // 2. Central Adaptive Icon & Wordmark
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (masterY + logoY).dp)
                .scale(masterScale * logoScale)
                .alpha(masterAlpha * logoAlpha),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(26.dp)
            ) {
                // Signature 320x320 SVG Glass Icon
                val iconSize = if (w < 600.dp) 160.dp else 195.dp
                JadzumiAdaptiveSplashIcon(
                    modifier = Modifier.size(iconSize),
                    rearProgress = rearProgress,
                    frontProgress = frontProgress,
                    sheenProgress = sheenProgress
                )

                // Title Typography
                Text(
                    text = "JADZUMI PLAYER",
                    color = fgColor,
                    fontSize = if (w < 600.dp) 17.sp else 21.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.23.em,
                    modifier = Modifier
                        .offset(y = titleY.dp)
                        .alpha(titleAlpha)
                )
            }
        }

        // 3. Bottom Progress Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 34.dp)
                .width(85.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(trackColor)
                .alpha(masterAlpha)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(loadProgress)
                    .background(Color(0xFF4E9DFF))
            )
        }
    }
}
