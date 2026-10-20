package com.jadzumi.video

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import kotlin.math.hypot

private val Blue = Color(0xFF0B57D0)
private val PaleBlue = Color(0xFFD3E3FD)
private val Light = lightColorScheme(
    primary = Blue,
    primaryContainer = PaleBlue,
    onPrimaryContainer = Color(0xFF041E49),
    background = Color(0xFFF4F7FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE7EEF8),
    onSurface = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFF44474E),
    outlineVariant = Color(0xFFE2E8F0)
)
private val Dark = darkColorScheme(
    primary = Color(0xFFADC7FF),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFD3E3FD),
    background = Color(0xFF0F1724),
    surface = Color(0xFF182232),
    surfaceVariant = Color(0xFF233044),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
    outlineVariant = Color(0xFF2E3C52)
)

private data class Destination(val name: String, val icon: ImageVector)
private val destinations = listOf(
    Destination("Home", Icons.Rounded.Home),
    Destination("Videos", Icons.Rounded.Movie),
    Destination("TV Shows", Icons.Rounded.Tv),
    Destination("Folders", Icons.Rounded.Folder),
    Destination("Playlists", Icons.AutoMirrored.Rounded.QueueMusic),
    Destination("Settings", Icons.Rounded.Settings)
)

@Composable fun JadzumiLogo(size: Dp = 48.dp, darkSurface: Boolean = false) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size * 0.26f),
        color = if (darkSurface) Color(0xFF132238) else Color.White,
        border = BorderStroke(1.dp, if (darkSurface) Color.White.copy(alpha = 0.18f) else Color(0xFFD3E3FD)),
        shadowElevation = if (darkSurface) 6.dp else 4.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            JadzumiPlaySymbol(Modifier.size(size * 0.68f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun JadzumiApp(playback: PlaybackController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("appearance", 0) }
    var theme by rememberSaveable { mutableStateOf(prefs.getString("theme", "Light") ?: "Light") }
    var reducedMotion by rememberSaveable { mutableStateOf(prefs.getBoolean("reducedMotion", false)) }
    var themeBloomEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("themeBloom", true)) }
    var firstLaunchCardShown by rememberSaveable { mutableStateOf(prefs.getBoolean("pad5_welcome_done", false)) }

    var route by rememberSaveable { mutableStateOf("Home") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var demo by rememberSaveable { mutableStateOf(false) }
    val library = remember { LocalLibrary.get(context) }
    val localState by library.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var cornerStyle by rememberSaveable { mutableStateOf(prefs.getString("cornerStyle", "Rounded") ?: "Rounded") }
    val cornerRadiusDp = when (cornerStyle) {
        "Compact" -> 14.dp
        "Sharp" -> 0.dp
        else -> 24.dp
    }
    var showSplash by rememberSaveable { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val bloomProgress = remember { Animatable(1f) }
    var bloomOriginColor by remember { mutableStateOf<Color?>(null) }

    val sysDark = isSystemInDarkTheme()
    val colors = when (theme) {
        "Dark" -> Dark
        "AMOLED" -> Dark.copy(background = Color.Black, surface = Color(0xFF0A0A0C), surfaceVariant = Color(0xFF141418))
        "System" -> if (sysDark) Dark else Light
        else -> Light
    }

    fun switchThemeWithBloom(newTheme: String) {
        if (newTheme == theme) return
        val oldBg = colors.background
        theme = newTheme
        prefs.edit { putString("theme", newTheme) }
        if (themeBloomEnabled && !reducedMotion) {
            bloomOriginColor = oldBg
            scope.launch {
                bloomProgress.snapTo(0f)
                bloomProgress.animateTo(1f, tween(320))
                bloomOriginColor = null
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        library.autoRefreshOnResume()
    }

    DisposableEffect(colors) {
        val style = if (colors == Light) SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        else SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        (context as? ComponentActivity)?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        onDispose { }
    }

    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(
            small = RoundedCornerShape((cornerRadiusDp / 2).coerceAtLeast(0.dp)),
            medium = RoundedCornerShape(cornerRadiusDp),
            large = RoundedCornerShape((cornerRadiusDp + 4.dp).coerceAtLeast(0.dp))
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            Surface(Modifier.fillMaxSize(), color = colors.background) {
                BackHandler(selectedId != null || (route != "Home" && route != "Library")) {
                    if (selectedId != null) selectedId = null else route = "Home"
                }
                BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
                    val showRail = maxWidth >= 640.dp
                    Row(Modifier.fillMaxSize()) {
                        if (showRail) AppRail(route, colors != Light) { route = it; selectedId = null }
                        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                            val contentWidth = maxWidth
                            val topPad = if (contentWidth >= 720.dp) 24.dp else 16.dp

                            Column(Modifier.fillMaxSize()) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = topPad, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    if (selectedId != null) {
                                        IconButton(onClick = { selectedId = null }) {
                                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to library")
                                        }
                                        Text(
                                            "Library / Details",
                                            Modifier.weight(1f),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else if (route == "Home" || route == "Library") {
                                        Column(Modifier.weight(1f)) {
                                            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                                            val greeting = when (hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; in 17..21 -> "Good evening"; else -> "Good night" }
                                            Text(greeting, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                            Text("Your movies. Your space.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    } else {
                                        Text(
                                            route,
                                            Modifier.weight(1f),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .widthIn(min = 220.dp, max = 340.dp)
                                            .height(42.dp),
                                        shape = RoundedCornerShape(21.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Search,
                                                contentDescription = "Search",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                                if (query.isEmpty()) {
                                                    Text(
                                                        "Search videos, shows...",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                BasicTextField(
                                                    value = query,
                                                    onValueChange = { query = it },
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 13.sp
                                                    ),
                                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            if (query.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { query = "" },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.Close,
                                                        contentDescription = "Clear search",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { library.rescan() },
                                        enabled = !localState.busy && localState.folders.isNotEmpty()
                                    ) {
                                        if (localState.busy) {
                                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Rounded.Sync, "Sync library now", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    Surface(
                                        onClick = { route = "Settings"; selectedId = null },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                }

                                Crossfade(
                                    targetState = selectedId ?: route,
                                    animationSpec = tween(if (reducedMotion) 0 else 220),
                                    modifier = Modifier.weight(1f),
                                    label = "screen"
                                ) { current ->
                                    val video = previewVideos.find { it.id == current }
                                    val localVideo = localState.videos.find { it.uri == current }
                                    if (localVideo != null) LocalDetailPage(localVideo, localState, playback)
                                    else if (video != null) DetailPage(video)
                                    else when (current) {
                                        "Home", "Library" -> if (demo) LibraryPage(query, demo, { demo = it }) { selectedId = it } else LocalLibraryPage(localState, library, playback, query, { selectedId = it }, { demo = true })
                                        "Videos" -> LocalLibraryPage(localState, library, playback, query, { selectedId = it }, { demo = true }, defaultCategory = "Movies")
                                        "TV Shows" -> LocalLibraryPage(localState, library, playback, query, { selectedId = it }, { demo = true }, defaultCategory = "TV Shows")
                                        "Folders" -> LocalFoldersPage(localState, library, contentWidth >= 840.dp) { selectedId = it }
                                        "Settings" -> UnifiedSettingsAndPrivacyScreen(
                                            theme = theme,
                                            onTheme = ::switchThemeWithBloom,
                                            themeBloom = themeBloomEnabled,
                                            onThemeBloom = { themeBloomEnabled = it; prefs.edit { putBoolean("themeBloom", it) } },
                                            reduced = reducedMotion,
                                            onReduced = { reducedMotion = it; prefs.edit { putBoolean("reducedMotion", it) } },
                                            cornerStyle = cornerStyle,
                                            onCornerStyle = {
                                                cornerStyle = it
                                                prefs.edit { putString("cornerStyle", it) }
                                            },
                                            onReplaySplash = { showSplash = true },
                                            localState = localState,
                                            library = library,
                                            playback = playback,
                                            onNavigateFolders = { route = "Folders" }
                                        )
                                        "Playlists" -> EmptyPage(Icons.AutoMirrored.Rounded.QueueMusic, "Your collections, together", "Playlist creation is planned for a later update.")
                                        else -> if (playback.video != null) {
                                            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Text("${playback.video?.title}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                                Button(onClick = { playback.fullscreen = true }) { Text("Open player") }
                                            }
                                        } else {
                                            EmptyPage(Icons.Rounded.PlayCircle, "Your cinema is ready", "Choose an imported video from your library, then tap Play.")
                                        }
                                    }
                                }

                                if (!showRail) {
                                    NavigationBar {
                                        destinations.forEach { d ->
                                            NavigationBarItem(
                                                selected = route == d.name,
                                                onClick = { route = d.name; selectedId = null },
                                                icon = { Icon(d.icon, d.name) },
                                                label = { Text(d.name, maxLines = 1, fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            bloomOriginColor?.let { oldColor ->
                val progress = bloomProgress.value
                if (progress < 1f) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(oldColor.copy(alpha = (1f - progress).coerceIn(0f, 1f)))
                    )
                }
            }

            CinemaPlayer(playback, reducedMotion)

            if (showSplash) {
                JadzumiSplashScreen(
                    theme = theme,
                    onFinished = { showSplash = false }
                )
            }
        }
    }
}

@Composable private fun AppRail(route: String, darkSurface: Boolean, navigate: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.width(96.dp).fillMaxHeight()) {
        Column(Modifier.fillMaxSize().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { navigate("Home") }.padding(bottom = 16.dp)
            ) {
                JadzumiLogo(size = 42.dp, darkSurface = darkSurface)
                Spacer(Modifier.height(4.dp))
                Text("Jadzumi", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                Text("Video", fontWeight = FontWeight.Medium, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            destinations.take(5).forEach { d ->
                val isSelected = route == d.name || (d.name == "Home" && route == "Library")
                NavigationRailItem(
                    selected = isSelected,
                    onClick = { navigate(d.name) },
                    icon = { Icon(d.icon, null) },
                    label = { Text(d.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1) },
                    modifier = Modifier.padding(vertical = 4.dp),
                    colors = NavigationRailItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Spacer(Modifier.weight(1f))
            val isSettingsSelected = route == "Settings"
            NavigationRailItem(
                selected = isSettingsSelected,
                onClick = { navigate("Settings") },
                icon = { Icon(Icons.Rounded.Settings, null) },
                label = { Text("Settings", fontSize = 11.sp, fontWeight = if (isSettingsSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

private data class SettingsCategory(val id: String, val label: String, val icon: ImageVector)
private val settingsCategories = listOf(
    SettingsCategory("Video", "Video", Icons.Rounded.PlayCircle),
    SettingsCategory("Audio", "Audio", Icons.Rounded.VolumeUp),
    SettingsCategory("Subtitles", "Subtitles", Icons.Rounded.Subtitles),
    SettingsCategory("Display", "Display", Icons.Rounded.Palette),
    SettingsCategory("Gestures", "Gestures", Icons.Rounded.TouchApp),
    SettingsCategory("Privacy", "Privacy & About", Icons.Rounded.Security)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun UnifiedSettingsAndPrivacyScreen(
    theme: String,
    onTheme: (String) -> Unit,
    themeBloom: Boolean,
    onThemeBloom: (Boolean) -> Unit,
    reduced: Boolean,
    onReduced: (Boolean) -> Unit,
    cornerStyle: String,
    onCornerStyle: (String) -> Unit,
    onReplaySplash: () -> Unit,
    localState: LocalLibraryState,
    library: LocalLibrary,
    playback: PlaybackController,
    onNavigateFolders: () -> Unit
) {
    var activeCategory by rememberSaveable { mutableStateOf("Privacy") }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    val addFolder = rememberFolderPicker(library)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= 760.dp
        val pad = if (maxWidth >= 720.dp) 24.dp else 16.dp

        Column(Modifier.fillMaxSize().padding(horizontal = pad)) {
            statusNotice?.let { msg ->
                Surface(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { statusNotice = null }) { Text("Dismiss", maxLines = 1, softWrap = false) }
                    }
                }
            }

            if (!twoPane) {
                LazyRow(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(settingsCategories) { cat ->
                        FilterChip(
                            selected = activeCategory == cat.id,
                            onClick = { activeCategory = cat.id },
                            leadingIcon = { Icon(cat.icon, null, Modifier.size(18.dp)) },
                            label = { Text(cat.label, fontWeight = if (activeCategory == cat.id) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                if (twoPane) {
                    Column(
                        Modifier.width(210.dp).fillMaxHeight().padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        settingsCategories.forEach { cat ->
                            val isSelected = activeCategory == cat.id
                            Surface(
                                onClick = { activeCategory = cat.id },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        cat.icon,
                                        null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        cat.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        Text(
                            when (activeCategory) {
                                "Video" -> "Video Settings"
                                "Audio" -> "Audio Settings"
                                "Subtitles" -> "Subtitle Settings"
                                "Display" -> "Display & Appearance"
                                "Gestures" -> "Gestures & Controls"
                                else -> "Privacy & About"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    when (activeCategory) {
                        "Video" -> {
                            item {
                                Panel {
                                    Text("Playback & Decoding", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Configure video decoding pipeline and screen scaling.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Video Decoder", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Snapdragon 860 hardware decoder with NextLib fallback", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("Auto (Recommended) ›", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Hardware Acceleration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Uses Qualcomm Adreno GPU for 60/120 FPS high-efficiency playback", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = true, onCheckedChange = {})
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Video Scaling Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Default aspect ratio preservation on 16:10 display", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("Fit (Keep aspect ratio) ›", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Brightness Boost & HDR", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Force maximum panel luminance and enable wide color gamut HDR10/HDR10+/HLG", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = playback.brightnessBoost, onCheckedChange = { playback.updateBrightnessBoost(it) })
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Rounded.HdrOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                            Column {
                                                Text("Hardware HDR10 / HDR10+ Display Mode", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                                Text("Adreno 640 hardware video surface delivers direct 10-bit wide gamut HDR rendering", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "Audio" -> {
                            item {
                                Panel {
                                    Text("Audio Output & Surround", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("NextLib Media3 FFmpeg decoder for Dolby Atmos / 5.1 / 7.1 surround tracks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Audio Decoder", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Bundled FFmpeg: EAC-3, AC-3, TrueHD, DTS-HD, FLAC, AAC", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("Auto (NextLib FFmpeg) ›", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Downmix to Stereo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Optimized for Xiaomi Pad 5 quad-speaker array and headphones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = true, onCheckedChange = {})
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Screen-Off Background Audio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Continue playback when turning off screen or locking device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = playback.backgroundAudioEnabled, onCheckedChange = { playback.setBackgroundAudio(it) })
                                    }
                                }
                            }
                            item {
                                Panel {
                                    Text("Active Stream Format", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    val currentTrack = playback.audioTracks.firstOrNull()
                                    val audioLabel = if (currentTrack != null) {
                                        "${currentTrack.codec} · ${currentTrack.channels}"
                                    } else "Dolby Atmos / 5.1 / 7.1 / AAC Decoder Ready"
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Icon(Icons.Rounded.Headphones, null, tint = MaterialTheme.colorScheme.primary)
                                            Text(audioLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                }
                            }
                        }

                        "Subtitles" -> {
                            item {
                                val context = LocalContext.current
                                var subFont by remember { mutableStateOf(SubtitlePreferences.getFont(context)) }
                                var subSize by remember { mutableFloatStateOf(SubtitlePreferences.getSize(context)) }
                                var subColor by remember { mutableStateOf(SubtitlePreferences.getColor(context)) }
                                var subBg by remember { mutableStateOf(SubtitlePreferences.getBackground(context)) }
                                var subEdge by remember { mutableStateOf(SubtitlePreferences.getEdge(context)) }

                                Panel {
                                    Text("Subtitle Appearance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Customization and live rendering for SRT, VTT, and embedded SSA/ASS subtitles.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

                                    // Live Subtitle Preview Card
                                    Card(
                                        modifier = Modifier.fillMaxWidth().height(140.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1017))
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                                            Box(
                                                Modifier.fillMaxSize().background(
                                                    Brush.verticalGradient(listOf(Color(0xFF161F2E), Color(0xFF080B0F)))
                                                )
                                            )
                                            val previewTextColor = Color(SubtitlePreferences.getColorInt(subColor))
                                            val previewBgColor = Color(SubtitlePreferences.getBackgroundInt(subBg))
                                            val previewFontFamily = when (subFont) {
                                                "Roboto Condensed" -> androidx.compose.ui.text.font.FontFamily.SansSerif
                                                "Roboto Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
                                                "System Default" -> androidx.compose.ui.text.font.FontFamily.Default
                                                else -> androidx.compose.ui.text.font.FontFamily.SansSerif
                                            }
                                            val previewFontSize = (15.sp * subSize)

                                            Surface(
                                                modifier = Modifier.padding(bottom = 18.dp, start = 16.dp, end = 16.dp),
                                                color = previewBgColor,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "We must reach beyond our own horizon.",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    color = previewTextColor,
                                                    fontSize = previewFontSize,
                                                    fontFamily = previewFontFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Text("Font Family", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SubtitlePreferences.fontOptions.forEach { font ->
                                            FilterChip(
                                                selected = subFont == font,
                                                onClick = {
                                                    subFont = font
                                                    SubtitlePreferences.setFont(context, font)
                                                },
                                                label = { Text(font, maxLines = 1, softWrap = false) }
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Text("Text Size", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SubtitlePreferences.sizeOptions.forEach { (sz, label) ->
                                            FilterChip(
                                                selected = subSize == sz,
                                                onClick = {
                                                    subSize = sz
                                                    SubtitlePreferences.setSize(context, sz)
                                                },
                                                label = { Text(label, maxLines = 1, softWrap = false) }
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Text("Text Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SubtitlePreferences.colorOptions.forEach { colorName ->
                                            val dotColor = Color(SubtitlePreferences.getColorInt(colorName))
                                            FilterChip(
                                                selected = subColor == colorName,
                                                onClick = {
                                                    subColor = colorName
                                                    SubtitlePreferences.setColor(context, colorName)
                                                },
                                                leadingIcon = {
                                                    Box(Modifier.size(12.dp).clip(CircleShape).background(dotColor))
                                                },
                                                label = { Text(colorName, maxLines = 1, softWrap = false) }
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Text("Background Style", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SubtitlePreferences.backgroundOptions.forEach { bgName ->
                                            FilterChip(
                                                selected = subBg == bgName,
                                                onClick = {
                                                    subBg = bgName
                                                    SubtitlePreferences.setBackground(context, bgName)
                                                },
                                                label = { Text(bgName, maxLines = 1, softWrap = false) }
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Text("Edge & Shadow Style", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SubtitlePreferences.edgeOptions.forEach { edgeName ->
                                            FilterChip(
                                                selected = subEdge == edgeName,
                                                onClick = {
                                                    subEdge = edgeName
                                                    SubtitlePreferences.setEdge(context, edgeName)
                                                },
                                                label = { Text(edgeName, maxLines = 1, softWrap = false) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "Display" -> {
                            item {
                                val themeItems = listOf(
                                    Triple("Light", "Airy Pale-Blue", Icons.Rounded.LightMode),
                                    Triple("Dark", "Night Slate", Icons.Rounded.DarkMode),
                                    Triple("AMOLED", "Pure Black", Icons.Rounded.Brightness1)
                                )
                                Panel {
                                    Text("Appearance Theme", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Select appearance mode for ambient viewing.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        themeItems.forEach { (mode, subtitle, icon) ->
                                            ThemeOptionCard(mode, subtitle, icon, theme == mode, { onTheme(mode) }, Modifier.weight(1f))
                                        }
                                    }
                                    HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                    Text("Corner Style Presets", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Curvature geometry applied to cards, sheets, and dialogs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(10.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        listOf(
                                            Triple("Rounded", "24dp curve", Icons.Rounded.Circle),
                                            Triple("Compact", "14dp curve", Icons.Rounded.CropPortrait),
                                            Triple("Sharp", "0dp crisp", Icons.Rounded.CropSquare)
                                        ).forEach { (style, radiusText, icon) ->
                                            val isSel = cornerStyle == style
                                            Surface(
                                                modifier = Modifier.weight(1f).clickable { onCornerStyle(style) },
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(icon, null, tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(style, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                                    Text(radiusText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Theme Transition Blend (Fade)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Smooth fade in / fade out blend when switching themes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = themeBloom, onCheckedChange = onThemeBloom)
                                    }
                                    HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Reduced Motion", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Disable animations for instant screen transitions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = reduced, onCheckedChange = onReduced)
                                    }
                                    HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { onReplaySplash() }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Replay Aurora Splash Screen", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Play the cinematic opening animation in current theme", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("Preview ›", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        "Gestures" -> {
                            item {
                                Panel {
                                    Text("Player Gestures & Seeking", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Configure double-tap seeking and edge swipe controls.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Text("Double-Tap Seek Interval", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(5, 10, 15, 30).forEach { sec ->
                                            FilterChip(
                                                selected = playback.seekIncrementSeconds == sec,
                                                onClick = { playback.updateSeekSeconds(sec) },
                                                label = { Text("$sec seconds", maxLines = 1, softWrap = false) }
                                            )
                                        }
                                    }
                                    HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Swipe Up/Down Gestures", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Left side controls brightness; right side controls volume with deadzone protection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = playback.swipeGesturesEnabled, onCheckedChange = { playback.updateSwipeGestures(it) })
                                    }
                                    HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Long-Press 2x Speed Up", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Press and hold player surface to temporarily speed up to 2.0x", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = true, onCheckedChange = {})
                                    }
                                }
                            }
                        }

                        else -> {
                            // "Privacy & About"
                            item {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Your movies. Your space.", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text(
                                            "Jadzumi Video keeps your video library, playback history, and settings strictly on your tablet. No telemetry, no cloud sync, 100% offline-first.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFECFDF5), border = BorderStroke(1.dp, Color(0xFFA7F3D0))) {
                                                Text("Local-first", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                            }
                                            Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFEFF6FF), border = BorderStroke(1.dp, Color(0xFFBFDBFE))) {
                                                Text("No tracking", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                                            }
                                            Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFF5F3FF), border = BorderStroke(1.dp, Color(0xFFDDD6FE))) {
                                                Text("You're in control", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9))
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                PrivacyLeftPane(localState, playback, addFolder, onNavigateFolders) { statusNotice = it }
                            }
                            item {
                                AboutRightPane(darkSurface = theme == "Dark" || theme == "AMOLED")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun ThemeOptionCard(
    mode: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = border,
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Text(mode, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
            Text(
                if (selected) "Active" else subtitle,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun PrivacyLeftPane(
    localState: LocalLibraryState,
    playback: PlaybackController,
    onAddDirectory: () -> Unit,
    onManagePermissions: () -> Unit,
    onStatus: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("PRIVACY & CONTROLS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Local-only guarantee", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Panel {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(Modifier.weight(1f, fill = false).padding(end = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text("Folder Access", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text(
                        "Jadzumi only scans media directories granted explicitly by you. No background disk sweeps are performed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                FilledTonalButton(
                    onClick = onAddDirectory,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Directory", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("ACTIVE INTERNAL DIRECTORIES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            if (localState.folders.isEmpty()) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                    Text("No directories added yet. Tap Add Directory to grant folder access.", Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    localState.folders.forEach { folder ->
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Folder, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                                    Text("/${folder.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            }
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Modify or revoke media directory permissions at any time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 12.dp).align(Alignment.CenterVertically)
                )
                OutlinedButton(onClick = onManagePermissions, shape = RoundedCornerShape(12.dp)) {
                    Text("Manage Permissions", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                }
            }
        }

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text("Watch History & Playback", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Text(
                "Playback state, resume points, and preferences are kept exclusively on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Remember Playback Progress", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Resumes video position locally on device whenever you reopen a title.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = playback.rememberProgress, onCheckedChange = { playback.updateRememberProgress(it) })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Save Audio & Subtitle Preferences", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Keeps track and delay preferences locally per file.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = playback.saveTrackPrefs, onCheckedChange = { playback.updateSaveTrackPrefs(it) })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                FilledTonalButton(
                    onClick = {
                        playback.clearWatchHistory()
                        onStatus("All saved watch history and resume positions have been cleared.")
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFFF1F2), contentColor = Color(0xFFE11D48)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.DeleteSweep, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clear Watch History", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Storage, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text("App Data & Cache", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Text(
                "All metadata and playback logs remain stored strictly on-device. Optional online metadata fetching is disabled by default.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        clearThumbnailMemoryCache()
                        onStatus("Thumbnail memory cache cleared. Your video files remain untouched.")
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Cached, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clear Cache", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = { onStatus("App settings snapshot verified locally (${localState.folders.size} folders, ${localState.videos.size} indexed titles).") },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.FileDownload, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Export App Settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun AboutRightPane(darkSurface: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("ABOUT JADZUMI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("App Identity", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                JadzumiLogo(size = 60.dp, darkSurface = darkSurface)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Jadzumi", fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text(
                                "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Text("Your movies. Your space.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
            }
        }

        Panel {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(end = 12.dp)) {
                    Text("CREATION & PRODUCT DIRECTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Omkar Patil", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Creator · Product & Design Direction", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Text(
                            "AI-assisted development",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Panel {
            Text("PHILOSOPHY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Crafted to deliver uncompromised video playback with fluid gesture controls, accurate color rendering, and complete privacy. No background telemetry, no ads, no trackers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Text(
            "\"No popcorn included. Sorry.\"",
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable private fun LibraryPage(query: String, demo: Boolean, onDemo: (Boolean)->Unit, onSelect: (String)->Unit) {
    var category by rememberSaveable { mutableStateOf("All videos") }
    val filtered = filterVideos(if (demo) previewVideos else emptyList(), query, category)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Column {
                Text("Your cinema,\nat home.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("A quiet space for the stories you keep.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        }
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Design preview", fontWeight = FontWeight.SemiBold)
                        Text("Sample titles only · no videos imported", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(demo, onDemo, modifier = Modifier.semantics { contentDescription = "Show sample titles" })
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(listOf("All videos", "Movies", "TV & Anime")) { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, maxLines = 1, softWrap = false) })
                }
            }
        }
        if (filtered.isEmpty()) {
            item { EmptyPage(Icons.Rounded.VideoLibrary, if (demo) "No matching titles" else "Make room for your stories", "Try another title or category.") }
        }
        if (filtered.isNotEmpty()) {
            item { Hero(filtered.first(), onSelect) }
            item { Text("Continue watching", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(filtered, key = { it.id }) { video -> VideoRow(video) { onSelect(video.id) } }
        }
    }
}

@Composable private fun Art(video: PreviewVideo, modifier: Modifier = Modifier) {
    val id = when (video.artwork) { "anime" -> R.drawable.anime; "cosmos" -> R.drawable.cosmos; else -> R.drawable.hero }
    Image(painterResource(id), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
}

@Composable private fun Hero(video: PreviewVideo, onSelect: (String)->Unit) {
    Card(onClick = { onSelect(video.id) }, shape = RoundedCornerShape(28.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 240.dp)) {
            Art(video, Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color(0xF0102034), Color(0x550F2038)))))
            Column(Modifier.padding(24.dp).widthIn(max = 580.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("FEATURED · SAMPLE ${video.category.uppercase()}", color = Color(0xFFADC7FF), style = MaterialTheme.typography.labelMedium)
                Text(video.title, color = Color.White, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                Text("Pick up where the story left off.", color = Color.White.copy(alpha = 0.85f))
                Button(onClick = { onSelect(video.id) }) { Icon(Icons.Rounded.Movie, null); Spacer(Modifier.width(8.dp)); Text("Explore details") }
            }
        }
    }
}

@Composable private fun VideoRow(video: PreviewVideo, onSelect: ()->Unit) {
    Card(onClick = onSelect, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Art(video, Modifier.size(96.dp, 72.dp).clip(RoundedCornerShape(14.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(video.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${timecode(video.positionSeconds)} / ${timecode(video.durationSeconds)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { video.progress }, modifier = Modifier.fillMaxWidth().height(4.dp))
            }
            Icon(Icons.Rounded.ChevronRight, "View details")
        }
    }
}

@Composable private fun DetailPage(video: PreviewVideo) {
    var favorite by rememberSaveable(video.id) { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(28.dp))) {
                Art(video, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD9101C30)))))
                Text(video.title, Modifier.align(Alignment.BottomStart).padding(24.dp), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Panel {
                Text("SAMPLE ${video.category.uppercase()}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text("A story waiting to be rediscovered in your personal cinema.", modifier = Modifier.padding(vertical = 12.dp))
                Text("${timecode(video.remainingSeconds)} remaining · ${(video.progress * 100).toInt()}% watched (sample)")
                LinearProgressIndicator(progress = { video.progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {}, enabled = false) { Text("Sample preview only") }
                    IconToggleButton(checked = favorite, onCheckedChange = { favorite = it }) {
                        Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite")
                    }
                }
            }
        }
    }
}

@Composable private fun Panel(content: @Composable ColumnScope.()->Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable private fun EmptyPage(icon: ImageVector, title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
// END OF FILE
