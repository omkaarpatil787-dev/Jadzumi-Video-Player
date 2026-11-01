package com.jadzumi.video

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.*
import kotlin.math.roundToInt

data class BatteryCinemaStatus(
    val percentage: Int,
    val isCharging: Boolean,
    val estimatedMinutesLeft: Int,
    val isEcoActive: Boolean
) {
    val formattedRemaining: String get() {
        if (isCharging) return "Charging (Unlimited)"
        val hours = estimatedMinutesLeft / 60
        val mins = estimatedMinutesLeft % 60
        return if (hours > 0) "~${hours}h ${mins}m left" else "~${mins}m left"
    }

    val episodesLeftDescription: String get() {
        if (isCharging) return "Power connected"
        val episodes = (estimatedMinutesLeft / 45.0).coerceAtLeast(0.1)
        return String.format(java.util.Locale.US, "≈ %.1f episodes", episodes)
    }
}

class CinemaPowerManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("jadzumi_power", Context.MODE_PRIVATE)

    var isEcoModeEnabled by mutableStateOf(prefs.getBoolean("eco_mode", false))
        private set

    var batteryLevel by mutableIntStateOf(100)
        private set

    var isCharging by mutableStateOf(false)
        private set

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                if (level >= 0 && scale > 0) {
                    batteryLevel = ((level.toFloat() / scale.toFloat()) * 100f).roundToInt()
                }
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
    }

    init {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initial = context.registerReceiver(receiver, filter)
        if (initial != null) {
            val level = initial.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = initial.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = initial.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            if (level >= 0 && scale > 0) {
                batteryLevel = ((level.toFloat() / scale.toFloat()) * 100f).roundToInt()
            }
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }
    }

    fun toggleEcoMode() {
        isEcoModeEnabled = !isEcoModeEnabled
        prefs.edit().putBoolean("eco_mode", isEcoModeEnabled).apply()
    }

    fun getStatus(): BatteryCinemaStatus {
        // Average drain: ~14% per hour normal, ~10% per hour in Eco mode
        val hourlyDrainPct = if (isEcoModeEnabled) 10f else 14f
        val remainingHours = (batteryLevel / hourlyDrainPct).coerceAtLeast(0.1f)
        val minutesLeft = (remainingHours * 60f).roundToInt()

        return BatteryCinemaStatus(
            percentage = batteryLevel,
            isCharging = isCharging,
            estimatedMinutesLeft = minutesLeft,
            isEcoActive = isEcoModeEnabled
        )
    }

    fun release() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }
}
