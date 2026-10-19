package com.jadzumi.video

import androidx.media3.common.C
import java.util.Locale
import kotlin.math.roundToInt

/** Unknown duration is represented as zero; do not turn it into a negative seek. */
fun boundedPosition(position: Long, duration: Long): Long = if (duration > 0) position.coerceIn(0, duration) else position.coerceAtLeast(0)
fun resumePosition(position: Long, duration: Long, completed: Boolean): Long =
    if (completed) 0 else if (duration > 0) position.coerceIn(0, duration - 1) else position.coerceAtLeast(0)

fun seekPosition(position: Long, delta: Long, duration: Long): Long {
    val target = when {
        delta > 0 && position > Long.MAX_VALUE - delta -> Long.MAX_VALUE
        delta < 0 && position < Long.MIN_VALUE - delta -> Long.MIN_VALUE
        else -> position + delta
    }
    return boundedPosition(target, duration)
}

data class VideoContentBounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

fun videoContentBounds(width: Int, height: Int, aspect: Float, fit: Boolean): VideoContentBounds {
    val w = width.coerceAtLeast(0)
    val h = height.coerceAtLeast(0)
    if (!fit || w == 0 || h == 0 || !aspect.isFinite() || aspect <= 0f) return VideoContentBounds(0, 0, w, h)
    val ratio = aspect.toDouble()
    val contentWidth = if (w.toDouble() / h > ratio) (h * ratio).roundToInt().coerceIn(1, w) else w
    val contentHeight = if (w.toDouble() / h > ratio) h else (w / ratio).roundToInt().coerceIn(1, h)
    val left = (w - contentWidth) / 2
    val top = (h - contentHeight) / 2
    return VideoContentBounds(left, top, left + contentWidth, top + contentHeight)
}

/** Formatted channel description for Xiaomi Pad 5 downmixing. */
fun formatChannelLayout(channelCount: Int): String = when (channelCount) {
    1 -> "Mono (1.0)"
    2 -> "Stereo (2.0)"
    6 -> "5.1 Surround (6 Ch)"
    8 -> "7.1 Surround (8 Ch)"
    else -> if (channelCount > 0) "$channelCount Channels" else "Unknown layout"
}

/** Pretty-printed audio codec name. */
fun formatAudioCodec(mimeType: String?): String = when (mimeType) {
    "audio/ac3" -> "Dolby Digital (AC-3)"
    "audio/eac3" -> "Dolby Digital Plus (E-AC-3)"
    "audio/eac3-joc" -> "Dolby Atmos (E-AC-3 JOC)"
    "audio/mp4a-latm" -> "AAC Stereo"
    "audio/flac" -> "FLAC Lossless"
    "audio/vorbis" -> "Ogg Vorbis"
    "audio/opus" -> "Opus"
    "audio/true-hd" -> "Dolby TrueHD"
    else -> mimeType?.substringAfter('/')?.uppercase(Locale.ROOT) ?: "Audio Stream"
}

/** Pretty-printed subtitle format name. */
fun formatSubtitleFormat(mimeType: String?): String = when (mimeType) {
    "text/x-ssa", "text/ssa", "application/x-ass" -> "Styled ASS/SSA"
    "application/x-subrip", "text/vtt" -> "SubRip (.srt / .vtt)"
    else -> "Text Subtitle"
}

/** Detects HDR10/HLG color transfer characteristics. */
fun isHdrFormat(colorTransfer: Int): Boolean =
    colorTransfer == C.COLOR_TRANSFER_ST2084 || colorTransfer == C.COLOR_TRANSFER_HLG

data class AudioTrackInfo(
    val id: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val codec: String,
    val channels: String,
    val isSelected: Boolean
)

data class SubtitleTrackInfo(
    val id: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val format: String,
    val isSelected: Boolean
)
