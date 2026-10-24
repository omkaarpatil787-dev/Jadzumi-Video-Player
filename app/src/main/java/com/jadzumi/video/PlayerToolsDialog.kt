package com.jadzumi.video

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlayerToolsDialog(section: String, playback: PlaybackController, onMessage: (String) -> Unit, onDismiss: () -> Unit) {
    if (playback.inPip) return
    val video = playback.video ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editing by remember(video.uri) { mutableStateOf<SceneBookmark?>(null) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var exportText by remember { mutableStateOf("") }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) scope.launch {
            try {
                val text = exportText
                withContext(Dispatchers.IO) {
                    requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).bufferedWriter(Charsets.UTF_8).use { it.write(text) }
                }
                onMessage("Scene notes exported; no video content was copied.")
            } catch (_: Exception) { onMessage("Could not export scene notes.") }
        }
    }
    if (editing != null) {
        val item = requireNotNull(editing)
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("Scene at ${timecode(item.positionMs / 1000)}") },
            text = { Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it.take(80) }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(note, { note = it.take(500) }, label = { Text("Personal note (optional)") }, maxLines = 5)
                Text("Playback is paused while you write. Notes stay on this device unless you export them.", style = MaterialTheme.typography.bodySmall)
            } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
            confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = {
                val oldId = item.id.takeIf { it.isNotEmpty() }
                if (playback.saveScene(title, note, item.positionMs, oldId)) editing = null
                else onMessage("Cannot save: limit is 200 scenes per video and 64 KB of notes.")
            }) { Text("Save") } })
        return
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(when (section) { "sleep" -> "Sleep controls"; "continuity" -> "Episode continuity"; else -> "Scene bookmarks" }) },
        text = {
            val base = Modifier.fillMaxWidth().heightIn(max = 460.dp)
            Column(if (section == "bookmarks") base else base.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    "continuity" -> {
                        Row { Text("Autoplay adjacent episodes", Modifier.weight(1f)); Switch(playback.autoplayNext, playback::updateAutoplayNext) }
                        Text("Autoplay waits until the file ends. Missing episode numbers, season changes and uncertain numbering always ask for confirmation.")
                        Text("Audio/subtitle language choices are remembered per series when files label their languages. Unlabeled tracks use automatic selection on the next file.")
                        playback.nextEpisode?.let { next ->
                            Text("Next available: ${next.name}")
                            episodeContinuationWarning(video.name, next.name)?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                            TextButton(onClick = { playback.playNextEpisode(); onDismiss() }) { Text("Play next available file") }
                        } ?: Text("No next file in this queue.")
                    }
                    else -> {
                        val scenes = playback.sceneBookmarks()
                        TextButton(onClick = {
                            val at = playback.pauseForNotes()
                            title = "Scene ${scenes.size + 1}"; note = ""
                            editing = SceneBookmark("", at, title)
                        }) { Text("Add at ${timecode(playback.position / 1000)}") }
                        TextButton(enabled = scenes.isNotEmpty(), onClick = {
                            exportText = SceneBookmarkCodec.notesExport(video.name, scenes)
                            export.launch("Jadzumi-scenes.txt")
                        }) { Text("Export timestamps and notes") }
                        if (scenes.isEmpty()) Text("No scenes saved for this video yet.")
                        LazyColumn(Modifier.weight(1f, fill = false)) {
                            items(scenes, key = { it.id }) { scene ->
                                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Text("${timecode(scene.positionMs / 1000)} · ${scene.title}", Modifier.clickable {
                                        playback.seekToSubtitle(scene.positionMs); onDismiss()
                                    }, color = MaterialTheme.colorScheme.primary)
                                    if (scene.note.isNotBlank()) Text(scene.note, style = MaterialTheme.typography.bodySmall)
                                    Row {
                                        TextButton(onClick = { playback.pauseForNotes(); title = scene.title; note = scene.note; editing = scene }) { Text("Edit") }
                                        TextButton(onClick = { playback.removeScene(scene.id) }) { Text("Delete") }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
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
