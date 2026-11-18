package com.jadzumi.video

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * Material You / Monet Dynamic Poster Color Extractor
 * Deterministically samples the movie poster bitmap and extracts the dominant vibrant chromatic accent.
 */
object PosterMonetExtractor {
    private val colorCache = mutableMapOf<String, Color>()

    fun getColor(uri: String?, bitmap: Bitmap?): Color? {
        if (uri == null) return null
        synchronized(colorCache) {
            colorCache[uri]?.let { return it }
        }
        if (bitmap != null) {
            val extracted = extractDominantPosterColor(bitmap)
            if (extracted != null) {
                synchronized(colorCache) {
                    colorCache[uri] = extracted
                }
                return extracted
            }
        }
        return null
    }

    /**
     * Fast 144-point grid chroma sampler with saturation scoring and luminosity bounds.
     */
    fun extractDominantPosterColor(bitmap: Bitmap): Color? {
        if (bitmap.width <= 0 || bitmap.height <= 0) return null
        var bestColor: Color? = null
        var bestScore = -1f

        val stepX = (bitmap.width / 12).coerceAtLeast(1)
        val stepY = (bitmap.height / 12).coerceAtLeast(1)

        for (x in stepX until bitmap.width step stepX) {
            for (y in stepY until bitmap.height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val max = maxOf(r, g, b).toFloat()
                val min = minOf(r, g, b).toFloat()
                val l = (max + min) / (2f * 255f)
                val d = (max - min) / 255f
                val s = if (l > 0f && l < 1f) d / (1f - abs(2f * l - 1f)) else 0f

                // Filter out near-blacks (l < 0.16) and washed-out highlights (l > 0.85)
                if (l in 0.18f..0.85f && s > 0.22f) {
                    val score = s * 1.6f + (0.5f - abs(l - 0.5f))
                    if (score > bestScore) {
                        bestScore = score
                        bestColor = Color(pixel)
                    }
                }
            }
        }
        return bestColor
    }
}
