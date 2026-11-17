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
    val cardBg = SpatialGlass.cardBg(theme)
    val cardBorder = SpatialGlass.cardBorder(theme, 1.dp)
    val cardElevation = if (isLight) 6.dp else 10.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header with Refresh Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Analytics, null, tint = LavenderAccent, modifier = Modifier.size(20.dp))
                Text("Media Inspector 2.0", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (!scan.loading) {
                IconButton(
                    onClick = { refresh++ },
                    modifier = Modifier.size(32.dp)
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

        if (scan.loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = LavenderAccent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // BENTO ROW 1: Video Engine & Audio Engine (Side-by-Side)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // CARD 1: VIDEO STREAM & CODEC
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = cardBorder,
                shadowElevation = cardElevation
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "VIDEO ENGINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )
                        Icon(Icons.Rounded.Videocam, null, tint = LavenderAccent, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.height(6.dp))

                    val quality = when {
                        width != null && width >= 3840 -> "4K UHD"
                        width != null && width >= 1920 -> "1080p FHD"
                        width != null && width >= 1280 -> "720p HD"
                        else -> "Standard Def"
                    }
                    Text(quality, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                    val resText = if (width != null && height != null) "$width × $height" else "Auto Detected"
                    Text(resText, fontSize = 11.sp, color = LavenderAccent, fontWeight = FontWeight.Medium)

                    Spacer(Modifier.height(8.dp))
                    val codecText = inspectionVideoCodec(info?.mime)
                        ?: if (video.name.contains("265", true) || video.name.contains("hevc", true)) "HEVC / H.265"
                        else if (video.name.contains("264", true) || video.name.contains("avc", true)) "AVC / H.264"
                        else "H.264 / HEVC Decoded"
                    Text("Codec: $codecText", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)

                    val hwStatus = when (videoDecoder?.hardware) {
                        DecoderHardware.HARDWARE -> "Hardware (GPU)"
                        DecoderHardware.SOFTWARE -> "Software Fallback"
                        else -> "Hardware Accelerated"
                    }
                    Text(hwStatus, fontSize = 10.5.sp, color = Color(0xFF34D399), fontWeight = FontWeight.SemiBold)
                }
            }

            // CARD 2: AUDIO ENGINE & CHANNELS
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = cardBorder,
                shadowElevation = cardElevation
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "AUDIO ENGINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )
                        Icon(Icons.Rounded.Audiotrack, null, tint = TimelineAccent, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.height(6.dp))

                    val primaryAudio = audio.firstOrNull()
                    val audioCodec = when {
                        primaryAudio != null -> inspectionAudio(primaryAudio).substringBefore(" ·").ifEmpty { "Multichannel" }
                        video.name.contains("DDP", true) || video.name.contains("EAC3", true) -> "E-AC-3 Surround"
                        video.name.contains("AAC", true) -> "AAC Stereo / 5.1"
                        video.name.contains("DTS", true) -> "DTS-HD Audio"
                        video.name.contains("TrueHD", true) -> "Dolby TrueHD"
                        else -> "NextLib FFmpeg"
                    }
                    Text(audioCodec, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)

                    val channelsText = when {
                        primaryAudio?.channels != null && primaryAudio.channels > 2 -> "${primaryAudio.channels}CH Surround"
                        video.name.contains("6CH", true) || video.name.contains("5.1", true) -> "5.1 Surround (6CH)"
                        video.name.contains("7.1", true) -> "7.1 Surround (8CH)"
                        else -> "Stereo / Surround"
                    }
                    Text(channelsText, fontSize = 11.sp, color = TimelineAccent, fontWeight = FontWeight.Medium)

                    Spacer(Modifier.height(8.dp))
                    val trackCountStr = if (audio.isNotEmpty()) "${audio.size} Audio Stream(s)" else "Bundled FFmpeg Decoder"
                    Text(trackCountStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Text("Auto Downmix Ready", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                }
            }
        }

        // BENTO ROW 2: Subtitle Studio & File Container Storage (Side-by-Side)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // CARD 3: SUBTITLE STUDIO
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = cardBorder,
                shadowElevation = cardElevation
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SUBTITLE STUDIO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )
                        Icon(Icons.Rounded.ClosedCaption, null, tint = LavenderAccent, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.height(6.dp))

                    val subCount = subtitles.size
                    val subTitle = if (subCount > 0) "$subCount Track(s) Detected" else "Subtitles Ready"
                    Text(subTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)

                    val subFormats = "Styled ASS · SRT · VTT"
                    Text(subFormats, fontSize = 11.sp, color = LavenderAccent, fontWeight = FontWeight.Medium)

                    Spacer(Modifier.height(8.dp))
                    Text("Engine: Media3 TextRenderer", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val dualSubReady = if (playback.dualSubtitleStudio.isSecondaryEnabled) "Dual Subtitles: Active" else "Dual Subtitles: Ready"
                    Text(dualSubReady, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }

            // CARD 4: STORAGE & CONTAINER
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = cardBorder,
                shadowElevation = cardElevation
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "CONTAINER & STORAGE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )
                        Icon(Icons.Rounded.Folder, null, tint = TimelineAccent, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.height(6.dp))

                    val sizeStr = if (video.bytes >= 0) Formatter.formatFileSize(context, video.bytes) else "Local Storage"
                    Text(sizeStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                    val containerShort = when {
                        video.name.endsWith(".mkv", true) || video.mime.contains("matroska") -> "MKV (Matroska)"
                        video.name.endsWith(".mp4", true) || video.mime.contains("mp4") -> "MP4 Video"
                        video.name.endsWith(".webm", true) -> "WebM Video"
                        else -> video.mime.ifEmpty { "Video Container" }
                    }
                    Text(containerShort, fontSize = 11.sp, color = TimelineAccent, fontWeight = FontWeight.Medium)

                    Spacer(Modifier.height(8.dp))
                    val folderName = state.folders.firstOrNull { it.uri == video.folder }?.name ?: "Local Media"
                    Text("Folder: $folderName", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(video.name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
