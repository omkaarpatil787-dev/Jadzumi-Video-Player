package com.jadzumi.video

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp

/** A bounded opening mask over real content. Does not clip/recreate the hardware video surface. */
@Composable internal fun PortalApertureMask(progress: () -> Float, color: Color, modifier: Modifier = Modifier, coverCenter: Boolean = false) {
    val path = remember { Path() }
    Canvas(modifier) {
        val p = progress().coerceIn(0f, 1f)
        if (p < 1f) {
            val x = size.width * 0.12f * (1f - p)
            val y = size.height * 0.22f * (1f - p)
            val aperture = RoundRect(Rect(x, y, size.width - x, size.height - y), CornerRadius(32.dp.toPx() * (1f - p)))
            path.reset(); path.fillType = PathFillType.EvenOdd
            path.addRect(Rect(Offset.Zero, size)); path.addRoundRect(aperture)
            drawPath(path, color)
            if (coverCenter) drawRoundRect(color.copy(alpha = 1f - p), Offset(x, y),
                androidx.compose.ui.geometry.Size(size.width - x * 2, size.height - y * 2), aperture.topLeftCornerRadius)
        }
    }
}

@Composable internal fun cinemaSurfaceBrush(): Brush {
    val surface = MaterialTheme.colorScheme.surface
    val accent = MaterialTheme.colorScheme.primary
    return remember(surface, accent) { Brush.linearGradient(listOf(accent.copy(alpha = 0.045f), surface.copy(alpha = 0f))) }
}

/** Three matching line segments interpolate into an options/close icon; no new setting is added. */
@Composable internal fun CinemaOptionsMorph(open: Boolean, modifier: Modifier = Modifier) {
    val motion = LocalMotionStyle.current
    val progress = animateFloatAsState(if (open) 1f else 0f,
        motion.springSpec(), label = "Options icon")
    val start = remember { arrayOf(floatArrayOf(5f,6f,19f,6f), floatArrayOf(5f,12f,19f,12f), floatArrayOf(5f,18f,19f,18f)) }
    val end = remember { arrayOf(floatArrayOf(6f,6f,18f,18f), floatArrayOf(12f,12f,12f,12f), floatArrayOf(6f,18f,18f,6f)) }
    Canvas(modifier) {
        val p = progress.value
        for (i in start.indices) {
            fun point(index: Int) = Offset((start[i][index] + (end[i][index] - start[i][index]) * p) * size.width / 24f,
                (start[i][index + 1] + (end[i][index + 1] - start[i][index + 1]) * p) * size.height / 24f)
            drawLine(Color.White.copy(alpha = if (i == 1) 1f - p else 1f), point(0), point(2), 1.8.dp.toPx(), StrokeCap.Round)
        }
    }
}
