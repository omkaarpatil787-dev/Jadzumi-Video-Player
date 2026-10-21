package com.jadzumi.video

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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

@Composable
fun WelcomeOnboardingCard(
    onAddFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Text(
                "Welcome to Jadzumi Player",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                "100% Offline-First Cinema Player for your phone and tablet. Connect a folder on your device (e.g. Movies, TV Shows, or Downloads) to scan and play your media with hardware acceleration and automatic artwork detection.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )
            Button(
                onClick = onAddFolder,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Select Video Folder", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable private fun LocalPanel(content: @Composable ColumnScope.() -> Unit) {
    val glassIntensity = LocalGlassIntensity.current
    val theme = LocalThemeName.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = GlassTheme.embossedBorderStroke(theme, glassIntensity),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassTheme.frostedSurfaceColor(theme, glassIntensity))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

private data class HeroItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String? = null,
    val localVideo: LocalVideo? = null,
    val previewArtwork: String = "hero",
    val onPlay: () -> Unit,
    val onInfo: () -> Unit
)

@Composable
private fun HeroMoviePager(
    items: List<HeroItem>,
    isPhone: Boolean = false,
    isMinimized: Boolean = false
) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { items.size })

    val fullHeight = if (isPhone) 220.dp else 260.dp
    val minHeight = if (isPhone) 74.dp else 84.dp

    val animatedHeight by animateDpAsState(
        targetValue = if (isMinimized) minHeight else fullHeight,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "heroHeight"
    )
    val animatedCorners by animateDpAsState(
        targetValue = if (isMinimized) 20.dp else 26.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "heroCorners"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight),
        shape = RoundedCornerShape(animatedCorners),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        AnimatedContent(
            targetState = isMinimized,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "heroContentMode",
            modifier = Modifier.fillMaxSize()
        ) { minimized ->
            Box(Modifier.fillMaxSize()) {
                if (minimized) {
                    val currentItem = items[pagerState.currentPage.coerceIn(0, items.size - 1)]
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(if (isPhone) 80.dp else 100.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            if (currentItem.localVideo != null) {
                                LocalThumbnail(currentItem.localVideo, Modifier.fillMaxSize())
                            } else {
                                val res = when (currentItem.previewArtwork) {
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
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (currentItem.badge != null) {
                                Text(
                                    currentItem.badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                            Text(
                                currentItem.title,
                                color = Color.White,
                                fontSize = if (isPhone) 15.sp else 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                currentItem.subtitle,
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            onClick = currentItem.onPlay,
                            shape = CircleShape,
                            color = Color(0xFF0B57D0),
                            modifier = Modifier.size(if (isPhone) 40.dp else 44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val item = items[page]
                        Box(Modifier.fillMaxSize()) {
                            if (item.localVideo != null) {
                                LocalThumbnail(item.localVideo, Modifier.fillMaxSize())
                            } else {
                                val res = when (item.previewArtwork) {
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
                                if (item.badge != null) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF0B57D0).copy(alpha = 0.85f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            item.badge,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Text(
                                    item.title,
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    item.subtitle,
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
                                        onClick = item.onPlay,
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B57D0)),
                                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Surface(
                                        onClick = item.onPlay,
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
                                        onClick = item.onInfo,
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
                        }
                    }

                    if (items.size > 1) {
                        Row(
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(items.size) { idx ->
                                val isSelected = pagerState.currentPage == idx
                                val dotWidth by animateDpAsState(
                                    targetValue = if (isSelected) 22.dp else 5.dp,
                                    label = "dotWidth"
                                )
                                val dotAlpha by animateFloatAsState(
                                    targetValue = if (isSelected) 1f else 0.45f,
                                    label = "dotAlpha"
                                )
                                Box(
                                    Modifier
                                        .size(width = dotWidth, height = 5.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = dotAlpha))
                                )
                            }
                        }
                    }
                }
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
    val theme = LocalThemeName.current
    val glassIntensity = LocalGlassIntensity.current
    Surface(
        onClick = onClick,
        modifier = Modifier.width(250.dp),
        shape = RoundedCornerShape(20.dp),
        color = GlassTheme.frostedSurfaceColor(theme, glassIntensity),
        border = GlassTheme.embossedBorderStroke(theme, glassIntensity)
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

                // Remaining time pill
                val remMs = (durationMs - savedPos).coerceAtLeast(0)
                val remMins = (remMs / 60_000L).coerceAtLeast(1)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.70f),
                    modifier = Modifier.padding(8.dp).align(Alignment.BottomEnd)
                ) {
                    Text(
                        "${remMins}m left",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
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
                    color = TimelineAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
        BoxWithConstraints(Modifier.fillMaxWidth().padding(18.dp)) {
            val compact = maxWidth < 460.dp
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (compact) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        Column(Modifier.weight(1f)) {
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
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Folder", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
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
}

/**
 * Smart multi-word search matcher that normalizes punctuation, symbols, and whitespace.
 * Handles filename formats with dots, underscores, hyphens, and brackets.
 * Returns true if every query word matches at least one target token.
 */
fun matchesQuery(query: String, vararg targets: String?): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true

    val queryTokens = q.lowercase().split(Regex("[\\s._\\-]+")).filter { it.isNotEmpty() }
    if (queryTokens.isEmpty()) return true

    val normalizedTarget = targets.filterNotNull().joinToString(" ") { text ->
        text.replace('.', ' ')
            .replace('_', ' ')
            .replace('-', ' ')
            .replace('+', ' ')
            .replace(':', ' ')
            .replace('/', ' ')
            .replace('\\', ' ')
            .replace('(', ' ')
            .replace(')', ' ')
            .replace('[', ' ')
            .replace(']', ' ')
            .replace('{', ' ')
            .replace('}', ' ')
            .lowercase()
    }

    return queryTokens.all { token -> normalizedTarget.contains(token) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun LocalLibraryPage(
    state: LocalLibraryState,
    library: LocalLibrary,
    playback: PlaybackController,
    query: String,
    select: (String) -> Unit,
    preview: () -> Unit = {},
    defaultCategory: String = "All videos",
    onScrollActive: ((Boolean) -> Unit)? = null
) {
    val add = rememberFolderPicker(library)
    val listState = rememberLazyListState()
    val isHeroMinimized by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 60
        }
    }
    val shouldMinimizeDock by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 100
        }
    }
    LaunchedEffect(shouldMinimizeDock) {
        onScrollActive?.invoke(shouldMinimizeDock)
    }
    var category by rememberSaveable(defaultCategory) { mutableStateOf(defaultCategory) }
    var isGridView by rememberSaveable { mutableStateOf(false) }
    var activeFilter by rememberSaveable { mutableStateOf("All") }
    var activeSort by rememberSaveable { mutableStateOf("Date Added") }
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    val selectedUris = remember { mutableStateListOf<String>() }

    BackHandler(isSelectionMode) {
        isSelectionMode = false
        selectedUris.clear()
    }

    val allVideos = remember(state.videos) { state.videos.distinctBy { it.identity } }

    val seriesList = remember(state.videos, state.sidecars) { state.tvSeries }
    val moviesCount = remember(allVideos) { allVideos.count { it.episode == null } }
    val showsCount = remember(seriesList, allVideos) { seriesList.size.takeIf { it > 0 } ?: allVideos.count { it.episode != null } }
    val animeCount = remember(allVideos) { allVideos.count { it.name.contains("anime", true) || it.folder.contains("anime", true) } }
    val recentCount = remember(allVideos) { allVideos.size }

    val isSearching = query.isNotBlank()

    val searchSeries = remember(seriesList, query) {
        if (!isSearching) emptyList()
        else {
            seriesList.filter { series ->
                val epNames = series.seasons.values.flatten().map { it.name }
                matchesQuery(
                    query,
                    series.title,
                    series.studio,
                    series.synopsis,
                    series.genres.joinToString(" "),
                    series.folderUri,
                    *epNames.toTypedArray()
                )
            }
        }
    }

    val searchMovies = remember(allVideos, query) {
        if (!isSearching) emptyList()
        else {
            allVideos.filter {
                it.episode == null && matchesQuery(query, it.name, it.title, it.folder, it.parentDir)
            }
        }
    }

    val searchEpisodes = remember(allVideos, query) {
        if (!isSearching) emptyList()
        else {
            allVideos.filter {
                it.episode != null && matchesQuery(
                    query,
                    it.name,
                    it.title,
                    it.folder,
                    it.parentDir,
                    EpisodePatterns.parse(it.name)?.seriesTitle,
                    EpisodePatterns.parse(it.name)?.episodeTitle
                )
            }
        }
    }

    val totalSearchResults = searchSeries.size + searchMovies.size + searchEpisodes.size

    val candidateVideos = remember(allVideos, category) {
        when (category) {
            "Movies" -> allVideos.filter { it.episode == null }
            "TV Shows" -> allVideos.filter { it.episode != null }
            else -> allVideos
        }
    }

    val videos = remember(allVideos, query, category) {
        candidateVideos.filter {
            matchesQuery(query, it.name, it.title, it.folder) && when (category) {
                "Anime" -> it.name.contains("anime", true) || it.folder.contains("anime", true)
                "Recently Added" -> true
                else -> true
            }
        }.let { list ->
            if (category == "Recently Added") list.sortedByDescending { it.modified } else list
        }
    }

    val displayVideos = remember(videos, activeFilter, activeSort, playback.historyVersion) {
        val filtered = when (activeFilter) {
            "In Progress" -> videos.filter { playback.isInProgress(it) }
            "Unwatched" -> videos.filter { playback.savedPosition(it) == 0L }
            "4K UHD" -> videos.filter { it.width >= 3840 || it.height >= 2160 || it.name.contains("2160", true) || it.name.contains("4k", true) || it.name.contains("uhd", true) }
            "Recently Added" -> videos.sortedByDescending { it.modified }
            else -> videos
        }
        when (activeSort) {
            "Title (A-Z)" -> filtered.sortedBy { it.title.lowercase() }
            "Duration" -> filtered.sortedByDescending { it.duration }
            "File Size" -> filtered.sortedByDescending { it.bytes }
            else -> if (activeFilter == "Recently Added" || category == "Recently Added") filtered else filtered.sortedByDescending { it.modified }
        }
    }

    val filteredSeries = remember(seriesList, query) {
        seriesList.filter { matchesQuery(query, it.title, it.studio, it.synopsis) }
    }

    val continueWatching = remember(candidateVideos, playback.historyVersion) {
        candidateVideos.filter { playback.savedPosition(it) > 0 }
    }

    val isHomeTab = category == "All videos" || category == "Home"
    val heroItems = remember(candidateVideos, continueWatching, seriesList, category, playback.historyVersion) {
        if (!isHomeTab || candidateVideos.isEmpty()) {
            emptyList()
        } else {
            val suggestions = mutableListOf<HeroItem>()

            // 1. Real-time "Up Next" episode for active series
            val activeSeries = seriesList.filter { s ->
                s.seasons.values.flatten().any { playback.savedPosition(it) > 0 }
            }
            activeSeries.forEach { series ->
                val nextEp = series.nextUnwatchedEpisode(playback)
                if (nextEp != null && suggestions.size < 2) {
                    val epInfo = EpisodePatterns.parse(nextEp.name)
                    val sNum = epInfo?.season ?: 1
                    val eNum = epInfo?.episode ?: 1
                    suggestions.add(
                        HeroItem(
                            id = nextEp.uri,
                            title = series.title,
                            subtitle = "Up Next: S%02dE%02d · %s".format(sNum, eNum, epInfo?.episodeTitle ?: nextEp.title),
                            badge = "▶ UP NEXT · S%02dE%02d".format(sNum, eNum),
                            localVideo = nextEp,
                            onPlay = {
                                playback.open(nextEp)
                                playback.fullscreen = true
                            },
                            onInfo = {
                                select("series:${series.id}")
                            }
                        )
                    )
                }
            }

            // 2. Real-time Continue Watching in-progress movie
            continueWatching.filter { it.episode == null }.take(2).forEach { movie ->
                if (suggestions.none { it.id == movie.uri } && suggestions.size < 4) {
                    val pos = playback.savedPosition(movie)
                    val dur = movie.duration.takeIf { it > 0 } ?: 1000L
                    suggestions.add(
                        HeroItem(
                            id = movie.uri,
                            title = movie.title,
                            subtitle = "Resume from ${timecode(pos / 1000)} · ${timecode((dur - pos).coerceAtLeast(0) / 1000)} left",
                            badge = "⏱ RESUME WATCHING",
                            localVideo = movie,
                            onPlay = {
                                playback.open(movie)
                                playback.fullscreen = true
                            },
                            onInfo = {
                                select(movie.uri)
                            }
                        )
                    )
                }
            }

            // 3. Unwatched spotlight recommendations from library
            candidateVideos.filter { playback.savedPosition(it) == 0L }.take(3).forEach { video ->
                if (suggestions.none { it.id == video.uri } && suggestions.size < 6) {
                    val epInfo = EpisodePatterns.parse(video.name)
                    val durStr = if (video.duration > 0) timecode(video.duration / 1000) else "Feature Film"
                    val resStr = if (video.width > 0 && video.height > 0) "${video.width}×${video.height}" else "1080p FHD"
                    suggestions.add(
                        HeroItem(
                            id = video.uri,
                            title = epInfo?.seriesTitle ?: video.title,
                            subtitle = "$durStr · $resStr · Dolby Audio",
                            badge = if (epInfo != null) "✨ TV SERIES SPOTLIGHT" else "★ RECOMMENDED CINEMA",
                            localVideo = video,
                            onPlay = {
                                playback.open(video)
                                playback.fullscreen = true
                            },
                            onInfo = {
                                if (epInfo != null) {
                                    val matched = seriesList.find { it.title.equals(epInfo.seriesTitle, ignoreCase = true) }
                                    if (matched != null) select("series:${matched.id}") else select(video.uri)
                                } else {
                                    select(video.uri)
                                }
                            }
                        )
                    )
                }
            }

            suggestions
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenMaxWidth = maxWidth
        val pad = if (maxWidth >= 720.dp) 28.dp else 18.dp
        val isPhone = maxWidth < 600.dp
        val bottomPad = if (isPhone) 88.dp else 96.dp
        PullToRefreshBox(
            isRefreshing = state.busy,
            onRefresh = { library.rescan() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .animatedScrollBlendFade(listState),
                contentPadding = PaddingValues(start = pad, end = pad, top = 12.dp, bottom = bottomPad),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                if (isSearching) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Search Results",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (totalSearchResults == 0) "No matching videos or series found"
                                else "$totalSearchResults result${if (totalSearchResults == 1) "" else "s"} for \"$query\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (totalSearchResults == 0) {
                        item {
                            LocalPanel {
                                Icon(Icons.Rounded.Search, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("No Results Found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    "No titles, series, or video files matched \"$query\". Check your spelling or try searching for another word.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        if (category == "TV Shows") {
                            if (searchSeries.isNotEmpty()) {
                                item {
                                    Text("TV Series (${searchSeries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                item {
                                    TvSeriesCardGrid(seriesList = searchSeries, playback = playback, onSelect = { select("series:${it.id}") })
                                }
                            }
                            if (searchEpisodes.isNotEmpty()) {
                                item {
                                    Text("Episodes (${searchEpisodes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                items(searchEpisodes, key = { it.uri }) { video ->
                                    LocalVideoCard(video) { select(video.uri) }
                                }
                            }
                            if (searchMovies.isNotEmpty()) {
                                item {
                                    Text("Movies (${searchMovies.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                item {
                                    MovieCardGrid(movies = searchMovies, sidecars = state.sidecars, playback = playback, onSelect = { select(it.uri) })
                                }
                            }
                        } else if (category == "Movies") {
                            if (searchMovies.isNotEmpty()) {
                                item {
                                    Text("Movies (${searchMovies.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                item {
                                    MovieCardGrid(movies = searchMovies, sidecars = state.sidecars, playback = playback, onSelect = { select(it.uri) })
                                }
                            }
                            if (searchSeries.isNotEmpty()) {
                                item {
                                    Text("TV Series (${searchSeries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                item {
                                    TvSeriesCardGrid(seriesList = searchSeries, playback = playback, onSelect = { select("series:${it.id}") })
                                }
                            }
                            if (searchEpisodes.isNotEmpty()) {
                                item {
                                    Text("Other Videos (${searchEpisodes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                items(searchEpisodes, key = { it.uri }) { video ->
                                    LocalVideoCard(video) { select(video.uri) }
                                }
                            }
                        } else {
                            if (searchSeries.isNotEmpty()) {
                                item {
                                    Text("TV Series (${searchSeries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                item {
                                    TvSeriesCardGrid(seriesList = searchSeries, playback = playback, onSelect = { select("series:${it.id}") })
                                }
                            }
                            if (searchMovies.isNotEmpty()) {
                                item {
                                    Text("Movies (${searchMovies.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                item {
                                    MovieCardGrid(movies = searchMovies, sidecars = state.sidecars, playback = playback, onSelect = { select(it.uri) })
                                }
                            }
                            if (searchEpisodes.isNotEmpty()) {
                                item {
                                    Text("Episodes & Videos (${searchEpisodes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                items(searchEpisodes, key = { it.uri }) { video ->
                                    LocalVideoCard(video) { select(video.uri) }
                                }
                            }
                        }
                    }
                } else {
                    // 0. Welcome Onboarding (shown if no video folders connected yet)
                    if (state.folders.isEmpty()) {
                        item {
                            WelcomeOnboardingCard(onAddFolder = add)
                        }
                    }

                    // 1. Hero Movie Banner Pager (only shown on Home tab and if real indexed videos exist)
                    if (heroItems.isNotEmpty() && (category == "All videos" || category == "Home")) {
                        item {
                            HeroMoviePager(
                                items = heroItems,
                                isPhone = isPhone,
                                isMinimized = isHeroMinimized
                            )
                        }
                    }

                    // 2. Continue Watching Section (only real in-progress videos)
                    if (continueWatching.isNotEmpty()) {
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
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(continueWatching, key = { it.uri }) { v ->
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
                            }
                        }
                    }

                    if (category == "TV Shows") {
                        if (filteredSeries.isEmpty()) {
                            item {
                                LocalPanel {
                                    Icon(Icons.Rounded.Tv, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text("No TV Series Found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Videos with episode naming patterns (e.g. S01E01, 1x04, Episode 12) will automatically be organized into dedicated series pages.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                        "TV Series (${filteredSeries.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (defaultCategory == "All videos") {
                                        TextButton(onClick = { category = "All videos" }) { Text("Show all") }
                                    }
                                }
                            }

                            item {
                                TvSeriesCardGrid(
                                    seriesList = filteredSeries,
                                    playback = playback,
                                    onSelect = { select("series:${it.id}") }
                                )
                            }
                        }
                    } else if (category == "Movies") {
                        val movieList = videos.filter { it.episode == null }
                        if (movieList.isEmpty()) {
                            item {
                                LocalPanel {
                                    Icon(Icons.Rounded.Movie, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text("No Movies Found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Standalone feature films stored in your connected folders will appear here.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                        "Movies (${movieList.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (defaultCategory == "All videos") {
                                        TextButton(onClick = { category = "All videos" }) { Text("Show all") }
                                    }
                                }
                            }

                            item {
                                MovieCardGrid(
                                    movies = movieList,
                                    sidecars = state.sidecars,
                                    playback = playback,
                                    onSelect = { select(it.uri) }
                                )
                            }
                        }
                    } else {
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
                            if (isSelectionMode) {
                                item {
                                    val theme = LocalThemeName.current
                                    val glassIntensity = LocalGlassIntensity.current
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        color = GlassTheme.frostedSurfaceColor(theme, glassIntensity),
                                        border = GlassTheme.embossedBorderStroke(theme, glassIntensity)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                IconButton(
                                                    onClick = { isSelectionMode = false; selectedUris.clear() },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Close, "Exit selection", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                                                }
                                                Text(
                                                    "${selectedUris.size} selected",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                val selectedVideos = remember(selectedUris.toList(), displayVideos) {
                                                    displayVideos.filter { it.uri in selectedUris }
                                                }
                                                TextButton(
                                                    onClick = {
                                                        if (selectedUris.size == displayVideos.size) selectedUris.clear()
                                                        else {
                                                            selectedUris.clear()
                                                            selectedUris.addAll(displayVideos.map { it.uri })
                                                        }
                                                    }
                                                ) {
                                                    Text(if (selectedUris.size == displayVideos.size) "Deselect" else "Select All", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        playback.markVideosWatched(selectedVideos, true)
                                                        isSelectionMode = false
                                                        selectedUris.clear()
                                                    },
                                                    enabled = selectedVideos.isNotEmpty()
                                                ) {
                                                    Text("Watched", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        playback.clearProgress(selectedVideos)
                                                        isSelectionMode = false
                                                        selectedUris.clear()
                                                    },
                                                    enabled = selectedVideos.isNotEmpty()
                                                ) {
                                                    Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Smart Filter Chips and Grid / List toggle
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LazyRow(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val filterChips = listOf("All", "In Progress", "Unwatched", "4K UHD", "Recently Added")
                                        items(filterChips) { f ->
                                            val isSelected = activeFilter == f
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                                modifier = Modifier.clickable { activeFilter = f }
                                            ) {
                                                Text(
                                                    text = f,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        IconButton(onClick = { isGridView = !isGridView }, modifier = Modifier.fillMaxSize()) {
                                            Icon(
                                                if (isGridView) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                                                contentDescription = "Toggle Grid or List View",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (category == "All videos") "All Indexed Videos (${displayVideos.size})" else "$category (${displayVideos.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (category != "All videos") {
                                        TextButton(onClick = { category = "All videos" }) { Text("Show all") }
                                    }
                                }
                            }

                            if (isGridView) {
                                val colCount = if (isPhone) 2 else if (screenMaxWidth >= 900.dp) 4 else 3
                                items(displayVideos.chunked(colCount), key = { chunk -> chunk.joinToString { it.uri } }) { chunk ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        for (video in chunk) {
                                            Box(Modifier.weight(1f)) {
                                                LocalPosterGridCard(
                                                    video = video,
                                                    isSelectionMode = isSelectionMode,
                                                    isSelected = video.uri in selectedUris,
                                                    playback = playback,
                                                    onClick = {
                                                        if (isSelectionMode) {
                                                            if (video.uri in selectedUris) selectedUris.remove(video.uri)
                                                            else selectedUris.add(video.uri)
                                                        } else {
                                                            select(video.uri)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (!isSelectionMode) {
                                                            isSelectionMode = true
                                                            selectedUris.add(video.uri)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        val fillSlots = colCount - chunk.size
                                        for (i in 0 until fillSlots) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else {
                                items(displayVideos, key = { it.uri }) { video ->
                                    LocalVideoCard(
                                        video = video,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = video.uri in selectedUris,
                                        onLongClick = {
                                            if (!isSelectionMode) {
                                                isSelectionMode = true
                                                selectedUris.add(video.uri)
                                            }
                                        },
                                        select = {
                                            if (isSelectionMode) {
                                                if (video.uri in selectedUris) selectedUris.remove(video.uri)
                                                else selectedUris.add(video.uri)
                                            } else {
                                                select(video.uri)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
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
                    Box(Modifier.weight(0.62f).fillMaxHeight()) {
                        AnimatedContent(
                            targetState = active?.uri,
                            transitionSpec = {
                                (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 10 })
                                    .togetherWith(fadeOut(tween(140)))
                            },
                            label = "folderVideosTwoPane"
                        ) { _ ->
                            LazyColumn(
                                Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                item {
                                    Text("${videos.size} indexed videos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                                items(videos, key = { "${active?.uri ?: ""}_${it.uri}" }) { video ->
                                    LocalVideoCard(video, onDelete = { videoToDelete = video }) { select(video.uri) }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Single-column stacked layout for portrait / split-screen so video rows have 100% width
            val isPhone = maxWidth < 600.dp
            val bottomPad = if (isPhone) 88.dp else 96.dp
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = pad, end = pad, top = pad, bottom = bottomPad),
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
                items(videos, key = { "${active?.uri ?: ""}_${it.uri}" }) { video ->
                    LocalVideoCard(video, onDelete = { videoToDelete = video }) { select(video.uri) }
                }
            }
        }
    }

    remove?.let { folder ->
        AlertDialog(
            onDismissRequest = { remove = null },
            title = { Text("Remove ${folder.name}?") },
            text = { Text("Remove this folder from Jadzumi Player. The videos on your storage will remain unchanged.") },
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
            text = { Text("This will permanently delete \"${video.name}\" ($sizeStr) from your device's internal storage. This action cannot be undone.") },
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
@Composable internal fun FolderSummaryCard(
    folder: LocalFolder,
    state: LocalLibraryState,
    isActive: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val folderVideos = remember(state.videos, folder.uri) { state.videos.filter { it.folder == folder.uri } }
    val folderBytes = remember(folderVideos) { folderVideos.sumOf { it.bytes.coerceAtLeast(0) } }
    val borderColor by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(220),
        label = "folderBorder"
    )
    val containerColor by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(220),
        label = "folderContainer"
    )
    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(if (isActive) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor)
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

@Composable
fun MovieCardGrid(
    movies: List<LocalVideo>,
    sidecars: Map<String, List<LocalSidecar>>,
    playback: PlaybackController,
    onSelect: (LocalVideo) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= 1000.dp -> 4
            maxWidth >= 680.dp -> 3
            else -> 2
        }
        val rows = movies.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rows.forEach { rowMovies ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    rowMovies.forEach { movie ->
                        Box(Modifier.weight(1f)) {
                            MovieSummaryCard(
                                video = movie,
                                sidecars = sidecars,
                                playback = playback,
                                onClick = { onSelect(movie) }
                            )
                        }
                    }
                    val missing = columns - rowMovies.size
                    repeat(missing) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun MovieSummaryCard(
    video: LocalVideo,
    sidecars: Map<String, List<LocalSidecar>>,
    playback: PlaybackController,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val metadata = remember(video, sidecars) {
        extractMovieMetadata(video, sidecars, context)
    }
    val savedPos = playback.savedPosition(video)
    val dur = video.duration.takeIf { it > 0 } ?: 1000L
    val isWatched = savedPos >= dur * 0.9f
    val inProgress = savedPos in 1 until (dur * 0.9f).toLong()
    val progress = (savedPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

    val theme = LocalThemeName.current
    val glassIntensity = LocalGlassIntensity.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = GlassTheme.embossedBorderStroke(theme, glassIntensity)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassTheme.frostedSurfaceColor(theme, glassIntensity))
        ) {
            // Poster / Backdrop Thumbnail Container (16:10 aspect ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                SidecarImage(
                    uri = metadata.posterUri ?: metadata.backdropUri,
                    fallbackVideo = video,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Quality / Codec Badge - Uniform Capsule Pill
                val badge = when {
                    video.width >= 3840 -> "4K UHD"
                    video.width >= 1920 -> "1080p"
                    video.name.contains("hevc", true) || video.name.contains("x265", true) -> "HEVC"
                    else -> "HD"
                }
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.70f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Text(
                        badge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Top Left Rating Badge - Uniform Capsule Pill
                metadata.rating?.let { rating ->
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.70f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Rounded.Star, null, Modifier.size(11.dp), tint = Color(0xFFF9AB00))
                            Text(rating, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Progress Line at bottom of thumbnail
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(progress)
                            .height(3.5.dp)
                            .background(if (isWatched) Color(0xFF1E8E3E) else MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Info Section
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = metadata.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val durStr = if (video.duration > 0) timecode(video.duration / 1000) else "Feature Film"
                val sizeStr = if (video.bytes >= 0) Formatter.formatFileSize(context, video.bytes) else ""
                Text(
                    text = listOfNotNull(metadata.year, durStr, sizeStr.takeIf { it.isNotEmpty() }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isWatched) {
                        Icon(Icons.Rounded.CheckCircle, null, Modifier.size(13.dp), tint = Color(0xFF1E8E3E))
                        Text("Watched", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E8E3E))
                    } else if (inProgress) {
                        Text("Resume ${timecode(savedPos / 1000)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text(metadata.genres.firstOrNull() ?: "Movie", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
fun TvSeriesCardGrid(
    seriesList: List<TvSeries>,
    playback: PlaybackController,
    onSelect: (TvSeries) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= 1000.dp -> 4
            maxWidth >= 680.dp -> 3
            else -> 2
        }
        val rows = seriesList.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rows.forEach { rowSeries ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    rowSeries.forEach { series ->
                        Box(Modifier.weight(1f)) {
                            TvSeriesSummaryCard(
                                series = series,
                                playback = playback,
                                onClick = { onSelect(series) }
                            )
                        }
                    }
                    val missing = columns - rowSeries.size
                    repeat(missing) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun TvSeriesSummaryCard(
    series: TvSeries,
    playback: PlaybackController,
    onClick: () -> Unit
) {
    val watched = remember(series, playback.historyVersion) { series.watchedCount(playback) }
    val total = series.totalEpisodes
    val progress = if (total > 0) (watched.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    val theme = LocalThemeName.current
    val glassIntensity = LocalGlassIntensity.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = GlassTheme.embossedBorderStroke(theme, glassIntensity)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassTheme.frostedSurfaceColor(theme, glassIntensity))
        ) {
            // Poster / Backdrop Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                val firstEp = series.seasons.values.firstOrNull()?.firstOrNull()
                SidecarImage(
                    uri = series.posterUri ?: series.backdropUri,
                    fallbackVideo = firstEp,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Left Rating Badge - Uniform Capsule Pill
                series.rating?.let { rating ->
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.70f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Rounded.Star, null, Modifier.size(11.dp), tint = Color(0xFFF9AB00))
                            Text(rating, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Seasons badge - Uniform Capsule Pill
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.70f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Text(
                        "${series.seasonCount} Season${if (series.seasonCount > 1) "s" else ""}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Progress Line at bottom of thumbnail
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(progress)
                            .height(3.5.dp)
                            .background(if (progress >= 1f) Color(0xFF1E8E3E) else MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Info Section
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = series.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${series.totalEpisodes} Episodes · ${series.year ?: "2024"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (progress >= 1f) {
                        Icon(Icons.Rounded.CheckCircle, null, Modifier.size(13.dp), tint = Color(0xFF1E8E3E))
                        Text("All Watched", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E8E3E))
                    } else if (watched > 0) {
                        Text("$watched/$total Watched", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("Unwatched", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun LocalVideoCard(
    video: LocalVideo,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    select: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalThemeName.current
    val glassIntensity = LocalGlassIntensity.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                onClick = select,
                onLongClick = onLongClick
            ),
        color = GlassTheme.frostedSurfaceColor(theme, glassIntensity),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else GlassTheme.embossedBorderStroke(theme, glassIntensity),
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
                if (isSelectionMode) {
                    Icon(
                        if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected" else "Unselected",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                }

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
                    if (onDelete != null && !isSelectionMode) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.DeleteOutline, "Delete video from storage", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (!isSelectionMode) {
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocalPosterGridCard(
    video: LocalVideo,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    playback: PlaybackController,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalThemeName.current
    val glassIntensity = LocalGlassIntensity.current
    val savedPos = playback.savedPosition(video)
    val dur = video.duration.takeIf { it > 0 } ?: 1000L
    val isWatched = savedPos >= dur * 0.9f
    val inProgress = savedPos in 1 until (dur * 0.9f).toLong()
    val progress = (savedPos.toDouble() / dur).toFloat().coerceIn(0f, 1f)
    val is4k = video.width >= 3840 || video.height >= 2160 || video.name.contains("2160", true) || video.name.contains("4k", true) || video.name.contains("uhd", true)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = GlassTheme.frostedSurfaceColor(theme, glassIntensity),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else GlassTheme.embossedBorderStroke(theme, glassIntensity),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                LocalThumbnail(video = video, modifier = Modifier.fillMaxSize())

                // 4K Badge top-left
                if (is4k) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.padding(6.dp).align(Alignment.TopStart)
                    ) {
                        Text(
                            "4K UHD",
                            color = TimelineAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Selection checkmark top-right
                if (isSelectionMode) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, Color.White),
                        modifier = Modifier.padding(6.dp).size(22.dp).align(Alignment.TopEnd)
                    ) {
                        if (isSelected) {
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Timecode bottom-right
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.70f),
                    modifier = Modifier.padding(6.dp).align(Alignment.BottomEnd)
                ) {
                    Text(
                        if (video.duration > 0) timecode(video.duration / 1000) else "--:--",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // In-progress / watched bar
                if (inProgress || isWatched) {
                    LinearProgressIndicator(
                        progress = { if (isWatched) 1f else progress },
                        modifier = Modifier.fillMaxWidth().height(3.5.dp).align(Alignment.BottomCenter),
                        color = if (isWatched) Color(0xFF1E8E3E) else MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sizeText = if (video.bytes >= 0) Formatter.formatFileSize(context, video.bytes) else ""
                Text(
                    text = sizeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1
                )
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
            text = { Text("This will permanently delete \"${video.name}\" ($sizeStr) from your device's internal storage. This action cannot be undone.") },
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

@Composable
internal fun CinematicFallbackPoster(title: String, name: String, modifier: Modifier = Modifier) {
    val cleanTitle = remember(title, name) {
        val t = if (title.isNotBlank()) title else name.substringBeforeLast('.')
        t.replace(Regex("(?i)\\b(1080p|2160p|4k|uhd|hdr|hdr10|bluray|hevc|x265|x264|dvdrip|web-dl|webrip|ddp|aac|remux)\\b"), "")
            .replace(Regex("[-._]+"), " ")
            .trim()
            .ifEmpty { "Cinema" }
    }
    val lower = remember(title, name) { "$title $name".lowercase() }
    val gradientColors = remember(lower) {
        when {
            lower.contains("horror") || lower.contains("conjuring") || lower.contains("insidious") || lower.contains("halloween") || lower.contains("evil") || lower.contains("zombie") || lower.contains("scream") || lower.contains("quiet place") ->
                listOf(Color(0xFF2E0A0A), Color(0xFF1A0505), Color(0xFF0D0202))
            lower.contains("sci-fi") || lower.contains("interstellar") || lower.contains("inception") || lower.contains("dune") || lower.contains("matrix") || lower.contains("star") || lower.contains("avatar") ->
                listOf(Color(0xFF0C253D), Color(0xFF051525), Color(0xFF020C17))
            lower.contains("avengers") || lower.contains("marvel") || lower.contains("batman") || lower.contains("action") || lower.contains("mission") || lower.contains("wick") ->
                listOf(Color(0xFF102033), Color(0xFF1B283D), Color(0xFF0B1421))
            lower.contains("animation") || lower.contains("toy") || lower.contains("shrek") || lower.contains("disney") || lower.contains("pixar") ->
                listOf(Color(0xFF27153D), Color(0xFF160B24), Color(0xFF0A0314))
            else ->
                listOf(Color(0xFF182233), Color(0xFF101724), Color(0xFF080C14))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.4f),
                    radius = size.width * 0.6f
                )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = cleanTitle,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable internal fun LocalThumbnail(video: LocalVideo, modifier: Modifier) {
    val context = LocalContext.current.applicationContext
    val key = "${video.uri}:${video.modified}:${video.bytes}"
    val cached = remember(key) { ThumbnailMemory.cache.get(key) }
    val bitmap by produceState<Bitmap?>(initialValue = cached, key1 = key) {
        if (cached != null) {
            value = cached
            return@produceState
        }
        value = null
        value = withContext(Dispatchers.IO) {
            ThumbnailMemory.permits.withPermit {
                ThumbnailMemory.cache.get(key) ?: run {
                    var bmp: Bitmap? = null
                    // 1. On Android 10+, request cached system thumbnail from ContentResolver
                    if (Build.VERSION.SDK_INT >= 29) {
                        runCatching {
                            bmp = context.contentResolver.loadThumbnail(video.uri.toUri(), android.util.Size(480, 270), null)
                        }
                    }
                    // 2. Sample frame at 12% into the movie (or 15s in) to bypass black intro frames
                    if (bmp == null) {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, video.uri.toUri())
                            val sampleTimeUs = if (video.duration > 0) {
                                (video.duration * 1000L * 0.12f).toLong().coerceIn(10_000_000L, 180_000_000L)
                            } else {
                                15_000_000L
                            }
                            if (Build.VERSION.SDK_INT >= 27) {
                                bmp = retriever.getScaledFrameAtTime(sampleTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 480, 270)
                                if (bmp == null) {
                                    bmp = retriever.getScaledFrameAtTime(sampleTimeUs / 2, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 480, 270)
                                }
                                if (bmp == null) {
                                    bmp = retriever.getScaledFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 480, 270)
                                }
                            }
                            if (bmp == null) {
                                bmp = retriever.getFrameAtTime(sampleTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                    ?: retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            }
                        } catch (_: Exception) { } finally {
                            runCatching { retriever.release() }
                        }
                    }
                    bmp?.also { ThumbnailMemory.cache.put(key, it) }
                }
            }
        }
    }
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant) {
        val frame = bitmap
        if (frame != null) {
            Image(frame.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            CinematicFallbackPoster(title = video.title, name = video.name)
        }
    }
}
// END OF FILE
