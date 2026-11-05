package com.jadzumi.video

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbMatrix

/**
 * GPU-accelerated video color enhancement filter for Media3/ExoPlayer.
 *
 * Implements [RgbMatrix] to execute color transformations directly within
 * the hardware OpenGL ES video rendering pipeline of ExoPlayer.
 *
 * Preserves hardware decoding and renders to [android.view.SurfaceView].
 * Subtitles, gestures, and HUD overlays reside on separate window layers
 * and remain completely unaffected by color transformations.
 */
@OptIn(UnstableApi::class)
class VideoEnhancerEffect(
    val params: ColorStudioParams
) : RgbMatrix {

    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
        // If HDR is active, strictly return the 4x4 identity matrix to preserve
        // 10-bit wide color gamut (BT.2020 / PQ / HLG) without SDR clipping.
        if (useHdr || params.isNeutral()) {
            return IDENTITY_MATRIX.clone()
        }

        return computeTransformationMatrix(params)
    }

    companion object {
        private val IDENTITY_MATRIX = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )

        // ITU-R BT.709 standard luminance coefficients for perceptual color weighting
        private const val LUMA_R = 0.2126f
        private const val LUMA_G = 0.7152f
        private const val LUMA_B = 0.0722f

        /**
         * Computes the composite 4x4 column-major color transformation matrix.
         * Combines Saturation/Vibrance, Contrast around middle-gray (0.5),
         * Shadow Lift (gamma-safe tonal lift), and AMOLED pitch-black thresholding.
         */
        fun computeTransformationMatrix(params: ColorStudioParams): FloatArray {
            val (contrast, shadowLift, saturation, vibrance, amoled) = params

            // 1. Effective saturation incorporating selective vibrance
            val effectiveSaturation = (saturation + vibrance * 0.5f).coerceIn(0.0f, 2.5f)

            // Saturation matrix coefficients
            val invSat = 1.0f - effectiveSaturation
            val sR = invSat * LUMA_R
            val sG = invSat * LUMA_G
            val sB = invSat * LUMA_B

            // 2. Contrast & Shadow Lift
            // Contrast pivots around 0.5: out = c * (in - 0.5) + 0.5 = c * in + 0.5 * (1 - c)
            val c = contrast.coerceIn(0.6f, 1.6f)
            val contrastOffset = 0.5f * (1.0f - c)

            // Shadow lift provides tonal lift in dark scenes without blowing out highlights
            val liftOffset = (shadowLift * 0.18f).coerceIn(-0.10f, 0.15f)

            // AMOLED true black applies a gentle negative offset to clamp low-level near-black noise
            val amoledOffset = if (amoled) -0.035f else 0.0f

            val totalBias = contrastOffset + liftOffset + amoledOffset

            // Compose Saturation and Contrast: M = ContrastMatrix * SaturationMatrix
            // Row 0
            val m00 = c * (sR + effectiveSaturation)
            val m01 = c * sG
            val m02 = c * sB
            val m03 = 0f

            // Row 1
            val m10 = c * sR
            val m11 = c * (sG + effectiveSaturation)
            val m12 = c * sB
            val m13 = 0f

            // Row 2
            val m20 = c * sR
            val m21 = c * sG
            val m22 = c * (sB + effectiveSaturation)
            val m23 = 0f

            // Row 3 (Offset/Translation in 4x4 affine matrix)
            // In Android/OpenGL 4x4 matrix for color [R, G, B, 1]^T:
            // Out = M * [R, G, B, 1] where column 3 holds the translation [totalBias, totalBias, totalBias, 1]
            val m30 = totalBias
            val m31 = totalBias
            val m32 = totalBias
            val m33 = 1.0f

            // Return in 4x4 format
            return floatArrayOf(
                m00, m10, m20, 0f,
                m01, m11, m21, 0f,
                m02, m12, m22, 0f,
                m30, m31, m32, m33
            )
        }
    }
}
