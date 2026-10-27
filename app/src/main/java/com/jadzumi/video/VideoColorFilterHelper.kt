package com.jadzumi.video

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RenderEffect
import android.os.Build
import android.view.View

/**
 * GPU-accelerated video color grading and equalizer helper.
 * Applies real-time Contrast, Shadow/Brightness lift, Vibrance/Saturation, and AMOLED true-black filters
 * directly on the RenderThread via hardware ColorMatrix and RenderEffect without decoding stalls.
 */
object VideoColorFilterHelper {

    fun buildColorMatrix(contrast: Float, brightness: Float, saturation: Float, amoled: Boolean): ColorMatrix? {
        val isDefault = contrast == 1.0f && brightness == 0.0f && saturation == 1.0f && !amoled
        if (isDefault) return null

        val matrix = ColorMatrix()

        // 1. Saturation / Vibrance adjustment using ITU luminance coefficients
        matrix.setSaturation(saturation)

        // 2. Contrast & Shadow/Brightness Lift
        val c = if (amoled) contrast * 1.08f else contrast
        val bOffset = (brightness * 255f) + if (amoled) -16f else 0f
        val t = 128f * (1.0f - c) + bOffset

        val cbMatrix = ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(cbMatrix)
        return matrix
    }

    fun applyToView(targetView: View?, contrast: Float, brightness: Float, saturation: Float, amoled: Boolean) {
        val view = targetView ?: return
        val matrix = buildColorMatrix(contrast, brightness, saturation, amoled)
        if (matrix == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                view.setRenderEffect(null)
            } else {
                view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }
        } else {
            val filter = ColorMatrixColorFilter(matrix)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                view.setRenderEffect(RenderEffect.createColorFilterEffect(filter))
            } else {
                val paint = Paint().apply { colorFilter = filter }
                view.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
            }
        }
    }
}
