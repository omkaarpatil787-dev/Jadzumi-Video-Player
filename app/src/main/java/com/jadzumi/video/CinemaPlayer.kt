package com.jadzumi.video

import android.content.Context
import android.graphics.Rect
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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

private val QuickMenuBg = Color(0xFF151B26).copy(alpha = 0.90f)
private val FrostedCircleBg = Color.White.copy(alpha = 0.16f)
private val FrostedCircleRim = Color.White.copy(alpha = 0.30f)
private val TimelineAccent = Color(0xFF8B9DFF)

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun CinemaPlayer(playback: PlaybackController, reducedMotion: Boolean) {
    val video = playback.video ?: return
    val activity = LocalActivity.current as? MainActivity ?: return
    val previousBrightness = remember(activity) { activity.window.attributes.screenBrightness }
    val effectiveDuration = playback.duration.takeIf { it > 0 } ?: video.duration
    val heading = remember(video.name, video.width, video.height, effectiveDuration) {
        formatCleanPlayerTitle(video.name, video.width, video.height, effectiveDuration)
    }

    var controls by remember(video.identity) { mutableStateOf(true) }
    var quickMenuSection by remember(video.identity) { mutableStateOf<String?>(null) }
    var sleepTimerMinutes by remember(video.identity) { mutableIntStateOf(0) }
    var scrub by remember(video.identity) { mutableStateOf<Float?>(null) }
    var message by remember(video.identity) { mutableStateOf<String?>(null) }
    var hudSide by remember(video.identity) { mutableStateOf<String?>(null) }
    var hudValue by remember(video.identity) { mutableFloatStateOf(0f) }
    var interaction by remember(video.identity) { mutableIntStateOf(0) }
    val seekDeltaMs = playback.seekIncrementSeconds * 1000L

    // Working Sleep Timer coroutine
    LaunchedEffect(sleepTimerMinutes, video.identity) {
        if (sleepTimerMinutes > 0) {
            delay(sleepTimerMinutes * 60_000L)
            if (playback.playing) playback.toggle()
            sleepTimerMinutes = 0
            message = "Sleep timer paused playback"
        }
    }

    DisposableEffect(activity) {
        onDispose { activity.videoBounds = null; activity.updatePip() }
    }

    BackHandler(!playback.inPip) {
        if (quickMenuSection != null) {
            quickMenuSection = if (quickMenuSection == "main") null else "main"
        } else if (playback.locked) {
            controls = true; interaction++
        } else {
            playback.close()
        }
    }

    LaunchedEffect(controls, playback.playing, interaction, playback.locked, quickMenuSection, scrub != null) {
        if (controls && playback.playing && quickMenuSection == null && scrub == null) {
            delay(3500)
            controls = false
        }
    }
    LaunchedEffect(message) { if (message != null) { delay(1400); message = null } }
    LaunchedEffect(hudSide, interaction) { if (hudSide != null) { delay(1200); hudSide = null } }
    LaunchedEffect(playback.inPip) {
        if (playback.inPip) { quickMenuSection = null; scrub = null; controls = false }
    }

    LaunchedEffect(playback.brightnessBoost) {
        if (playback.brightnessBoost) {
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = 1.0f }
        }
        activity.updateHdrColorMode()
    }

    DisposableEffect(playback.inPip) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (!playback.inPip) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = previousBrightness }
            activity.updateHdrColorMode()
        }
    }

    LaunchedEffect(playback.playing, playback.playRequested, playback.seekable, playback.aspect, playback.resizeMode) {
        activity.updatePip()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setKeepContentOnPlayerReset(true)
                }
            },
            modifier = Modifier.fillMaxSize().onGloballyPositioned {
                val b = it.boundsInWindow()
                activity.videoBounds = Rect(b.left.roundToInt(), b.top.roundToInt(), b.right.roundToInt(), b.bottom.roundToInt())
                activity.updatePip()
            },
            update = { view ->
                if (view.player !== playback.player) view.player = playback.player
                view.resizeMode = playback.resizeMode
                view.keepScreenOn = playback.playRequested
                SubtitlePreferences.applyToPlayerView(view.context, view)
            },
            onReset = null,
            onRelease = { it.player = null }
        )

            if (!playback.inPip) {
                val audio = remember { activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
                Box(
                    Modifier.fillMaxSize()
                        .pointerInput(video.identity, playback.locked, seekDeltaMs) {
                            detectTapGestures(
                                onTap = {
                                    if (quickMenuSection != null) quickMenuSection = null
                                    else controls = !controls
                                    interaction++
                                },
                                onDoubleTap = { offset ->
                                    if (!playback.locked) {
                                        val isLeft = offset.x < size.width / 2
                                        playback.skip(if (isLeft) -seekDeltaMs else seekDeltaMs)
                                        message = if (isLeft) "−${playback.seekIncrementSeconds}s" else "+${playback.seekIncrementSeconds}s"
                                        interaction++
                                    }
                                }
                            )
                        }
                        .pointerInput(video.identity, playback.locked, playback.swipeGesturesEnabled) {
                            var left = false; var level = 0f; var isGestureValid = false
                            detectVerticalDragGestures(
                                onDragStart = { offset ->
                                    val inVerticalZone = offset.y >= size.height * 0.18f && offset.y <= size.height * 0.82f
                                    val inLeftZone = offset.x <= size.width * 0.38f
                                    val inRightZone = offset.x >= size.width * 0.62f
                                    isGestureValid = inVerticalZone && (inLeftZone || inRightZone)
                                    if (isGestureValid) {
                                        left = inLeftZone
                                        val maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                        level = if (left) activity.window.attributes.screenBrightness.takeIf { it >= 0 } ?: 0.5f
                                        else audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol
                                    }
                                },
                                onDragEnd = { hudSide = null; isGestureValid = false },
                                onDragCancel = { hudSide = null; isGestureValid = false }
                            ) { change, amount ->
                                if (isGestureValid && !playback.locked && playback.swipeGesturesEnabled) {
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
                    CircularProgressIndicator(Modifier.align(Alignment.Center).size(42.dp), color = Color.White, strokeWidth = 2.5.dp)
                }

                // Horizontal Swipe Brightness / Volume Capsule HUD
                hudSide?.let { side ->
                    val isBright = side == "Brightness"
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .safeDrawingPadding()
                            .padding(top = 74.dp),
                        color = QuickMenuBg,
                        border = BorderStroke(1.dp, FrostedCircleRim),
                        shape = RoundedCornerShape(999.dp),
                        shadowElevation = 16.dp
                    ) {
                        Row(
                            Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                if (isBright) Icons.Rounded.Brightness6 else Icons.Rounded.VolumeUp,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                side,
                                color = Color.White.copy(alpha = 0.90f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            LinearProgressIndicator(
                                progress = { hudValue.coerceIn(0f, 1f) },
                                modifier = Modifier.width(110.dp).height(5.dp).clip(CircleShape),
                                color = TimelineAccent,
                                trackColor = Color.White.copy(alpha = 0.22f)
                            )
                            Text(
                                "${(hudValue * 100).roundToInt()}%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 34.dp)
                            )
                        }
                    }
                }

                val enterAnim = if (reducedMotion) EnterTransition.None else fadeIn(spring(stiffness = Spring.StiffnessMediumLow))
                val exitAnim = if (reducedMotion) ExitTransition.None else fadeOut(tween(180))

                AnimatedVisibility(
                    visible = controls && !playback.locked,
                    enter = enterAnim,
                    exit = exitAnim
                ) {
                    val subtleScrim = Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.68f),
                        0.22f to Color.Transparent,
                        0.76f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.78f)
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(subtleScrim)
                            .safeDrawingPadding()
                            .padding(horizontal = 24.dp, vertical = 18.dp)
                    ) {
                        // PANEL 1 & 4: CINEMATIC TOP BAR
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
                                FrostedCircleButton("Back to library", playback::close, size = 44.dp) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Color.White)
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        heading.primaryTitle,
                                        color = Color.White,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    heading.secondaryMeta?.let { meta ->
                                        Text(
                                            meta,
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Top-Right Icons: Clean and uncluttered glassy controls
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                FrostedCircleButton("Audio and subtitles", { quickMenuSection = "subs"; interaction++ }, size = 44.dp) {
                                    Icon(Icons.Rounded.ClosedCaption, null, Modifier.size(22.dp), tint = Color.White)
                                }
                                FrostedCircleButton("Cinema settings", { quickMenuSection = "main"; interaction++ }, size = 44.dp) {
                                    Icon(Icons.Rounded.Tune, null, Modifier.size(22.dp), tint = Color.White)
                                }
                            }
                        }

                        // PANEL 1: CENTRAL FROSTED-GLASS CIRCULAR PLAY/PAUSE & 10s SKIP CONTROLS
                        Row(
                            Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(34.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FrostedCircleButton(
                                label = "Back ${playback.seekIncrementSeconds} seconds",
                                onClick = { playback.skip(-seekDeltaMs); interaction++ },
                                enabled = playback.seekable,
                                size = 54.dp
                            ) {
                                Icon(Icons.Rounded.Replay10, null, Modifier.size(26.dp), tint = Color.White)
                            }

                            FrostedCircleButton(
                                label = if (playback.playRequested) "Pause" else "Play",
                                onClick = { playback.toggle(); interaction++ },
                                size = 82.dp
                            ) {
                                PlayPauseMorph(playback.playRequested, reducedMotion, Modifier.size(36.dp))
                            }

                            FrostedCircleButton(
                                label = "Forward ${playback.seekIncrementSeconds} seconds",
                                onClick = { playback.skip(seekDeltaMs); interaction++ },
                                enabled = playback.seekable,
                                size = 54.dp
                            ) {
                                Icon(Icons.Rounded.Forward10, null, Modifier.size(26.dp), tint = Color.White)
                            }
                        }

                        // PANEL 1 & 3: INLINE TIMELINE ROW + MINIMALIST BOTTOM CONTROLS
                        Column(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val currentFraction = scrub ?: if (playback.duration > 0) {
                                (playback.position.toFloat() / playback.duration).coerceIn(0f, 1f)
                            } else 0f
                            val bufferedPos = playback.player?.bufferedPosition ?: 0L
                            val bufferedFraction = if (playback.duration > 0) {
                                (bufferedPos.toFloat() / playback.duration).coerceIn(0f, 1f)
                            } else 0f

                            // Edge-Clamped Scrubbing Thumbnail Preview (Panel 3)
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                val totalW = maxWidth
                                scrub?.let { frac ->
                                    val cardW = 164.dp
                                    val rawX = (totalW * frac) - (cardW / 2)
                                    val safeX = rawX.coerceIn(0.dp, (totalW - cardW).coerceAtLeast(0.dp))
                                    Box(Modifier.offset(x = safeX).padding(bottom = 4.dp)) {
                                        ScrubPreview(video, (frac * playback.duration).toLong())
                                    }
                                }
                            }

                            // Inline Timeline Row
                            val activePosMs = scrub?.let { (it * playback.duration).toLong() } ?: playback.position
                            val elapsedSec = (activePosMs / 1000).coerceAtLeast(0)
                            val totalSec = (playback.duration / 1000).coerceAtLeast(0)

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

                            // Bottom Row: Glass Time Capsule (Left) & Glass Actions Dock (Right)
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Bottom-Left: Glassy Time Capsule Pill
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = FrostedCircleBg,
                                    border = BorderStroke(1.dp, FrostedCircleRim)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            compactTimecode(elapsedSec),
                                            color = TimelineAccent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text("/", color = Color.White.copy(alpha = 0.40f), fontSize = 12.sp)
                                        Text(
                                            compactTimecode(totalSec),
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        val remSec = (totalSec - elapsedSec).coerceAtLeast(0)
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = Color.White.copy(alpha = 0.14f),
                                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                                        ) {
                                            Text(
                                                "-${compactTimecode(remSec)} remaining",
                                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                color = Color.White.copy(alpha = 0.92f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // Bottom-Right: Glassy Actions Dock
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = FrostedCircleBg,
                                    border = BorderStroke(1.dp, FrostedCircleRim)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        CinemaIcon("Lock controls", { playback.locked = true; controls = false }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Rounded.LockOpen, null, Modifier.size(19.dp))
                                        }
                                        if (activity.supportsPip()) {
                                            CinemaIcon("Picture in picture", { if (!activity.enterCinemaPip()) message = "PiP unavailable" }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Rounded.PictureInPictureAlt, null, Modifier.size(19.dp))
                                            }
                                        }
                                        CinemaIcon("Close player", playback::close, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Rounded.Close, null, Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Unlock Button when Screen Locked
                if (playback.locked && controls) {
                    FrostedCircleButton(
                        label = "Unlock controls",
                        onClick = { playback.locked = false; controls = true; interaction++ },
                        modifier = Modifier.align(Alignment.CenterStart).safeDrawingPadding().padding(start = 24.dp),
                        size = 50.dp
                    ) {
                        Icon(Icons.Rounded.Lock, null, tint = Color.White)
                    }
                }

                // PANEL 5: RIGHT-SIDE QUICK MENU
                AnimatedVisibility(
                    visible = quickMenuSection != null,
                    enter = fadeIn(tween(if (reducedMotion) 0 else 160)) + slideInHorizontally(tween(if (reducedMotion) 0 else 200)) { it / 3 },
                    exit = fadeOut(tween(if (reducedMotion) 0 else 140)) + slideOutHorizontally(tween(if (reducedMotion) 0 else 160)) { it / 3 }
                ) {
                    quickMenuSection?.let { section ->
                        RightSideQuickMenu(
                            playback = playback,
                            section = section,
                            sleepTimerMinutes = sleepTimerMinutes,
                            onSelectSection = { quickMenuSection = it; interaction++ },
                            onSleepTimer = { mins ->
                                sleepTimerMinutes = mins
                                message = if (mins > 0) "Sleep timer: $mins min" else "Sleep timer: Off"
                                interaction++
                            },
                            onMessage = { message = it; interaction++ },
                            onDismiss = { quickMenuSection = null }
                        )
                    }
                }

                message?.let { msg ->
                    Surface(
                        Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = 74.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = QuickMenuBg,
                        border = BorderStroke(1.dp, FrostedCircleRim)
                    ) {
                        Text(msg, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                playback.error?.let { error ->
                    Surface(
                        Modifier.align(Alignment.Center).padding(24.dp).widthIn(max = 520.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = QuickMenuBg,
                        border = BorderStroke(1.dp, FrostedCircleRim)
                    ) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(error, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = playback::retry) { Text("Retry") }
                                OutlinedButton(onClick = playback::close) { Text("Close", color = Color.White) }
                            }
                        }
                    }
                }
            }
        }
    }

/** Panel 1 & 3: Ultra-Thin Periwinkle-Blue Timeline with Prominent White Circular Thumb */
@Composable private fun CinemaTimelineBar(
    fraction: Float,
    bufferedFraction: Float,
    isScrubbing: Boolean,
    enabled: Boolean,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit
) {
    val trackHeight by animateDpAsState(if (isScrubbing) 5.dp else 3.dp, label = "trackHeight")
    val thumbRadius by animateDpAsState(if (isScrubbing) 8.5.dp else 6.5.dp, label = "thumbRadius")
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
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
        Canvas(Modifier.fillMaxWidth().height(26.dp)) {
            val h = with(density) { trackHeight.toPx() }
            val r = with(density) { thumbRadius.toPx() }
            val yCenter = size.height / 2f
            val top = yCenter - h / 2f
            val corner = CornerRadius(h / 2f, h / 2f)

            // Remaining Track
            drawRoundRect(
                color = Color.White.copy(alpha = 0.24f),
                topLeft = Offset(0f, top),
                size = Size(size.width, h),
                cornerRadius = corner
            )
            // Buffered Track
            if (bufferedFraction > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.40f),
                    topLeft = Offset(0f, top),
                    size = Size(size.width * bufferedFraction.coerceIn(0f, 1f), h),
                    cornerRadius = corner
                )
            }
            // Played Track (Soft Periwinkle-Blue matching Panel 1 & 3)
            val activeW = size.width * fraction.coerceIn(0f, 1f)
            if (activeW > 0f) {
                drawRoundRect(
                    color = TimelineAccent,
                    topLeft = Offset(0f, top),
                    size = Size(activeW, h),
                    cornerRadius = corner
                )
            }
            // Crisp White Circular Thumb
            if (enabled) {
                val thumbX = activeW.coerceIn(r, (size.width - r).coerceAtLeast(r))
                if (isScrubbing) {
                    drawCircle(
                        color = TimelineAccent.copy(alpha = 0.38f),
                        radius = r * 1.7f,
                        center = Offset(thumbX, yCenter)
                    )
                }
                drawCircle(
                    color = Color.White,
                    radius = r,
                    center = Offset(thumbX, yCenter)
                )
            }
        }
    }
}

/** Panel 5: Right-Side Floating Translucent Quick Menu */
@Composable private fun RightSideQuickMenu(
    playback: PlaybackController,
    section: String,
    sleepTimerMinutes: Int,
    onSelectSection: (String) -> Unit,
    onSleepTimer: (Int) -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val activeAudio = playback.audioTracks.firstOrNull { it.isSelected }
    val audioSummary = activeAudio?.let { "${it.label} (${it.codec} ${it.channels})" } ?: "Default"
    val activeSub = playback.subtitleTracks.firstOrNull { it.isSelected }
    val subSummary = activeSub?.let { "${it.label} (${it.format})" } ?: "Off"
    val sleepSummary = if (sleepTimerMinutes > 0) "${sleepTimerMinutes}m" else "Off"

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)).clickable(onClick = onDismiss)) {
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .safeDrawingPadding()
                .padding(end = 24.dp, top = 24.dp, bottom = 24.dp)
                .widthIn(min = 300.dp, max = 350.dp)
                .clickable(enabled = false) {},
            color = QuickMenuBg,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 24.dp
        ) {
            when (section) {
                "main" -> {
                    Column(Modifier.padding(vertical = 10.dp)) {
                        QuickMenuRow(
                            icon = Icons.Rounded.Speed,
                            label = "Playback speed",
                            value = "${playback.speed}x",
                            onClick = { onSelectSection("speed") }
                        )
                        val fitName = when (playback.resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Crop to fill"
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
                            else -> "Fit to screen"
                        }
                        QuickMenuRow(
                            icon = Icons.Rounded.AspectRatio,
                            label = "Fit / Aspect ratio",
                            value = fitName,
                            onClick = { onSelectSection("aspect") }
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.Timer,
                            label = "Sleep timer",
                            value = sleepSummary,
                            onClick = { onSelectSection("sleep") }
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.GraphicEq,
                            label = "Audio track",
                            value = audioSummary,
                            onClick = { onSelectSection("audio") }
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.Subtitles,
                            label = "Subtitle track",
                            value = subSummary,
                            onClick = { onSelectSection("subs") }
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.Settings,
                            label = "More settings",
                            value = "",
                            onClick = { onSelectSection("more") }
                        )
                    }
                }

                "speed" -> {
                    QuickSubMenuContainer(title = "Playback speed", onBack = { onSelectSection("main") }) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { rate ->
                            QuickOptionItem(
                                label = if (rate == 1.0f) "1.0x (Normal)" else "${rate}x",
                                selected = playback.speed == rate,
                                onClick = { playback.changeSpeed(rate); onMessage("Speed: ${rate}x"); onSelectSection("main") }
                            )
                        }
                    }
                }

                "aspect" -> {
                    QuickSubMenuContainer(title = "Fit / Aspect ratio", onBack = { onSelectSection("main") }) {
                        listOf(
                            AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit to screen (Letterbox)",
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Crop to fill (Zoom 16:10)",
                            AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch to fill"
                        ).forEach { (mode, label) ->
                            QuickOptionItem(
                                label = label,
                                selected = playback.resizeMode == mode,
                                onClick = {
                                    playback.resizeMode = mode
                                    onMessage("Aspect: $label")
                                    onSelectSection("main")
                                }
                            )
                        }
                    }
                }

                "sleep" -> {
                    QuickSubMenuContainer(title = "Sleep timer", onBack = { onSelectSection("main") }) {
                        listOf(0 to "Off", 15 to "15 minutes", 30 to "30 minutes", 60 to "60 minutes").forEach { (mins, label) ->
                            QuickOptionItem(
                                label = label,
                                selected = sleepTimerMinutes == mins,
                                onClick = { onSleepTimer(mins); onSelectSection("main") }
                            )
                        }
                    }
                }

                "audio" -> {
                    QuickSubMenuContainer(title = "Audio track", onBack = { onSelectSection("main") }) {
                        if (playback.audioTracks.isEmpty()) {
                            Text("Default audio stream active", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                        } else {
                            playback.audioTracks.forEach { track ->
                                QuickOptionItem(
                                    label = "${track.label} (${track.codec} · ${track.channels})",
                                    selected = track.isSelected,
                                    onClick = { playback.selectAudioTrack(track); onMessage("Audio: ${track.label}"); onSelectSection("main") }
                                )
                            }
                        }
                    }
                }

                "subs" -> {
                    QuickSubMenuContainer(title = "Subtitle track", onBack = { onSelectSection("main") }) {
                        val noneSelected = playback.subtitleTracks.none { it.isSelected }
                        QuickOptionItem(
                            label = "Off",
                            selected = noneSelected,
                            onClick = { playback.selectSubtitleTrack(null); onMessage("Subtitles Off"); onSelectSection("main") }
                        )
                        playback.subtitleTracks.forEach { track ->
                            QuickOptionItem(
                                label = "${track.label} (${track.format})",
                                selected = track.isSelected,
                                onClick = { playback.selectSubtitleTrack(track); onMessage("Subtitles: ${track.label}"); onSelectSection("main") }
                            )
                        }
                    }
                }

                "more" -> {
                    QuickSubMenuContainer(title = "More settings", onBack = { onSelectSection("main") }) {
                        val fitName = when (playback.resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Crop to fill"
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
                            else -> "Fit to screen"
                        }
                        QuickMenuRow(
                            icon = Icons.Rounded.AspectRatio,
                            label = "Video scaling",
                            value = fitName,
                            onClick = {
                                playback.resizeMode = when (playback.resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            }
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.Headphones,
                            label = "Background audio",
                            value = if (playback.backgroundAudioEnabled) "On" else "Off",
                            onClick = { playback.setBackgroundAudio(!playback.backgroundAudioEnabled) }
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.BrightnessMedium,
                            label = "Brightness Boost (HDR)",
                            value = if (playback.brightnessBoost) "On (100%)" else "Off",
                            onClick = {
                                playback.updateBrightnessBoost(!playback.brightnessBoost)
                                onMessage(if (playback.brightnessBoost) "Brightness Boost: On (100%)" else "Brightness Boost: Off")
                            }
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.Replay,
                            label = "Restart from beginning",
                            value = "",
                            onClick = { playback.seekTo(0); onDismiss() }
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.Close,
                            label = "Close player",
                            value = "",
                            onClick = { playback.close(); onDismiss() }
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun QuickMenuRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.wrapContentWidth()
        ) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                value,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable private fun QuickSubMenuContainer(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.padding(vertical = 10.dp).heightIn(max = 340.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onBack).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        LazyColumn(content = { item { Column(content = content) } })
    }
}

@Composable private fun QuickOptionItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = if (selected) TimelineAccent else Color.White,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Rounded.Check, null, tint = TimelineAccent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable private fun FrostedCircleButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(size).semantics { contentDescription = label },
        shape = CircleShape,
        color = if (enabled) FrostedCircleBg else FrostedCircleBg.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, if (enabled) FrostedCircleRim else FrostedCircleRim.copy(alpha = 0.10f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
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
        contentColor = Color.White.copy(alpha = 0.90f),
        disabledContentColor = Color.White.copy(alpha = 0.30f)
    )
    IconButton(
        onClick = action,
        modifier = modifier.size(42.dp).semantics { contentDescription = label },
        enabled = enabled,
        colors = colors
    ) {
        icon()
    }
}
// END OF FILE
