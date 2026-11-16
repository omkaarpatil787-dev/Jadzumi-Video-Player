package com.jadzumi.video

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

private val LiveRed = Color(0xFFEF4444)
private val LavenderAccent = Color(0xFFADC7FF)
private val DarkCardSurface = Color(0xFF161F2E)
private val LightCardSurface = Color(0xFFFFFFFF)

@Composable
fun WebSourcesPage(
    manager: WebSourcesManager,
    playback: PlaybackController,
    effectiveTheme: String,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isEnabled by manager.isFeatureEnabled.collectAsState()
    val sources by manager.sources.collectAsState()
    val channels by manager.channels.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var showAddSourceDialog by remember { mutableStateOf(false) }
    var browserUrl by remember { mutableStateOf<String?>(null) }
    var browserTitle by remember { mutableStateOf("Web Browser") }

    val isDark = effectiveTheme != "Light"
    val cardBg = if (isDark) DarkCardSurface else LightCardSurface
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)

    val categories = remember(channels) {
        val groups = channels.mapNotNull { it.group }.distinct().sorted()
        listOf("All", "Favorites") + groups
    }

    val filteredChannels = remember(channels, searchQuery, selectedCategory) {
        channels.filter { channel ->
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Favorites" -> channel.isFavorite
                else -> channel.group.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                channel.name.contains(searchQuery, ignoreCase = true) ||
                        (channel.group?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesCategory && matchesSearch
        }
    }

    // Embedded Browser Overlay
    browserUrl?.let { url ->
        WebSourcesBrowserOverlay(
            initialUrl = url,
            title = browserTitle,
            onDismiss = { browserUrl = null },
            onPlayStream = { streamUrl, streamName ->
                browserUrl = null
                val channel = LiveChannel(
                    name = streamName,
                    streamUrl = streamUrl,
                    group = "Web Discovery"
                )
                manager.addChannel(channel)
                playback.open(channel.toLocalVideo())
            }
        )
    }

    // Add Source Dialog
    if (showAddSourceDialog) {
        AddWebSourceDialog(
            onDismiss = { showAddSourceDialog = false },
            onAdd = { newSource, m3uContent ->
                manager.addSource(newSource)
                if (newSource.type == WebSourceType.IPTV_PLAYLIST && m3uContent.isNotBlank()) {
                    val parsed = manager.parseM3u(m3uContent, newSource.id)
                    parsed.forEach { manager.addChannel(it) }
                } else if (newSource.type == WebSourceType.IPTV_PLAYLIST && newSource.url.startsWith("http")) {
                    coroutineScope.launch {
                        manager.fetchAndImportM3u(newSource.url, newSource.id)
                    }
                }
                showAddSourceDialog = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Feature Enable / Privacy Capsule Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = if (isEnabled) {
                if (isDark) Color(0xFF1A2639) else Color(0xFFEFF6FF)
            } else {
                if (isDark) Color(0xFF1E1E24) else Color(0xFFF8FAFC)
            },
            border = BorderStroke(
                1.dp,
                if (isEnabled) LavenderAccent.copy(alpha = 0.35f) else cardBorder
            ),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isEnabled) LavenderAccent.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.LiveTv,
                        contentDescription = null,
                        tint = if (isEnabled) LavenderAccent else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Jadzumi Web Sources & Live TV",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isEnabled) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = LiveRed
                            ) {
                                Text(
                                    "ACTIVE",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Text(
                        "Watch live streams & IPTV through Cinema HUD 3.0. Offline local library remains completely isolated.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { manager.setFeatureEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        if (!isEnabled) {
            // Disabled Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        "Web Sources are Disabled",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Enable Web Sources above to browse approved websites, import M3U IPTV playlists, and watch live TV channels with Cinema HUD 3.0.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { manager.setFeatureEnabled(true) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enable Web Sources")
                    }
                }
            }
        } else {
            // Toolbar: Category Filter Pills and Add Source Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category Pills (Horizontal Scroll)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.clickable { selectedCategory = category }
                        ) {
                            Text(
                                category,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                FilledTonalButton(
                    onClick = { showAddSourceDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Source", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Approved Website Directories Bar (e.g. Global Free TV)
            val websiteSources = sources.filter { it.type == WebSourceType.WEBSITE }
            if (websiteSources.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Approved Directory Sources",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        websiteSources.forEach { ws ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = cardBg,
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.clickable {
                                    browserUrl = ws.url
                                    browserTitle = ws.name
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Public, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text(ws.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Tap to Browse Streams", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Rounded.OpenInBrowser, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Live Channels Grid
            if (filteredChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No channels found in this category.\nTap '+ Add Source' to import an M3U playlist or direct HLS stream.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.5.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isTablet) 3 else 1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredChannels, key = { it.id }) { channel ->
                        LiveChannelCard(
                            channel = channel,
                            isDark = isDark,
                            onPlay = { playback.open(channel.toLocalVideo()) },
                            onToggleFavorite = { manager.toggleFavorite(channel.id) },
                            onDelete = { manager.removeSource(channel.sourceId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveChannelCard(
    channel: LiveChannel,
    isDark: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = if (isDark) DarkCardSurface else LightCardSurface
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Category Badge + LIVE Badge + Star
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    channel.group?.let { grp ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                grp.uppercase(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LiveRed
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(Modifier.size(5.dp).background(Color.White, CircleShape))
                            Text(
                                "LIVE",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (channel.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (channel.isFavorite) Color(0xFFFFB800) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Channel Name & Stream Specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        channel.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (channel.streamUrl.contains(".mpd")) "MPEG-DASH Stream • Adaptive" else "HLS Master Stream • Adaptive Rate",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Pill: Watch in Cinema HUD 3.0
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Watch in Cinema HUD 3.0", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AddWebSourceDialog(
    onDismiss: () -> Unit,
    onAdd: (WebSource, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(WebSourceType.DIRECT_STREAM) }
    var m3uContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Web Source", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Add an approved streaming website, direct HLS stream, or M3U IPTV playlist.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Type Capsule Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WebSourceType.values().forEach { t ->
                        val isSelected = type == t
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { type = t },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                when (t) {
                                    WebSourceType.WEBSITE -> "Website"
                                    WebSourceType.DIRECT_STREAM -> "Direct HLS"
                                    WebSourceType.IPTV_PLAYLIST -> "IPTV M3U"
                                },
                                modifier = Modifier.padding(vertical = 8.dp),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Source / Channel Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = {
                        Text(
                            when (type) {
                                WebSourceType.WEBSITE -> "Website URL (https://...)"
                                WebSourceType.DIRECT_STREAM -> "Direct Stream URL (.m3u8 / .mpd)"
                                WebSourceType.IPTV_PLAYLIST -> "M3U Playlist URL or File Link"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (type == WebSourceType.IPTV_PLAYLIST) {
                    OutlinedTextField(
                        value = m3uContent,
                        onValueChange = { m3uContent = it },
                        label = { Text("Or Paste M3U Raw Text (Optional)") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        val source = WebSource(
                            name = name.trim(),
                            url = url.trim(),
                            type = type
                        )
                        onAdd(source, m3uContent)
                    }
                },
                enabled = name.isNotBlank() && url.isNotBlank(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Add Source")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebSourcesBrowserOverlay(
    initialUrl: String,
    title: String,
    onDismiss: () -> Unit,
    onPlayStream: (streamUrl: String, streamName: String) -> Unit
) {
    var detectedStreamUrl by remember { mutableStateOf<String?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler {
            if (webViewRef?.canGoBack() == true) {
                webViewRef?.goBack()
            } else {
                onDismiss()
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Browser Top Toolbar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close Browser")
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                currentUrl,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { webViewRef?.reload() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Reload")
                        }
                    }
                }

                // Stream Detection Banner
                AnimatedVisibility(
                    visible = detectedStreamUrl != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    detectedStreamUrl?.let { stream ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, LavenderAccent)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text(
                                            "Compatible Native Stream Detected!",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            "Ready for native playback in Cinema HUD 3.0",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onPlayStream(stream, title) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Watch Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Android WebView for Browsing & Stream Detection
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = true
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 JadzumiPlayer/2.0.9"

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    url?.let { currentUrl = it }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val reqUrl = request?.url?.toString() ?: ""
                                    if (isStreamUrl(reqUrl)) {
                                        detectedStreamUrl = reqUrl
                                        return true
                                    }
                                    return false
                                }

                                override fun onLoadResource(view: WebView?, url: String?) {
                                    super.onLoadResource(view, url)
                                    url?.let {
                                        if (isStreamUrl(it)) {
                                            detectedStreamUrl = it
                                        }
                                    }
                                }
                            }
                            loadUrl(initialUrl)
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        webViewRef = view
                    }
                )
            }
        }
    }
}

private fun isStreamUrl(url: String): Boolean {
    val clean = url.lowercase()
    return clean.contains(".m3u8") ||
            clean.contains(".mpd") ||
            (clean.contains(".mp4") && (clean.contains("/live/") || clean.contains("stream")))
}
