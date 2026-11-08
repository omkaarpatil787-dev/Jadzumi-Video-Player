package com.jadzumi.video

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.media.AudioManager
import android.os.BatteryManager
import java.text.DateFormat
import java.util.Date
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.math.roundToInt

private val QuickMenuBg = Color(0xFF151B26).copy(alpha = 0.90f)
private val FrostedCircleBg = Color.White.copy(alpha = 0.16f)
private val FrostedCircleRim = Color.White.copy(alpha = 0.30f)
internal val TimelineAccent = Color(0xFF8B9DFF)

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun CinemaPlayer(
    playback: PlaybackController,
    reducedMotion: Boolean,
    glassIntensity: Float = 0.75f,
    theme: String = "Dark"
) {
    val video = playback.video ?: return
    if (playback.isDocked) return
    val activity = LocalActivity.current as? MainActivity ?: return
    val coroutineScope = rememberCoroutineScope()
    val isOled = remember { DeviceHardwareInspector.isOledOrAmoledDisplay(activity) }
    val isLight = theme == "Light"
    val isAmoled = theme == "AMOLED" || (isOled && theme != "Light")
    val playerContentColor = if (isLight) Color(0xFF183654) else Color.White
    val playerSubtextColor = if (isLight) Color(0xFF526B85) else Color.White.copy(alpha = 0.75f)
    val playerAccentColor = if (isLight) Color(0xFF0061A4) else TimelineAccent

    val playerPrefs = remember(activity) { activity.getSharedPreferences("player_ui_prefs", Context.MODE_PRIVATE) }
    var isMinimalHud by remember(video.identity) { mutableStateOf(playerPrefs.getBoolean("player_hud_minimal", true)) }

    val dynamicFrostedCircleBg = remember(glassIntensity, isOled, theme) {
        GlassTheme.playerFrostedBg(theme, isOled, glassIntensity)
    }
    val dynamicFrostedCircleRim = remember(glassIntensity, isOled, theme) {
        GlassTheme.playerFrostedRim(theme, isOled, glassIntensity)
    }
    val previousBrightness = remember(activity) { activity.window.attributes.screenBrightness }
    val effectiveDuration = playback.duration.takeIf { it > 0 } ?: video.duration
    val heading = remember(video.name, video.width, video.height, effectiveDuration) {
        formatCleanPlayerTitle(video.name, video.width, video.height, effectiveDuration)
    }

    var controls by remember(video.identity) { mutableStateOf(true) }
    var quickMenuSection by remember(video.identity) { mutableStateOf<String?>(null) }
    var toolsSection by remember(video.identity) { mutableStateOf<String?>(null) }
    var isFastForwarding by remember(video.identity) { mutableStateOf(false) }
    var speedBeforeHold by remember(video.identity) { mutableFloatStateOf(1f) }
    var scrub by remember(video.identity) { mutableStateOf<Float?>(null) }
    var message by remember(video.identity) { mutableStateOf<String?>(null) }
    var hudSide by remember(video.identity) { mutableStateOf<String?>(null) }
    var hudValue by remember(video.identity) { mutableFloatStateOf(0f) }
    var interaction by remember(video.identity) { mutableIntStateOf(0) }
    var seekBackAccumulator by remember(video.identity) { mutableIntStateOf(0) }
    var seekFwdAccumulator by remember(video.identity) { mutableIntStateOf(0) }
    var seekBackJob by remember(video.identity) { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var seekFwdJob by remember(video.identity) { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val seekDeltaMs = playback.seekIncrementSeconds * 1000L
    val totalSec = (effectiveDuration / 1000).coerceAtLeast(0)
    val elapsedSec = (playback.position / 1000).coerceAtLeast(0)
    val remSec = (totalSec - elapsedSec).coerceAtLeast(0)
    val showAnyHud = playback.showPlayerClock || playback.showBatteryPercentage || playback.showRemainingTime

    var subtitleSearchCues by remember(video.identity) { mutableStateOf<List<SubtitleSearchCue>?>(null) }
    var subtitleSearchBusy by remember(video.identity) { mutableStateOf(false) }
    val subtitleSearchLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) coroutineScope.launch {
            subtitleSearchBusy = true
            try {
                val cues = withContext(Dispatchers.IO) {
                    require(uri.scheme == "content" || uri.scheme == "file") { "Only local subtitle files are supported" }
                    val bytes = java.io.ByteArrayOutputStream()
                    requireNotNull(activity.contentResolver.openInputStream(uri)).use { input ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(bytes.size() + count <= SubtitleSearch.MAX_BYTES)
                            bytes.write(buffer, 0, count)
                        }
                    }
                    SubtitleSearch.parse(bytes.toString("UTF-8"))
                }
                if (cues.isEmpty()) message = "No searchable cues. Choose a UTF-8 SRT or VTT file."
                else subtitleSearchCues = cues
            } catch (_: Exception) {
                message = "Cannot search this file. Choose a local UTF-8 SRT/VTT file up to 2 MB."
            } finally { subtitleSearchBusy = false }
        }
    }
    subtitleSearchCues?.let { cues ->
        SubtitleSearchDialog(cues, onSeek = { target ->
            if (playback.seekable) {
                playback.seekToSubtitle(target)
                subtitleSearchCues = null
                message = "Jumped to subtitle line"
            } else message = "This video cannot seek yet"
        }, onDismiss = { subtitleSearchCues = null })
    }
    val subtitleFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val label = runCatching {
                activity.contentResolver.query(it, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull() ?: "External Subtitle"
            playback.addExternalSubtitle(it, label)
            message = "External Subtitle Added"
            interaction++
        }
    }

    var showChaptersDrawer by remember(video.identity) { mutableStateOf(false) }
    var showTrimmerDialog by remember(video.identity) { mutableStateOf(false) }
    var showBingeDialog by remember(video.identity) { mutableStateOf(false) }

    EpisodeContinuationPrompt(playback)
    toolsSection?.let { section ->
        PlayerToolsDialog(section, playback, onMessage = { message = it; interaction++ }, onDismiss = { toolsSection = null })
    }

    var isDismissing by remember { mutableStateOf(false) }
    val playerEnterProgress = remember(video.identity) { Animatable(if (reducedMotion) 1f else 0f) }

    val dismissPlayerWithFluidity: () -> Unit = {
        if (!isDismissing) {
            if (reducedMotion || playback.inPip) {
                playback.close()
            } else {
                isDismissing = true
                coroutineScope.launch {
                    playerEnterProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(360, easing = CubicBezierEasing(0.32f, 0.0f, 0.67f, 0.0f))
                    )
                    playback.close()
                }
            }
        }
    }

    DisposableEffect(activity) {
        onDispose { activity.videoBounds = null; activity.updatePip() }
    }

    BackHandler(!playback.inPip) {
        if (isDismissing) return@BackHandler
        if (quickMenuSection != null) {
            quickMenuSection = if (quickMenuSection == "main") null else "main"
        } else if (playback.locked) {
            controls = true; interaction++
        } else {
            dismissPlayerWithFluidity()
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
        if (playback.inPip) {
            quickMenuSection = null
            scrub = null
            controls = false
        } else {
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(playback.brightnessBoost, playback.isHdr, playback.hdrShadowClarifierLevel, isOled) {
        val clarifier = playback.hdrShadowClarifierLevel
        val shouldBoost = playback.brightnessBoost || playback.isHdr
        if (shouldBoost) {
            val targetBrightness = when {
                playback.brightnessBoost -> 1.0f
                clarifier >= 0.50f -> 1.0f
                clarifier >= 0.25f -> 0.85f
                else -> 0.70f
            }
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = targetBrightness }
        } else {
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = previousBrightness }
        }
        activity.updateHdrColorMode(isOled)
    }

    DisposableEffect(video.identity) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = previousBrightness }
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
            activity.updateHdrColorMode(isOled)
        }
    }

    LaunchedEffect(playback.playing, playback.playRequested, playback.seekable, playback.aspect, playback.aspectMode) {
        if (!playback.inPip) {
            activity.updatePip()
        }
    }

    LaunchedEffect(video.identity) {
        if (!reducedMotion) {
            playerEnterProgress.snapTo(0f)
            playerEnterProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(420, easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f))
            )
        }
    }

    if (playback.inPip) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setKeepContentOnPlayerReset(true)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (view.player !== playback.player) view.player = playback.player
                val contentFrame = view.findViewById<AspectRatioFrameLayout>(androidx.media3.ui.R.id.exo_content_frame)
                val targetRatio = playback.aspectMode.forcedRatio ?: playback.aspect
                if (contentFrame != null) {
                    if (targetRatio > 0f) contentFrame.setAspectRatio(targetRatio)
                    contentFrame.setResizeMode(playback.aspectMode.resizeMode)
                }
                view.resizeMode = playback.aspectMode.resizeMode
                view.keepScreenOn = playback.playRequested
            }
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer {
                if (!playback.inPip) {
                    val p = playerEnterProgress.value
                    alpha = p
                    scaleX = 0.93f + 0.07f * p
                    scaleY = 0.93f + 0.07f * p
                } else {
                    alpha = 1f
                    scaleX = 1f
                    scaleY = 1f
                }
            }
    ) {
        val isPhonePortrait = maxWidth < 600.dp && maxHeight > maxWidth
        val haptic = LocalHapticFeedback.current
        var rippleSide by remember { mutableStateOf<String?>(null) }
        var lastActiveSide by remember { mutableStateOf("left") }
        var rippleCount by remember { mutableIntStateOf(0) }

        LaunchedEffect(rippleSide, rippleCount) {
            if (rippleSide != null) {
                kotlinx.coroutines.delay(650)
                rippleSide = null
                rippleCount = 0
            }
        }

        // True Auto Frame Rate (AFR) matching
        DisposableEffect(playback.matchRefreshRate, playback.playRequested, activity) {
            val previous = activity.window.attributes.preferredDisplayModeId
            if (playback.matchRefreshRate && playback.playRequested) {
                val contentFps = playback.runtimeVideoFormat?.frameRate ?: 24.0
                val match = AutoFrameRateManager.findOptimalDisplayMode(activity, contentFps)
                if (match != null) {
                    AutoFrameRateManager.applyDisplayMode(activity, match)
                    message = "AFR: ${match.description}"
                } else {
                    activity.window.attributes = activity.window.attributes.apply { preferredDisplayModeId = 0 }
                }
            }
            onDispose {
                AutoFrameRateManager.restoreMaxRefreshRate(activity)
            }
        }

        var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

        LaunchedEffect(playback.aspectMode, playback.aspect, playerViewRef) {
            val pv = playerViewRef ?: return@LaunchedEffect
            val targetRatio = playback.aspectMode.forcedRatio ?: playback.aspect
            val contentFrame = pv.findViewById<AspectRatioFrameLayout>(androidx.media3.ui.R.id.exo_content_frame)
            if (contentFrame != null) {
                if (targetRatio > 0f) contentFrame.setAspectRatio(targetRatio)
                contentFrame.setResizeMode(playback.aspectMode.resizeMode)
                contentFrame.requestLayout()
            }
            pv.resizeMode = playback.aspectMode.resizeMode
            pv.requestLayout()
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setKeepContentOnPlayerReset(true)
                }.also { playerViewRef = it }
            },
            modifier = Modifier.fillMaxSize().onGloballyPositioned {
                if (!playback.inPip) {
                    val b = it.boundsInWindow()
                    val newRect = Rect(b.left.roundToInt(), b.top.roundToInt(), b.right.roundToInt(), b.bottom.roundToInt())
                    if (activity.videoBounds != newRect) {
                        activity.videoBounds = newRect
                        activity.updatePip()
                    }
                }
            },
            update = { view ->
                if (view.player !== playback.player) view.player = playback.player
                val contentFrame = view.findViewById<AspectRatioFrameLayout>(androidx.media3.ui.R.id.exo_content_frame)
                val targetRatio = playback.aspectMode.forcedRatio ?: playback.aspect
                if (contentFrame != null) {
                    if (targetRatio > 0f) contentFrame.setAspectRatio(targetRatio)
                    contentFrame.setResizeMode(playback.aspectMode.resizeMode)
                    contentFrame.requestLayout()
                }
                view.resizeMode = playback.aspectMode.resizeMode
                view.keepScreenOn = playback.playRequested
                view.requestLayout()
                SubtitlePreferences.applyToPlayerView(view.context, view)
                if (playback.matchRefreshRate && playback.playRequested) {
                    val contentFps = playback.runtimeVideoFormat?.frameRate ?: 24.0
                    val sv = view.videoSurfaceView as? android.view.SurfaceView
                    AutoFrameRateManager.applySurfaceFrameRate(sv?.holder?.surface, contentFps)
                }
            },
            onReset = null,
            onRelease = {
                val sv = it.videoSurfaceView as? android.view.SurfaceView
                AutoFrameRateManager.clearSurfaceFrameRate(sv?.holder?.surface)
                it.player = null
                playerViewRef = null
            }
        )

        if (playback.dualSubtitleStudio.isSecondaryEnabled) {
            val secCue = playback.dualSubtitleStudio.getActiveCue(playback.position)
            Box(Modifier.fillMaxSize().padding(bottom = 86.dp), contentAlignment = Alignment.BottomCenter) {
                SecondarySubtitleOverlay(text = secCue)
            }
        }

            if (!playback.inPip) {
                val audio = remember { activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
                Box(
                    Modifier.fillMaxSize()
                        .pointerInput(video.identity, playback.locked, seekDeltaMs, playback.playing) {
                            detectTapGestures(
                                onPress = { offset ->
                                    var activated = false
                                    val job = coroutineScope.launch {
                                        delay(320)
                                        if (!playback.locked && playback.playing) {
                                            activated = true
                                            speedBeforeHold = playback.speed
                                            isFastForwarding = true
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            playback.changeSpeed(2.0f)
                                        }
                                    }
                                    val released = tryAwaitRelease()
                                    job.cancel()
                                    if (activated || isFastForwarding) {
                                        isFastForwarding = false
                                        playback.changeSpeed(speedBeforeHold)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onTap = {
                                    if (!isFastForwarding) {
                                        if (quickMenuSection != null) quickMenuSection = null
                                        else controls = !controls
                                        interaction++
                                    }
                                },
                                onDoubleTap = { offset ->
                                    if (!isFastForwarding && !playback.locked) {
                                        val isLeft = offset.x < size.width / 2
                                        val targetSide = if (isLeft) "left" else "right"
                                        lastActiveSide = targetSide
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (rippleSide == targetSide) {
                                            rippleCount++
                                        } else {
                                            rippleSide = targetSide
                                            rippleCount = 1
                                        }
                                        playback.skip(if (isLeft) -seekDeltaMs else seekDeltaMs)
                                        message = if (isLeft) "−${playback.seekIncrementSeconds * rippleCount}s" else "+${playback.seekIncrementSeconds * rippleCount}s"
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
                                    val wasAtMax = level >= 1f
                                    level = (level - amount / size.height.coerceAtLeast(1)).coerceIn(if (left) 0.05f else 0f, 1f)
                                    if (level >= 1f && !wasAtMax) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
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

                // YouTube-Style Double-Tap Fast-Forward / Rewind Pill with Cascading Chevrons
                val activeSide = rippleSide ?: lastActiveSide
                AnimatedVisibility(
                    visible = rippleSide != null,
                    enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.88f),
                    exit = fadeOut(tween(240)) + scaleOut(targetScale = 1.04f),
                    modifier = Modifier.align(if (activeSide == "left") Alignment.CenterStart else Alignment.CenterEnd)
                ) {
                    val isLeft = activeSide == "left"
                    val totalSec = playback.seekIncrementSeconds * rippleCount.coerceAtLeast(1)
                    val infiniteTransition = rememberInfiniteTransition(label = "chevronTransition")
                    val chAlpha1 by infiniteTransition.animateFloat(
                        initialValue = 0.35f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(550, delayMillis = 0), RepeatMode.Reverse),
                        label = "ch1"
                    )
                    val chAlpha2 by infiniteTransition.animateFloat(
                        initialValue = 0.35f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(550, delayMillis = 150), RepeatMode.Reverse),
                        label = "ch2"
                    )
                    val chAlpha3 by infiniteTransition.animateFloat(
                        initialValue = 0.35f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(550, delayMillis = 300), RepeatMode.Reverse),
                        label = "ch3"
                    )

                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 28.dp)
                            .height(38.dp)
                            .wrapContentWidth(),
                        shape = CircleShape,
                        color = dynamicFrostedCircleBg,
                        border = BorderStroke(1.dp, dynamicFrostedCircleRim)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isLeft) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("‹", color = Color.White.copy(alpha = chAlpha1), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("‹", color = Color.White.copy(alpha = chAlpha2), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("‹", color = Color.White.copy(alpha = chAlpha3), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "${totalSec}s",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            } else {
                                Text(
                                    "${totalSec}s",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.width(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("›", color = Color.White.copy(alpha = chAlpha1), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("›", color = Color.White.copy(alpha = chAlpha2), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("›", color = Color.White.copy(alpha = chAlpha3), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Horizontal Swipe Brightness / Volume Capsule HUD with Elastic Rubber-Band Boundary
                hudSide?.let { side ->
                    val isBright = side == "Brightness"
                    val isCeiling = hudValue >= 1f
                    val elasticScaleX by animateFloatAsState(
                        targetValue = if (isCeiling) 1.07f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                        label = "elasticScaleX"
                    )
                    val elasticScaleY by animateFloatAsState(
                        targetValue = if (isCeiling) 0.94f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                        label = "elasticScaleY"
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .safeDrawingPadding()
                            .padding(top = 74.dp)
                            .graphicsLayer {
                                scaleX = elasticScaleX
                                scaleY = elasticScaleY
                            },
                        color = QuickMenuBg,
                        border = BorderStroke(1.dp, if (isCeiling) Color(0xFFF59E0B).copy(alpha = 0.5f) else FrostedCircleRim),
                        shape = RoundedCornerShape(999.dp),
                        shadowElevation = 16.dp
                    ) {
                        Row(
                            Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                if (isBright) Icons.Rounded.Brightness6 else Icons.AutoMirrored.Rounded.VolumeUp,
                                null,
                                tint = if (isCeiling) Color(0xFFF59E0B) else Color.White,
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
                                color = if (isCeiling) Color(0xFFF59E0B) else TimelineAccent,
                                trackColor = Color.White.copy(alpha = 0.22f)
                            )
                            Text(
                                "${(hudValue * 100).roundToInt()}%",
                                color = if (isCeiling) Color(0xFFF59E0B) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 34.dp)
                            )
                        }
                    }
                }

                // 2.0X Fast Forward Floating Frosted Pill
                AnimatedVisibility(
                    visible = isFastForwarding,
                    enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.88f),
                    exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.94f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .safeDrawingPadding()
                        .padding(top = 28.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = QuickMenuBg,
                        border = BorderStroke(1.dp, TimelineAccent.copy(alpha = 0.6f)),
                        shadowElevation = 16.dp
                    ) {
                        Row(
                            Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.FastForward, null, tint = TimelineAccent, modifier = Modifier.size(18.dp))
                            Text("2.0X SPEED", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        }
                    }
                }

                // Immersive Playback HUD Overlay: Clock, Battery %, Remaining Time
                AnimatedVisibility(
                    visible = !controls && !playback.locked && showAnyHud && !playback.inPip,
                    enter = fadeIn(tween(360, easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f))),
                    exit = fadeOut(tween(240)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .safeDrawingPadding()
                        .padding(top = 18.dp, end = 24.dp)
                ) {
                    PlayerStatusPill(
                        showClock = playback.showPlayerClock,
                        showBattery = playback.showBatteryPercentage,
                        showRemainingTime = playback.showRemainingTime,
                        remainingSec = remSec,
                        hdrBadge = playback.hdrFlavor,
                        bgColor = dynamicFrostedCircleBg,
                        rimColor = dynamicFrostedCircleRim,
                        contentColor = playerContentColor,
                        accentColor = playerAccentColor
                    )
                }

                val enterAnim = if (reducedMotion) EnterTransition.None else fadeIn(tween(360, easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)))
                val exitAnim = if (reducedMotion) ExitTransition.None else fadeOut(tween(260))

                AnimatedVisibility(
                    visible = controls && !playback.locked && !playback.inPip,
                    enter = enterAnim,
                    exit = exitAnim
                ) {
                    val subtleScrim = Brush.verticalGradient(
                        0.0f to (if (isLight) Color.Black.copy(alpha = 0.50f) else Color.Black.copy(alpha = 0.68f)),
                        0.22f to Color.Transparent,
                        0.76f to Color.Transparent,
                        1.0f to (if (isLight) Color.Black.copy(alpha = 0.60f) else Color.Black.copy(alpha = 0.78f))
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
                                FrostedCircleButton(
                                    label = "Back to library",
                                    onClick = dismissPlayerWithFluidity,
                                    size = 44.dp,
                                    bgColor = dynamicFrostedCircleBg,
                                    rimColor = dynamicFrostedCircleRim
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = playerContentColor)
                                }
                                FrostedCircleButton(
                                    label = "Dock player",
                                    onClick = { playback.dock() },
                                    size = 44.dp,
                                    bgColor = dynamicFrostedCircleBg,
                                    rimColor = dynamicFrostedCircleRim
                                ) {
                                    Icon(Icons.Rounded.KeyboardArrowDown, "Dock player", Modifier.size(24.dp), tint = playerContentColor)
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        heading.primaryTitle,
                                        color = playerContentColor,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    heading.secondaryMeta?.let { meta ->
                                        Text(
                                            meta,
                                            color = playerSubtextColor,
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
                                if (!isMinimalHud) {
                                    val isVoiceClarityActive = playback.nightModeVoiceBoost
                                    val voiceScale by animateFloatAsState(
                                        targetValue = if (isVoiceClarityActive) 1.08f else 1.0f,
                                        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                                        label = "voiceScale"
                                    )
                                    FrostedCircleButton(
                                        label = if (isVoiceClarityActive) "Voice Clarity: On" else "Voice Clarity: Off",
                                        onClick = {
                                            playback.updateNightMode(!playback.nightModeVoiceBoost)
                                            message = if (playback.nightModeVoiceBoost) "Voice Clarity: On" else "Voice Clarity: Off"
                                            interaction++
                                        },
                                        size = 44.dp,
                                        bgColor = if (isVoiceClarityActive) playerAccentColor.copy(alpha = 0.22f) else dynamicFrostedCircleBg,
                                        rimColor = if (isVoiceClarityActive) playerAccentColor.copy(alpha = 0.65f) else dynamicFrostedCircleRim,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = voiceScale
                                            scaleY = voiceScale
                                        }
                                    ) {
                                        Icon(
                                            Icons.Rounded.GraphicEq,
                                            contentDescription = if (isVoiceClarityActive) "Voice Clarity: On" else "Voice Clarity: Off",
                                            tint = if (isVoiceClarityActive) playerAccentColor else playerContentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (showAnyHud) {
                                    PlayerStatusPill(
                                        showClock = playback.showPlayerClock,
                                        showBattery = playback.showBatteryPercentage,
                                        showRemainingTime = playback.showRemainingTime,
                                        remainingSec = remSec,
                                        hdrBadge = playback.hdrFlavor,
                                        bgColor = dynamicFrostedCircleBg,
                                        rimColor = dynamicFrostedCircleRim,
                                        contentColor = playerContentColor,
                                        accentColor = playerAccentColor
                                    )
                                }



                                FrostedCircleButton(
                                    label = "Cinema settings",
                                    onClick = { quickMenuSection = "main"; interaction++ },
                                    size = 44.dp,
                                    bgColor = dynamicFrostedCircleBg,
                                    rimColor = dynamicFrostedCircleRim
                                ) {
                                    Icon(Icons.Rounded.Tune, null, Modifier.size(22.dp), tint = playerContentColor)
                                }
                            }
                        }

                        // PANEL 1: CENTRAL FROSTED-GLASS CIRCULAR PLAY/PAUSE & 10s SKIP CONTROLS
                        Row(
                            Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedSeekCircleButton(
                                isForward = false,
                                accumulator = seekBackAccumulator,
                                enabled = playback.seekable,
                                onClick = {
                                    val stepSec = playback.seekIncrementSeconds
                                    playback.skip(-seekDeltaMs)
                                    seekBackAccumulator += stepSec
                                    seekBackJob?.cancel()
                                    seekBackJob = coroutineScope.launch {
                                        kotlinx.coroutines.delay(950)
                                        seekBackAccumulator = 0
                                    }
                                    interaction++
                                },
                                size = 54.dp,
                                bgColor = dynamicFrostedCircleBg,
                                rimColor = dynamicFrostedCircleRim,
                                contentColor = playerContentColor
                            )

                            FrostedCircleButton(
                                label = if (playback.playRequested) "Pause" else "Play",
                                onClick = { playback.toggle(); interaction++ },
                                size = 82.dp,
                                bgColor = dynamicFrostedCircleBg,
                                rimColor = dynamicFrostedCircleRim
                            ) {
                                AnimatedContent(
                                    targetState = playback.playRequested,
                                    transitionSpec = {
                                        (fadeIn(tween(160)) + scaleIn(spring(dampingRatio = 0.65f, stiffness = 500f), initialScale = 0.72f))
                                            .togetherWith(fadeOut(tween(120)) + scaleOut(targetScale = 0.72f))
                                    },
                                    label = "playPauseIcon"
                                ) { isPlaying ->
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = playerContentColor,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }

                            AnimatedSeekCircleButton(
                                isForward = true,
                                accumulator = seekFwdAccumulator,
                                enabled = playback.seekable,
                                onClick = {
                                    val stepSec = playback.seekIncrementSeconds
                                    playback.skip(seekDeltaMs)
                                    seekFwdAccumulator += stepSec
                                    seekFwdJob?.cancel()
                                    seekFwdJob = coroutineScope.launch {
                                        kotlinx.coroutines.delay(950)
                                        seekFwdAccumulator = 0
                                    }
                                    interaction++
                                },
                                size = 54.dp,
                                bgColor = dynamicFrostedCircleBg,
                                rimColor = dynamicFrostedCircleRim,
                                contentColor = playerContentColor
                            )
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

                            // Skip Intro Pill Button (Accurate per-series timestamp)
                            val seriesBingeKey = remember(video.name) { BingeManager.resolveSeriesKey(video.name) }
                            val bingeSettings = remember(seriesBingeKey) { BingeManager.getSettings(activity, seriesBingeKey) }
                            val introLimitMs = bingeSettings.introSkipSeconds * 1000L
                            val showSkipIntro = playback.duration > 180_000 && playback.position in 2_000..introLimitMs
                            if (showSkipIntro) {
                                Surface(
                                    onClick = {
                                        playback.seekTo(introLimitMs)
                                        message = "Skipped Intro (${bingeSettings.introSkipSeconds}s)"
                                        interaction++
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = dynamicFrostedCircleBg,
                                    border = BorderStroke(1.dp, dynamicFrostedCircleRim),
                                    shadowElevation = 0.dp,
                                    modifier = Modifier.align(Alignment.End).padding(bottom = 2.dp)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Rounded.FastForward, null, tint = playerAccentColor, modifier = Modifier.size(16.dp))
                                        Text("Skip Intro (${bingeSettings.introSkipSeconds}s)", color = playerContentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                chapters = playback.chapters,
                                onScrub = { scrub = it; interaction++ },
                                onScrubFinished = {
                                    scrub?.let { playback.seekTo((it * playback.duration).toLong()) }
                                    scrub = null
                                }
                            )

                            // Bottom Controls: Clean Single Row in Minimalist HUD (or on landscape/tablet)
                            if (isMinimalHud || !isPhonePortrait) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Glassy Time Capsule Pill
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = dynamicFrostedCircleBg,
                                        border = BorderStroke(1.dp, dynamicFrostedCircleRim)
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                compactTimecode(elapsedSec),
                                                color = playerAccentColor,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text("/", color = playerSubtextColor.copy(alpha = 0.5f), fontSize = 11.5.sp)
                                            Text(
                                                compactTimecode(totalSec),
                                                color = playerContentColor,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (playback.showRemainingTime) {
                                                val remSec = (totalSec - elapsedSec).coerceAtLeast(0)
                                                Surface(
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = if (isLight) Color(0xFF183654).copy(alpha = 0.08f) else Color.White.copy(alpha = 0.14f),
                                                    border = BorderStroke(0.5.dp, dynamicFrostedCircleRim)
                                                ) {
                                                    Text(
                                                        "-${compactTimecode(remSec)}",
                                                        Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        color = playerContentColor.copy(alpha = 0.90f),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Glassy Actions Dock
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = dynamicFrostedCircleBg,
                                        border = BorderStroke(1.dp, dynamicFrostedCircleRim)
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            CinemaIcon("Lock controls", { playback.locked = true; controls = false }, modifier = Modifier.size(36.dp), contentColor = playerContentColor) {
                                                Icon(Icons.Rounded.LockOpen, null, Modifier.size(19.dp))
                                            }
                                            CinemaIcon("Rotate orientation", {
                                                val current = activity.requestedOrientation
                                                val next = if (current == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE || current == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                                } else if (current == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT || current == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                                                    ActivityInfo.SCREEN_ORIENTATION_USER
                                                } else {
                                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                                }
                                                activity.requestedOrientation = next
                                                message = when (next) {
                                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE -> "Orientation: Landscape"
                                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT -> "Orientation: Portrait"
                                                    else -> "Orientation: Auto (Sensor)"
                                                }
                                                interaction++
                                            }, modifier = Modifier.size(36.dp), contentColor = playerContentColor) {
                                                Icon(Icons.Rounded.ScreenRotation, "Rotate orientation", Modifier.size(19.dp))
                                            }
                                            if (activity.supportsPip()) {
                                                CinemaIcon("Picture in picture", { if (!activity.enterCinemaPip()) message = "PiP unavailable" }, modifier = Modifier.size(36.dp), contentColor = playerContentColor) {
                                                    Icon(Icons.Rounded.PictureInPictureAlt, null, Modifier.size(19.dp))
                                                }
                                            }
                                            CinemaIcon("Close player", dismissPlayerWithFluidity, modifier = Modifier.size(36.dp), contentColor = playerContentColor) {
                                                Icon(Icons.Rounded.Close, null, Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Row 1: Glassy Time Capsule Pill
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = dynamicFrostedCircleBg,
                                        border = BorderStroke(1.dp, dynamicFrostedCircleRim),
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                compactTimecode(elapsedSec),
                                                color = playerAccentColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text("/", color = playerSubtextColor.copy(alpha = 0.5f), fontSize = 11.sp)
                                            Text(
                                                compactTimecode(totalSec),
                                                color = playerContentColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (playback.showRemainingTime) {
                                                val remSec = (totalSec - elapsedSec).coerceAtLeast(0)
                                                Surface(
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = if (isLight) Color(0xFF183654).copy(alpha = 0.08f) else Color.White.copy(alpha = 0.14f),
                                                    border = BorderStroke(0.5.dp, dynamicFrostedCircleRim)
                                                ) {
                                                    Text(
                                                        "-${compactTimecode(remSec)} remaining",
                                                        Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                        color = playerContentColor.copy(alpha = 0.92f),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Row 2: Glassy Actions Dock
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = dynamicFrostedCircleBg,
                                        border = BorderStroke(1.dp, dynamicFrostedCircleRim),
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            CinemaIcon("Rotate orientation", {
                                                val current = activity.requestedOrientation
                                                val next = if (current == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE || current == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                                } else if (current == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT || current == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                                                    ActivityInfo.SCREEN_ORIENTATION_USER
                                                } else {
                                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                                }
                                                activity.requestedOrientation = next
                                                message = when (next) {
                                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE -> "Orientation: Landscape"
                                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT -> "Orientation: Portrait"
                                                    else -> "Orientation: Auto (Sensor)"
                                                }
                                                interaction++
                                            }, modifier = Modifier.size(36.dp), contentColor = playerContentColor) {
                                                Icon(Icons.Rounded.ScreenRotation, "Rotate orientation", Modifier.size(19.dp))
                                            }
                                            CinemaIcon("Lock controls", { playback.locked = true; controls = false }, modifier = Modifier.size(36.dp), contentColor = playerContentColor) {
                                                Icon(Icons.Rounded.LockOpen, null, Modifier.size(19.dp))
                                            }
                                            if (activity.supportsPip()) {
                                                CinemaIcon("Picture in picture", { if (!activity.enterCinemaPip()) message = "PiP unavailable" }, modifier = Modifier.size(36.dp), contentColor = playerContentColor) {
                                                    Icon(Icons.Rounded.PictureInPictureAlt, null, Modifier.size(19.dp))
                                                }
                                            }
                                            CinemaIcon("Close player", dismissPlayerWithFluidity, modifier = Modifier.size(36.dp), contentColor = playerContentColor) {
                                                Icon(Icons.Rounded.Close, null, Modifier.size(20.dp))
                                            }
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

                // PANEL 5: RIGHT-SIDE QUICK MENU (Fluid Spring Morphing)
                AnimatedVisibility(
                    visible = quickMenuSection != null && !playback.inPip,
                    enter = fadeIn(tween(if (reducedMotion) 0 else 220)) +
                            slideInHorizontally(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)) { it / 2 } +
                            scaleIn(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow), initialScale = 0.90f),
                    exit = fadeOut(tween(if (reducedMotion) 0 else 140)) +
                           slideOutHorizontally(tween(if (reducedMotion) 0 else 160)) { it / 3 } +
                           scaleOut(tween(if (reducedMotion) 0 else 140), targetScale = 0.92f)
                ) {
                    quickMenuSection?.let { section ->
                        RightSideQuickMenu(
                            playback = playback,
                            section = section,
                            theme = theme,
                            isMinimalHud = isMinimalHud,
                            onToggleMinimalHud = {
                                val next = !isMinimalHud
                                isMinimalHud = next
                                playerPrefs.edit().putBoolean("player_hud_minimal", next).apply()
                                message = if (next) "Minimalist HUD enabled" else "Expressive HUD enabled"
                                interaction++
                            },
                            onSelectSection = { quickMenuSection = it; interaction++ },
                            onAddExternalSubtitle = { subtitleFileLauncher.launch(arrayOf("*/*")) },
                            onOpenChapters = { showChaptersDrawer = true; quickMenuSection = null },
                            onOpenTrimmer = { showTrimmerDialog = true; quickMenuSection = null },
                            onOpenBinge = { showBingeDialog = true; quickMenuSection = null },
                            onOpenTools = { section ->
                                toolsSection = section; quickMenuSection = null
                            },
                            onSearchSubtitles = {
                                if (!subtitleSearchBusy) subtitleSearchLauncher.launch(arrayOf("*/*"))
                            },
                            onMessage = { message = it; interaction++ },
                            onDismiss = { quickMenuSection = null },
                            onClosePlayer = dismissPlayerWithFluidity
                        )
                    }
                }

                // Milestone 5A: 10-Second Next-Episode Autoplay Countdown Banner
                val nextEp = playback.nextEpisode
                val countdown = playback.nextEpisodeCountdown
                AnimatedVisibility(
                    visible = nextEp != null && countdown != null && !playback.locked && !playback.inPip,
                    enter = fadeIn(tween(if (reducedMotion) 0 else 250)) + slideInVertically(tween(if (reducedMotion) 0 else 300)) { it / 2 },
                    exit = fadeOut(tween(if (reducedMotion) 0 else 200)) + slideOutVertically(tween(if (reducedMotion) 0 else 250)) { it / 2 },
                    modifier = Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(bottom = 100.dp, end = 24.dp)
                ) {
                    if (nextEp != null && countdown != null) {
                        NextEpisodeAutoplayBanner(
                            nextEpisode = nextEp,
                            countdownSeconds = countdown,
                            onPlayNow = { playback.playNextEpisode() },
                            onCancel = { playback.dismissNextEpisodeCountdown() }
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
                                OutlinedButton(onClick = dismissPlayerWithFluidity) { Text("Close", color = Color.White) }
                            }
                        }
                    }
                }
            }
        }

        if (showChaptersDrawer) {
            val extractedChapters by produceState(emptyList<VideoChapter>(), video.identity) {
                value = ChapterExtractor.extractChapters(activity, video)
            }
            ChapterDrawerSheet(
                chapters = extractedChapters,
                currentPositionMs = playback.position,
                onSeekToChapter = { pos ->
                    playback.seekTo(pos)
                    message = "Jumped to chapter"
                },
                onDismiss = { showChaptersDrawer = false }
            )
        }

        if (showTrimmerDialog) {
            VideoTrimmerDialog(
                video = video,
                currentPosMs = playback.position,
                onMessage = { message = it },
                onDismiss = { showTrimmerDialog = false }
            )
        }

        if (showBingeDialog) {
            BingeSettingsDialog(
                seriesName = BingeManager.resolveSeriesKey(video.name),
                onDismiss = { showBingeDialog = false }
            )
        }
    }

/** Panel 1 & 3: Ultra-Thin Periwinkle-Blue Timeline with Chapter Marks & Haptic Snap */
@Composable private fun CinemaTimelineBar(
    fraction: Float,
    bufferedFraction: Float,
    isScrubbing: Boolean,
    enabled: Boolean,
    chapters: List<ChapterMarker> = emptyList(),
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit
) {
    val motion = LocalMotionStyle.current
    val haptic = LocalHapticFeedback.current
    val trackHeight by animateDpAsState(if (isScrubbing) 5.dp else 3.dp, label = "trackHeight")
    val thumbRadius by animateDpAsState(if (isScrubbing) 8.5.dp else 6.5.dp, label = "thumbRadius")
    val density = LocalDensity.current
    var lastHapticStep by remember { mutableIntStateOf(-1) }

    fun processScrubFraction(rawF: Float) {
        var snapped = rawF
        val snapThreshold = 0.014f
        for (chap in chapters) {
            if (kotlin.math.abs(rawF - chap.fraction) < snapThreshold) {
                snapped = chap.fraction
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                break
            }
        }
        val step = (snapped * 20).toInt()
        if (step != lastHapticStep) {
            lastHapticStep = step
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        onScrub(snapped)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .pointerInput(enabled, chapters) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val f = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    processScrubFraction(f)
                    onScrubFinished()
                }
            }
            .pointerInput(enabled, chapters) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val f = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        processScrubFraction(f)
                    },
                    onDragEnd = onScrubFinished,
                    onDragCancel = onScrubFinished
                ) { change, _ ->
                    change.consume()
                    val f = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    processScrubFraction(f)
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

            if (enabled && isScrubbing && motion.enabled && motion.light) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(TimelineAccent.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width * fraction.coerceIn(0f, 1f), yCenter),
                        radius = 24.dp.toPx()
                    ),
                    radius = 24.dp.toPx(),
                    center = Offset(size.width * fraction.coerceIn(0f, 1f), yCenter)
                )
            }
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
            // Subtle Chapter tick marks
            for (chap in chapters) {
                val cx = size.width * chap.fraction.coerceIn(0f, 1f)
                drawLine(
                    color = Color.White.copy(alpha = 0.70f),
                    start = Offset(cx, yCenter - h * 1.5f),
                    end = Offset(cx, yCenter + h * 1.5f),
                    strokeWidth = with(density) { 1.5.dp.toPx() }
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

private data class QuickMenuPalette(
    val bg: Color,
    val rim: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val iconTint: Color,
    val divider: Color,
    val accent: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val btnBg: Color,
    val btnRim: Color
)

private val LocalQuickMenuPalette = staticCompositionLocalOf {
    QuickMenuPalette(
        bg = Color(0xF00D1520),
        rim = Color.White.copy(alpha = 0.14f),
        primaryText = Color.White,
        secondaryText = Color.White.copy(alpha = 0.72f),
        iconTint = Color.White.copy(alpha = 0.85f),
        divider = Color.White.copy(alpha = 0.12f),
        accent = TimelineAccent,
        cardBg = Color.White.copy(alpha = 0.06f),
        cardBorder = Color.White.copy(alpha = 0.12f),
        btnBg = FrostedCircleBg,
        btnRim = FrostedCircleRim
    )
}

/** Panel 5: Right-Side Floating Translucent Quick Menu */
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Composable private fun RightSideQuickMenu(
    playback: PlaybackController,
    section: String,
    theme: String = "Dark",
    isMinimalHud: Boolean = true,
    onToggleMinimalHud: () -> Unit = {},
    onSelectSection: (String) -> Unit,
    onAddExternalSubtitle: () -> Unit,
    onOpenChapters: () -> Unit = {},
    onOpenTrimmer: () -> Unit = {},
    onOpenBinge: () -> Unit = {},
    onSearchSubtitles: () -> Unit,
    onOpenTools: (String) -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit,
    onClosePlayer: () -> Unit = playback::close
) {
    val activeAudio = playback.audioTracks.firstOrNull { it.isSelected }
    val audioSummary = activeAudio?.let { "${it.label} (${it.codec} ${it.channels})" } ?: "Default"
    val activeSub = playback.subtitleTracks.firstOrNull { it.isSelected }
    val subSummary = activeSub?.let { "${it.label} (${it.format})" } ?: "Off"

    val isLight = theme == "Light"
    val isAmoled = theme == "AMOLED"

    val palette = remember(theme) {
        if (isLight) {
            QuickMenuPalette(
                bg = Color(0xF6F8FAFD),
                rim = Color(0xFFD0D7DE),
                primaryText = Color(0xFF183654),
                secondaryText = Color(0xFF526B85),
                iconTint = Color(0xFF334A62),
                divider = Color(0x1F000000),
                accent = Color(0xFF0061A4),
                cardBg = Color(0xFFE8EEF6),
                cardBorder = Color(0xFFCBD5E1),
                btnBg = Color(0xFFE2EBF5),
                btnRim = Color(0xFFCBD5E1)
            )
        } else if (isAmoled) {
            QuickMenuPalette(
                bg = Color(0xFC000000),
                rim = Color.White.copy(alpha = 0.18f),
                primaryText = Color.White,
                secondaryText = Color.White.copy(alpha = 0.75f),
                iconTint = Color.White.copy(alpha = 0.90f),
                divider = Color.White.copy(alpha = 0.14f),
                accent = TimelineAccent,
                cardBg = Color(0xFF10141C),
                cardBorder = Color.White.copy(alpha = 0.14f),
                btnBg = Color(0xFF141923),
                btnRim = Color.White.copy(alpha = 0.18f)
            )
        } else {
            QuickMenuPalette(
                bg = Color(0xF00D1520),
                rim = Color.White.copy(alpha = 0.14f),
                primaryText = Color.White,
                secondaryText = Color.White.copy(alpha = 0.72f),
                iconTint = Color.White.copy(alpha = 0.85f),
                divider = Color.White.copy(alpha = 0.12f),
                accent = TimelineAccent,
                cardBg = Color.White.copy(alpha = 0.06f),
                cardBorder = Color.White.copy(alpha = 0.12f),
                btnBg = FrostedCircleBg,
                btnRim = FrostedCircleRim
            )
        }
    }

    CompositionLocalProvider(LocalQuickMenuPalette provides palette) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (isLight) 0.18f else 0.25f)).clickable(onClick = onDismiss)) {
            val isPhonePortrait = maxWidth < 600.dp && maxHeight > maxWidth
            Surface(
                modifier = if (isPhonePortrait) {
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .clickable(enabled = false) {}
                } else {
                    Modifier
                        .align(Alignment.CenterEnd)
                        .safeDrawingPadding()
                        .padding(end = 24.dp, top = 24.dp, bottom = 24.dp)
                        .widthIn(min = 300.dp, max = 350.dp)
                        .clickable(enabled = false) {}
                },
                color = palette.bg,
                border = BorderStroke(1.dp, palette.rim),
                shape = if (isPhonePortrait) RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) else RoundedCornerShape(22.dp),
                shadowElevation = if (isLight) 8.dp else 24.dp
            ) {
            when (section) {
                "main" -> {
                    Column(Modifier.heightIn(max = 560.dp).elasticEdge().verticalScroll(rememberScrollState()).padding(vertical = 10.dp)) {
                        QuickMenuRow(
                            icon = Icons.Rounded.Speed,
                            label = "Playback speed",
                            value = "${playback.speed}x",
                            onClick = { onSelectSection("speed") }
                        )
                        QuickMenuRow(
                            icon = if (isMinimalHud) Icons.Rounded.ViewSidebar else Icons.Rounded.Splitscreen,
                            label = "Minimalist HUD",
                            value = if (isMinimalHud) "On" else "Off",
                            onClick = onToggleMinimalHud
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.RecordVoiceOver,
                            label = "Voice Clarity (Vocal Boost)",
                            value = if (playback.nightModeVoiceBoost) "Enhanced" else "Off",
                            onClick = {
                                playback.nightModeVoiceBoost = !playback.nightModeVoiceBoost
                                onMessage(if (playback.nightModeVoiceBoost) "Voice Clarity: On" else "Voice Clarity: Off")
                            }
                        )

                        QuickMenuRow(Icons.Rounded.Bedtime, "Sleep controls", if (playback.sleepPlan != null) "Active" else "Off", { onOpenTools("sleep") })
                        QuickMenuRow(Icons.Rounded.PlaylistPlay, "Episode continuity", "", { onOpenTools("continuity") })
                        QuickMenuRow(
                            icon = Icons.Rounded.BookmarkBorder,
                            label = "Chapters & Acts",
                            value = if (playback.chapters.isNotEmpty()) "${playback.chapters.size} marks" else "Acts",
                            onClick = onOpenChapters
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.ContentCut,
                            label = "Clip Scene",
                            value = "Instant 1080p/4K",
                            onClick = onOpenTrimmer
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.ColorLens,
                            label = "Aesthetic Color Harmony & Frame",
                            value = "5-Swatch Palette",
                            onClick = { onOpenTools("palette") }
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.Animation,
                            label = "Create Animated Scene Clip",
                            value = "Instant WebP",
                            onClick = { onOpenTools("clip") }
                        )
                        if (playback.video?.episode != null) {
                            QuickMenuRow(
                                icon = Icons.Rounded.FastForward,
                                label = "Binge Automation",
                                value = "Auto-Skip Intro/Outro",
                                onClick = onOpenBinge
                            )
                        }
                        QuickMenuRow(
                            icon = Icons.Rounded.AspectRatio,
                            label = "Fit / Aspect ratio",
                            value = playback.aspectMode.displayName.substringBefore(" ("),
                            onClick = { onSelectSection("aspect") }
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.Tune,
                            label = "Video Enhancer",
                            value = if (playback.isHdr) "HDR Master" else playback.videoEnhancerProfile.displayName.substringBefore(" ("),
                            onClick = { onSelectSection("enhancer") }
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.GraphicEq,
                            label = "Audio track",
                            value = audioSummary,
                            onClick = { onSelectSection("audio") }
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.SurroundSound,
                            label = "Spatial Cinema 3D",
                            value = playback.spatial3DMode.displayName.substringBefore(" ("),
                            onClick = { onSelectSection("spatial_3d") }
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
                        AspectRatioMode.values().forEach { mode ->
                            QuickOptionItem(
                                label = mode.displayName,
                                selected = playback.aspectMode == mode,
                                onClick = {
                                    playback.setAspectRatioMode(mode)
                                    onMessage("Aspect: ${mode.displayName.substringBefore(" (")}")
                                    onSelectSection("main")
                                }
                            )
                        }
                    }
                }


                "enhancer" -> {
                    QuickSubMenuContainer(title = "Video Enhancer & Color Studio", onBack = { onSelectSection("main") }) {
                        val isPotentialHdr = playback.isHdr || playback.video?.name?.let {
                            it.contains("hdr", ignoreCase = true) || it.contains("10bit", ignoreCase = true) ||
                            it.contains("dovi", ignoreCase = true) || it.contains("dv", ignoreCase = true)
                        } == true

                        if (isPotentialHdr || playback.isHdr) {
                            val flavorText = playback.hdrFlavor ?: "HDR10"
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (isLight) Color(0xFFFFF8E1) else Color(0xFF2E2200)),
                                border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("$flavorText Master Active", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Native 10-bit BT.2020 wide color gamut & HDR dynamic range active. Use the Shadow Clarifier below to rescue crushed dark scenes without clipping highlights.",
                                        color = if (isLight) Color(0xFF5D4037) else Color.White.copy(alpha = 0.82f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Text("HDR Profile Selection", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                            HdrProfileMode.entries.forEach { mode ->
                                QuickOptionItem(
                                    label = mode.displayName,
                                    selected = playback.hdrProfileMode == mode,
                                    onClick = {
                                        playback.setHdrProfile(mode)
                                        onMessage("HDR Profile: ${mode.displayName.substringBefore(" (")}")
                                    }
                                )
                            }

                            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = palette.divider)

                            Text("HDR Shadow Clarifier (AMOLED Black-Crush Rescue)", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                            val clarifierLevels = listOf(
                                0.0f to "Off (Native Master Curve)",
                                0.25f to "Mild (+25% Shadow Lift)",
                                0.50f to "High (+50% Shadow Lift)"
                            )
                            clarifierLevels.forEach { (level, label) ->
                                QuickOptionItem(
                                    label = label,
                                    selected = kotlin.math.abs(playback.hdrShadowClarifierLevel - level) < 0.01f,
                                    onClick = {
                                        playback.setHdrShadowClarifier(level)
                                        onMessage("Shadow Clarifier: $label")
                                    }
                                )
                            }
                            Text(
                                "Lifts dark shadows and shadow silhouettes on AMOLED panels without clipping highlights or over-saturating.",
                                color = palette.secondaryText,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                            )

                            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = palette.divider)

                            QuickOptionItem(
                                label = if (playback.brightnessBoost) "HDR Peak Luminance Boost: 100%" else "HDR Peak Luminance Boost: Auto",
                                selected = playback.brightnessBoost,
                                onClick = {
                                    playback.updateBrightnessBoost(!playback.brightnessBoost)
                                    onMessage(if (playback.brightnessBoost) "HDR Peak Luminance: 100%" else "HDR Peak Luminance: Auto")
                                }
                            )
                            Text(
                                "Maximizes display panel specular headroom to illuminate dark scenes and highlights.",
                                color = palette.secondaryText,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                            )
                        } else {
                            Text("Color Profiles", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                            ColorEnhancementProfile.entries.forEach { profile ->
                                QuickOptionItem(
                                    label = profile.displayName,
                                    selected = playback.videoEnhancerProfile == profile,
                                    onClick = {
                                        playback.updateColorProfile(profile)
                                        onMessage("Profile: ${profile.displayName.substringBefore(" (")}")
                                    }
                                )
                                Text(
                                    profile.description,
                                    color = palette.secondaryText,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                                )
                            }

                            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = palette.divider)

                            val params = playback.videoEnhancerParams
                            Text("Fine-Tuning Controls", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                            EnhancerSliderRow(
                                title = "Contrast",
                                valueText = String.format(java.util.Locale.US, "%.2fx", params.contrast),
                                subtext = "Pivots around middle-gray (0.5); neutral = 1.00x",
                                value = params.contrast,
                                valueRange = 0.6f..1.6f,
                                onValueChange = {
                                    playback.updateColorParams(params.copy(contrast = it))
                                }
                            )

                            EnhancerSliderRow(
                                title = "Shadow Lift / Gamma",
                                valueText = "${(params.shadowLift * 100).roundToInt()}%",
                                subtext = "Lifts dark shadows without blowing out highlights; neutral = 0%",
                                value = params.shadowLift,
                                valueRange = -0.25f..0.25f,
                                onValueChange = {
                                    playback.updateColorParams(params.copy(shadowLift = it))
                                }
                            )

                            EnhancerSliderRow(
                                title = "Saturation",
                                valueText = "${(params.saturation * 100).roundToInt()}%",
                                subtext = "0% = pure monochrome, 100% = neutral, 200% = vivid",
                                value = params.saturation,
                                valueRange = 0.0f..2.0f,
                                onValueChange = {
                                    playback.updateColorParams(params.copy(saturation = it))
                                }
                            )

                            EnhancerSliderRow(
                                title = "Vibrance",
                                valueText = "${(params.vibrance * 100).roundToInt()}%",
                                subtext = "Selectively boosts muted tones while protecting skin tones",
                                value = params.vibrance,
                                valueRange = -0.25f..0.25f,
                                onValueChange = {
                                    playback.updateColorParams(params.copy(vibrance = it))
                                }
                            )

                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text("AMOLED Deep Black (Experimental)", color = palette.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(ColorStudioParams.AMOLED_WARNING, color = Color(0xFFFFB300).copy(alpha = 0.85f), fontSize = 10.sp)
                                }
                                Switch(
                                    checked = params.amoledDeepBlack,
                                    onCheckedChange = {
                                        playback.updateColorParams(params.copy(amoledDeepBlack = it))
                                        onMessage(if (it) "AMOLED Deep Black ON" else "AMOLED Deep Black OFF")
                                    }
                                )
                            }

                            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = palette.divider)
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        playback.resetColorStudio()
                                        onMessage("Video enhancer reset to Neutral")
                                    },
                                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.Refresh, null, tint = palette.accent, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Reset to Neutral", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                "audio" -> {
                    QuickSubMenuContainer(title = "Audio Studio & Stream Format", onBack = { onSelectSection("main") }) {
                        val streamInfo = remember(
                            playback.runtimeAudioFormat,
                            playback.runtimeAudioDecoder,
                            playback.audioTracks,
                            playback.audioChannelMode,
                            playback.nightModeVoiceBoost,
                            playback.audioDelayMs
                        ) { playback.inspectActiveAudioStream() }

                        Text("Active Stream Telemetry", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = palette.cardBg),
                            border = BorderStroke(1.dp, palette.cardBorder),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                AudioTelemetryRow("Container", streamInfo.container)
                                AudioTelemetryRow("Source Codec", streamInfo.sourceCodec)
                                AudioTelemetryRow("Channels & Layout", streamInfo.sourceChannels)
                                AudioTelemetryRow("Sample Rate", streamInfo.sourceSampleRate)
                                AudioTelemetryRow("Source Bit Depth", streamInfo.sourceBitDepth)
                                AudioTelemetryRow("Active Decoder", streamInfo.activeDecoder)
                                AudioTelemetryRow("Decoder Engine", streamInfo.decoderEngine)
                                AudioTelemetryRow("Player Audio Output", "${streamInfo.playerOutputChannels} · ${streamInfo.playerOutputSampleRate} · ${streamInfo.playerOutputEncoding}")

                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Processing:", color = palette.secondaryText, fontSize = 11.sp)
                                    streamInfo.activeProcessing.forEach { proc ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = palette.accent.copy(alpha = 0.16f),
                                            border = BorderStroke(0.5.dp, palette.accent.copy(alpha = 0.35f))
                                        ) {
                                            Text(proc, color = palette.accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = palette.divider)

                        Text("Channel Processing Mode", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        AudioChannelMode.entries.forEach { mode ->
                            QuickOptionItem(
                                label = mode.title,
                                selected = playback.audioChannelMode == mode,
                                onClick = {
                                    playback.updateAudioChannelMode(mode)
                                    onMessage("Channel Mode: ${mode.title}")
                                }
                            )
                            Text(
                                mode.description,
                                color = palette.secondaryText,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                            )
                        }

                        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = palette.divider)

                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Bluetooth Audio Delay (Lip-Sync)", color = palette.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Negative (−) advances audio; Positive (+) delays audio to sync with Bluetooth latency", color = palette.secondaryText, fontSize = 10.sp)
                                Text("Current offset: ${playback.audioDelayMs}ms", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                FrostedCircleButton("-50ms", { playback.updateAudioDelay(-50); onMessage("Audio Delay: ${playback.audioDelayMs}ms") }, size = 32.dp, bgColor = palette.btnBg, rimColor = palette.btnRim) {
                                    Text("-", color = palette.primaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                if (playback.audioDelayMs != 0L) {
                                    FrostedCircleButton("Reset", { playback.resetAudioDelay(); onMessage("Audio Delay: 0ms") }, size = 32.dp, bgColor = palette.btnBg, rimColor = palette.btnRim) {
                                        Text("0", color = palette.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                FrostedCircleButton("+50ms", { playback.updateAudioDelay(50); onMessage("Audio Delay: ${playback.audioDelayMs}ms") }, size = 32.dp, bgColor = palette.btnBg, rimColor = palette.btnRim) {
                                    Text("+", color = palette.primaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = palette.divider)

                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Night Mode / Dynamic Compression", color = palette.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Dynamic compression amplifies quiet dialogue and dampens loud effects", color = palette.secondaryText, fontSize = 11.sp)
                            }
                            Switch(
                                checked = playback.nightModeVoiceBoost,
                                onCheckedChange = {
                                    playback.updateNightMode(it)
                                    onMessage(if (it) "Night Mode (Voice Boost) ON" else "Night Mode OFF")
                                }
                            )
                        }

                        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = palette.divider)

                        Text("Available Audio Tracks", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        if (playback.audioTracks.isEmpty()) {
                            Text("Default audio stream active", color = palette.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
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
                    QuickSubMenuContainer(title = "Subtitle settings", onBack = { onSelectSection("main") }) {
                        Text("Active Track", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
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

                        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = palette.divider)

                        QuickMenuRow(
                            icon = Icons.Rounded.Add,
                            label = "Add external subtitle (.srt, .vtt, .ass)",
                            value = "",
                            onClick = { onAddExternalSubtitle(); onDismiss() }
                        )

                        QuickMenuRow(
                            icon = Icons.Rounded.Search,
                            label = "Search a local SRT / VTT file",
                            value = "",
                            onClick = { onSearchSubtitles(); onDismiss() }
                        )

                        // Subtitle Timing Calibration
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Subtitle Sync Offset", color = palette.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                val sec = playback.subtitleDelayMs / 1000f
                                Text("Offset: ${if (sec > 0) "+$sec" else "$sec"}s", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                FrostedCircleButton("Auto-Sync", {
                                    val synced = playback.autoSyncSubtitles()
                                    onMessage(if (synced) "Acoustic Auto-Sync: Timestamps Aligned" else "Acoustic Auto-Sync: Subtitles Already In Sync")
                                }, size = 32.dp, bgColor = palette.btnBg, rimColor = palette.btnRim) {
                                    Icon(Icons.Rounded.AutoAwesome, "Auto-Sync", tint = palette.accent, modifier = Modifier.size(15.dp))
                                }
                                FrostedCircleButton("-100ms", { playback.updateSubtitleDelay(-100); onMessage("Sub Delay: ${playback.subtitleDelayMs / 1000f}s") }, size = 32.dp, bgColor = palette.btnBg, rimColor = palette.btnRim) {
                                    Text("-", color = palette.primaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                if (playback.subtitleDelayMs != 0L) {
                                    FrostedCircleButton("Reset", { playback.resetSubtitleDelay(); onMessage("Sub Delay: 0.0s") }, size = 32.dp, bgColor = palette.btnBg, rimColor = palette.btnRim) {
                                        Text("0", color = palette.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                FrostedCircleButton("+100ms", { playback.updateSubtitleDelay(100); onMessage("Sub Delay: ${playback.subtitleDelayMs / 1000f}s") }, size = 32.dp, bgColor = palette.btnBg, rimColor = palette.btnRim) {
                                    Text("+", color = palette.primaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = palette.divider)

                        Text("Dual Subtitles Studio (Secondary Track)", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        val secTrack = playback.dualSubtitleStudio.secondaryTrack
                        QuickOptionItem(
                            label = "Off (Single Subtitle)",
                            selected = secTrack == null,
                            onClick = { playback.selectSecondarySubtitle(null); onMessage("Secondary Subtitle Off") }
                        )
                        playback.subtitleTracks.filter { !it.isSelected }.forEach { track ->
                            QuickOptionItem(
                                label = "${track.label} (${track.format})",
                                selected = secTrack == track,
                                onClick = { playback.selectSecondarySubtitle(track); onMessage("Dual Subtitles: ${track.label} Active") }
                            )
                        }
                    }
                }

                "more" -> {
                    QuickSubMenuContainer(title = "More settings", onBack = { onSelectSection("main") }) {
                        QuickMenuRow(
                            icon = Icons.Rounded.AspectRatio,
                            label = "Video scaling",
                            value = playback.aspectMode.displayName.substringBefore(" ("),
                            onClick = {
                                val modes = AspectRatioMode.values()
                                val nextIndex = (playback.aspectMode.ordinal + 1) % modes.size
                                val nextMode = modes[nextIndex]
                                playback.setAspectRatioMode(nextMode)
                                onMessage("Aspect: ${nextMode.displayName.substringBefore(" (")}")
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
                            onClick = { onClosePlayer(); onDismiss() }
                        )
                    }
                }

                "spatial_3d" -> {
                    QuickSubMenuContainer(title = "Spatial Cinema 3D Virtualizer", onBack = { onSelectSection("main") }) {
                        Text(
                            "Expands stereo audio into a 3D binaural acoustic room model for headphones and multi-speaker setups.",
                            color = palette.secondaryText,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        Spatial3DMode.entries.forEach { mode ->
                            QuickOptionItem(
                                label = mode.displayName,
                                selected = playback.spatial3DMode == mode,
                                onClick = {
                                    playback.setSpatial3D(mode)
                                    onMessage("Spatial 3D: ${mode.displayName.substringBefore(" (")}")
                                }
                            )
                        }
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
    val palette = LocalQuickMenuPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .touchMotion()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.wrapContentWidth()
        ) {
            Icon(icon, null, tint = palette.iconTint, modifier = Modifier.size(20.dp))
            Text(label, color = palette.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                value,
                color = palette.secondaryText,
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
    val palette = LocalQuickMenuPalette.current
    Column(Modifier.padding(vertical = 10.dp).heightIn(max = 340.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onBack).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = palette.primaryText, modifier = Modifier.size(18.dp))
            Text(title, color = palette.primaryText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(color = palette.divider)
        LazyColumn(content = { item { Column(content = content) } })
    }
}

@Composable private fun QuickOptionItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalQuickMenuPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .touchMotion()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = if (selected) palette.accent else palette.primaryText,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Rounded.Check, null, tint = palette.accent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable private fun FrostedCircleButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    bgColor: Color = FrostedCircleBg,
    rimColor: Color = FrostedCircleRim,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(size).touchMotion().semantics { contentDescription = label },
        shape = CircleShape,
        color = if (enabled) bgColor else bgColor.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, if (enabled) rimColor else rimColor.copy(alpha = 0.10f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}

@Composable
private fun AnimatedSeekCircleButton(
    isForward: Boolean,
    accumulator: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    bgColor: Color = FrostedCircleBg,
    rimColor: Color = FrostedCircleRim,
    contentColor: Color = Color.White
) {
    var bounceTrigger by remember { mutableIntStateOf(0) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (bounceTrigger % 2 == 1) (if (isForward) 24f else -24f) else 0f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow),
        label = "seekRotation_${if (isForward) "fwd" else "back"}"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (bounceTrigger % 2 == 1) 1.18f else 1.0f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow),
        label = "seekScale_${if (isForward) "fwd" else "back"}"
    )

    LaunchedEffect(bounceTrigger) {
        if (bounceTrigger % 2 == 1) {
            kotlinx.coroutines.delay(160)
            bounceTrigger++
        }
    }

    Box(contentAlignment = Alignment.Center) {
        Surface(
            modifier = modifier
                .size(size)
                .touchMotion()
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 28.dp)
                ) {
                    bounceTrigger++
                    onClick()
                },
            shape = CircleShape,
            color = bgColor,
            border = BorderStroke(1.dp, rimColor),
            shadowElevation = 6.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = if (isForward) Icons.Rounded.Forward10 else Icons.Rounded.Replay10,
                    contentDescription = if (isForward) "Forward 10 seconds" else "Rewind 10 seconds",
                    tint = contentColor,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer {
                            rotationZ = rotationAngle
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }
        }

        // Floating skip accumulator badge pill (Clean animation, no button morphing)
        androidx.compose.animation.AnimatedVisibility(
            visible = accumulator > 0,
            enter = fadeIn(tween(140)) + scaleIn(spring(dampingRatio = 0.7f, stiffness = 600f), initialScale = 0.75f),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-36).dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = TimelineAccent,
                shadowElevation = 8.dp
            ) {
                Text(
                    text = if (isForward) "+${accumulator}s" else "-${accumulator}s",
                    color = Color(0xFF0F1724),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable private fun CinemaIcon(
    label: String,
    action: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = Color.White.copy(alpha = 0.90f),
    icon: @Composable () -> Unit
) {
    val colors = IconButtonDefaults.iconButtonColors(
        contentColor = contentColor,
        disabledContentColor = contentColor.copy(alpha = 0.35f)
    )
    IconButton(
        onClick = action,
        modifier = modifier.size(42.dp).touchMotion().semantics { contentDescription = label },
        enabled = enabled,
        colors = colors
    ) {
        icon()
    }
}

@Composable
private fun NextEpisodeAutoplayBanner(
    nextEpisode: LocalVideo,
    countdownSeconds: Int,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit
) {
    val epInfo = remember(nextEpisode.name) { EpisodePatterns.parse(nextEpisode.name) }
    val title = epInfo?.episodeTitle ?: nextEpisode.title
    val epLabel = if (epInfo != null) "S%02dE%02d".format(epInfo.season, epInfo.episode) else "Next Episode"

    Surface(
        modifier = Modifier.widthIn(max = 380.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xF0131922),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Countdown badge + Cancel (X)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF388E3C), CircleShape)
                    )
                    Text(
                        "Up next in ${countdownSeconds}s",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Cancel",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Thumbnail + Title
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(76.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    LocalThumbnail(video = nextEpisode, modifier = Modifier.fillMaxSize())
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = TimelineAccent.copy(alpha = 0.25f)
                    ) {
                        Text(
                            epLabel,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TimelineAccent
                        )
                    }
                    Text(
                        title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Linear Countdown Progress Line
            val progress = (countdownSeconds / 12f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF1687F7), Color(0xFF71D8FF))),
                            RoundedCornerShape(2.dp)
                        )
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlayNow,
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = TimelineAccent)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, Modifier.size(16.dp), tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("Play Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Text("Stay here", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AudioTelemetryRow(label: String, value: String) {
    val palette = LocalQuickMenuPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = palette.secondaryText, fontSize = 11.sp)
        Text(value, color = palette.primaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EnhancerSliderRow(
    title: String,
    valueText: String,
    subtext: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val palette = LocalQuickMenuPalette.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = palette.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(valueText, color = palette.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Text(subtext, color = palette.secondaryText, fontSize = 10.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.secondaryText.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PlayerStatusPill(
    showClock: Boolean,
    showBattery: Boolean,
    showRemainingTime: Boolean = false,
    remainingSec: Long? = null,
    hdrBadge: String? = null,
    bgColor: Color = Color(0x990A0E17),
    rimColor: Color = Color.White.copy(alpha = 0.22f),
    contentColor: Color = Color.White,
    accentColor: Color = TimelineAccent,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var timeText by remember { mutableStateOf("") }
    var batteryText by remember { mutableStateOf("") }
    var isCharging by remember { mutableStateOf(false) }

    LaunchedEffect(showClock, showBattery) {
        while (true) {
            if (showClock) {
                timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
            }
            if (showBattery) {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                val pct = if (level >= 0 && scale > 0) (level * 100) / scale else -1
                batteryText = if (pct >= 0) "$pct%" else ""
            }
            kotlinx.coroutines.delay(10_000)
        }
    }

    val hasRemaining = showRemainingTime && remainingSec != null && remainingSec > 0
    val remainingText = if (hasRemaining) "−${compactTimecode(remainingSec!!)}" else ""

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
        border = BorderStroke(1.dp, rimColor),
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val hasClock = showClock && timeText.isNotEmpty()
            val hasBattery = showBattery && batteryText.isNotEmpty()

            if (!hdrBadge.isNullOrEmpty()) {
                Text(
                    text = hdrBadge,
                    color = Color(0xFFFFB300),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("·", color = contentColor.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (hasClock) {
                Text(
                    text = timeText,
                    color = contentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (hasClock && (hasBattery || hasRemaining)) {
                Text("·", color = contentColor.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (hasBattery) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                        contentDescription = "Battery level",
                        tint = if (isCharging) accentColor else contentColor.copy(alpha = 0.9f),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = batteryText,
                        color = contentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (hasBattery && hasRemaining) {
                Text("·", color = contentColor.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (hasRemaining) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Schedule,
                        contentDescription = "Remaining time",
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = remainingText,
                        color = contentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
// END OF FILE
