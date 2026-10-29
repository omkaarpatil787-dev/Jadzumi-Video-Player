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

    val fontOptions = listOf(
        "Roboto Sans",
        "Cinema Serif",
        "Heavy Sans",
        "Roboto Condensed",
        "Rounded Sans",
        "Roboto Monospace",
        "System Default"
    )

    val fontStyleOptions = listOf("Regular", "Bold", "Italic", "Bold Italic")

    val sizeOptions = listOf(
        0.75f to "Tiny (75%)",
        0.85f to "Small (85%)",
        1.0f to "Normal (100%)",
        1.20f to "Large (120%)",
        1.45f to "Extra Large (145%)",
        1.70f to "Cinema Huge (170%)"
    )

    val colorOptions = listOf(
        "White",
        "Yellow",
        "Amber Gold",
        "Cyan",
        "Green",
        "Lavender",
        "Soft Orange"
    )

    val backgroundOptions = listOf("None", "Semi-transparent", "Solid Black", "Rounded Pill Box")

    val edgeOptions = listOf(
        "Drop Shadow",
        "Uniform Outline",
        "Raised 3D",
        "Depressed 3D",
        "Soft Glow",
        "None"
    )

    val bottomMarginOptions = listOf(
        0.04f to "Low (4%)",
        0.08f to "Standard (8%)",
        0.14f to "High (14%)",
        0.20f to "Raised (20%)"
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSize(context: Context): Float = prefs(context).getFloat("size", 1.0f)
    fun setSize(context: Context, size: Float) = prefs(context).edit { putFloat("size", size) }

    fun getFont(context: Context): String = prefs(context).getString("font", "Roboto Sans") ?: "Roboto Sans"
    fun setFont(context: Context, font: String) = prefs(context).edit { putString("font", font) }

    fun getFontStyle(context: Context): String = prefs(context).getString("font_style", "Regular") ?: "Regular"
    fun setFontStyle(context: Context, style: String) = prefs(context).edit { putString("font_style", style) }

    fun getColor(context: Context): String = prefs(context).getString("color", "White") ?: "White"
    fun setColor(context: Context, color: String) = prefs(context).edit { putString("color", color) }

    fun getBackground(context: Context): String = prefs(context).getString("bg", "None") ?: "None"
    fun setBackground(context: Context, bg: String) = prefs(context).edit { putString("bg", bg) }

    fun getEdge(context: Context): String = prefs(context).getString("edge", "Drop Shadow") ?: "Drop Shadow"
    fun setEdge(context: Context, edge: String) = prefs(context).edit { putString("edge", edge) }

    fun getBottomMargin(context: Context): Float = prefs(context).getFloat("bottom_margin", 0.08f)
    fun setBottomMargin(context: Context, margin: Float) = prefs(context).edit { putFloat("bottom_margin", margin) }

    fun getTypeface(font: String, styleName: String = "Regular"): Typeface {
        val baseTypeface = when (font) {
            "Cinema Serif" -> Typeface.SERIF
            "Heavy Sans" -> Typeface.create("sans-serif-black", Typeface.BOLD)
            "Roboto Condensed" -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            "Rounded Sans" -> Typeface.create("sans-serif-rounded", Typeface.NORMAL)
            "Roboto Monospace" -> Typeface.MONOSPACE
            "System Default" -> Typeface.DEFAULT
            else -> Typeface.SANS_SERIF
        }
        val tfStyle = when (styleName) {
            "Bold" -> Typeface.BOLD
            "Italic" -> Typeface.ITALIC
            "Bold Italic" -> Typeface.BOLD_ITALIC
            else -> Typeface.NORMAL
        }
        return if (font == "Heavy Sans" && styleName == "Regular") baseTypeface else Typeface.create(baseTypeface, tfStyle)
    }

    fun getColorInt(color: String): Int = when (color) {
        "Yellow" -> Color.parseColor("#FFEB3B")
        "Amber Gold" -> Color.parseColor("#FFC107")
        "Cyan" -> Color.parseColor("#00E5FF")
        "Green" -> Color.parseColor("#69F0AE")
        "Lavender" -> Color.parseColor("#E1BEE7")
        "Soft Orange" -> Color.parseColor("#FFAB91")
        else -> Color.WHITE
    }

    fun getBackgroundInt(bg: String): Int = when (bg) {
        "Solid Black" -> Color.BLACK
        "Semi-transparent" -> Color.argb(160, 0, 0, 0)
        "Rounded Pill Box" -> Color.argb(130, 15, 23, 42)
        else -> Color.TRANSPARENT
    }

    fun getEdgeType(edge: String): Int = when (edge) {
        "Uniform Outline" -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
        "Raised 3D" -> CaptionStyleCompat.EDGE_TYPE_RAISED
        "Depressed 3D" -> CaptionStyleCompat.EDGE_TYPE_DEPRESSED
        "Soft Glow" -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
        "None" -> CaptionStyleCompat.EDGE_TYPE_NONE
        else -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
    }

    fun getEdgeColor(edge: String): Int = when (edge) {
        "Soft Glow" -> Color.argb(220, 139, 157, 255)
        "None" -> Color.TRANSPARENT
        else -> Color.BLACK
    }

    fun applyToPlayerView(context: Context, playerView: PlayerView) {
        val font = getFont(context)
        val fontStyle = getFontStyle(context)
        val color = getColor(context)
        val bg = getBackground(context)
        val edge = getEdge(context)
        val size = getSize(context)
        val bottomMargin = getBottomMargin(context)

        val style = CaptionStyleCompat(
            getColorInt(color),
            getBackgroundInt(bg),
            Color.TRANSPARENT,
            getEdgeType(edge),
            getEdgeColor(edge),
            getTypeface(font, fontStyle)
        )
        playerView.subtitleView?.apply {
            setStyle(style)
            setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * size)
            setBottomPaddingFraction(bottomMargin)
        }
    }
}
