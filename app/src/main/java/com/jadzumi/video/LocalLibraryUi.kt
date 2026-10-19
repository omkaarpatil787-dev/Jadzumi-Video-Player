package com.jadzumi.video

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.text.format.Formatter
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

@Composable fun rememberFolderPicker(library: LocalLibrary): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) {
            try {
                val flags = result.data?.flags ?: 0
                val takeFlags = (flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
                    .takeIf { it != 0 } ?: Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                library.add(uri)
            } catch (_: SecurityException) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    library.add(uri)
                }.onFailure {
                    library.report("Android did not retain access. Please select the folder again.")
                }
            }
        }
    }
    return {
        try {
            launcher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                )
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            })
        } catch (_: android.content.ActivityNotFoundException) {
            library.report("Android's folder picker is unavailable on this device.")
        }
    }
}

@Composable private fun LocalPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun LocalLibraryPage(
    state: LocalLibraryState,
    library: LocalLibrary,
    query: String,
    select: (String) -> Unit,
    preview: () -> Unit
) {
    val add = rememberFolderPicker(library)
    var category by rememberSaveable { mutableStateOf("All videos") }
    val videos = remember(state.videos, query, category) {
        state.videos.distinctBy { it.identity }.filter {
            it.name.contains(query.trim(), true) && when (category) {
                "Episodes" -> it.episode != null
                "Other videos" -> it.episode == null
                else -> true
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pad = if (maxWidth >= 720.dp) 28.dp else 18.dp
        PullToRefreshBox(
            isRefreshing = state.busy,
            onRefresh = { library.rescan() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(pad),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Text("Your cinema, at home.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${state.videos.distinctBy { it.identity }.size} local videos · ${state.folders.size} folders",
                        Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    LocalPanel {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = add, enabled = !state.busy) {
                                Icon(Icons.Rounded.CreateNewFolder, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add folder", maxLines = 1, softWrap = false)
                            }
                            if (state.busy) {
                                TextButton(onClick = library::cancel) { Text("Cancel", maxLines = 1, softWrap = false) }
                            }
                        }
                        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        Text(
                            "Pull down to refresh or tap the top sync icon to discover new videos. Files remain in place.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("All videos", "Episodes", "Other videos")) { name ->
                            FilterChip(
                                selected = category == name,
                                onClick = { category = name },
                                label = { Text(name, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }
                if (videos.isEmpty()) {
                    item {
                        LocalPanel {
                            Icon(Icons.Rounded.VideoLibrary, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                if (state.videos.isEmpty()) "Make room for your stories" else "No matching videos",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (state.folders.isEmpty()) "Add your first folder to build your offline library."
                                else "Pull down to refresh or try another search."
                            )
                        }
                    }
                }
                items(videos, key = { it.uri }) { video ->
                    LocalVideoCard(video) { select(video.uri) }
                }
                item {
                    TextButton(onClick = preview) { Text("View original design samples") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun LocalFoldersPage(
    state: LocalLibraryState,
    library: LocalLibrary,
    wide: Boolean,
    select: (String) -> Unit
) {
    val context = LocalContext.current
    val add = rememberFolderPicker(library)
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var remove by remember { mutableStateOf<LocalFolder?>(null) }
    var videoToDelete by remember { mutableStateOf<LocalVideo?>(null) }
    val active = state.folders.firstOrNull { it.uri == selected } ?: state.folders.firstOrNull()
    val videos = state.videos.filter { it.folder == active?.uri }
    val totalBytes = remember(videos) { videos.sumOf { it.bytes.coerceAtLeast(0) } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= 840.dp
        val pad = if (maxWidth >= 720.dp) 28.dp else 18.dp

        if (twoPane) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = pad, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FoldersHeader(active, videos.size, totalBytes, state, add, library)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    LazyColumn(
                        Modifier.weight(0.38f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (state.folders.isEmpty()) {
                            item { LocalPanel { Text("No folders connected", fontWeight = FontWeight.Bold); Text("Add a folder using Android's file picker.") } }
                        }
                        items(state.folders, key = { it.uri }) { folder ->
                            FolderSummaryCard(folder, state, active == folder, { selected = folder.uri }, { remove = folder })
                        }
                    }
                    LazyColumn(
                        Modifier.weight(0.62f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            Text("${videos.size} indexed videos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        items(videos, key = { it.uri }) { video ->
                            LocalVideoCard(video, onDelete = { videoToDelete = video }) { select(video.uri) }
                        }
                    }
                }
            }
        } else {
            // Single-column stacked layout for portrait / split-screen so video rows have 100% width
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(pad),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    FoldersHeader(active, videos.size, totalBytes, state, add, library)
                }
                if (state.folders.isEmpty()) {
                    item { LocalPanel { Text("No folders connected", fontWeight = FontWeight.Bold); Text("Add a folder using Android's file picker.") } }
                } else {
                    item {
                        Text("Connected Folders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(state.folders, key = { it.uri }) { folder ->
                        FolderSummaryCard(folder, state, active == folder, { selected = folder.uri }, { remove = folder })
                    }
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("${videos.size} indexed videos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(videos, key = { it.uri }) { video ->
                    LocalVideoCard(video, onDelete = { videoToDelete = video }) { select(video.uri) }
                }
            }
        }
    }

    remove?.let { folder ->
        AlertDialog(
            onDismissRequest = { remove = null },
            title = { Text("Remove ${folder.name}?") },
            text = { Text("Remove this folder from Jadzumi Video. The videos on your storage will remain unchanged.") },
            confirmButton = { TextButton(onClick = { library.remove(folder); remove = null }, enabled = !state.busy) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { remove = null }) { Text("Keep folder") } }
        )
    }

    videoToDelete?.let { video ->
        val sizeStr = if (video.bytes >= 0) Formatter.formatFileSize(context, video.bytes) else "Unknown size"
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            icon = { Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Permanently delete video?") },
            text = { Text("This will permanently delete \"${video.name}\" ($sizeStr) from your tablet's internal storage. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { library.deleteVideo(video); videoToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete permanently") }
            },
            dismissButton = { TextButton(onClick = { videoToDelete = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun FoldersHeader(
    active: LocalFolder?,
    videoCount: Int,
    totalBytes: Long,
    state: LocalLibraryState,
    add: () -> Unit,
    library: LocalLibrary
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.padding(end = 16.dp)) {
                Text("Folders & Storage", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                if (active != null) {
                    Text(
                        "$videoCount videos · ${Formatter.formatFileSize(context, totalBytes)} indexed in ${active.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = add, enabled = !state.busy) {
                    Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add folder", maxLines = 1, softWrap = false)
                }
                OutlinedButton(onClick = library::fullRescan, enabled = !state.busy && state.folders.isNotEmpty()) {
                    Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Full Rescan", maxLines = 1, softWrap = false)
                }
                if (state.busy) {
                    TextButton(onClick = library::cancel) { Text("Cancel", maxLines = 1, softWrap = false) }
                }
            }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun FolderSummaryCard(
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
        shape = RoundedCornerShape(22.dp),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.primary)
                    Text(folder.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(
                    onClick = onRemove,
                    enabled = !state.busy,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Remove", fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
            Text(
                "${folderVideos.size} videos · ${Formatter.formatFileSize(context, folderBytes)} · ${folder.status}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable private fun LocalVideoCard(
    video: LocalVideo,
    onDelete: (() -> Unit)? = null,
    select: () -> Unit
) {
    val context = LocalContext.current
    Card(
        onClick = select,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(22.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(14.dp)) {
            val compact = maxWidth < 460.dp
            val thumbWidth = if (compact) 92.dp else 116.dp
            val thumbHeight = if (compact) 64.dp else 78.dp

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocalThumbnail(video, Modifier.size(thumbWidth, thumbHeight).clip(RoundedCornerShape(14.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        video.title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sizeText = if (video.bytes >= 0) " · ${Formatter.formatFileSize(context, video.bytes)}" else ""
                    val durText = if (video.duration > 0) timecode(video.duration / 1000) else "Duration unavailable"
                    Text(
                        durText + sizeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    video.episode?.let {
                        Text(
                            "Season ${it.season} · Episode ${it.episode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.DeleteOutline, "Delete video from storage", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        "View file details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun LocalDetailPage(video: LocalVideo, state: LocalLibraryState, playback: PlaybackController) {
    val context = LocalContext.current
    val library = remember { LocalLibrary.get(context) }
    var confirmDelete by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pad = if (maxWidth >= 720.dp) 28.dp else 18.dp
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(pad),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                LocalThumbnail(video, Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 260.dp).clip(RoundedCornerShape(28.dp)))
            }
            item {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        video.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false).padding(end = 12.dp)
                    )

                    val badge = if (playback.video?.identity == video.identity && playback.videoCodecBadge != null) {
                        playback.videoCodecBadge
                    } else when {
                        video.name.contains("hevc", ignoreCase = true) || video.name.contains("x265", ignoreCase = true) -> "HEVC Main10"
                        video.width >= 3840 -> "4K UHD"
                        video.width >= 1920 -> "1080p FHD"
                        else -> null
                    }

                    badge?.let {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                it,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            item {
                LocalPanel {
                    Text("Local file", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(video.name, style = MaterialTheme.typography.bodyMedium)
                    Text("Folder: ${state.folders.firstOrNull { it.uri == video.folder }?.name ?: "Unknown"}")
                    Text("Duration: ${if (video.duration > 0) timecode(video.duration / 1000) else "Unavailable"}")
                    Text("Size: ${if (video.bytes >= 0) Formatter.formatFileSize(context, video.bytes) else "Unavailable"}")
                    Text("Dimensions: ${if (video.width > 0 && video.height > 0) "${video.width} × ${video.height}" else "Unavailable"}")
                    Text("Media type: ${video.mime.ifEmpty { "Unavailable" }}")
                    video.episode?.let { Text("Filename suggests season ${it.season}, episode ${it.episode}.") }

                    val resume = playback.savedPosition(video)
                    if (resume > 0) {
                        Text(
                            "Resume from ${timecode(resume / 1000)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { playback.open(video) }) {
                            Icon(Icons.Rounded.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (resume > 0) "Resume" else "Play", maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete file", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        val sizeStr = if (video.bytes >= 0) Formatter.formatFileSize(context, video.bytes) else "Unknown size"
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Permanently delete video?") },
            text = { Text("This will permanently delete \"${video.name}\" ($sizeStr) from your tablet's internal storage. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (playback.video?.identity == video.identity) {
                            playback.close()
                        }
                        library.deleteVideo(video)
                        confirmDelete = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

internal object ThumbnailMemory {
    val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) { override fun sizeOf(key: String, value: Bitmap) = value.byteCount }
    val permits = Semaphore(2)
}

fun clearThumbnailMemoryCache() {
    ThumbnailMemory.cache.evictAll()
}

@Composable private fun LocalThumbnail(video: LocalVideo, modifier: Modifier) {
    val context = LocalContext.current.applicationContext
    val key = "${video.uri}:${video.modified}:${video.bytes}"
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = key) {
        value = null
        value = withContext(Dispatchers.IO) {
            ThumbnailMemory.permits.withPermit {
                ThumbnailMemory.cache.get(key) ?: run {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, video.uri.toUri())
                        if (Build.VERSION.SDK_INT >= 27) {
                            retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 480, 270)
                                ?.also { ThumbnailMemory.cache.put(key, it) }
                        } else null
                    } catch (_: Exception) { null } finally { runCatching { retriever.release() } }
                }
            }
        }
    }
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant) {
        val frame = bitmap
        if (frame != null) Image(frame.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Movie, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary) }
    }
}
// END OF FILE
