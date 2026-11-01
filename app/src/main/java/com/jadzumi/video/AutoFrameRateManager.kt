package com.jadzumi.video

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.roundToInt

data class AfrMatchResult(
    val modeId: Int,
    val targetRefreshRate: Float,
    val description: String,
    val isExact: Boolean
)

object AutoFrameRateManager {

    /**
     * Determines the optimal display mode for a given content frame rate.
     * Prioritizes exact matches, then integer multiples (e.g., 120Hz for 24fps = 5:5 pulldown).
     */
    fun findOptimalDisplayMode(activity: Activity, contentFps: Double): AfrMatchResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        if (!contentFps.isFinite() || contentFps <= 0.0) return null

        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            val wm = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            @Suppress("DEPRECATION")
            wm?.defaultDisplay
        } ?: return null

        val modes = display.supportedModes ?: return null
        if (modes.isEmpty()) return null

        // 1. Look for near-exact match (within 0.06 Hz)
        val exactMode = modes.firstOrNull { mode ->
            abs(mode.refreshRate - contentFps) < 0.06
        }
        if (exactMode != null) {
            val formatted = String.format(java.util.Locale.US, "%.2f Hz (Exact Match)", exactMode.refreshRate)
            return AfrMatchResult(exactMode.modeId, exactMode.refreshRate, formatted, true)
        }

        // 2. Look for integer multiples (e.g. 24fps -> 48Hz, 72Hz, 120Hz, 144Hz)
        val integerMultipleModes = modes.filter { mode ->
            val ratio = mode.refreshRate / contentFps
            val nearestInt = ratio.roundToInt()
            nearestInt in 2..6 && abs(ratio - nearestInt) < 0.05
        }

        if (integerMultipleModes.isNotEmpty()) {
            // Prefer 120Hz or the best multiple
            val bestMultiple = integerMultipleModes.maxByOrNull { it.refreshRate } ?: integerMultipleModes.first()
            val multiple = (bestMultiple.refreshRate / contentFps).roundToInt()
            val formatted = String.format(
                java.util.Locale.US,
                "%.0f Hz (%d:%d Cinema Cadence)",
                bestMultiple.refreshRate,
                multiple,
                multiple
            )
            return AfrMatchResult(bestMultiple.modeId, bestMultiple.refreshRate, formatted, false)
        }

        // 3. Fallback to standard 60Hz if content is 30/60
        if (abs(contentFps - 29.97) < 0.1 || abs(contentFps - 30.0) < 0.1 || abs(contentFps - 59.94) < 0.1 || abs(contentFps - 60.0) < 0.1) {
            val sixtyMode = modes.firstOrNull { abs(it.refreshRate - 60f) < 0.1 }
            if (sixtyMode != null) {
                return AfrMatchResult(sixtyMode.modeId, sixtyMode.refreshRate, "60.00 Hz (Standard)", true)
            }
        }

        return null
    }

    /**
     * Applies the selected mode to the Activity window and configures Surface frame rate if API 30+.
     */
    fun applyDisplayMode(activity: Activity, match: AfrMatchResult) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.window.attributes = activity.window.attributes.apply {
                preferredDisplayModeId = match.modeId
            }
        }
    }

    /**
     * Sets frame rate compatibility directly on the playback Surface (Android 11+ / API 30+).
     */
    fun applySurfaceFrameRate(surface: Surface?, contentFps: Double) {
        if (surface == null || !surface.isValid) return
        if (contentFps.isFinite() && contentFps > 0.0) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    surface.setFrameRate(
                        contentFps.toFloat(),
                        Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                        Surface.CHANGE_FRAME_RATE_ALWAYS
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    surface.setFrameRate(
                        contentFps.toFloat(),
                        Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE
                    )
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Clears any forced surface frame rate.
     */
    fun clearSurfaceFrameRate(surface: Surface?) {
        if (surface == null || !surface.isValid) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                surface.setFrameRate(0f, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
            } catch (_: Exception) {}
        }
    }

    /**
     * Restores window to maximum device refresh rate for fluid UI navigation.
     */
    fun restoreMaxRefreshRate(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display
            } else {
                val wm = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                @Suppress("DEPRECATION")
                wm?.defaultDisplay
            }
            val maxMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
            activity.window.attributes = activity.window.attributes.apply {
                preferredDisplayModeId = maxMode?.modeId ?: 0
            }
        }
    }
}
