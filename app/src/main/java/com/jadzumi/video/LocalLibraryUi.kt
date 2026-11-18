package com.jadzumi.video

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
    val theme = LocalThemeName.current
    Surface(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = SpatialGlass.cardBg(theme),
        border = SpatialGlass.cardBorder(theme, 0.75.dp),
        shadowElevation = MaterialExpressive.cardElevation(theme)
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
                "Cinema video player for your phone and tablet. Connect a folder on your device (e.g. Movies, TV Shows, or Downloads) to scan and play your media with hardware acceleration and automatic artwork detection.",
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
    val theme = LocalThemeName.current
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = SpatialGlass.cardBg(theme),
        border = SpatialGlass.cardBorder(theme, 0.75.dp),
        shadowElevation = MaterialExpressive.cardElevation(theme)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

private data class HeroItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String? = null,
    val localVideo: LocalVideo? = null,
    val backdropUri: String? = null,
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
                            color = LavenderAccent,
                            modifier = Modifier.size(if (isPhone) 40.dp else 44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Obsidian,
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
                            if (item.backdropUri != null) {
                                SidecarImage(item.backdropUri, item.localVideo, Modifier.fillMaxSize())
                            } else if (item.localVideo != null) {
                                SidecarImage(null, item.localVideo, Modifier.fillMaxSize())
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
                            // Subtle dark overlay for readability
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.35f),
                                            Obsidian.copy(alpha = 0.90f)
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
                                        color = LavenderAccent.copy(alpha = 0.90f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            item.badge,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Obsidian
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
                                        colors = ButtonDefaults.buttonColors(containerColor = LavenderAccent),
                                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, null, tint = Obsidian, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Play", color = Obsidian, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(240.dp)
            .height(135.dp)
            .clip(RoundedCornerShape(20.dp))
            .touchMotion(),
        shape = RoundedCornerShape(20.dp),
        color = Charcoal,
        border = SpatialGlass.cardBorder(LocalThemeName.current, 0.75.dp),
        shadowElevation = MaterialExpressive.cardElevation(LocalThemeName.current)
    ) {
        Box(Modifier.fillMaxSize()) {
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

            // Subtle dark overlay for readability
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
            )

            // Centered Play Button
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.50f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier.align(Alignment.Center).size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Bottom Info Overlay: Title on left, percentage / episode on right, and thin progress bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val title = video?.title ?: previewFallback?.title ?: "Video"
                val durSafe = if (durationMs > 0) durationMs else 1L
                val progress = (savedPos.toDouble() / durSafe).toFloat().coerceIn(0f, 1f)
                val percent = (progress * 100).toInt().coerceIn(1, 99)
                val subText = video?.episode?.let { "S${it.season} · E${it.episode}" } ?: "$percent%"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        subText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LavenderAccent
                    )
                }

                Spacer(Modifier.height(5.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(CircleShape),
                    color = LavenderAccent,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
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
    val theme = LocalThemeName.current
    Card(
        onClick = onClick,
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else SpatialGlass.cardBg(theme)
        ),
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else SpatialGlass.cardBorder(theme, 1.dp)
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
    onAddFolder: () -> Unit,
    onAnalyzeStorage: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val theme = LocalThemeName.current
    val stat = remember { runCatching { android.os.StatFs(android.os.Environment.getDataDirectory().path) }.getOrNull() }
    val totalDeviceBytes = stat?.totalBytes ?: (256L * 1024 * 1024 * 1024)
    val freeDeviceBytes = stat?.availableBytes ?: (178L * 1024 * 1024 * 1024)
    val usedDeviceBytes = (totalDeviceBytes - freeDeviceBytes).coerceAtLeast(0)
    val storageRatio = (usedDeviceBytes.toDouble() / totalDeviceBytes.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SpatialGlass.cardBg(theme)),
        border = SpatialGlass.cardBorder(theme, 1.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
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
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAddFolder,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Folder", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        if (onAnalyzeStorage != null) {
                            OutlinedButton(
                                onClick = onAnalyzeStorage,
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.PieChart, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Clean", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (onAnalyzeStorage != null) {
                                OutlinedButton(
                                    onClick = onAnalyzeStorage,
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.PieChart, null, Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Clean", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun JadzumiCategoryPillsRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalThemeName.current
    val isLight = theme == "Light"
    val isAmoled = theme == "AMOLED"
    val categories = remember { listOf("Movies", "Series", "Anime") }

    val selectedIndex = when (selectedCategory) {
        "Series", "TV Shows" -> 1
        "Anime" -> 2
        else -> 0
    }

    val sliderProgress by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
        label = "categoryCapsuleProgress"
    )

    val capsuleBg = when {
        isAmoled -> Color(0xFF14121B)
        isLight -> Color(0xF2FFFFFF)
        else -> Color(0xFF1E1A29).copy(alpha = 0.82f)
    }
    val capsuleBorder = when {
        isAmoled -> Color.White.copy(alpha = 0.12f)
        isLight -> Color(0x286750A4)
        else -> Color.White.copy(alpha = 0.08f)
    }
    val unselectedText = if (isLight) Color(0xFF49454F) else Color(0xFFCAC4D0)
    val thumbGradient = when {
        isAmoled -> Brush.horizontalGradient(listOf(Color(0xFF6750A4), Color(0xFF5E35B1)))
        isLight -> Brush.horizontalGradient(listOf(Color(0xFF6750A4), Color(0xFF7E57C2)))
        else -> Brush.horizontalGradient(listOf(Color(0xFF7E57C2), Color(0xFF9575CD)))
    }

    Surface(
        modifier = modifier
            .height(38.dp)
            .width(225.dp),
        shape = RoundedCornerShape(19.dp),
        color = capsuleBg,
        border = BorderStroke(0.85.dp, capsuleBorder),
        shadowElevation = if (isLight) 2.dp else 0.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        ) {
            val segmentWidth = maxWidth / categories.size

            // Animated sliding selection pill thumb
            Box(
                modifier = Modifier
                    .offset(x = segmentWidth * sliderProgress)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .shadow(
                        elevation = if (isLight) 4.dp else 2.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color(0xFF6750A4).copy(alpha = 0.40f)
                    )
                    .background(thumbGradient, shape = RoundedCornerShape(16.dp))
            )

            // Category text labels row
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEachIndexed { index, cat ->
                    val isSelected = selectedIndex == index
                    Box(
                        modifier = Modifier
                            .width(segmentWidth)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onCategorySelected(if (cat == "Series") "TV Shows" else cat)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) Color.White else unselectedText,
                            maxLines = 1
                        )
                    }
                }
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
    preview: () -> Unit = {},
    defaultCategory: String = "Movies",
    onCategoryChanged: ((String) -> Unit)? = null,
    onScrollActive: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val add = rememberFolderPicker(library)
    val listStateMovies = rememberLazyListState()
    val listStateSeries = rememberLazyListState()
    val listStateAnime = rememberLazyListState()
    val categories = remember { listOf("Movies", "TV Shows", "Anime") }
    var category by rememberSaveable(defaultCategory) { mutableStateOf(if (defaultCategory == "Series") "TV Shows" else defaultCategory) }

    LaunchedEffect(defaultCategory) {
        val norm = if (defaultCategory == "Series") "TV Shows" else defaultCategory
        if (category != norm) {
            category = norm
        }
    }

    fun getCategoryListState(cat: String): LazyListState = when (cat) {
        "TV Shows" -> listStateSeries
        "Anime" -> listStateAnime
        else -> listStateMovies
    }

    val currentActiveListState = getCategoryListState(category)
    val isHeroMinimized by remember(currentActiveListState) {
        derivedStateOf {
            currentActiveListState.firstVisibleItemIndex > 0 || currentActiveListState.firstVisibleItemScrollOffset > 60
        }
    }
    val shouldMinimizeDock by remember(currentActiveListState) {
        derivedStateOf {
            currentActiveListState.firstVisibleItemIndex > 0 || currentActiveListState.firstVisibleItemScrollOffset > 100
        }
    }
    LaunchedEffect(shouldMinimizeDock) {
        onScrollActive?.invoke(shouldMinimizeDock)
    }
    val libraryPrefs = remember(context) { context.getSharedPreferences("appearance", Context.MODE_PRIVATE) }
    val showHeroBanner = remember(context) { libraryPrefs.getBoolean("show_hero_banner", false) }
    var isGridView by rememberSaveable { mutableStateOf(libraryPrefs.getBoolean("library_grid_view", false)) }
    var isPosterAspect by rememberSaveable { mutableStateOf(libraryPrefs.getBoolean("library_poster_aspect", true)) }
    var libraryDensity by rememberSaveable {
        mutableStateOf(libraryPrefs.getString("library_density", "comfortable") ?: "comfortable")
    }
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

    val allMovies = remember(allVideos) { allVideos.filter { it.episode == null } }
    val animeKeywords = remember {
        listOf(
            "anime", "animation", "manga", "jujutsu", "titan", "demon slayer", "naruto",
            "one piece", "bleach", "frieren", "evangelion", "ghibli", "chainsaw", "vinland",
            "haikyu", "spy x family", "death note", "fullmetal", "solo leveling", "dandadan"
        )
    }
    val allAnime = remember(allVideos, animeKeywords) {
        allVideos.filter { v ->
            val nl = v.name.lowercase()
            val fl = v.folder.lowercase()
            animeKeywords.any { nl.contains(it) || fl.contains(it) }
        }
    }
    val allSeries = seriesList

    val continueWatchingMovies = remember(allMovies, playback.historyVersion) {
        allMovies.filter { playback.savedPosition(it) > 0 && !playback.isWatched(it) }
    }
    val continueWatchingSeries = remember(allSeries, playback.historyVersion) {
        allSeries.mapNotNull { s ->
            val inProg = s.seasons.values.flatten().firstOrNull { playback.savedPosition(it) > 0 && !playback.isWatched(it) }
            if (inProg != null) Pair(s, inProg) else null
        }
    }
    val continueWatchingAnime = remember(allAnime, playback.historyVersion) {
        allAnime.filter { playback.savedPosition(it) > 0 && !playback.isWatched(it) }
    }

    val franchises = remember(allMovies) { CinemaFranchiseManager.detectFranchises(allMovies) }

    val filteredSeries = remember(allSeries, query) {
        if (query.isBlank()) allSeries
        else allSeries.filter { matchesQuery(query, it.title, it.studio, it.synopsis) }
    }
    val filteredMovies = remember(allMovies, query) {
        if (query.isBlank()) allMovies
        else allMovies.filter { matchesQuery(query, it.name, it.title, it.folder) }
    }
    val filteredAnime = remember(allAnime, query) {
        if (query.isBlank()) allAnime
        else allAnime.filter { matchesQuery(query, it.name, it.title, it.folder) }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val screenMaxWidth = maxWidth
        val isPhone = (maxWidth < 600.dp || configuration.screenWidthDp < 600) && !isLandscape
        val pad = if (screenMaxWidth >= 720.dp && isLandscape) 28.dp else 16.dp
        val bottomPad = if (isPhone) 36.dp else 28.dp

        Column(Modifier.fillMaxSize()) {
            val motionStyle = LocalMotionStyle.current

            AnimatedContent(
                targetState = category,
                transitionSpec = {
                    if (!motionStyle.enabled) {
                        fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                    } else {
                        val tabOrder = listOf("Movies", "TV Shows", "Anime", "Folders")
                        val initialIndex = tabOrder.indexOf(initialState)
                        val targetIndex = tabOrder.indexOf(targetState)
                        val isForward = if (initialIndex != -1 && targetIndex != -1) targetIndex > initialIndex else true
                        sharedAxisTransition(isForward = isForward, dampingRatio = motionStyle.damping)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentKey = { it },
                label = "categorySharedAxisTransition"
            ) { currentCategory ->
                if (currentCategory == "Folders") {
                    Box(Modifier.fillMaxSize()) {
                        LocalFoldersPage(state = state, library = library, wide = screenMaxWidth >= 640.dp, select = select)
                    }
                } else {
                    val catListState = getCategoryListState(currentCategory)
                    val effectiveTheme = LocalThemeName.current
                    val colCount = if (isPhone) {
                        2
                    } else {
                        when (libraryDensity) {
                            "compact" -> if (screenMaxWidth >= 900.dp) 5 else 4
                            "spacious" -> if (screenMaxWidth >= 900.dp) 3 else 3
                            else -> if (screenMaxWidth >= 900.dp) 4 else 3
                        }
                    }
                    val seriesRows = remember(filteredSeries, colCount) { filteredSeries.chunked(colCount) }
                    val animeRows = remember(filteredAnime, colCount) { filteredAnime.chunked(colCount) }
                    val movieRows = remember(filteredMovies, colCount) { filteredMovies.chunked(colCount) }

                    PullToRefreshBox(
                        isRefreshing = state.busy,
                        onRefresh = { library.rescan() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = catListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .animatedScrollBlendFade(catListState),
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
                        if (currentCategory == "TV Shows") {
                            if (searchSeries.isNotEmpty()) {
                                item {
                                    Text("TV Series (${searchSeries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                item {
                                    TvSeriesCardGrid(seriesList = searchSeries, playback = playback, isPosterAspect = isPosterAspect, onSelect = { select("series:${it.id}") })
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
                                    MovieCardGrid(movies = searchMovies, sidecars = state.sidecars, playback = playback, isPosterAspect = isPosterAspect, onSelect = { select(it.uri) })
                                }
                            }
                        } else if (currentCategory == "Movies") {
                            if (searchMovies.isNotEmpty()) {
                                item {
                                    Text("Movies (${searchMovies.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                item {
                                    MovieCardGrid(movies = searchMovies, sidecars = state.sidecars, playback = playback, isPosterAspect = isPosterAspect, onSelect = { select(it.uri) })
                                }
                            }
                            if (searchSeries.isNotEmpty()) {
                                item {
                                    Text("TV Series (${searchSeries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                item {
                                    TvSeriesCardGrid(seriesList = searchSeries, playback = playback, isPosterAspect = isPosterAspect, onSelect = { select("series:${it.id}") })
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
                                    TvSeriesCardGrid(seriesList = searchSeries, playback = playback, isPosterAspect = isPosterAspect, onSelect = { select("series:${it.id}") })
                                }
                            }
                            if (searchMovies.isNotEmpty()) {
                                item {
                                    Text("Movies (${searchMovies.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                item {
                                    MovieCardGrid(movies = searchMovies, sidecars = state.sidecars, playback = playback, isPosterAspect = isPosterAspect, onSelect = { select(it.uri) })
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
                    } else {
                        when (currentCategory) {
                            "TV Shows" -> {
                                // 1. Continue Watching shelf for in-progress series episodes
                                if (continueWatchingSeries.isNotEmpty()) {
                                    item {
                                        CleanRailsContinueWatchingShelf(
                                            title = "CONTINUE WATCHING",
                                            countText = "${continueWatchingSeries.size} in progress"
                                        ) {
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                                contentPadding = PaddingValues(horizontal = 2.dp)
                                            ) {
                                                items(continueWatchingSeries, key = { it.second.uri }) { (series, ep) ->
                                                    val pos = playback.savedPosition(ep)
                                                    val dur = ep.duration.takeIf { it > 0 } ?: 1000L
                                                    val epInfo = EpisodePatterns.parse(ep.name)
                                                    val epSubtitle = if (epInfo != null) "S%02dE%02d · %s".format(epInfo.season, epInfo.episode, epInfo.episodeTitle ?: ep.title) else ep.title
                                                    CleanContinueWatchingCard(
                                                        video = ep,
                                                        title = series.title,
                                                        subtitle = epSubtitle,
                                                        savedPos = pos,
                                                        durationMs = dur,
                                                        sidecars = state.sidecars,
                                                        onClick = {
                                                            playback.open(ep)
                                                            playback.fullscreen = true
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 2. TV Series All Titles Rail
                                if (state.busy && filteredSeries.isEmpty()) {
                                    item { ExpressiveLibrarySkeleton() }
                                } else if (filteredSeries.isEmpty()) {
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
                                        CleanRailSectionHeader(
                                            title = "TV SERIES",
                                            countText = "${filteredSeries.size} Shows",
                                            isGridView = isGridView,
                                            onToggleGrid = {
                                                isGridView = !isGridView
                                                libraryPrefs.edit().putBoolean("library_grid_view", isGridView).apply()
                                            },
                                            density = libraryDensity,
                                            onCycleDensity = {
                                                libraryDensity = when (libraryDensity) {
                                                    "compact" -> "comfortable"
                                                    "comfortable" -> "spacious"
                                                    else -> "compact"
                                                }
                                                libraryPrefs.edit().putString("library_density", libraryDensity).apply()
                                            }
                                        )
                                    }

                                    if (isGridView) {
                                        items(seriesRows, key = { it.first().id }) { row ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                for (series in row) {
                                                    Box(Modifier.weight(1f)) {
                                                        CleanRailSeriesCard(
                                                            series = series,
                                                            playback = playback,
                                                            theme = effectiveTheme,
                                                            onClick = { select("series:${series.id}") }
                                                        )
                                                    }
                                                }
                                                val fillSlots = colCount - row.size
                                                repeat(fillSlots) {
                                                    Spacer(Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    } else {
                                        items(filteredSeries, key = { it.id }) { series ->
                                            TvSeriesRowItem(
                                                series = series,
                                                playback = playback,
                                                onClick = { select("series:${series.id}") }
                                            )
                                        }
                                    }
                                }
                            }

                            "Anime" -> {
                                // 1. Continue Watching shelf for anime
                                if (continueWatchingAnime.isNotEmpty()) {
                                    item {
                                        CleanRailsContinueWatchingShelf(
                                            title = "CONTINUE WATCHING",
                                            countText = "${continueWatchingAnime.size} in progress"
                                        ) {
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                                contentPadding = PaddingValues(horizontal = 2.dp)
                                            ) {
                                                items(continueWatchingAnime, key = { it.uri }) { anime ->
                                                    val pos = playback.savedPosition(anime)
                                                    val dur = anime.duration.takeIf { it > 0 } ?: 1000L
                                                    CleanContinueWatchingCard(
                                                        video = anime,
                                                        title = anime.title,
                                                        subtitle = timecode(pos / 1000) + " / " + timecode(dur / 1000),
                                                        savedPos = pos,
                                                        durationMs = dur,
                                                        sidecars = state.sidecars,
                                                        onClick = {
                                                            playback.open(anime)
                                                            playback.fullscreen = true
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 2. Anime Collection Rail
                                if (state.busy && filteredAnime.isEmpty()) {
                                    item { ExpressiveLibrarySkeleton() }
                                } else if (filteredAnime.isEmpty()) {
                                    item {
                                        LocalPanel {
                                            Icon(Icons.Rounded.Animation, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text("No Anime Found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Videos with anime names or located in Anime folders will automatically appear here with dual-titles and studio tags.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    item {
                                        CleanRailSectionHeader(
                                            title = "ANIME COLLECTION",
                                            countText = "${filteredAnime.size} Titles",
                                            isGridView = isGridView,
                                            onToggleGrid = {
                                                isGridView = !isGridView
                                                libraryPrefs.edit().putBoolean("library_grid_view", isGridView).apply()
                                            },
                                            density = libraryDensity,
                                            onCycleDensity = {
                                                libraryDensity = when (libraryDensity) {
                                                    "compact" -> "comfortable"
                                                    "comfortable" -> "spacious"
                                                    else -> "compact"
                                                }
                                                libraryPrefs.edit().putString("library_density", libraryDensity).apply()
                                            }
                                        )
                                    }

                                    if (isGridView) {
                                        items(animeRows, key = { it.first().uri }) { row ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                for (anime in row) {
                                                    Box(Modifier.weight(1f)) {
                                                        CleanRailAnimeCard(
                                                            video = anime,
                                                            sidecars = state.sidecars,
                                                            theme = effectiveTheme,
                                                            playback = playback,
                                                            onClick = { select(anime.uri) }
                                                        )
                                                    }
                                                }
                                                val fillSlots = colCount - row.size
                                                repeat(fillSlots) {
                                                    Spacer(Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    } else {
                                        items(filteredAnime, key = { it.uri }) { anime ->
                                            LocalVideoCard(
                                                video = anime,
                                                isSelectionMode = isSelectionMode,
                                                isSelected = anime.uri in selectedUris,
                                                isWatched = playback.isWatched(anime),
                                                onLongClick = {
                                                    if (!isSelectionMode) {
                                                        isSelectionMode = true
                                                        selectedUris.add(anime.uri)
                                                    }
                                                },
                                                select = {
                                                    if (isSelectionMode) {
                                                        if (anime.uri in selectedUris) selectedUris.remove(anime.uri)
                                                        else selectedUris.add(anime.uri)
                                                    } else {
                                                        select(anime.uri)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            else -> {
                                // Default / Movies Tab
                                // 1. Continue Watching shelf for movies
                                if (continueWatchingMovies.isNotEmpty()) {
                                    item {
                                        CleanRailsContinueWatchingShelf(
                                            title = "CONTINUE WATCHING",
                                            countText = "${continueWatchingMovies.size} in progress"
                                        ) {
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                                contentPadding = PaddingValues(horizontal = 2.dp)
                                            ) {
                                                items(continueWatchingMovies, key = { it.uri }) { movie ->
                                                    val pos = playback.savedPosition(movie)
                                                    val dur = movie.duration.takeIf { it > 0 } ?: 1000L
                                                    CleanContinueWatchingCard(
                                                        video = movie,
                                                        title = movie.title,
                                                        subtitle = timecode(pos / 1000) + " / " + timecode(dur / 1000),
                                                        savedPos = pos,
                                                        durationMs = dur,
                                                        sidecars = state.sidecars,
                                                        onClick = {
                                                            playback.open(movie)
                                                            playback.fullscreen = true
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 2. Franchises Shelf (if any)
                                if (franchises.isNotEmpty()) {
                                    item {
                                        CinemaFranchiseShelf(
                                            franchises = franchises,
                                            onSelectFilm = { select(it.uri) }
                                        )
                                    }
                                }

                                // 3. Feature Films Rail
                                if (state.busy && filteredMovies.isEmpty()) {
                                    item { ExpressiveLibrarySkeleton() }
                                } else if (filteredMovies.isEmpty()) {
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
                                        CleanRailSectionHeader(
                                            title = "FEATURE FILMS",
                                            countText = "${filteredMovies.size} Titles",
                                            isGridView = isGridView,
                                            onToggleGrid = {
                                                isGridView = !isGridView
                                                libraryPrefs.edit().putBoolean("library_grid_view", isGridView).apply()
                                            },
                                            density = libraryDensity,
                                            onCycleDensity = {
                                                libraryDensity = when (libraryDensity) {
                                                    "compact" -> "comfortable"
                                                    "comfortable" -> "spacious"
                                                    else -> "compact"
                                                }
                                                libraryPrefs.edit().putString("library_density", libraryDensity).apply()
                                            }
                                        )
                                    }

                                    if (isGridView) {
                                        items(movieRows, key = { it.first().uri }) { row ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                for (movie in row) {
                                                    Box(Modifier.weight(1f)) {
                                                        CleanRailPosterCard(
                                                            video = movie,
                                                            sidecars = state.sidecars,
                                                            playback = playback,
                                                            theme = effectiveTheme,
                                                            onClick = { select(movie.uri) }
                                                        )
                                                    }
                                                }
                                                val fillSlots = colCount - row.size
                                                repeat(fillSlots) {
                                                    Spacer(Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    } else {
                                        items(filteredMovies, key = { it.uri }) { movie ->
                                            LocalVideoCard(
                                                video = movie,
                                                isSelectionMode = isSelectionMode,
                                                isSelected = movie.uri in selectedUris,
                                                isWatched = playback.isWatched(movie),
                                                onLongClick = {
                                                    if (!isSelectionMode) {
                                                        isSelectionMode = true
                                                        selectedUris.add(movie.uri)
                                                    }
                                                },
                                                select = {
                                                    if (isSelectionMode) {
                                                        if (movie.uri in selectedUris) selectedUris.remove(movie.uri)
                                                        else selectedUris.add(movie.uri)
                                                    } else {
                                                        select(movie.uri)
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
        }
    }
}
}
}

@Composable
fun CleanRailsContinueWatchingShelf(
    title: String = "CONTINUE WATCHING",
    countText: String = "",
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (countText.isNotEmpty()) {
                Text(
                    countText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        content()
    }
}

@Composable
fun CleanContinueWatchingCard(
    video: LocalVideo,
    title: String = video.title,
    subtitle: String = "",
    savedPos: Long,
    durationMs: Long,
    sidecars: Map<String, List<LocalSidecar>> = emptyMap(),
    onClick: () -> Unit
) {
    val theme = LocalThemeName.current
    val context = LocalContext.current
    val progress = if (durationMs > 0) (savedPos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val remainingSec = ((durationMs - savedPos).coerceAtLeast(0) / 1000)
    val remainingText = if (remainingSec >= 3600) "${remainingSec / 3600}h ${(remainingSec % 3600) / 60}m left" else "${(remainingSec / 60).coerceAtLeast(1)}m left"
    val percentText = "${(progress * 100).toInt()}%"

    val movieMeta = remember(video.identity, sidecars) {
        if (sidecars.isNotEmpty()) extractMovieMetadata(video, sidecars, context) else null
    }
    var onlinePosterUri by remember(video.identity) {
        mutableStateOf(movieMeta?.backdropUri ?: movieMeta?.posterUri)
    }

    LaunchedEffect(video.identity) {
        if (onlinePosterUri == null) {
            val (cleanTitle, cleanYear) = OnlineMetadataFetcher.extractCleanTitleAndYear(video.name)
            val searchTitle = cleanTitle.ifBlank { video.title }
            val cached = withContext(Dispatchers.IO) {
                OnlineMetadataFetcher.getCachedMovie(context, searchTitle, cleanYear, video.identity)
                    ?: OnlineMetadataFetcher.getCachedTvShow(context, searchTitle)
                    ?: if (OnlineMetadataFetcher.isOnlineMetadataEnabled(context)) {
                        OnlineMetadataFetcher.fetchMovieMetadata(context, searchTitle, cleanYear, video.identity)
                    } else null
            }
            if (cached != null) {
                val uri = cached.backdropUrl ?: cached.localPosterPath ?: cached.posterUrl
                if (!uri.isNullOrBlank()) {
                    onlinePosterUri = uri
                }
            }
        }
    }

    val artworkUri = onlinePosterUri ?: movieMeta?.backdropUri ?: movieMeta?.posterUri

    val (extractedCleanTitle, _) = remember(video.name) { OnlineMetadataFetcher.extractCleanTitleAndYear(video.name) }
    val displayTitle = remember(extractedCleanTitle, title) {
        if (extractedCleanTitle.isNotBlank()) extractedCleanTitle else title
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp)
            .touchMotion(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialExpressive.cardBg(theme),
        border = SpatialGlass.cardBorder(theme, 0.75.dp),
        shadowElevation = MaterialExpressive.cardElevation(theme)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                if (artworkUri != null) {
                    SidecarImage(
                        uri = artworkUri,
                        fallbackVideo = video,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LocalThumbnail(video, Modifier.fillMaxSize())
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                startY = 40f
                            )
                        )
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        percentText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = "Resume",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        remainingText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Black.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = displayTitle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun CleanRailSectionHeader(
    title: String,
    countText: String,
    isGridView: Boolean,
    onToggleGrid: () -> Unit,
    density: String,
    onCycleDensity: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "· $countText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isGridView) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(
                        onClick = onCycleDensity,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            when (density) {
                                "compact" -> Icons.Rounded.DensitySmall
                                "spacious" -> Icons.Rounded.DensityLarge
                                else -> Icons.Rounded.DensityMedium
                            },
                            contentDescription = "Grid Density",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                border = subtleCardBorder(strokeWidth = 0.75.dp),
                modifier = Modifier.size(36.dp)
            ) {
                IconButton(
                    onClick = onToggleGrid,
                    modifier = Modifier.fillMaxSize()
                ) {
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
}

@Composable
fun CleanRailPosterCard(
    video: LocalVideo,
    sidecars: Map<String, List<LocalSidecar>>,
    playback: PlaybackController,
    theme: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val movieMeta = remember(video.identity, sidecars) {
        extractMovieMetadata(video, sidecars, context)
    }
    var onlinePosterUri by remember(video.identity) {
        mutableStateOf(movieMeta.posterUri)
    }

    LaunchedEffect(video.identity) {
        if (onlinePosterUri == null) {
            val (cleanTitle, cleanYear) = OnlineMetadataFetcher.extractCleanTitleAndYear(video.name)
            val searchTitle = cleanTitle.ifBlank { video.title }
            val cached = withContext(Dispatchers.IO) {
                OnlineMetadataFetcher.getCachedMovie(context, searchTitle, cleanYear, video.identity)
                    ?: if (OnlineMetadataFetcher.isOnlineMetadataEnabled(context)) {
                        OnlineMetadataFetcher.fetchMovieMetadata(context, searchTitle, cleanYear, video.identity)
                    } else null
            }
            if (cached != null) {
                val uri = cached.localPosterPath ?: cached.posterUrl
                if (!uri.isNullOrBlank()) {
                    onlinePosterUri = uri
                }
            }
        }
    }

    val posterTarget = onlinePosterUri ?: movieMeta.posterUri
    val savedPos = playback.savedPosition(video)
    val dur = video.duration.takeIf { it > 0 } ?: 1000L
    val isWatched = playback.isWatched(video)
    val inProgress = savedPos > 0 && !isWatched
    val progress = (savedPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

    val qualityBadge = when {
        isMedia4K(video.width, video.height, video.name) -> "4K UHD"
        video.height >= 1000 || video.width >= 1900 -> "1080p"
        video.height >= 700 || video.width >= 1200 -> "720p"
        else -> null
    }

    val (extractedCleanTitle, cleanYear) = remember(video.name) { OnlineMetadataFetcher.extractCleanTitleAndYear(video.name) }
    val displayTitle = remember(extractedCleanTitle, video.title) {
        if (extractedCleanTitle.isNotBlank()) extractedCleanTitle else video.title
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .touchMotion(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialExpressive.cardBg(theme),
        border = SpatialGlass.cardBorder(theme, 0.75.dp),
        shadowElevation = MaterialExpressive.cardElevation(theme)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                if (posterTarget != null) {
                    SidecarImage(
                        uri = posterTarget,
                        fallbackVideo = video,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LocalThumbnail(video, Modifier.fillMaxSize())
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                startY = 100f
                            )
                        )
                )

                if (qualityBadge != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            qualityBadge,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 9.sp
                        )
                    }
                }

                if (inProgress) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Black.copy(alpha = 0.4f)
                    )
                }
            }

            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val durFormatted = if (video.duration > 0) timecode(video.duration / 1000) else "Movie"
                val (_, cleanYear) = OnlineMetadataFetcher.extractCleanTitleAndYear(video.name)
                val subText = if (cleanYear != null) "$cleanYear · $durFormatted" else durFormatted
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun CleanRailSeriesCard(
    series: TvSeries,
    playback: PlaybackController,
    theme: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var seriesPoster by remember(series.id) {
        mutableStateOf(series.posterUri ?: series.backdropUri)
    }

    LaunchedEffect(series.id) {
        if (seriesPoster == null) {
            val cached = withContext(Dispatchers.IO) {
                OnlineMetadataFetcher.getCachedTvShow(context, series.title)
                    ?: if (OnlineMetadataFetcher.isOnlineMetadataEnabled(context)) {
                        OnlineMetadataFetcher.fetchTvShowMetadata(context, series.title)
                    } else null
            }
            if (cached != null) {
                val uri = cached.localPosterPath ?: cached.posterUrl
                if (!uri.isNullOrBlank()) {
                    seriesPoster = uri
                }
            }
        }
    }

    val totalEpisodes = series.seasons.values.sumOf { it.size }
    val seasonCount = series.seasons.size
    val firstEp = series.seasons.values.firstOrNull()?.firstOrNull()

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .touchMotion(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialExpressive.cardBg(theme),
        border = SpatialGlass.cardBorder(theme, 0.75.dp),
        shadowElevation = MaterialExpressive.cardElevation(theme)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                if (seriesPoster != null) {
                    SidecarImage(
                        uri = seriesPoster,
                        fallbackVideo = firstEp,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (firstEp != null) {
                    LocalThumbnail(firstEp, Modifier.fillMaxSize())
                } else {
                    CinematicFallbackPoster(title = series.title, name = series.title, modifier = Modifier.fillMaxSize())
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                startY = 100f
                            )
                        )
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        "$seasonCount ${if (seasonCount == 1) "Season" else "Seasons"}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }
            }

            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = series.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$seasonCount ${if (seasonCount == 1) "Season" else "Seasons"} · $totalEpisodes Episodes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun CleanRailAnimeCard(
    video: LocalVideo,
    sidecars: Map<String, List<LocalSidecar>> = emptyMap(),
    theme: String,
    playback: PlaybackController,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val movieMeta = remember(video.identity, sidecars) {
        if (sidecars.isNotEmpty()) extractMovieMetadata(video, sidecars, context) else null
    }
    var onlinePosterUri by remember(video.identity) {
        mutableStateOf(movieMeta?.posterUri)
    }

    LaunchedEffect(video.identity) {
        if (onlinePosterUri == null) {
            val (cleanTitle, cleanYear) = OnlineMetadataFetcher.extractCleanTitleAndYear(video.name)
            val searchTitle = cleanTitle.ifBlank { video.title }
            val cached = withContext(Dispatchers.IO) {
                OnlineMetadataFetcher.getCachedTvShow(context, searchTitle)
                    ?: OnlineMetadataFetcher.getCachedMovie(context, searchTitle, cleanYear, video.identity)
                    ?: if (OnlineMetadataFetcher.isOnlineMetadataEnabled(context)) {
                        OnlineMetadataFetcher.fetchTvShowMetadata(context, searchTitle)
                    } else null
            }
            if (cached != null) {
                val uri = cached.localPosterPath ?: cached.posterUrl
                if (!uri.isNullOrBlank()) {
                    onlinePosterUri = uri
                }
            }
        }
    }

    val targetArtwork = onlinePosterUri ?: movieMeta?.posterUri
    val savedPos = playback.savedPosition(video)
    val dur = video.duration.takeIf { it > 0 } ?: 1000L
    val isWatched = playback.isWatched(video)
    val inProgress = savedPos > 0 && !isWatched
    val progress = (savedPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

    val nameLower = video.name.lowercase()
    val studio = when {
        nameLower.contains("mappa") -> "MAPPA"
        nameLower.contains("ufotable") -> "ufotable"
        nameLower.contains("wit") -> "Wit Studio"
        nameLower.contains("cloverworks") -> "CloverWorks"
        nameLower.contains("kyoto") || nameLower.contains("kyoani") -> "Kyoto Animation"
        nameLower.contains("bones") -> "Bones"
        nameLower.contains("madhouse") -> "Madhouse"
        nameLower.contains("ghibli") -> "Studio Ghibli"
        nameLower.contains("toei") -> "Toei"
        nameLower.contains("a-1") -> "A-1 Pictures"
        else -> null
    }

    val (extractedCleanTitle, cleanYear) = remember(video.name) { OnlineMetadataFetcher.extractCleanTitleAndYear(video.name) }
    val displayTitle = remember(extractedCleanTitle, video.title) {
        if (extractedCleanTitle.isNotBlank()) extractedCleanTitle else video.title
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .touchMotion(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialExpressive.cardBg(theme),
        border = SpatialGlass.cardBorder(theme, 0.75.dp),
        shadowElevation = MaterialExpressive.cardElevation(theme)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                if (targetArtwork != null) {
                    SidecarImage(
                        uri = targetArtwork,
                        fallbackVideo = video,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LocalThumbnail(video, Modifier.fillMaxSize())
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                                startY = 100f
                            )
                        )
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (studio != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        studio ?: "ANIME",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }

                if (inProgress) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Black.copy(alpha = 0.4f)
                    )
                }
            }

            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val epInfo = EpisodePatterns.parse(video.name)
                val epText = if (epInfo != null) "S${epInfo.season}E${epInfo.episode}" else if (video.duration > 0) timecode(video.duration / 1000) else "Anime Feature"
                Text(
                    text = if (studio != null) "$studio · $epText" else epText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
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
    val folderPrefs = remember { context.getSharedPreferences("appearance", Context.MODE_PRIVATE) }
    var isFolderGridView by rememberSaveable { mutableStateOf(folderPrefs.getBoolean("folder_grid_view", false)) }
    val add = rememberFolderPicker(library)
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var remove by remember { mutableStateOf<LocalFolder?>(null) }
    var videoToDelete by remember { mutableStateOf<LocalVideo?>(null) }
    val networkManager = remember { NetworkStorageManager(context) }
    var showAddShareDialog by remember { mutableStateOf(false) }
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
                        item {
                            NetworkSharesShelf(
                                networkManager = networkManager,
                                onAddClick = { showAddShareDialog = true },
                                onSelectShare = { select(it.serverAddress) }
                            )
                        }
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
                        ) { folderUri ->
                            val folderVideos = state.videos.filter { it.folder == folderUri }
                            LazyColumn(
                                Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                item {
                                    Text("${folderVideos.size} indexed videos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                                items(folderVideos, key = { "${folderUri ?: ""}_${it.uri}" }) { video ->
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
                item {
                    NetworkSharesShelf(
                        networkManager = networkManager,
                        onAddClick = { showAddShareDialog = true },
                        onSelectShare = { select(it.serverAddress) }
                    )
                }
                if (state.folders.isEmpty()) {
                    item { LocalPanel { Text("No folders connected", fontWeight = FontWeight.Bold); Text("Add a folder using Android's file picker.") } }
                } else {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Connected Folders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                                border = subtleCardBorder(strokeWidth = 0.75.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        isFolderGridView = !isFolderGridView
                                        folderPrefs.edit().putBoolean("folder_grid_view", isFolderGridView).apply()
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        if (isFolderGridView) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                                        contentDescription = "Toggle Grid or List View",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (isFolderGridView) {
                        items(state.folders.chunked(2), key = { chunk -> chunk.firstOrNull()?.uri ?: "" }) { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (folder in chunk) {
                                    Box(Modifier.weight(1f)) {
                                        FolderGridTile(folder, state, active == folder, { selected = folder.uri }, { remove = folder })
                                    }
                                }
                                if (chunk.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(state.folders, key = { it.uri }) { folder ->
                            FolderSummaryCard(folder, state, active == folder, { selected = folder.uri }, { remove = folder })
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("${videos.size} indexed videos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                if (isFolderGridView) {
                    items(videos.chunked(2), key = { chunk -> chunk.firstOrNull()?.uri ?: "" }) { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (video in chunk) {
                                Box(Modifier.weight(1f)) {
                                    LocalPosterGridCard(
                                        video = video,
                                        onClick = { select(video.uri) }
                                    )
                                }
                            }
                            if (chunk.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    items(videos, key = { "${active?.uri ?: ""}_${it.uri}" }) { video ->
                        LocalVideoCard(video, onDelete = { videoToDelete = video }) { select(video.uri) }
                    }
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

    if (showAddShareDialog) {
        var shareName by remember { mutableStateOf("") }
        var shareType by remember { mutableStateOf("SMB") }
        var serverAddr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddShareDialog = false },
            icon = { Icon(Icons.Rounded.Lan, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Add Network Stream / Share") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = shareName,
                        onValueChange = { shareName = it },
                        label = { Text("Share Name (e.g. Living Room NAS)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = serverAddr,
                        onValueChange = { serverAddr = it },
                        label = { Text("Stream URL (smb://, http://, webdav://)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SMB", "WebDAV", "DLNA").forEach { type ->
                            FilterChip(
                                selected = shareType == type,
                                onClick = { shareType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (serverAddr.isNotBlank()) {
                            networkManager.addShare(
                                name = shareName.ifBlank { "Network Share" },
                                type = shareType,
                                address = serverAddr
                            )
                            showAddShareDialog = false
                        }
                    },
                    enabled = serverAddr.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddShareDialog = false }) {
                    Text("Cancel")
                }
            }
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
                Button(onClick = add, enabled = !state.busy, modifier = Modifier.touchMotion()) {
                    Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add folder", maxLines = 1, softWrap = false)
                }
                OutlinedButton(onClick = library::fullRescan, enabled = !state.busy && state.folders.isNotEmpty(), modifier = Modifier.touchMotion()) {
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
    val containerColor by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(220),
        label = "folderContainer"
    )
    Card(
        onClick = onSelect,
        modifier = Modifier.touchMotion(),
        shape = RoundedCornerShape(22.dp),
        border = if (isActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
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
    isPosterAspect: Boolean = true,
    density: String = "comfortable",
    onSelect: (LocalVideo) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val minColWidth = when (density) {
            "compact" -> 104.dp
            "spacious" -> 194.dp
            else -> 142.dp
        }
        val gap = when (density) {
            "compact" -> 10.dp
            "spacious" -> 18.dp
            else -> 14.dp
        }
        val columns = if (isPosterAspect) {
            ((maxWidth + gap) / (minColWidth + gap)).toInt().coerceIn(2, 6)
        } else {
            ((maxWidth + gap) / (minColWidth * 1.35f + gap)).toInt().coerceIn(2, 5)
        }
        val rows = movies.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(gap + 2.dp)) {
            rows.forEach { rowMovies ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    rowMovies.forEach { movie ->
                        Box(Modifier.weight(1f)) {
                            MovieSummaryCard(
                                video = movie,
                                sidecars = sidecars,
                                playback = playback,
                                isPosterAspect = isPosterAspect,
                                density = density,
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
    isPosterAspect: Boolean = true,
    density: String = "comfortable",
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var livePosterUri by remember(video.identity) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(video.identity, video.name) {
        val (cleanTitle, cleanYear) = OnlineMetadataFetcher.extractCleanTitleAndYear(video.name)
        val cached = OnlineMetadataFetcher.getCachedMovie(context, cleanTitle, cleanYear, video.identity)
        val cachedPoster = cached?.localPosterPath ?: cached?.posterUrl
        if (cachedPoster != null) {
            livePosterUri = cachedPoster
        } else {
            val fetched = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                OnlineMetadataFetcher.fetchMovieMetadata(context, cleanTitle, cleanYear, video.identity)
            }
            val fetchedPoster = fetched?.localPosterPath ?: fetched?.posterUrl
            if (fetchedPoster != null) {
                livePosterUri = fetchedPoster
            }
        }
    }

    val metadata = remember(video, sidecars, livePosterUri) {
        val base = extractMovieMetadata(video, sidecars, context)
        if (base.posterUri == null && livePosterUri != null) {
            base.copy(posterUri = livePosterUri, backdropUri = base.backdropUri ?: livePosterUri)
        } else {
            base
        }
    }
    val savedPos = playback.savedPosition(video)
    val dur = video.duration.takeIf { it > 0 } ?: 1000L
    val isWatched = savedPos >= dur * 0.9f
    val inProgress = savedPos in 1 until (dur * 0.9f).toLong()
    val progress = (savedPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

    val theme = LocalThemeName.current
    val cardRadius = when (density) {
        "compact" -> 16.dp
        else -> 20.dp
    }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .touchMotion(),
        shape = RoundedCornerShape(cardRadius),
        color = SpatialGlass.cardBg(theme),
        border = SpatialGlass.cardBorder(theme, 0.75.dp),
        shadowElevation = MaterialExpressive.cardElevation(theme)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val imageUri = if (isPosterAspect) {
                metadata.posterUri ?: metadata.backdropUri
            } else {
                metadata.backdropUri ?: metadata.posterUri
            }
            val targetAspect = if (isPosterAspect) (2f / 3f) else (16f / 9f)

            // Poster / Backdrop Thumbnail Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(targetAspect)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                SidecarImage(
                    uri = imageUri,
                    fallbackVideo = video,
                    modifier = Modifier.fillMaxSize()
                )

                // Soft bottom gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                            )
                        )
                )

                // Consolidated Top-Right Badge: Rating + Resolution/Codec
                val qualityText = when {
                    video.width >= 3840 -> "4K"
                    video.width >= 1920 -> "1080p"
                    video.name.contains("hevc", true) || video.name.contains("x265", true) -> "HEVC"
                    else -> "HD"
                }

                if (isWatched) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2E7D32).copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.40f)),
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp).size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, contentDescription = "Watched", tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.72f),
                    border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (metadata.rating != null) {
                            Icon(Icons.Rounded.Star, null, Modifier.size(10.5.dp), tint = Color(0xFFF9AB00))
                            Text(metadata.rating, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("·", fontSize = 9.5.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Text(
                            qualityText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Quick Play/Resume button floating on bottom-right of poster with frosted glass & depth
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(34.dp)
                        .clickable {
                            playback.open(video)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = "Play movie",
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Progress Line at bottom of thumbnail with dark underlay track
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .background(Color.Black.copy(alpha = 0.40f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(if (isWatched) Color(0xFF1E8E3E) else MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // Info Section
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = metadata.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val runtimeStr = if (video.duration > 0) {
                    val totalSec = video.duration / 1000
                    val hours = totalSec / 3600
                    val minutes = (totalSec % 3600) / 60
                    if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                } else "Feature Film"
                val genre = metadata.genres.firstOrNull()
                val metaLine = listOfNotNull(metadata.year, runtimeStr, genre).joinToString(" · ")
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Dynamic Status / Action Row (No duplicate genre repetition!)
                if (isWatched) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, Modifier.size(13.dp), tint = Color(0xFF1E8E3E))
                        Text("Watched", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E8E3E))
                    }
                } else if (inProgress) {
                    val remainingSec = (dur - savedPos) / 1000
                    val remHours = remainingSec / 3600
                    val remMins = (remainingSec % 3600) / 60
                    val remStr = if (remHours > 0) "${remHours}h ${remMins}m left" else "${remMins}m left"
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Schedule, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Resume · $remStr",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
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
    isPosterAspect: Boolean = true,
    density: String = "comfortable",
    onSelect: (TvSeries) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val minColWidth = when (density) {
            "compact" -> 104.dp
            "spacious" -> 194.dp
            else -> 142.dp
        }
        val gap = when (density) {
            "compact" -> 10.dp
            "spacious" -> 18.dp
            else -> 14.dp
        }
        val columns = if (isPosterAspect) {
            ((maxWidth + gap) / (minColWidth + gap)).toInt().coerceIn(2, 6)
        } else {
            ((maxWidth + gap) / (minColWidth * 1.35f + gap)).toInt().coerceIn(2, 5)
        }
        val rows = seriesList.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(gap + 2.dp)) {
            rows.forEach { rowSeries ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    rowSeries.forEach { series ->
                        Box(Modifier.weight(1f)) {
                            TvSeriesSummaryCard(
                                series = series,
                                playback = playback,
                                isPosterAspect = isPosterAspect,
                                density = density,
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
    isPosterAspect: Boolean = true,
    density: String = "comfortable",
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var livePosterUri by remember(series.title) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(series.title) {
        val cached = OnlineMetadataFetcher.getCachedTvShow(context, series.title)
        val cachedPoster = cached?.localPosterPath ?: cached?.posterUrl
        if (cachedPoster != null) {
            livePosterUri = cachedPoster
        } else {
            val fetched = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                OnlineMetadataFetcher.fetchTvShowMetadata(context, series.title)
            }
            val fetchedPoster = fetched?.localPosterPath ?: fetched?.posterUrl
            if (fetchedPoster != null) {
                livePosterUri = fetchedPoster
            }
        }
    }

    val watched = remember(series, playback.historyVersion) { series.watchedCount(playback) }
    val total = series.totalEpisodes
    val isCompleted = total > 0 && watched >= total
    val progress = if (total > 0) (watched.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val nextEp = remember(series, playback.historyVersion) { series.nextUnwatchedEpisode(playback) }

    val theme = LocalThemeName.current
    val cardRadius = when (density) {
        "compact" -> 16.dp
        else -> 20.dp
    }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .touchMotion(),
        shape = RoundedCornerShape(cardRadius),
        color = SpatialGlass.cardBg(theme),
        border = SpatialGlass.cardBorder(theme, 0.75.dp),
        shadowElevation = MaterialExpressive.cardElevation(theme)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val effectivePoster = series.posterUri ?: livePosterUri
            val effectiveBackdrop = series.backdropUri ?: livePosterUri ?: effectivePoster
            val imageUri = if (isPosterAspect) {
                effectivePoster ?: effectiveBackdrop
            } else {
                effectiveBackdrop ?: effectivePoster
            }
            val targetAspect = if (isPosterAspect) (2f / 3f) else (16f / 9f)

            // Poster / Backdrop Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(targetAspect)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                val firstEp = series.seasons.values.firstOrNull()?.firstOrNull()
                SidecarImage(
                    uri = imageUri,
                    fallbackVideo = firstEp,
                    modifier = Modifier.fillMaxSize()
                )

                // Soft bottom gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                            )
                        )
                )

                // Top-Right Consolidated Badge: Rating + Seasons
                val seasonsText = "${series.seasonCount} Season${if (series.seasonCount > 1) "s" else ""}"
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.72f),
                    border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (series.rating != null) {
                            Icon(Icons.Rounded.Star, null, Modifier.size(10.5.dp), tint = Color(0xFFF9AB00))
                            Text(series.rating, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("·", fontSize = 9.5.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Text(
                            seasonsText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // If next episode is available, show a floating mini play button on the bottom right with frosted glass & depth
                if (nextEp != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(34.dp)
                            .clickable {
                                playback.open(nextEp)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = "Play next episode",
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                // Progress Line at bottom of thumbnail with dark underlay track
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .background(Color.Black.copy(alpha = 0.40f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(if (isCompleted) Color(0xFF1E8E3E) else MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // Info Section
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = series.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val episodesWatchedText = if (isCompleted) {
                    "Completed · ${series.totalEpisodes} eps"
                } else if (watched > 0) {
                    "$watched of $total watched"
                } else {
                    "${series.totalEpisodes} Episodes · ${series.year ?: "Series"}"
                }

                Text(
                    text = episodesWatchedText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCompleted) Color(0xFF1E8E3E) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Interactive "Up Next" episode chip
                if (nextEp != null && !isCompleted) {
                    val epHint = nextEp.episode
                    val upNextLabel = if (epHint != null) {
                        "Up Next: S%02dE%02d".format(epHint.season, epHint.episode)
                    } else {
                        "Up Next: ${nextEp.title}"
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable {
                                playback.open(nextEp)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                upNextLabel,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else if (isCompleted) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, Modifier.size(13.dp), tint = Color(0xFF1E8E3E))
                        Text("All Watched", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E8E3E))
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
    isWatched: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    select: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalThemeName.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .touchMotion()
            .combinedClickable(
                onClick = select,
                onLongClick = onLongClick
            ),
        color = SpatialGlass.cardBg(theme),
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else SpatialGlass.cardBorder(theme, 0.75.dp),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = MaterialExpressive.cardElevation(theme)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            video.title,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
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
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    playback: PlaybackController? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val theme = LocalThemeName.current
    val glassIntensity = LocalGlassIntensity.current
    val savedPos = playback?.savedPosition(video) ?: 0L
    val dur = video.duration.takeIf { it > 0 } ?: 1000L
    val isWatched = savedPos >= dur * 0.9f
    val inProgress = savedPos in 1 until (dur * 0.9f).toLong()
    val progress = (savedPos.toDouble() / dur).toFloat().coerceIn(0f, 1f)
    val is4k = isMedia4K(video.width, video.height, video.name)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SpatialGlass.cardBg(theme),
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else SpatialGlass.cardBorder(theme, 0.75.dp),
        shadowElevation = MaterialExpressive.cardElevation(theme),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .touchMotion()
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
                } else if (isWatched) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2E7D32).copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier.padding(6.dp).size(22.dp).align(Alignment.TopEnd)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, contentDescription = "Watched", tint = Color.White, modifier = Modifier.size(14.dp))
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
                    } else measuredVideoBadge(null, video.width, video.height, false)

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
    private val maxCacheBytes = ((Runtime.getRuntime().maxMemory() / 7L).coerceIn(32L * 1024 * 1024, 96L * 1024 * 1024)).toInt()
    val cache = object : LruCache<String, Bitmap>(maxCacheBytes) { override fun sizeOf(key: String, value: Bitmap) = value.byteCount }
    val permits = Semaphore(4)

    fun getDiskFile(context: Context, key: String): File {
        val hash = runCatching {
            MessageDigest.getInstance("MD5").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
        }.getOrElse { key.hashCode().toString() }
        val dir = File(context.cacheDir, "thumbnails").apply { if (!exists()) mkdirs() }
        return File(dir, "$hash.webp")
    }

    fun loadFromDisk(context: Context, key: String): Bitmap? {
        return runCatching {
            val file = getDiskFile(context, key)
            if (file.exists() && file.length() > 0) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        }.getOrNull()
    }

    fun saveToDisk(context: Context, key: String, bmp: Bitmap) {
        runCatching {
            val file = getDiskFile(context, key)
            val temp = File(file.parentFile, "${file.name}.tmp")
            FileOutputStream(temp).use { out ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bmp.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
                } else {
                    @Suppress("DEPRECATION")
                    bmp.compress(Bitmap.CompressFormat.WEBP, 80, out)
                }
            }
            temp.renameTo(file)
        }
    }
}

fun clearThumbnailMemoryCache(context: Context? = null) {
    ThumbnailMemory.cache.evictAll()
    context?.let { ctx ->
        runCatching {
            File(ctx.cacheDir, "thumbnails").deleteRecursively()
        }
    }
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
    var hasLoaded by remember(key) { mutableStateOf(cached != null) }
    val bitmap by produceState<Bitmap?>(initialValue = cached, key1 = key) {
        if (cached != null) {
            value = cached
            hasLoaded = true
            return@produceState
        }
        value = null
        val loaded = withContext(Dispatchers.IO) {
            ThumbnailMemory.permits.withPermit {
                // 1. Check RAM LruCache
                ThumbnailMemory.cache.get(key) ?: run {
                    // 2. Check Persistent WebP Disk Cache (Fast decode, zero media decoders)
                    val diskBmp = ThumbnailMemory.loadFromDisk(context, key)
                    if (diskBmp != null) {
                        ThumbnailMemory.cache.put(key, diskBmp)
                        diskBmp
                    } else {
                        var bmp: Bitmap? = null
                        // 3. On Android 10+, request cached system thumbnail from ContentResolver
                        if (Build.VERSION.SDK_INT >= 29) {
                            runCatching {
                                bmp = context.contentResolver.loadThumbnail(video.uri.toUri(), android.util.Size(480, 270), null)
                            }
                        }
                        // 4. Sample frame at 12% into the movie (or 15s in) to bypass black intro frames
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
                        bmp?.also {
                            ThumbnailMemory.cache.put(key, it)
                            ThumbnailMemory.saveToDisk(context, key, it)
                        }
                    }
                }
            }
        }
        value = loaded
        hasLoaded = true
    }
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant) {
        val frame = bitmap
        if (frame != null) {
            Image(frame.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else if (!hasLoaded) {
            ShimmerPlaceholder(Modifier.fillMaxSize())
        } else {
            CinematicFallbackPoster(title = video.title, name = video.name, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ExpressiveLibrarySkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    ShimmerPlaceholder(Modifier.fillMaxSize())
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Scanning your local library…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CinemaFranchiseShelf(
    franchises: List<CinemaFranchise>,
    onSelectFilm: (LocalVideo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Rounded.CollectionsBookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Curated Sagas & Box Sets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    "${franchises.size} Sagas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(franchises, key = { it.id }) { franchise ->
                FranchiseCard(franchise = franchise, onSelectFilm = onSelectFilm)
            }
        }
    }
}

@Composable
private fun FranchiseCard(
    franchise: CinemaFranchise,
    onSelectFilm: (LocalVideo) -> Unit
) {
    val theme = LocalThemeName.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SpatialGlass.cardBg(theme),
        border = SpatialGlass.cardBorder(theme, 0.75.dp),
        shadowElevation = MaterialExpressive.cardElevation(theme),
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                val target = franchise.nextUnwatchedFilm ?: franchise.films.firstOrNull()
                target?.let(onSelectFilm)
            }
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    franchise.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        "${franchise.films.size} Films",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                franchise.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                franchise.films.take(3).forEach { film ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { onSelectFilm(film) }
                    ) {
                        Text(
                            film.title.take(14),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (franchise.films.size > 3) {
                    Text(
                        "+${franchise.films.size - 3}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkSharesShelf(
    networkManager: NetworkStorageManager,
    onAddClick: () -> Unit,
    onSelectShare: (NetworkShare) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Rounded.Lan,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Network & LAN Streaming",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            FilledTonalButton(
                onClick = onAddClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Rounded.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Share", fontSize = 12.sp)
            }
        }

        if (networkManager.shares.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Stream directly from Windows SMB, WebDAV, or UPnP/DLNA with zero local disk space.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(networkManager.shares, key = { it.id }) { share ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        border = subtleCardBorder(strokeWidth = 0.75.dp),
                        modifier = Modifier
                            .width(240.dp)
                            .clickable { onSelectShare(share) }
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    share.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        share.type,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                share.serverAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Tap to stream",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(
                                    onClick = { networkManager.removeShare(share.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Remove Share",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
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
// END OF FILE
