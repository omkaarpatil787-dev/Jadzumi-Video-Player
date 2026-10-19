package com.jadzumi.video

import androidx.core.content.edit
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Blue = Color(0xFF005BBF)
private val PaleBlue = Color(0xFFD0E1FA)
private val Light = lightColorScheme(primary=Blue, primaryContainer=PaleBlue, onPrimaryContainer=Color(0xFF0B1D2F), background=Color(0xFFF7F9FC), surface=Color.White, surfaceVariant=Color(0xFFECF1F8), onSurface=Color(0xFF191C1E), onSurfaceVariant=Color(0xFF414754))
private val Dark = darkColorScheme(primary=Color(0xFFADC7FF), primaryContainer=Color(0xFF234572), background=Color(0xFF111820), surface=Color(0xFF1B2532), surfaceVariant=Color(0xFF273446))
private data class Destination(val name: String, val icon: ImageVector)
private val destinations = listOf(Destination("Library", Icons.Rounded.VideoLibrary), Destination("Folders", Icons.Rounded.FolderOpen), Destination("Playlists",Icons.AutoMirrored.Rounded.QueueMusic), Destination("Player",Icons.Rounded.PlayCircle), Destination("Settings",Icons.Rounded.Settings))

@Composable fun JadzumiApp(playback:PlaybackController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("appearance",0) }
    var theme by rememberSaveable { mutableStateOf(prefs.getString("theme","Light") ?: "Light") }
    var reducedMotion by rememberSaveable { mutableStateOf(prefs.getBoolean("reducedMotion",false)) }
    var route by rememberSaveable { mutableStateOf("Library") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var demo by rememberSaveable { mutableStateOf(false) }
    val library = remember { LocalLibrary.get(context) }
    val localState by library.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val colors = when(theme) { "Dark" -> Dark; "AMOLED" -> Dark.copy(background=Color.Black,surface=Color(0xFF080808)); "System" -> if(isSystemInDarkTheme()) Dark else Light; else -> Light }
    DisposableEffect(colors) {
        val style = if(colors == Light) SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) else SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        (context as? ComponentActivity)?.enableEdgeToEdge(statusBarStyle=style,navigationBarStyle=style)
        onDispose { }
    }
    MaterialTheme(colorScheme=colors, shapes=Shapes(medium=RoundedCornerShape(24.dp),large=RoundedCornerShape(28.dp))) {
        Box(Modifier.fillMaxSize()) {
        Surface(Modifier.fillMaxSize(),color=colors.background) {
            BackHandler(selectedId != null || route != "Library") { if(selectedId != null) selectedId=null else route="Library" }
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
                val wide = maxWidth >= 720.dp
                Row(Modifier.fillMaxSize()) {
                    if(wide) AppRail(route) { route=it; selectedId=null }
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal=24.dp, vertical=10.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                            if(selectedId != null) IconButton(onClick={selectedId=null}) { Icon(Icons.AutoMirrored.Rounded.ArrowBack,"Back to library") }
                            if(route == "Library" && selectedId == null) {
                                OutlinedTextField(query,{query=it},Modifier.weight(1f).widthIn(max=520.dp),placeholder={Text("Search your video library")},singleLine=true,shape=RoundedCornerShape(32.dp),leadingIcon={Icon(Icons.Rounded.Search,null)})
                            } else Text(if(selectedId != null) "Library / Details" else route,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                            AssistChip(onClick={route="Settings";selectedId=null},label={Text("M3 Preview")})
                        }
                        Crossfade(targetState=selectedId ?: route, animationSpec=tween(if(reducedMotion) 0 else 220),modifier=Modifier.weight(1f),label="screen") { current ->
                            val video = previewVideos.find { it.id==current }
                            val localVideo = localState.videos.find { it.uri==current }
                            if(localVideo != null) LocalDetailPage(localVideo,localState,playback)
                            else if(video != null) DetailPage(video)
                            else when(current) {
                                "Library" -> if(demo) LibraryPage(query,demo,{demo=it}) { selectedId=it } else LocalLibraryPage(localState,library,query,{selectedId=it},{demo=true})
                                "Folders" -> LocalFoldersPage(localState,library,wide) { selectedId=it }
                                "Settings" -> SettingsPage(wide,theme,{theme=it;prefs.edit { putString("theme",it) }},reducedMotion,{reducedMotion=it;prefs.edit { putBoolean("reducedMotion",it) }})
                                "Playlists" -> EmptyPage(Icons.AutoMirrored.Rounded.QueueMusic,"Your collections, together","Playlist creation is planned for a later update.")
                                else -> if(playback.video!=null) Column(Modifier.padding(28.dp)) { Text("${playback.video?.title}",style=MaterialTheme.typography.titleLarge);Button(onClick={playback.fullscreen=true}) { Text("Open player") } } else EmptyPage(Icons.Rounded.PlayCircle,"Your cinema is ready","Choose an imported video from your library, then tap Play.")
                            }
                        }
                        if(!wide) NavigationBar {
                            destinations.forEach { d -> NavigationBarItem(selected=route==d.name,onClick={route=d.name;selectedId=null},icon={Icon(d.icon,d.name)},label={Text(d.name,maxLines=1,fontSize=10.sp)}) }
                        }
                    }
                }
            }
        }
        CinemaPlayer(playback,reducedMotion)
        }
    }
}

@Composable private fun AppRail(route: String, navigate: (String)->Unit) {
    Surface(color=MaterialTheme.colorScheme.surface) {
        Column(Modifier.width(96.dp).fillMaxHeight().padding(vertical=24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Surface(color=Color(0xFF1A73E8),shape=RoundedCornerShape(18.dp)) { Icon(Icons.Rounded.PlayArrow,null,Modifier.padding(12.dp).size(28.dp),tint=Color.White) }
            Text("Jadzumi",Modifier.padding(top=8.dp,bottom=28.dp),fontWeight=FontWeight.Bold,fontSize=13.sp)
            destinations.take(4).forEach { d -> NavigationRailItem(selected=route==d.name,onClick={navigate(d.name)},icon={Icon(d.icon,null)},label={Text(d.name,fontSize=11.sp)},modifier=Modifier.padding(bottom=12.dp)) }
            Spacer(Modifier.weight(1f))
            NavigationRailItem(selected=route=="Settings",onClick={navigate("Settings")},icon={Icon(Icons.Rounded.Settings,null)},label={Text("Settings",fontSize=11.sp)})
        }
    }
}

@Composable private fun LibraryPage(query: String,demo: Boolean,onDemo:(Boolean)->Unit,onSelect:(String)->Unit) {
    var category by rememberSaveable { mutableStateOf("All videos") }
    val filtered = filterVideos(if(demo) previewVideos else emptyList(),query,category)
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(28.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
        item { Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Your cinema,\nat home.",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold); Text("A quiet space for the stories you keep.",color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=8.dp)) }; } }
        item { Panel { Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Design preview",fontWeight=FontWeight.SemiBold); Text("Sample titles only · no videos imported",style=MaterialTheme.typography.bodySmall) }; Switch(demo,onDemo,modifier=Modifier.semantics { contentDescription="Show sample titles" }) } } }
        item { LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)) { items(listOf("All videos","Movies","TV & Anime")) { c -> FilterChip(selected=category==c,onClick={category=c},label={Text(c)}) } } }
        if(filtered.isEmpty()) item { EmptyPage(Icons.Rounded.VideoLibrary,if(demo) "No matching titles" else "Make room for your stories",if(demo) "Try another title or category." else "Your real library is empty. Folder import arrives in the next milestone.") }
        if(filtered.isNotEmpty()) {
            item { Hero(filtered.first(),onSelect) }
            item { Text("Continue watching",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }
            items(filtered,key={it.id}) { video -> VideoRow(video) { onSelect(video.id) } }
            item { Text("Preview artwork is bundled locally. No network access or media permission is used by this build.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable private fun Art(video: PreviewVideo, modifier: Modifier=Modifier) {
    val id=when(video.artwork) { "anime" -> R.drawable.anime; "cosmos" -> R.drawable.cosmos; else -> R.drawable.hero }
    Image(painterResource(id),contentDescription=null,modifier=modifier,contentScale=ContentScale.Crop)
}
@Composable private fun Hero(video:PreviewVideo,onSelect:(String)->Unit) {
    Card(onClick={onSelect(video.id)},shape=RoundedCornerShape(28.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min=280.dp)) {
            Art(video,Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color(0xF0102034),Color(0x550F2038)))))
            Column(Modifier.padding(28.dp).widthIn(max=580.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Text("FEATURED · SAMPLE ${video.category.uppercase()}",color=Color(0xFFADC7FF),style=MaterialTheme.typography.labelMedium)
                Text(video.title,color=Color.White,fontSize=32.sp,lineHeight=38.sp,fontWeight=FontWeight.Bold)
                Text("Pick up where the story left off.",color=Color(0xFFD7E3F6))
                Button(onClick={onSelect(video.id)}) { Icon(Icons.Rounded.Movie,null); Spacer(Modifier.width(8.dp));Text("Explore details") }
            }
        }
    }
}
@Composable private fun VideoRow(video:PreviewVideo,onSelect:()->Unit) {
    Card(onClick=onSelect,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),shape=RoundedCornerShape(24.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
            Art(video,Modifier.size(100.dp,76.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Text(video.title,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
                Text("${timecode(video.positionSeconds)} / ${timecode(video.durationSeconds)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress={video.progress},modifier=Modifier.fillMaxWidth().height(4.dp))
            }
            Icon(Icons.Rounded.ChevronRight,"View details")
        }
    }
}
@Composable private fun DetailPage(video:PreviewVideo) {
    var favorite by rememberSaveable(video.id) { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(28.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
        item { Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(28.dp))) { Art(video,Modifier.fillMaxSize()); Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xD9101C30))))); Text(video.title,Modifier.align(Alignment.BottomStart).padding(24.dp),color=Color.White,fontSize=32.sp,fontWeight=FontWeight.Bold) } }
        item { Panel { Text("SAMPLE ${video.category.uppercase()}",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge); Text(if(video.id=="show") "A temporal investigator follows fractured timelines through Neo-Shinjuku." else "A story waiting to be rediscovered in your personal cinema.",modifier=Modifier.padding(vertical=12.dp)); Text("${timecode(video.remainingSeconds)} remaining · ${(video.progress*100).toInt()}% watched (sample)"); LinearProgressIndicator(progress={video.progress},modifier=Modifier.fillMaxWidth().padding(vertical=16.dp)); Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { Button(onClick={},enabled=false) { Text("Playback in next milestone") }; IconToggleButton(checked=favorite,onCheckedChange={favorite=it}) { Icon(if(favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,if(favorite) "Remove preview favorite" else "Favorite preview") } } } }
        if(video.id=="show") {
            item { Text("Season 2 · Preview episodes",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }
            items(listOf("08 · Fractured Horizons","09 · Convergence of Timelines","10 · Echoes of Tomorrow","11 · The Singularity Gate","12 · Infinite Present")) { name -> Panel { Text(name,fontWeight=FontWeight.SemiBold); Text("Sample episode · playback not yet available",style=MaterialTheme.typography.bodySmall) } }
        } else item { Panel { Text("Chapters & timestamps",style=MaterialTheme.typography.titleLarge); Text("Real chapter data will appear after local media is imported.",Modifier.padding(top=12.dp)) } }
        item { Panel { Text("Media details",fontWeight=FontWeight.Bold); Text("This is a design sample. No file has been scanned, no checksum verified, and no decoder or surround format has been tested.",Modifier.padding(top=8.dp),color=MaterialTheme.colorScheme.onSurfaceVariant) } }
    }
}
@Composable private fun SettingsPage(wide:Boolean,theme:String,onTheme:(String)->Unit,reduced:Boolean,onReduced:(Boolean)->Unit) {
    var section by rememberSaveable { mutableStateOf("Appearance") }
    val sections=listOf("Appearance","Playback","Audio","Subtitles","Performance","Storage","Privacy","About Me")
    Column(Modifier.fillMaxSize().padding(horizontal=28.dp)) {
        Text("Settings & Customization",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=16.dp))
        Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
            if(wide) LazyColumn(Modifier.width(210.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { items(sections) { s -> FilterChip(selected=section==s,onClick={section=s},label={Text(s)},modifier=Modifier.fillMaxWidth()) } }
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                if(!wide) item { LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { items(sections) { s -> FilterChip(section==s,{section=s},label={Text(s)}) } } }
                item { Text(section,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }
                when(section) {
                    "Appearance" -> {
                        item {
                            Panel {
                                Text("Base theme", fontWeight=FontWeight.Bold)
                                Text("Saved on this device", style=MaterialTheme.typography.bodySmall)
                                Column {
                                    listOf("Light","Dark","AMOLED","System").forEach { t ->
                                        Row(verticalAlignment=Alignment.CenterVertically) {
                                            RadioButton(theme==t, {onTheme(t)})
                                            TextButton(onClick={onTheme(t)}) { Text(t) }
                                        }
                                    }
                                }
                            }
                        }
                        item { Panel { Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Reduced motion",fontWeight=FontWeight.Bold);Text("Remove screen fades",style=MaterialTheme.typography.bodySmall) };Switch(reduced,onReduced,modifier=Modifier.semantics { contentDescription="Reduced motion" }) };Text("Pale-blue accents · rounded surfaces · native controls",Modifier.padding(top=12.dp),style=MaterialTheme.typography.bodySmall) } }
                    }
                    "Privacy" -> item { Panel { Text("Your cinema stays yours.",style=MaterialTheme.typography.titleLarge);Text("This build has no internet permission, accounts, analytics or ads. Only folders you select are read. Library data and appearance preferences remain on this device. Android cloud backup is disabled.",Modifier.padding(top=16.dp)) } }
                    "About Me" -> item { Panel { Text("Jadzumi Video",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Created for Omkar Patil",Modifier.padding(vertical=12.dp));Text("Version ${BuildConfig.VERSION_NAME} · Milestone 3");Text("Native Kotlin + Jetpack Compose\nTablet-first · Xiaomi Pad 5\nOriginal UI references supplied by Omkar",Modifier.padding(top=16.dp)); Text("Playback preview: codec validation, advanced subtitles, full Material 3 Expressive motion and remaining customization are still to be implemented.",Modifier.padding(top=16.dp),style=MaterialTheme.typography.bodySmall) } }
                    else -> item { Panel { Text("Planned for the next milestones",fontWeight=FontWeight.Bold);Text(when(section) { "Playback" -> "Resume, seeking gestures, screen lock, speed, aspect ratio and picture-in-picture."; "Audio" -> "Track selection, AC-3/E-AC-3 validation, surround downmix and supported output routing."; "Subtitles" -> "Embedded and external subtitles, styling, language selection and sync."; "Performance" -> "Hardware decoding, software fallback investigation and frame pacing tests."; else -> "Use Folders to add local or USB folders, refresh the index, and remove access without deleting your videos. Thumbnails use a bounded memory cache." },Modifier.padding(top=12.dp)) } }
                }
            }
        }
    }
}
@Composable private fun Panel(content:@Composable ColumnScope.()->Unit) { Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface) { Column(Modifier.padding(24.dp),content=content) } }
@Composable private fun EmptyPage(icon:ImageVector,title:String,body:String) { Column(Modifier.fillMaxWidth().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) { Icon(icon,null,Modifier.size(56.dp),tint=MaterialTheme.colorScheme.primary);Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(body,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
