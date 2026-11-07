package com.jadzumi.video

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlayerToolsDialog(section: String, playback: PlaybackController, onMessage: (String) -> Unit, onDismiss: () -> Unit) {
    if (playback.inPip) return
    val video = playback.video ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (section == "sleep") "Sleep controls" else "Episode continuity") },
        text = {
            val base = Modifier.fillMaxWidth().heightIn(max = 460.dp)
            Column(base.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (section == "sleep") {
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
                } else {
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
