package com.jadzumi.video

import android.content.Context
import android.media.audiofx.Virtualizer
import android.util.Log

/**
 * Spatial Cinema 3D audio virtualizer engine.
 *
 * Utilizes Android's hardware/DSP [Virtualizer] to expand the stereo soundstage into a multi-channel
 * binaural acoustic field for headphones and multi-speaker setups.
 */
enum class Spatial3DMode(val id: String, val displayName: String, val strength: Short) {
    OFF("off", "Off (Stereo Direct)", 0),
    SUBTLE("subtle", "Subtle (Club Acoustic)", 350),
    STUDIO("studio", "Studio 3D (Reference Room)", 650),
    WIDE_IMAX("imax", "Wide IMAX 3D (Acoustic Theater)", 1000);

    companion object {
        fun fromId(id: String?): Spatial3DMode = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OFF
    }
}

class SpatialAudioEngine(private val context: Context) {
    private var virtualizer: Virtualizer? = null
    var currentMode: Spatial3DMode = Spatial3DMode.OFF
        private set

    fun attach(audioSessionId: Int, mode: Spatial3DMode = currentMode) {
        if (audioSessionId == 0 || audioSessionId == -1) return
        try {
            release()
            currentMode = mode
            if (mode == Spatial3DMode.OFF) return

            virtualizer = Virtualizer(0, audioSessionId).apply {
                if (strengthSupported) {
                    setStrength(mode.strength)
                }
                enabled = true
            }
        } catch (e: Exception) {
            Log.w("SpatialAudioEngine", "Failed to attach Virtualizer: ${e.message}")
        }
    }

    fun setMode(mode: Spatial3DMode) {
        currentMode = mode
        try {
            if (mode == Spatial3DMode.OFF) {
                virtualizer?.enabled = false
            } else {
                virtualizer?.let { v ->
                    if (v.strengthSupported) {
                        v.setStrength(mode.strength)
                    }
                    v.enabled = true
                }
            }
        } catch (e: Exception) {
            Log.w("SpatialAudioEngine", "Error setting spatial 3D mode: ${e.message}")
        }
    }

    fun release() {
        try {
            virtualizer?.enabled = false
            virtualizer?.release()
        } catch (_: Exception) {}
        virtualizer = null
    }
}
