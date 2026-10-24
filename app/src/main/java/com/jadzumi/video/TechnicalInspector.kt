package com.jadzumi.video

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal data class InspectionState(val details: MediaInspection? = null, val loading: Boolean = true)

@Composable
internal fun rememberLocalInspection(video: LocalVideo?, refresh: Int = 0): State<InspectionState> {
    val context = LocalContext.current.applicationContext
    return key(video?.uri, video?.bytes, video?.modified, refresh) {
        produceState(InspectionState()) {
            value = if (video == null) InspectionState(MediaInspection(note = "Select a video to inspect."), false)
                else {
                    val result = try { LocalMediaProbe.inspect(context, video, refresh > 0) }
                    catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (_: Exception) { MediaInspection(note = "Couldn't inspect this file. Check folder access and retry.") }
                    InspectionState(result, false)
                }
        }
    }
}

@Composable
internal fun TechnicalInspectorCard(sampleVideo: LocalVideo?, playback: PlaybackController) {
    var refresh by remember(sampleVideo?.uri) { mutableIntStateOf(0) }
    val scan by rememberLocalInspection(sampleVideo, refresh)
    val sameFile = sampleVideo != null && sampleVideo.uri == playback.video?.uri
    val info = mergeVideoInspection(if (sameFile) playback.runtimeVideoFormat else null, scan.details?.video)
    val videoDecoder = decoderForInspectedFile(sampleVideo?.uri, playback.video?.uri, playback.runtimeVideoDecoder)
    val audioDecoder = decoderForInspectedFile(sampleVideo?.uri, playback.video?.uri, playback.runtimeAudioDecoder)
    val missing = if (scan.loading) "Inspecting…" else "Not reported by this file"
    val width = info?.width ?: sampleVideo?.width?.takeIf { it > 0 }
    val height = info?.height ?: sampleVideo?.height?.takeIf { it > 0 }
    val audio = (if (sameFile) playback.runtimeAudioTracks.takeIf { it.isNotEmpty() }
        ?: playback.runtimeAudioFormat?.let { listOf(it) } else null) ?: scan.details?.audio.orEmpty()
    val subtitles = (if (sameFile) playback.runtimeSubtitleTracks.takeIf { it.isNotEmpty() } else null)
        ?: scan.details?.subtitles.orEmpty()
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Memory, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Technical Inspector", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 8.dp))
                IconButton(enabled = sampleVideo != null && !scan.loading, onClick = { refresh++ }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh file inspection")
                }
            }
            if (scan.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            TechRow("File MIME", sampleVideo?.mime?.takeIf { it.isNotBlank() } ?: missing)
            TechRow("Video codec", inspectionVideoCodec(info?.mime) ?: missing)
            TechRow("Resolution", if (width != null && height != null) "$width × $height pixels" else missing)
            TechRow("Color space", inspectionColor(info) ?: missing)
            info?.bitDepth?.let { TechRow("Reported bit depth", "$it-bit") }
            TechRow("Frame rate", inspectionFrameRate(info?.frameRate) ?: missing)
            HorizontalDivider()
            TechRow("Video decoder", videoDecoder?.name ?: "Detected during playback")
            TechRow("HW acceleration", when (videoDecoder?.hardware) {
                DecoderHardware.HARDWARE -> "Hardware (Android reported)"
                DecoderHardware.SOFTWARE -> "Software decoder"
                DecoderHardware.NOT_REPORTED -> "Not reported by Android"
                null -> "Detected during playback"
            })
            TechRow("Audio decoder", audioDecoder?.name ?: "Detected during playback")
            TechRow("Audio tracks", if (audio.isNotEmpty()) audio.joinToString("\n", transform = ::inspectionAudio)
                else if (scan.loading) missing else if (scan.details?.tracksRead == true) "No audio tracks reported" else missing)
            TechRow("Subtitle tracks", subtitles.takeIf { it.isNotEmpty() }
                ?.joinToString("\n", transform = ::inspectionSubtitle)
                ?: if (scan.loading) missing else if (scan.details?.tracksRead == true) "No embedded subtitle tracks reported" else missing)
            TechRow("Subtitle engine", "AndroidX Media3")
            scan.details?.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text("File details are read locally. Decoder names reflect the inspected video only while it is loaded in the player.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun InspectedTracksCard(video: LocalVideo, playback: PlaybackController) {
    var refresh by remember(video.uri) { mutableIntStateOf(0) }
    val scan by rememberLocalInspection(video, refresh)
    val sameFile = video.uri == playback.video?.uri
    val audio = (if (sameFile) playback.runtimeAudioTracks.takeIf { it.isNotEmpty() }
        ?: playback.runtimeAudioFormat?.let { listOf(it) } else null) ?: scan.details?.audio.orEmpty()
    val subtitles = (if (sameFile) playback.runtimeSubtitleTracks.takeIf { it.isNotEmpty() } else null)
        ?: scan.details?.subtitles.orEmpty()
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Audio & Subtitle Tracks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            audio.forEachIndexed { index, track -> TechRow("Audio ${index + 1}", inspectionAudio(track)) }
            subtitles.forEachIndexed { index, track -> TechRow("Subtitle ${index + 1}", inspectionSubtitle(track)) }
            if (scan.loading) Text("Reading track headers…", style = MaterialTheme.typography.bodySmall)
            else if (audio.isEmpty() && subtitles.isEmpty()) {
                Text(if (scan.details?.tracksRead == true) "No audio or embedded subtitle tracks reported."
                    else "Track details aren't exposed by Android for this file. Try playback for more information.", style = MaterialTheme.typography.bodySmall)
            }
            Text("File headers are scanned locally; the loaded player can report additional tracks.", style = MaterialTheme.typography.bodySmall)
            if (!scan.loading && scan.details?.tracksRead != true) TextButton(onClick = { refresh++ }) { Text("Retry inspection") }
        }
    }
}
