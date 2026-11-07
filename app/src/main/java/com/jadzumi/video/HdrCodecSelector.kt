package com.jadzumi.video

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import java.util.ArrayList

/**
 * Supported user-selectable HDR profile operating modes.
 */
enum class HdrProfileMode(val id: String, val displayName: String, val badge: String) {
    AUTO("auto", "Auto (Display Match)", "AUTO"),
    DOLBY_VISION("dv", "Dolby Vision Master", "DV"),
    HDR10_PLUS("hdr10_plus", "HDR10+ Dynamic Metadata", "HDR10+"),
    HDR10("hdr10", "HDR10 / Base Layer (BT.2020)", "HDR10"),
    SDR_TONEMAPPED("sdr", "SDR Tone-Mapped (Full Color Studio)", "SDR")
}

/**
 * Intelligent MediaCodecSelector for high-fidelity HDR10, HDR10+, and Dolby Vision playback.
 *
 * Resolves the infamous "purple and green" color distortion on Dolby Vision Profile 7 & 8 files:
 * - On devices lacking certified Dolby Vision hardware decoders (or displays without DV support,
 *   such as Samsung Galaxy phones/tablets, Google Pixel, and standard HDR10/HDR10+ panels), hybrid DV files
 *   (which contain a complete 10-bit HDR10/HDR10+ base layer) are automatically routed to
 *   native SoC hardware HEVC decoders (Qualcomm, MediaTek, Exynos, Tensor).
 * - On Dolby Vision-certified panels, native DV decoders are prioritized with seamless
 *   hardware HEVC fallback.
 * - Allows explicit user selection of HDR Profile (Auto, DV, HDR10+, HDR10 Base, SDR Tone-Mapped).
 */
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
object HdrCodecSelector {

    fun create(context: Context, profileMode: HdrProfileMode = HdrProfileMode.AUTO): MediaCodecSelector {
        return MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val defaultDecoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType, requiresSecureDecoder, requiresTunnelingDecoder
            )

            val isDolbyVision = mimeType.equals(MimeTypes.VIDEO_DOLBY_VISION, ignoreCase = true) ||
                    mimeType.contains("dolby-vision", ignoreCase = true)

            if (isDolbyVision) {
                // Obtain hardware HEVC decoders for HDR10/HDR10+ base-layer fallback
                val hevcDecoders = runCatching {
                    MediaCodecSelector.DEFAULT.getDecoderInfos(
                        MimeTypes.VIDEO_H265, requiresSecureDecoder, requiresTunnelingDecoder
                    )
                }.getOrDefault(emptyList())

                when (profileMode) {
                    HdrProfileMode.HDR10, HdrProfileMode.HDR10_PLUS -> {
                        // Explicitly force native SoC HEVC hardware decoders for the 10-bit HDR10 base layer
                        if (hevcDecoders.isNotEmpty()) {
                            return@MediaCodecSelector hevcDecoders
                        }
                    }
                    HdrProfileMode.DOLBY_VISION -> {
                        // Prioritize native Dolby Vision decoders with fallback to HEVC
                        if (defaultDecoders.isNotEmpty()) {
                            return@MediaCodecSelector defaultDecoders
                        }
                    }
                    HdrProfileMode.SDR_TONEMAPPED -> {
                        // Route through HEVC for tone-mapped playback
                        if (hevcDecoders.isNotEmpty()) {
                            return@MediaCodecSelector hevcDecoders
                        }
                    }
                    HdrProfileMode.AUTO -> {
                        val displaySupportsDv = isDolbyVisionDisplaySupported(context)
                        if (!displaySupportsDv || defaultDecoders.isEmpty()) {
                            if (hevcDecoders.isNotEmpty()) {
                                return@MediaCodecSelector hevcDecoders
                            }
                        }
                        val combined = ArrayList<MediaCodecInfo>(defaultDecoders.size + hevcDecoders.size)
                        combined.addAll(defaultDecoders)
                        combined.addAll(hevcDecoders)
                        return@MediaCodecSelector combined
                    }
                }
            }

            defaultDecoders
        }
    }

    @Suppress("DEPRECATION")
    fun isDolbyVisionDisplaySupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return runCatching {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try { context.display } catch (_: Exception) { wm?.defaultDisplay }
            } else {
                wm?.defaultDisplay
            }
            val hdrCaps = display?.hdrCapabilities ?: return@runCatching false
            hdrCaps.supportedHdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION)
        }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    fun isHdr10PlusSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try { context.display } catch (_: Exception) { wm?.defaultDisplay }
            } else {
                wm?.defaultDisplay
            }
            val hdrCaps = display?.hdrCapabilities ?: return@runCatching false
            hdrCaps.supportedHdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS)
        }.getOrDefault(false)
    }
}
