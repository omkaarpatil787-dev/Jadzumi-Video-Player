package com.jadzumi.video

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch

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
    Destination("Movies", Icons.Rounded.Movie),
    Destination("TV Shows", Icons.Rounded.Tv),
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
    var theme by rememberSaveable { mutableStateOf(prefs.getString("theme", "Dark") ?: "Dark") }
    var reducedMotion by rememberSaveable { mutableStateOf(prefs.getBoolean("reducedMotion", false)) }
    var livelyAnimations by rememberSaveable { mutableStateOf(prefs.getBoolean("livelyAnimations", true)) }
    var themeBloomEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("themeBloom", true)) }
    var firstLaunchCardShown by rememberSaveable { mutableStateOf(prefs.getBoolean("pad5_welcome_done", false)) }

    var route by rememberSaveable { mutableStateOf("Home") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var demo by rememberSaveable { mutableStateOf(false) }
    val library = remember { LocalLibrary.get(context) }
    val localState by library.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var cornerStyle by rememberSaveable { mutableStateOf(prefs.getString("cornerStyle", "Rounded") ?: "Rounded") }
    var bottomBarStyle by rememberSaveable { mutableStateOf(prefs.getString("bottomBarStyle", "Icons and labels") ?: "Icons and labels") }
    var dockAutoMinimize by rememberSaveable { mutableStateOf(prefs.getBoolean("dockAutoMinimize", true)) }
    var isDockMinimized by rememberSaveable { mutableStateOf(false) }
    var glassIntensity by rememberSaveable { mutableStateOf(prefs.getFloat("glassIntensity", 0.75f)) }
    val cornerRadiusDp = when (cornerStyle) {
        "Compact" -> 14.dp
        "Sharp" -> 0.dp
        else -> 24.dp
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    var showSplash by rememberSaveable { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val bloomProgress = remember { Animatable(1f) }
    var bloomOriginColor by remember { mutableStateOf<Color?>(null) }

    val sysDark = isSystemInDarkTheme()
    val colors = when (theme) {
        "Light" -> Light
        "Dark" -> Dark
        "AMOLED" -> Dark.copy(background = Color.Black, surface = Color(0xFF0A0A0C), surfaceVariant = Color(0xFF141418))
        else -> if (sysDark) Dark else Light
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
        CompositionLocalProvider(LocalGlassIntensity provides glassIntensity) {
            Box(Modifier.fillMaxSize()) {
            Surface(Modifier.fillMaxSize(), color = colors.background) {
                BackHandler(query.isNotEmpty() || selectedId != null || (route != "Home" && route != "Library")) {
                    if (query.isNotEmpty()) {
                        query = ""
                    } else if (selectedId != null) {
                        selectedId = null
                    } else {
                        route = "Home"
                    }
                }
                BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
                    val isTablet = maxWidth >= 640.dp
                    val contentWidth = maxWidth
                    val isPhone = contentWidth < 600.dp
                    val topPad = if (contentWidth >= 720.dp) 24.dp else 16.dp

                    Box(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize()) {
                                    if (isPhone && selectedId == null) {
                                        // Adaptive Phone Top Bar (Greeting + Actions, Full-width frosted search capsule)
                                        Column(Modifier.fillMaxWidth().padding(horizontal = topPad, vertical = 6.dp)) {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                if (route == "Home" || route == "Library") {
                                                    InteractiveCinemaGreeting(
                                                        localState = localState,
                                                        playback = playback,
                                                        isPhone = true,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                } else {
                                                    Text(
                                                        route,
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
                                                    if (localState.busy) {
                                                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                                    } else {
                                                        Icon(Icons.Rounded.Sync, "Sync library now", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.height(6.dp))

                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(42.dp),
                                                shape = RoundedCornerShape(21.dp),
                                                color = GlassTheme.surfaceColor(theme, glassIntensity),
                                                border = BorderStroke(1.dp, GlassTheme.borderColor(theme, glassIntensity))
                                            ) {
                                                Row(
                                                    Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.Search,
                                                        contentDescription = "Search",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
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
                                                            onValueChange = { newQuery ->
                                                                query = newQuery
                                                                if (newQuery.isNotBlank()) {
                                                                    selectedId = null
                                                                    if (route == "Folders") {
                                                                        route = "Home"
                                                                    }
                                                                }
                                                            },
                                                            singleLine = true,
                                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                fontSize = 13.sp
                                                            ),
                                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                    if (query.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = {
                                                                query = ""
                                                                keyboardController?.hide()
                                                            },
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
                                        }
                                    } else {
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
                                                InteractiveCinemaGreeting(
                                                    localState = localState,
                                                    playback = playback,
                                                    isPhone = false,
                                                    modifier = Modifier.weight(1f)
                                                )
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
                                                color = GlassTheme.surfaceColor(theme, glassIntensity),
                                                border = BorderStroke(1.dp, GlassTheme.borderColor(theme, glassIntensity))
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
                                                            onValueChange = { newQuery ->
                                                                query = newQuery
                                                                if (newQuery.isNotBlank()) {
                                                                    selectedId = null
                                                                    if (route == "Folders") {
                                                                        route = "Home"
                                                                    }
                                                                }
                                                            },
                                                            singleLine = true,
                                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                fontSize = 13.sp
                                                            ),
                                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                    if (query.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = {
                                                                query = ""
                                                                keyboardController?.hide()
                                                            },
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
                                        }
                                    }

                                    AnimatedContent(
                                        targetState = selectedId ?: route,
                                        transitionSpec = {
                                            if (reducedMotion || !livelyAnimations) {
                                                fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                                            } else {
                                                val mainTabs = setOf("Home", "Library", "Movies", "Videos", "TV Shows", "Folders", "Settings", "Playlists")
                                                val isTargetDetails = targetState !in mainTabs
                                                val isInitialDetails = initialState !in mainTabs

                                                if (!isInitialDetails && isTargetDetails) {
                                                    // Navigating into Movie / TV Details: fluid slide in from right with subtle fade
                                                    (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } +
                                                            fadeIn(animationSpec = tween(220)))
                                                        .togetherWith(
                                                            slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it / 4 } +
                                                                    fadeOut(animationSpec = tween(180))
                                                        )
                                                } else if (isInitialDetails && !isTargetDetails) {
                                                    // Returning from Details back to Library: reverse slide out to right
                                                    (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it / 4 } +
                                                            fadeIn(animationSpec = tween(180)))
                                                        .togetherWith(
                                                            slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } +
                                                                    fadeOut(animationSpec = tween(220))
                                                        )
                                                } else {
                                                    // Navigating between tabs: silky smooth shared-axis animated crossfade (no fast flicker fade)
                                                    val tabOrder = listOf("Home", "Library", "Movies", "TV Shows", "Videos", "Folders", "Settings", "Playlists")
                                                    val initialIndex = tabOrder.indexOf(initialState)
                                                    val targetIndex = tabOrder.indexOf(targetState)
                                                    val isForward = if (initialIndex != -1 && targetIndex != -1) targetIndex > initialIndex else true

                                                    (slideInHorizontally(animationSpec = spring(dampingRatio = 0.90f, stiffness = Spring.StiffnessMediumLow)) { if (isForward) (it * 0.08f).toInt() else (-it * 0.08f).toInt() } +
                                                            fadeIn(animationSpec = tween(260, easing = LinearOutSlowInEasing)))
                                                        .togetherWith(
                                                            slideOutHorizontally(animationSpec = spring(dampingRatio = 0.90f, stiffness = Spring.StiffnessMediumLow)) { if (isForward) (-it * 0.08f).toInt() else (it * 0.08f).toInt() } +
                                                                    fadeOut(animationSpec = tween(190, easing = FastOutLinearInEasing))
                                                        )
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentKey = { it },
                                        label = "screen"
                                    ) { current ->
                                        val video = previewVideos.find { it.id == current }
                                        val localVideo = localState.videos.find { it.uri == current }
                                        if (current?.startsWith("series:") == true) {
                                            val seriesId = current.removePrefix("series:")
                                            val series = localState.tvSeries.find { it.id == seriesId }
                                            if (series != null) {
                                                TvShowDetailsPage(
                                                    series = series,
                                                    library = library,
                                                    playback = playback,
                                                    onBack = { selectedId = null }
                                                )
                                            } else {
                                                LocalLibraryPage(localState, library, playback, query, { selectedId = it }, defaultCategory = "TV Shows")
                                            }
                                        } else if (localVideo != null) {
                                            if (localVideo.episode == null) {
                                                MovieDetailsPage(
                                                    video = localVideo,
                                                    state = localState,
                                                    playback = playback,
                                                    onBack = { selectedId = null }
                                                )
                                            } else {
                                                val series = localState.tvSeries.find { it.seasons.values.any { eps -> eps.any { ep -> ep.identity == localVideo.identity } } }
                                                if (series != null) {
                                                    TvShowDetailsPage(
                                                        series = series,
                                                        library = library,
                                                        playback = playback,
                                                        onBack = { selectedId = null }
                                                    )
                                                } else {
                                                    MovieDetailsPage(
                                                        video = localVideo,
                                                        state = localState,
                                                        playback = playback,
                                                        onBack = { selectedId = null }
                                                    )
                                                }
                                            }
                                        } else if (video != null) DetailPage(video)
                                        else when (current) {
                                            "Home", "Library" -> LocalLibraryPage(localState, library, playback, query, { selectedId = it }, onScrollActive = { if (dockAutoMinimize) isDockMinimized = true })
                                            "Movies", "Videos" -> LocalLibraryPage(localState, library, playback, query, { selectedId = it }, defaultCategory = "Movies", onScrollActive = { if (dockAutoMinimize) isDockMinimized = true })
                                            "TV Shows" -> LocalLibraryPage(localState, library, playback, query, { selectedId = it }, defaultCategory = "TV Shows", onScrollActive = { if (dockAutoMinimize) isDockMinimized = true })
                                            "Folders" -> UnifiedSettingsAndPrivacyScreen(
                                                theme = theme,
                                                onTheme = ::switchThemeWithBloom,
                                                themeBloom = themeBloomEnabled,
                                                onThemeBloom = { themeBloomEnabled = it; prefs.edit { putBoolean("themeBloom", it) } },
                                                reduced = reducedMotion,
                                                onReduced = { reducedMotion = it; prefs.edit { putBoolean("reducedMotion", it) } },
                                                livelyAnimations = livelyAnimations,
                                                onLivelyAnimations = { livelyAnimations = it; prefs.edit { putBoolean("livelyAnimations", it) } },
                                                cornerStyle = cornerStyle,
                                                onCornerStyle = {
                                                    cornerStyle = it
                                                    prefs.edit { putString("cornerStyle", it) }
                                                },
                                                bottomBarStyle = bottomBarStyle,
                                                onBottomBarStyle = {
                                                    bottomBarStyle = it
                                                    prefs.edit { putString("bottomBarStyle", it) }
                                                },
                                                dockAutoMinimize = dockAutoMinimize,
                                                onDockAutoMinimize = {
                                                    dockAutoMinimize = it
                                                    prefs.edit { putBoolean("dockAutoMinimize", it) }
                                                },
                                                glassIntensity = glassIntensity,
                                                onGlassIntensity = {
                                                    glassIntensity = it
                                                    prefs.edit { putFloat("glassIntensity", it) }
                                                },
                                                onReplaySplash = { showSplash = true },
                                                localState = localState,
                                                library = library,
                                                playback = playback,
                                                initialCategory = "Folders"
                                            )
                                            "Settings" -> UnifiedSettingsAndPrivacyScreen(
                                                theme = theme,
                                                onTheme = ::switchThemeWithBloom,
                                                themeBloom = themeBloomEnabled,
                                                onThemeBloom = { themeBloomEnabled = it; prefs.edit { putBoolean("themeBloom", it) } },
                                                reduced = reducedMotion,
                                                onReduced = { reducedMotion = it; prefs.edit { putBoolean("reducedMotion", it) } },
                                                livelyAnimations = livelyAnimations,
                                                onLivelyAnimations = { livelyAnimations = it; prefs.edit { putBoolean("livelyAnimations", it) } },
                                                cornerStyle = cornerStyle,
                                                onCornerStyle = {
                                                    cornerStyle = it
                                                    prefs.edit { putString("cornerStyle", it) }
                                                },
                                                bottomBarStyle = bottomBarStyle,
                                                onBottomBarStyle = {
                                                    bottomBarStyle = it
                                                    prefs.edit { putString("bottomBarStyle", it) }
                                                },
                                                dockAutoMinimize = dockAutoMinimize,
                                                onDockAutoMinimize = {
                                                    dockAutoMinimize = it
                                                    prefs.edit { putBoolean("dockAutoMinimize", it) }
                                                },
                                                glassIntensity = glassIntensity,
                                                onGlassIntensity = {
                                                    glassIntensity = it
                                                    prefs.edit { putFloat("glassIntensity", it) }
                                                },
                                                onReplaySplash = { showSplash = true },
                                                localState = localState,
                                                library = library,
                                                playback = playback
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
                                }

                                // Floating Frosted Glass Bottom Navigation Bar (Universal across Phones, Foldables & Tablets)
                                if (selectedId == null) {
                                    FloatingFrostedBottomBar(
                                        route = route,
                                        theme = theme,
                                        bottomBarStyle = bottomBarStyle,
                                        glassIntensity = glassIntensity,
                                        isMinimized = isDockMinimized,
                                        isTablet = isTablet,
                                        onToggleMinimize = { isDockMinimized = !isDockMinimized },
                                        onNavigate = {
                                            if (isDockMinimized) isDockMinimized = false
                                            route = it
                                            selectedId = null
                                        },
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = if (isTablet) 12.dp else 6.dp)
                                    )
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

            CinemaPlayer(playback, reducedMotion, glassIntensity, theme)

            if (showSplash) {
                JadzumiSplashScreen(
                    theme = theme,
                    onFinished = { showSplash = false }
                )
            }
        }
        }
    }
}

@Composable
private fun InteractiveCinemaGreeting(
    localState: LocalLibraryState,
    playback: PlaybackController,
    isPhone: Boolean,
    modifier: Modifier = Modifier
) {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val baseGreeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }

    var factIndex by rememberSaveable { mutableStateOf(0) }

    val cinemaFacts = remember {
        listOf(
            "💡 Fun fact: Akira Kurosawa dyed rain with ink to film epic downpours.",
            "🎬 Cinema trivia: The 24fps motion picture standard was born in 1927.",
            "🍿 Pro tip: MediaCodec 10-bit HEVC decoding saves 60% battery life.",
            "✨ Fun fact: 2001: A Space Odyssey used 0 CGI — 100% physical models.",
            "🎧 Studio audio: NextLib FFmpeg powers true 5.1 & 7.1 discrete surround sound.",
            "🎥 Trivia: The iconic Jurassic Park T-Rex roar was mixed from a baby elephant & alligator.",
            "🎞️ Fun fact: The iconic green falling code in The Matrix is actually sushi recipes.",
            "🛡️ Pure privacy: 100% offline cinema vault, zero telemetry."
        )
    }

    // Check recent watch history
    val inProgressEpisode = remember(localState.videos, localState.tvSeries, playback.historyVersion) {
        localState.videos.filter { it.episode != null && playback.savedPosition(it) > 0 }.maxByOrNull { playback.savedPosition(it) }
    }
    val inProgressMovie = remember(localState.videos, playback.historyVersion) {
        localState.videos.filter { it.episode == null && playback.savedPosition(it) > 0 }.maxByOrNull { playback.savedPosition(it) }
    }

    val epInfo = inProgressEpisode?.let { EpisodePatterns.parse(it.name) }
    val series = inProgressEpisode?.let { ep ->
        localState.tvSeries.find { it.seasons.values.any { list -> list.any { v -> v.identity == ep.identity } } }
    }

    val currentFact = cinemaFacts[factIndex % cinemaFacts.size]

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { factIndex++ }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (inProgressEpisode != null || inProgressMovie != null) Color(0xFF1E8E3E) else MaterialTheme.colorScheme.primary)
            )
            Text(
                text = when {
                    factIndex > 0 -> "$baseGreeting, Cinephile"
                    series != null && epInfo != null -> "$baseGreeting · ${series.title}"
                    inProgressMovie != null -> "$baseGreeting · ${inProgressMovie.title}"
                    else -> baseGreeting
                },
                style = if (isPhone) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(1.dp))

        AnimatedContent(
            targetState = when {
                factIndex > 0 -> currentFact
                series != null && epInfo != null -> "Watching S%02dE%02d · Tap to cycle fun facts 🍿".format(epInfo.season, epInfo.episode)
                inProgressMovie != null -> "Resume from ${timecode(playback.savedPosition(inProgressMovie) / 1000)} · Tap for cinema facts 🎬"
                else -> currentFact
            },
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
            },
            label = "greetingSubtitle"
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GeminiGlowingJadzumiLogo(
    darkSurface: Boolean = true,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "geminiLogoGlow")

    // Smooth continuous aura rotation (Google Gemini celestial chromatic drift)
    val auraRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "geminiRotation"
    )

    // Breathing glow intensity pulse (alive and glooming)
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "geminiPulse"
    )

    // Breathing scale for organic celestial bloom
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "geminiScale"
    )

    // Strictly Non-Clickable container
    Box(
        modifier = modifier.size(size + 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Google Gemini Celestial Aurora Halo (Cyan -> Blue -> Cosmic Violet -> Coral Magenta -> Gold -> Cyan)
        Canvas(
            modifier = Modifier
                .size(size * 1.55f)
                .scale(auraScale)
                .rotate(auraRotation)
                .alpha(glowPulse)
        ) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF64DCFF), // Electric Sky Cyan
                        Color(0xFF247BFF), // Vibrant Blue
                        Color(0xFF9B51E0), // Cosmic Violet (Gemini signature)
                        Color(0xFFFF5376), // Radiant Coral Magenta
                        Color(0xFFFFB300), // Subtle Gold Shimmer
                        Color(0xFF64DCFF)  // Back to Cyan
                    )
                )
            )
        }

        // 2. Diffuse Ambient Blur Layer to soften the chromatic bloom
        Canvas(
            modifier = Modifier
                .size(size * 1.35f)
                .alpha(glowPulse * 0.75f)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF64DCFF).copy(alpha = 0.70f),
                        Color(0xFF9B51E0).copy(alpha = 0.40f),
                        Color.Transparent
                    )
                )
            )
        }

        // 3. Crisp Frosted Glass Tile (Non-Clickable)
        Surface(
            modifier = Modifier.size(size),
            shape = RoundedCornerShape(size * 0.28f),
            color = if (darkSurface) Color(0xFF0B192C).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.94f),
            border = BorderStroke(
                1.dp,
                Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF64DCFF).copy(alpha = 0.90f),
                        Color(0xFF9B51E0).copy(alpha = 0.75f),
                        Color(0xFFFF5376).copy(alpha = 0.65f),
                        Color(0xFF64DCFF).copy(alpha = 0.90f)
                    )
                )
            ),
            shadowElevation = 8.dp
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                JadzumiPlaySymbol(Modifier.size(size * 0.66f))
            }
        }
    }
}

@Composable private fun FloatingFrostedBottomBar(
    route: String,
    theme: String,
    bottomBarStyle: String,
    glassIntensity: Float = 0.75f,
    isMinimized: Boolean,
    isTablet: Boolean = false,
    onToggleMinimize: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navDestinations = remember {
        listOf(
            Destination("Home", Icons.Rounded.Home),
            Destination("Movies", Icons.Rounded.Movie),
            Destination("TV Shows", Icons.Rounded.Tv),
            Destination("Settings", Icons.Rounded.Settings)
        )
    }

    val isIconsOnly = bottomBarStyle == "Icons only"
    val activeDestination = navDestinations.firstOrNull { it.name == route || (it.name == "Home" && route == "Library") }
        ?: navDestinations.first()

    // Fluid smooth animations (Apple / M3 smooth settling, no excessive bounce)
    val dockHeight by animateDpAsState(
        targetValue = when {
            isMinimized -> 52.dp
            isIconsOnly -> 54.dp
            else -> 64.dp
        },
        animationSpec = spring(
            dampingRatio = 0.88f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dockHeight"
    )

    val dockWidth by animateDpAsState(
        targetValue = when {
            isMinimized -> if (isTablet) 100.dp else 88.dp
            isIconsOnly -> if (isTablet) 420.dp else 360.dp
            else -> if (isTablet) 540.dp else 470.dp
        },
        animationSpec = spring(
            dampingRatio = 0.88f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dockWidth"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isMinimized) 26.dp else 32.dp,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow),
        label = "dockCorner"
    )

    Surface(
        modifier = modifier
            .widthIn(max = dockWidth)
            .then(if (!isMinimized) Modifier.fillMaxWidth() else Modifier.width(dockWidth))
            .padding(horizontal = if (isMinimized) 0.dp else 18.dp, vertical = 4.dp)
            .height(dockHeight)
            .clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = GlassTheme.surfaceColor(theme, glassIntensity),
        border = BorderStroke(
            1.dp,
            GlassTheme.borderColor(theme, glassIntensity)
        ),
        shadowElevation = if (isMinimized) 18.dp else 14.dp
    ) {
        if (isMinimized) {
            // Minimized Floating Pill: Tap to expand with fluid bouncy pop
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onToggleMinimize)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GeminiGlowingJadzumiLogo(
                    darkSurface = theme != "Light",
                    size = 22.dp
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = activeDestination.icon,
                    contentDescription = "Expand navigation",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            // Full Floating Frosted Dock: Fluid single-action tap selection without double visual flashes
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isTablet) 12.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. Non-clickable Google Gemini-style Alive Glooming Logo
                GeminiGlowingJadzumiLogo(
                    darkSurface = theme != "Light",
                    size = if (isIconsOnly) 28.dp else 32.dp
                )

                // Subtle frosted vertical glass divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(if (isIconsOnly) 22.dp else 28.dp)
                        .background(
                            if (theme == "Light") Color(0xFFD3E3FD).copy(alpha = 0.8f)
                            else Color.White.copy(alpha = 0.16f)
                        )
                )

                // 2. Navigation Destinations
                navDestinations.forEach { d ->
                    val isSelected = route == d.name || (d.name == "Home" && route == "Library")
                    val activeColor = MaterialTheme.colorScheme.primary
                    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow),
                        label = "dockIconScale_${d.name}"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = remember(d.name) { MutableInteractionSource() },
                                indication = ripple(bounded = true, radius = 26.dp)
                            ) {
                                if (!isSelected) {
                                    onNavigate(d.name)
                                }
                            }
                            .padding(horizontal = if (isIconsOnly) 14.dp else 10.dp, vertical = if (isIconsOnly) 6.dp else 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isIconsOnly) {
                            // Ultra-sleek Snapchat-style Icons Only Mode (Pure icons, zero container shapes)
                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .widthIn(min = 44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = d.icon,
                                    contentDescription = d.name,
                                    tint = if (isSelected) activeColor else inactiveColor,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .scale(iconScale)
                                )
                            }
                        } else {
                            // Default Icons + Labels Mode (Snapchat-style alive spring icon + clear label)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(26.dp)
                                        .widthIn(min = 36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = d.icon,
                                        contentDescription = d.name,
                                        tint = if (isSelected) activeColor else inactiveColor,
                                        modifier = Modifier
                                            .size(21.dp)
                                            .scale(iconScale)
                                    )
                                }
                                Text(
                                    text = d.name,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) activeColor else inactiveColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SettingsCategory(val id: String, val label: String, val icon: ImageVector)
private val settingsCategories = listOf(
    SettingsCategory("Video", "Video", Icons.Rounded.PlayCircle),
    SettingsCategory("Audio", "Audio", Icons.AutoMirrored.Rounded.VolumeUp),
    SettingsCategory("Subtitles", "Subtitles", Icons.Rounded.Subtitles),
    SettingsCategory("Display", "Display", Icons.Rounded.Palette),
    SettingsCategory("Gestures", "Gestures", Icons.Rounded.TouchApp),
    SettingsCategory("Folders", "Storage & Folders", Icons.Rounded.Folder),
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
    livelyAnimations: Boolean = true,
    onLivelyAnimations: (Boolean) -> Unit = {},
    cornerStyle: String,
    onCornerStyle: (String) -> Unit,
    bottomBarStyle: String = "Icons and labels",
    onBottomBarStyle: (String) -> Unit = {},
    dockAutoMinimize: Boolean = true,
    onDockAutoMinimize: (Boolean) -> Unit = {},
    glassIntensity: Float = 0.75f,
    onGlassIntensity: (Float) -> Unit = {},
    onReplaySplash: () -> Unit,
    localState: LocalLibraryState,
    library: LocalLibrary,
    playback: PlaybackController,
    initialCategory: String = "Video"
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("playback", 0) }
    var activeCategory by rememberSaveable { mutableStateOf(initialCategory) }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    var folderToRemove by remember { mutableStateOf<LocalFolder?>(null) }
    val addFolder = rememberFolderPicker(library)

    folderToRemove?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToRemove = null },
            title = { Text("Remove ${folder.name}?") },
            text = { Text("Remove this folder from Jadzumi Player. The videos on your device storage will remain unchanged.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        library.remove(folder)
                        folderToRemove = null
                        statusNotice = "Removed folder: ${folder.name}"
                    },
                    enabled = !localState.busy
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToRemove = null }) {
                    Text("Keep folder")
                }
            }
        )
    }

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
                                "Folders" -> "Storage & Folders"
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
                                            Text(DeviceHardwareInspector.getDecoderDescription(context), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("Auto (Recommended) ›", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Hardware Acceleration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(DeviceHardwareInspector.getHardwareAccelerationDescription(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = true, onCheckedChange = {})
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Video Scaling Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(DeviceHardwareInspector.getDisplayScalingDescription(context), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    val (hdrTitle, hdrDesc) = remember { DeviceHardwareInspector.getHdrCapabilitiesInfo(context) }
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
                                                Text(hdrTitle, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                                Text(hdrDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    val (panelTitle, panelDesc) = remember { DeviceHardwareInspector.getDisplayPanelInfo(context) }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Rounded.Tv, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                                            Column {
                                                Text(panelTitle, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                Text(panelDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                            Text(DeviceHardwareInspector.getStereoDownmixDescription(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Night Mode & Dialogue Voice Boost", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Dynamic Range Compression & speech clarity boost for late-night watching", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = playback.nightModeVoiceBoost, onCheckedChange = { playback.updateNightMode(it) })
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(Modifier.weight(1f)) {
                                                Text("Bluetooth Audio Delay Compensation", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Calibrate audio lip-sync delay for Bluetooth earbuds & wireless speakers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text("${playback.audioDelayMs} ms", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedButton(onClick = { playback.updateAudioDelay(playback.audioDelayMs - 50) }) { Text("-50ms", fontSize = 12.sp) }
                                            OutlinedButton(onClick = { playback.updateAudioDelay(playback.audioDelayMs + 50) }) { Text("+50ms", fontSize = 12.sp) }
                                            if (playback.audioDelayMs != 0L) {
                                                TextButton(onClick = { playback.resetAudioDelay() }) { Text("Reset", fontSize = 12.sp) }
                                            }
                                        }
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

                                    Spacer(Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(Modifier.weight(1f)) {
                                                Text("Subtitle Sync Offset Calibration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Fine-tune subtitle timing synchronization for out-of-sync SRT/ASS tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text("${playback.subtitleDelayMs} ms", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedButton(onClick = { playback.updateSubtitleDelay(playback.subtitleDelayMs - 100) }) { Text("-100ms", fontSize = 12.sp) }
                                            OutlinedButton(onClick = { playback.updateSubtitleDelay(playback.subtitleDelayMs + 100) }) { Text("+100ms", fontSize = 12.sp) }
                                            if (playback.subtitleDelayMs != 0L) {
                                                TextButton(onClick = { playback.resetSubtitleDelay() }) { Text("Reset", fontSize = 12.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "Display" -> {
                            item {
                                Panel {
                                    Text("Appearance Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Select base lighting mode for ambient viewing.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

                                    val themeItems = listOf(
                                        Triple("Light", "Airy Pale-Blue", Icons.Rounded.LightMode),
                                        Triple("Dark", "Night Slate", Icons.Rounded.DarkMode),
                                        Triple("AMOLED", "Pure Black", Icons.Rounded.Brightness1)
                                    )
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

                                    Text("Bottom Navigation Dock Style", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Choose between classic icons with text labels or ultra-compact icons only", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(10.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        listOf(
                                            Triple("Icons and labels", "Default", Icons.Rounded.Splitscreen),
                                            Triple("Icons only", "Ultra-sleek", Icons.Rounded.ViewSidebar)
                                        ).forEach { (style, subtitle, icon) ->
                                            val isSel = bottomBarStyle == style
                                            Surface(
                                                modifier = Modifier.weight(1f).clickable { onBottomBarStyle(style) },
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(icon, null, tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(style, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                                    Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Auto-Minimize Dock on Scroll", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Smoothly collapse floating navigation into a bouncy mini-pill while scrolling movies or browsing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = dockAutoMinimize, onCheckedChange = onDockAutoMinimize)
                                    }

                                    HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                    Column(Modifier.fillMaxWidth()) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                                Text("Glassify UI Elements", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Adjust frosted glass translucency and depth across player pills, floating docks, and sheets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(999.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                            ) {
                                                Text(
                                                    "${(glassIntensity * 100).toInt()}%",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Slider(
                                            value = glassIntensity,
                                            onValueChange = onGlassIntensity,
                                            valueRange = 0.20f..1.0f,
                                            steps = 15,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Solid (20%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Balanced (65%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Deep Glass (100%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Lively UI Animations & Transitions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Fluid slide & spring transitions when navigating between folders, tabs, and details", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = livelyAnimations, onCheckedChange = onLivelyAnimations)
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

                        "Folders" -> {
                            item {
                                StorageSummaryCard(
                                    videoCount = localState.videos.size,
                                    totalIndexedBytes = localState.videos.sumOf { it.bytes.coerceAtLeast(0) },
                                    onAddFolder = addFolder
                                )
                            }
                            item {
                                Panel {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Indexed Folders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            Text("Manage media directories scanned for movies and TV shows.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        FilledTonalButton(
                                            onClick = addFolder,
                                            shape = RoundedCornerShape(14.dp),
                                            enabled = !localState.busy
                                        ) {
                                            Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Add Folder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Row(
                                        Modifier.fillMaxWidth().padding(top = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                library.rescan()
                                                statusNotice = "Rescanning media folders..."
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !localState.busy && localState.folders.isNotEmpty()
                                        ) {
                                            Icon(Icons.Rounded.Sync, null, Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Rescan Folders", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                clearThumbnailMemoryCache()
                                                statusNotice = "Thumbnail cache cleared successfully."
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Rounded.Cached, null, Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Clear Cache", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    if (localState.folders.isEmpty()) {
                                        Spacer(Modifier.height(16.dp))
                                        Surface(
                                            Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                "No media folders added yet. Tap Add Folder to grant access to your movie and video directories.",
                                                Modifier.padding(16.dp),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        Spacer(Modifier.height(14.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            localState.folders.forEach { folder ->
                                                FolderSummaryCard(
                                                    folder = folder,
                                                    state = localState,
                                                    isActive = false,
                                                    onSelect = { },
                                                    onRemove = { folderToRemove = folder }
                                                )
                                            }
                                        }
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
                                            "Jadzumi Player keeps your video library, playback history, and settings strictly on your device. No telemetry, no cloud sync, 100% offline-first.",
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
                                PrivacyLeftPane(localState, playback, addFolder, onManagePermissions = { activeCategory = "Folders" }) { statusNotice = it }
                            }
                            item {
                                var extractThumbnails by rememberSaveable { mutableStateOf<Boolean>(prefs.getBoolean("extract_thumbnails", true)) }
                                var scanSidecars by rememberSaveable { mutableStateOf<Boolean>(prefs.getBoolean("scan_sidecars", true)) }
                                var onlineMetadata by rememberSaveable { mutableStateOf<Boolean>(prefs.getBoolean("online_metadata", false)) }

                                Panel {
                                    Text("Artwork & Metadata", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Configure local media thumbnail generation, offline sidecar scraping (.nfo, poster.jpg, fanart.jpg) and optional online metadata.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    // 1. Hardware Video Thumbnail Extraction
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Local Thumbnail Extraction", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(
                                                "Hardware-accelerated on-device poster generation from local video keyframes when no sidecar image is present.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = extractThumbnails,
                                            onCheckedChange = {
                                                extractThumbnails = it
                                                prefs.edit { putBoolean("extract_thumbnails", it) }
                                            }
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    // 2. Scan Local Sidecars
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Scan Local Sidecar Files", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(
                                                "100% Offline — Automatically discovers poster.jpg, fanart.jpg, and Kodi/Jellyfin .nfo files in your media folders.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = scanSidecars,
                                            onCheckedChange = {
                                                scanSidecars = it
                                                prefs.edit { putBoolean("scan_sidecars", it) }
                                            }
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    // 3. Optional Online Metadata (Opt-In only, disabled by default)
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Fetch Online Metadata (Opt-In)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(
                                                "Disabled by default. When enabled, fetches missing high-resolution posters and plot summaries from Wikipedia & TVMaze, strictly cached locally.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = onlineMetadata,
                                            onCheckedChange = {
                                                onlineMetadata = it
                                                prefs.edit { putBoolean("online_metadata", it) }
                                            }
                                        )
                                    }
                                }
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
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("jadzumi_settings", Context.MODE_PRIVATE) }
    var localSandbox by rememberSaveable { mutableStateOf<Boolean>(prefs.getBoolean("local_sandbox", true)) }

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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Strict Local Sandbox", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "Enforces on-device isolated storage. Completely prohibits background telemetry and unrequested cloud sync.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = localSandbox,
                    onCheckedChange = {
                        localSandbox = it
                        prefs.edit { putBoolean("local_sandbox", it) }
                    }
                )
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
    val glassIntensity = LocalGlassIntensity.current
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = (0.98f - 0.45f * glassIntensity).coerceIn(0.50f, 1.0f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = (0.35f + 0.50f * glassIntensity).coerceIn(0.30f, 0.90f)))
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
