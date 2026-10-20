package com.jadzumi.video

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.common.util.UnstableApi

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class MainActivity : ComponentActivity() {
    lateinit var playback: PlaybackController
    var videoBounds: Rect? = null
    private val pipAction = "com.jadzumi.video.PIP_CONTROL"
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != pipAction) return
            when (intent.getIntExtra("command", 0)) {
                1 -> playback.skip(-10000)
                2 -> playback.toggle()
                3 -> playback.skip(10000)
            }
        }
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playback = ViewModelProvider(this)[PlaybackController::class.java]
        enableEdgeToEdge()
        ContextCompat.registerReceiver(this, receiver, IntentFilter(pipAction), ContextCompat.RECEIVER_NOT_EXPORTED)

        // Request notification permission on Android 13+ / A16 to allow lock-screen media card
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableMaxRefreshRate()

        setContent { JadzumiApp(playback) }
    }

    private fun enableMaxRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val maxMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
            if (maxMode != null) {
                window.attributes = window.attributes.apply {
                    preferredDisplayModeId = maxMode.modeId
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            @Suppress("DEPRECATION")
            val maxMode = wm?.defaultDisplay?.supportedModes?.maxByOrNull { it.refreshRate }
            if (maxMode != null) {
                window.attributes = window.attributes.apply {
                    preferredDisplayModeId = maxMode.modeId
                }
            }
        }
    }

    fun supportsPip() = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    fun pipParams(): PictureInPictureParams {
        fun action(command: Int, label: String, icon: Int, enabled: Boolean = true) = RemoteAction(
            Icon.createWithResource(this, icon), label, label,
            PendingIntent.getBroadcast(
                this, command,
                Intent(pipAction).setPackage(packageName).putExtra("command", command),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).apply { isEnabled = enabled }

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational((playback.aspect.coerceIn(0.4185f, 2.39f) * 10000).toInt(), 10000))
            .setActions(
                if (playback.video == null) emptyList()
                else listOf(
                    action(1, "Back 10s", android.R.drawable.ic_media_rew, playback.seekable),
                    action(2, if (playback.playRequested) "Pause" else "Play", if (playback.playRequested) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play),
                    action(3, "Forward 10s", android.R.drawable.ic_media_ff, playback.seekable)
                )
            )

        videoBounds?.takeIf { !it.isEmpty && playback.video != null }?.let { bounds ->
            val content = videoContentBounds(bounds.width(), bounds.height(), playback.aspect, playback.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT)
            builder.setSourceRectHint(Rect(bounds.left + content.left, bounds.top + content.top, bounds.left + content.right, bounds.top + content.bottom))
        }

        if (Build.VERSION.SDK_INT >= 31) {
            builder.setAutoEnterEnabled(playback.video != null && playback.fullscreen && playback.playing)
                .setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    fun updateHdrColorMode(isOled: Boolean = DeviceHardwareInspector.isOledOrAmoledDisplay(this)) {
        if (Build.VERSION.SDK_INT >= 26) {
            val shouldBoostHdr = playback.video != null && (playback.brightnessBoost || (playback.isHdr && isOled))
            window.colorMode = if (shouldBoostHdr) {
                ActivityInfo.COLOR_MODE_HDR
            } else {
                ActivityInfo.COLOR_MODE_DEFAULT
            }
        }
    }

    fun updatePip() {
        if (supportsPip()) runCatching { setPictureInPictureParams(pipParams()) }
        updateHdrColorMode()
    }

    fun enterCinemaPip(): Boolean {
        if (!supportsPip() || playback.video == null) return false
        playback.fullscreen = true
        playback.locked = false
        return runCatching { enterPictureInPictureMode(pipParams()) }.getOrDefault(false)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < 31 && playback.fullscreen && playback.playing) enterCinemaPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        playback.inPip = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            playback.fullscreen = true
            playback.locked = false
        } else {
            playback.fullscreen = true
        }
        updatePip()
    }

    override fun onStart() {
        super.onStart()
        if (::playback.isInitialized) playback.restoreVisible()
    }

    override fun onStop() {
        if (!isChangingConfigurations && ::playback.isInitialized) {
            playback.pauseForBackground()
        }
        super.onStop()
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }
}
