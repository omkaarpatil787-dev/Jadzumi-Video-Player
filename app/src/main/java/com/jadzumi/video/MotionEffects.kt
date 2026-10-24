package com.jadzumi.video

import android.animation.ValueAnimator
import android.content.SharedPreferences
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class MotionStyle(val enabled: Boolean = false, val expressive: Boolean = false, val light: Boolean = true) {
    val damping get() = if (expressive) 0.65f else 0.9f
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
        spring(dampingRatio = style.damping, stiffness = Spring.StiffnessMedium), label = "touch feedback")
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
    HorizontalDivider(Modifier.padding(vertical = 14.dp))
    Text("Motion & touch", style = MaterialTheme.typography.titleMedium)
    Text("Reduced Motion and Android's animation setting take priority.", style = MaterialTheme.typography.bodySmall)
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Expressive springs")
            Text("Off: gentle feedback · On: more bounce and stretch", style = MaterialTheme.typography.bodySmall)
        }
        Switch(expressive, { expressive = it; prefs.edit().putString("motionStyle", if (it) "Expressive" else "Gentle").apply() })
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("Touch lighting", Modifier.weight(1f))
        Switch(lighting, { lighting = it; prefs.edit().putBoolean("touchLight", it).apply() })
    }
}
