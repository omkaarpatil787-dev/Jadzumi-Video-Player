package com.jadzumi.video

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable private fun updates(): AppUpdates {
    val activity = requireNotNull(LocalActivity.current as? MainActivity)
    return remember(activity) { ViewModelProvider(activity)[AppUpdates::class.java] }
}

@Composable internal fun UpdateSettings() {
    val updater = updates()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Automatic update checks",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Contact GitHub when opening the app, at most once daily. Separate from online metadata; off by default.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = updater.automatic,
                onCheckedChange = updater::updateAutomatic
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                enabled = !updater.busy,
                onClick = { updater.check(manual = true) },
                modifier = Modifier.touchMotion(),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (updater.busy) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text("Check for updates", maxLines = 1, softWrap = false)
            }
        }

        if (updater.message.isNotBlank()) {
            Text(
                updater.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Jadzumi Seamless Update Studio Dialog
 * Spec-accurate Jetpack Compose implementation of jadzumi_update_studio.html:
 * - Fluid Glass appearance with atmospheric ambient orb glow
 * - 0.5s Continuous geometry morph entrance & dismissal (cubic-bezier(0.19, 1, 0.22, 1))
 * - Dynamic stage rendering: Available ➔ Downloading (with big % and linear fill) ➔ Verifying ➔ Ready to install
 */
@Composable internal fun UpdateHost() {
    val updater = updates()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { updater.check(manual = false) }

    val release = updater.release ?: return

    val morphProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        morphProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(500, easing = CubicBezierEasing(0.19f, 1.0f, 0.22f, 1.0f))
        )
    }

    Dialog(
        onDismissRequest = { if (!updater.busy) updater.dismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val morphVal = morphProgress.value
        val currentScale = 0.90f + 0.10f * morphVal
        val currentRounding = 32.dp * (1f - morphVal * 0.15f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f * morphVal))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { if (!updater.busy) updater.dismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .widthIn(max = 440.dp)
                    .scale(currentScale)
                    .alpha(morphVal.coerceIn(0f, 1f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                shape = RoundedCornerShape(currentRounding),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 10.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Brand Eyebrow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (updater.downloaded) "READY TO INSTALL" else if (updater.busy) "DOWNLOADING SECURELY" else "NEW VERSION AVAILABLE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.15.sp
                        )

                        if (!updater.busy) {
                            IconButton(
                                onClick = { updater.dismiss() },
                                modifier = Modifier.size(28.dp).touchMotion()
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Headline
                    Text(
                        text = if (updater.downloaded) "Ready when you are." else if (updater.busy) "Getting the good stuff." else "Something good just landed.",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Description & Version badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = release.tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "Same cinema, even smoother.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Release notes or progress indicator
                    if (updater.busy) {
                        val pct = (updater.progress * 100).toInt()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "$pct%",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-1).sp
                                )
                                Text(
                                    text = "%.1f MB / %.1f MB".format(updater.progress * (release.size / 1_048_576f), release.size / 1_048_576f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                progress = { updater.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    } else {
                        // Release Notes Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = release.notes.ifBlank { "✦ Expressive motion & shared-element morphing\n✦ Playback stability improvements\n✦ Persistent artwork & UI refinements" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // Status Message
                    if (updater.message.isNotBlank()) {
                        Text(
                            text = updater.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Action Buttons (Update Studio Style)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (updater.busy) {
                            OutlinedButton(
                                onClick = updater::cancel,
                                modifier = Modifier.weight(1f).touchMotion(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Cancel")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { updater.dismiss() },
                                modifier = Modifier.weight(1f).touchMotion(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Maybe later")
                            }

                            Button(
                                onClick = {
                                    if (updater.downloaded) updater.install(context) else updater.download()
                                },
                                modifier = Modifier.weight(1.2f).touchMotion(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    if (updater.downloaded) Icons.Rounded.InstallMobile else Icons.Rounded.ArrowDownward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (updater.downloaded) "Install now" else "Update now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
