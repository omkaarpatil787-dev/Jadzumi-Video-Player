package com.jadzumi.video

import android.content.Context
import android.media.audiofx.Visualizer
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

enum class HapticSubwooferMode(val id: String, val displayName: String, val durationMs: Long, val amplitude: Int) {
    OFF("off", "Off", 0L, 0),
    GENTLE("gentle", "Gentle (Subtle Rumble)", 35L, 90),
    BALANCED("balanced", "Balanced (4D Cinema)", 55L, 160),
    HIGH_OCTANE("high_octane", "High-Octane (Subwoofer Punch)", 80L, 255);

    companion object {
        fun fromId(id: String?): HapticSubwooferMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OFF
    }
}

/**
 * 4D Tactile Cinema Subwoofer Engine.
 *
 * Captures real-time sub-bass audio energy (20Hz–120Hz) directly from the active media session
 * and converts low-end bass rumbles and explosions into physical tactile vibrations.
 */
class HapticSubwooferEngine(private val context: Context) {
    private var visualizer: Visualizer? = null
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var currentMode: HapticSubwooferMode = HapticSubwooferMode.OFF
        private set

    private var lastVibrateTime = 0L

    fun attach(audioSessionId: Int, mode: HapticSubwooferMode = currentMode) {
        currentMode = mode
        release()
        if (mode == HapticSubwooferMode.OFF || audioSessionId == 0 || audioSessionId == -1) return

        try {
            val captureSizeRange = Visualizer.getCaptureSizeRange()
            val size = if (captureSizeRange.isNotEmpty()) captureSizeRange[0] else 128
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = size
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}

                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (currentMode == HapticSubwooferMode.OFF || fft == null || fft.isEmpty()) return
                        analyzeFftAndRumble(fft)
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (e: Exception) {
            Log.w("HapticSubwoofer", "Visualizer initialization skipped or restricted: ${e.message}")
        }
    }

    private fun analyzeFftAndRumble(fft: ByteArray) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastVibrateTime < 180L) return

        // Bins 1..4 represent low sub-bass energy (approx 20Hz - 120Hz)
        var lowEnergy = 0.0
        val binCount = minOf(4, fft.size / 2)
        for (i in 1..binCount) {
            val r = fft[2 * i].toInt()
            val im = fft[2 * i + 1].toInt()
            lowEnergy += kotlin.math.hypot(r.toDouble(), im.toDouble())
        }
        val avgEnergy = if (binCount > 0) lowEnergy / binCount else 0.0

        // Threshold for bass impact trigger
        if (avgEnergy > 42.0) {
            lastVibrateTime = now
            triggerPulse(currentMode)
        }
    }

    fun triggerPulse(mode: HapticSubwooferMode = currentMode) {
        if (mode == HapticSubwooferMode.OFF || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(mode.durationMs, mode.amplitude)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(mode.durationMs)
            }
        } catch (_: Exception) {}
    }

    fun setMode(mode: HapticSubwooferMode, audioSessionId: Int) {
        currentMode = mode
        if (mode == HapticSubwooferMode.OFF) {
            release()
        } else {
            attach(audioSessionId, mode)
        }
    }

    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {}
        visualizer = null
    }
}
