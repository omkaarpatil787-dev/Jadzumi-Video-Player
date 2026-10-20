package com.jadzumi.video

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

@Composable
private fun HeroMovieBanner(
    video: LocalVideo?,
    fallbackTitle: String = "Interstellar",
    fallbackDuration: String = "2h 49m",
    onPlay: () -> Unit,
    onInfo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (video != null) {
                LocalThumbnail(video, Modifier.fillMaxSize())
            } else {
                Image(
                    painter = painterResource(R.drawable.hero),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.94f)
                        )
                    )
                )
            )
            Column(
                Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                val title = video?.title ?: fallbackTitle
                val durStr = if (video != null && video.duration > 0) timecode(video.duration / 1000) else fallbackDuration
                Text(
                    title,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "2024 · $durStr · 4K · Dolby Atmos",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onPlay,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B57D0)),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Surface(
                        onClick = onPlay,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.BookmarkBorder, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Surface(
                        onClick = onInfo,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Info, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(width = 20.dp, height = 4.dp).clip(CircleShape).background(Color.White))
                Box(Modifier.size(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.45f)))
                Box(Modifier.size(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.45f)))
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    video: LocalVideo?,
    previewFallback: PreviewVideo?,
    savedPos: Long,
    durationMs: Long,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(240.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(130.dp)) {
                if (video != null) {
                    LocalThumbnail(video, Modifier.fillMaxSize())
                } else {
                    val res = when (previewFallback?.artwork) {
                        "anime" -> R.drawable.anime
                        "cosmos" -> R.drawable.cosmos
                        else -> R.drawable.hero
                    }
                    Image(
                        painter = painterResource(res),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
                    )
                )
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    modifier = Modifier.align(Alignment.Center).size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                val title = video?.title ?: previewFallback?.title ?: "Video"
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                val durSafe = if (durationMs > 0) durationMs else 1L
                val progress = (savedPos.toDouble() / durSafe).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val posSec = savedPos / 1000
                    val durSec = durationMs / 1000
                    Text(
                        "${timecode(posSec)} / ${timecode(durSec)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        Icons.Rounded.ChevronRight,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryQuickCard(
    title: String,
    countText: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            null,
                            tint = if (selected) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                    Text(countText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun StorageSummaryCard(
    videoCount: Int,
    totalIndexedBytes: Long,
    onAddFolder: () -> Unit
) {
    val context = LocalContext.current
    val stat = remember { runCatching { android.os.StatFs(android.os.Environment.getDataDirectory().path) }.getOrNull() }
    val totalDeviceBytes = stat?.totalBytes ?: (256L * 1024 * 1024 * 1024)
    val freeDeviceBytes = stat?.availableBytes ?: (178L * 1024 * 1024 * 1024)
    val usedDeviceBytes = (totalDeviceBytes - freeDeviceBytes).coerceAtLeast(0)
    val storageRatio = (usedDeviceBytes.toDouble() / totalDeviceBytes.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                    }
                    Column {
                        Text("Internal Storage", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "$videoCount videos · ${Formatter.formatFileSize(context, totalIndexedBytes)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = onAddFolder,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add folder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            LinearProgressIndicator(
                progress = { storageRatio },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Storage: ${Formatter.formatFileSize(context, freeDeviceBytes)} free of ${Formatter.formatFileSize(context, totalDeviceBytes)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(Icons.Rounded.Storage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun LocalLibraryPage(
    state: LocalLibraryState,
    library: LocalLibrary,
    playback: PlaybackController,
    query: String,
    select: (String) -> Unit,
    preview: () -> Unit,
    defaultCategory: String = "All videos"
) {
    val add = rememberFolderPicker(library)
    var category by rememberSaveable { mutableStateOf(defaultCategory) }
    val allVideos = remember(state.videos) { state.videos.distinctBy { it.identity } }

    val moviesCount = remember(allVideos) { allVideos.count { it.episode == null } }
    val showsCount = remember(allVideos) { allVideos.count { it.episode != null } }
    val animeCount = remember(allVideos) { allVideos.count { it.name.contains("anime", true) || it.folder.contains("anime", true) } }
    val recentCount = remember(allVideos) { allVideos.size }

    val videos = remember(allVideos, query, category) {
        allVideos.filter {
            it.name.contains(query.trim(), true) && when (category) {
                "Movies" -> it.episode == null
                "TV Shows" -> it.episode != null
                "Anime" -> it.name.contains("anime", true) || it.folder.contains("anime", true)
                "Recently Added" -> true
                else -> true
            }
        }.let { list ->
            if (category == "Recently Added") list.sortedByDescending { it.modified } else list
        }
    }

    val continueWatching = remember(allVideos, playback.historyVersion) {
        allVideos.filter { playback.savedPosition(it) > 0 }
    }

    val featuredVideo = remember(continueWatching, allVideos) {
        continueWatching.firstOrNull() ?: allVideos.firstOrNull()
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
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // 1. Hero Movie Banner
                item {
                    HeroMovieBanner(
                        video = featuredVideo,
                        fallbackTitle = "Interstellar",
                        fallbackDuration = "2h 49m",
                        onPlay = {
                            if (featuredVideo != null) {
                                playback.open(featuredVideo)
                                playback.fullscreen = true
                            } else {
                                preview()
                            }
                        },
                        onInfo = {
                            if (featuredVideo != null) {
                                select(featuredVideo.uri)
                            } else {
                                preview()
                            }
                        }
                    )
                }

                // 2. Continue Watching Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Continue Watching", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            if (continueWatching.size > 1) {
                                Text(
                                    "See all >",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable { category = "Recently Added" }
                                )
                            }
                        }
                        val itemsToWatch = if (continueWatching.isNotEmpty()) continueWatching else allVideos.take(4)
                        if (itemsToWatch.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(itemsToWatch, key = { it.uri }) { v ->
                                    val pos = playback.savedPosition(v)
                                    val dur = v.duration.takeIf { it > 0 } ?: 1000L
                                    ContinueWatchingCard(
                                        video = v,
                                        previewFallback = null,
                                        savedPos = pos,
                                        durationMs = dur,
                                        onClick = {
                                            playback.open(v)
                                            playback.fullscreen = true
                                        }
                                    )
                                }
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(previewVideos, key = { it.id }) { pv ->
                                    ContinueWatchingCard(
                                        video = null,
                                        previewFallback = pv,
                                        savedPos = pv.positionSeconds * 1000,
                                        durationMs = pv.durationSeconds * 1000,
                                        onClick = preview
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Category Quick Filter Cards Row
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryQuickCard(
                            title = "Recently Added",
                            countText = "$recentCount videos",
                            icon = Icons.Rounded.AccessTime,
                            selected = category == "Recently Added",
                            onClick = { category = if (category == "Recently Added") "All videos" else "Recently Added" },
                            modifier = Modifier.weight(1f)
                        )
                        CategoryQuickCard(
                            title = "Movies",
                            countText = "$moviesCount videos",
                            icon = Icons.Rounded.Movie,
                            selected = category == "Movies",
                            onClick = { category = if (category == "Movies") "All videos" else "Movies" },
                            modifier = Modifier.weight(1f)
                        )
                        CategoryQuickCard(
                            title = "TV Shows",
                            countText = "$showsCount shows",
                            icon = Icons.Rounded.Tv,
                            selected = category == "TV Shows",
                            onClick = { category = if (category == "TV Shows") "All videos" else "TV Shows" },
                            modifier = Modifier.weight(1f)
                        )
                        CategoryQuickCard(
                            title = "Anime",
                            countText = "$animeCount shows",
                            icon = Icons.Rounded.Bolt,
                            selected = category == "Anime",
                            onClick = { category = if (category == "Anime") "All videos" else "Anime" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Videos List / Empty State
                if (videos.isEmpty()) {
                    item {
                        LocalPanel {
                            Icon(Icons.Rounded.VideoLibrary, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                if (allVideos.isEmpty()) "Make room for your stories" else "No matching videos",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (state.folders.isEmpty()) "Connect a folder using Android's file picker to populate your library."
                                else "Pull down to refresh or tap another category.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (state.folders.isEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Button(onClick = add, shape = RoundedCornerShape(20.dp)) {
                                    Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add your first folder")
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (category == "All videos") "All Indexed Videos (${videos.size})" else "$category (${videos.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (category != "All videos") {
                                TextButton(onClick = { category = "All videos" }) { Text("Show all") }
                            }
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
                StorageSummaryCard(
                    videoCount = state.videos.size,
                    totalIndexedBytes = state.videos.sumOf { it.bytes.coerceAtLeast(0) },
                    onAddFolder = add
                )
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
                    StorageSummaryCard(
                    videoCount = state.videos.size,
                    totalIndexedBytes = state.videos.sumOf { it.bytes.coerceAtLeast(0) },
                    onAddFolder = add
                )
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

@Composable internal fun LocalThumbnail(video: LocalVideo, modifier: Modifier) {
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
