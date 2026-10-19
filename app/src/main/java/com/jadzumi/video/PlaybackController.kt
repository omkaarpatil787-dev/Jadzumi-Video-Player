package com.jadzumi.video

import android.app.Application
import androidx.compose.runtime.*
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ContentDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

@androidx.annotation.OptIn(markerClass=[UnstableApi::class])
class PlaybackController(application: Application): AndroidViewModel(application) {
    private val prefs=application.getSharedPreferences("playback",0)
    var player by mutableStateOf<ExoPlayer?>(null); private set
    private var session: MediaSession?=null
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
    var aspect by mutableFloatStateOf(16f/9f); private set
    var locked by mutableStateOf(false)
    var seekable by mutableStateOf(false); private set
    private var completed by mutableStateOf(false)
    private var suspendedPosition=0L
    private var mediaItemAssigned=false
    private var pollCount=0
    init {
        viewModelScope.launch {
            while(true) {
                delay(500)
                player?.let { p ->
                    if(mediaItemAssigned) position=p.currentPosition.coerceAtLeast(0)
                    duration=p.duration.takeIf { it>0 } ?: 0
                    if(++pollCount % 10 == 0) save()
                }
            }
        }
    }
    private fun key(v:LocalVideo): String = MessageDigest.getInstance("SHA-256").digest(v.identity.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun persistedPosition(v:LocalVideo):Long=resumePosition(prefs.getLong(key(v),0),v.duration,prefs.getBoolean(key(v)+".done",false))
    fun savedPosition(v:LocalVideo):Long {
        if(video?.identity==v.identity) return resumePosition(position,duration.takeIf { it>0 } ?: v.duration,completed)
        return persistedPosition(v)
    }
    fun open(v:LocalVideo) {
        if(video?.identity==v.identity && player!=null) {
            fullscreen=true;locked=false
            if(error!=null) retry() else {
                if(player?.playbackState==Player.STATE_ENDED) { completed=false;player?.seekTo(0) }
                if(player?.playbackState==Player.STATE_IDLE) player?.prepare()
                player?.play()
            }
            return
        }
        release()
        video=v;fullscreen=true;locked=false;error=null;completed=false;aspect=16f/9f
        position=persistedPosition(v);suspendedPosition=position;duration=v.duration
        prepare(autoplay=true)
    }
    private fun prepare(autoplay:Boolean) {
        val v=video ?: return
        if(player!=null) return
        mediaItemAssigned=false
        try {
            require(v.uri.toUri().scheme=="content")
            val app=getApplication<Application>()
            // Content-only reader: this player cannot open network URLs.
            val source=DefaultMediaSourceFactory(DataSource.Factory { ContentDataSource(app) })
            val p=ExoPlayer.Builder(app,DefaultRenderersFactory(app).setEnableDecoderFallback(true))
                .setMediaSourceFactory(source).build()
            player=p
            p.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true)
            p.setHandleAudioBecomingNoisy(true)
            p.addListener(object:Player.Listener {
                override fun onEvents(player:Player, events:Player.Events) {
                    if(this@PlaybackController.player !== player) return
                    playing=player.isPlaying;buffering=player.playbackState==Player.STATE_BUFFERING
                    playRequested=player.playWhenReady && player.playbackState!=Player.STATE_ENDED && player.playbackState!=Player.STATE_IDLE && player.playerError==null
                    speed=player.playbackParameters.speed
                    if(player.isPlaying) completed=false
                    seekable=player.isCurrentMediaItemSeekable
                    val size=player.videoSize
                    if(size.width>0 && size.height>0) {
                        val ratio=size.width*size.pixelWidthHeightRatio/size.height
                        if(ratio.isFinite() && ratio>0f) aspect=ratio
                    }
                    if(player.playbackState==Player.STATE_READY || player.playbackState==Player.STATE_BUFFERING) error=null
                    if(player.playbackState==Player.STATE_ENDED) { completed=true;save() }
                    else if(events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) || events.contains(Player.EVENT_POSITION_DISCONTINUITY)) save()
                }
                override fun onPlayerError(exception:PlaybackException) {
                    if(player !== p) return
                    error="Could not play this file (${exception.errorCodeName}). Check folder access or reconnect storage. This file may also need a decoder not yet included."
                    playing=false;playRequested=false;buffering=false;save()
                }
            })
            p.setMediaItem(MediaItem.Builder().setUri(v.uri).setMediaId(v.identity).setMediaMetadata(MediaMetadata.Builder().setTitle(v.title).build()).build(),suspendedPosition)
            mediaItemAssigned=true
            p.setPlaybackSpeed(speed)
            session=MediaSession.Builder(app,p).build()
            p.prepare();p.playWhenReady=autoplay
        } catch (_: Exception) { error="The player could not open this video. Try refreshing its folder and opening it again.";release() }
    }
    fun toggle() {
        if(error!=null) { retry();return }
        if(player==null) { prepare(true);return }
        val p=player ?: return
        if(p.playbackState==Player.STATE_IDLE) { p.prepare();p.play() }
        else if(p.playbackState==Player.STATE_ENDED) { completed=false;p.seekTo(0);p.play() }
        else if(p.playWhenReady) p.pause() else p.play()
        save()
    }
    fun seekTo(value:Long) { if(seekable) { completed=false;player?.seekTo(boundedPosition(value,duration));position=boundedPosition(value,duration);save() } }
    fun skip(delta:Long) = seekTo(seekPosition(player?.currentPosition ?: position,delta,duration))
    fun changeSpeed(value:Float) { if(value.isFinite()) { speed=value.coerceIn(0.25f,3f);player?.setPlaybackSpeed(speed) } }
    fun retry() { save();release();error=null;prepare(true) }
    private fun save() {
        val v=video ?: return
        // A failed initialization must not replace a saved resume point with the empty player's zero.
        val pos=if(mediaItemAssigned) player?.currentPosition?.coerceAtLeast(0) ?: position else position
        position=pos;suspendedPosition=pos
        prefs.edit { putLong(key(v),pos);putBoolean(key(v)+".done",completed) }
    }
    fun pauseForBackground() { save();release() }
    fun restoreVisible() { if(video!=null && player==null && error==null) prepare(false) }
    fun close() { release();video=null;fullscreen=false;locked=false;error=null;position=0;duration=0;completed=false }
    private fun release() {
        save()
        val oldPlayer=player
        player=null;mediaItemAssigned=false
        session?.release();session=null
        oldPlayer?.release();playing=false;playRequested=false;buffering=false;seekable=false
    }
    override fun onCleared() { release();super.onCleared() }
}
