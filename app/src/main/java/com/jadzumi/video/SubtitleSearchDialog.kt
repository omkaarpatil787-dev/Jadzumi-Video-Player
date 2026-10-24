package com.jadzumi.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SubtitleSearchDialog(cues: List<SubtitleSearchCue>, onSeek: (Long) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(cues, query) { SubtitleSearch.search(cues, query) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search subtitles") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = query, onValueChange = { query = it.take(120) }, singleLine = true,
                    label = { Text("Find a line") }, modifier = Modifier.fillMaxWidth())
                Text(when {
                    query.isBlank() -> "Search this local subtitle file and tap a line to jump to it."
                    results.isEmpty() -> "No matching lines."
                    else -> "${results.size} matches${if (results.size == 100) " (first 100)" else ""}"
                }, style = MaterialTheme.typography.bodySmall)
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(results) { cue ->
                        Column(Modifier.fillMaxWidth().clickable { onSeek(cue.startMs) }.padding(vertical = 10.dp)) {
                            Text(timecode(cue.startMs / 1000), color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium)
                            Text(cue.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
