package com.jadzumi.video

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dialog for choosing in-player overlay items (Arc Player style):
 * - Remaining Time
 * - Clock
 * - Battery Percentage
 */
@Composable
fun DisplayDuringPlaybackDialog(
    playback: PlaybackController,
    onDismiss: () -> Unit
) {
    var remainingTime by remember { mutableStateOf(playback.showRemainingTime) }
    var playerClock by remember { mutableStateOf(playback.showPlayerClock) }
    var batteryPercentage by remember { mutableStateOf(playback.showBatteryPercentage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Display during playback",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DisplayOptionRow(
                    label = "Remaining Time",
                    checked = remainingTime,
                    onCheckedChange = { remainingTime = it }
                )
                DisplayOptionRow(
                    label = "Clock",
                    checked = playerClock,
                    onCheckedChange = { playerClock = it }
                )
                DisplayOptionRow(
                    label = "Battery Percentage",
                    checked = batteryPercentage,
                    onCheckedChange = { batteryPercentage = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    playback.updateDisplayDuringPlayback(remainingTime, playerClock, batteryPercentage)
                    onDismiss()
                },
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
private fun DisplayOptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Dialog for managing blacklisted (hidden) folders.
 */
@Composable
fun FolderBlacklistDialog(
    library: LocalLibrary,
    folders: List<LocalFolder>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Rounded.VisibilityOff, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        },
        title = {
            Text("Folder Blacklist", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Control which folders are visible or hidden in your library. Blacklisted folders remain configured on your device but are excluded from movie and TV show indexing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (folders.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            "No media folders connected yet.",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(folders, key = { it.uri }) { folder ->
                            val isHidden = folder.isBlacklisted
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isHidden) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isHidden) MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { library.toggleFolderBlacklist(folder.uri) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f).padding(end = 10.dp)) {
                                        Text(
                                            folder.name,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            if (isHidden) "Hidden from library" else "Visible in library",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isHidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Switch(
                                        checked = isHidden,
                                        onCheckedChange = { library.toggleFolderBlacklist(folder.uri) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.error,
                                            checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(20.dp)) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

/**
 * TV Series Row Card for List view mode.
 */
@Composable
fun TvSeriesRowItem(
    series: TvSeries,
    playback: PlaybackController,
    onClick: () -> Unit
) {
    val watched = remember(series, playback.historyVersion) { series.watchedCount(playback) }
    val total = series.totalEpisodes
    val isComplete = watched >= total && total > 0
    val firstEpisode = remember(series) { series.seasons.values.firstOrNull()?.firstOrNull() }
    val thumbnailBitmap = rememberVideoThumbnail(firstEpisode)

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(width = 68.dp, height = 90.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap.asImageBitmap(),
                        contentDescription = series.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Tv, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    series.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${series.seasons.size} Season${if (series.seasons.size > 1) "s" else ""} · $total Episode${if (total > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (watched > 0) {
                    Text(
                        if (isComplete) "✓ Complete" else "$watched of $total watched",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Grid Tile for Connected Folders in Storage tab.
 */
@Composable
fun FolderGridTile(
    folder: LocalFolder,
    state: LocalLibraryState,
    isActive: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val folderVideos = remember(state.videos, folder.uri) { state.videos.filter { it.folder == folder.uri } }
    val folderBytes = remember(folderVideos) { folderVideos.sumOf { it.bytes.coerceAtLeast(0) } }

    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            if (isActive) 1.5.dp else 0.75.dp,
            if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                if (folder.isBlacklisted) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            "Hidden",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                folder.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${folderVideos.size} videos · ${Formatter.formatFileSize(context, folderBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
