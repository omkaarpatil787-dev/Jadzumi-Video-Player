package com.jadzumi.video

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import android.os.Build
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@Composable fun rememberFolderPicker(library: LocalLibrary): () -> Unit {
    val context=LocalContext.current
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri=result.data?.data
        if(result.resultCode==android.app.Activity.RESULT_OK && uri!=null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
                library.add(uri)
            } catch (_: SecurityException) { library.report("Android did not retain access. Please select the folder again.") }
        }
    }
    return { try {
        launcher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            putExtra(Intent.EXTRA_LOCAL_ONLY,true)
        })
    } catch (_: android.content.ActivityNotFoundException) { library.report("Android's folder picker is unavailable on this device.") } }
}

@Composable private fun LocalPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun LocalLibraryPage(state: LocalLibraryState, library: LocalLibrary, query: String, select: (String)->Unit, preview: ()->Unit) {
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

    PullToRefreshBox(
        isRefreshing = state.busy,
        onRefresh = { library.rescan() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Text("Your cinema, at home.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("${state.videos.distinctBy { it.identity }.size} local videos · ${state.folders.size} folders", Modifier.padding(top = 8.dp))
            }
            item {
                LocalPanel {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = add, enabled = !state.busy) {
                            Icon(Icons.Rounded.CreateNewFolder, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add folder")
                        }
                        OutlinedButton(onClick = library::rescan, enabled = !state.busy && state.folders.isNotEmpty()) {
                            Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Refresh")
                        }
                        if (state.busy) TextButton(onClick = library::cancel) { Text("Cancel") }
                    }
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    Text("Pull down to refresh or tap Refresh to discover new videos. Files remain in place.", style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All videos", "Episodes", "Other videos")) { name ->
                        FilterChip(category == name, { category = name }, label = { Text(name) })
                    }
                }
            }
            if (videos.isEmpty()) item {
                LocalPanel {
                    Icon(Icons.Rounded.VideoLibrary, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(if (state.videos.isEmpty()) "Make room for your stories" else "No matching videos", style = MaterialTheme.typography.titleLarge)
                    Text(if (state.folders.isEmpty()) "Add your first folder to build your offline library." else "Pull down to refresh or try another search.")
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

@Composable fun LocalFoldersPage(state: LocalLibraryState, library: LocalLibrary, wide: Boolean, select: (String)->Unit) {
    val add = rememberFolderPicker(library)
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var remove by remember { mutableStateOf<LocalFolder?>(null) }
    val active = state.folders.firstOrNull { it.uri == selected } ?: state.folders.firstOrNull()
    val videos = state.videos.filter { it.folder == active?.uri }

    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Folders & Storage", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = add, enabled = !state.busy) { Text("Add folder") }
            OutlinedButton(onClick = library::rescan, enabled = !state.busy && state.folders.isNotEmpty()) {
                Text("Refresh")
            }
            OutlinedButton(onClick = library::fullRescan, enabled = !state.busy && state.folders.isNotEmpty()) {
                Text("Full Rescan")
            }
            if (state.busy) TextButton(onClick = library::cancel) { Text("Cancel") }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (wide) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            FolderChoices(state, active, { selected = it }, { remove = it }, Modifier.weight(0.4f))
            FolderVideos(videos, select, Modifier.weight(0.6f))
        } else Column(Modifier.weight(1f)) {
            FolderChoices(state, active, { selected = it }, { remove = it }, Modifier.fillMaxWidth().heightIn(max = 220.dp))
            FolderVideos(videos, select, Modifier.weight(1f))
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
}

@Composable private fun FolderChoices(state: LocalLibraryState, active: LocalFolder?, select: (String)->Unit, remove: (LocalFolder)->Unit, modifier: Modifier) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        if (state.folders.isEmpty()) item { LocalPanel { Text("No folders connected"); Text("Add a folder using Android's file picker.") } }
        items(state.folders, key = { it.uri }) { folder ->
            Card(onClick = { select(folder.uri) }, colors = CardDefaults.cardColors(containerColor = if (active == folder) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(folder.name, fontWeight = FontWeight.Bold)
                    Text(folder.status, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { remove(folder) }, enabled = !state.busy) { Text("Remove from library") }
                }
            }
        }
    }
}

@Composable private fun FolderVideos(videos: List<LocalVideo>, select: (String)->Unit, modifier: Modifier) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Text("${videos.size} indexed videos", style = MaterialTheme.typography.titleLarge) }
        items(videos, key = { it.uri }) { video -> LocalVideoCard(video) { select(video.uri) } }
    }
}

@Composable private fun LocalVideoCard(video: LocalVideo, select: ()->Unit) {
    Card(onClick = select, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            LocalThumbnail(video, Modifier.size(112.dp, 80.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(video.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (video.duration > 0) timecode(video.duration / 1000) else "Duration unavailable", style = MaterialTheme.typography.bodySmall)
                video.episode?.let { Text("Season ${it.season} · Episode ${it.episode} · filename hint", style = MaterialTheme.typography.labelSmall) }
            }
            Icon(Icons.Rounded.ChevronRight, "View file details")
        }
    }
}

@Composable fun LocalDetailPage(video: LocalVideo, state: LocalLibraryState, playback: PlaybackController) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { LocalThumbnail(video, Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(28.dp))) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(video.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

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
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Text(it, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
        item { LocalPanel {
            Text("Local file", style = MaterialTheme.typography.titleLarge)
            Text(video.name)
            Text("Folder: ${state.folders.firstOrNull { it.uri == video.folder }?.name ?: "Unknown"}")
            Text("Duration: ${if (video.duration > 0) timecode(video.duration / 1000) else "Unavailable"}")
            Text("Size: ${if (video.bytes >= 0) android.text.format.Formatter.formatFileSize(LocalContext.current, video.bytes) else "Unavailable"}")
            Text("Dimensions: ${if (video.width > 0 && video.height > 0) "${video.width} × ${video.height}" else "Unavailable"}")
            Text("Media type: ${video.mime.ifEmpty { "Unavailable" }}")
            video.episode?.let { Text("Filename suggests season ${it.season}, episode ${it.episode}.") }

            val resume = playback.savedPosition(video)
            if (resume > 0) Text("Resume from ${timecode(resume / 1000)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Button(onClick = { playback.open(video) }) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(if (resume > 0) "Resume" else "Play") }
        } }
    }
}

private object ThumbnailMemory {
    val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) { override fun sizeOf(key: String, value: Bitmap) = value.byteCount }
    val permits = Semaphore(2)
}

@Composable private fun LocalThumbnail(video: LocalVideo, modifier: Modifier) {
    val context = LocalContext.current.applicationContext
    val key = "${video.uri}:${video.modified}:${video.bytes}"
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = key) {
        value = null
        value = withContext(Dispatchers.IO) { ThumbnailMemory.permits.withPermit {
            ThumbnailMemory.cache.get(key) ?: run {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, video.uri.toUri())
                    if (Build.VERSION.SDK_INT >= 27) {
                        retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 480, 270)?.also { ThumbnailMemory.cache.put(key, it) }
                    } else null
                } catch (_: Exception) { null } finally { runCatching { retriever.release() } }
            }
        } }
    }
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant) {
        val frame = bitmap
        if (frame != null) Image(frame.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Movie, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary) }
    }
}
