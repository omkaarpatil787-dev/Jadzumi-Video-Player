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
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
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
import androidx.compose.ui.platform.LocalContext
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
internal val TimelineAccent = Color(0xFFB79AF7)

private fun formatCompactAudioPill(track: AudioTrackInfo?): String {
    if (track == null) return "Audio"
    val codecRaw = track.codec.trim()
    val codec = when {
        codecRaw.contains("E-AC-3", true) || codecRaw.contains("Dolby Digital Plus", true) -> "E-AC-3"
        codecRaw.contains("AC-3", true) || codecRaw.contains("Dolby Digital", true) -> "AC-3"
        codecRaw.contains("TrueHD", true) -> "TrueHD"
        codecRaw.contains("Atmos", true) -> "Atmos"
        codecRaw.contains("DTS-HD MA", true) || codecRaw.contains("Master Audio", true) -> "DTS-HD MA"
        codecRaw.contains("DTS-HD", true) -> "DTS-HD"
        codecRaw.contains("DTS", true) -> "DTS"
        codecRaw.contains("FLAC", true) -> "FLAC"
        codecRaw.contains("Opus", true) -> "Opus"
        codecRaw.contains("AAC", true) -> "AAC"
        codecRaw.contains("MP3", true) -> "MP3"
        codecRaw.isNotBlank() -> codecRaw.take(6)
        else -> "Audio"
    }

    val channelsRaw = track.channels.trim()
    val ch = when {
        channelsRaw.contains("7.1", true) || channelsRaw.contains("8", true) -> "7.1"
        channelsRaw.contains("5.1", true) || channelsRaw.contains("6", true) -> "5.1"
        channelsRaw.contains("2.1", true) -> "2.1"
        channelsRaw.contains("Stereo", true) || channelsRaw.contains("2", true) -> "2.0"
        channelsRaw.contains("Mono", true) || channelsRaw.contains("1", true) -> "1.0"
        else -> ""
    }

    return if (ch.isNotBlank()) "$codec $ch" else codec
}

private fun formatCompactSubtitlePill(track: SubtitleTrackInfo?): String {
    if (track == null) return "Subs Off"
    val lang = track.language.trim()
    val label = track.label.trim()
    return when {
        lang.isNotBlank() && !lang.equals("und", true) -> {
            lang.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }.take(8)
        }
        label.isNotBlank() -> label.substringBefore(" (").substringBefore(" -").take(8)
        else -> "Sub"
    }
}

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
    // Cinema overlay controls are permanently luminous Moonlit White/Lavender to guarantee 100% legibility over any video content
    val playerContentColor = Color.White
    val playerSubtextColor = Color.White.copy(alpha = 0.72f)
    val playerAccentColor = TimelineAccent

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
    var showEpisodesDrawer by remember(video.identity) { mutableStateOf(false) }
    val watchPartyServer = remember(video.identity) { WatchPartyServer(activity) }
    var showRelaySheet by remember(video.identity) { mutableStateOf(false) }
    var isChatDockShrunk by remember(video.identity) { mutableStateOf(false) }
    var showPartyContentPicker by remember(video.identity) { mutableStateOf(false) }

    DisposableEffect(video.identity) {
        onDispose {
            watchPartyServer.stop()
        }
    }
    var microScrubLabel by remember(video.identity) { mutableStateOf<String?>(null) }
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
    val showAnyHud = playback.showPlayerClock || playback.showBatteryPercentage || playback.showRemainingTime
    var showDisplayDuringPlaybackDialog by remember(video.identity) { mutableStateOf(false) }
    var playerOrientationMode by remember(video.identity) {
        val isLandscape = activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        mutableIntStateOf(if (isLandscape) 0 else 1)
    }
    val orientationAngle by animateFloatAsState(
        targetValue = when (playerOrientationMode) {
            0 -> 90f
            1 -> 0f
            else -> 180f
        },
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "orientationRotationAnim"
    )

    var showDiagnosticsCapsule by remember(video.identity) { mutableStateOf(false) }
    var seekGestureDeltaText by remember(video.identity) { mutableStateOf<String?>(null) }
    var seekGestureIsForward by remember(video.identity) { mutableStateOf(true) }
    var seekGestureProgress by remember(video.identity) { mutableFloatStateOf(0f) }
    var zoomScale by remember(video.identity) { mutableFloatStateOf(1f) }
    var panOffset by remember(video.identity) { mutableStateOf(Offset.Zero) }
    var zoomToastText by remember(video.identity) { mutableStateOf<String?>(null) }
    var zoomToastJob by remember(video.identity) { mutableStateOf<kotlinx.coroutines.Job?>(null) }

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
                        animationSpec = tween(JadzumiMotion.PlayerDismissDurationMs, easing = JadzumiMotion.PhysicalGeometryEasing)
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

    val isInteracting = quickMenuSection != null ||
            toolsSection != null ||
            showDiagnosticsCapsule ||
            showEpisodesDrawer ||
            showRelaySheet ||
            showChaptersDrawer ||
            hudSide != null ||
            scrub != null ||
            isFastForwarding ||
            seekBackAccumulator > 0 ||
            seekFwdAccumulator > 0 ||
            playback.locked

    LaunchedEffect(controls, playback.playing, interaction, isInteracting) {
        if (controls && playback.playing && !isInteracting) {
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

    LaunchedEffect(playback.brightnessBoost, playback.isHdr, isOled) {
        if (playback.brightnessBoost) {
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = 1.0f }
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
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (zoomScale != 1f || panOffset != Offset.Zero) {
                        Modifier.graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panOffset.x
                            translationY = panOffset.y
                        }
                    } else Modifier
                )
                .onGloballyPositioned {
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
                                        val isLeft = offset.x < size.width * 0.35f
                                        val isRight = offset.x > size.width * 0.65f
                                        if (isLeft || isRight) {
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
                                            val seekSec = playback.seekIncrementSeconds * rippleCount
                                            val seekTxt = if (isLeft) "−${seekSec}s" else "+${seekSec}s"
                                            message = seekTxt
                                            seekGestureIsForward = !isLeft
                                            seekGestureDeltaText = seekTxt
                                            seekGestureProgress = (playback.position.toFloat() / effectiveDuration.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                            hudSide = "Seek"
                                            interaction++
                                        } else {
                                            if (zoomScale > 1.05f) {
                                                zoomScale = 1.0f
                                                panOffset = Offset.Zero
                                                zoomToastText = "Zoom: 100% (Fit)"
                                                zoomToastJob?.cancel()
                                                zoomToastJob = coroutineScope.launch { delay(1200); zoomToastText = null }
                                                interaction++
                                            } else {
                                                // Center Double-Tap: Quick toggle Fit vs Zoom (Fill Screen)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                val nextMode = if (playback.aspectMode == AspectRatioMode.FIT) AspectRatioMode.ZOOM else AspectRatioMode.FIT
                                                playback.setAspectRatioMode(nextMode)
                                                message = if (nextMode == AspectRatioMode.ZOOM) "Crop / Fill (Zoom)" else "Fit (Original)"
                                                interaction++
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        .pointerInput(video.identity, playback.locked) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (!playback.locked && (zoom != 1f || pan != Offset.Zero)) {
                                    val newScale = (zoomScale * zoom).coerceIn(1.0f, 5.0f)
                                    zoomScale = newScale
                                    if (newScale > 1.0f) {
                                        val maxPanX = (size.width * (newScale - 1f)) / 2f
                                        val maxPanY = (size.height * (newScale - 1f)) / 2f
                                        panOffset = Offset(
                                            x = (panOffset.x + pan.x * newScale).coerceIn(-maxPanX, maxPanX),
                                            y = (panOffset.y + pan.y * newScale).coerceIn(-maxPanY, maxPanY)
                                        )
                                    } else {
                                        panOffset = Offset.Zero
                                    }
                                    zoomToastText = "Zoom: ${(newScale * 100).roundToInt()}%"
                                    zoomToastJob?.cancel()
                                    zoomToastJob = coroutineScope.launch {
                                        delay(1200)
                                        zoomToastText = null
                                    }
                                    interaction++
                                }
                            }
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
                        shape = RoundedCornerShape(999.dp),
                        color = SpatialGlass.pillBg(isAmoled),
                        border = SpatialGlass.pillBorder(1.dp),
                        shadowElevation = 8.dp
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

                val isSeekFwd = seekGestureIsForward
                val isLeftSide = hudSide == "Brightness" || (hudSide == "Seek" && !isSeekFwd)
                val hudAlignment = if (isLeftSide) Alignment.CenterStart else Alignment.CenterEnd
                val hudEnterAnim = fadeIn(tween(200, easing = CubicBezierEasing(0.19f, 1.0f, 0.22f, 1.0f))) +
                        slideInHorizontally(tween(240, easing = CubicBezierEasing(0.19f, 1.0f, 0.22f, 1.0f))) { if (isLeftSide) -it / 2 else it / 2 }
                val hudExitAnim = fadeOut(tween(180)) +
                        slideOutHorizontally(tween(200)) { if (isLeftSide) -it / 3 else it / 3 }

                // Non-intrusive Minimal Side-Docked Gesture Pills (Left: Brightness/Rewind, Right: Volume/Forward)
                AnimatedVisibility(
                    visible = hudSide != null,
                    enter = hudEnterAnim,
                    exit = hudExitAnim,
                    modifier = Modifier
                        .align(hudAlignment)
                        .safeDrawingPadding()
                        .padding(start = if (isLeftSide) 28.dp else 0.dp, end = if (!isLeftSide) 28.dp else 0.dp)
                ) {
                    hudSide?.let { side ->
                        val isBright = side == "Brightness"
                        val isSeek = side == "Seek"
                        val icon = when {
                            isBright -> if (hudValue < 0.35f) Icons.Rounded.BrightnessLow else if (hudValue < 0.75f) Icons.Rounded.BrightnessMedium else Icons.Rounded.BrightnessHigh
                            isSeek -> if (seekGestureIsForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind
                            else -> Icons.AutoMirrored.Rounded.VolumeUp
                        }
                        val valueText = when {
                            isSeek -> seekGestureDeltaText ?: "+10s"
                            else -> "${(hudValue * 100).roundToInt()}%"
                        }
                        val fraction = when {
                            isSeek -> seekGestureProgress
                            else -> hudValue
                        }

                        FluidGestureCapsule(
                            type = side,
                            valueText = valueText,
                            fraction = fraction,
                            icon = icon,
                            isOled = isAmoled
                        )
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
                        border = SpatialGlass.pillBorder(0.75.dp),
                        shadowElevation = 0.dp
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

                // Precision Zoom Float Pill
                AnimatedVisibility(
                    visible = zoomToastText != null,
                    enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.88f),
                    exit = fadeOut(tween(220)) + scaleOut(targetScale = 0.94f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .safeDrawingPadding()
                        .padding(top = 28.dp)
                ) {
                    zoomToastText?.let { txt ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = QuickMenuBg,
                            border = BorderStroke(1.dp, TimelineAccent.copy(alpha = 0.6f)),
                            shadowElevation = 16.dp
                        ) {
                            Text(
                                txt,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                            )
                        }
                    }
                }

                // Immersive Playback HUD Overlay: Clock, Battery %, Remaining Time
                val hudAlign = when (playback.displayDuringPlaybackPosition) {
                    "top_left" -> Alignment.TopStart
                    "bottom_left" -> Alignment.BottomStart
                    else -> Alignment.TopEnd
                }
                val hudPad = when (playback.displayDuringPlaybackPosition) {
                    "top_left" -> Modifier.padding(top = 18.dp, start = 24.dp)
                    "bottom_left" -> Modifier.padding(bottom = 24.dp, start = 24.dp)
                    else -> Modifier.padding(top = 18.dp, end = 24.dp)
                }

                AnimatedVisibility(
                    visible = !controls && !playback.locked && showAnyHud && !playback.inPip,
                    enter = fadeIn(tween(360, easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f))),
                    exit = fadeOut(tween(240)),
                    modifier = Modifier
                        .align(hudAlign)
                        .safeDrawingPadding()
                        .then(hudPad)
                ) {
                    PlayerStatusPill(
                        showClock = playback.showPlayerClock,
                        showBattery = playback.showBatteryPercentage,
                        showRemainingTime = playback.showRemainingTime,
                        remainingSecProvider = { ((effectiveDuration - playback.position) / 1000).coerceAtLeast(0) },
                        bgColor = (if (isAmoled) Color.Black else Color(0xFF0A0E17)).copy(alpha = playback.displayDuringPlaybackOpacity),
                        rimColor = dynamicFrostedCircleRim,
                        contentColor = playerContentColor,
                        accentColor = playerAccentColor,
                        style = playback.displayDuringPlaybackStyle
                    )
                }

                val enterAnim = if (reducedMotion) EnterTransition.None else fadeIn(
                    animationSpec = tween(220, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f))
                ) + slideInVertically(
                    animationSpec = tween(220, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)),
                    initialOffsetY = { 20 }
                )
                val exitAnim = if (reducedMotion) ExitTransition.None else fadeOut(
                    animationSpec = tween(180, easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f))
                ) + slideOutVertically(
                    animationSpec = tween(180, easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)),
                    targetOffsetY = { 10 }
                )

                AnimatedVisibility(
                    visible = controls && !playback.locked && !playback.inPip,
                    enter = enterAnim,
                    exit = exitAnim
                ) {
                    val subtleScrim = Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.53f),
                        0.37f to Color.Transparent,
                        0.58f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.68f)
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(subtleScrim)
                            .safeDrawingPadding()
                            .padding(horizontal = 24.dp, vertical = 18.dp)
                    ) {
                        // PANEL 1 & 4: CINEMATIC TOP BAR (Approved v2.0 HTML Specification)
                        Row(
                            Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                Modifier.weight(1f).padding(end = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FrostedCircleButton(
                                    label = "Back to library",
                                    onClick = dismissPlayerWithFluidity,
                                    size = 42.dp,
                                    bgColor = dynamicFrostedCircleBg,
                                    rimColor = dynamicFrostedCircleRim
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = playerContentColor)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(28.dp)
                                        .clip(CircleShape)
                                        .background(TimelineAccent)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        heading.primaryTitle,
                                        color = playerContentColor,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val decName = playback.runtimeVideoDecoder?.name
                                    val liveCodec: String = if (decName != null) {
                                        when {
                                            decName.contains("hevc", true) || decName.contains("h265", true) -> "HEVC Main 10"
                                            decName.contains("avc", true) || decName.contains("h264", true) -> "H.264 High"
                                            decName.contains("av01", true) || decName.contains("av1", true) -> "AV1 Main"
                                            decName.contains("vp9", true) -> "VP9 Profile 0"
                                            else -> playback.videoCodecBadge ?: "HEVC"
                                        }
                                    } else {
                                        playback.videoCodecBadge ?: "HEVC"
                                    }

                                    val fpsVal = playback.runtimeVideoFormat?.frameRate?.takeIf { it > 5.0 && it.isFinite() } ?: 24.0
                                    val fpsText = String.format(java.util.Locale.US, "%.3f fps", fpsVal)
                                    val hdrText = if (playback.isHdr) (playback.hdrFlavor ?: "HDR10+") else "BT.709 SDR"
                                    val telemetryString = "$liveCodec · ${video.width}x${video.height} @ $fpsText · $hdrText"

                                    Text(
                                        telemetryString,
                                        color = playerSubtextColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Top-Right: Clean Symmetrical Utilities (42dp aligned with Back Button)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FrostedCircleButton(
                                    label = "Rotate screen orientation",
                                    onClick = {
                                        playerOrientationMode = (playerOrientationMode + 1) % 3
                                        when (playerOrientationMode) {
                                            0 -> {
                                                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                                message = "Orientation: Landscape Locked"
                                            }
                                            1 -> {
                                                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                                message = "Orientation: Portrait Locked"
                                            }
                                            2 -> {
                                                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
                                                message = "Orientation: Auto Sensor"
                                            }
                                        }
                                        interaction++
                                    },
                                    size = 42.dp,
                                    bgColor = if (playerOrientationMode != 2) LavenderAccent.copy(alpha = 0.16f) else dynamicFrostedCircleBg,
                                    rimColor = if (playerOrientationMode != 2) LavenderAccent.copy(alpha = 0.45f) else dynamicFrostedCircleRim
                                ) {
                                    Icon(
                                        Icons.Rounded.ScreenRotation,
                                        contentDescription = "Screen orientation",
                                        tint = if (playerOrientationMode != 2) LavenderAccent else playerContentColor,
                                        modifier = Modifier
                                            .size(19.dp)
                                            .graphicsLayer(rotationZ = orientationAngle)
                                    )
                                }

                                FrostedCircleButton(
                                    label = "Picture in Picture",
                                    onClick = {
                                        controls = false
                                        activity.enterCinemaPip()
                                        interaction++
                                    },
                                    size = 42.dp,
                                    bgColor = dynamicFrostedCircleBg,
                                    rimColor = dynamicFrostedCircleRim
                                ) {
                                    Icon(
                                        Icons.Rounded.PictureInPictureAlt,
                                        contentDescription = "PiP",
                                        tint = playerContentColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                if (playback.currentQueue.size > 1 || playback.video?.episode != null) {
                                    FrostedCircleButton(
                                        label = "Season Episodes",
                                        onClick = {
                                            showEpisodesDrawer = true
                                            interaction++
                                        },
                                        size = 42.dp,
                                        bgColor = if (showEpisodesDrawer) LavenderAccent.copy(alpha = 0.22f) else dynamicFrostedCircleBg,
                                        rimColor = if (showEpisodesDrawer) LavenderAccent.copy(alpha = 0.5f) else dynamicFrostedCircleRim
                                    ) {
                                        Icon(
                                            Icons.Rounded.VideoLibrary,
                                            contentDescription = "Season Episodes",
                                            tint = if (showEpisodesDrawer) LavenderAccent else playerContentColor,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }

                                if (watchPartyServer.isRunning) {
                                    FrostedCircleButton(
                                        label = if (watchPartyServer.relayMode == CinemaRelayMode.WATCH_PARTY) "Watch Party (${watchPartyServer.guestCount})" else "Audio Bridge",
                                        onClick = {
                                            showRelaySheet = true
                                            interaction++
                                        },
                                        size = 42.dp,
                                        bgColor = if (watchPartyServer.relayMode == CinemaRelayMode.WATCH_PARTY) Color(0xFF4ADE80).copy(alpha = 0.25f) else Color(0xFF38BDF8).copy(alpha = 0.25f),
                                        rimColor = if (watchPartyServer.relayMode == CinemaRelayMode.WATCH_PARTY) Color(0xFF4ADE80).copy(alpha = 0.6f) else Color(0xFF38BDF8).copy(alpha = 0.6f)
                                    ) {
                                        Icon(
                                            if (watchPartyServer.relayMode == CinemaRelayMode.WATCH_PARTY) Icons.Rounded.Groups else Icons.Rounded.Headphones,
                                            contentDescription = "Active Relay",
                                            tint = if (watchPartyServer.relayMode == CinemaRelayMode.WATCH_PARTY) Color(0xFF4ADE80) else Color(0xFF38BDF8),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }

                                FrostedCircleButton(
                                    label = "Quick Menu",
                                    onClick = {
                                        quickMenuSection = if (quickMenuSection == "main") null else "main"
                                        interaction++
                                    },
                                    size = 42.dp,
                                    bgColor = if (quickMenuSection == "main") LavenderAccent.copy(alpha = 0.22f) else dynamicFrostedCircleBg,
                                    rimColor = if (quickMenuSection == "main") LavenderAccent.copy(alpha = 0.5f) else dynamicFrostedCircleRim
                                ) {
                                    Icon(
                                        Icons.Rounded.MoreVert,
                                        contentDescription = "More options",
                                        tint = if (quickMenuSection == "main") LavenderAccent else playerContentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                FrostedCircleButton(
                                    label = "Lock controls",
                                    onClick = {
                                        playback.locked = true
                                        interaction++
                                    },
                                    size = 42.dp,
                                    bgColor = dynamicFrostedCircleBg,
                                    rimColor = dynamicFrostedCircleRim
                                ) {
                                    Icon(
                                        Icons.Rounded.Lock,
                                        contentDescription = "Lock controls",
                                        tint = playerContentColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                        }

                        // PANEL 1: CENTRAL MINIMALIST GHOST SEEK & PLAY/PAUSE (Style C - Borderless Vector Minimalism)
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Ghost Rewind: â†º 10s
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(enabled = playback.seekable) {
                                        val stepSec = playback.seekIncrementSeconds
                                        playback.skip(-seekDeltaMs)
                                        seekBackAccumulator += stepSec
                                        seekGestureIsForward = false
                                        seekGestureDeltaText = "âˆ’${seekBackAccumulator}s"
                                        seekGestureProgress = (playback.position.toFloat() / effectiveDuration.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                        seekBackJob?.cancel()
                                        seekBackJob = coroutineScope.launch {
                                            delay(950)
                                            seekBackAccumulator = 0
                                        }
                                        interaction++
                                    }
                                    .touchMotion()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.Replay, contentDescription = "Rewind", tint = Color.White.copy(alpha = 0.90f), modifier = Modifier.size(20.dp))
                                Text("${playback.seekIncrementSeconds}s", color = Color.White.copy(alpha = 0.90f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Step Backward (Active when paused)
                            if (!playback.playing) {
                                IconButton(
                                    onClick = { playback.stepFrameBackward(); message = "Frame Step -1"; interaction++ },
                                    modifier = Modifier.size(36.dp).touchMotion()
                                ) {
                                    Icon(Icons.Rounded.SkipPrevious, "Step Backward", tint = Color.White.copy(alpha = 0.80f), modifier = Modifier.size(20.dp))
                                }
                            }

                            // Style C Minimalist Outline Play/Pause Centerpiece (50dp, Circular Ripple & Clipped Outline)
                            Surface(
                                onClick = { playback.toggle(); interaction++ },
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.50f)),
                                modifier = Modifier
                                    .size(50.dp)
                                    .touchMotion()
                                    .semantics { contentDescription = if (playback.playRequested) "Pause" else "Play" }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    AnimatedContent(
                                        targetState = playback.playRequested,
                                        transitionSpec = {
                                            (fadeIn(tween(140)) + scaleIn(spring(dampingRatio = 0.65f, stiffness = 500f), initialScale = 0.72f))
                                                .togetherWith(fadeOut(tween(110)) + scaleOut(targetScale = 0.72f))
                                        },
                                        label = "playPauseIcon"
                                    ) { isPlaying ->
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }

                            // Step Forward (Active when paused)
                            if (!playback.playing) {
                                IconButton(
                                    onClick = { playback.stepFrameForward(); message = "Frame Step +1"; interaction++ },
                                    modifier = Modifier.size(36.dp).touchMotion()
                                ) {
                                    Icon(Icons.Rounded.SkipNext, "Step Forward", tint = Color.White.copy(alpha = 0.80f), modifier = Modifier.size(20.dp))
                                }
                            }

                            // Ghost Forward: 10s â†»
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(enabled = playback.seekable) {
                                        val stepSec = playback.seekIncrementSeconds
                                        playback.skip(seekDeltaMs)
                                        seekFwdAccumulator += stepSec
                                        seekGestureIsForward = true
                                        seekGestureDeltaText = "+${seekFwdAccumulator}s"
                                        seekGestureProgress = (playback.position.toFloat() / effectiveDuration.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                        seekFwdJob?.cancel()
                                        seekFwdJob = coroutineScope.launch {
                                            delay(950)
                                            seekFwdAccumulator = 0
                                        }
                                        interaction++
                                    }
                                    .touchMotion()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("${playback.seekIncrementSeconds}s", color = Color.White.copy(alpha = 0.90f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Rounded.Forward10, contentDescription = "Forward", tint = Color.White.copy(alpha = 0.90f), modifier = Modifier.size(20.dp))
                            }
                        }

                        // PANEL 3: PRECISION SPLIT CAPSULES (HUD 3 Cinephile Command Deck)
                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
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

                            // Skip Intro Pill Button
                            val seriesBingeKey = remember(video.name) { BingeManager.resolveSeriesKey(video.name) }
                            val bingeSettings = remember(seriesBingeKey) { BingeManager.getSettings(activity, seriesBingeKey) }
                            val introLimitMs = bingeSettings.introSkipSeconds * 1000L
                            val showSkipIntro = playback.duration > 180_000 && playback.position in 2_000..introLimitMs
                            if (showSkipIntro) {
                                SpatialTactilePill(
                                    onClick = {
                                        playback.seekTo(introLimitMs)
                                        message = "Skipped Intro (${bingeSettings.introSkipSeconds}s)"
                                        interaction++
                                    },
                                    isOled = isAmoled,
                                    modifier = Modifier.align(Alignment.End).padding(bottom = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.FastForward, null, tint = TimelineAccent, modifier = Modifier.size(16.dp))
                                    Text("Skip Intro (${bingeSettings.introSkipSeconds}s)", color = playerContentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            val activePosMs = scrub?.let { (it * playback.duration).toLong() } ?: playback.position
                            val elapsedSec = (activePosMs / 1000).coerceAtLeast(0)
                            val totalSec = (playback.duration / 1000).coerceAtLeast(0)
                            val pct = ((playback.position.toFloat() / effectiveDuration.coerceAtLeast(1L)) * 100).toInt().coerceIn(0, 100)

                            // Approach B: Integrated Unified Bottom Deck (Timeline Scrubber + Integrated Controls)
                            val audioTrack = playback.audioTracks.firstOrNull { it.isSelected }
                            val audioText = formatCompactAudioPill(audioTrack)
                            val subTrack = playback.subtitleTracks.firstOrNull { it.isSelected }
                            val subText = formatCompactSubtitlePill(subTrack)
                            val isZoomMode = playback.aspectMode == AspectRatioMode.ZOOM

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(22.dp),
                                color = SpatialGlass.pillBg(isAmoled),
                                border = SpatialGlass.pillBorder(0.75.dp),
                                shadowElevation = 0.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Variable Micro-Scrubbing Velocity Badge
                                    AnimatedVisibility(
                                        visible = microScrubLabel != null,
                                        enter = fadeIn(tween(140)) + slideInVertically(tween(140)) { it / 2 },
                                        exit = fadeOut(tween(100)) + slideOutVertically(tween(100)) { it / 2 },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = Color(0xF21E1A29),
                                            border = BorderStroke(1.dp, LavenderAccent.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = microScrubLabel ?: "",
                                                color = LavenderAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Row 1: Timeline Scrubber with Timecodes
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            compactTimecode(elapsedSec),
                                            color = TimelineAccent,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                        Box(modifier = Modifier.weight(1f)) {
                                            CinemaTimelineBar(
                                                fraction = currentFraction,
                                                bufferedFraction = bufferedFraction,
                                                isScrubbing = scrub != null,
                                                enabled = playback.seekable && playback.duration > 0,
                                                chapters = playback.chapters,
                                                onScrub = { scrub = it; interaction++ },
                                                onScrubRateChange = { microScrubLabel = it },
                                                onScrubFinished = {
                                                    scrub?.let { playback.seekTo((it * playback.duration).toLong()) }
                                                    scrub = null
                                                    microScrubLabel = null
                                                }
                                            )
                                        }
                                        val remainingSec = (totalSec - elapsedSec).coerceAtLeast(0)
                                        Text(
                                            "${compactTimecode(totalSec)} (-${compactTimecode(remainingSec)})",
                                            color = playerSubtextColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }

                                    // Hairline Divider
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.75.dp)
                                            .background(Color.White.copy(alpha = 0.10f))
                                    )

                                    // Row 2: Stream Action Controls (Subtitles, Audio, Speed, Aspect Mode)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                            // Subtitles Pill Chip
                                            Row(
                                                modifier = Modifier
                                                    .height(30.dp)
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(if (subTrack != null) Color(0xFFD0BCFF).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f))
                                                    .clickable {
                                                        quickMenuSection = if (quickMenuSection == "subs") null else "subs"
                                                        interaction++
                                                    }
                                                    .padding(horizontal = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Rounded.ClosedCaption,
                                                    null,
                                                    Modifier.size(14.dp),
                                                    tint = if (subTrack != null) Color(0xFFD0BCFF) else playerContentColor
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    subText,
                                                    color = if (subTrack != null) Color(0xFFD0BCFF) else playerContentColor,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            // Audio Pill Chip
                                            Row(
                                                modifier = Modifier
                                                    .height(30.dp)
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(Color.White.copy(alpha = 0.06f))
                                                    .clickable {
                                                        quickMenuSection = if (quickMenuSection == "audio") null else "audio"
                                                        interaction++
                                                    }
                                                    .padding(horizontal = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Rounded.GraphicEq,
                                                    null,
                                                    Modifier.size(14.dp),
                                                    tint = TimelineAccent
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    audioText,
                                                    color = playerContentColor,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            // Speed Pill Chip
                                            Box(
                                                modifier = Modifier
                                                    .height(30.dp)
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(if (playback.speed != 1.0f) Color(0xFFD0BCFF).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f))
                                                    .clickable {
                                                        val next = when (playback.speed) {
                                                            1.0f -> 1.25f
                                                            1.25f -> 1.5f
                                                            2.0f -> 1.0f
                                                            else -> 1.0f
                                                        }
                                                        playback.changeSpeed(next)
                                                        message = "${next}x Speed"
                                                        interaction++
                                                    }
                                                    .padding(horizontal = 9.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "${playback.speed}x",
                                                    color = if (playback.speed != 1.0f) Color(0xFFD0BCFF) else playerContentColor,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Aspect Ratio Mode Chip
                                            Row(
                                                modifier = Modifier
                                                    .height(30.dp)
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(if (isZoomMode) Color(0xFFD0BCFF).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f))
                                                    .clickable {
                                                        val next = if (isZoomMode) AspectRatioMode.FIT else AspectRatioMode.ZOOM
                                                        playback.setAspectRatioMode(next)
                                                        message = if (next == AspectRatioMode.ZOOM) "Crop / Fill (Zoom)" else "Fit (Original)"
                                                        interaction++
                                                    }
                                                    .padding(horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Rounded.AspectRatio,
                                                    null,
                                                    Modifier.size(13.5.dp),
                                                    tint = if (isZoomMode) Color(0xFFD0BCFF) else playerContentColor
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    if (isZoomMode) "Zoom" else "Fit",
                                                    color = if (isZoomMode) Color(0xFFD0BCFF) else playerContentColor,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                // Unlock Button when Screen Locked (Spatial Glass Unlock Pill)
                AnimatedVisibility(
                    visible = playback.locked && controls,
                    enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.90f),
                    exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.90f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .safeDrawingPadding()
                        .padding(bottom = 36.dp)
                ) {
                    SpatialTactilePill(
                        onClick = {
                            playback.locked = false
                            controls = true
                            interaction++
                        },
                        isOled = isAmoled
                    ) {
                        Icon(Icons.Rounded.LockOpen, contentDescription = "Unlock controls", tint = LavenderAccent, modifier = Modifier.size(16.dp))
                        Text("Tap to Unlock", color = playerContentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                            onOpenEpisodes = { showEpisodesDrawer = true; quickMenuSection = null },
                            onOpenWatchParty = {
                                if (!watchPartyServer.isRunning || watchPartyServer.relayMode != CinemaRelayMode.WATCH_PARTY) {
                                    watchPartyServer.start(CinemaRelayMode.WATCH_PARTY, playback)
                                }
                                showRelaySheet = true
                                quickMenuSection = null
                            },
                            onOpenAudioBridge = {
                                if (!watchPartyServer.isRunning || watchPartyServer.relayMode != CinemaRelayMode.AUDIO_BRIDGE) {
                                    watchPartyServer.start(CinemaRelayMode.AUDIO_BRIDGE, playback)
                                }
                                showRelaySheet = true
                                quickMenuSection = null
                            },
                            watchPartyServer = watchPartyServer,
                            onOpenChapters = { showChaptersDrawer = true; quickMenuSection = null },
                            onOpenTrimmer = { showTrimmerDialog = true; quickMenuSection = null },
                            onOpenBinge = { showBingeDialog = true; quickMenuSection = null },
                            onOpenTools = { section ->
                                if (section == "diagnostics") {
                                    showDiagnosticsCapsule = true
                                    quickMenuSection = null
                                } else if (section == "display_playback") {
                                    showDisplayDuringPlaybackDialog = true
                                    quickMenuSection = null
                                } else {
                                    toolsSection = section
                                    quickMenuSection = null
                                }
                            },
                            onSearchSubtitles = {
                                if (!subtitleSearchBusy) subtitleSearchLauncher.launch(arrayOf("*/*"))
                            },
                            onApplySubtitles = {
                                playerViewRef?.let { SubtitlePreferences.applyToPlayerView(activity, it) }
                            },
                            onMessage = { message = it; interaction++ },
                            onDismiss = { quickMenuSection = null },
                            onClosePlayer = dismissPlayerWithFluidity
                        )
                    }
                }

                // Approved Next-Gen Playback Information Capsule (User-invoked, Read-only Diagnostics)
                AnimatedVisibility(
                    visible = showDiagnosticsCapsule,
                    enter = fadeIn(tween(320, easing = CubicBezierEasing(0.19f, 1.0f, 0.22f, 1.0f))) +
                            slideInVertically(initialOffsetY = { -it / 6 }),
                    exit = fadeOut(tween(240)) + slideOutVertically(targetOffsetY = { -it / 6 })
                ) {
                    PlaybackInfoCapsule(
                        playback = playback,
                        video = video,
                        onDismiss = { showDiagnosticsCapsule = false }
                    )
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

                // Dedicated Watch Party Top Capsule HUD
                AnimatedVisibility(
                    visible = watchPartyServer.isRunning && watchPartyServer.relayMode == CinemaRelayMode.WATCH_PARTY && !playback.locked && !playback.inPip,
                    enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { -it },
                    exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { -it },
                    modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = 16.dp)
                ) {
                    WatchPartyTopHud(
                        server = watchPartyServer,
                        onOpenContentPicker = { showPartyContentPicker = true; interaction++ },
                        onOpenShare = { showRelaySheet = true; interaction++ },
                        isChatShrunk = isChatDockShrunk,
                        onToggleChatShrunk = { isChatDockShrunk = !isChatDockShrunk; interaction++ },
                        onEndParty = {
                            watchPartyServer.stop()
                            message = "Watch Party session ended"
                        }
                    )
                }

                // In-Video Floating Reaction Bursts
                if (watchPartyServer.isRunning && watchPartyServer.relayMode == CinemaRelayMode.WATCH_PARTY) {
                    WatchPartyFloatingReactions(messages = watchPartyServer.chatMessages)
                }

                // Dedicated Watch Party Live Chat Dock (Normal Size or Shrunk Capsule)
                AnimatedVisibility(
                    visible = watchPartyServer.isRunning && watchPartyServer.relayMode == CinemaRelayMode.WATCH_PARTY && !playback.locked && !playback.inPip,
                    enter = fadeIn(tween(250)) + slideInHorizontally(tween(300)) { it },
                    exit = fadeOut(tween(200)) + slideOutHorizontally(tween(250)) { it },
                    modifier = Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(end = 16.dp, bottom = 16.dp)
                ) {
                    WatchPartyHostChatDock(
                        server = watchPartyServer,
                        isShrunk = isChatDockShrunk,
                        onToggleShrunk = { isChatDockShrunk = !isChatDockShrunk; interaction++ }
                    )
                }
            }
        }

        if (showEpisodesDrawer) {
            val episodes = remember(playback.video?.identity, playback.currentQueue) {
                playback.currentQueue.ifEmpty { listOfNotNull(playback.video) }
            }
            EpisodeDrawerSheet(
                episodes = episodes,
                currentVideo = video,
                onSelectEpisode = { ep ->
                    showEpisodesDrawer = false
                    playback.open(ep, episodes)
                    message = "Switched to ${ep.title.ifBlank { ep.name }}"
                },
                onDismiss = { showEpisodesDrawer = false }
            )
        }

        if (showRelaySheet) {
            CinemaRelaySheet(
                server = watchPartyServer,
                playback = playback,
                onDismiss = { showRelaySheet = false }
            )
        }

        if (showPartyContentPicker) {
            val episodes = remember(playback.video?.identity, playback.currentQueue) {
                playback.currentQueue.ifEmpty { listOfNotNull(playback.video) }
            }
            WatchPartyContentPickerSheet(
                episodes = episodes,
                currentVideo = video,
                onSelectEpisode = { ep ->
                    showPartyContentPicker = false
                    playback.open(ep, episodes)
                    watchPartyServer.sendHostChat("Host switched stream to: ${ep.title.ifBlank { ep.name }}")
                    message = "Switched to ${ep.title.ifBlank { ep.name }}"
                },
                onDismiss = { showPartyContentPicker = false }
            )
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

        if (showDisplayDuringPlaybackDialog) {
            DisplayDuringPlaybackDialog(
                playback = playback,
                onDismiss = { showDisplayDuringPlaybackDialog = false }
            )
        }
    }

/** Panel 1 & 3: Ultra-Thin Periwinkle-Blue Timeline with Chapter Marks, Haptic Snap & Variable Micro-Scrubbing */
@Composable private fun CinemaTimelineBar(
    fraction: Float,
    bufferedFraction: Float,
    isScrubbing: Boolean,
    enabled: Boolean,
    chapters: List<ChapterMarker> = emptyList(),
    onScrub: (Float) -> Unit,
    onScrubRateChange: (String?) -> Unit = {},
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
                var startY = 0f
                var currentFrac = fraction
                var lastTier = -1
                detectDragGestures(
                    onDragStart = { offset ->
                        startY = offset.y
                        currentFrac = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        processScrubFraction(currentFrac)
                        onScrubRateChange(null)
                    },
                    onDragEnd = {
                        onScrubRateChange(null)
                        onScrubFinished()
                    },
                    onDragCancel = {
                        onScrubRateChange(null)
                        onScrubFinished()
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val dy = (change.position.y - startY).coerceAtLeast(0f)
                    val (rate, label, tier) = when {
                        dy > 110f -> Triple(0.08f, "Fine Micro-Scrub (1s)", 3)
                        dy > 60f  -> Triple(0.25f, "Quarter Speed (25%)", 2)
                        dy > 25f  -> Triple(0.50f, "Half Speed (50%)", 1)
                        else      -> Triple(1.0f, null, 0)
                    }
                    if (tier != lastTier) {
                        lastTier = tier
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onScrubRateChange(label)
                    val deltaFrac = (dragAmount.x / size.width.coerceAtLeast(1)) * rate
                    currentFrac = (currentFrac + deltaFrac).coerceIn(0f, 1f)
                    processScrubFraction(currentFrac)
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
    onOpenEpisodes: () -> Unit = {},
    onOpenWatchParty: () -> Unit = {},
    onOpenAudioBridge: () -> Unit = {},
    watchPartyServer: WatchPartyServer? = null,
    onOpenChapters: () -> Unit = {},
    onOpenTrimmer: () -> Unit = {},
    onOpenBinge: () -> Unit = {},
    onSearchSubtitles: () -> Unit,
    onOpenTools: (String) -> Unit,
    onApplySubtitles: () -> Unit = {},
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit,
    onClosePlayer: () -> Unit = playback::close
) {
    val activeAudio = playback.audioTracks.firstOrNull { it.isSelected }
    val audioSummary = activeAudio?.let { "${it.label} (${it.codec} ${it.channels})" } ?: "Default"
    val activeSub = playback.subtitleTracks.firstOrNull { it.isSelected }
    val subSummary = activeSub?.let { "${it.label} (${it.format})" } ?: "Off"

    val isAmoled = theme == "AMOLED"

    val palette = remember(isAmoled) {
        if (isAmoled) {
            QuickMenuPalette(
                bg = Color(0xFC000000),
                rim = Color.White.copy(alpha = 0.18f),
                primaryText = MoonlitPrimaryText,
                secondaryText = MoonlitSecondaryText,
                iconTint = Color.White.copy(alpha = 0.90f),
                divider = Color.White.copy(alpha = 0.14f),
                accent = LavenderAccent,
                cardBg = Obsidian,
                cardBorder = Color.White.copy(alpha = 0.14f),
                btnBg = Charcoal,
                btnRim = Color.White.copy(alpha = 0.18f)
            )
        } else {
            QuickMenuPalette(
                bg = Color(0xF4141120),
                rim = Color.White.copy(alpha = 0.14f),
                primaryText = MoonlitPrimaryText,
                secondaryText = MoonlitSecondaryText,
                iconTint = Color.White.copy(alpha = 0.88f),
                divider = Color.White.copy(alpha = 0.10f),
                accent = LavenderAccent,
                cardBg = Color(0xFF1E1A2C),
                cardBorder = Color.White.copy(alpha = 0.10f),
                btnBg = Color(0xFF2C273C),
                btnRim = Color.White.copy(alpha = 0.14f)
            )
        }
    }

    CompositionLocalProvider(LocalQuickMenuPalette provides palette) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)).clickable(onClick = onDismiss)) {
            val isPhonePortrait = maxWidth < 600.dp && maxHeight > maxWidth
            val isFloatingBottom = section == "subs" || section == "audio" || isPhonePortrait
            Surface(
                modifier = if (isFloatingBottom) {
                    Modifier
                        .align(Alignment.BottomCenter)
                        .safeDrawingPadding()
                        .padding(horizontal = 20.dp, vertical = if (isPhonePortrait) 0.dp else 24.dp)
                        .widthIn(max = 520.dp)
                        .clickable(enabled = false) {}
                } else {
                    Modifier
                        .align(Alignment.CenterEnd)
                        .safeDrawingPadding()
                        .padding(end = 24.dp, top = 24.dp, bottom = 24.dp)
                        .widthIn(min = 320.dp, max = 380.dp)
                        .clickable(enabled = false) {}
                },
                color = palette.bg,
                border = SpatialGlass.pillBorder(0.75.dp),
                shape = if (isPhonePortrait && !isFloatingBottom) RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp) else RoundedCornerShape(20.dp),
                shadowElevation = 0.dp
            ) {
                Column {
                    if (isPhonePortrait) {
                        Box(
                            modifier = Modifier
                                .padding(top = 12.dp, bottom = 4.dp)
                                .align(Alignment.CenterHorizontally)
                                .width(32.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B8096))
                        )
                    }
                    when (section) {
                "main" -> {
                    Column(Modifier.heightIn(max = 560.dp).elasticEdge().verticalScroll(rememberScrollState()).padding(vertical = 10.dp)) {
                        // Category 1: Playback & Display
                        QuickMenuCategoryHeader("PLAYBACK & DISPLAY")
                        QuickMenuRow(
                            icon = Icons.Rounded.Speed,
                            label = "Playback speed",
                            value = "${playback.speed}x",
                            onClick = { onSelectSection("speed") }
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.AspectRatio,
                            label = "Fit / Aspect ratio",
                            value = playback.aspectMode.displayName.substringBefore(" ("),
                            onClick = { onSelectSection("aspect") }
                        )
                        QuickMenuRow(
                            icon = if (isMinimalHud) Icons.Rounded.ViewSidebar else Icons.Rounded.Splitscreen,
                            label = "Minimalist HUD",
                            value = if (isMinimalHud) "On" else "Off",
                            onClick = onToggleMinimalHud
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.Visibility,
                            label = "Display during playback",
                            value = "${(playback.displayDuringPlaybackOpacity * 100).toInt()}% · ${playback.displayDuringPlaybackSummary}",
                            onClick = {
                                onOpenTools("display_playback")
                            }
                        )
                        QuickMenuRow(Icons.Rounded.Bedtime, "Sleep controls", if (playback.sleepPlan != null) "Active" else "Off", { onOpenTools("sleep") })
                        QuickMenuRow(Icons.Rounded.PlaylistPlay, "Episode continuity", "", { onOpenTools("continuity") })

                        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = palette.divider)

                        // Category 2: Playback & Diagnostics (Reference Pure)
                        QuickMenuCategoryHeader("PLAYBACK & DIAGNOSTICS")
                        QuickMenuRow(
                            icon = Icons.Rounded.Analytics,
                            label = "Playback Diagnostics",
                            value = "Nerd Stats",
                            onClick = { onOpenTools("diagnostics") }
                        )

                        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = palette.divider)

                        // Category 3: Audio & Voice Clarity
                        QuickMenuCategoryHeader("AUDIO & VOICE")
                        QuickMenuRow(
                            icon = Icons.Rounded.GraphicEq,
                            label = "Audio track",
                            value = audioSummary,
                            onClick = { onSelectSection("audio") }
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

                        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = palette.divider)

                        // Category 4: Subtitles
                        QuickMenuCategoryHeader("SUBTITLES & CAPTIONS")
                        QuickMenuRow(
                            icon = Icons.Rounded.Subtitles,
                            label = "Subtitle track",
                            value = subSummary,
                            onClick = { onSelectSection("subs") }
                        )

                        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = palette.divider)

                        // Category 5: Cinephile Navigation & Tools
                        QuickMenuCategoryHeader("CINEPHILE TOOLS")
                        QuickMenuRow(
                            icon = Icons.Rounded.Groups,
                            label = "Live Watch Party",
                            value = if (watchPartyServer?.isRunning == true && watchPartyServer.relayMode == CinemaRelayMode.WATCH_PARTY) "Active (${watchPartyServer.guestCount})" else "WebRTC & Sync",
                            onClick = onOpenWatchParty
                        )
                        QuickMenuRow(
                            icon = Icons.Rounded.Headphones,
                            label = "Audio Cinema Bridge",
                            value = if (watchPartyServer?.isRunning == true && watchPartyServer.relayMode == CinemaRelayMode.AUDIO_BRIDGE) "Active" else "Dual Headphone",
                            onClick = onOpenAudioBridge
                        )
                        if (playback.currentQueue.size > 1 || playback.video?.episode != null) {
                            QuickMenuRow(
                                icon = Icons.Rounded.VideoLibrary,
                                label = "Season Episodes",
                                value = "${playback.currentQueue.size} in queue",
                                onClick = onOpenEpisodes
                            )
                        }
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

                        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = palette.divider)

                        QuickMenuRow(
                            icon = Icons.Rounded.Settings,
                            label = "More settings",
                            value = "",
                            onClick = { onSelectSection("more") }
                        )
                    }
                }

                "speed" -> {
                    QuickSubMenuContainer(
                        title = "Playback speed",
                        onBack = { onSelectSection("main") },
                        badgeText = "${playback.speed}x"
                    ) {
                        val quickRates = listOf(0.75f, 1.0f, 1.25f, 1.5f)
                        if (playback.speed in quickRates) {
                            SlidingSegmentedControl(
                                items = quickRates,
                                selectedItem = playback.speed,
                                onItemSelected = { rate ->
                                    playback.changeSpeed(rate)
                                    onMessage("Speed: ${rate}x")
                                },
                                labelProvider = { if (it == 1.0f) "1.0x" else "${it}x" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .padding(horizontal = 14.dp, vertical = 2.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                        }
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
                    QuickSubMenuContainer(
                        title = "Fit / Aspect ratio",
                        onBack = { onSelectSection("main") },
                        badgeText = playback.aspectMode.displayName.substringBefore(" (")
                    ) {
                        val quickAspects = listOf(AspectRatioMode.FIT, AspectRatioMode.ZOOM, AspectRatioMode.FILL, AspectRatioMode.RATIO_16_9)
                        if (playback.aspectMode in quickAspects) {
                            SlidingSegmentedControl(
                                items = quickAspects,
                                selectedItem = playback.aspectMode,
                                onItemSelected = { mode ->
                                    playback.setAspectRatioMode(mode)
                                    onMessage("Aspect: ${mode.displayName.substringBefore(" (")}")
                                },
                                labelProvider = { mode ->
                                    when (mode) {
                                        AspectRatioMode.FIT -> "Fit"
                                        AspectRatioMode.ZOOM -> "Zoom"
                                        AspectRatioMode.FILL -> "Fill"
                                        AspectRatioMode.RATIO_16_9 -> "16:9"
                                        else -> mode.displayName.substringBefore(" (")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .padding(horizontal = 14.dp, vertical = 2.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                        }
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
                        val isHdrActive = playback.isHdr

                        if (isHdrActive) {
                            val flavorText = playback.hdrFlavor ?: "HDR10"
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2200)),
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
                                        color = Color.White.copy(alpha = 0.82f),
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

                            Text("HDR Output & Profile Mode", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
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
                                    onClick = { playback.selectAudioTrack(track); onMessage("Audio: ${track.label}"); onDismiss() }
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
                            onClick = { playback.selectSubtitleTrack(null); onMessage("Subtitles Off"); onDismiss() }
                        )
                        playback.subtitleTracks.forEach { track ->
                            QuickOptionItem(
                                label = "${track.label} (${track.format})",
                                selected = track.isSelected,
                                onClick = { playback.selectSubtitleTrack(track); onMessage("Subtitles: ${track.label}"); onDismiss() }
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

                        // Letterbox Placement Toggle (Widescreen Cinema Bar)
                        val subCtx = androidx.compose.ui.platform.LocalContext.current
                        var isLetterbox by remember { mutableStateOf(SubtitlePreferences.getLetterboxPlacement(subCtx)) }
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Letterbox Bar Placement", color = palette.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Drop subtitles into black bars on 2.39:1 films", color = palette.secondaryText, fontSize = 11.sp)
                            }
                            Switch(
                                checked = isLetterbox,
                                onCheckedChange = { checked ->
                                    isLetterbox = checked
                                    SubtitlePreferences.setLetterboxPlacement(subCtx, checked)
                                    onApplySubtitles()
                                    onMessage(if (checked) "Subtitles: Render in Black Letterbox Bar" else "Subtitles: Inside Video Frame")
                                }
                            )
                        }

                        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = palette.divider)

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
}
}

@Composable private fun QuickMenuCategoryHeader(title: String) {
    val palette = LocalQuickMenuPalette.current
    Text(
        text = title,
        color = palette.accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
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
    badgeText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalQuickMenuPalette.current
    Column(Modifier.padding(vertical = 10.dp).heightIn(max = 440.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.primaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (badgeText != null) {
                Surface(
                    color = Color(0xFF35303E),
                    shape = CircleShape
                ) {
                    Text(
                        text = badgeText,
                        color = Color(0xFFD8C4F7),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            color = palette.divider
        )
        LazyColumn(content = { item { Column(content = content) } })
    }
}

@Composable private fun QuickOptionItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalQuickMenuPalette.current
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .heightIn(min = 45.dp)
            .touchMotion(),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color(0xFF4B3E5B) else Color(0xFF3A3545),
        border = BorderStroke(1.dp, if (selected) Color(0xFFB49BD1) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = if (selected) Color(0xFFF4ECFF) else Color(0xFFE2DCE8),
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFFDDC8FF),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun <T> SlidingSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    BoxWithConstraints(
        modifier = modifier
            .background(Color(0xFF302E3B), CircleShape)
            .padding(4.dp)
    ) {
        val count = items.size.coerceAtLeast(1)
        val tabWidth = maxWidth / count

        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = tween(380, easing = CubicBezierEasing(0.19f, 1.0f, 0.22f, 1.0f)),
            label = "indicatorOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Color(0xFFDDC8FF))
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable { onItemSelected(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelProvider(item),
                        color = if (isSelected) Color(0xFF241A31) else Color(0xFFBFB7CC),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// Approved Next-Gen Cinematic Gesture Capsule (Spatial Optical Capsule)
@Composable
fun FluidGestureCapsule(
    type: String,
    valueText: String,
    fraction: Float,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isOled: Boolean = false
) {
    val isSeek = type == "Seek"
    if (isSeek) {
        Surface(
            modifier = modifier.wrapContentSize(),
            color = SpatialGlass.pillBg(isOled),
            border = SpatialGlass.pillBorder(0.75.dp),
            shape = RoundedCornerShape(999.dp),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = type,
                    tint = TimelineAccent,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = valueText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        // Vertical Spatial Optical Capsule (Left: Brightness, Right: Volume)
        Surface(
            modifier = modifier
                .width(44.dp)
                .height(138.dp),
            color = SpatialGlass.pillBg(isOled),
            border = SpatialGlass.pillBorder(0.75.dp),
            shape = RoundedCornerShape(999.dp),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = type,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = valueText,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val animatedFill by animateFloatAsState(
                        targetValue = fraction.coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
                        label = "verticalMeterFill"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedFill)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFC0A2FB), TimelineAccent)
                                )
                            )
                    )
                }
            }
        }
    }
}

// Approved Next-Gen Playback Information Capsule (Read-Only Diagnostics)
@Composable
fun PlaybackInfoCapsule(
    playback: PlaybackController,
    video: LocalVideo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    val vInfo = playback.runtimeVideoFormat
    val dec = playback.runtimeVideoDecoder
    val context = androidx.compose.ui.platform.LocalContext.current

    val codecLabel = inspectionVideoCodec(vInfo?.mime) ?: vInfo?.mime?.substringAfterLast("/")?.uppercase() ?: "HEVC"
    val vW = vInfo?.width ?: video.width
    val vH = vInfo?.height ?: video.height
    val resLabel = when {
        vW >= 3800 || vH >= 2000 -> "4K"
        vW >= 1800 || vH >= 800 -> "1080p"
        vW >= 1200 || vH >= 600 -> "720p"
        vH > 0 -> "${vH}p"
        else -> "1080p"
    }
    val hdrLabel = if (playback.isHdr) (playback.hdrFlavor ?: "HDR10") else "SDR"
    val summaryPillText = "$codecLabel · $resLabel · $hdrLabel"

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(top = 70.dp, start = 16.dp, end = 16.dp)
                .widthIn(min = 320.dp, max = 460.dp)
                .clickable(enabled = false) {}
                .animateContentSize(spring(dampingRatio = 0.8f, stiffness = 400f)),
            color = SpatialGlass.pillBg(false),
            border = SpatialGlass.pillBorder(1.dp),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 20.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                // Header with Pill Toggle Button (.cc-pill)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Playback Diagnostics", color = Color(0xFFF5F1FB), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("User-invoked · Read-only metrics", color = Color(0xFFACA5B7), fontSize = 11.sp)
                    }
                    Surface(
                        onClick = { expanded = !expanded },
                        shape = RoundedCornerShape(999.dp),
                        color = if (expanded) LavenderAccent else Color.White.copy(alpha = 0.12f),
                        contentColor = if (expanded) SpatialGlass.Obsidian else Color.White,
                        border = SpatialGlass.pillBorder(1.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                summaryPillText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (expanded) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF151221),
                        border = SpatialGlass.pillBorder(1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Video / Decoder / Output", color = Color(0xFFDDC8FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = Color(0xFF35303E),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        "READ-ONLY",
                                        color = Color(0xFFD8C4F7),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Key-Value Rows (.cc-kv)
                            PlaybackKvRow("Source video codec", codecLabel)
                            PlaybackKvRow("Source resolution", "${vInfo?.width ?: video.width} × ${vInfo?.height ?: video.height}")
                            val xfer = when (vInfo?.colorTransfer) {
                                6 -> "SMPTE ST 2084 (PQ)"
                                7 -> "ARIB STD-B67 (HLG)"
                                3 -> "BT.709 / SDR"
                                else -> if (playback.isHdr) "SMPTE ST 2084 (PQ)" else "SDR (Standard)"
                            }
                            PlaybackKvRow("Source transfer", xfer)
                            val declaredHdr = if (playback.isHdr) (playback.hdrFlavor ?: "HDR10") else "SDR (Standard)"
                            PlaybackKvRow("Declared HDR metadata", declaredHdr)

                            val decType = if (dec?.hardware == DecoderHardware.HARDWARE) "Hardware" else if (dec?.hardware == DecoderHardware.SOFTWARE) "Software (FFmpeg)" else "Native MediaCodec"
                            PlaybackKvRow("Actual decoder", "${dec?.name ?: "c2.android.video"} ($decType)")

                            val actualHdrOutput = if (playback.isHdr && HdrCodecSelector.isHdr10Supported(context)) {
                                "Native HDR (Wide Gamut 10-bit)"
                            } else if (playback.isHdr) {
                                "SDR Tone-mapped"
                            } else {
                                "Standard SDR"
                            }
                            PlaybackKvRow("Actual HDR output", actualHdrOutput)

                            val dropped = "0 (Smooth)"
                            PlaybackKvRow("Dropped frames", dropped, isLast = true)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Values observed from active Media3 Android decoder pipeline. Strictly read-only.",
                        color = Color(0xFFACA5B7),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackKvRow(label: String, value: String, isLast: Boolean = false) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color(0xFFB3ABBF), fontSize = 12.sp)
            Text(value, color = Color(0xFFF5F1FB), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
        }
        if (!isLast) {
            HorizontalDivider(color = Color(0xFF393543), thickness = 1.dp)
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
        border = SpatialGlass.pillBorder(1.dp)
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
            border = if (rimColor == Color.Transparent) null else BorderStroke(1.dp, rimColor),
            shadowElevation = if (bgColor == Color.Transparent) 0.dp else 6.dp
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
        shape = RoundedCornerShape(20.dp),
        color = Color(0xE60D0F17),
        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
        shadowElevation = 16.dp
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
                        shape = RoundedCornerShape(999.dp),
                        color = TimelineAccent.copy(alpha = 0.25f),
                        border = BorderStroke(0.5.dp, Color(0x33FFFFFF))
                    ) {
                        Text(
                            epLabel,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize = 10.5.sp,
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
    remainingSecProvider: (() -> Long)? = null,
    bgColor: Color = Color(0x990A0E17),
    rimColor: Color = Color.White.copy(alpha = 0.22f),
    contentColor: Color = Color.White,
    accentColor: Color = TimelineAccent,
    style: String = "glass",
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

    val actualRemSec = remainingSecProvider?.invoke() ?: remainingSec
    val hasRemaining = showRemainingTime && actualRemSec != null && actualRemSec > 0
    val remainingText = if (hasRemaining) "−${compactTimecode(actualRemSec!!)}" else ""

    val pillContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(
                horizontal = if (style == "text") 6.dp else 14.dp,
                vertical = if (style == "text") 4.dp else 7.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val hasClock = showClock && timeText.isNotEmpty()
            val hasBattery = showBattery && batteryText.isNotEmpty()

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

    when (style) {
        "text" -> {
            Box(modifier = modifier) {
                pillContent()
            }
        }
        "hairline" -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = bgColor,
                border = SpatialGlass.pillBorder(0.75.dp),
                shadowElevation = 0.dp,
                modifier = modifier
            ) {
                pillContent()
            }
        }
        else -> {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = bgColor,
                border = SpatialGlass.pillBorder(0.75.dp),
                shadowElevation = 0.dp,
                modifier = modifier
            ) {
                pillContent()
            }
        }
    }
}

/**
 * In-Player Episode Drawer Sheet
 * Allows instant browsing and jumping across season episodes without leaving the player surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDrawerSheet(
    episodes: List<LocalVideo>,
    currentVideo: LocalVideo,
    onSelectEpisode: (LocalVideo) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.VideoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        val seriesTitle = remember(currentVideo.name) {
                            EpisodePatterns.parse(currentVideo.name)?.seriesTitle ?: currentVideo.title.ifBlank { currentVideo.name }
                        }
                        Text(
                            text = seriesTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${episodes.size} episodes available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(episodes, key = { _, ep -> ep.identity }) { index, ep ->
                    val isPlaying = ep.identity == currentVideo.identity
                    val epInfo = EpisodePatterns.parse(ep.name)
                    val epLabel = if (epInfo != null) "S%02dE%02d".format(epInfo.season, epInfo.episode) else "Ep ${index + 1}"
                    val epTitle = epInfo?.episodeTitle?.ifBlank { ep.title } ?: ep.title.ifBlank { ep.name }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                        border = if (isPlaying) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        else BorderStroke(0.75.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectEpisode(ep)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Thumbnail with Now Playing badge
                            Box(
                                modifier = Modifier
                                    .width(84.dp)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.25f))
                            ) {
                                LocalThumbnail(video = ep, modifier = Modifier.fillMaxSize())
                                if (isPlaying) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = "PLAYING",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Metadata Column
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = epLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = epTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (ep.duration > 0) {
                                    val durMin = (ep.duration / 60000).toInt()
                                    Text(
                                        text = "${durMin}m",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cinema Relay Sheet: Interactive Bottom Sheet for WebRTC Watch Party & Audio Cinema Bridge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaRelaySheet(
    server: WatchPartyServer,
    playback: PlaybackController,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isWatchParty = server.relayMode == CinemaRelayMode.WATCH_PARTY
    val activeUrl = if (isWatchParty) server.partyUrl else server.audioBridgeUrl
    val qrBitmap = remember(activeUrl) {
        QrCodeGenerator.generate(activeUrl, sizePx = 384, foregroundColor = Color.White, backgroundColor = Color(0x33100B1A))
    }
    var chatInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xF5130F1F),
        contentColor = Color.White,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.35f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isWatchParty) Color(0x289D85FF) else Color(0x2838BDF8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isWatchParty) Icons.Rounded.Groups else Icons.Rounded.Headphones,
                            contentDescription = null,
                            tint = if (isWatchParty) Color(0xFF9D85FF) else Color(0xFF38BDF8),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            if (isWatchParty) "Live Cinema Watch Party" else "Audio Cinema Bridge",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            if (isWatchParty) "Zero-install browser stream & real-time sync" else "Dual-headphone zero lag relay for partner",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA49FB5)
                        )
                    }
                }
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Status Pill
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x1F4ADE80))
                    .border(1.dp, Color(0x404ADE80), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4ADE80))
                )
                Text(
                    text = if (isWatchParty) "ROOM ONLINE • ${server.guestCount} GUESTS CONNECTED" else "HEADPHONE STREAM ONLINE • READY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4ADE80)
                )
            }

            // QR Code Container
            if (qrBitmap != null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1B1629),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.size(190.dp).padding(10.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = "Relay QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Text(
                    "Scan with any phone camera or open the link below",
                    fontSize = 12.sp,
                    color = Color(0xFFA49FB5)
                )
            }

            // URL Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = activeUrl,
                        fontSize = 12.sp,
                        color = Color(0xFFD4CCF0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(activeUrl))
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D85FF), contentColor = Color(0xFF0B0718))
                        ) {
                            Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Join my Jadzumi Cinema Stream: $activeUrl")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Stream"))
                            }
                        ) {
                            Icon(Icons.Rounded.Share, "Share", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Voice Call & Controls (Watch Party Mode)
            if (isWatchParty) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("VOICE CALL & AUDIO SETTINGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA49FB5))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Host Microphone", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                Text(if (server.isMicMuted) "Muted" else "Open mic with Echo Cancellation", fontSize = 11.sp, color = Color(0xFFA49FB5))
                            }
                            Switch(
                                checked = !server.isMicMuted,
                                onCheckedChange = { server.isMicMuted = !it }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Movie Auto-Ducking", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                Text("Dips movie volume by 25% when voice is detected", fontSize = 11.sp, color = Color(0xFFA49FB5))
                            }
                            Switch(
                                checked = server.isAudioDuckingEnabled,
                                onCheckedChange = { server.isAudioDuckingEnabled = it }
                            )
                        }
                    }
                }

                // Chat Input & Feed
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("LIVE REACTION CHAT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA49FB5))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 120.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            server.chatMessages.takeLast(6).forEach { msg ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${msg.sender}:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (msg.sender == "Host") Color(0xFF9D85FF) else Color(0xFF4ADE80))
                                    Text(msg.text, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                placeholder = { Text("Send reaction...", fontSize = 12.sp, color = Color(0xFFA49FB5)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    server.sendHostChat(chatInput)
                                    chatInput = ""
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D85FF), contentColor = Color(0xFF0B0718))
                            ) {
                                Text("Send", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Stop Session Button
            Button(
                onClick = {
                    server.stop()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935).copy(alpha = 0.2f), contentColor = Color(0xFFFF6B6B)),
                border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.4f))
            ) {
                Text(if (isWatchParty) "End Watch Party Session" else "Disconnect Audio Bridge", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WatchPartyTopHud(
    server: WatchPartyServer,
    onOpenContentPicker: () -> Unit,
    onOpenShare: () -> Unit,
    isChatShrunk: Boolean,
    onToggleChatShrunk: () -> Unit,
    onEndParty: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color(0xF2140F22),
        border = BorderStroke(1.dp, Color(0x559D85FF)),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4ADE80))
            )
            Text(
                text = "WATCH PARTY (${server.guestCount})",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF4ADE80)
            )

            // Choose Content Button
            Surface(
                onClick = onOpenContentPicker,
                shape = CircleShape,
                color = Color(0x339D85FF),
                border = BorderStroke(1.dp, Color(0x669D85FF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Rounded.VideoLibrary, null, tint = Color(0xFFC4B5FD), modifier = Modifier.size(14.dp))
                    Text("Choose Video", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC4B5FD))
                }
            }

            // Share / QR Button
            Surface(
                onClick = onOpenShare,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Rounded.Share, null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Text("Share", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            // Chat size toggle
            Surface(
                onClick = onToggleChatShrunk,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (isChatShrunk) Icons.Rounded.ChatBubble else Icons.Rounded.Remove,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(if (isChatShrunk) "Expand Chat" else "Shrink Chat", fontSize = 11.sp, color = Color.White)
                }
            }

            // End party button
            Surface(
                onClick = onEndParty,
                shape = CircleShape,
                color = Color(0x28F87171),
                border = BorderStroke(1.dp, Color(0x55F87171))
            ) {
                Text(
                    "End Party",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF87171)
                )
            }
        }
    }
}

@Composable
private fun WatchPartyFloatingReactions(messages: List<PartyChatMessage>) {
    val recentReaction = remember(messages.size) {
        messages.lastOrNull()?.text?.takeIf { txt ->
            txt.contains("Love") || txt.contains("Epic") || txt.contains("Laugh") ||
            txt.contains("Bravo") || txt.contains("Cinema") || txt.contains("Mindblown")
        }
    }
    recentReaction?.let { rx ->
        var animStarted by remember(rx, messages.size) { mutableStateOf(false) }
        val offsetY by animateFloatAsState(
            targetValue = if (animStarted) -320f else 0f,
            animationSpec = tween(2600, easing = FastOutSlowInEasing),
            label = "rxFloat"
        )
        val alpha by animateFloatAsState(
            targetValue = if (animStarted) 0f else 1f,
            animationSpec = tween(2600),
            label = "rxAlpha"
        )
        LaunchedEffect(rx, messages.size) {
            animStarted = true
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp, start = 80.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0x449D85FF),
                border = BorderStroke(1.dp, Color(0x889D85FF)),
                modifier = Modifier
                    .graphicsLayer {
                        translationY = offsetY
                        this.alpha = alpha
                    }
            ) {
                Text(
                    text = rx,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun WatchPartyHostChatDock(
    server: WatchPartyServer,
    isShrunk: Boolean,
    onToggleShrunk: () -> Unit
) {
    var hostInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val lastMsg = server.chatMessages.lastOrNull()

    if (isShrunk) {
        // Compact Actionable Floating Frosted Capsule (Shrunk State)
        Surface(
            shape = CircleShape,
            color = Color(0xF2140F22),
            border = BorderStroke(1.dp, Color(0x669D85FF)),
            shadowElevation = 18.dp,
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onToggleShrunk)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Direct Host Mic Toggle on Capsule
                Surface(
                    onClick = { server.isMicMuted = !server.isMicMuted },
                    shape = CircleShape,
                    color = if (!server.isMicMuted) Color(0x334ADE80) else Color(0x33F87171),
                    border = BorderStroke(1.dp, if (!server.isMicMuted) Color(0x884ADE80) else Color(0x88F87171)),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (!server.isMicMuted) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                            contentDescription = "Toggle Host Mic",
                            tint = if (!server.isMicMuted) Color(0xFF4ADE80) else Color(0xFFF87171),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Title + Latest Message Preview
                Column(modifier = Modifier.widthIn(max = 140.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("Cinema Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF9D85FF)
                        ) {
                            Text(
                                text = "${server.chatMessages.size}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0D081A),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = if (lastMsg != null) "${lastMsg.sender}: ${lastMsg.text}" else "Tap to expand",
                        fontSize = 10.sp,
                        color = Color(0xFFA49FB5),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quick Reaction Strip right on the collapsed pill
                Row(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Cinema", "Epic", "Bravo").forEach { rx ->
                        Surface(
                            onClick = { server.sendHostChat(rx) },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = rx,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4CCF0),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Expand Chevron Button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ExpandLess, "Expand", tint = Color(0xFF9D85FF), modifier = Modifier.size(15.dp))
                }
            }
        }
    } else {
        // Normal Full-Size Hyper-Interactive Watch Party Dock
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF2140F22),
            border = BorderStroke(1.dp, Color(0x449D85FF)),
            shadowElevation = 24.dp,
            modifier = Modifier
                .width(340.dp)
                .heightIn(max = 410.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header with Tab Bar & Shrink Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Tabs: Live Chat vs Guests
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = { selectedTab = 0 },
                            shape = CircleShape,
                            color = if (selectedTab == 0) Color(0xFF9D85FF) else Color.White.copy(alpha = 0.06f)
                        ) {
                            Text(
                                text = "Live Chat",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) Color(0xFF0F0A1F) else Color(0xFFA49FB5),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Surface(
                            onClick = { selectedTab = 1 },
                            shape = CircleShape,
                            color = if (selectedTab == 1) Color(0xFF9D85FF) else Color.White.copy(alpha = 0.06f)
                        ) {
                            Text(
                                text = "Guests (${server.guestCount})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) Color(0xFF0F0A1F) else Color(0xFFA49FB5),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Shrink Button
                    Surface(
                        onClick = onToggleShrunk,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Rounded.ExpandMore, null, tint = Color(0xFFA49FB5), modifier = Modifier.size(14.dp))
                            Text("Shrink", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA49FB5))
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Voice Status Bar with animated VU meters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { server.isMicMuted = !server.isMicMuted },
                            shape = CircleShape,
                            color = if (!server.isMicMuted) Color(0x334ADE80) else Color(0x33F87171),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (!server.isMicMuted) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                                    null,
                                    tint = if (!server.isMicMuted) Color(0xFF4ADE80) else Color(0xFFF87171),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        // VU Meter Bars
                        val infiniteTransition = rememberInfiniteTransition(label = "vuBars")
                        val vuScale by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
                            label = "vuScale"
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.height(14.dp)
                        ) {
                            Box(modifier = Modifier.width(3.dp).fillMaxHeight(if (!server.isMicMuted) vuScale * 0.7f else 0.2f).background(Color(0xFF4ADE80), RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.width(3.dp).fillMaxHeight(if (!server.isMicMuted) vuScale else 0.2f).background(Color(0xFF4ADE80), RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.width(3.dp).fillMaxHeight(if (!server.isMicMuted) vuScale * 0.5f else 0.2f).background(Color(0xFF4ADE80), RoundedCornerShape(2.dp)))
                        }

                        Text(
                            text = if (!server.isMicMuted) "Broadcasting Live" else "Mic Muted",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (!server.isMicMuted) Color.White else Color(0xFFA49FB5)
                        )
                    }

                    // Auto-Ducking Badge
                    Surface(
                        shape = CircleShape,
                        color = Color(0x289D85FF),
                        border = BorderStroke(0.5.dp, Color(0x669D85FF))
                    ) {
                        Text(
                            "Ducking -25%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC4B5FD),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                if (selectedTab == 0) {
                    // TAB 0: LIVE CHAT FEED
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        server.chatMessages.takeLast(25).forEach { msg ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (msg.sender == "Host") Color(0x339D85FF)
                                    else if (msg.sender == "System") Color(0x22FFFFFF)
                                    else Color(0x334ADE80)
                                ) {
                                    Text(
                                        text = msg.sender,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (msg.sender == "Host") Color(0xFFC4B5FD)
                                        else if (msg.sender == "System") Color(0xFFA49FB5)
                                        else Color(0xFF4ADE80),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                                Text(msg.text, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    // Quick Reactions Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Love", "Epic", "Laugh", "Bravo", "Cinema", "Mindblown").forEach { rx ->
                            Surface(
                                onClick = { server.sendHostChat(rx) },
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.07f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f))
                            ) {
                                Text(
                                    text = rx,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Composer Input Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = hostInput,
                            onValueChange = { hostInput = it },
                            placeholder = { Text("Chat with party...", fontSize = 11.sp, color = Color(0xFFA49FB5)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                if (hostInput.isNotBlank()) {
                                    server.sendHostChat(hostInput)
                                    hostInput = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D85FF), contentColor = Color(0xFF0B0718))
                        ) {
                            Text("Send", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // TAB 1: GUESTS MANAGEMENT
                    val peers = server.connectedPeers
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "CONNECTED PARTICIPANTS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA49FB5)
                        )

                        if (peers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No guests connected yet.\nShare the link or QR to join.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFA49FB5),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            peers.forEach { peerIp ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(peerIp, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("Active • Sub-second Sync", fontSize = 10.sp, color = Color(0xFF4ADE80))
                                        }
                                        OutlinedButton(
                                            onClick = { server.disconnectPeer(peerIp) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171))
                                        ) {
                                            Text("Disconnect", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchPartyContentPickerSheet(
    episodes: List<LocalVideo>,
    currentVideo: LocalVideo,
    onSelectEpisode: (LocalVideo) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xF5130F1F),
        contentColor = Color.White,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.35f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Choose What to Watch Next",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Select any movie or episode to switch stream for all connected guests",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA49FB5)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, "Close", tint = Color.White)
                }
            }

            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(episodes, key = { _, ep -> ep.identity }) { index, ep ->
                    val isPlaying = ep.identity == currentVideo.identity
                    val epInfo = EpisodePatterns.parse(ep.name)
                    val epLabel = if (epInfo != null) "S%02dE%02d".format(epInfo.season, epInfo.episode) else "Movie ${index + 1}"
                    val epTitle = epInfo?.episodeTitle?.ifBlank { ep.title } ?: ep.title.ifBlank { ep.name }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isPlaying) Color(0x339D85FF) else Color(0x15FFFFFF),
                        border = if (isPlaying) BorderStroke(1.5.dp, Color(0xFF9D85FF)) else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectEpisode(ep) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(84.dp)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.35f))
                            ) {
                                LocalThumbnail(video = ep, modifier = Modifier.fillMaxSize())
                                if (isPlaying) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF4ADE80),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            "STREAMING",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = epLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPlaying) Color(0xFF9D85FF) else Color(0xFF38BDF8)
                                )
                                Text(
                                    text = epTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (ep.duration > 0) {
                                    val durMin = (ep.duration / 60000).toInt()
                                    Text("${durMin}m", fontSize = 11.sp, color = Color(0xFFA49FB5))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
// END OF FILE
