package com.jadzumi.video

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun PlayerToolsDialog(
    section: String,
    playback: PlaybackController,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (playback.inPip) return
    val video = playback.video ?: return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (section) {
                    "sleep" -> "Sleep Controls"
                    "palette" -> "Aesthetic Color Harmony & Frame Still"
                    "clip" -> "Animated Scene Clipper"
                    else -> "Episode Continuity"
                }
            )
        },
        text = {
            val base = Modifier.fillMaxWidth().heightIn(max = 480.dp)
            Column(base.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (section) {
                    "sleep" -> {
                        val plan = playback.sleepPlan
                        Text(when (plan?.mode) {
                            SleepMode.TIMER -> "Pauses in ${timecode(playback.sleepRemainingSeconds)}; audio fades during the last 10 seconds."
                            SleepMode.END_CURRENT -> "Pauses at the end of this video."
                            SleepMode.EPISODES -> "Stops after ${plan.episodes} more video endings."
                            null -> "No sleep stop is active."
                        })
                        listOf(15, 30, 60, 90).forEach { minutes ->
                            TextButton(onClick = { playback.startSleep(minutes); onMessage("Sleep timer: $minutes minutes"); onDismiss() }) { Text("Pause in $minutes minutes") }
                        }
                        TextButton(onClick = { playback.sleepAfterEpisodes(); onDismiss() }) { Text("Stop at the end of this video") }
                        TextButton(onClick = { playback.sleepAfterEpisodes(2); onDismiss() }) { Text("Stop after 2 videos") }
                        TextButton(onClick = { playback.sleepAfterEpisodes(3); onDismiss() }) { Text("Stop after 3 videos") }
                        if (plan != null) TextButton(onClick = { playback.cancelSleep() }) { Text("Cancel sleep stop") }
                    }

                    "palette" -> {
                        var paletteResult by remember { mutableStateOf<CinemaPaletteResult?>(null) }
                        var loading by remember { mutableStateOf(true) }

                        LaunchedEffect(Unit) {
                            paletteResult = CinemaPaletteExtractor.extractFromVideo(context, video, playback.position)
                            loading = false
                        }

                        if (loading) {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            paletteResult?.let { res ->
                                res.frameBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Frame Still",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("5-Swatch Aesthetic Palette", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                ) {
                                    res.swatches.forEach { swatch ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(swatch.color),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                swatch.hexCode,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (swatch.color.red * 0.299 + swatch.color.green * 0.587 + swatch.color.blue * 0.114 > 0.5) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "clip" -> {
                        var selectedDuration by remember { mutableIntStateOf(3) }
                        var isClipping by remember { mutableStateOf(false) }
                        var clipUri by remember { mutableStateOf<Uri?>(null) }

                        Text("Capture a short 2–6s animated clip starting at ${timecode(playback.position / 1000)}:")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(2, 3, 4, 6).forEach { sec ->
                                FilterChip(
                                    selected = selectedDuration == sec,
                                    onClick = { selectedDuration = sec },
                                    label = { Text("${sec}s") }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                isClipping = true
                                coroutineScope.launch {
                                    clipUri = SceneGifMaker.createSceneClip(context, video, playback.position, selectedDuration)
                                    isClipping = false
                                    clipUri?.let { SceneGifMaker.shareClip(context, it, video.title.ifBlank { video.name }) }
                                }
                            },
                            enabled = !isClipping,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            if (isClipping) {
                                CircularProgressIndicator(Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Clipping Scene...")
                            } else {
                                Text("Generate & Share Clip")
                            }
                        }
                    }

                    else -> {
                        Row { Text("Autoplay adjacent episodes", Modifier.weight(1f)); Switch(playback.autoplayNext, playback::updateAutoplayNext) }
                        Text("Autoplay waits until the file ends. Missing episode numbers, season changes and uncertain numbering always ask for confirmation.")
                        Text("Audio/subtitle language choices are remembered per series when files label their languages. Unlabeled tracks use automatic selection on the next file.")
                        playback.nextEpisode?.let { next ->
                            Text("Next available: ${next.name}")
                            episodeContinuationWarning(video.name, next.name)?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                            TextButton(onClick = { playback.playNextEpisode(); onDismiss() }) { Text("Play next available file") }
                        } ?: Text("No next file in this queue.")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun EpisodeContinuationPrompt(playback: PlaybackController) {
    if (playback.inPip) return
    playback.continuationPrompt?.let { next ->
        AlertDialog(onDismissRequest = playback::dismissNextEpisodeCountdown,
            title = { Text("Continue to the next episode?") },
            text = { Text("${playback.continuationMessage}\n\n${next.name}") },
            dismissButton = { TextButton(onClick = playback::dismissNextEpisodeCountdown) { Text("Stay here") } },
            confirmButton = { TextButton(onClick = { playback.confirmNextEpisode(next) }) { Text("Play next") } })
    }
}
