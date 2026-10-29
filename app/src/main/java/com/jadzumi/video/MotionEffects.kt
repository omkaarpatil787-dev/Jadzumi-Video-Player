package com.jadzumi.video

import android.animation.ValueAnimator
import android.content.SharedPreferences
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class MotionStyle(val enabled: Boolean = false, val expressive: Boolean = false, val light: Boolean = true) {
    val damping get() = if (expressive) 0.70f else 0.85f
}
internal val LocalMotionStyle = staticCompositionLocalOf { MotionStyle() }

@Composable internal fun rememberMotionStyle(reduced: Boolean, lively: Boolean): MotionStyle {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("appearance", 0) }
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return remember(revision, reduced, lively) {
        MotionStyle(!reduced && lively && ValueAnimator.areAnimatorsEnabled(),
            prefs.getString("motionStyle", "Gentle") == "Expressive", prefs.getBoolean("touchLight", true))
    }
}

/** Observes touches without consuming them: existing clicks, scrolls and accessibility remain intact. */
internal fun Modifier.touchMotion(): Modifier = composed {
    val style = LocalMotionStyle.current
    if (!style.enabled) return@composed this
    var pressed by remember { mutableStateOf(false) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val amount by animateFloatAsState(if (pressed) 1f else 0f,
        spring(dampingRatio = style.damping, stiffness = Spring.StiffnessMediumLow), label = "touch feedback")
    val tint = MaterialTheme.colorScheme.primary
    this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                pressed = event.changes.any { it.pressed }
                event.changes.firstOrNull { it.pressed }?.let { origin = it.position }
            }
        }
    }.graphicsLayer {
        scaleX = 1f - amount * if (style.expressive) 0.025f else 0.012f
        scaleY = 1f - amount * if (style.expressive) 0.035f else 0.012f
    }.drawWithContent {
        drawContent()
        if (style.light && amount > 0.001f) {
            drawRect(Brush.radialGradient(listOf(tint.copy(alpha = 0.12f * amount), tint.copy(alpha = 0f)),
                center = origin, radius = size.maxDimension.coerceAtLeast(1f)))
        }
    }
}

/** Only unconsumed edge scrolling stretches the viewport; never takes scroll distance from children. */
internal fun Modifier.elasticEdge(): Modifier = composed {
    val style = LocalMotionStyle.current
    if (!style.enabled) return@composed this
    val scope = rememberCoroutineScope()
    var pull by remember { mutableFloatStateOf(0f) }
    val stretch by animateFloatAsState(pull, spring(style.damping, Spring.StiffnessLow), label = "edge return")
    val connection = remember {
        object : NestedScrollConnection {
            var reset: Job? = null
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y != 0f && source == NestedScrollSource.UserInput) {
                    pull = (pull + available.y * 0.12f).coerceIn(-20f, 20f)
                    reset?.cancel()
                    reset = scope.launch { delay(100); pull = 0f }
                }
                return Offset.Zero
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                reset?.cancel(); pull = 0f
                return Velocity.Zero
            }
        }
    }
    this.nestedScroll(connection).graphicsLayer {
        translationY = stretch
        scaleY = 1f + kotlin.math.abs(stretch) / size.height.coerceAtLeast(1f)
    }
}

@Composable internal fun MotionCustomization() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("appearance", 0) }
    var expressive by remember { mutableStateOf(prefs.getString("motionStyle", "Gentle") == "Expressive") }
    var lighting by remember { mutableStateOf(prefs.getBoolean("touchLight", true)) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Motion & Touch Physics",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Fine-tune physical bounce, damping, and touch feedback. Reduced Motion takes priority.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Expressive Springs",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Off: gentle fluid feedback · On: dynamic elastic bounce & stretch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = expressive,
                onCheckedChange = {
                    expressive = it
                    prefs.edit().putString("motionStyle", if (it) "Expressive" else "Gentle").apply()
                }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Touch Lighting & Ripple",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Subtle ambient luminous highlights on pressed cards and navigation buttons",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = lighting,
                onCheckedChange = {
                    lighting = it
                    prefs.edit().putBoolean("touchLight", it).apply()
                }
            )
        }
    }
}
