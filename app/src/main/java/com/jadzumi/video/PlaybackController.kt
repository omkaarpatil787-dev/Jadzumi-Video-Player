package com.jadzumi.video

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.*
import androidx.core.app.NotificationCompat
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
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.ui.AspectRatioFrameLayout
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Locale
import android.net.Uri

data class ChapterMarker(
    val timeMs: Long,
    val title: String,
    val fraction: Float
)

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

    var audioTracks by mutableStateOf<List<AudioTrackInfo>>(emptyList()); private set
    var subtitleTracks by mutableStateOf<List<SubtitleTrackInfo>>(emptyList()); private set
    var isHdr by mutableStateOf(false); private set
    var videoCodecBadge by mutableStateOf<String?>(null); private set

    // Chapters & External Subtitles
    var chapters by mutableStateOf<List<ChapterMarker>>(emptyList()); private set
    var externalSubtitles by mutableStateOf<List<MediaItem.SubtitleConfiguration>>(emptyList()); private set

    // Milestone 5A: Queue, Next Episode Continuity & Autoplay
    var currentQueue by mutableStateOf<List<LocalVideo>>(emptyList()); private set
    var nextEpisodeCountdown by mutableStateOf<Int?>(null); private set
    var countdownDismissedForVideo by mutableStateOf<String?>(null); private set

    val nextEpisode: LocalVideo?
        get() {
            val current = video ?: return null
            if (currentQueue.isNotEmpty()) {
                val idx = currentQueue.indexOfFirst { it.identity == current.identity }
                if (idx != -1 && idx + 1 < currentQueue.size) {
                    return currentQueue[idx + 1]
                }
            }
            return null
        }

    // Milestone 4 Privacy & Playback Settings
    var backgroundAudioEnabled by mutableStateOf(prefs.getBoolean("bg_audio", false))
    var brightnessBoost by mutableStateOf(prefs.getBoolean("brightness_boost", false))
    var rememberProgress by mutableStateOf(prefs.getBoolean("remember_progress", true))
    var saveTrackPrefs by mutableStateOf(prefs.getBoolean("save_track_prefs", true))
    var swipeGesturesEnabled by mutableStateOf(prefs.getBoolean("swipe_gestures", true))
    var seekIncrementSeconds by mutableIntStateOf(prefs.getInt("seek_seconds", 10))
    var audioDelayMs by mutableLongStateOf(prefs.getLong("audio_delay", 0L))
    var subtitleDelayMs by mutableLongStateOf(prefs.getLong("sub_delay", 0L))
    var nightModeVoiceBoost by mutableStateOf(prefs.getBoolean("night_mode", false))
    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null
    var historyVersion by mutableIntStateOf(0); private set

    private val channelId = "playback_channel"
    private val notificationId = 1001
    private val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var completed by mutableStateOf(false)
    private var suspendedPosition = 0L
    private var mediaItemAssigned = false
    private var pollCount = 0

    init {
        createNotificationChannel()
        viewModelScope.launch {
            while (true) {
                delay(500)
                player?.let { p ->
                    if (mediaItemAssigned) position = p.currentPosition.coerceAtLeast(0)
                    val currentDur = p.duration.takeIf { it > 0 } ?: video?.duration?.takeIf { it > 0 } ?: 0
                    if (currentDur > 0) {
                        if (duration != currentDur) updateChapters(currentDur)
                        duration = currentDur
                    }
                    if (++pollCount % 10 == 0) save()

                    // Intelligent End-Credit Detection & Next-Episode Autoplay Countdown
                    val next = nextEpisode
                    if (next != null && countdownDismissedForVideo != video?.identity && currentDur > 30_000) {
                        val creditLeadMs = when {
                            currentDur >= 70 * 60_000L -> 110_000L // 70m+ movies: 110s credits
                            currentDur >= 35 * 60_000L -> 80_000L  // 45-60m drama: 80s credits
                            else -> 60_000L                        // 20-30m anime/sitcoms: 60s credits
                        }.coerceAtMost(currentDur / 3)

                        val creditStartPos = currentDur - creditLeadMs
                        val elapsedInCredits = position - creditStartPos
                        val countdownWindowMs = 12_000L

                        if (position >= creditStartPos && p.isPlaying) {
                            if (elapsedInCredits in 0..countdownWindowMs) {
                                val seconds = ((countdownWindowMs - elapsedInCredits + 999) / 1000).toInt().coerceIn(1, 12)
                                nextEpisodeCountdown = seconds
                            } else if (elapsedInCredits > countdownWindowMs && nextEpisodeCountdown != null) {
                                nextEpisodeCountdown = null
                                playNextEpisode()
                            }
                        } else {
                            if (nextEpisodeCountdown != null) nextEpisodeCountdown = null
                        }
                    } else {
                        if (nextEpisodeCountdown != null && p.playbackState != Player.STATE_ENDED) {
                            nextEpisodeCountdown = null
                        }
                    }
                }
            }
        }
    }

    private fun updateChapters(dur: Long) {
        if (dur <= 60_000L) {
            chapters = emptyList()
            return
        }
        val creditLeadMs = when {
            dur >= 70 * 60_000L -> 110_000L
            dur >= 35 * 60_000L -> 80_000L
            else -> 60_000L
        }.coerceAtMost(dur / 3)
        val creditPos = dur - creditLeadMs

        val list = mutableListOf<ChapterMarker>()
        val numIntervals = if (dur > 60 * 60_000L) 5 else 3
        for (i in 1..numIntervals) {
            val time = (dur * i) / (numIntervals + 1)
            if (time < creditPos - 20_000L) {
                list.add(ChapterMarker(time, "Chapter $i", time.toFloat() / dur.toFloat()))
            }
        }
        if (creditPos > 30_000L) {
            list.add(ChapterMarker(creditPos, "End Credits", creditPos.toFloat() / dur.toFloat()))
        }
        chapters = list
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Media Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows player controls on lock screen and notification shade"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val s = session ?: return
        val v = video ?: return
        val app = getApplication<Application>()

        val openIntent = PendingIntent.getActivity(
            app, 0,
            Intent(app, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(app, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(v.title)
            .setContentText("Jadzumi Cinema Player")
            .setContentIntent(openIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(playing)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(s))
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: SecurityException) { }
    }

    private fun cancelNotification() {
        notificationManager.cancel(notificationId)
    }

    private fun key(v: LocalVideo): String = MessageDigest.getInstance("SHA-256")
        .digest(v.identity.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun persistedPosition(v: LocalVideo): Long {
        if (!rememberProgress) return 0L
        return resumePosition(prefs.getLong(key(v), 0), v.duration, prefs.getBoolean(key(v) + ".done", false))
    }

    fun savedPosition(v: LocalVideo): Long {
        @Suppress("UNUSED_EXPRESSION")
        historyVersion
        if (!rememberProgress) return 0L
        if (video?.identity == v.identity) return resumePosition(position, duration.takeIf { it > 0 } ?: v.duration, completed)
        return persistedPosition(v)
    }

    fun setBackgroundAudio(enabled: Boolean) {
        backgroundAudioEnabled = enabled
        prefs.edit { putBoolean("bg_audio", enabled) }
    }

    fun updateRememberProgress(enabled: Boolean) {
        rememberProgress = enabled
        prefs.edit { putBoolean("remember_progress", enabled) }
    }

    fun updateSaveTrackPrefs(enabled: Boolean) {
        saveTrackPrefs = enabled
        prefs.edit { putBoolean("save_track_prefs", enabled) }
    }

    fun updateSwipeGestures(enabled: Boolean) {
        swipeGesturesEnabled = enabled
        prefs.edit { putBoolean("swipe_gestures", enabled) }
    }

    fun updateBrightnessBoost(enabled: Boolean) {
        brightnessBoost = enabled
        prefs.edit { putBoolean("brightness_boost", enabled) }
    }

    fun updateSeekSeconds(seconds: Int) {
        seekIncrementSeconds = seconds.coerceIn(5, 60)
        prefs.edit { putInt("seek_seconds", seekIncrementSeconds) }
    }

    fun updateNightMode(enabled: Boolean) {
        nightModeVoiceBoost = enabled
        prefs.edit { putBoolean("night_mode", enabled) }
        applyNightMode()
    }

    fun applyNightMode() {
        try {
            val sessionId = player?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
            if (nightModeVoiceBoost && sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0) {
                if (loudnessEnhancer == null) {
                    loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(sessionId)
                }
                loudnessEnhancer?.setTargetGain(600)
                loudnessEnhancer?.enabled = true
            } else {
                loudnessEnhancer?.enabled = false
                loudnessEnhancer?.release()
                loudnessEnhancer = null
            }
        } catch (_: Exception) {}
    }

    fun updateAudioDelay(deltaMs: Long) {
        audioDelayMs = (audioDelayMs + deltaMs).coerceIn(-500L, 500L)
        prefs.edit { putLong("audio_delay", audioDelayMs) }
    }

    fun resetAudioDelay() {
        audioDelayMs = 0L
        prefs.edit { putLong("audio_delay", 0L) }
    }

    fun updateSubtitleDelay(deltaMs: Long) {
        subtitleDelayMs = (subtitleDelayMs + deltaMs).coerceIn(-10_000L, 10_000L)
        prefs.edit { putLong("sub_delay", subtitleDelayMs) }
    }

    fun resetSubtitleDelay() {
        subtitleDelayMs = 0L
        prefs.edit { putLong("sub_delay", 0L) }
    }

    fun clearWatchHistory() {
        // Remove only hashed viewing records. Clearing history must preserve consent/settings.
        prefs.edit { prefs.all.keys.filter(::isWatchHistoryKey).forEach { remove(it) } }
        suspendedPosition = 0L
        historyVersion++
    }

    fun markEpisodeWatched(v: LocalVideo, watched: Boolean) {
        val k = key(v)
        val dur = v.duration.takeIf { it > 0 } ?: 1000L
        prefs.edit {
            if (watched) {
                putLong(k, dur)
                putBoolean("$k.done", true)
            } else {
                remove(k)
                remove("$k.done")
            }
        }
        historyVersion++
    }

    fun markSeasonWatched(videos: List<LocalVideo>, watched: Boolean) {
        prefs.edit {
            videos.forEach { v ->
                val k = key(v)
                val dur = v.duration.takeIf { it > 0 } ?: 1000L
                if (watched) {
                    putLong(k, dur)
                    putBoolean("$k.done", true)
                } else {
                    remove(k)
                    remove("$k.done")
                }
            }
        }
        historyVersion++
    }

    fun markVideosWatched(videos: List<LocalVideo>, watched: Boolean) {
        prefs.edit {
            videos.forEach { v ->
                val k = key(v)
                val dur = v.duration.takeIf { it > 0 } ?: 1000L
                if (watched) {
                    putLong(k, dur)
                    putBoolean("$k.done", true)
                } else {
                    remove(k)
                    remove("$k.done")
                }
            }
        }
        historyVersion++
    }

    fun clearProgress(videos: List<LocalVideo>) {
        prefs.edit {
            videos.forEach { v ->
                val k = key(v)
                remove(k)
                remove("$k.done")
            }
        }
        historyVersion++
    }

    fun isWatched(v: LocalVideo): Boolean {
        val pos = savedPosition(v)
        val dur = v.duration.takeIf { it > 0 } ?: 1000L
        return pos >= dur * 0.9f
    }

    fun isInProgress(v: LocalVideo): Boolean {
        val pos = savedPosition(v)
        val dur = v.duration.takeIf { it > 0 } ?: 1000L
        return pos in 1 until (dur * 0.9f).toLong()
    }

    fun addExternalSubtitle(uri: Uri, label: String = "External Subtitle") {
        val uriStr = uri.toString().lowercase(Locale.ROOT)
        val mimeType = when {
            uriStr.endsWith(".vtt") -> MimeTypes.TEXT_VTT
            uriStr.endsWith(".ass") || uriStr.endsWith(".ssa") -> MimeTypes.TEXT_SSA
            else -> MimeTypes.APPLICATION_SUBRIP
        }
        val config = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mimeType)
            .setLabel(label)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        externalSubtitles = externalSubtitles + config

        val p = player ?: return
        val v = video ?: return
        val curPos = p.currentPosition
        val isPlay = p.isPlaying
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(v.title)
            .setArtist("Jadzumi Cinema Player")
            .build()
        val mediaItem = MediaItem.Builder()
            .setUri(v.uri)
            .setMediaId(v.identity)
            .setMediaMetadata(mediaMetadata)
            .setSubtitleConfigurations(externalSubtitles)
            .build()
        p.setMediaItem(mediaItem)
        p.prepare()
        if (curPos > 0) p.seekTo(curPos)
        if (isPlay) p.play()
    }

    fun dismissNextEpisodeCountdown() {
        nextEpisodeCountdown = null
        countdownDismissedForVideo = video?.identity
    }

    fun playNextEpisode() {
        val next = nextEpisode ?: return
        nextEpisodeCountdown = null
        open(next, currentQueue)
    }

    fun open(v: LocalVideo, queue: List<LocalVideo> = emptyList()) {
        if (queue.isNotEmpty()) {
            currentQueue = queue
        } else {
            // Automatically discover sibling episodes if part of a TV series
            val app = getApplication<Application>() as? JadzumiApplication
            val allVideos = app?.library?.state?.value?.videos ?: emptyList()
            val info = EpisodePatterns.parse(v.name)
            if (info != null && allVideos.isNotEmpty()) {
                val seriesEpisodes = allVideos.mapNotNull { candidate ->
                    if (candidate.episode == null) return@mapNotNull null
                    val other = EpisodePatterns.parse(candidate.name)
                    if (other != null && other.seriesTitle.equals(info.seriesTitle, ignoreCase = true) && other.season == info.season) {
                        candidate to other.episode
                    } else null
                }.sortedBy { it.second }.map { it.first }
                currentQueue = if (seriesEpisodes.isNotEmpty()) seriesEpisodes else listOf(v)
            } else if (currentQueue.none { it.identity == v.identity }) {
                currentQueue = listOf(v)
            }
        }

        nextEpisodeCountdown = null
        countdownDismissedForVideo = null
        if (video?.identity != v.identity) {
            externalSubtitles = emptyList()
            chapters = emptyList()
        }

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
        isHdr = v.name.let {
            it.contains("hdr", ignoreCase = true) ||
            it.contains("10bit", ignoreCase = true) ||
            it.contains("dovi", ignoreCase = true) ||
            it.contains("dv", ignoreCase = true) ||
            it.contains("hlg", ignoreCase = true)
        }
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

            val renderersFactory = NextRenderersFactory(app)
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

            val p = ExoPlayer.Builder(app, renderersFactory)
                .setMediaSourceFactory(source)
                .setLoadControl(instantLoadControl)
                .build()
            player = p

            p.setSeekParameters(SeekParameters.CLOSEST_SYNC)
            p.setWakeMode(C.WAKE_MODE_LOCAL)

            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            p.setHandleAudioBecomingNoisy(true)

            p.addListener(object : Player.Listener {
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    if (nightModeVoiceBoost) applyNightMode()
                }

                override fun onTracksChanged(tracks: Tracks) {
                    if (this@PlaybackController.player !== p) return
                    updateTracks(tracks)
                }

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
                        val nameHdr = video?.name?.let {
                            it.contains("hdr", ignoreCase = true) ||
                            it.contains("10bit", ignoreCase = true) ||
                            it.contains("dovi", ignoreCase = true) ||
                            it.contains("dv", ignoreCase = true) ||
                            it.contains("hlg", ignoreCase = true)
                        } ?: false
                        isHdr = isHdrFormat(format.colorInfo?.colorTransfer ?: C.COLOR_TRANSFER_SDR) || nameHdr
                        videoCodecBadge = measuredVideoBadge(
                            format.sampleMimeType, format.width, format.height,
                            isHdrFormat(format.colorInfo?.colorTransfer ?: C.COLOR_TRANSFER_SDR)
                        )
                    }

                    if (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING) error = null
                    if (player.playbackState == Player.STATE_ENDED) {
                        completed = true
                        position = 0L
                        playing = false
                        playRequested = false
                        save()
                        // Autoplay next episode if available and not dismissed
                        if (nextEpisode != null && countdownDismissedForVideo != video?.identity) {
                            playNextEpisode()
                        }
                    } else if (events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) || events.contains(Player.EVENT_POSITION_DISCONTINUITY)) {
                        save()
                    }

                    updateNotification()
                }

                override fun onPlayerError(exception: PlaybackException) {
                    if (player !== p) return
                    val curPos = p.currentPosition
                    val dur = p.duration.takeIf { it > 0 } ?: duration
                    val isNearEnd = dur > 0 && (curPos >= dur - 2500L || position >= dur - 2500L || (dur > 10000L && curPos.toFloat() / dur >= 0.98f))
                    if (isNearEnd && (
                        exception.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                        exception.errorCode == PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE ||
                        exception.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW
                    )) {
                        completed = true
                        position = 0L
                        playing = false
                        playRequested = false
                        buffering = false
                        error = null
                        p.pause()
                        save()
                        cancelNotification()
                        if (nextEpisode != null && countdownDismissedForVideo != video?.identity) {
                            playNextEpisode()
                        }
                        return
                    }

                    if (exception.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED) {
                        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .build()
                        error = null
                        p.prepare()
                        p.play()
                        return
                    }

                    error = "Could not play this file (${exception.errorCodeName}). Verify folder access or check if this codec requires fallback."
                    playing = false; playRequested = false; buffering = false; save()
                    cancelNotification()
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
                .setSubtitleConfigurations(externalSubtitles)
                .build()

            p.setMediaItem(mediaItem)
            p.prepare()
            if (nightModeVoiceBoost) applyNightMode()
            if (suspendedPosition > 0 && rememberProgress) {
                p.seekTo(suspendedPosition)
            }
            mediaItemAssigned = true
            p.setPlaybackSpeed(speed)

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
                    val delta = seekIncrementSeconds * 1000L
                    when (customCommand.customAction) {
                        "ACTION_REWIND_10" -> skip(-delta)
                        "ACTION_FORWARD_10" -> skip(delta)
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }

            val rewindButton = CommandButton.Builder()
                .setDisplayName("Back ${seekIncrementSeconds}s")
                .setIconResId(android.R.drawable.ic_media_previous)
                .setSessionCommand(rewindCmd)
                .build()

            val forwardButton = CommandButton.Builder()
                .setDisplayName("Forward ${seekIncrementSeconds}s")
                .setIconResId(android.R.drawable.ic_media_next)
                .setSessionCommand(forwardCmd)
                .build()

            session = MediaSession.Builder(app, p)
                .setCallback(sessionCallback)
                .setCustomLayout(listOf(rewindButton, forwardButton))
                .build()

            if (autoplay) p.play()
            updateNotification()
        } catch (_: Exception) {
            error = "Could not initialize media session or renderer."
            release()
        }
    }

    private fun isHdrFormat(colorTransfer: Int): Boolean {
        return colorTransfer == C.COLOR_TRANSFER_ST2084 || colorTransfer == C.COLOR_TRANSFER_HLG
    }

    private fun updateTracks(tracks: Tracks) {
        val audioList = mutableListOf<AudioTrackInfo>()
        val subList = mutableListOf<SubtitleTrackInfo>()

        for (g in 0 until tracks.groups.size) {
            val group = tracks.groups[g]
            val type = group.type
            for (t in 0 until group.length) {
                val f = group.getTrackFormat(t)
                val lang = f.language
                val langDisplay = if (lang != null) Locale.forLanguageTag(lang).displayLanguage.ifBlank { "Unknown" } else "Unknown"
                when (type) {
                    C.TRACK_TYPE_AUDIO -> {
                        val mime = f.sampleMimeType ?: f.containerMimeType
                        val name = f.label ?: if (langDisplay != "Unknown") langDisplay else "Audio Track ${audioList.size + 1}"
                        audioList += AudioTrackInfo(
                            id = "$g:$t",
                            groupIndex = g,
                            trackIndex = t,
                            label = name,
                            language = langDisplay,
                            codec = formatAudioCodec(mime),
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
                            format = formatSubtitleFormat(f.sampleMimeType ?: f.containerMimeType),
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
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
            .build()
        if (saveTrackPrefs && track.language.isNotEmpty() && track.language != "Unknown") {
            prefs.edit { putString("preferred_audio_lang", track.language) }
        }
    }

    fun selectSubtitleTrack(track: SubtitleTrackInfo?) {
        val p = player ?: return
        if (track == null) {
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            if (saveTrackPrefs) prefs.edit { putString("preferred_sub_lang", "none") }
        } else {
            val group = p.currentTracks.groups.getOrNull(track.groupIndex) ?: return
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
                .build()
            if (saveTrackPrefs && track.language.isNotEmpty() && track.language != "Unknown") {
                prefs.edit { putString("preferred_sub_lang", track.language) }
            }
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
        updateNotification()
    }

    fun seekTo(value: Long) {
        if (seekable) {
            completed = false
            player?.seekTo(boundedPosition(value, duration))
            position = boundedPosition(value, duration)
            save()
            updateNotification()
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

    fun flushProgress() = save()

    fun refreshHistory() { historyVersion++ }

    private fun save() {
        val v = video ?: return
        if (!rememberProgress) return
        val pos = if (mediaItemAssigned) player?.currentPosition?.coerceAtLeast(0) ?: position else position
        position = pos; suspendedPosition = pos
        prefs.edit { putLong(key(v), pos); putBoolean(key(v) + ".done", completed) }
    }

    fun pauseForBackground() {
        if (!backgroundAudioEnabled) {
            save()
            release()
            cancelNotification()
        }
    }

    fun restoreVisible() { if (video != null && player == null && error == null) prepare(false) }
    fun close() {
        release()
        cancelNotification()
        video = null; fullscreen = false; locked = false; error = null
        position = 0; duration = 0; completed = false
        audioTracks = emptyList(); subtitleTracks = emptyList()
        currentQueue = emptyList()
        nextEpisodeCountdown = null
        chapters = emptyList()
        externalSubtitles = emptyList()
    }

    private fun release() {
        save()
        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
        } catch (_: Exception) {}
        val oldPlayer = player
        player = null; mediaItemAssigned = false
        session?.release(); session = null
        oldPlayer?.release()
        playing = false; playRequested = false; buffering = false; seekable = false
        audioTracks = emptyList(); subtitleTracks = emptyList()
    }

    override fun onCleared() {
        release()
        cancelNotification()
        super.onCleared()
    }
}
