package com.jadzumi.video

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class AmbilightMode(val id: String, val displayName: String, val glowAlpha: Float) {
    OFF("off", "Off", 0f),
    SUBTLE("subtle", "Subtle (Calm Ambient)", 0.28f),
    EXPRESSIVE("expressive", "Expressive (Dynamic Halo)", 0.55f);

    companion object {
        fun fromId(id: String?): AmbilightMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SUBTLE
    }
}

/**
 * Atmospheric Screen Ambilight (OLED Edge Glow).
 *
 * Casts a soft, dynamic ambient halo onto the letterbox and pillarbox black bars,
 * creating the sensation that the cinema canvas expands seamlessly beyond the screen bezel.
 */
@Composable
fun CinemaAmbilight(
    mode: AmbilightMode,
    accentColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    if (mode == AmbilightMode.OFF || !isPlaying) return

    val animatedColor by animateColorAsState(
        targetValue = accentColor.copy(alpha = mode.glowAlpha),
        animationSpec = tween(durationMillis = 600),
        label = "ambilight_color"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Top Letterbox Ambient Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            animatedColor,
                            animatedColor.copy(alpha = animatedColor.alpha * 0.45f),
                            Color.Transparent,
                            Color.Transparent,
                            animatedColor.copy(alpha = animatedColor.alpha * 0.45f),
                            animatedColor
                        )
                    )
                )
        )

        // Left / Right Pillarbox Ambient Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            animatedColor.copy(alpha = animatedColor.alpha * 0.65f),
                            Color.Transparent,
                            Color.Transparent,
                            animatedColor.copy(alpha = animatedColor.alpha * 0.65f)
                        )
                    )
                )
        )
    }
}
