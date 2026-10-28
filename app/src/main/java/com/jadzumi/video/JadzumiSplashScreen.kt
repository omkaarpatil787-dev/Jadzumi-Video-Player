package com.jadzumi.video

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

/** Opens onto the composed destination. No simulated loading percentage or artificial scan delay. */
@Composable fun JadzumiSplashScreen(theme: String, onFinished: () -> Unit, ready: Boolean = true) {
    val motion = LocalMotionStyle.current
    val progress = remember { Animatable(0f) }
    val brand = remember { Animatable(0f) }
    val finished by rememberUpdatedState(onFinished)
    val background = when (theme) {
        "Light" -> Color(0xFFF4F7FC)
        "AMOLED" -> Color.Black
        else -> Color(0xFF0F1724)
    }
    LaunchedEffect(ready, motion.enabled) {
        if (motion.enabled) brand.animateTo(1f, tween(180))
        if (ready) {
            if (motion.enabled) progress.animateTo(1f, tween(620, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)))
            finished()
        }
    }
    BackHandler { finished() }
    BoxWithConstraints(Modifier.fillMaxSize().pointerInput(Unit) {
        awaitPointerEventScope { while (true) awaitPointerEvent(PointerEventPass.Final).changes.forEach { it.consume() } }
    }) {
        PortalApertureMask({ progress.value }, background, Modifier.fillMaxSize(), coverCenter = true)
        val compact = maxWidth < 600.dp
        val symbolSize = if (maxHeight < 400.dp) 72.dp else if (compact) 100.dp else 120.dp
        val label: @Composable () -> Unit = {
            Column(horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start) {
                Text("JADZUMI", fontSize = if (compact) 27.sp else 34.sp, letterSpacing = 4.sp,
                    fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                Text("PLAYER", letterSpacing = 6.sp, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Box(Modifier.align(Alignment.Center).graphicsLayer {
            alpha = brand.value * (1f - progress.value * 2f).coerceIn(0f, 1f)
            translationY = -12.dp.toPx() * progress.value
        }) {
            if (compact) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                JadzumiPlaySymbol(Modifier.size(symbolSize)); label()
            } else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                JadzumiPlaySymbol(Modifier.size(symbolSize)); label()
            }
        }
        TextButton(onClick = { finished() }, modifier = Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(16.dp)) { Text("Skip opening") }
    }
}
