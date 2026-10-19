package com.jadzumi.video

import android.content.Context
import android.graphics.Rect
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val GlassSurface = Color(0xFF0D1321).copy(alpha = 0.78f)
private val GlassPillBg = Color.White.copy(alpha = 0.13f)
private val GlassPillBorder = Color.White.copy(alpha = 0.16f)
private val JadzumiPrimary = Color(0xFF1A73E8)
private val SkyAccent = Color(0xFF70B5FF)

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun CinemaPlayer(playback: PlaybackController, reducedMotion: Boolean) {
    val video = playback.video ?: return
    val activity = LocalActivity.current as? MainActivity ?: return
    val previousBrightness = remember(activity) { activity.window.attributes.screenBrightness }
    val heading = remember(video.name, video.width, video.height) {
        formatCleanPlayerTitle(video.name, video.width, video.height)
    }

    var controls by remember(video.identity) { mutableStateOf(true) }
    var quickDrawerTab by remember(video.identity) { mutableStateOf<String?>(null) }
    var scrub by remember(video.identity) { mutableStateOf<Float?>(null) }
    var message by remember(video.identity) { mutableStateOf<String?>(null) }
    var hudSide by remember(video.identity) { mutableStateOf<String?>(null) }
    var hudValue by remember(video.identity) { mutableFloatStateOf(0f) }
    var interaction by remember(video.identity) { mutableIntStateOf(0) }
    val seekDeltaMs = playback.seekIncrementSeconds * 1000L

    DisposableEffect(activity) {
        onDispose { activity.videoBounds = null; activity.updatePip() }
    }
    val full = playback.fullscreen || playback.inPip
    BackHandler(full && !playback.inPip) {
        if (quickDrawerTab != null) quickDrawerTab = null
        else if (playback.locked) { controls = true; interaction++ }
        else playback.fullscreen = false
    }

    LaunchedEffect(controls, playback.playing, interaction, playback.locked, quickDrawerTab, scrub != null) {
        if (controls && playback.playing && quickDrawerTab == null && scrub == null) {
            delay(3500)
            controls = false
        }
    }
    LaunchedEffect(message) { if (message != null) { delay(1400); message = null } }
    LaunchedEffect(hudSide, interaction) { if (hudSide != null) { delay(1200); hudSide = null } }
    LaunchedEffect(full) { controls = true; interaction++ }
    LaunchedEffect(playback.inPip) {
        if (playback.inPip) { quickDrawerTab = null; scrub = null; controls = false }
    }

    DisposableEffect(full, playback.inPip) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (full && !playback.inPip) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = previousBrightness }
        }
    }

    LaunchedEffect(playback.playing, playback.playRequested, playback.seekable, playback.aspect, playback.resizeMode, full) {
        activity.updatePip()
    }

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = if (full) Alignment.Center else Alignment.BottomEnd) {
        val miniW = (maxWidth * 0.42f).coerceIn(240.dp, 340.dp)
        val frame = if (full) Modifier.fillMaxSize()
        else Modifier.safeDrawingPadding().padding(end = 20.dp, bottom = 92.dp).width(miniW).aspectRatio(16f / 9f).clip(RoundedCornerShape(24.dp))

        Box(frame.background(Color.Black)) {
            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { useController = false; setKeepContentOnPlayerReset(true) } },
                modifier = Modifier.fillMaxSize().onGloballyPositioned {
                    val b = it.boundsInWindow()
                    activity.videoBounds = Rect(b.left.roundToInt(), b.top.roundToInt(), b.right.roundToInt(), b.bottom.roundToInt())
                    activity.updatePip()
                },
                update = { view ->
                    if (view.player !== playback.player) view.player = playback.player
                    view.resizeMode = playback.resizeMode
                    view.keepScreenOn = playback.playRequested
                },
                onReset = null,
                onRelease = { it.player = null }
            )

            if (!playback.inPip) {
                val audio = remember { activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
                Box(
                    Modifier.fillMaxSize()
                        .pointerInput(video.identity, playback.locked, full, seekDeltaMs) {
                            detectTapGestures(
                                onTap = {
                                    if (!full) playback.fullscreen = true
                                    else if (quickDrawerTab != null) quickDrawerTab = null
                                    else controls = !controls
                                    interaction++
                                },
                                onDoubleTap = { offset ->
                                    if (!playback.locked && full) {
                                        val isLeft = offset.x < size.width / 2
                                        playback.skip(if (isLeft) -seekDeltaMs else seekDeltaMs)
                                        message = if (isLeft) "−${playback.seekIncrementSeconds} sec" else "+${playback.seekIncrementSeconds} sec"
                                        interaction++
                                    }
                                }
                            )
                        }
                        .pointerInput(video.identity, playback.locked, full, playback.swipeGesturesEnabled) {
                            var left = false; var level = 0f
                            detectVerticalDragGestures(
                                onDragStart = { offset ->
                                    left = offset.x < size.width / 2
                                    val maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                    level = if (left) activity.window.attributes.screenBrightness.takeIf { it >= 0 } ?: 0.5f
                                    else audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol
                                },
                                onDragEnd = { hudSide = null }
                            ) { change, amount ->
                                if (!playback.locked && full && playback.swipeGesturesEnabled) {
                                    change.consume()
                                    level = (level - amount / size.height.coerceAtLeast(1)).coerceIn(if (left) 0.05f else 0f, 1f)
                                    if (left) activity.window.attributes = activity.window.attributes.apply { screenBrightness = level }
                                    else audio.setStreamVolume(AudioManager.STREAM_MUSIC, (level * audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(), 0)
                                    hudSide = if (left) "Brightness" else "Volume"
                                    hudValue = level
                                    interaction++
                                }
                            }
                        }
                )

                if (playback.buffering) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center).size(44.dp), color = SkyAccent, strokeWidth = 3.dp)
                }

                // Side Gesture HUD Indicators (Matching jadzumi_tablet_cinema_player_refined.html)
                hudSide?.let { side ->
                    val isBright = side == "Brightness"
                    Surface(
                        modifier = Modifier
                            .align(if (isBright) Alignment.CenterStart else Alignment.CenterEnd)
                            .safeDrawingPadding()
                            .padding(horizontal = 28.dp),
                        color = GlassSurface,
                        border = BorderStroke(1.dp, GlassPillBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                if (isBright) Icons.Rounded.Brightness6 else Icons.Rounded.VolumeUp,
                                null,
                                tint = if (isBright) Color(0xFFFCD34D) else SkyAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(Modifier.width(104.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(side, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${(hudValue * 100).roundToInt()}%", color = if (isBright) Color(0xFFFCD34D) else SkyAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { hudValue.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                    color = if (isBright) Color(0xFFFBBF24) else JadzumiPrimary,
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }

                if (full) {
                    val enterAnim = if (reducedMotion) EnterTransition.None else fadeIn(spring(stiffness = Spring.StiffnessMediumLow))
                    val exitAnim = if (reducedMotion) ExitTransition.None else fadeOut(tween(180))

                    AnimatedVisibility(
                        visible = controls && !playback.locked,
                        enter = enterAnim,
                        exit = exitAnim
                    ) {
                        val vignette = Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.78f),
                            0.24f to Color.Transparent,
                            0.74f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.86f)
                        )
                        Box(Modifier.fillMaxSize().background(vignette).safeDrawingPadding().padding(horizontal = 24.dp, vertical = 18.dp)) {
                            // 1. CINEMATIC TOP CONTROLS
                            Row(
                                Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    Modifier.weight(1f).padding(end = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    GlassCircleButton("Back to library", { playback.fullscreen = false }, size = 46.dp) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(22.dp), tint = Color.White)
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            heading.primaryTitle,
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        heading.secondaryMeta?.let { meta ->
                                            Text(
                                                meta,
                                                color = Color.White.copy(alpha = 0.72f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Top-Right Floating Pill Cluster
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    GlassPillButton(onClick = { quickDrawerTab = "audio"; interaction++ }) {
                                        Icon(Icons.Rounded.Subtitles, null, Modifier.size(18.dp), tint = SkyAccent)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Audio & Subs", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                                    }

                                    val aspectLabel = when (playback.resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Crop"
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
                                        else -> "Fit"
                                    }
                                    GlassPillButton(onClick = {
                                        playback.resizeMode = when (playback.resizeMode) {
                                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        }
                                        val newLabel = when (playback.resizeMode) {
                                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Crop to fill"
                                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
                                            else -> "Fit to screen"
                                        }
                                        message = "Aspect: $newLabel"
                                        interaction++
                                    }) {
                                        Icon(Icons.Rounded.AspectRatio, null, Modifier.size(18.dp), tint = Color.White.copy(alpha = 0.9f))
                                        Spacer(Modifier.width(5.dp))
                                        Text(aspectLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                                    }

                                    GlassPillButton(onClick = { quickDrawerTab = "options"; interaction++ }) {
                                        Icon(Icons.Rounded.Speed, null, Modifier.size(17.dp), tint = Color.White.copy(alpha = 0.9f))
                                        Spacer(Modifier.width(4.dp))
                                        Text("${playback.speed}×", color = SkyAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }

                                    GlassCircleButton("Player settings", { quickDrawerTab = "options"; interaction++ }, size = 44.dp) {
                                        Icon(Icons.Rounded.Tune, null, Modifier.size(20.dp), tint = Color.White)
                                    }
                                }
                            }

                            // 2. CENTRAL PLAYBACK CONTROLS (Balanced Skip Pills + Hero Squircle Play/Pause)
                            Row(
                                Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(32.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassCircleButton(
                                    label = "Back ${playback.seekIncrementSeconds} seconds",
                                    onClick = { playback.skip(-seekDeltaMs); interaction++ },
                                    enabled = playback.seekable,
                                    size = 54.dp
                                ) {
                                    Icon(Icons.Rounded.Replay10, null, Modifier.size(28.dp), tint = Color.White)
                                }

                                Surface(
                                    onClick = { playback.toggle(); interaction++ },
                                    modifier = Modifier.size(72.dp).semantics { contentDescription = if (playback.playRequested) "Pause" else "Play" },
                                    shape = RoundedCornerShape(24.dp),
                                    color = JadzumiPrimary.copy(alpha = 0.90f),
                                    border = BorderStroke(1.dp, SkyAccent.copy(alpha = 0.45f)),
                                    shadowElevation = 12.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        PlayPauseMorph(playback.playRequested, reducedMotion, Modifier.size(36.dp))
                                    }
                                }

                                GlassCircleButton(
                                    label = "Forward ${playback.seekIncrementSeconds} seconds",
                                    onClick = { playback.skip(seekDeltaMs); interaction++ },
                                    enabled = playback.seekable,
                                    size = 54.dp
                                ) {
                                    Icon(Icons.Rounded.Forward10, null, Modifier.size(28.dp), tint = Color.White)
                                }
                            }

                            // 3 & 4. ULTRA-THIN TIMELINE & MINIMALIST BOTTOM FOOTER
                            Column(
                                Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val currentFraction = scrub ?: if (playback.duration > 0) {
                                    (playback.position.toFloat() / playback.duration).coerceIn(0f, 1f)
                                } else 0f
                                val bufferedPos = playback.player?.bufferedPosition ?: 0L
                                val bufferedFraction = if (playback.duration > 0) {
                                    (bufferedPos.toFloat() / playback.duration).coerceIn(0f, 1f)
                                } else 0f

                                // Edge-Clamped Scrubbing Thumbnail Preview
                                BoxWithConstraints(Modifier.fillMaxWidth()) {
                                    val trackWidth = maxWidth
                                    scrub?.let { frac ->
                                        val previewWidth = 168.dp
                                        val targetX = (trackWidth * frac) - (previewWidth / 2)
                                        val clampedX = targetX.coerceIn(0.dp, (trackWidth - previewWidth).coerceAtLeast(0.dp))
                                        Box(Modifier.offset(x = clampedX).padding(bottom = 6.dp)) {
                                            ScrubPreview(video, (frac * playback.duration).toLong())
                                        }
                                    }
                                }

                                CinemaTimelineBar(
                                    fraction = currentFraction,
                                    bufferedFraction = bufferedFraction,
                                    isScrubbing = scrub != null,
                                    enabled = playback.seekable && playback.duration > 0,
                                    onScrub = { scrub = it; interaction++ },
                                    onScrubFinished = {
                                        scrub?.let { playback.seekTo((it * playback.duration).toLong()) }
                                        scrub = null
                                    }
                                )

                                // Minimalist Bottom Row: Timestamps + Remaining Pill on Left, Utility Icons on Right
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    val activePosMs = scrub?.let { (it * playback.duration).toLong() } ?: playback.position
                                    val elapsedSec = (activePosMs / 1000).coerceAtLeast(0)
                                    val totalSec = (playback.duration / 1000).coerceAtLeast(0)
                                    val remainingSec = (totalSec - elapsedSec).coerceAtLeast(0)

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            timecode(elapsedSec),
                                            color = SkyAccent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text("/", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                                        Text(
                                            if (playback.duration > 0) timecode(totalSec) else "—",
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (playback.duration > 0) {
                                            Surface(
                                                color = Color.White.copy(alpha = 0.10f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    "−${timecode(remainingSec)} remaining",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.weight(1f))

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        CinemaIcon("Lock controls", { playback.locked = true; controls = false }) {
                                            Icon(Icons.Rounded.LockOpen, null, Modifier.size(20.dp))
                                        }
                                        if (activity.supportsPip()) {
                                            CinemaIcon("Picture in picture", { if (!activity.enterCinemaPip()) message = "PiP unavailable" }) {
                                                Icon(Icons.Rounded.PictureInPictureAlt, null, Modifier.size(20.dp))
                                            }
                                        }
                                        CinemaIcon("Mini-player", { playback.fullscreen = false }) {
                                            Icon(Icons.Rounded.FullscreenExit, null, Modifier.size(22.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating Lock Bubble when Controls Locked
                    if (playback.locked && controls) {
                        GlassCircleButton(
                            label = "Unlock controls",
                            onClick = { playback.locked = false; controls = true; interaction++ },
                            modifier = Modifier.align(Alignment.BottomStart).safeDrawingPadding().padding(start = 24.dp, bottom = 96.dp),
                            size = 50.dp
                        ) {
                            Icon(Icons.Rounded.Lock, null, tint = SkyAccent)
                        }
                    }
                } else {
                    // Mini-Player Overlay
                    Row(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(heading.primaryTitle, Modifier.weight(1f), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                        CinemaIcon(if (playback.playRequested) "Pause" else "Play", { playback.toggle() }) {
                            Icon(if (playback.playRequested) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                        }
                        CinemaIcon("Close mini-player", playback::close) { Icon(Icons.Rounded.Close, null) }
                    }
                }

                // 6. RIGHT-SIDE TRANSLUCENT FLOATING QUICK MENU
                AnimatedVisibility(
                    visible = quickDrawerTab != null,
                    enter = fadeIn(tween(if (reducedMotion) 0 else 180)) + slideInHorizontally(tween(if (reducedMotion) 0 else 220)) { it / 2 },
                    exit = fadeOut(tween(if (reducedMotion) 0 else 150)) + slideOutHorizontally(tween(if (reducedMotion) 0 else 180)) { it / 2 }
                ) {
                    quickDrawerTab?.let { initialTab ->
                        QuickCinemaDrawer(
                            playback = playback,
                            initialTab = initialTab,
                            onMessage = { message = it },
                            onDismiss = { quickDrawerTab = null }
                        )
                    }
                }

                // Top-Center State Badge Pill Toast
                message?.let { msg ->
                    Surface(
                        Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = 78.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = GlassSurface,
                        border = BorderStroke(1.dp, SkyAccent.copy(alpha = 0.35f))
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = SkyAccent, modifier = Modifier.size(16.dp))
                            Text(msg, color = Color(0xFFBAE6FD), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                playback.error?.let { error ->
                    Surface(
                        Modifier.align(Alignment.Center).padding(24.dp).widthIn(max = 520.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = GlassSurface,
                        border = BorderStroke(1.dp, GlassPillBorder)
                    ) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(if (full) error else "Playback unavailable", color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = playback::retry, colors = ButtonDefaults.buttonColors(containerColor = JadzumiPrimary)) { Text("Retry") }
                                OutlinedButton(onClick = playback::close) { Text("Close", color = Color.White) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Custom Ultra-Thin Cinema Timeline Bar (No Vertical Stick Thumb) */
@Composable private fun CinemaTimelineBar(
    fraction: Float,
    bufferedFraction: Float,
    isScrubbing: Boolean,
    enabled: Boolean,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit
) {
    val trackHeight by animateDpAsState(if (isScrubbing) 6.dp else 3.5.dp, label = "trackHeight")
    val thumbRadius by animateDpAsState(if (isScrubbing) 9.dp else 6.5.dp, label = "thumbRadius")
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val f = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    onScrub(f)
                    onScrubFinished()
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val f = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onScrub(f)
                    },
                    onDragEnd = onScrubFinished,
                    onDragCancel = onScrubFinished
                ) { change, _ ->
                    change.consume()
                    val f = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    onScrub(f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val h = with(density) { trackHeight.toPx() }
            val r = with(density) { thumbRadius.toPx() }
            val yCenter = size.height / 2f
            val top = yCenter - h / 2f
            val corner = CornerRadius(h / 2f, h / 2f)

            // Remaining Track
            drawRoundRect(
                color = Color.White.copy(alpha = 0.20f),
                topLeft = Offset(0f, top),
                size = Size(size.width, h),
                cornerRadius = corner
            )
            // Buffered Track
            if (bufferedFraction > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.36f),
                    topLeft = Offset(0f, top),
                    size = Size(size.width * bufferedFraction.coerceIn(0f, 1f), h),
                    cornerRadius = corner
                )
            }
            // Elapsed Active Track
            val activeW = size.width * fraction.coerceIn(0f, 1f)
            if (activeW > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(JadzumiPrimary, SkyAccent)),
                    topLeft = Offset(0f, top),
                    size = Size(activeW, h),
                    cornerRadius = corner
                )
            }
            // Circular Scrubber Thumb with Halo
            if (enabled) {
                val thumbX = activeW.coerceIn(r, (size.width - r).coerceAtLeast(r))
                if (isScrubbing) {
                    drawCircle(
                        color = JadzumiPrimary.copy(alpha = 0.40f),
                        radius = r * 1.8f,
                        center = Offset(thumbX, yCenter)
                    )
                }
                drawCircle(
                    color = Color.White,
                    radius = r,
                    center = Offset(thumbX, yCenter)
                )
                drawCircle(
                    color = JadzumiPrimary,
                    radius = r * 0.45f,
                    center = Offset(thumbX, yCenter)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun QuickCinemaDrawer(
    playback: PlaybackController,
    initialTab: String,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember(initialTab) { mutableStateOf(initialTab) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onDismiss)) {
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .safeDrawingPadding()
                .padding(16.dp)
                .fillMaxHeight()
                .widthIn(min = 320.dp, max = 430.dp)
                .clickable(enabled = false) {},
            color = Color(0xFF0D1321).copy(alpha = 0.92f),
            border = BorderStroke(1.dp, GlassPillBorder),
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 20.dp
        ) {
            Column(Modifier.padding(22.dp).fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Tune, null, tint = SkyAccent)
                        Text("Cinema Controls", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    GlassCircleButton("Close menu", onDismiss, size = 38.dp) {
                        Icon(Icons.Rounded.Close, null, Modifier.size(18.dp), tint = Color.White)
                    }
                }

                Spacer(Modifier.height(14.dp))

                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "audio" to "Audio (${playback.audioTracks.size})",
                            "subs" to "Subtitles (${playback.subtitleTracks.size})",
                            "options" to "Options"
                        ).forEach { (key, label) ->
                            val active = tab == key
                            Surface(
                                onClick = { tab = key },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) JadzumiPrimary else Color.Transparent
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = if (active) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                when (tab) {
                    "audio" -> {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(playback.audioTracks, key = { it.id }) { track ->
                                val bg = if (track.isSelected) JadzumiPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f)
                                val border = if (track.isSelected) JadzumiPrimary else Color.White.copy(alpha = 0.1f)
                                Card(
                                    onClick = { playback.selectAudioTrack(track); onMessage("Audio: ${track.label}") },
                                    colors = CardDefaults.cardColors(containerColor = bg),
                                    border = BorderStroke(1.dp, border),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(if (track.isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Audiotrack, null, tint = if (track.isSelected) SkyAccent else Color.White.copy(alpha = 0.6f))
                                        Column(Modifier.weight(1f)) {
                                            Text(track.label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("${track.codec} • ${track.channels}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                        Surface(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(16.dp), color = Color.Black.copy(alpha = 0.4f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Surround Downmix Engine", color = SkyAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Multi-channel 5.1/7.1 downmix balanced for Xiaomi Pad 5 stereo output.", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }

                    "subs" -> {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                val noneSelected = playback.subtitleTracks.none { it.isSelected }
                                val bg = if (noneSelected) JadzumiPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f)
                                val border = if (noneSelected) JadzumiPrimary else Color.White.copy(alpha = 0.1f)
                                Card(
                                    onClick = { playback.selectSubtitleTrack(null); onMessage("Subtitles Disabled") },
                                    colors = CardDefaults.cardColors(containerColor = bg),
                                    border = BorderStroke(1.dp, border),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(if (noneSelected) Icons.Rounded.CheckCircle else Icons.Rounded.SubtitlesOff, null, tint = if (noneSelected) SkyAccent else Color.White.copy(alpha = 0.6f))
                                        Text("Turn Off Subtitles", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                }
                            }
                            items(playback.subtitleTracks, key = { it.id }) { track ->
                                val bg = if (track.isSelected) JadzumiPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f)
                                val border = if (track.isSelected) JadzumiPrimary else Color.White.copy(alpha = 0.1f)
                                Card(
                                    onClick = { playback.selectSubtitleTrack(track); onMessage("Subtitles: ${track.label}") },
                                    colors = CardDefaults.cardColors(containerColor = bg),
                                    border = BorderStroke(1.dp, border),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(if (track.isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Subtitles, null, tint = if (track.isSelected) SkyAccent else Color.White.copy(alpha = 0.6f))
                                        Column(Modifier.weight(1f)) {
                                            Text(track.label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("${track.language} • ${track.format}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "options" -> {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            item {
                                Text("Playback Speed", color = SkyAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { rate ->
                                        val active = playback.speed == rate
                                        Surface(
                                            onClick = { playback.changeSpeed(rate); onMessage("Speed: ${rate}×") },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (active) JadzumiPrimary else Color.White.copy(alpha = 0.08f),
                                            border = BorderStroke(1.dp, if (active) SkyAccent else Color.White.copy(alpha = 0.12f))
                                        ) {
                                            Text("${rate}×", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            item {
                                Text("Aspect Ratio Mode", color = SkyAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit Screen",
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Crop to Fill",
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch"
                                    ).forEach { (mode, label) ->
                                        val active = playback.resizeMode == mode
                                        Surface(
                                            onClick = { playback.resizeMode = mode; onMessage("Aspect: $label") },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (active) JadzumiPrimary else Color.White.copy(alpha = 0.08f),
                                            border = BorderStroke(1.dp, if (active) SkyAccent else Color.White.copy(alpha = 0.12f))
                                        ) {
                                            Text(label, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.06f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text("Background Audio", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Continue playback when screen is off", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
                                        }
                                        Switch(
                                            checked = playback.backgroundAudioEnabled,
                                            onCheckedChange = {
                                                playback.setBackgroundAudio(it)
                                                onMessage(if (it) "Background audio ON" else "Background audio OFF")
                                            }
                                        )
                                    }
                                }
                            }

                            item {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(
                                        onClick = { playback.seekTo(0); onDismiss() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, GlassPillBorder)
                                    ) {
                                        Icon(Icons.Rounded.Replay, null, Modifier.size(16.dp), tint = Color.White)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Restart", color = Color.White, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { playback.close(); onDismiss() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.5f))
                                    ) {
                                        Icon(Icons.Rounded.Close, null, Modifier.size(16.dp), tint = Color(0xFFFCA5A5))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Close Video", color = Color(0xFFFCA5A5), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = JadzumiPrimary)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable private fun GlassCircleButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(size).semantics { contentDescription = label },
        shape = CircleShape,
        color = if (enabled) GlassPillBg else GlassPillBg.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (enabled) GlassPillBorder else GlassPillBorder.copy(alpha = 0.06f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}

@Composable private fun GlassPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(999.dp),
        color = GlassPillBg,
        border = BorderStroke(1.dp, GlassPillBorder)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable private fun CinemaIcon(
    label: String,
    action: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit
) {
    val colors = IconButtonDefaults.iconButtonColors(
        contentColor = Color.White,
        disabledContentColor = Color.White.copy(alpha = 0.3f)
    )
    IconButton(
        onClick = action,
        modifier = modifier.size(44.dp).semantics { contentDescription = label },
        enabled = enabled,
        colors = colors
    ) {
        icon()
    }
}
// END OF FILE
