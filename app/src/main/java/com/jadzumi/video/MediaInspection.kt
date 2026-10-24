package com.jadzumi.video

import java.util.Locale

data class VideoInspection(
    val mime: String? = null, val width: Int? = null, val height: Int? = null,
    val frameRate: Double? = null, val colorStandard: Int? = null,
    val colorTransfer: Int? = null, val colorRange: Int? = null, val bitDepth: Int? = null
)

data class AudioInspection(val mime: String?, val channels: Int?, val sampleRate: Int?, val language: String? = null)
data class SubtitleInspection(val mime: String, val language: String? = null)
data class MediaInspection(
    val video: VideoInspection? = null,
    val audio: List<AudioInspection> = emptyList(),
    val subtitles: List<SubtitleInspection> = emptyList(),
    val tracksRead: Boolean = false,
    val note: String? = null
)
enum class DecoderHardware { HARDWARE, SOFTWARE, NOT_REPORTED }
data class DecoderInspection(val name: String, val hardware: DecoderHardware = DecoderHardware.NOT_REPORTED)

fun decoderForInspectedFile(fileUri: String?, playingUri: String?, decoder: DecoderInspection?): DecoderInspection? =
    decoder.takeIf { fileUri != null && fileUri == playingUri }

fun mergeVideoInspection(live: VideoInspection?, file: VideoInspection?): VideoInspection? = when {
    live == null -> file
    file == null -> live
    else -> VideoInspection(live.mime ?: file.mime, live.width ?: file.width, live.height ?: file.height,
        live.frameRate ?: file.frameRate, live.colorStandard ?: file.colorStandard,
        live.colorTransfer ?: file.colorTransfer, live.colorRange ?: file.colorRange, live.bitDepth ?: file.bitDepth)
}

fun inspectionFrameRate(value: Double?): String? = value?.takeIf { it.isFinite() && it > 0 }
    ?.let { String.format(Locale.US, "%.3f", it).trimEnd('0').trimEnd('.') + " fps" }

fun inspectionColor(info: VideoInspection?): String? = listOfNotNull(
    when (info?.colorStandard) { 1 -> "BT.709"; 2, 4 -> "BT.601"; 6 -> "BT.2020"; else -> null },
    when (info?.colorTransfer) { 1 -> "Linear"; 3 -> "SDR"; 6 -> "PQ / ST 2084"; 7 -> "HLG"; else -> null },
    when (info?.colorRange) { 1 -> "Full range"; 2 -> "Limited range"; else -> null }
).takeIf { it.isNotEmpty() }?.joinToString(" · ")

fun inspectionVideoCodec(mime: String?): String? = when (mime) {
    null, "" -> null
    "video/avc" -> "H.264 / AVC"
    "video/hevc" -> "H.265 / HEVC"
    "video/av01" -> "AV1"
    "video/x-vnd.on2.vp9" -> "VP9"
    "video/x-vnd.on2.vp8" -> "VP8"
    else -> mime
}

fun inspectionAudio(info: AudioInspection): String = listOfNotNull(
    info.mime?.let { if (it == "audio/mp4a-latm") "AAC" else formatAudioCodec(it) },
    info.channels?.takeIf { it > 0 }?.let(::formatChannelLayout),
    info.sampleRate?.takeIf { it > 0 }?.let { "$it Hz" },
    info.language?.takeIf { it.isNotBlank() && it != "und" }
).joinToString(" · ").ifBlank { "Track details not reported" }

fun inspectionSubtitle(info: SubtitleInspection): String {
    val format = when (info.mime) {
        "application/x-subrip" -> "SRT"
        "text/vtt" -> "WebVTT"
        "text/x-ssa", "text/ssa", "application/x-ass" -> "ASS / SSA"
        "application/ttml+xml" -> "TTML"
        "application/pgs" -> "PGS"
        "application/dvbsubs" -> "DVB subtitles"
        else -> info.mime
    }
    return listOfNotNull(format, info.language?.takeIf { it.isNotBlank() && it != "und" }).joinToString(" · ")
}
