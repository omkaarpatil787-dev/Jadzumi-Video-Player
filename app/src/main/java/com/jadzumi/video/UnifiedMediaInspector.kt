package com.jadzumi.video

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun UnifiedMediaInspectorCard(
    video: LocalVideo,
    state: LocalLibraryState,
    playback: PlaybackController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var refresh by remember(video.uri) { mutableIntStateOf(0) }
    val scan by rememberLocalInspection(video, refresh)
    val sameFile = video.uri == playback.video?.uri
    val info = mergeVideoInspection(if (sameFile) playback.runtimeVideoFormat else null, scan.details?.video)
    val videoDecoder = decoderForInspectedFile(video.uri, playback.video?.uri, playback.runtimeVideoDecoder)
    val audioDecoder = decoderForInspectedFile(video.uri, playback.video?.uri, playback.runtimeAudioDecoder)

    val width = info?.width ?: video.width.takeIf { it > 0 }
    val height = info?.height ?: video.height.takeIf { it > 0 }
    val audio = (if (sameFile) playback.runtimeAudioTracks.takeIf { it.isNotEmpty() }
        ?: playback.runtimeAudioFormat?.let { listOf(it) } else null) ?: scan.details?.audio.orEmpty()
    val subtitles = (if (sameFile) playback.runtimeSubtitleTracks.takeIf { it.isNotEmpty() } else null)
        ?: scan.details?.subtitles.orEmpty()

    val theme = LocalThemeName.current
    val isLight = theme == "Light"
    val cardBg = MaterialExpressive.cardBg(theme)
    val cardBorder = MaterialExpressive.cardBorder(theme)
    val cardElevation = MaterialExpressive.cardElevation(theme)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = cardBorder,
        shadowElevation = cardElevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title, Quality Chip & Refresh Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.Analytics,
                        contentDescription = null,
                        tint = LavenderAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Media Inspector",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quality = when {
                        width != null && width >= 3840 -> "4K UHD"
                        width != null && width >= 1920 -> "1080p FHD"
                        width != null && width >= 1280 -> "720p HD"
                        else -> "Standard Def"
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = LavenderAccent.copy(alpha = 0.15f),
                        border = BorderStroke(0.75.dp, LavenderAccent.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = quality,
                            color = LavenderAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    if (!scan.loading) {
                        IconButton(
                            onClick = { refresh++ },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = "Refresh inspection",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (scan.loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = LavenderAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // SECTION 1: VIDEO STREAM
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Videocam, null, tint = LavenderAccent, modifier = Modifier.size(14.dp))
                    Text(
                        "VIDEO STREAM",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = LavenderAccent,
                        letterSpacing = 0.5.sp
                    )
                }

                val resText = if (width != null && height != null) "$width × $height" else "Auto Detected"
                InspectorSpecRow(label = "Resolution", value = resText)

                val codecText = inspectionVideoCodec(info?.mime)
                    ?: if (video.name.contains("265", true) || video.name.contains("hevc", true)) "HEVC / H.265"
                    else if (video.name.contains("264", true) || video.name.contains("avc", true)) "AVC / H.264"
                    else "H.264 / HEVC Decoded"
                InspectorSpecRow(label = "Codec", value = codecText)

                val hwStatus = when (videoDecoder?.hardware) {
                    DecoderHardware.HARDWARE -> "Hardware (GPU)"
                    DecoderHardware.SOFTWARE -> "Software Fallback"
                    else -> "Hardware Accelerated"
                }
                InspectorSpecRow(label = "Decoder Pipeline", value = hwStatus, valueColor = Color(0xFF34D399))

                val hdrText = if (playback.isHdr) (playback.hdrFlavor ?: "HDR10+") else "BT.709 SDR"
                InspectorSpecRow(label = "Color & Dynamic Range", value = hdrText)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.75.dp)

            // SECTION 2: AUDIO & ACOUSTICS
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Audiotrack, null, tint = TimelineAccent, modifier = Modifier.size(14.dp))
                    Text(
                        "AUDIO & ACOUSTICS",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TimelineAccent,
                        letterSpacing = 0.5.sp
                    )
                }

                val primaryAudio = audio.firstOrNull()
                val audioCodec = when {
                    primaryAudio != null -> inspectionAudio(primaryAudio).substringBefore(" ·").ifEmpty { "Multichannel" }
                    video.name.contains("DDP", true) || video.name.contains("EAC3", true) -> "E-AC-3 Surround"
                    video.name.contains("AAC", true) -> "AAC Stereo / 5.1"
                    video.name.contains("DTS", true) -> "DTS-HD Audio"
                    video.name.contains("TrueHD", true) -> "Dolby TrueHD"
                    else -> "NextLib FFmpeg"
                }
                val channelsText = when {
                    primaryAudio?.channels != null && primaryAudio.channels > 2 -> "${primaryAudio.channels}CH Surround"
                    video.name.contains("6CH", true) || video.name.contains("5.1", true) -> "5.1 Surround (6CH)"
                    video.name.contains("7.1", true) -> "7.1 Surround (8CH)"
                    else -> "Stereo (2CH)"
                }
                InspectorSpecRow(label = "Format & Channels", value = "$audioCodec · $channelsText")

                val trackCountStr = if (audio.isNotEmpty()) "${audio.size} Audio Stream(s)" else "Single Track"
                InspectorSpecRow(label = "Streams Detected", value = trackCountStr)
                InspectorSpecRow(label = "Downmix Routing", value = "Auto Downmix Ready")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.75.dp)

            // SECTION 3: SUBTITLE STUDIO
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.ClosedCaption, null, tint = LavenderAccent, modifier = Modifier.size(14.dp))
                    Text(
                        "SUBTITLE STUDIO",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = LavenderAccent,
                        letterSpacing = 0.5.sp
                    )
                }

                val subCount = subtitles.size
                val subTitle = if (subCount > 0) "$subCount Track(s) Detected" else "Subtitles Ready"
                InspectorSpecRow(label = "Tracks Detected", value = subTitle)
                InspectorSpecRow(label = "Supported Formats", value = "ASS · SSA · SRT · VTT")
                val dualSubReady = if (playback.dualSubtitleStudio.isSecondaryEnabled) "Active" else "Ready"
                InspectorSpecRow(label = "Dual Subtitles", value = dualSubReady, valueColor = MaterialTheme.colorScheme.primary)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.75.dp)

            // SECTION 4: CONTAINER & FILE
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Folder, null, tint = TimelineAccent, modifier = Modifier.size(14.dp))
                    Text(
                        "CONTAINER & FILE",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TimelineAccent,
                        letterSpacing = 0.5.sp
                    )
                }

                val containerShort = when {
                    video.name.endsWith(".mkv", true) || video.mime.contains("matroska") -> "MKV (Matroska)"
                    video.name.endsWith(".mp4", true) || video.mime.contains("mp4") -> "MP4 Video"
                    video.name.endsWith(".webm", true) -> "WebM Video"
                    else -> video.mime.ifEmpty { "Video Container" }
                }
                InspectorSpecRow(label = "Container", value = containerShort)

                val sizeStr = if (video.bytes >= 0) Formatter.formatFileSize(context, video.bytes) else "Local Storage"
                InspectorSpecRow(label = "File Size", value = sizeStr)

                val folderName = state.folders.firstOrNull { it.uri == video.folder }?.name ?: "Local Media"
                InspectorSpecRow(label = "Folder Location", value = folderName)
            }
        }
    }
}

@Composable
private fun InspectorSpecRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
