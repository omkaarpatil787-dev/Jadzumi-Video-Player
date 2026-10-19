package com.jadzumi.video

import android.graphics.Rect
import android.media.AudioManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CinemaPlayer(playback: PlaybackController, reducedMotion: Boolean) {
    val video = playback.video ?: return
    val activity = LocalActivity.current as? MainActivity ?: return
    val previousBrightness = remember(activity) { activity.window.attributes.screenBrightness }
    var controls by remember(video.identity) { mutableStateOf(true) }
    var menu by remember(video.identity) { mutableStateOf(false) }
    var speedMenu by remember(video.identity) { mutableStateOf(false) }
    var tracksDrawer by remember(video.identity) { mutableStateOf(false) }
    var scrub by remember(video.identity) { mutableStateOf<Float?>(null) }
    var message by remember(video.identity) { mutableStateOf<String?>(null) }
    var interaction by remember(video.identity) { mutableIntStateOf(0) }
    val seekDeltaMs = playback.seekIncrementSeconds * 1000L

    DisposableEffect(activity) {
        onDispose { activity.videoBounds = null; activity.updatePip() }
    }
    val full = playback.fullscreen || playback.inPip
    BackHandler(full && !playback.inPip) {
        if (tracksDrawer) tracksDrawer = false
        else if (playback.locked) { controls = true; interaction++ }
        else playback.fullscreen = false
    }

    LaunchedEffect(controls, playback.playing, interaction, playback.locked, menu, speedMenu, tracksDrawer, scrub != null) {
        if (controls && playback.playing && !menu && !speedMenu && !tracksDrawer && scrub == null) {
            delay(3500)
            controls = false
        }
    }
    LaunchedEffect(message) { if (message != null) { delay(1500); message = null } }
    LaunchedEffect(full) { controls = true; interaction++ }
    LaunchedEffect(playback.inPip) { if (playback.inPip) { menu = false; speedMenu = false; tracksDrawer = false; scrub = null; controls = false } }

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

    Box(Modifier.fillMaxSize(), contentAlignment = if (full) Alignment.Center else Alignment.BottomEnd) {
        val frame = if (full) Modifier.fillMaxSize()
        else Modifier.safeDrawingPadding().padding(end = 24.dp, bottom = 100.dp).width(320.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(24.dp))

        Box(frame.background(Color.Black)) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        setKeepContentOnPlayerReset(true)
                    }
                },
                modifier = Modifier.fillMaxSize().onGloballyPositioned {
                    val bounds = it.boundsInWindow()
                    activity.videoBounds = Rect(bounds.left.roundToInt(), bounds.top.roundToInt(), bounds.right.roundToInt(), bounds.bottom.roundToInt())
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
                                onTap = { if (!full) playback.fullscreen = true else controls = !controls; interaction++ },
                                onDoubleTap = { offset ->
                                    if (!playback.locked && full) {
                                        val isLeft = offset.x < size.width / 2
                                        playback.skip(if (isLeft) -seekDeltaMs else seekDeltaMs)
                                        message = if (isLeft) "−${playback.seekIncrementSeconds} seconds" else "+${playback.seekIncrementSeconds} seconds"
                                        interaction++
                                    }
                                }
                            )
                        }
                        .pointerInput(video.identity, playback.locked, full, playback.swipeGesturesEnabled) {
                            var left = false; var level = 0f
                            detectVerticalDragGestures(onDragStart = { offset ->
                                left = offset.x < size.width / 2
                                level = if (left) activity.window.attributes.screenBrightness.takeIf { it >= 0 } ?: 0.5f
                                else audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                            }) { change, amount ->
                                if (!playback.locked && full && playback.swipeGesturesEnabled) {
                                    change.consume()
                                    level = (level - amount / size.height.coerceAtLeast(1)).coerceIn(if (left) 0.05f else 0f, 1f)
                                    if (left) activity.window.attributes = activity.window.attributes.apply { screenBrightness = level }
                                    else audio.setStreamVolume(AudioManager.STREAM_MUSIC, (level * audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(), 0)
                                    message = "${if (left) "Brightness" else "Volume"} ${(level * 100).toInt()}%"
                                    interaction++
                                }
                            }
                        }
                )

                if (playback.buffering) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)

                if (full) {
                    AnimatedVisibility(
                        visible = controls && !playback.locked,
                        enter = fadeIn(tween(if (reducedMotion) 0 else 160)),
                        exit = fadeOut(tween(if (reducedMotion) 0 else 160))
                    ) {
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent, Color.Black.copy(alpha = 0.65f)))).safeDrawingPadding().padding(20.dp)) {
                            Row(Modifier.align(Alignment.TopCenter).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                CinemaIcon("Back to library", { playback.fullscreen = false }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
                                Text(video.title, Modifier.weight(1f).padding(horizontal = 16.dp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)

                                TextButton(
                                    onClick = { tracksDrawer = true; interaction++ },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                                ) {
                                    Icon(Icons.Rounded.Subtitles, null, Modifier.size(20.dp), tint = Color(0xFF70B5FF))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Audio & Subs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Box {
                                    CinemaIcon("Playback options", { menu = true; interaction++ }) { Icon(Icons.Rounded.MoreVert, null) }
                                    DropdownMenu(menu, { menu = false }) {
                                        DropdownMenuItem(text = { Text("Fit to screen") }, onClick = { playback.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; menu = false })
                                        DropdownMenuItem(text = { Text("Crop to fill") }, onClick = { playback.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM; menu = false })
                                        DropdownMenuItem(text = { Text("Stretch") }, onClick = { playback.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL; menu = false })
                                        DropdownMenuItem(
                                            text = { Text(if (playback.backgroundAudioEnabled) "Background Audio: ON" else "Background Audio: OFF") },
                                            onClick = {
                                                playback.setBackgroundAudio(!playback.backgroundAudioEnabled)
                                                message = if (playback.backgroundAudioEnabled) "Background audio enabled" else "Background audio disabled"
                                                menu = false
                                            }
                                        )
                                        DropdownMenuItem(text = { Text("Restart video") }, onClick = { playback.seekTo(0); menu = false })
                                        DropdownMenuItem(text = { Text("Close video") }, onClick = { playback.close(); menu = false })
                                    }
                                }
                            }

                            Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                CinemaIcon("Back ${playback.seekIncrementSeconds} seconds", { playback.skip(-seekDeltaMs); interaction++ }, enabled = playback.seekable) { Icon(Icons.Rounded.Replay10, null, Modifier.size(32.dp)) }
                                FilledIconButton(
                                    onClick = { playback.toggle(); interaction++ },
                                    modifier = Modifier.size(72.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White.copy(alpha = 0.22f), contentColor = Color.White)
                                ) {
                                    PlayPauseMorph(playback.playRequested, reducedMotion, Modifier.size(40.dp))
                                }
                                CinemaIcon("Forward ${playback.seekIncrementSeconds} seconds", { playback.skip(seekDeltaMs); interaction++ }, enabled = playback.seekable) { Icon(Icons.Rounded.Forward10, null, Modifier.size(32.dp)) }
                            }

                            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                                scrub?.let { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ScrubPreview(video, (it * playback.duration).toLong()) } }
                                Slider(
                                    value = scrub ?: if (playback.duration > 0) (playback.position.toFloat() / playback.duration).coerceIn(0f, 1f) else 0f,
                                    onValueChange = { scrub = it; interaction++ },
                                    onValueChangeFinished = { scrub?.let { playback.seekTo((it * playback.duration).toLong()) }; scrub = null },
                                    enabled = playback.seekable && playback.duration > 0,
                                    track = { state -> SliderDefaults.Track(sliderState = state, modifier = Modifier.height(3.dp)) },
                                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFFB5D5FF), inactiveTrackColor = Color.White.copy(alpha = 0.25f))
                                )
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${timecode((scrub?.let { (it * playback.duration).toLong() } ?: playback.position) / 1000)} / ${if (playback.duration > 0) timecode(playback.duration / 1000) else "—"}", color = Color.White, style = MaterialTheme.typography.labelLarge)
                                    Spacer(Modifier.weight(1f))
                                    Box {
                                        TextButton(onClick = { speedMenu = true; interaction++ }) { Text("${playback.speed}×", color = Color.White) }
                                        DropdownMenu(speedMenu, { speedMenu = false }) { listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { rate -> DropdownMenuItem(text = { Text("${rate}×") }, onClick = { playback.changeSpeed(rate); speedMenu = false }) } }
                                    }
                                    CinemaIcon("Lock controls", { playback.locked = true; controls = false }) { Icon(Icons.Rounded.LockOpen, null) }
                                    if (activity.supportsPip()) CinemaIcon("Picture in picture", { if (!activity.enterCinemaPip()) message = "Picture in picture is unavailable right now" }) { Icon(Icons.Rounded.PictureInPictureAlt, null) }
                                    CinemaIcon("Mini-player", { playback.fullscreen = false }) { Icon(Icons.Rounded.FullscreenExit, null) }
                                }
                            }
                        }
                    }
                    if (playback.locked && controls) CinemaIcon("Unlock controls", { playback.locked = false; controls = true; interaction++ }, Modifier.align(Alignment.CenterStart).safeDrawingPadding().padding(24.dp)) { Icon(Icons.Rounded.Lock, null) }
                } else {
                    Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)), verticalAlignment = Alignment.CenterVertically) {
                        Text(video.title, Modifier.weight(1f).padding(start=12.dp), color=Color.White, maxLines=1, overflow=TextOverflow.Ellipsis, style=MaterialTheme.typography.labelMedium)
                        CinemaIcon(if (playback.playRequested) "Pause" else "Play", { playback.toggle() }) { Icon(if (playback.playRequested) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null) }
                        CinemaIcon("Close mini-player", playback::close) { Icon(Icons.Rounded.Close, null) }
                    }
                }

                if (tracksDrawer) {
                    AudioSubsDrawer(
                        playback = playback,
                        onDismiss = { tracksDrawer = false }
                    )
                }

                message?.let { Surface(Modifier.align(Alignment.TopCenter).padding(top = 80.dp), shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = 0.7f), contentColor = Color.White) { Text(it, Modifier.padding(16.dp)) } }
                playback.error?.let { error ->
                    Surface(Modifier.align(Alignment.Center).padding(24.dp).widthIn(max = 520.dp), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.padding(24.dp)) { Text(if (full) error else "Playback unavailable"); Row { TextButton(onClick = playback::retry) { Text("Retry") }; TextButton(onClick = playback::close) { Text("Close") } } }
                    }
                }
            }
        }
    }
}

@Composable private fun AudioSubsDrawer(playback: PlaybackController, onDismiss: () -> Unit) {
    var tab by remember { mutableStateOf("audio") }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(onClick = onDismiss)) {
        Surface(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().widthIn(max = 420.dp).clickable(enabled = false) {},
            color = Color(0xFF0F172A).copy(alpha = 0.95f),
            shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
        ) {
            Column(Modifier.padding(24.dp).fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Audio & Subtitles", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Close", tint = Color.White) }
                }

                TabRow(
                    selectedTabIndex = if (tab == "audio") 0 else 1,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF70B5FF),
                    divider = {}
                ) {
                    Tab(selected = tab == "audio", onClick = { tab = "audio" }, text = { Text("Audio (${playback.audioTracks.size})") })
                    Tab(selected = tab == "subs", onClick = { tab = "subs" }, text = { Text("Subtitles (${playback.subtitleTracks.size})") })
                }

                Spacer(Modifier.height(16.dp))

                if (tab == "audio") {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(playback.audioTracks, key = { it.id }) { track ->
                            Card(
                                onClick = { playback.selectAudioTrack(track) },
                                colors = CardDefaults.cardColors(containerColor = if (track.isSelected) Color(0xFF1A73E8).copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(
                                        if (track.isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Audiotrack,
                                        null,
                                        tint = if (track.isSelected) Color(0xFF70B5FF) else Color.White.copy(alpha = 0.6f)
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(track.label, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text("${track.codec} • ${track.channels}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.35f)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Surround Downmix Algorithm", color = Color(0xFF70B5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Stereo downmix optimized for Xiaomi Pad 5 quad Dolby Atmos tuned speakers.", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            val noneSelected = playback.subtitleTracks.none { it.isSelected }
                            Card(
                                onClick = { playback.selectSubtitleTrack(null) },
                                colors = CardDefaults.cardColors(containerColor = if (noneSelected) Color(0xFF1A73E8).copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(if (noneSelected) Icons.Rounded.CheckCircle else Icons.Rounded.SubtitlesOff, null, tint = if (noneSelected) Color(0xFF70B5FF) else Color.White.copy(alpha = 0.6f))
                                    Text("Turn Off Subtitles", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        items(playback.subtitleTracks, key = { it.id }) { track ->
                            Card(
                                onClick = { playback.selectSubtitleTrack(track) },
                                colors = CardDefaults.cardColors(containerColor = if (track.isSelected) Color(0xFF1A73E8).copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(if (track.isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Subtitles, null, tint = if (track.isSelected) Color(0xFF70B5FF) else Color.White.copy(alp
