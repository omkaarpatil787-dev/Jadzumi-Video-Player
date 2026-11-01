package com.jadzumi.video

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.core.net.toUri

data class StorageBreakdown(
    val totalBytes: Long,
    val watchedBytes: Long,
    val watchedCount: Int,
    val giantVideos: List<LocalVideo>,
    val watchedVideos: List<LocalVideo>
)

object StorageAnalyzerHelper {
    fun analyze(videos: List<LocalVideo>, playback: PlaybackController): StorageBreakdown {
        var totalBytes = 0L
        var watchedBytes = 0L
        val watchedList = mutableListOf<LocalVideo>()
        val giantList = mutableListOf<LocalVideo>()

        videos.forEach { v ->
            totalBytes += v.bytes
            val isWatched = playback.isWatched(v)
            if (isWatched) {
                watchedBytes += v.bytes
                watchedList.add(v)
            }
            if (v.bytes >= 3L * 1024 * 1024 * 1024) { // >= 3 GB
                giantList.add(v)
            }
        }

        return StorageBreakdown(
            totalBytes = totalBytes,
            watchedBytes = watchedBytes,
            watchedCount = watchedList.size,
            giantVideos = giantList.sortedByDescending { it.bytes },
            watchedVideos = watchedList.sortedByDescending { it.bytes }
        )
    }

    fun deleteSafely(context: Context, video: LocalVideo): Boolean {
        return try {
            val uri = video.uri.toUri()
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun StorageAnalyzerSheet(
    videos: List<LocalVideo>,
    playback: PlaybackController,
    onVideoDeleted: () -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0: Watched Cleanup, 1: Heaviest Files
    var videoPendingDelete by remember { mutableStateOf<LocalVideo?>(null) }
    var confirmBatchDelete by remember { mutableStateOf(false) }

    val breakdown = remember(videos, playback.historyVersion) {
        StorageAnalyzerHelper.analyze(videos, playback)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Storage Analyzer & Cleaner",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${formatBytes(breakdown.totalBytes)} indexed across ${videos.size} files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Recoverable Space Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Recoverable Watched Space",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatBytes(breakdown.watchedBytes),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${breakdown.watchedCount} fully watched movies and episodes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (breakdown.watchedCount > 0) {
                        Button(
                            onClick = { confirmBatchDelete = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Clean All")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tab Bar
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Watched Media (${breakdown.watchedCount})") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Heaviest Videos (${breakdown.giantVideos.size})") }
                )
            }

            Spacer(Modifier.height(12.dp))

            val displayList = if (activeTab == 0) breakdown.watchedVideos else breakdown.giantVideos

            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (activeTab == 0) Icons.Rounded.DoneAll else Icons.Rounded.HighQuality,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (activeTab == 0) "No watched videos pending cleanup" else "No large videos detected (> 3 GB)",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayList, key = { it.uri }) { video ->
                        StorageVideoRow(
                            video = video,
                            isWatched = playback.isWatched(video),
                            onDelete = { videoPendingDelete = video }
                        )
                    }
                }
            }
        }
    }

    // Individual Delete Confirmation Dialog
    videoPendingDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoPendingDelete = null },
            title = { Text("Delete Video?") },
            text = {
                Text("Are you sure you want to delete '${video.title}' (${formatBytes(video.bytes)})? This removes the file permanently from storage.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = StorageAnalyzerHelper.deleteSafely(context, video)
                        if (ok) {
                            onMessage("Deleted '${video.title}'")
                            onVideoDeleted()
                        } else {
                            onMessage("Could not delete file via storage provider.")
                        }
                        videoPendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Batch Delete Confirmation Dialog
    if (confirmBatchDelete) {
        AlertDialog(
            onDismissRequest = { confirmBatchDelete = false },
            title = { Text("Clean All Watched Media?") },
            text = {
                Text("This will permanently delete ${breakdown.watchedCount} watched videos, freeing up ${formatBytes(breakdown.watchedBytes)} of device storage.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        var successCount = 0
                        breakdown.watchedVideos.forEach { v ->
                            if (StorageAnalyzerHelper.deleteSafely(context, v)) {
                                successCount++
                            }
                        }
                        onMessage("Cleaned $successCount watched videos (${formatBytes(breakdown.watchedBytes)} freed)")
                        onVideoDeleted()
                        confirmBatchDelete = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clean All")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmBatchDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StorageVideoRow(
    video: LocalVideo,
    isWatched: Boolean,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        video.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isWatched) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2E7D32).copy(alpha = 0.20f),
                            contentColor = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatBytes(video.bytes)} • ${timecode(video.duration / 1000)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                )
            }
        }
    }
}
