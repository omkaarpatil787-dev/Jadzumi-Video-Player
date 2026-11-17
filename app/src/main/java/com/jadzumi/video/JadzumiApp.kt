package com.jadzumi.video

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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

// Moonlit Lavender Palette (Jadzumi Pure • Cinematic Hybrid)
val Obsidian = Color(0xFF101016)
val Charcoal = Color(0xFF1C1A25)
val MoonlitSurface = Color(0xFF292535)
val LavenderAccent = Color(0xFFB79AF7)
val MoonlitHighlight = Color(0xFFE8DDFB)
val MoonlitPrimaryText = Color(0xFFF4F1FA)
val MoonlitSecondaryText = Color(0xFFA7A3B4)
val MoonlitSuccess = Color(0xFF80CDB6)
val MoonlitWarning = Color(0xFFF0B67A)

private val Light = lightColorScheme(
    primary = Color(0xFF704DB8),
    primaryContainer = Color(0xFFE8DDFB),
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF25104B),
    background = Color(0xFFF7F5FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFEDE8F7),
    onSurface = Color(0xFF1C1A25),
    onSurfaceVariant = Color(0xFF676175),
    outlineVariant = Color(0xFFE2DCED),
    secondary = MoonlitWarning,
    tertiary = MoonlitSuccess
)
private val Dark = darkColorScheme(
    primary = LavenderAccent,
    primaryContainer = MoonlitSurface,
    onPrimary = Obsidian,
    onPrimaryContainer = MoonlitHighlight,
    background = Obsidian,
    surface = Charcoal,
    surfaceVariant = MoonlitSurface,
    onSurface = MoonlitPrimaryText,
    onSurfaceVariant = MoonlitSecondaryText,
    outlineVariant = Color(0xFF383345),
    secondary = MoonlitWarning,
    tertiary = MoonlitSuccess
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable fun JadzumiApp(playback: PlaybackController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("appearance", 0) }
    var theme by rememberSaveable { mutableStateOf(prefs.getString("theme", "Dark") ?: "Dark") }
    var reducedMotion by rememberSaveable { mutableStateOf(prefs.getBoolean("reducedMotion", false)) }
    var livelyAnimations by rememberSaveable { mutableStateOf(prefs.getBoolean("livelyAnimations", true)) }
    var themeBloomEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("themeBloom", true)) }
    var firstLaunchCardShown by rememberSaveable { mutableStateOf(prefs.getBoolean("pad5_welcome_done", false)) }

    var showStorageSheet by remember { mutableStateOf(false) }

    var route by rememberSaveable { mutableStateOf("Home") }
    var activeLibraryCategory by rememberSaveable { mutableStateOf("Movies") }
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
    var haloEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("halo_enabled", true)) }
    var haloPaletteName by rememberSaveable { mutableStateOf(prefs.getString("halo_palette", "Glacier Blue") ?: "Glacier Blue") }
    var haloIntensity by rememberSaveable { mutableFloatStateOf(prefs.getFloat("halo_intensity", 0.30f)) }
    var haloSpread by rememberSaveable { mutableFloatStateOf(prefs.getFloat("halo_spread", 0.70f)) }
    val cornerRadiusDp = when (cornerStyle) {
        "Compact" -> 16.dp
        else -> 20.dp
    }
    val motionStyle = rememberMotionStyle(reducedMotion, livelyAnimations)
    val keyboardController = LocalSoftwareKeyboardController.current
    var showSplash by rememberSaveable { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val bloomProgress = remember { Animatable(1f) }
    var bloomOriginColor by remember { mutableStateOf<Color?>(null) }
    var bloomJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val sysDark = isSystemInDarkTheme()
    val effectiveTheme = when (theme) {
        "System default", "System" -> if (sysDark) "Dark" else "Light"
        else -> theme
    }
    val colors = when (effectiveTheme) {
        "Light" -> Light
        "Dark" -> Dark
        "AMOLED" -> Dark.copy(background = Color.Black, surface = Obsidian, surfaceVariant = Charcoal)
        else -> if (sysDark) Dark else Light
    }

    fun switchThemeWithBloom(newTheme: String) {
        if (newTheme == theme) return
        val oldBg = colors.background
        theme = newTheme
        prefs.edit { putString("theme", newTheme) }
        if (themeBloomEnabled && motionStyle.enabled) {
            bloomJob?.cancel()
            bloomOriginColor = oldBg
            bloomJob = scope.launch {
                bloomProgress.snapTo(0f)
                bloomProgress.animateTo(1f, tween(420, easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)))
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
        CompositionLocalProvider(
            LocalMotionStyle provides motionStyle,
            LocalGlassIntensity provides glassIntensity,
            LocalThemeName provides effectiveTheme
        ) {
            Box(Modifier.fillMaxSize()) {
            Surface(Modifier.fillMaxSize(), color = colors.background) {
                Box(Modifier.fillMaxSize()) {
                    PortalHaloBackdrop(
                        theme = effectiveTheme,
                        palette = HaloPalette.fromName(haloPaletteName),
                        enabled = haloEnabled && (playback.video == null || playback.isDocked || playback.inPip),
                        intensity = haloIntensity,
                        spread = haloSpread,
                        reducedMotion = reducedMotion || !motionStyle.enabled
                    )
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
                                    if (isPhone) {
                                        AnimatedVisibility(
                                            visible = selectedId == null,
                                            enter = fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) + expandVertically(animationSpec = tween(220, easing = LinearOutSlowInEasing)),
                                            exit = fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) + shrinkVertically(animationSpec = tween(180, easing = FastOutLinearInEasing))
                                        ) {
                                            // Adaptive Phone Top Bar (Greeting + Actions, Full-width frosted search capsule)
                                            Column(Modifier.fillMaxWidth().padding(horizontal = topPad, vertical = 6.dp)) {
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    JadzumiCategoryPillsRow(
                                                        selectedCategory = when (activeLibraryCategory) {
                                                            "Movies" -> "Movies"
                                                            "TV Shows" -> "Series"
                                                            "Anime" -> "Anime"
                                                            else -> "Movies"
                                                        },
                                                        onCategorySelected = { cat ->
                                                            val newCat = when (cat) {
                                                                "Movies" -> "Movies"
                                                                "Series" -> "TV Shows"
                                                                "Anime" -> "Anime"
                                                                else -> "Movies"
                                                            }
                                                            activeLibraryCategory = newCat
                                                        }
                                                    )

                                                    JadzumiSegmentedNavCapsule(
                                                        currentRoute = if (route == "Settings") "Settings" else "Home",
                                                        onNavigate = { newRoute ->
                                                            route = newRoute
                                                            selectedId = null
                                                        }
                                                    )

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        IconButton(
                                                            onClick = { showStorageSheet = true },
                                                            modifier = Modifier.size(34.dp)
                                                        ) {
                                                            Icon(Icons.Rounded.PieChart, "Storage Cleaner", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                                                        }
                                                        IconButton(
                                                            onClick = { library.rescan() },
                                                            enabled = !localState.busy && localState.folders.isNotEmpty(),
                                                            modifier = Modifier.size(34.dp)
                                                        ) {
                                                            if (localState.busy) {
                                                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                                            } else {
                                                                Icon(Icons.Rounded.Sync, "Sync library now", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(Modifier.height(6.dp))

                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(42.dp),
                                                    shape = RoundedCornerShape(21.dp),
                                                    color = GlassTheme.surfaceColor(effectiveTheme, glassIntensity),
                                                    border = BorderStroke(1.dp, GlassTheme.borderColor(effectiveTheme, glassIntensity))
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
                                                        BasicTextField(
                                                            value = query,
                                                            onValueChange = { query = it },
                                                            modifier = Modifier.weight(1f),
                                                            singleLine = true,
                                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            ),
                                                            decorationBox = { innerTextField ->
                                                                if (query.isEmpty()) {
                                                                    Text(
                                                                        "Search titles, directors, actors...",
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                                    )
                                                                }
                                                                innerTextField()
                                                            }
                                                        )
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
                                            } else {
                                                JadzumiCategoryPillsRow(
                                                    selectedCategory = when (activeLibraryCategory) {
                                                        "Movies" -> "Movies"
                                                        "TV Shows" -> "Series"
                                                        "Anime" -> "Anime"
                                                        else -> "Movies"
                                                    },
                                                    onCategorySelected = { cat ->
                                                        val newCat = when (cat) {
                                                            "Movies" -> "Movies"
                                                            "Series" -> "TV Shows"
                                                            "Anime" -> "Anime"
                                                            else -> "Movies"
                                                        }
                                                        activeLibraryCategory = newCat
                                                    }
                                                )

                                                Spacer(Modifier.weight(1f))

                                                JadzumiSegmentedNavCapsule(
                                                    currentRoute = if (route == "Settings") "Settings" else "Home",
                                                    onNavigate = { newRoute ->
                                                        route = newRoute
                                                        selectedId = null
                                                    }
                                                )

                                                Spacer(Modifier.weight(1f))
                                            }

                                            Surface(
                                                modifier = Modifier
                                                    .widthIn(min = 220.dp, max = 340.dp)
                                                    .height(42.dp),
                                                shape = RoundedCornerShape(999.dp),
                                                color = MaterialExpressive.subCardBg(effectiveTheme),
                                                border = MaterialExpressive.cardBorder(effectiveTheme)
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

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    onClick = { showStorageSheet = true },
                                                    shape = CircleShape,
                                                    color = MaterialExpressive.subCardBg(effectiveTheme),
                                                    border = MaterialExpressive.cardBorder(effectiveTheme)
                                                ) {
                                                    Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Rounded.PieChart, "Storage Cleaner", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                                Surface(
                                                    onClick = { library.rescan() },
                                                    enabled = !localState.busy && localState.folders.isNotEmpty(),
                                                    shape = CircleShape,
                                                    color = MaterialExpressive.subCardBg(effectiveTheme),
                                                    border = MaterialExpressive.cardBorder(effectiveTheme)
                                                ) {
                                                    Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                                                        if (localState.busy) {
                                                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                                        } else {
                                                            Icon(Icons.Rounded.Sync, "Sync library now", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    AnimatedContent(
                                        targetState = selectedId ?: route,
                                        transitionSpec = {
                                            if (!motionStyle.enabled) {
                                                fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                                            } else {
                                                val mainTabs = setOf("Home", "Library", "Movies", "Videos", "TV Shows", "Folders", "Settings", "Playlists")
                                                val isTargetDetails = targetState !in mainTabs
                                                val isInitialDetails = initialState !in mainTabs

                                                val morphCurve = CubicBezierEasing(0.19f, 1.0f, 0.22f, 1.0f)
                                                if (!isInitialDetails && isTargetDetails) {
                                                    // Deliberate 500ms shared-element spring entrance into Details
                                                    (fadeIn(animationSpec = tween(500, easing = morphCurve)) +
                                                            scaleIn(initialScale = 0.92f, animationSpec = tween(500, easing = morphCurve)))
                                                        .togetherWith(
                                                            fadeOut(animationSpec = tween(260, easing = FastOutLinearInEasing)) +
                                                                    scaleOut(targetScale = 0.98f, animationSpec = tween(260, easing = FastOutLinearInEasing))
                                                        )
                                                } else if (isInitialDetails && !isTargetDetails) {
                                                    // Deliberate 500ms shared-element collapse back to Library
                                                    (fadeIn(animationSpec = tween(500, easing = morphCurve)) +
                                                            scaleIn(initialScale = 0.98f, animationSpec = tween(500, easing = morphCurve)))
                                                        .togetherWith(
                                                            fadeOut(animationSpec = tween(500, easing = morphCurve)) +
                                                                    scaleOut(targetScale = 0.92f, animationSpec = tween(500, easing = morphCurve))
                                                        )
                                                } else {
                                                    // Navigating between tabs: silky smooth shared-axis animated crossfade (like iOS / ColorOS)
                                                    val tabOrder = listOf("Home", "Library", "Movies", "TV Shows", "Videos", "Folders", "Settings", "Playlists")
                                                    val initialIndex = tabOrder.indexOf(initialState)
                                                    val targetIndex = tabOrder.indexOf(targetState)
                                                    val isForward = if (initialIndex != -1 && targetIndex != -1) targetIndex > initialIndex else true

                                                    (slideInHorizontally(animationSpec = spring(dampingRatio = motionStyle.damping, stiffness = Spring.StiffnessLow)) { if (isForward) (it * 0.06f).toInt() else (-it * 0.06f).toInt() } +
                                                            fadeIn(animationSpec = tween(360, easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f))))
                                                        .togetherWith(
                                                            slideOutHorizontally(animationSpec = spring(dampingRatio = motionStyle.damping, stiffness = Spring.StiffnessLow)) { if (isForward) (-it * 0.06f).toInt() else (it * 0.06f).toInt() } +
                                                                    fadeOut(animationSpec = tween(240, easing = FastOutLinearInEasing))
                                                        )
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f).elasticEdge(),
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
                                                LocalLibraryPage(localState, library, playback, query, { selectedId = it }, defaultCategory = activeLibraryCategory, onCategoryChanged = { activeLibraryCategory = it })
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
                                        } else when (current) {
                                            "Home", "Library" -> LocalLibraryPage(localState, library, playback, query, { selectedId = it }, defaultCategory = activeLibraryCategory, onCategoryChanged = { activeLibraryCategory = it }, onScrollActive = { shouldMin -> if (dockAutoMinimize) isDockMinimized = shouldMin })
                                            "Movies", "Videos" -> LocalLibraryPage(localState, library, playback, query, { selectedId = it }, defaultCategory = activeLibraryCategory, onCategoryChanged = { activeLibraryCategory = it }, onScrollActive = { shouldMin -> if (dockAutoMinimize) isDockMinimized = shouldMin })
                                            "TV Shows" -> LocalLibraryPage(localState, library, playback, query, { selectedId = it }, defaultCategory = activeLibraryCategory, onCategoryChanged = { activeLibraryCategory = it }, onScrollActive = { shouldMin -> if (dockAutoMinimize) isDockMinimized = shouldMin })
                                            "Folders" -> UnifiedSettingsAndPrivacyScreen(
                                                theme = theme,
                                                effectiveTheme = effectiveTheme,
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
                                                haloEnabled = haloEnabled,
                                                onHaloEnabled = {
                                                    haloEnabled = it
                                                    prefs.edit { putBoolean("halo_enabled", it) }
                                                },
                                                haloPalette = HaloPalette.fromName(haloPaletteName),
                                                onHaloPalette = {
                                                    haloPaletteName = it.title
                                                    prefs.edit { putString("halo_palette", it.title) }
                                                },
                                                haloIntensity = haloIntensity,
                                                onHaloIntensity = {
                                                    haloIntensity = it
                                                    prefs.edit { putFloat("halo_intensity", it) }
                                                },
                                                haloSpread = haloSpread,
                                                onHaloSpread = {
                                                    haloSpread = it
                                                    prefs.edit { putFloat("halo_spread", it) }
                                                },
                                                onReplaySplash = { showSplash = true },
                                                localState = localState,
                                                library = library,
                                                playback = playback,
                                                initialCategory = "Folders"
                                            )
                                            "Settings" -> UnifiedSettingsAndPrivacyScreen(
                                                theme = theme,
                                                effectiveTheme = effectiveTheme,
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
                                                haloEnabled = haloEnabled,
                                                onHaloEnabled = {
                                                    haloEnabled = it
                                                    prefs.edit { putBoolean("halo_enabled", it) }
                                                },
                                                haloPalette = HaloPalette.fromName(haloPaletteName),
                                                onHaloPalette = {
                                                    haloPaletteName = it.title
                                                    prefs.edit { putString("halo_palette", it.title) }
                                                },
                                                haloIntensity = haloIntensity,
                                                onHaloIntensity = {
                                                    haloIntensity = it
                                                    prefs.edit { putFloat("halo_intensity", it) }
                                                },
                                                haloSpread = haloSpread,
                                                onHaloSpread = {
                                                    haloSpread = it
                                                    prefs.edit { putFloat("halo_spread", it) }
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

                                // In-App Docked Mini Player (YouTube / Spotify Style)
                                if (playback.video != null && playback.isDocked) {
                                    val bottomPad = if (isTablet) 16.dp else 12.dp
                                    DockedMiniPlayer(
                                        playback = playback,
                                        theme = effectiveTheme,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = bottomPad)
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

            UpdateHost()

            if (showStorageSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showStorageSheet = false },
                    containerColor = Color.Transparent
                ) {
                    StorageAnalyzerSheet(
                        videos = localState.videos,
                        playback = playback,
                        onVideoDeleted = { library.rescan() },
                        onMessage = { /* toast */ },
                        onDismiss = { showStorageSheet = false }
                    )
                }
            }

            CinemaPlayer(playback, reducedMotion, glassIntensity, effectiveTheme)

            if (showSplash) {
                JadzumiSplashScreen(
                    theme = effectiveTheme,
                    reducedMotion = reducedMotion,
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
            "Fun fact: Akira Kurosawa dyed rain with ink to film epic downpours.",
            "Cinema trivia: The 24fps motion picture standard was born in 1927.",
            "Pro tip: MediaCodec 10-bit HEVC decoding saves 60% battery life.",
            "Fun fact: 2001: A Space Odyssey used 0 CGI — 100% physical models.",
            "Studio audio: NextLib FFmpeg powers true 5.1 & 7.1 discrete surround sound.",
            "Trivia: The iconic Jurassic Park T-Rex roar was mixed from a baby elephant & alligator.",
            "Fun fact: The iconic green falling code in The Matrix is actually sushi recipes.",
            "Pure privacy: 100% offline cinema vault, zero telemetry."
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
                series != null && epInfo != null -> "Watching S%02dE%02d · Tap to cycle trivia".format(epInfo.season, epInfo.episode)
                inProgressMovie != null -> "Resume from ${timecode(playback.savedPosition(inProgressMovie) / 1000)} · Tap for cinema trivia"
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
    val theme = LocalThemeName.current
    val isAmoled = theme == "AMOLED"
    val infiniteTransition = rememberInfiniteTransition(label = "livingEmblemGlow")

    // Approved Living Emblem 5-second gentle idle breathing animation (0.42f to 0.73f)
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.73f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emblemBreathe"
    )

    // Specular light sweep offset (0f to 1f)
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "emblemSweep"
    )

    val cornerRadius = size * 0.28f

    // Non-clickable container — three distinct visual layers preserving original asset
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1 (behind): Living Emblem Halo (soft lavender radial glow)
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(1.35f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        0.0f to Color(0xFFB78BF9).copy(alpha = glowPulse * 0.55f),
                        0.38f to Color(0xFF8E5FCA).copy(alpha = glowPulse * 0.20f),
                        0.72f to Color.Transparent
                    )
                )
        )

        // Layer 2 (middle): Clean surface tile with untouched original Jadzumi symbol
        Surface(
            modifier = Modifier.size(size),
            shape = RoundedCornerShape(cornerRadius),
            color = when {
                isAmoled -> Color.Black
                darkSurface -> Color(0xFF191A22)
                else -> Color(0xFFF6F1FA)
            },
            border = BorderStroke(
                1.dp,
                when {
                    isAmoled -> Color(0xFF39323D)
                    darkSurface -> Color(0xFF4D435A).copy(alpha = 0.6f + 0.3f * glowPulse)
                    else -> Color(0xFFDCC8FF)
                }
            ),
            shadowElevation = if (isAmoled) 0.dp else 4.dp
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // Untouched original vector symbol — NO resizing distortion or whole-logo scaling
                JadzumiPlaySymbol(
                    Modifier.size(size * 0.66f)
                )

                // Layer 3 (above): Living Emblem Specular Light Sweep overlay
                val sweepOffset = size.value * sweepProgress
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(
                            Brush.linearGradient(
                                0.0f to Color.Transparent,
                                0.5f to Color(0x33F5E4FF),
                                1.0f to Color.Transparent,
                                start = Offset(sweepOffset - 40f, 0f),
                                end = Offset(sweepOffset + 40f, size.value)
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun JadzumiSegmentedNavCapsule(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalThemeName.current
    val isLight = theme == "Light"
    val isSettings = currentRoute == "Settings"
    val sliderProgress by animateFloatAsState(
        targetValue = if (isSettings) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f),
        label = "capsuleSliderProgress"
    )

    val capsuleBg = if (isLight) Color(0xFFECE6F4) else Color(0xFF1E1A29).copy(alpha = 0.75f)
    val capsuleBorder = if (isLight) Color(0xFFD4CBE5) else Color.White.copy(alpha = 0.08f)
    val unselectedText = if (isLight) Color(0xFF5A526B) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .height(38.dp)
            .width(190.dp),
        shape = RoundedCornerShape(19.dp),
        color = capsuleBg,
        border = BorderStroke(1.dp, capsuleBorder),
        shadowElevation = if (isLight) 1.dp else 2.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        ) {
            val segmentWidth = maxWidth / 2

            // Animated sliding selection capsule in Hybrid Lavender Gradient
            Box(
                modifier = Modifier
                    .offset(x = segmentWidth * sliderProgress)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), spotColor = LavenderAccent.copy(alpha = 0.45f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFC0A2FB), Color(0xFF986FF5))
                        )
                    )
            )

            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigate("Home") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Home",
                        fontSize = 13.sp,
                        fontWeight = if (!isSettings) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isSettings) Obsidian else unselectedText
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigate("Settings") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Settings",
                        fontSize = 13.sp,
                        fontWeight = if (isSettings) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSettings) Obsidian else unselectedText
                    )
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
    effectiveTheme: String = theme,
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
    haloEnabled: Boolean = true,
    onHaloEnabled: (Boolean) -> Unit = {},
    haloPalette: HaloPalette = HaloPalette.GlacierBlue,
    onHaloPalette: (HaloPalette) -> Unit = {},
    haloIntensity: Float = 0.30f,
    onHaloIntensity: (Float) -> Unit = {},
    haloSpread: Float = 0.70f,
    onHaloSpread: (Float) -> Unit = {},
    onReplaySplash: () -> Unit,
    localState: LocalLibraryState,
    library: LocalLibrary,
    playback: PlaybackController,
    initialCategory: String = "Video"
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("playback", 0) }
    val settingsListState = rememberLazyListState()
    var activeCategory by rememberSaveable { mutableStateOf(initialCategory) }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    var folderToRemove by remember { mutableStateOf<LocalFolder?>(null) }
    var showDisplayDuringPlaybackDialog by remember { mutableStateOf(false) }
    var showFolderBlacklistDialog by remember { mutableStateOf(false) }
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

                Column(
                    Modifier.weight(1f).fillMaxHeight()
                ) {
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
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp, top = 2.dp)
                    )

                    LazyColumn(
                        state = settingsListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .animatedScrollBlendFade(settingsListState),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                    when (activeCategory) {
                        "Video" -> {
                            item {
                                Panel {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Hardware Acceleration Engine", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E8E3E).copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, Color(0xFF1E8E3E).copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                "● GPU Active (60/120 FPS)",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF34D399)
                                            )
                                        }
                                    }
                                    Text(
                                        "Uses dedicated GPU MediaCodec decoders for fluid 4K/1080p playback with zero battery drain.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                    )

                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Decoder Priority", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                            Text("Automatic fallback to software decoding when needed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                        ) {
                                            Text("Auto (Recommended)", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Match Display Refresh Rate", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                            Text("Synchronize display Hz (24Hz / 60Hz / 120Hz) to video frame rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(playback.matchRefreshRate, playback::updateRefreshMatching)
                                    }
                                }
                            }

                            // BENTO CARD 2: DEFAULT VIDEO SCALING MODE (In-Card Tactile Chips)
                            item {
                                Panel {
                                    Text("Default Video Scaling Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("How videos fit your display upon playback start. Double-tap the center of the video during playback to toggle instantly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            AspectRatioMode.FIT to "Fit (Original)",
                                            AspectRatioMode.ZOOM to "Crop / Fill",
                                            AspectRatioMode.FILL to "Stretch"
                                        ).forEach { (mode, label) ->
                                            val isSelected = playback.aspectMode == mode
                                            Surface(
                                                modifier = Modifier.weight(1f).clickable { playback.setAspectRatioMode(mode) },
                                                shape = RoundedCornerShape(999.dp),
                                                color = if (isSelected) LavenderAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                border = BorderStroke(1.dp, if (isSelected) LavenderAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            ) {
                                                Box(Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                                    Text(
                                                        label,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) Obsidian else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // BENTO CARD 3: LUMINANCE & DISPLAY OVERLAYS
                            item {
                                Panel {
                                    val appearancePrefs = remember(context) { context.getSharedPreferences("appearance", Context.MODE_PRIVATE) }
                                    var heroBannerEnabled by remember { mutableStateOf(appearancePrefs.getBoolean("show_hero_banner", false)) }

                                    Text("Luminance & Display Overlays", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Manage HDR luminance boost, persistent telemetry, and cinema player overlays.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

                                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Brightness Boost & HDR", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                            Text("Force maximum panel luminance for HDR10 and Dolby Vision content", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = playback.brightnessBoost, onCheckedChange = { playback.updateBrightnessBoost(it) })
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Show Featured Hero Banner", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                            Text("Display large recommendation banner at top of library (Default: Off)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(
                                            checked = heroBannerEnabled,
                                            onCheckedChange = {
                                                heroBannerEnabled = it
                                                appearancePrefs.edit().putBoolean("show_hero_banner", it).apply()
                                            }
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    Row(
                                        Modifier.fillMaxWidth().clickable { showDisplayDuringPlaybackDialog = true }.padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Display during playback", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                            Text(playback.displayDuringPlaybackSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("Configure ›", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            item {
                                Panel {
                                    Text("Device Hardware Capabilities", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Detected display panel capabilities and HDR standard support.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

                                    val (hdrTitle, hdrDesc) = remember { DeviceHardwareInspector.getHdrCapabilitiesInfo(context) }
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
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
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Rounded.Tv, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                                            Column {
                                                Text(panelTitle, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                Text(panelDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "Audio" -> {
                            // BENTO CARD 1: AUDIO OUTPUT & SPATIAL 3D
                            item {
                                Panel {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Audio Output & Spatial 3D", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E8E3E).copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, Color(0xFF1E8E3E).copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                "● NextLib FFmpeg Active",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF34D399)
                                            )
                                        }
                                    }
                                    Text("Bundled FFmpeg: EAC-3, AC-3, TrueHD, DTS-HD, FLAC, and AAC with zero stutter.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

                                    // Spatial Cinema 3D
                                    Text("Spatial Cinema 3D Virtualizer", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Spatial3DMode.entries.forEach { mode ->
                                            val isSelected = playback.spatial3DMode == mode
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { playback.setSpatial3D(mode) },
                                                label = { Text(mode.displayName.substringBefore(" (")) }
                                            )
                                        }
                                    }

                                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Downmix to Stereo", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                            Text(DeviceHardwareInspector.getStereoDownmixDescription(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = true, onCheckedChange = {})
                                    }
                                }
                            }

                            // BENTO CARD 2: PLAYBACK AUDIO FEATURES
                            item {
                                Panel {
                                    Text("Playback Audio Enhancements", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Smart dialogue tuning and background audio for uninterrupted listening.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

                                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Night Mode & Dialogue Voice Boost", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                            Text("Dynamic Range Compression & speech clarity boost for late-night watching", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = playback.nightModeVoiceBoost, onCheckedChange = { playback.updateNightMode(it) })
                                    }

                                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Screen-Off Background Audio", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                            Text("Continue playback when turning off screen or locking device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = playback.backgroundAudioEnabled, onCheckedChange = { playback.setBackgroundAudio(it) })
                                    }
                                }
                            }

                            // BENTO CARD 3: WIRELESS AUDIO LIP-SYNC CALIBRATION
                            item {
                                Panel {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Wireless Lip-Sync Calibration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                "${playback.audioDelayMs} ms",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Text("Calibrate audio delay for Bluetooth earbuds, headphones, and wireless soundbars.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(
                                            onClick = { playback.updateAudioDelay(playback.audioDelayMs - 50) },
                                            shape = RoundedCornerShape(12.dp)
                                        ) { Text("-50 ms", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                        OutlinedButton(
                                            onClick = { playback.updateAudioDelay(playback.audioDelayMs + 50) },
                                            shape = RoundedCornerShape(12.dp)
                                        ) { Text("+50 ms", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                        if (playback.audioDelayMs != 0L) {
                                            TextButton(onClick = { playback.resetAudioDelay() }) {
                                                Text("Reset (0 ms)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "Subtitles" -> {
                            item {
                                val context = LocalContext.current
                                var subFont by remember { mutableStateOf(SubtitlePreferences.getFont(context)) }
                                var subFontStyle by remember { mutableStateOf(SubtitlePreferences.getFontStyle(context)) }
                                var subSize by remember { mutableFloatStateOf(SubtitlePreferences.getSize(context)) }
                                var subColor by remember { mutableStateOf(SubtitlePreferences.getColor(context)) }
                                var subBg by remember { mutableStateOf(SubtitlePreferences.getBackground(context)) }
                                var subEdge by remember { mutableStateOf(SubtitlePreferences.getEdge(context)) }
                                var subMargin by remember { mutableFloatStateOf(SubtitlePreferences.getBottomMargin(context)) }

                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    // BENTO CARD 1: LIVE PREVIEW & TYPOGRAPHY
                                    Panel {
                                        Text("Subtitle Live Studio & Typography", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("Typography, font weight, and real-time rendering for SRT, VTT, and embedded SSA/ASS subtitles.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

                                        // Live Subtitle Preview Card
                                        Card(
                                            modifier = Modifier.fillMaxWidth().height(145.dp),
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
                                                    "Cinema Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                                                    "Heavy Sans" -> androidx.compose.ui.text.font.FontFamily.SansSerif
                                                    "Roboto Condensed" -> androidx.compose.ui.text.font.FontFamily.SansSerif
                                                    "Roboto Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
                                                    "System Default" -> androidx.compose.ui.text.font.FontFamily.Default
                                                    else -> androidx.compose.ui.text.font.FontFamily.SansSerif
                                                }
                                                val previewFontWeight = when {
                                                    subFont == "Heavy Sans" || subFontStyle.contains("Bold") -> FontWeight.ExtraBold
                                                    else -> FontWeight.SemiBold
                                                }
                                                val previewFontStyle = if (subFontStyle.contains("Italic")) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                                                val previewFontSize = (15.sp * subSize)

                                                Surface(
                                                    modifier = Modifier.padding(bottom = (12.dp + (subMargin * 50).dp), start = 16.dp, end = 16.dp),
                                                    color = previewBgColor,
                                                    shape = RoundedCornerShape(if (subBg == "Rounded Pill Box") 8.dp else 4.dp),
                                                    border = if (subEdge == "Soft Glow") BorderStroke(1.dp, Color(0xFF8B9DFF).copy(alpha = 0.5f)) else null
                                                ) {
                                                    Text(
                                                        "We must reach beyond our own horizon.",
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                        color = previewTextColor,
                                                        fontSize = previewFontSize,
                                                        fontFamily = previewFontFamily,
                                                        fontWeight = previewFontWeight,
                                                        fontStyle = previewFontStyle,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(14.dp))
                                        Text("Font Family", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, modifier = Modifier.padding(bottom = 6.dp))
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

                                        Spacer(Modifier.height(10.dp))
                                        Text("Font Style & Weight", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, modifier = Modifier.padding(bottom = 6.dp))
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            SubtitlePreferences.fontStyleOptions.forEach { style ->
                                                FilterChip(
                                                    selected = subFontStyle == style,
                                                    onClick = {
                                                        subFontStyle = style
                                                        SubtitlePreferences.setFontStyle(context, style)
                                                    },
                                                    label = { Text(style, maxLines = 1, softWrap = false) }
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(10.dp))
                                        Text("Text Size", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, modifier = Modifier.padding(bottom = 6.dp))
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
                                    }

                                    // BENTO CARD 2: COLORS & 3D EDGE EFFECTS
                                    Panel {
                                        Text("Colors, Shading & Background Style", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("Customize text contrast, background pill geometry, and glowing drop shadows.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

                                        Text("Text Color", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, modifier = Modifier.padding(bottom = 6.dp))
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

                                        Spacer(Modifier.height(10.dp))
                                        Text("Edge, 3D & Glow Effect", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, modifier = Modifier.padding(bottom = 6.dp))
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

                                        Spacer(Modifier.height(10.dp))
                                        Text("Background Box Style", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, modifier = Modifier.padding(bottom = 6.dp))
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

                                        Spacer(Modifier.height(10.dp))
                                        Text("Vertical Margin & Position", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, modifier = Modifier.padding(bottom = 6.dp))
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            SubtitlePreferences.bottomMarginOptions.forEach { (margin, label) ->
                                                FilterChip(
                                                    selected = (subMargin - margin) in -0.01f..0.01f,
                                                    onClick = {
                                                        subMargin = margin
                                                        SubtitlePreferences.setBottomMargin(context, margin)
                                                    },
                                                    label = { Text(label, maxLines = 1, softWrap = false) }
                                                )
                                            }
                                        }
                                    }

                                    // BENTO CARD 3: SUBTITLE TIMING & SYNC
                                    Panel {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("⏱ Subtitle Sync Offset Calibration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                            ) {
                                                Text(
                                                    "${playback.subtitleDelayMs} ms",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Text("Fine-tune subtitle timing synchronization for out-of-sync SRT/ASS tracks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedButton(
                                                onClick = { playback.updateSubtitleDelay(playback.subtitleDelayMs - 100) },
                                                shape = RoundedCornerShape(12.dp)
                                            ) { Text("-100 ms", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                            OutlinedButton(
                                                onClick = { playback.updateSubtitleDelay(playback.subtitleDelayMs + 100) },
                                                shape = RoundedCornerShape(12.dp)
                                            ) { Text("+100 ms", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                            if (playback.subtitleDelayMs != 0L) {
                                                TextButton(onClick = { playback.resetSubtitleDelay() }) {
                                                    Text("Reset (0 ms)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "Display" -> {
                            // 1. Base Appearance Mode & Transitions
                            item {
                                Panel {
                                    Text("Appearance Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Select base lighting mode for ambient viewing.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

                                    val themeItems = listOf(
                                        Triple("System default", "Follow System", Icons.Rounded.BrightnessAuto),
                                        Triple("Light", "Airy Pale-Blue", Icons.Rounded.LightMode),
                                        Triple("Dark", "Night Slate", Icons.Rounded.DarkMode),
                                        Triple("AMOLED", "Pure Black", Icons.Rounded.Brightness1)
                                    )
                                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            ThemeOptionCard(themeItems[0].first, themeItems[0].second, themeItems[0].third, theme == themeItems[0].first, { onTheme(themeItems[0].first) }, Modifier.weight(1f))
                                            ThemeOptionCard(themeItems[1].first, themeItems[1].second, themeItems[1].third, theme == themeItems[1].first, { onTheme(themeItems[1].first) }, Modifier.weight(1f))
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            ThemeOptionCard(themeItems[2].first, themeItems[2].second, themeItems[2].third, theme == themeItems[2].first, { onTheme(themeItems[2].first) }, Modifier.weight(1f))
                                            ThemeOptionCard(themeItems[3].first, themeItems[3].second, themeItems[3].third, theme == themeItems[3].first, { onTheme(themeItems[3].first) }, Modifier.weight(1f))
                                        }
                                    }

                                    HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Theme Transition Blend (Fade)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Smooth 580ms circular blend reveal when switching themes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = themeBloom, onCheckedChange = onThemeBloom)
                                    }
                                }
                            }

                            // 2. Portal Halo Backdrop System
                            item {
                                HaloSettingsCard(
                                    theme = effectiveTheme,
                                    enabled = haloEnabled,
                                    onEnabledChange = onHaloEnabled,
                                    palette = haloPalette,
                                    onPaletteChange = onHaloPalette,
                                    intensity = haloIntensity,
                                    onIntensityChange = onHaloIntensity,
                                    spread = haloSpread,
                                    onSpreadChange = onHaloSpread
                                )
                            }

                            // 3. Motion Physics & Touch Dynamics
                            item {
                                Panel {
                                    Text("Motion Physics & Transitions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Tune organic spring stiffness, navigation slide curves, and tactile squish feedback.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

                                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Lively UI Animations & Transitions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Fluid slide & spring transitions when navigating between folders, tabs, and details", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = livelyAnimations, onCheckedChange = onLivelyAnimations)
                                    }

                                    HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Reduced Motion", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Disable animations for instant screen transitions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = reduced, onCheckedChange = onReduced)
                                    }

                                    HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                    MotionCustomization()
                                }
                            }

                            // 6. App Icon & Brand Identity
                            item {
                                Panel {
                                    Text("App Launcher Icon & Identity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Choose from 10 distinct launcher icons crafted for Jadzumi Player.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

                                    var currentAppIconId by rememberSaveable { mutableStateOf(AppIconManager.getCurrentIconId(context)) }
                                    val iconScope = rememberCoroutineScope()
                                    val screenWidth = LocalConfiguration.current.screenWidthDp
                                    val iconColumns = when {
                                        screenWidth >= 840 -> 5
                                        screenWidth >= 600 -> 4
                                        screenWidth >= 440 -> 3
                                        else -> 2
                                    }

                                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        AppIconManager.ALL_ICONS.chunked(iconColumns).forEach { rowGroup ->
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                rowGroup.forEach { itemIcon ->
                                                    AppIconOptionCard(
                                                        appIcon = itemIcon,
                                                        selected = currentAppIconId == itemIcon.id,
                                                        onClick = {
                                                            if (currentAppIconId != itemIcon.id) {
                                                                currentAppIconId = itemIcon.id
                                                                iconScope.launch {
                                                                    val ok = AppIconManager.setAppIcon(context, itemIcon.id)
                                                                    if (ok) {
                                                                        statusNotice = "Applied \"${itemIcon.name}\" icon. Note: Launcher may take a few moments to refresh."
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                val emptySlots = iconColumns - rowGroup.size
                                                repeat(emptySlots) {
                                                    Spacer(Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            "Home-screen icon caching depends on your Android launcher and may update after a device unlock.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }

                                    HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { onReplaySplash() }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Replay Cinema Portal Splash Screen", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Play the cinematic opening portal animation in current theme", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                            item {
                                Panel {
                                    Text("Files", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))

                                    // 1. Blacklist
                                    Row(
                                        Modifier.fillMaxWidth().clickable { showFolderBlacklistDialog = true }.padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                                        ) {
                                            Icon(Icons.Rounded.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Column {
                                                Text("Blacklist", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Control which folders are visible or hidden.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    // 2. Show .nomedia files
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                                        ) {
                                            Icon(Icons.Rounded.FolderOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Column {
                                                Text("Show .nomedia files", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Show files in folders containing .nomedia file.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Switch(
                                            checked = library.showNoMediaFiles,
                                            onCheckedChange = library::updateShowNoMediaFiles
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    // 3. Show hidden files
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                                        ) {
                                            Icon(Icons.Rounded.Article, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Column {
                                                Text("Show hidden files", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("Show files starting with dot(.).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Switch(
                                            checked = library.showHiddenFiles,
                                            onCheckedChange = library::updateShowHiddenFiles
                                        )
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
                                AboutRightPane(darkSurface = effectiveTheme != "Light")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDisplayDuringPlaybackDialog) {
        DisplayDuringPlaybackDialog(
            playback = playback,
            onDismiss = { showDisplayDuringPlaybackDialog = false }
        )
    }

    if (showFolderBlacklistDialog) {
        FolderBlacklistDialog(
            library = library,
            folders = localState.folders,
            onDismiss = { showFolderBlacklistDialog = false }
        )
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
    val border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Card(
        onClick = onClick,
        modifier = modifier.touchMotion(),
        shape = RoundedCornerShape(20.dp),
        border = border,
        elevation = CardDefaults.cardElevation(0.dp),
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

@Composable private fun AppIconOptionCard(
    appIcon: AppIcon,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        border = border,
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Image(
                    painter = painterResource(id = appIcon.iconRes),
                    contentDescription = appIcon.name,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Text(
                    text = appIcon.name,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (selected) "Active" else appIcon.subtitle,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(16.dp)
                )
            }
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
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
                BackupControls(playback = playback, onStatus = onStatus)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun AboutRightPane(darkSurface: Boolean) {
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var showLicenses by remember { mutableStateOf(false) }
    if (showLicenses) {
        val notices = remember {
            buildString {
                append(context.assets.open("THIRD_PARTY_LICENSES.md").bufferedReader().use { it.readText() })
                append("\n\n")
                append(context.assets.open("LICENSE").bufferedReader().use { it.readText() })
                context.assets.list("licenses").orEmpty().sorted().forEach { name ->
                    append("\n\n$name\n")
                    append(context.assets.open("licenses/$name").bufferedReader().use { it.readText() })
                }
            }
        }
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text("Open-source licenses") },
            text = {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                        Text(notices, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text("Close") } }
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("ABOUT JADZUMI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("App Identity", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Panel {
            TextButton(onClick = {
                uriHandler.openUri("https://github.com/omkaarpatil787-dev/Jadzumi-Video-Player")
            }) { Text("Source code") }
            TextButton(onClick = { showLicenses = true }) { Text("Licenses") }
        }

        Panel {
            UpdateSettings()
        }

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GeminiGlowingJadzumiLogo(size = 64.dp, darkSurface = darkSurface)
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

@Composable
private fun Panel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalThemeName.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialExpressive.cardBg(theme),
        border = MaterialExpressive.cardBorder(theme),
        shadowElevation = MaterialExpressive.cardElevation(theme)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            content = content
        )
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
