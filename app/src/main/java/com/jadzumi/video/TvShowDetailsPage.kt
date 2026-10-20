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
    var onlineShow by remember(series.id, series.title) {
        mutableStateOf(
            OnlineMetadataFetcher.getCachedTvShow(context, series.title)
                ?: OnlineMetadataFetcher.getCachedTvShow(context, series.id)
        )
    }
    LaunchedEffect(series.id, series.title) {
        val fetched = OnlineMetadataFetcher.fetchTvShowMetadata(context, series.title)
            ?: OnlineMetadataFetcher.fetchTvShowMetadata(context, series.id)
        if (fetched != null) {
            onlineShow = fetched
        }
    }

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
        val topPad = if (isTablet) 20.dp else 12.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = horizontalPad, top = topPad, end = horizontalPad, bottom = if (isTablet) 24.dp else 88.dp),
            verticalArrangement = Arrangement.spacedBy(if (isTablet) 22.dp else 16.dp)
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
                            .weight(1f, fill = false)
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
                            text = "TV Shows  ›  ${onlineShow?.title ?: series.title}",
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
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                if (onlineShow != null) Icons.Rounded.CloudDone else Icons.Rounded.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                if (onlineShow != null) (if (isTablet) "Verified Album Artwork & Metadata" else "Online Art")
                                else if (series.posterUri != null) (if (isTablet) "100% Offline Sidecar Artwork" else "Offline Sidecar")
                                else if (isTablet) "Hardware Offline Analysis" else "Offline",
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
                    onlineShow = onlineShow,
                    isTablet = isTablet,
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

                    // Default Audio & Subtitle detected chips: Adaptive FlowRow for phone portrait & tablet
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                Text("Multichannel 5.1 / EAC3", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1)
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
                                Text("English [Styled ASS / SRT]", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1)
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
    onlineShow: OnlineMediaMetadata?,
    isTablet: Boolean,
    onPlayNext: () -> Unit,
    onMarkSeasonWatched: () -> Unit
) {
    var synopsisExpanded by rememberSaveable { mutableStateOf(false) }
    val displayPoster = onlineShow?.localPosterPath
        ?: onlineShow?.posterUrl
        ?: series.posterUri
        ?: series.backdropUri
    val syn = series.synopsis
    val displaySynopsis = onlineShow?.synopsis?.takeIf { it.isNotBlank() }
        ?: syn
        ?: "Offline TV Series in your local library. High-efficiency playback with hardware-accelerated decoding."

    val displayTitle = onlineShow?.title?.takeIf { it.isNotBlank() } ?: series.title
    val displayYear = onlineShow?.year?.takeIf { it.isNotBlank() } ?: series.year ?: "2024"
    val displayGenres = (onlineShow?.genres?.takeIf { it.isNotEmpty() } ?: series.genres).filter { it != "Cinema" && it != "Feature Film" }

    val heroPad = if (isTablet) 22.dp else 16.dp
    val posterWidth = if (isTablet) 140.dp else 102.dp

    val nextInfo = nextUnwatched?.let { EpisodePatterns.parse(it.name) }
    val buttonText = when {
        nextInfo != null -> "Play S%02dE%02d".format(nextInfo.season, nextInfo.episode)
        else -> "Start Series"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .cinemaGlassCard(CinemaCorners.Xl),
        shape = CinemaCorners.Xl,
        color = Color.Transparent
    ) {
            Column(
                modifier = Modifier.padding(heroPad),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Poster Image (2:3 aspect ratio)
                    Box(
                        modifier = Modifier
                            .width(posterWidth)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        SidecarImage(
                            uri = displayPoster,
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
                            text = displayTitle,
                            style = if (isTablet) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (displayGenres.isNotEmpty()) {
                            Text(
                                displayGenres.joinToString(" · "),
                                fontSize = if (isTablet) 13.sp else 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

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

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    displayYear,
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
                                text = displaySynopsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                                maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
                            )
                        }
                    }
                }

                if (!isTablet) {
                    Text(
                        text = displaySynopsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
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

@Composable
private fun SeasonTabPill(
    label: String,
    badgeText: String,
    isSelected: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalCinemaFlowColors.current
    Surface(
        shape = CinemaCorners.Pill,
        color = if (isSelected) colors.pillActiveBg else colors.pillBg,
        modifier = Modifier
            .clip(CinemaCorners.Pill)
            .clickable(onClick = onClick)
            .cinemaPill(active = isSelected)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) colors.pillActiveText else Color(0xFF1E8E3E)
                )
            }
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) colors.pillActiveText else colors.inkPrimary
            )
            Surface(
                shape = CinemaCorners.Pill,
                color = if (isSelected) colors.pillActiveText.copy(alpha = 0.15f) else colors.bgSecondary
            ) {
                Text(
                    badgeText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) colors.pillActiveText else colors.inkMuted
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
    val colors = LocalCinemaFlowColors.current
    val savedPos = playback.savedPosition(video)
    val dur = video.duration.takeIf { it > 0 } ?: 1000L
    val isWatched = savedPos >= dur * 0.9f
    val inProgress = savedPos in 1 until (dur * 0.9f).toLong()
    val progressFraction = (savedPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .cinemaGlassCard(CinemaCorners.Lg)
            .clickable(onClick = onClick),
        shape = CinemaCorners.Lg,
        color = if (isNext) colors.accentPrimary.copy(alpha = 0.08f) else Color.Transparent
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val isNarrow = maxWidth < 480.dp
            val thumbWidth = if (isNarrow) 116.dp else 156.dp
            val cardPad = if (isNarrow) 10.dp else 14.dp
            val gap = if (isNarrow) 10.dp else 14.dp

            Row(
                modifier = Modifier.padding(cardPad),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Episode Thumbnail with badges & progress
                Box(
                    modifier = Modifier
                        .width(thumbWidth)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    LocalThumbnail(video = video, modifier = Modifier.fillMaxSize())

                    // Timecode
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                    ) {
                        Text(
                            if (video.duration > 0) timecode(video.duration / 1000) else "24:00",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    // Watched / Up Next Badge
                    if (isWatched) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E8E3E),
                            modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(10.dp), tint = Color.White)
                                Text("Watched", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    } else if (isNext) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                        ) {
                            Text("Up Next", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

                // Episode Details Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val epNumber = info?.episode ?: 1
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                "EP %02d".format(epNumber),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = info?.episodeTitle ?: "Episode $epNumber",
                            style = if (isNarrow) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
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

                    // Adaptive specs & Status (Wraps cleanly without letter-by-letter truncation)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val sizeStr = if (video.bytes >= 0) Formatter.formatFileSize(context, video.bytes) else "HD"
                        Text(sizeStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outlineVariant)
                        val resStr = if (video.width > 0 && video.height > 0) "${video.width}×${video.height}" else "1080p FHD"
                        Text(resStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            if (isWatched) "Completed" else if (inProgress) "Resume ${timecode(savedPos / 1000)}" else "Unwatched",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            color = if (isWatched) Color(0xFF1E8E3E) else if (inProgress) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Quick Play Icon
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(if (isNarrow) 38.dp else 44.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                ) {
                    Icon(
                        if (isWatched) Icons.Rounded.Replay else Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isNarrow) 20.dp else 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun TechnicalInspectorCard(sampleVideo: LocalVideo?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .cinemaGlassCard(CinemaCorners.Lg),
        shape = CinemaCorners.Lg,
        color = Color.Transparent
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
    if (value.length > 25) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 16.sp)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.widthIn(min = 90.dp, max = 130.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
internal fun OfflineVerificationCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .cinemaGlassCard(CinemaCorners.Lg),
        shape = CinemaCorners.Lg,
        color = Color.Transparent
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
    return try {
        val resolvedPathOrUri: String = if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
            val key = uriString.hashCode().toString()
            val posterDir = java.io.File(context.cacheDir, "poster_cache")
            if (!posterDir.exists()) posterDir.mkdirs()
            val cachedFile = java.io.File(posterDir, "$key.jpg")
            if (!cachedFile.exists() || cachedFile.length() < 500) {
                try {
                    val conn = java.net.URL(uriString).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 6000
                    conn.readTimeout = 8000
                    conn.setRequestProperty("User-Agent", "JadzumiPlayer/1.2")
                    if (conn.responseCode in 200..299) {
                        conn.inputStream.use { input ->
                            java.io.FileOutputStream(cachedFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
            if (cachedFile.exists() && cachedFile.length() > 500) cachedFile.absolutePath else uriString
        } else {
            uriString
        }

        fun getInputStream(): java.io.InputStream? {
            return when {
                resolvedPathOrUri.startsWith("/") -> java.io.FileInputStream(java.io.File(resolvedPathOrUri))
                resolvedPathOrUri.startsWith("file://") -> {
                    val path = android.net.Uri.parse(resolvedPathOrUri).path ?: resolvedPathOrUri.removePrefix("file://")
                    java.io.FileInputStream(java.io.File(path))
                }
                else -> context.contentResolver.openInputStream(android.net.Uri.parse(resolvedPathOrUri))
            }
        }

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        getInputStream()?.use { stream ->
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

        getInputStream()?.use { stream ->
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
    val cached = remember(uri) { uri?.let { SidecarMemory.cache.get(it) } }
    val bitmap by produceState<Bitmap?>(initialValue = cached, key1 = uri) {
        if (cached != null) {
            value = cached
            return@produceState
        }
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
        CinematicFallbackPoster(title = "Cinema Series", name = "", modifier = modifier)
    }
}
