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
    Destination("Library", Icons.Rounded.VideoLibrary),
    Destination("Folders", Icons.Rounded.FolderOpen),
    Destination("Playlists", Icons.AutoMirrored.Rounded.QueueMusic),
    Destination("Player", Icons.Rounded.PlayCircle),
    Destination("Settings", Icons.Rounded.Settings)
)

@Composable fun JadzumiLogo(size: Dp = 48.dp, darkSurface: Boolean = false) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val bgColors = if (darkSurface) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
        else listOf(Color(0xFFFFFFFF), Color(0xFFE8F1FF), Color(0xFFD3E3FD))
        drawRoundRect(
            brush = Brush.linearGradient(bgColors, Offset.Zero, Offset(w, h)),
            size = Size(w, h),
            cornerRadius = CornerRadius(w * 0.24f, h * 0.24f)
        )
        val backWing = Path().apply {
            moveTo(w * 0.40f, h * 0.21f)
            lineTo(w * 0.80f, h * 0.41f)
            lineTo(w * 0.80f, h * 0.73f)
            lineTo(w * 0.40f, h * 0.53f)
            close()
        }
        drawPath(
            path = backWing,
            brush = Brush.linearGradient(
                listOf(Color(0xFF3B82F6), Color(0xFF0B57D0), Color(0xFF1D4ED8)),
                Offset(w * 0.4f, h * 0.2f),
                Offset(w * 0.8f, h * 0.75f)
            )
        )
        val frontPlay = Path().apply {
            moveTo(w * 0.26f, h * 0.29f)
            lineTo(w * 0.66f, h * 0.50f)
            lineTo(w * 0.26f, h * 0.71f)
            close()
        }
        drawPath(
            path = frontPlay,
            brush = Brush.linearGradient(
                listOf(Color.White, Color(0xFFEFF6FF), Color(0xFFBFDBFE)),
                Offset(w * 0.26f, h * 0.29f),
                Offset(w * 0.66f, h * 0.71f)
            )
        )
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

    var route by rememberSaveable { mutableStateOf("Library") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var demo by rememberSaveable { mutableStateOf(false) }
    val library = remember { LocalLibrary.get(context) }
    val localState by library.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }

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
                bloomProgress.animateTo(1f, tween(580, easing = FastOutSlowInEasing))
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

    MaterialTheme(colorScheme = colors, shapes = Shapes(medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(28.dp))) {
        Box(Modifier.fillMaxSize()) {
            Surface(Modifier.fillMaxSize(), color = colors.background) {
                BackHandler(selectedId != null || route != "Library") {
                    if (selectedId != null) selectedId = null else route = "Library"
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
                                    Modifier.fillMaxWidth().padding(horizontal = topPad, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (selectedId != null) {
                                        IconButton(onClick = { selectedId = null }) {
                                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to library")
                                        }
                                    }
                                    if (route == "Library" && selectedId == null) {
                                        OutlinedTextField(
                                            value = query,
                                            onValueChange = { query = it },
                                            modifier = Modifier.weight(1f).widthIn(max = 520.dp),
                                            placeholder = { Text("Search your video library", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(32.dp),
                                            leadingIcon = { Icon(Icons.Rounded.Search, null) }
                                        )
                                    } else {
                                        Text(
                                            if (selectedId != null) "Library / Details" else route,
                                            Modifier.weight(1f),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { library.rescan() },
                                        enabled = !localState.busy && localState.folders.isNotEmpty()
                                    ) {
                                        Icon(Icons.Rounded.Sync, "Sync library now", tint = MaterialTheme.colorScheme.primary)
                                    }

                                    if (contentWidth >= 520.dp) {
                                        AssistChip(
                                            onClick = { route = "Settings"; selectedId = null },
                                            label = { Text("Privacy & About", maxLines = 1, softWrap = false) },
                                            leadingIcon = { Icon(Icons.Rounded.VerifiedUser, null, Modifier.size(16.dp)) }
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = !firstLaunchCardShown && route == "Library") {
                                    Surface(
                                        Modifier.fillMaxWidth().padding(horizontal = topPad, vertical = 6.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Row(
                                            Modifier.padding(18.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            JadzumiLogo(size = 44.dp)
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    "Welcome to Jadzumi Player",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    "16:10 Display Detected · Hardware MediaCodec & Quad-Speaker Stereo Downmix Ready · 100% Offline",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                                )
                                            }
                                            TextButton(onClick = {
                                                firstLaunchCardShown = true
                                                prefs.edit { putBoolean("pad5_welcome_done", true) }
                                            }) {
                                                Text("Got it", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                            }
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
                                        "Library" -> if (demo) LibraryPage(query, demo, { demo = it }) { selectedId = it } else LocalLibraryPage(localState, library, query, { selectedId = it }, { demo = true })
                                        "Folders" -> LocalFoldersPage(localState, library, contentWidth >= 840.dp) { selectedId = it }
                                        "Settings" -> UnifiedSettingsAndPrivacyScreen(
                                            theme = theme,
                                            onTheme = ::switchThemeWithBloom,
                                            themeBloom = themeBloomEnabled,
                                            onThemeBloom = { themeBloomEnabled = it; prefs.edit { putBoolean("themeBloom", it) } },
                                            reduced = reducedMotion,
                                            onReduced = { reducedMotion = it; prefs.edit { putBoolean("reducedMotion", it) } },
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
                    Canvas(Modifier.fillMaxSize()) {
                        val maxRadius = hypot(size.width, size.height)
                        val currentRadius = maxRadius * (1f - progress)
                        drawCircle(
                            color = oldColor.copy(alpha = (1f - progress).coerceIn(0f, 1f)),
                            radius = currentRadius,
                            center = Offset(size.width * 0.5f, size.height * 0.35f)
                        )
                    }
                }
            }

            CinemaPlayer(playback, reducedMotion)
        }
    }
}

@Composable private fun AppRail(route: String, darkSurface: Boolean, navigate: (String)->Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.width(92.dp).fillMaxHeight().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.clickable { navigate("Settings") }) {
                JadzumiLogo(size = 46.dp, darkSurface = darkSurface)
            }
            Text("Jadzumi", Modifier.padding(top = 8.dp, bottom = 24.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            destinations.take(4).forEach { d ->
                NavigationRailItem(
                    selected = route == d.name,
                    onClick = { navigate(d.name) },
                    icon = { Icon(d.icon, null) },
                    label = { Text(d.name, fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            NavigationRailItem(
                selected = route == "Settings",
                onClick = { navigate("Settings") },
                icon = { Icon(Icons.Rounded.Settings, null) },
                label = { Text("Settings", fontSize = 11.sp, maxLines = 1) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun UnifiedSettingsAndPrivacyScreen(
    theme: String,
    onTheme: (String) -> Unit,
    themeBloom: Boolean,
    onThemeBloom: (Boolean) -> Unit,
    reduced: Boolean,
    onReduced: (Boolean) -> Unit,
    localState: LocalLibraryState,
    library: LocalLibrary,
    playback: PlaybackController,
    onNavigateFolders: () -> Unit
) {
    var topTab by rememberSaveable { mutableStateOf("Privacy & About") }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    val addFolder = rememberFolderPicker(library)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= 840.dp
        val pad = if (maxWidth >= 720.dp) 28.dp else 18.dp
        val fourColThemes = maxWidth >= 680.dp

        Column(Modifier.fillMaxSize().padding(horizontal = pad)) {
            // Adaptive Header: Never squeezes "Fully Local & Private" into a narrow vertical column
            FlowRow(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Privacy & About", "Appearance & Themes", "Playback & Audio").forEach { tabName ->
                        FilterChip(
                            selected = topTab == tabName,
                            onClick = { topTab = tabName },
                            label = {
                                Text(
                                    tabName,
                                    fontWeight = if (topTab == tabName) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
                Surface(
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        Text(
                            "Fully Local & Private",
                            color = Color(0xFF047857),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            statusNotice?.let { msg ->
                Surface(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
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

            when (topTab) {
                "Privacy & About" -> {
                    if (twoPane) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            LazyColumn(
                                Modifier.weight(0.57f).fillMaxHeight(),
                                contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                item { PrivacyLeftPane(localState, playback, addFolder, onNavigateFolders) { statusNotice = it } }
                            }
                            LazyColumn(
                                Modifier.weight(0.43f).fillMaxHeight(),
                                contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                item { AboutRightPane(darkSurface = theme == "Dark" || theme == "AMOLED") }
                            }
                        }
                    } else {
                        // Single-column layout for portrait / split-screen with balanced spacing
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            item { PrivacyLeftPane(localState, playback, addFolder, onNavigateFolders) { statusNotice = it } }
                            item { AboutRightPane(darkSurface = theme == "Dark" || theme == "AMOLED") }
                        }
                    }
                }

                "Appearance & Themes" -> {
                    val themeItems = listOf(
                        Triple("Light", "Airy Pale-Blue", Icons.Rounded.LightMode),
                        Triple("Dark", "Night Slate", Icons.Rounded.DarkMode),
                        Triple("AMOLED", "Pure Black", Icons.Rounded.Brightness1),
                        Triple("System", "Auto Sync", Icons.Rounded.Devices)
                    )
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        item {
                            Panel {
                                Text("Base Theme & Display Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    "Switch between airy light, deep dark slate, or 0% pixel-draw AMOLED black.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                )
                                if (fourColThemes) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        themeItems.forEach { (mode, subtitle, icon) ->
                                            ThemeOptionCard(mode, subtitle, icon, theme == mode, { onTheme(mode) }, Modifier.weight(1f))
                                        }
                                    }
                                } else {
                                    // 2x2 grid on narrower portrait/split-screen widths so cards never squash
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        themeItems.chunked(2).forEach { pair ->
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                pair.forEach { (mode, subtitle, icon) ->
                                                    ThemeOptionCard(mode, subtitle, icon, theme == mode, { onTheme(mode) }, Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Panel {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text("Theme Bloom Circular Reveal (580ms)", fontWeight = FontWeight.Bold)
                                        Text("Smooth radial wave transition when switching between themes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = themeBloom, onCheckedChange = onThemeBloom)
                                }
                                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text("Reduced Motion", fontWeight = FontWeight.Bold)
                                        Text("Disable screen fades and spring animations for instant transitions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = reduced, onCheckedChange = onReduced, modifier = Modifier.semantics { contentDescription = "Reduced motion" })
                                }
                            }
                        }
                    }
                }

                "Playback & Audio" -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        item {
                            Panel {
                                Text("Double-Tap Seek Interval", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Choose how many seconds to jump when double-tapping in the player.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(5, 10, 15, 30).forEach { sec ->
                                        FilterChip(
                                            selected = playback.seekIncrementSeconds == sec,
                                            onClick = { playback.updateSeekSeconds(sec) },
                                            label = { Text("$sec seconds", maxLines = 1, softWrap = false) }
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Panel {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text("Vertical Swipe HUD Gestures", fontWeight = FontWeight.Bold)
                                        Text("Left vertical swipe controls brightness; right controls volume.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = playback.swipeGesturesEnabled, onCheckedChange = { playback.updateSwipeGestures(it) })
                                }
                                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text("Screen-Off Background Audio", fontWeight = FontWeight.Bold)
                                        Text("Keep playing audio with lock-screen controls when screen is off.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = playback.backgroundAudioEnabled, onCheckedChange = { playback.setBackgroundAudio(it) })
                                }
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
