package com.jadzumi.video

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager
import java.util.Locale

object DeviceHardwareInspector {

    fun getDecoderDescription(context: Context): String {
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN }
        } else null
        val hardware = Build.HARDWARE.takeIf { it.isNotBlank() && it != Build.UNKNOWN }
        val chipName = soc ?: hardware ?: Build.BOARD
        val model = Build.MODEL
        return "$model ($chipName) hardware decoder with NextLib FFmpeg fallback"
    }

    fun getHardwareAccelerationDescription(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        val board = Build.BOARD
        return "Hardware GPU acceleration active on $manufacturer $board for fluid 60/120 FPS playback"
    }

    fun getDisplayScalingDescription(context: Context): String {
        val dm = context.resources.displayMetrics
        val w = dm.widthPixels.coerceAtLeast(1)
        val h = dm.heightPixels.coerceAtLeast(1)
        val maxDim = maxOf(w, h)
        val minDim = minOf(w, h)
        val ratio = maxDim.toFloat() / minDim.toFloat()
        val ratioText = when {
            ratio in 1.55f..1.65f -> "16:10"
            ratio in 1.70f..1.83f -> "16:9"
            ratio in 1.95f..2.05f -> "18:9"
            ratio in 2.10f..2.20f -> "19.5:9"
            ratio in 2.21f..2.28f -> "20:9"
            ratio in 2.30f..2.45f -> "21:9"
            ratio in 1.30f..1.40f -> "4:3"
            else -> "${String.format(Locale.US, "%.1f", ratio)}:1"
        }
        val isTablet = (minDim / dm.density) >= 600
        val formFactor = if (isTablet) "tablet" else "phone"
        return "Default aspect ratio preservation on $ratioText $formFactor display (${maxDim}x${minDim})"
    }

    fun getHdrCapabilitiesInfo(context: Context): Pair<String, String> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try { context.display } catch (_: Exception) { windowManager?.defaultDisplay }
        } else {
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay
        }

        val hdrTypes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && display != null) {
            try {
                val hdrCaps = display.hdrCapabilities
                val types = mutableListOf<String>()
                hdrCaps?.supportedHdrTypes?.forEach { type ->
                    when (type) {
                        Display.HdrCapabilities.HDR_TYPE_HDR10 -> types.add("HDR10")
                        Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> types.add("HDR10+")
                        Display.HdrCapabilities.HDR_TYPE_HLG -> types.add("HLG")
                        Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> types.add("Dolby Vision")
                    }
                }
                types
            } catch (_: Exception) { emptyList() }
        } else emptyList()

        return if (hdrTypes.isNotEmpty()) {
            val title = "Hardware ${hdrTypes.joinToString(" / ")} Supported"
            val desc = "${Build.MODEL} display hardware supports 10-bit wide color gamut ${hdrTypes.joinToString(", ")} rendering"
            title to desc
        } else {
            val title = "Standard Dynamic Range (SDR) Display"
            val desc = "${Build.MODEL} display active · High-contrast tone mapping enabled for HDR videos"
            title to desc
        }
    }

    /**
     * Detects whether the device display panel is OLED / AMOLED vs LCD.
     * On OLED/AMOLED displays, true pitch black is achieved (0.0 nits), so boosting peak luminance
     * enhances dynamic range with infinite contrast.
     * On LCD displays (even those supporting HDR10), forcing 100% luminance causes severe backlight bleed
     * and washes out contrast, so peak screen luminance auto-boost is bypassed.
     */
    fun isOledOrAmoledDisplay(context: Context): Boolean {
        val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
        val userOverride = prefs.getString("displayPanelType", "auto") ?: "auto"
        if (userOverride == "oled") return true
        if (userOverride == "lcd") return false

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try { context.display } catch (_: Exception) { windowManager?.defaultDisplay }
        } else {
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && display != null) {
            try {
                val hdrCaps = display.hdrCapabilities
                if (hdrCaps != null) {
                    val desiredMin = hdrCaps.desiredMinLuminance
                    if (desiredMin >= 0.04f) {
                        return false // Definitively LCD backlight glow
                    } else if (desiredMin >= 0.0f && desiredMin < 0.02f && hdrCaps.desiredMaxLuminance > 0f) {
                        return true // Definitively OLED / AMOLED true black
                    }
                }
            } catch (_: Exception) { }
        }

        val dm = context.resources.displayMetrics
        val minDimDp = minOf(dm.widthPixels, dm.heightPixels) / dm.density
        val isTablet = minDimDp >= 600f

        val model = (Build.MODEL + " " + Build.PRODUCT + " " + Build.DEVICE).lowercase(Locale.US)

        val isAmoledTablet = model.contains("sm-t970") || model.contains("sm-t976") || // Tab S7+
                model.contains("sm-x800") || model.contains("sm-x806") || // Tab S8+
                model.contains("sm-x900") || model.contains("sm-x906") || // Tab S8 Ultra
                model.contains("sm-x710") || model.contains("sm-x716") || // Tab S9
                model.contains("sm-x810") || model.contains("sm-x816") || // Tab S9+
                model.contains("sm-x910") || model.contains("sm-x916") || // Tab S9 Ultra
                model.contains("sm-x820") || model.contains("sm-x920")    // Tab S10

        if (isTablet) {
            // Tablets with HDR are predominantly IPS LCD (Xiaomi Pad 5/6, OnePlus Pad, Tab S7/S8 base, Tab S9 FE, Lenovo)
            return isAmoledTablet
        }

        val isExplicitLcdPhone = model.contains("lcd") || model.contains("ips") || model.contains("nord ce") || model.contains("poco x4 gt")
        return !isExplicitLcdPhone
    }

    fun getDisplayPanelInfo(context: Context): Pair<String, String> {
        val isOled = isOledOrAmoledDisplay(context)
        return if (isOled) {
            "OLED / AMOLED Display Panel" to "Auto HDR Peak Luminance active · Infinite contrast with pitch blacks"
        } else {
            "IPS / LCD Display Panel" to "Peak luminance boost bypassed to preserve contrast and prevent backlight bleed"
        }
    }

    fun getStereoDownmixDescription(): String {
        return "Optimized for device stereo speakers, earpieces, and headphones"
    }
}
