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
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    var enteringPip by mutableStateOf(false)
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

    var renderedVideoIdentity by mutableStateOf<String?>(null); private set

    var runtimeVideoDecoder by mutableStateOf<DecoderInspection?>(null); private set
    var runtimeAudioDecoder by mutableStateOf<DecoderInspection?>(null); private set
    var runtimeVideoFormat by mutableStateOf<VideoInspection?>(null); private set
    var runtimeAudioTracks by mutableStateOf<List<AudioInspection>>(emptyList()); private set
    var runtimeSubtitleTracks by mutableStateOf<List<SubtitleInspection>>(emptyList()); private set
    var runtimeAudioFormat by mutableStateOf<AudioInspection?>(null); private set

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

    // Display During Playback Overlay Controls
    var showRemainingTime by mutableStateOf(prefs.getBoolean("show_remaining_time", false))
        private set
    var showPlayerClock by mutableStateOf(prefs.getBoolean("show_player_clock", true))
        private set
    var showBatteryPercentage by mutableStateOf(prefs.getBoolean("show_battery_pct", true))
        private set

    val displayDuringPlaybackSummary: String
        get() {
            val list = mutableListOf<String>()
            if (showRemainingTime) list.add("Remaining Time")
            if (showPlayerClock) list.add("Clock")
            if (showBatteryPercentage) list.add("Battery Percentage")
            return if (list.isEmpty()) "None" else list.joinToString(", ")
        }

    fun updateDisplayDuringPlayback(remaining: Boolean, clock: Boolean, battery: Boolean) {
        showRemainingTime = remaining
        showPlayerClock = clock
        showBatteryPercentage = battery
        prefs.edit {
            putBoolean("show_remaining_time", remaining)
            putBoolean("show_player_clock", clock)
            putBoolean("show_battery_pct", battery)
        }
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
    var dialogueReplayActive by mutableStateOf(false); private set
    private var dialogueWindow: DialogueReplayWindow? = null
    private var replaySeekParameters: SeekParameters? = null
    private var replayReturnToPause = false

    // GPU Video Enhancer & Color Studio
    var videoEnhancerProfile by mutableStateOf(
        ColorEnhancementProfile.fromName(prefs.getString("video_enhancer_profile", ColorEnhancementProfile.NATURAL.name))
    ); private set

    var videoEnhancerParams by mutableStateOf(
        ColorStudioParams(
            contrast = prefs.getFloat("video_enhancer_contrast", 1.0f),
            shadowLift = prefs.getFloat("video_enhancer_shadow", 0.0f),
            saturation = prefs.getFloat("video_enhancer_saturation", 1.0f),
            vibrance = prefs.getFloat("video_enhancer_vibrance", 0.0f),
            amoledDeepBlack = prefs.getBoolean("video_enhancer_amoled", false)
        )
    ); private set

    // Expanded Audio Studio: Channel Modes
    var audioChannelMode by mutableStateOf(
        AudioChannelMode.fromName(prefs.getString("audio_channel_mode", AudioChannelMode.DEFAULT.name))
    ); private set

    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null
    var historyVersion by mutableIntStateOf(0); private set

    private val channelId = "playback_channel"
    private val notificationId = 1001
    private val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var completed by mutableStateOf(false)
    private var suspendedPosition = 0L
    private var mediaItemAssigned = false
    private var pollCount = 0
    var matchRefreshRate by mutableStateOf(prefs.getBoolean("match_refresh_rate", false)); private set

    fun updateRefreshMatching(enabled: Boolean) {
        matchRefreshRate = enabled
        prefs.edit().putBoolean("match_refresh_rate", enabled).apply()
        player?.setVideoChangeFrameRateStrategy(if (enabled) C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS else C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF)
    }

    var autoplayNext by mutableStateOf(prefs.getBoolean("autoplay_next", true)); private set
    var continuationPrompt by mutableStateOf<LocalVideo?>(null); private set
    var continuationMessage by mutableStateOf(""); private set
    private var nextCountdownJob: Job? = null
    private var endedHandledFor: String? = null
    var sleepPlan by mutableStateOf<SleepPlan?>(null); private set
    var sleepRemainingSeconds by mutableLongStateOf(0L); private set
    private var fadedPlayer: ExoPlayer? = null
    private var volumeBeforeFade: Float? = null
    var bookmarkVersion by mutableIntStateOf(0); private set

    init {
        createNotificationChannel()
        viewModelScope.launch {
            while (true) {
                delay(500)
                processSleepTimer()
                player?.let { p ->
                    if (mediaItemAssigned) position = p.currentPosition.coerceAtLeast(0)
                    val currentDur = p.duration.takeIf { it > 0 } ?: video?.duration?.takeIf { it > 0 } ?: 0
                    if (currentDur > 0) {
                        if (duration != currentDur) updateChapters(currentDur)
                        duration = currentDur
                    }
                    if (dialogueWindow?.reachedEnd(position) == true) stopDialogueReplay(restorePlaybackState = true)
                    if (++pollCount % 10 == 0) save()


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
                list.add(ChapterMarker(time, "Jump point $i", time.toFloat() / dur.toFloat()))
            }
        }
        if (creditPos > 30_000L) {
            list.add(ChapterMarker(creditPos, "Estimated credits", creditPos.toFloat() / dur.toFloat()))
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
            val needEnhancer = nightModeVoiceBoost || dialogueReplayActive || audioChannelMode == AudioChannelMode.DIALOGUE_FOCUS
            if (needEnhancer && sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0) {
                if (loudnessEnhancer == null) {
                    loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(sessionId)
                }
                val gain = when {
                    nightModeVoiceBoost -> 600
                    dialogueReplayActive -> 500
                    audioChannelMode == AudioChannelMode.DIALOGUE_FOCUS -> 350
                    else -> 0
                }
                loudnessEnhancer?.setTargetGain(gain)
                loudnessEnhancer?.enabled = true
            } else {
                loudnessEnhancer?.enabled = false
                loudnessEnhancer?.release()
                loudnessEnhancer = null
            }
        } catch (_: Exception) {}
    }

    // --- GPU Video Enhancer & Color Studio Methods ---

    fun updateColorProfile(profile: ColorEnhancementProfile) {
        videoEnhancerProfile = profile
        if (profile != ColorEnhancementProfile.CUSTOM) {
            videoEnhancerParams = profile.params
        }
        prefs.edit()
            .putString("video_enhancer_profile", profile.name)
            .putFloat("video_enhancer_contrast", videoEnhancerParams.contrast)
            .putFloat("video_enhancer_shadow", videoEnhancerParams.shadowLift)
            .putFloat("video_enhancer_saturation", videoEnhancerParams.saturation)
            .putFloat("video_enhancer_vibrance", videoEnhancerParams.vibrance)
            .putBoolean("video_enhancer_amoled", videoEnhancerParams.amoledDeepBlack)
            .apply()
        applyVideoEffects()
    }

    fun updateColorParams(params: ColorStudioParams) {
        videoEnhancerProfile = ColorEnhancementProfile.CUSTOM
        videoEnhancerParams = params
        prefs.edit()
            .putString("video_enhancer_profile", ColorEnhancementProfile.CUSTOM.name)
            .putFloat("video_enhancer_contrast", params.contrast)
            .putFloat("video_enhancer_shadow", params.shadowLift)
            .putFloat("video_enhancer_saturation", params.saturation)
            .putFloat("video_enhancer_vibrance", params.vibrance)
            .putBoolean("video_enhancer_amoled", params.amoledDeepBlack)
            .apply()
        applyVideoEffects()
    }

    fun resetColorStudio() {
        videoEnhancerProfile = ColorEnhancementProfile.NATURAL
        videoEnhancerParams = ColorStudioParams.NEUTRAL
        prefs.edit()
            .putString("video_enhancer_profile", ColorEnhancementProfile.NATURAL.name)
            .putFloat("video_enhancer_contrast", 1.0f)
            .putFloat("video_enhancer_shadow", 0.0f)
            .putFloat("video_enhancer_saturation", 1.0f)
            .putFloat("video_enhancer_vibrance", 0.0f)
            .putBoolean("video_enhancer_amoled", false)
            .apply()
        applyVideoEffects()
    }

    fun applyVideoEffects() {
        val p = player ?: return
        try {
            // Rule: When HDR is active, or Natural profile is chosen, or parameters are neutral,
            // strictly pass emptyList() to preserve 100% zero-copy, untouched SurfaceView playback.
            if (isHdr || videoEnhancerProfile == ColorEnhancementProfile.NATURAL || videoEnhancerParams.isNeutral()) {
                p.setVideoEffects(emptyList())
            } else {
                p.setVideoEffects(listOf(VideoEnhancerEffect(videoEnhancerParams)))
            }
        } catch (_: Exception) {
            // Graceful fallback to default playback without throwing or disrupting audio
            p.setVideoEffects(emptyList())
        }
    }

    // --- Audio Studio Methods ---

    fun updateAudioChannelMode(mode: AudioChannelMode) {
        audioChannelMode = mode
        prefs.edit().putString("audio_channel_mode", mode.name).apply()
        applyAudioChannelMode()
    }

    fun applyAudioChannelMode() {
        val p = player ?: return
        try {
            when (audioChannelMode) {
                AudioChannelMode.STEREO_DOWNMIX -> {
                    p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                        .setMaxAudioChannelCount(2)
                        .build()
                }
                AudioChannelMode.DEFAULT -> {
                    p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                        .setMaxAudioChannelCount(Int.MAX_VALUE)
                        .build()
                }
                AudioChannelMode.DIALOGUE_FOCUS -> {
                    p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                        .setMaxAudioChannelCount(2)
                        .build()
                    applyNightMode()
                }
            }
        } catch (_: Exception) {}
    }

    fun inspectActiveAudioStream(): ActiveAudioStreamInfo {
        val v = video
        val container = AudioStudioHelper.detectContainer(v?.uri, v?.name)
        val audioFmt = runtimeAudioFormat
        val activeTrack = audioTracks.firstOrNull { it.isSelected }

        val codecName = when {
            activeTrack != null && activeTrack.codec.isNotBlank() -> activeTrack.codec
            audioFmt?.mime != null -> formatAudioCodec(audioFmt.mime)
            else -> "Not reported"
        }

        val channelLayout = AudioStudioHelper.formatChannelLayout(audioFmt?.channels)
        val sampleRate = AudioStudioHelper.formatSampleRate(audioFmt?.sampleRate)
        val bitDepth = AudioStudioHelper.formatBitDepth(null, audioFmt?.mime)

        val (decName, decEngine) = AudioStudioHelper.formatDecoderEngine(
            runtimeAudioDecoder?.name,
            runtimeAudioDecoder?.hardware
        )

        val outputChannels = when (audioChannelMode) {
            AudioChannelMode.STEREO_DOWNMIX, AudioChannelMode.DIALOGUE_FOCUS -> "Stereo (2 channels downmixed)"
            AudioChannelMode.DEFAULT -> if ((audioFmt?.channels ?: 2) > 2) "Multichannel (${audioFmt?.channels} ch direct)" else "Stereo (2 channels)"
        }

        val outputSampleRate = sampleRate
        val outputEncoding = "PCM 16-bit"

        val processingList = mutableListOf<String>()
        when (audioChannelMode) {
            AudioChannelMode.STEREO_DOWNMIX -> processingList.add("Stereo Downmix")
            AudioChannelMode.DIALOGUE_FOCUS -> processingList.add("Dialogue Focus")
            AudioChannelMode.DEFAULT -> processingList.add("Default / Direct")
        }
        if (nightModeVoiceBoost) {
            processingList.add("Night Mode (Dynamic Compression)")
        }
        if (audioDelayMs != 0L) {
            processingList.add("Lip-Sync (${audioDelayMs}ms)")
        }

        return ActiveAudioStreamInfo(
            container = container,
            sourceCodec = codecName,
            sourceChannels = channelLayout,
            sourceSampleRate = sampleRate,
            sourceBitDepth = bitDepth,
            activeDecoder = decName,
            decoderEngine = decEngine,
            playerOutputChannels = outputChannels,
            playerOutputSampleRate = outputSampleRate,
            playerOutputEncoding = outputEncoding,
            activeProcessing = processingList
        )
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
        @Suppress("UNUSED_EXPRESSION")
        historyVersion
        if (!rememberProgress) return false
        val done = prefs.getBoolean(key(v) + ".done", false) ||
            (video?.identity == v.identity && completed)
        return watchHistoryIsComplete(savedPosition(v), v.duration, done)
    }

    fun isInProgress(v: LocalVideo): Boolean = !isWatched(v) && savedPosition(v) > 0L

    fun addExternalSubtitle(uri: Uri, label: String = "External Subtitle") {
        val uriStr = (if (label != "External Subtitle") label else uri.toString()).lowercase(Locale.ROOT)
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
        nextCountdownJob?.cancel(); nextCountdownJob = null
        continuationPrompt = null
        nextEpisodeCountdown = null
        countdownDismissedForVideo = video?.identity
    }

    fun updateAutoplayNext(enabled: Boolean) {
        autoplayNext = enabled
        prefs.edit { putBoolean("autoplay_next", enabled) }
        if (!enabled) {
            nextCountdownJob?.cancel(); nextCountdownJob = null
            nextEpisodeCountdown = null
        }
    }

    fun playNextEpisode() {
        val next = nextEpisode ?: return
        val current = video ?: return
        val warning = episodeContinuationWarning(current.name, next.name)
        if (warning != null) {
            nextCountdownJob?.cancel(); nextCountdownJob = null; nextEpisodeCountdown = null
            continuationMessage = warning; continuationPrompt = next
        } else confirmNextEpisode(next)
    }

    fun confirmNextEpisode(next: LocalVideo? = continuationPrompt) {
        val candidate = next ?: return
        val queue = currentQueue
        nextCountdownJob?.cancel(); nextCountdownJob = null
        continuationPrompt = null; nextEpisodeCountdown = null
        open(candidate, queue)
    }

    private fun handleEpisodeEnd(allowAuto: Boolean = true) {
        val current = video ?: return
        if (endedHandledFor == current.identity) return
        endedHandledFor = current.identity
        val plan = sleepPlan
        if (plan != null && plan.mode != SleepMode.TIMER) {
            val remaining = episodesAfterEnd(plan)
            if (remaining == 0) {
                cancelSleep(); player?.pause(); dismissNextEpisodeCountdown(); return
            }
            sleepPlan = plan.copy(episodes = remaining)
        }
        if (plan?.mode == SleepMode.TIMER && android.os.SystemClock.elapsedRealtime() >= plan.deadlineMs) {
            processSleepTimer(); return
        }
        val next = nextEpisode ?: return
        if (countdownDismissedForVideo == current.identity) return
        val warning = episodeContinuationWarning(current.name, next.name)
        if (!allowAuto || !autoplayNext || warning != null) {
            continuationMessage = warning ?: if (allowAuto) "Ready for the next episode?" else "Playback stopped near the end. Continue with the next file?"
            continuationPrompt = next
            return
        }
        nextCountdownJob?.cancel()
        nextCountdownJob = viewModelScope.launch {
            for (seconds in 12 downTo 1) {
                if (video?.identity != current.identity) return@launch
                nextEpisodeCountdown = seconds
                delay(1000)
            }
            nextEpisodeCountdown = null
            confirmNextEpisode(next)
        }
    }

    private fun seriesKey(suffix: String): String? {
        val name = video?.name ?: return null
        val series = EpisodePatterns.parse(name)?.seriesTitle ?: return null
        val token = MessageDigest.getInstance("SHA-256").digest(normalizedSeriesName(series).toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "series_${token}_$suffix"
    }

    private fun applySavedTrackLanguages(p: ExoPlayer) {
        if (!saveTrackPrefs) return
        val audio = seriesKey("audio")?.let { prefs.getString(it, null) } ?: prefs.getString("preferred_audio_lang", null)
        val subtitle = seriesKey("subtitle")?.let { prefs.getString(it, null) } ?: prefs.getString("preferred_sub_lang", null)
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setPreferredAudioLanguage(preferredLanguageTag(audio))
            .setPreferredTextLanguage(preferredLanguageTag(subtitle))
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, subtitle == "none")
            .build()
    }

    fun startSleep(minutes: Int) {
        cancelSleep()
        sleepPlan = SleepPlan(SleepMode.TIMER, sleepDeadline(android.os.SystemClock.elapsedRealtime(), minutes))
        processSleepTimer()
    }
    fun sleepAfterEpisodes(count: Int = 1) {
        require(count in 1..10)
        cancelSleep()
        if (count == 1 && player?.playbackState == Player.STATE_ENDED) {
            player?.pause(); dismissNextEpisodeCountdown(); return
        }
        sleepPlan = SleepPlan(if (count == 1) SleepMode.END_CURRENT else SleepMode.EPISODES, episodes = count)
    }
    private fun restoreSleepVolume() {
        val previous = volumeBeforeFade
        if (previous != null) fadedPlayer?.let { runCatching { it.volume = previous } }
        fadedPlayer = null; volumeBeforeFade = null
    }
    fun cancelSleep() {
        restoreSleepVolume(); sleepPlan = null; sleepRemainingSeconds = 0
    }
    private fun processSleepTimer(): Boolean {
        val plan = sleepPlan ?: return false
        if (plan.mode != SleepMode.TIMER) return false
        val remaining = (plan.deadlineMs - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0)
        sleepRemainingSeconds = (remaining + 999) / 1000
        val p = player
        if (remaining == 0L) {
            p?.pause(); cancelSleep(); dismissNextEpisodeCountdown(); return true
        }
        if (p != null && remaining < 10_000 && p.isPlaying) {
            if (fadedPlayer !== p) { restoreSleepVolume(); fadedPlayer = p; volumeBeforeFade = p.volume }
            p.volume = (volumeBeforeFade ?: 1f) * sleepFadeFactor(remaining)
        }
        return false
    }

    fun pauseForNotes(): Long {
        val at = player?.currentPosition ?: position
        player?.pause()
        if (player?.playbackState == Player.STATE_ENDED) dismissNextEpisodeCountdown()
        save()
        return at.coerceAtLeast(0)
    }
    fun sceneBookmarks(): List<SceneBookmark> {
        @Suppress("UNUSED_EXPRESSION")
        bookmarkVersion
        val v = video ?: return emptyList()
        return SceneBookmarkStore.read(getApplication(), v.identity)
    }
    fun saveScene(title: String, note: String, atMs: Long, id: String? = null): Boolean {
        val v = video ?: return false
        return runCatching {
            val values = sceneBookmarks().filterNot { it.id == id } + SceneBookmark(id ?: java.util.UUID.randomUUID().toString(), boundedPosition(atMs, duration), title.trim(), note)
            SceneBookmarkStore.write(getApplication(), v.identity, values)
            bookmarkVersion++
            true
        }.getOrDefault(false)
    }
    fun removeScene(id: String) {
        val v = video ?: return
        SceneBookmarkStore.write(getApplication(), v.identity, sceneBookmarks().filterNot { it.id == id })
        bookmarkVersion++
    }

    fun open(v: LocalVideo, queue: List<LocalVideo> = emptyList()) {
        nextCountdownJob?.cancel(); nextCountdownJob = null; continuationPrompt = null; endedHandledFor = null
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
                    if (other != null && normalizedSeriesName(other.seriesTitle) == normalizedSeriesName(info.seriesTitle)) {
                        candidate to other
                    } else null
                }.sortedWith(compareBy<Pair<LocalVideo, SeriesEpisodeInfo>> { if (it.second.season == 0) Int.MAX_VALUE else it.second.season }.thenBy { it.second.episode }).map { it.first }
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

    private fun inspectedVideo(format: Format): VideoInspection = VideoInspection(
        mime = format.sampleMimeType,
        width = format.width.takeIf { it > 0 }, height = format.height.takeIf { it > 0 },
        frameRate = format.frameRate.toDouble().takeIf { it.isFinite() && it > 0 },
        colorStandard = format.colorInfo?.colorSpace?.takeIf { it > 0 },
        colorTransfer = format.colorInfo?.colorTransfer?.takeIf { it > 0 },
        colorRange = format.colorInfo?.colorRange?.takeIf { it > 0 },
        bitDepth = format.colorInfo?.lumaBitdepth?.takeIf { it in 8..16 }
    )

    private fun recordDecoder(source: ExoPlayer, name: String, videoDecoder: Boolean) {
        if (player !== source) return
        val initial = DecoderInspection(name,
            if (name.startsWith("ffmpeg", ignoreCase = true)) DecoderHardware.SOFTWARE else DecoderHardware.NOT_REPORTED)
        if (videoDecoder) runtimeVideoDecoder = initial else runtimeAudioDecoder = initial
        if (initial.hardware == DecoderHardware.SOFTWARE || Build.VERSION.SDK_INT < 29) return
        viewModelScope.launch {
            val kind = withContext(Dispatchers.Default) {
                if (Build.VERSION.SDK_INT < 29) return@withContext DecoderHardware.NOT_REPORTED
                runCatching {
                    val codec = android.media.MediaCodecList(android.media.MediaCodecList.ALL_CODECS).codecInfos
                        .firstOrNull { !it.isEncoder && it.name.equals(name, ignoreCase = true) }
                    when {
                        codec?.isSoftwareOnly == true -> DecoderHardware.SOFTWARE
                        codec?.isHardwareAccelerated == true -> DecoderHardware.HARDWARE
                        else -> DecoderHardware.NOT_REPORTED
                    }
                }.getOrDefault(DecoderHardware.NOT_REPORTED)
            }
            if (player !== source) return@launch
            if (videoDecoder && runtimeVideoDecoder?.name == name) runtimeVideoDecoder = DecoderInspection(name, kind)
            if (!videoDecoder && runtimeAudioDecoder?.name == name) runtimeAudioDecoder = DecoderInspection(name, kind)
        }
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
            p.setVideoChangeFrameRateStrategy(if (matchRefreshRate) C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS else C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF)
            player = p
            applyAudioChannelMode()
            applyVideoEffects()

            p.addAnalyticsListener(object : AnalyticsListener {
                override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
                    if (player === p) renderedVideoIdentity = video?.identity
                }

                override fun onVideoDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) {
                    recordDecoder(p, decoderName, true)
                }
                override fun onAudioDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) {
                    recordDecoder(p, decoderName, false)
                }
                override fun onVideoInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format, decoderReuseEvaluation: DecoderReuseEvaluation?) {
                    if (player === p) {
                        runtimeVideoFormat = inspectedVideo(format)
                    }
                }
                override fun onAudioInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format, decoderReuseEvaluation: DecoderReuseEvaluation?) {
                    if (player === p) {
                        runtimeAudioFormat = AudioInspection(format.sampleMimeType,
                            format.channelCount.takeIf { it > 0 }, format.sampleRate.takeIf { it > 0 }, format.language)
                    }
                }
                override fun onVideoDecoderReleased(eventTime: AnalyticsListener.EventTime, decoderName: String) {
                    if (player === p && runtimeVideoDecoder?.name == decoderName) {
                        runtimeVideoDecoder = null
                    }
                }
                override fun onAudioDecoderReleased(eventTime: AnalyticsListener.EventTime, decoderName: String) {
                    if (player === p && runtimeAudioDecoder?.name == decoderName) {
                        runtimeAudioDecoder = null
                    }
                }
            })

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
                    if (nightModeVoiceBoost || dialogueReplayActive) applyNightMode()
                }

                override fun onTracksChanged(tracks: Tracks) {
                    if (this@PlaybackController.player !== p) return
                    updateTracks(tracks)
                }

                override fun onEvents(player: Player, events: Player.Events) {
                    if (this@PlaybackController.player !== player) return
                    if (dialogueReplayActive && (!player.playWhenReady || player.playbackState == Player.STATE_ENDED)) {
                        stopDialogueReplay()
                    }
                    playing = player.isPlaying
                    buffering = player.playbackState == Player.STATE_BUFFERING
                    playRequested = (player.playWhenReady &&
                        player.playbackState != Player.STATE_ENDED &&
                        player.playbackState != Player.STATE_IDLE &&
                        player.playerError == null)
                    speed = player.playbackParameters.speed
                    if (player.isPlaying) { completed = false; endedHandledFor = null }
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
                        runtimeVideoFormat = inspectedVideo(format)
                        val nameHdr = video?.name?.let {
                            it.contains("hdr", ignoreCase = true) ||
                            it.contains("10bit", ignoreCase = true) ||
                            it.contains("dovi", ignoreCase = true) ||
                            it.contains("dv", ignoreCase = true) ||
                            it.contains("hlg", ignoreCase = true)
                        } ?: false
                        val newHdr = isHdrFormat(format.colorInfo?.colorTransfer ?: C.COLOR_TRANSFER_SDR) || nameHdr
                        if (isHdr != newHdr) {
                            isHdr = newHdr
                            applyVideoEffects()
                        }
                        videoCodecBadge = measuredVideoBadge(
                            format.sampleMimeType, format.width, format.height,
                            isHdrFormat(format.colorInfo?.colorTransfer ?: C.COLOR_TRANSFER_SDR)
                        )
                    }

                    if (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING) {
                        error = null
                    }
                    if (player.playbackState == Player.STATE_ENDED) {
                        completed = true
                        position = 0L
                        playing = false
                        playRequested = false
                        save()
                        handleEpisodeEnd()
                    } else if (events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) || events.contains(Player.EVENT_POSITION_DISCONTINUITY)) {
                        save()
                    }

                    updateNotification()
                }

                override fun onPlayerError(exception: PlaybackException) {
                    if (player !== p) return
                    stopDialogueReplay()
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
                        handleEpisodeEnd(allowAuto = false)
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

            applySavedTrackLanguages(p)
            p.setMediaItem(mediaItem)
            p.prepare()
            if (nightModeVoiceBoost || dialogueReplayActive) applyNightMode()
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

            if (autoplay && !processSleepTimer()) p.play()
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
        val inspectedAudio = mutableListOf<AudioInspection>()
        val inspectedSubtitles = mutableListOf<SubtitleInspection>()

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
                        inspectedAudio.add(AudioInspection(mime, f.channelCount.takeIf { it > 0 }, f.sampleRate.takeIf { it > 0 }, f.language))
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
                        (f.sampleMimeType ?: f.containerMimeType)?.let { inspectedSubtitles.add(SubtitleInspection(it, f.language)) }
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
        runtimeAudioTracks = inspectedAudio
        runtimeSubtitleTracks = inspectedSubtitles
    }

    fun selectAudioTrack(track: AudioTrackInfo) {
        val p = player ?: return
        val group = p.currentTracks.groups.getOrNull(track.groupIndex) ?: return
        if (track.trackIndex !in 0 until group.length) return
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
            .build()
        if (saveTrackPrefs) {
            val language = preferredLanguageTag(group.getTrackFormat(track.trackIndex).language) ?: "auto"
            prefs.edit { putString("preferred_audio_lang", language); seriesKey("audio")?.let { putString(it, language) } }
        }
    }

    fun selectSubtitleTrack(track: SubtitleTrackInfo?) {
        val p = player ?: return
        if (track == null) {
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            if (saveTrackPrefs) prefs.edit { putString("preferred_sub_lang", "none"); seriesKey("subtitle")?.let { putString(it, "none") } }
        } else {
            val group = p.currentTracks.groups.getOrNull(track.groupIndex) ?: return
            if (track.trackIndex !in 0 until group.length) return
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
                .build()
            if (saveTrackPrefs) {
                val language = preferredLanguageTag(group.getTrackFormat(track.trackIndex).language) ?: "auto"
                prefs.edit { putString("preferred_sub_lang", language); seriesKey("subtitle")?.let { putString(it, language) } }
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

    fun replayDialogue(): Boolean {
        val p = player ?: return false
        if (!seekable) return false
        val window = dialogueReplayWindow(p.currentPosition, duration) ?: return false
        stopDialogueReplay()
        replayReturnToPause = !p.playWhenReady
        replaySeekParameters = p.seekParameters
        dialogueWindow = window
        dialogueReplayActive = true
        applyNightMode() // Temporary effect only: never changes the saved voice-boost preference.
        p.setSeekParameters(SeekParameters.EXACT)
        p.seekTo(window.startMs)
        position = window.startMs
        nextEpisodeCountdown = null
        p.play()
        return true
    }

    fun stopDialogueReplay(restorePlaybackState: Boolean = false) {
        if (!dialogueReplayActive) return
        val pauseAfter = restorePlaybackState && replayReturnToPause
        dialogueReplayActive = false
        dialogueWindow = null
        replaySeekParameters?.let { player?.setSeekParameters(it) }
        replaySeekParameters = null
        replayReturnToPause = false
        applyNightMode()
        if (pauseAfter) player?.pause()
    }

    fun seekTo(value: Long) {
        if (seekable) {
            stopDialogueReplay()
            nextCountdownJob?.cancel(); nextCountdownJob = null; nextEpisodeCountdown = null
            continuationPrompt = null; endedHandledFor = null
            completed = false
            player?.seekTo(boundedPosition(value, duration))
            position = boundedPosition(value, duration)
            save()
            updateNotification()
        }
    }

    fun seekToSubtitle(value: Long) {
        val p = player ?: return
        if (!seekable) return
        stopDialogueReplay()
        val previous = p.seekParameters
        p.setSeekParameters(SeekParameters.EXACT)
        seekTo(value)
        p.setSeekParameters(previous)
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

    fun refreshHistory() { historyVersion++; bookmarkVersion++ }

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
        cancelSleep(); dismissNextEpisodeCountdown()
        release()
        cancelNotification()
        video = null; fullscreen = false; locked = false; error = null
        enteringPip = false
        position = 0; duration = 0; completed = false
        audioTracks = emptyList(); subtitleTracks = emptyList()
        currentQueue = emptyList()
        nextEpisodeCountdown = null
        chapters = emptyList()
        externalSubtitles = emptyList()
    }

    private fun release() {
        restoreSleepVolume()
        stopDialogueReplay()
        save()
        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
        } catch (_: Exception) {}
        val oldPlayer = player
        player = null; mediaItemAssigned = false
        renderedVideoIdentity = null
        runtimeVideoDecoder = null; runtimeAudioDecoder = null
        runtimeVideoFormat = null; runtimeAudioFormat = null
        runtimeAudioTracks = emptyList(); runtimeSubtitleTracks = emptyList()
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
