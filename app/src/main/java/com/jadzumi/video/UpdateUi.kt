package com.jadzumi.video

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable private fun updates(): AppUpdates {
    val activity = LocalContext.current as MainActivity
    return remember(activity) { ViewModelProvider(activity)[AppUpdates::class.java] }
}

@Composable internal fun UpdateSettings() {
    val updater = updates()
    Text("App updates", style = MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("Automatic update checks")
            Text("Contact GitHub when opening the app, at most once daily. Separate from online metadata; off by default.", style = MaterialTheme.typography.bodySmall)
        }
        Switch(updater.automatic, updater::updateAutomatic)
    }
    TextButton(enabled = !updater.busy, onClick = { updater.check(manual = true) }) { Text("Check for updates") }
    if (updater.message.isNotBlank()) Text(updater.message, style = MaterialTheme.typography.bodySmall)
}

@Composable internal fun UpdateHost() {
    val updater = updates()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { updater.check(manual = false) }
    val release = updater.release ?: return
    AlertDialog(
        onDismissRequest = { if (!updater.busy) updater.dismiss() },
        title = { Text("Update ${release.tag}") },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(release.notes.ifBlank { "A new stable release is available." })
                Text("Downloads from GitHub. Android will ask you to approve installation. Settings and history are retained when updating with the same signing key.", style = MaterialTheme.typography.bodySmall)
                Text(updater.message, style = MaterialTheme.typography.bodySmall)
                if (updater.busy) LinearProgressIndicator(progress = { updater.progress }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = !updater.busy, onClick = {
                if (updater.downloaded) updater.install(context) else updater.download()
            }) { Text(if (updater.downloaded) "Install" else "Update") }
        },
        dismissButton = {
            Row {
                if (updater.busy) TextButton(onClick = updater::cancel) { Text("Cancel") }
                else {
                    TextButton(onClick = { updater.dismiss(skip = true) }) { Text("Skip version") }
                    TextButton(onClick = { updater.dismiss() }) { Text("Later") }
                }
            }
        }
    )
}
