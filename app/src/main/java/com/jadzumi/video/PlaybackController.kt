package com.jadzumi.video

import android.app.Application
import android.os.Bundle
import androidx.compose.runtime.*
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ContentDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.ui.AspectRatioFrameLayout
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Locale

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class PlaybackController(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("playback", 0)
    var player by mutableStateOf<ExoPlayer?>(null); private set
    private var session: MediaSession? = null
    var video by mutableStateOf<LocalVideo?>(null); private set
    var fullscreen by mutableStateOf(false)
    var inPip by mutableStateOf(false)
    var playing by mutableStateOf(false); private set
    var playRequested by mutableStateOf(false); private set
    var buffering by mutableStateOf(false); private set
    var position by mutableLongStateOf(0); private set
    var duration by mutableLongStateOf(0); private set
    var error by mutableStateOf<String?>(null); private set
    var speed by mutableFloatStateOf(1f); private set
    var resizeMode by mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT)
    var aspect by mutableFloatStateOf(16f / 9f); private set
    var locked by mutableStateOf(false)
    var seekable by mutableStateOf(false); private set

    // Milestone 3A.2 & 3A.3 State
    var audioTracks by mutableStateOf<List<AudioTrackInfo>>(emptyList()); private set
    var subtitleTracks by mutableStateOf<List<SubtitleTrackInfo>>(emptyList()); private set
    var isHdr by mutableStateOf(false); private set
    var videoCodecBadge by mutableStateOf<String?>(null); private set
    var backgroundAudioEnabled by mutableStateOf(prefs.getBoolean("bg_audio", false))

    private var completed by mutableStateOf(false)
    private var suspendedPosition = 0L
    private var mediaItemAssigned = false
    private var pollCount = 0

    init {
        viewModelScope.launch {
            while (true) {
                delay(500)
                player?.let { p ->
                    if (mediaItemAssigned) position = p.currentPosition.coerceAtLeast(0)
                    val currentDur = p.duration.takeIf { it > 0 } ?: video?.duration?.takeIf { it > 0 } ?: 0
                    if (currentDur > 0) duration = currentDur
                    if (++pollCount % 10 == 0) save()
                }
            }
        }
    }

    private fun key(v: LocalVideo): String = MessageDigest.getInstance("SHA-256")
        .digest(v.identity.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun persistedPosition(v: LocalVideo): Long =
        resumePosition(prefs.getLong(key(v), 0), v.duration, prefs.getBoolean(key(v) + ".done", false))

    fun savedPosition(v: LocalVideo): Long {
        if (video?.identity == v.identity) return resumePosition(position, duration.takeIf { it > 0 } ?: v.duration, completed)
        return persistedPosition(v)
    }

    fun setBackgroundAudio(enabled: Boolean) {
        backgroundAudioEnabled = enabled
        prefs.edit { putBoolean("bg_audio", enabled) }
    }

    fun open(v: LocalVideo) {
        if (video?.identity == v.identity && player != null) {
            fullscreen = true; locked = false
            if (error != null) retry() else {
                val p = player ?: return
                if (p.playbackState == Player.STATE_ENDED) { completed = false; p.seekTo(0) }
                if (p.playbackState == Player.STATE_IDLE) p.prepare()
                p.play()
            }
            return
        }
        release()
        video = v; fullscreen = true; locked = false; error = null; completed = false; aspect = 16f / 9f
        position = persistedPosition(v); suspendedPosition = position; duration = v.duration
        prepare(autoplay = true)
    }

    private fun prepare(autoplay: Boolean) {
        val v = video ?: return
        if (player != null) return
        mediaItemAssigned = false
        try {
            require(v.uri.toUri().scheme == "content")
            val app = getApplication<Application>()
            val source = DefaultMediaSourceFactory(DataSource.Factory { ContentDataSource(app) })

            val instantLoadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(1_500, 8_000, 100, 250)
                .setBackBuffer(10_000, true)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            val p = ExoPlayer.Builder(app, DefaultRenderersFactory(app).setEnableDecoderFallback(true))
                .setMediaSourceFactory(source)
                .setLoadControl(instantLoadControl)
                .build()
            player = p

            p.setSeekParameters(SeekParameters.CLOSEST_SYNC)

            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            p.setHandleAudioBecomingNoisy(true)

            p.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    if (this@PlaybackController.player !== player) return
                    playing = player.isPlaying
                    buffering = player.playbackState == Player.STATE_BUFFERING
                    playRequested = player.playWhenReady &&
                            player.playbackState != Player.STATE_ENDED &&
                            player.playbackState != Player.STATE_IDLE &&
                            player.playerError == null
                    speed = player.playbackParameters.speed
                    if (player.isPlaying) completed = false
                    seekable = player.isCurrentMediaItemSeekable

                    val size = player.videoSize
                    if (size.width > 0 && size.height > 0) {
                        val ratio = size.width * size.pixelWidthHeightRatio / size.height
                        if (ratio.isFinite() && ratio > 0f) aspect = ratio
                    }

                    if (events.contains(Player.EVENT_TRACKS_CHANGED)) {
                        updateTracks(player.currentTracks)
                    }

                    val format = player.videoFormat
                    if (format != null) {
                        isHdr = isHdrFormat(format.colorInfo?.colorTransfer ?: C.COLOR_TRANSFER_SDR)
                        videoCodecBadge = when {
                            format.sampleMimeType == MimeTypes.VIDEO_H265 -> if (isHdr) "4K HDR10" else "HEVC Main10"
                            format.sampleMimeType == MimeTypes.VIDEO_H264 -> "1080p AVC"
                            else -> if (isHdr) "HDR" else null
                        }
                    }

                    if (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING) error = null
                    if (player.playbackState == Player.STATE_ENDED) { completed = true; save() }
                    else if (events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) || events.contains(Player.EVENT_POSITION_DISCONTINUITY)) save()
                }

                override fun onPlayerError(exception: PlaybackException) {
                    if (player !== p) return
                    error = "Could not play this file (${exception.errorCodeName}). Verify folder access or check if this codec requires fallback."
                    playing = false; playRequested = false; buffering = false; save()
                }
            })

            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(v.title)
                .setDisplayTitle(v.title)
                .setArtist("Jadzumi Cinema Player")
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(v.uri)
                .setMediaId(v.identity)
                .setMediaMetadata(mediaMetadata)
                .build()

            p.setMediaItem(mediaItem)
            p.prepare()
            if (suspendedPosition > 0) {
                p.seekTo(suspendedPosition)
            }
            mediaItemAssigned = true
            p.setPlaybackSpeed(speed)

            // Setup System MediaSession with -10s / +10s interactive buttons
            val rewindCmd = SessionCommand("ACTION_REWIND_10", Bundle.EMPTY)
            val forwardCmd = SessionCommand("ACTION_FORWARD_10", Bundle.EMPTY)

            val sessionCallback = object : MediaSession.Callback {
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .add(rewindCmd)
                        .add(forwardCmd)
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .build()
                }

                override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
                    when (customCommand.customAction) {
                        "ACTION_REWIND_10" -> skip(-10000)
                        "ACTION_FORWARD_10" -> skip(10000)
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }

            val rewindButton = CommandButton.Builder()
                .setDisplayName("Back 10s")
                .setIconResId(android.R.drawable.ic_media_rew)
                .setSessionCommand(rewindCmd)
                .build()

            val forwardButton = CommandButton.Builder()
                .setDisplayName("Forward 10s")
                .setIconResId(android.R.drawable.ic_media_ff)
                .setSessionCommand(forwardCmd)
                .build()

            session = MediaSession.Builder(app, p)
                .setCallback(sessionCallback)
                .setCustomLayout(listOf(rewindButton, forwardButton))
                .build()

            p.playWhenReady = autoplay
        } catch (_: Exception) {
            error = "The player could not open this video. Try refreshing its folder and opening it again."
            release()
        }
    }

    private fun updateTracks(tracks: Tracks) {
        val audioList = mutableListOf<AudioTrackInfo>()
        val subList = mutableListOf<SubtitleTrackInfo>()

        for (g in 0 until tracks.groups.size) {
            val group = tracks.groups[g]
            for (t in 0 until group.length) {
                if (!group.isTrackSupported(t)) continue
                val f = group.getTrackFormat(t)
                val langTag = f.language ?: ""
                val langDisplay = if (langTag.isNotEmpty()) Locale.forLanguageTag(langTag).displayLanguage.ifEmpty { langTag } else "Unknown"

                when (group.type) {
                    C.TRACK_TYPE_AUDIO -> {
                        val name = f.label ?: if (langDisplay != "Unknown") langDisplay else "Track ${audioList.size + 1}"
                        audioList += AudioTrackInfo(
                            id = "$g:$t",
                            groupIndex = g,
                            trackIndex = t,
                            label = name,
                            language = langDisplay,
                            codec = formatAudioCodec(f.sampleMimeType),
                            channels = formatChannelLayout(f.channelCount),
                            isSelected = group.isTrackSelected(t)
                        )
                    }
                    C.TRACK_TYPE_TEXT -> {
                        val name = f.label ?: if (langDisplay != "Unknown") langDisplay else "Subtitle ${subList.size + 1}"
                        subList += SubtitleTrackInfo(
                            id = "$g:$t",
                            groupIndex = g,
                            trackIndex = t,
                            label = name,
                            language = langDisplay,
                            format = formatSubtitleFormat(f.sampleMimeType),
                            isSelected = group.isTrackSelected(t)
                        )
                    }
                }
            }
        }
        audioTracks = audioList
        subtitleTracks = subList
    }

    fun selectAudioTrack(track: AudioTrackInfo) {
        val p = player ?: return
        val group = p.currentTracks.groups.getOrNull(track.groupIndex) ?: return
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
            .build()
    }

    fun selectSubtitleTrack(track: SubtitleTrackInfo?) {
        val p = player ?: return
        if (track == null) {
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            val group = p.currentTracks.groups.getOrNull(track.groupIndex) ?: return
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
                .build()
        }
    }

    fun toggle() {
        if (error != null) { retry(); return }
        if (player == null) { prepare(true); return }
        val p = player ?: return
        if (p.playbackState == Player.STATE_IDLE) { p.prepare(); p.play() }
        else if (p.playbackState == Player.STATE_ENDED) { completed = false; p.seekTo(0); p.play() }
        else if (p.playWhenReady) p.pause() else p.play()
        save()
    }

    fun seekTo(value: Long) {
        if (seekable) {
            completed = false
            player?.seekTo(boundedPosition(value, duration))
            position = boundedPosition(value, duration)
            save()
        }
    }

    fun skip(delta: Long) = seekTo(seekPosition(player?.currentPosition ?: position, delta, duration))
    fun changeSpeed(value: Float) {
        if (value.isFinite()) {
            speed = value.coerceIn(0.25f, 3f)
            player?.setPlaybackSpeed(speed)
        }
    }

    fun retry() { save(); release(); error = null; prepare(true) }

    private fun save() {
        val v = video ?: return
        val pos = if (mediaItemAssigned) player?.currentPosition?.coerceAtLeast(0) ?: position else position
        position = pos; suspendedPosition = pos
        prefs.edit { putLong(key(v), pos); putBoolean(key(v) + ".done", completed) }
    }

    fun pauseForBackground() {
        if (!backgroundAudioEnabled) {
            save()
            release()
        }
    }

    fun restoreVisible() { if (video != null && player == null && error == null) prepare(false) }
    fun close() {
        release()
        video = null; fullscreen = false; locked = false; error = null
        position = 0; duration = 0; completed = false
        audioTracks = emptyList(); subtitleTracks = emptyList()
    }

    private fun release() {
        save()
        val oldPlayer = player
        player = null; mediaItemAssigned = false
        session?.release(); session = null
        oldPlayer?.release()
        playing = false; playRequested = false; buffering = false; seekable = false
    }

    override fun onCleared() { release(); super.onCleared() }
}
