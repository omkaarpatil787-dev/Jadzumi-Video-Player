package com.jadzumi.video

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import androidx.core.content.edit
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
object SubtitlePreferences {
    private const val PREFS_NAME = "subtitle_prefs"

    val fontOptions = listOf("Roboto Sans", "Roboto Condensed", "Roboto Monospace", "System Default")
    val sizeOptions = listOf(0.80f to "Small (80%)", 1.0f to "Normal (100%)", 1.25f to "Large (125%)", 1.50f to "Extra Large (150%)")
    val colorOptions = listOf("White", "Yellow", "Cyan", "Green")
    val backgroundOptions = listOf("Semi-transparent", "Solid Black", "None")
    val edgeOptions = listOf("Drop Shadow", "Uniform Outline", "None")

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSize(context: Context): Float = prefs(context).getFloat("size", 1.0f)
    fun setSize(context: Context, size: Float) = prefs(context).edit { putFloat("size", size) }

    fun getFont(context: Context): String = prefs(context).getString("font", "Roboto Sans") ?: "Roboto Sans"
    fun setFont(context: Context, font: String) = prefs(context).edit { putString("font", font) }

    fun getColor(context: Context): String = prefs(context).getString("color", "White") ?: "White"
    fun setColor(context: Context, color: String) = prefs(context).edit { putString("color", color) }

    fun getBackground(context: Context): String = prefs(context).getString("bg", "Semi-transparent") ?: "Semi-transparent"
    fun setBackground(context: Context, bg: String) = prefs(context).edit { putString("bg", bg) }

    fun getEdge(context: Context): String = prefs(context).getString("edge", "Drop Shadow") ?: "Drop Shadow"
    fun setEdge(context: Context, edge: String) = prefs(context).edit { putString("edge", edge) }

    fun getTypeface(font: String): Typeface = when (font) {
        "Roboto Condensed" -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        "Roboto Monospace" -> Typeface.MONOSPACE
        "System Default" -> Typeface.DEFAULT
        else -> Typeface.SANS_SERIF
    }

    fun getColorInt(color: String): Int = when (color) {
        "Yellow" -> Color.parseColor("#FFEB3B")
        "Cyan" -> Color.parseColor("#00E5FF")
        "Green" -> Color.parseColor("#69F0AE")
        else -> Color.WHITE
    }

    fun getBackgroundInt(bg: String): Int = when (bg) {
        "Solid Black" -> Color.BLACK
        "None" -> Color.TRANSPARENT
        else -> Color.argb(160, 0, 0, 0)
    }

    fun getEdgeType(edge: String): Int = when (edge) {
        "Uniform Outline" -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
        "None" -> CaptionStyleCompat.EDGE_TYPE_NONE
        else -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
    }

    fun applyToPlayerView(context: Context, playerView: PlayerView) {
        val font = getFont(context)
        val color = getColor(context)
        val bg = getBackground(context)
        val edge = getEdge(context)
        val size = getSize(context)

        val style = CaptionStyleCompat(
            getColorInt(color),
            getBackgroundInt(bg),
            Color.TRANSPARENT,
            getEdgeType(edge),
            Color.BLACK,
            getTypeface(font)
        )
        playerView.subtitleView?.apply {
            setStyle(style)
            setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * size)
        }
    }
}
