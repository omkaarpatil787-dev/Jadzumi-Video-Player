package com.jadzumi.video

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

@Composable
fun TvShowDetailsPage(
    series: TvSeries,
    library: LocalLibrary,
    playback: PlaybackController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sortedSeasonNumbers = remember(series.seasons) {
        series.seasons.keys.sortedWith(Comparator { a, b ->
            if (a == 0) 1 else if (b == 0) -1 else a.compareTo(b)
        })
    }

    var selectedSeason by rememberSaveable(series.id) {
        mutableIntStateOf(sortedSeasonNumbers.firstOrNull { it > 0 } ?: sortedSeasonNumbers.firstOrNull() ?: 1)
    }
    var sortAscending by rememberSaveable { mutableStateOf(true) }

    val currentSeasonEpisodes = remember(series.seasons, selectedSeason, sortAscending) {
        val list = series.seasons[selectedSeason] ?: emptyList()
        if (sortAscending) list else list.reversed()
    }

    val nextUnwatched = remember(series, playback.historyVersion) {
        series.nextUnwatchedEpisode(playback)
    }

    val totalWatchedInSeries = remember(series, playback.historyVersion) {
        series.watchedCount(playback)
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isTablet = maxWidth >= 840.dp
        val horizontalPad = if (isTablet) 28.dp else 16.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // 1. Top Breadcrumb & 100% Offline Badge
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onBack)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "TV Shows  ›  ${series.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                if (isTablet) "100% Offline Local Metadata" else "Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Hero Show Banner Card
            item {
                ShowHeroCard(
                    series = series,
                    nextUnwatched = nextUnwatched,
                    totalWatched = totalWatchedInSeries,
                    onPlayNext = {
                        val epToPlay = nextUnwatched ?: currentSeasonEpisodes.firstOrNull()
                        if (epToPlay != null) {
                            playback.open(epToPlay, currentSeasonEpisodes)
                            playback.fullscreen = true
                        }
                    },
                    onMarkSeasonWatched = {
                        val allInSeason = series.seasons[selectedSeason] ?: emptyList()
                        val isSeasonWatched = allInSeason.all { ep ->
                            playback.savedPosition(ep) >= (ep.duration.takeIf { d -> d > 0 } ?: 1000L) * 0.9f
                        }
                        playback.markSeasonWatched(allInSeason, !isSeasonWatched)
                    }
                )
            }

            // 3. Season Tabs Row & Audio/Subtitles Info Pills
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Season tabs
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(sortedSeasonNumbers) { s ->
                            val epList = series.seasons[s] ?: emptyList()
                            val watchedCount = epList.count { ep ->
                                val pos = playback.savedPosition(ep)
                                val dur = ep.duration.takeIf { it > 0 } ?: 1000L
                                pos >= dur * 0.9f
                            }
                            val isCompleted = watchedCount == epList.size && epList.isNotEmpty()
                            val isSelected = s == selectedSeason
                            val seasonLabel = when (s) {
                                0 -> "OVAs & Specials"
                                else -> "Season $s"
                            }

                            SeasonTabPill(
                                label = seasonLabel,
                                badgeText = if (isCompleted) "${epList.size}/${epList.size} Completed" else "$watchedCount/${epList.size} Watched",
                                isSelected = isSelected,
                                isCompleted = isCompleted,
                                onClick = { selectedSeason = s }
                            )
                        }
                    }

                    // Default Audio & Subtitle detected chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Audiotrack, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Default Audio:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Multichannel 5.1 / EAC3", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.ClosedCaption, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Default Subs:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("English [Styled ASS / SRT]", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // 4. Two-Column Tablet Layout: Episode List (Left) + Technical Inspector (Right)
            item {
                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left Column: Episodes (Weight 1.7)
                        Column(
                            modifier = Modifier.weight(1.7f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            EpisodesHeader(
                                count = currentSeasonEpisodes.size,
                                sortAscending = sortAscending,
                                onToggleSort = { sortAscending = !sortAscending }
                            )

                            currentSeasonEpisodes.forEach { video ->
                                val info = EpisodePatterns.parse(video.name)
                                val isNext = nextUnwatched?.identity == video.identity
                                EpisodeCard(
                                    video = video,
                                    info = info,
                                    isNext = isNext,
                                    playback = playback,
                                    onClick = {
                                        playback.open(video, currentSeasonEpisodes)
                                        playback.fullscreen = true
                                    }
                                )
                            }
                        }

                        // Right Column: Technical Inspector & Offline Verification (Weight 1.0)
                        Column(
                            modifier = Modifier.weight(1.0f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TechnicalInspectorCard(
                                sampleVideo = currentSeasonEpisodes.firstOrNull()
                            )
                            OfflineVerificationCard()
                        }
                    }
                } else {
                    // Mobile / Narrow Layout: Stacked
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        EpisodesHeader(
                            count = currentSeasonEpisodes.size,
                            sortAscending = sortAscending,
                            onToggleSort = { sortAscending = !sortAscending }
                        )

                        currentSeasonEpisodes.forEach { video ->
                            val info = EpisodePatterns.parse(video.name)
                            val isNext = nextUnwatched?.identity == video.identity
                            EpisodeCard(
                                video = video,
                                info = info,
                                isNext = isNext,
                                playback = playback,
                                onClick = {
                                    playback.open(video, currentSeasonEpisodes)
                                    playback.fullscreen = true
                                }
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        TechnicalInspectorCard(sampleVideo = currentSeasonEpisodes.firstOrNull())
                        OfflineVerificationCard()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShowHeroCard(
    series: TvSeries,
    nextUnwatched: LocalVideo?,
    totalWatched: Int,
    onPlayNext: () -> Unit,
    onMarkSeasonWatched: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val isTablet = maxWidth >= 600.dp
        val heroPad = if (isTablet) 22.dp else 16.dp
        val posterWidth = if (isTablet) 135.dp else 102.dp

        val nextInfo = nextUnwatched?.let { EpisodePatterns.parse(it.name) }
        val buttonText = when {
            nextInfo != null -> "Play S%02dE%02d".format(nextInfo.season, nextInfo.episode)
            else -> "Start Series"
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(heroPad),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Poster Image
                    Box(
                        modifier = Modifier
                            .width(posterWidth)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        SidecarImage(
                            uri = series.posterUri,
                            fallbackVideo = nextUnwatched ?: series.seasons.values.firstOrNull()?.firstOrNull(),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Info Beside Poster
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = series.title,
                            style = if (isTablet) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Metadata badge chips: unified CircleShape pills
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "TV Series",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    "1080p HEVC",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            series.mpaa?.let { mpaa ->
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        mpaa,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            series.year?.let { yr ->
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        yr,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    "${series.seasonCount} Seasons",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    "${series.totalEpisodes} Episodes",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isTablet) {
                            Text(
                                text = series.synopsis ?: "Offline TV Series in your local library. High-efficiency playback with hardware-accelerated decoding.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (!isTablet) {
                    Text(
                        text = series.synopsis ?: "Offline TV Series in your local library. High-efficiency playback with hardware-accelerated decoding.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action Buttons: Symmetrical and beautifully aligned pill buttons
                if (isTablet) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onPlayNext,
                            modifier = Modifier.height(44.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onMarkSeasonWatched,
                            modifier = Modifier.height(44.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text("Toggle Season Watched", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Mobile Portrait Action Buttons: Full-width primary button + secondary button below
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onPlayNext,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        OutlinedButton(
                            onClick = onMarkSeasonWatched,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text("Toggle Season Watched", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonTabPill(
    label: String,
    badgeText: String,
    isSelected: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.clip(CircleShape).clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                )
            }
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    badgeText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EpisodesHeader(
    count: Int,
    sortAscending: Boolean,
    onToggleSort: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Episodes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    "$count Total",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onToggleSort)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.SwapVert, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text(if (sortAscending) "Ascending" else "Descending", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    video: LocalVideo,
    info: SeriesEpisodeInfo?,
    isNext: Boolean,
    playback: PlaybackController,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val savedPos = playback.savedPosition(video)
    val dur = video.duration.takeIf { it > 0 } ?: 1000L
    val isWatched = savedPos >= dur * 0.9f
    val inProgress = savedPos in 1 until (dur * 0.9f).toLong()
    val progressFraction = (savedPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            if (isNext) 2.dp else 1.dp,
            if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode Thumbnail with badges & progress
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                LocalThumbnail(video = video, modifier = Modifier.fillMaxSize())

                // Timecode
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                ) {
                    Text(
                        if (video.duration > 0) timecode(video.duration / 1000) else "24:00",
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Watched / Up Next Badge
                if (isWatched) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E8E3E),
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, Modifier.size(11.dp), tint = Color.White)
                            Text("Watched", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else if (isNext) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                    ) {
                        Text("Up Next", modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Progress Bar at bottom
                if (progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(progressFraction)
                            .height(3.dp)
                            .background(if (isWatched) Color(0xFF1E8E3E) else MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Episode Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val epNumber = info?.episode ?: 1
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            "EP %02d".format(epNumber),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = info?.episodeTitle ?: "Episode $epNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = video.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Tech specs & Status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sizeStr = if (video.bytes >= 0) Formatter.formatFileSize(context, video.bytes) else "HD"
                    Text(sizeStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outlineVariant)
                    val resStr = if (video.width > 0 && video.height > 0) "${video.width}×${video.height}" else "1080p FHD"
                    Text(resStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        if (isWatched) "Completed" else if (inProgress) "Resume ${timecode(savedPos / 1000)}" else "Unwatched",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isWatched) Color(0xFF1E8E3E) else if (inProgress) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Quick Play Icon
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            ) {
                Icon(
                    if (isWatched) Icons.Rounded.Replay else Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
internal fun TechnicalInspectorCard(sampleVideo: LocalVideo?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Memory, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text("Technical Inspector", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            TechRow("Container & Codec", "MKV / HEVC Main 10-bit")
            TechRow("Resolution", if (sampleVideo != null && sampleVideo.width > 0) "${sampleVideo.width} × ${sampleVideo.height} FHD" else "1920 × 1080 FHD")
            TechRow("Color Space", "BT.709 / 10-bit YUV420p")
            TechRow("Frame Rate", "23.976 fps")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("HW Acceleration", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE6F4EA)
                ) {
                    Text("Detected (MediaCodec HW+)", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF137333))
                }
            }

            TechRow("Audio Decoder", "FFmpeg 6.1 (libavcodec)")
            TechRow("Subtitle Engine", "libass / System CaptionStyle")
        }
    }
}

@Composable
internal fun TechRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
internal fun OfflineVerificationCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CloudDone, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Metadata Fully Cached", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Posters, episode synopses, and chapter marks ready for 100% offline usage.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

internal object SidecarMemory {
    val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    val permits = Semaphore(3)
}

internal fun decodeSampledBitmap(context: android.content.Context, uriString: String, reqWidth: Int = 800, reqHeight: Int = 1200): Bitmap? {
    val uri = Uri.parse(uriString)
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        var inSampleSize = 1
        val halfHeight = options.outHeight / 2
        val halfWidth = options.outWidth / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
        options.inSampleSize = inSampleSize
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565

        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    } catch (_: Throwable) {
        null
    }
}

@Composable
internal fun SidecarImage(
    uri: String?,
    fallbackVideo: LocalVideo?,
    modifier: Modifier
) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = uri) {
        value = null
        if (uri != null) {
            value = withContext(Dispatchers.IO) {
                SidecarMemory.permits.withPermit {
                    SidecarMemory.cache.get(uri) ?: decodeSampledBitmap(context, uri)?.also {
                        SidecarMemory.cache.put(uri, it)
                    }
                }
            }
        }
    }

    if (bitmap != null) {
        Image(bitmap!!.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop)
    } else if (fallbackVideo != null) {
        LocalThumbnail(fallbackVideo, modifier)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Movie, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}
