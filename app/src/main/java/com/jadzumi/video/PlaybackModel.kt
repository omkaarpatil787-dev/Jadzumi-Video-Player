package com.jadzumi.video

import androidx.media3.common.C
import java.util.Locale
import kotlin.math.roundToInt

/** Unknown duration is represented as zero; do not turn it into a negative seek. */
fun boundedPosition(position: Long, duration: Long): Long =
    if (duration > 0) position.coerceIn(0, duration) else position.coerceAtLeast(0)

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

fun formatChannelLayout(channelCount: Int): String = when (channelCount) {
    1 -> "Mono (1.0)"
    2 -> "Stereo (2.0)"
    6 -> "5.1 Surround (6 Ch)"
    8 -> "7.1 Surround (8 Ch)"
    else -> if (channelCount > 0) "$channelCount Ch" else "Unknown layout"
}

fun formatAudioCodec(mimeType: String?): String = when (mimeType) {
    "audio/ac3" -> "Dolby Digital (AC-3)"
    "audio/eac3" -> "Dolby Digital Plus (E-AC-3)"
    "audio/eac3-joc" -> "Dolby Digital Plus (E-AC-3 JOC / Atmos)"
    "audio/mp4a-latm" -> "AAC Stereo"
    "audio/flac" -> "FLAC Lossless"
    "audio/vorbis" -> "Vorbis"
    "audio/opus" -> "Opus"
    "audio/true-hd" -> "Dolby TrueHD"
    "audio/vnd.dts" -> "DTS"
    "audio/vnd.dts.hd" -> "DTS-HD"
    else -> mimeType?.substringAfter('/')?.uppercase(Locale.ROOT)?.replace("EAC3", "E-AC-3") ?: "Audio"
}

fun formatSubtitleFormat(mimeType: String?): String = when (mimeType) {
    "text/x-ssa", "text/ssa", "application/x-ass" -> "Styled ASS/SSA"
    "application/x-subrip", "text/vtt" -> "SubRip (.srt / .vtt)"
    else -> "SubRip (.srt / .vtt)"
}

@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
fun isHdrFormat(colorTransfer: Int): Boolean =
    colorTransfer == C.COLOR_TRANSFER_ST2084 || colorTransfer == C.COLOR_TRANSFER_HLG

/** Formats duration in "2h 49m" or "54m" for the top info bar (Panel 1 & 4) */
fun formatCompactRuntime(durationMs: Long): String? {
    if (durationMs <= 0L) return null
    val totalMinutes = (durationMs / 60_000L).toInt()
    if (totalMinutes <= 0) return null
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

/** Compact timecode: "1:02:15" or "25:35" matching Panel 1 & 3 */
fun compactTimecode(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0)
    val h = safe / 3600
    val m = (safe / 60) % 60
    val s = safe % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.ROOT, "%02d:%02d", m, s)
}

data class CleanPlayerHeading(val primaryTitle: String, val secondaryMeta: String?)

private val epRegexSxxExx = Regex("(?i)(?:^|[ ._\\-])s(\\d{1,2})e(\\d{1,3})(?=$|[^0-9])")
private val epRegexNxNN = Regex("(?i)(?:^|[ ._\\-])(\\d{1,2})x(\\d{1,3})(?=$|[^0-9])")
private val yearRegex = Regex("(?:^|[ ._\\-(])(19\\d{2}|20\\d{2})(?=$|[ ._\\-)])")
private val techStopToken = Regex(
    "(?i)(?:^|[ ._\\-\\[(])(2160p|1080p|720p|480p|4k|uhd|webrip|web-dl|webdl|bluray|bdrip|hdrip|dvdrip|x264|x265|h264|h265|hevc|avc|10bit|8bit|hdr|hdr10|dv|ddp|aac|ac3|eac3|6ch|2ch|dual|multi|org|esub|subs|repack|proper)(?=$|[ ._\\-\\])])"
)

fun formatCleanPlayerTitle(
    rawName: String,
    width: Int = 0,
    height: Int = 0,
    durationMs: Long = 0L
): CleanPlayerHeading {
    val base = rawName.substringBeforeLast('.', rawName).trim()
    val epMatch = epRegexSxxExx.find(base) ?: epRegexNxNN.find(base)
    val yearMatch = yearRegex.find(base)
    val year = yearMatch?.groupValues?.getOrNull(1)
    val runtime = formatCompactRuntime(durationMs)

    val resLabel = when {
        width >= 3840 || height >= 2160 || base.contains("2160p", true) || base.contains("4k", true) -> "4K"
        width >= 1920 || height >= 1080 || base.contains("1080p", true) -> "1080p"
        width >= 1280 || height >= 720 || base.contains("720p", true) -> "720p"
        else -> null
    }

    if (epMatch != null) {
        val season = epMatch.groupValues[1].toIntOrNull() ?: 1
        val episode = epMatch.groupValues[2].toIntOrNull() ?: 1
        val code = String.format(Locale.ROOT, "S%02dE%02d", season, episode)
        val rawShow = base.substring(0, epMatch.range.first)
            .replace(Regex("\\(\\d{4}\\)"), " ")
            .replace(Regex("\\b(19\\d{2}|20\\d{2})\\b"), " ")
            .replace('.', ' ')
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', '·', '[', ']')
        val showTitle = rawShow.ifEmpty { base.replace('_', ' ') }
        val metaParts = listOfNotNull(year, runtime, resLabel)
        return CleanPlayerHeading(
            primaryTitle = "$showTitle · $code",
            secondaryMeta = metaParts.joinToString(" · ").ifEmpty { null }
        )
    }

    val stopIndex = listOfNotNull(
        yearMatch?.range?.first,
        techStopToken.find(base)?.range?.first
    ).minOrNull() ?: base.length

    val cleanedMovie = base.substring(0, stopIndex)
        .replace('.', ' ')
        .replace('_', ' ')
        .replace(Regex("\\s+"), " ")
        .trim(' ', '-', '·', '(', '[')
        .ifEmpty { base.replace('.', ' ').replace('_', ' ').trim() }

    val metaParts = listOfNotNull(year, runtime, resLabel)
    return CleanPlayerHeading(
        primaryTitle = cleanedMovie,
        secondaryMeta = metaParts.joinToString(" · ").ifEmpty { null }
    )
}

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
// END OF FILE
