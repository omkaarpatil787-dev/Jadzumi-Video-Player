package com.jadzumi.video

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupControls(playback: PlaybackController, onStatus: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Map<String, Map<String, Any>>?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            playback.flushProgress()
            try {
                withContext(Dispatchers.IO) { LocalBackup.export(context, uri) }
                onStatus("Backup saved. It contains settings, scene notes and viewing history, not video files or folder permissions.")
            } catch (_: Exception) {
                onStatus("Could not save backup. Check the destination and available storage.")
            } finally { busy = false }
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                pending = withContext(Dispatchers.IO) { LocalBackup.read(context, uri) }
            } catch (_: Exception) {
                onStatus("Cannot read backup: choose a valid Jadzumi backup (up to 1 MB). Nothing was restored.")
            } finally { busy = false }
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = !busy, onClick = { export.launch("Jadzumi-backup.jadzumi") }) { Text("Export backup") }
        OutlinedButton(enabled = !busy, onClick = { restore.launch(arrayOf("*/*")) }) { Text("Restore backup") }
    }
    pending?.let { data ->
        AlertDialog(
            onDismissRequest = { if (!busy) pending = null },
            title = { Text("Restore backup?") },
            text = { Text("Merge ${data.values.sumOf { it.size }} saved settings/history/bookmark entries? Matching entries will be replaced. Online lookup consent stays unchanged. Restart the app afterward. After reinstalling, re-add the same media folders; moved or renamed files may not match saved history. Video files and folder permissions are not included.") },
            dismissButton = { TextButton(enabled = !busy, onClick = { pending = null }) { Text("Cancel") } },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        playback.close()
                        try {
                            withContext(Dispatchers.IO) { LocalBackup.restore(context, data) }
                            playback.refreshHistory()
                            onStatus("Backup restored. Restart Jadzumi to apply all restored settings.")
                        } catch (_: Exception) {
                            onStatus("Restore could not be completed. Previous settings were restored where possible.")
                        } finally { pending = null; busy = false }
                    }
                }) { Text(if (busy) "Restoring…" else "Restore") }
            }
        )
    }
}
