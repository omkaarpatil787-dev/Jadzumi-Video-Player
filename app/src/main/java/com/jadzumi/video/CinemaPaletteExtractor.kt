package com.jadzumi.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ColorSwatch(
    val color: Color,
    val hexCode: String,
    val label: String
)

data class CinemaPaletteResult(
    val title: String,
    val frameBitmap: Bitmap?,
    val swatches: List<ColorSwatch>
)

/**
 * Cinephile Color Palette & Aesthetic Frame Extractor.
 *
 * Samples video frame stills to extract a 5-swatch aesthetic color harmony strip
 * complete with hex values for cinema design inspiration and social sharing.
 */
object CinemaPaletteExtractor {

    suspend fun extractFromVideo(context: Context, video: LocalVideo, positionMs: Long): CinemaPaletteResult {
        return withContext(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, Uri.parse(video.uri))
                bitmap = retriever.getFrameAtTime(positionMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retriever.release()
            } catch (_: Exception) {}

            val swatches = if (bitmap != null) {
                sampleSwatchesFromBitmap(bitmap)
            } else {
                defaultFallbackSwatches()
            }

            CinemaPaletteResult(
                title = video.title.ifBlank { video.name },
                frameBitmap = bitmap,
                swatches = swatches
            )
        }
    }

    private fun sampleSwatchesFromBitmap(bitmap: Bitmap): List<ColorSwatch> {
        val scaled = Bitmap.createScaledBitmap(bitmap, 48, 48, false)
        val colorCounts = mutableMapOf<Int, Int>()

        for (x in 0 until scaled.width step 2) {
            for (y in 0 until scaled.height step 2) {
                val pixel = scaled.getPixel(x, y)
                // Quantize to reduce near-duplicate shades
                val r = (pixel shr 16 and 0xFF) / 16 * 16
                val g = (pixel shr 8 and 0xFF) / 16 * 16
                val b = (pixel and 0xFF) / 16 * 16
                val quantized = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                colorCounts[quantized] = (colorCounts[quantized] ?: 0) + 1
            }
        }

        val topColors = colorCounts.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .distinct()
            .take(5)

        val labels = listOf("Dominant", "Vibrant", "Accent", "Muted", "Deep Shadow")

        return topColors.mapIndexed { index, colorInt ->
            val color = Color(colorInt)
            val hex = String.format(java.util.Locale.US, "#%06X", 0xFFFFFF and colorInt)
            val label = labels.getOrElse(index) { "Tone ${index + 1}" }
            ColorSwatch(color, hex, label)
        }.ifEmpty { defaultFallbackSwatches() }
    }

    private fun defaultFallbackSwatches(): List<ColorSwatch> {
        return listOf(
            ColorSwatch(Color(0xFF0F172A), "#0F172A", "Deep Night"),
            ColorSwatch(Color(0xFF1E293B), "#1E293B", "Slate Shadow"),
            ColorSwatch(Color(0xFF3B82F6), "#3B82F6", "Cinema Blue"),
            ColorSwatch(Color(0xFF60A5FA), "#60A5FA", "Cyan Highlight"),
            ColorSwatch(Color(0xFFF1F5F9), "#F1F5F9", "Specular Light")
        )
    }
}
