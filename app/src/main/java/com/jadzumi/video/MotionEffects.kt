package com.jadzumi.video

import android.animation.ValueAnimator
import android.content.SharedPreferences
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/** Jadzumi Living Motion Specification Tokens */
object JadzumiMotion {
    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedEntranceEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val ExitEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val PhysicalGeometryEasing = EmphasizedEntranceEasing

    const val ButtonPressDurationMs = 120
    const val TouchFeedbackDurationMs = 120
    const val PlayPauseMorphDurationMs = 220
    const val TabNavDurationMs = 380
    const val FilterSelectDurationMs = 240
    const val PanelDurationMs = 280
    const val ContainerExpansionMs = 340
    const val PosterMorphDurationMs = 480
    const val PlayerDismissDurationMs = 420
    const val EpisodeExpansionMs = 300
    const val InspectorRevealMs = 320
    const val AmbientBlendDurationMs = 550
    const val HudRevealDurationMs = 220
    const val HudHideDurationMs = 180
    const val IconMorphDurationMs = 200

    fun <T> springForPreset(preset: String): SpringSpec<T> {
        return when (preset) {
            "Subtle" -> spring(dampingRatio = 1.00f, stiffness = 600f)
            "Expressive" -> spring(dampingRatio = 0.70f, stiffness = 300f)
            else -> spring(dampingRatio = 0.85f, stiffness = 400f) // Balanced (Default)
        }
    }
}

internal data class MotionStyle(
    val enabled: Boolean = false,
    val preset: String = "Balanced",
    val light: Boolean = true
) {
    val expressive: Boolean get() = preset == "Expressive"
    val damping: Float get() = when (preset) {
        "Subtle" -> 1.00f
        "Expressive" -> 0.70f
        else -> 0.85f
    }
    val stiffness: Float get() = when (preset) {
        "Subtle" -> 600f
        "Expressive" -> 300f
        else -> 400f
    }
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
        val motionPreset = prefs.getString("motionPreset", "Balanced") ?: "Balanced"
        MotionStyle(
            enabled = !reduced && lively && ValueAnimator.areAnimatorsEnabled(),
            preset = motionPreset,
            light = prefs.getBoolean("touchLight", true)
        )
    }
}

/** Observes touches and applies Living Motion press (Scale 1.0 -> 0.96) and release spring. */
internal fun Modifier.touchMotion(): Modifier = composed {
    val style = LocalMotionStyle.current
    if (!style.enabled) return@composed this
    var pressed by remember { mutableStateOf(false) }
    var origin by remember { mutableStateOf(Offset.Zero) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1.0f,
        animationSpec = if (pressed) {
            tween(durationMillis = JadzumiMotion.ButtonPressDurationMs, easing = JadzumiMotion.StandardEasing)
        } else {
            spring(dampingRatio = 0.75f, stiffness = 650f)
        },
        label = "touch feedback scale"
    )

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
        scaleX = scale
        scaleY = scale
    }.drawWithContent {
        drawContent()
        if (style.light && pressed) {
            drawRect(
                Brush.radialGradient(
                    listOf(tint.copy(alpha = 0.12f), tint.copy(alpha = 0f)),
                    center = origin,
                    radius = size.maxDimension.coerceAtLeast(1f)
                )
            )
        }
    }
}

/** Only unconsumed edge scrolling stretches the viewport; never takes scroll distance from children. */
internal fun Modifier.elasticEdge(): Modifier = composed {
    val style = LocalMotionStyle.current
    if (!style.enabled) return@composed this
    val scope = rememberCoroutineScope()
    var pull by remember { mutableFloatStateOf(0f) }
    val stretch by animateFloatAsState(pull, spring(style.damping, style.stiffness), label = "edge return")
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
    var preset by remember { mutableStateOf(prefs.getString("motionPreset", "Balanced") ?: "Balanced") }
    var lighting by remember { mutableStateOf(prefs.getBoolean("touchLight", true)) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Living Motion Physics",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Subtle (1.00/600 damping/stiffness), Balanced (0.85/400 natural elasticity), Expressive (0.70/300 dynamic bounce)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Subtle", "Balanced", "Expressive").forEach { opt ->
                    val selected = opt == preset
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                preset = opt
                                prefs.edit().putString("motionPreset", opt).apply()
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                opt,
                                fontSize = 12.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
