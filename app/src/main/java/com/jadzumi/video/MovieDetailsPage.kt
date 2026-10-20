package com.jadzumi.video

import android.content.Context
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieDetailsPage(
    video: LocalVideo,
    state: LocalLibraryState,
    playback: PlaybackController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val library = remember { LocalLibrary.get(context) }
    var confirmDelete by remember { mutableStateOf(false) }
    var synopsisExpanded by rememberSaveable { mutableStateOf(false) }

    val metadata = remember(video, state.sidecars) {
        extractMovieMetadata(video, state.sidecars, context)
    }

    var onlineMeta by remember(video.identity) { mutableStateOf<OnlineMediaMetadata?>(null) }
    LaunchedEffect(video.identity, metadata.title, metadata.year) {
        val fetched = OnlineMetadataFetcher.fetchMovieMetadata(context, metadata.title, metadata.year)
        if (fetched != null) {
            onlineMeta = fetched
        }
    }

    val effectiveMetadata = remember(metadata, onlineMeta) {
        val meta = onlineMeta
        if (meta != null) {
            val syn = metadata.synopsis
            val isGenericSynopsis = syn.isNullOrBlank() || syn.contains("offline library", ignoreCase = true) || syn.contains("feature film stored", ignoreCase = true)
            metadata.copy(
                title = meta.title.ifBlank { metadata.title },
                year = meta.year ?: metadata.year,
                rating = meta.rating ?: metadata.rating,
                genres = if ((metadata.genres.isEmpty() || metadata.genres == listOf("Feature Film", "Cinema")) && meta.genres.isNotEmpty()) meta.genres else metadata.genres,
                synopsis = if (isGenericSynopsis && !meta.synopsis.isNullOrBlank()) meta.synopsis else syn,
                posterUri = metadata.posterUri ?: meta.localPosterPath ?: meta.posterUrl,
                backdropUri = metadata.backdropUri ?: meta.backdropUrl ?: meta.localPosterPath ?: meta.posterUrl
            )
        } else {
            metadata
        }
    }

    val savedPos = playback.savedPosition(video)
    val dur = video.duration.takeIf { it > 0 } ?: 1000L
    val isWatched = savedPos >= dur * 0.9f
    val inProgress = savedPos in 1 until (dur * 0.9f).toLong()
    val progressFraction = (savedPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isTablet = maxWidth >= 840.dp
        val horizontalPad = if (isTablet) 28.dp else 16.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // 1. Top Breadcrumb & Offline Status Badge
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
                            text = "Movies  ›  ${effectiveMetadata.title}",
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
                                if (onlineMeta != null) Icons.Rounded.CloudDone else Icons.Rounded.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                if (onlineMeta != null) (if (isTablet) "Verified Album Artwork & Metadata" else "Online Art")
                                else if (metadata.posterUri != null) (if (isTablet) "100% Offline Sidecar Artwork" else "Offline Sidecar")
                                else if (isTablet) "Hardware Offline Analysis" else "Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Movie Hero Banner Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = 2.dp
                ) {
                    val heroPad = if (isTablet) 22.dp else 16.dp
                    val posterWidth = if (isTablet) 140.dp else 102.dp

                    val fullSynopsis = effectiveMetadata.synopsis
                        ?: "Offline Feature Film stored in your local library. High-efficiency playback with hardware-accelerated MediaCodec decoding."

                    Column(
                        modifier = Modifier.padding(heroPad),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isTablet) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Poster Thumbnail (2:3 aspect ratio)
                                Box(
                                    modifier = Modifier
                                        .width(posterWidth)
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    SidecarImage(
                                        uri = effectiveMetadata.posterUri ?: effectiveMetadata.backdropUri,
                                        fallbackVideo = video,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Movie Info Beside Poster
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = effectiveMetadata.title,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (effectiveMetadata.genres.isNotEmpty()) {
                                        Text(
                                            effectiveMetadata.genres.joinToString(" · "),
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    MovieBadgesFlow(effectiveMetadata, video)

                                    Text(
                                        text = fullSynopsis,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp,
                                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
                                    )
                                }
                            }
                        } else {
                            // Mobile Phone Optimized Layout: Full-width title, genre subtitle, side-by-side poster + badges, synopsis
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = effectiveMetadata.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (effectiveMetadata.genres.isNotEmpty()) {
                                    Text(
                                        effectiveMetadata.genres.joinToString(" · "),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(posterWidth)
                                            .aspectRatio(2f / 3f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    ) {
                                        SidecarImage(
                                            uri = effectiveMetadata.posterUri ?: effectiveMetadata.backdropUri,
                                            fallbackVideo = video,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        MovieBadgesFlow(effectiveMetadata, video)
                                    }
                                }

                                Text(
                                    text = fullSynopsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp,
                                    maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
                                )
                            }
                        }

                        // Action Buttons: Symmetrical and beautifully aligned pill buttons
                        if (isTablet) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        playback.open(video)
                                        playback.fullscreen = true
                                    },
                                    modifier = Modifier.height(44.dp),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (inProgress) "Resume from ${timecode(savedPos / 1000)}" else "Play Movie",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                if (inProgress || isWatched) {
                                    OutlinedButton(
                                        onClick = {
                                            playback.markEpisodeWatched(video, false)
                                            playback.open(video)
                                            playback.fullscreen = true
                                        },
                                        modifier = Modifier.height(44.dp),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Icon(Icons.Rounded.Replay, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(6.dp))
                                        Text("From Beginning", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                IconButton(
                                    onClick = { playback.markEpisodeWatched(video, !isWatched) },
                                    modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Toggle Watched",
                                        tint = if (isWatched) Color(0xFF1E8E3E) else MaterialTheme.colorScheme.outline
                                    )
                                }

                                IconButton(
                                    onClick = { confirmDelete = true },
                                    modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else {
                            // Phone Portrait Action Buttons: Big prominent primary button + sleek secondary row
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        playback.open(video)
                                        playback.fullscreen = true
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (inProgress) "Resume from ${timecode(savedPos / 1000)}" else "Play Movie",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (inProgress || isWatched) {
                                        OutlinedButton(
                                            onClick = {
                                                playback.markEpisodeWatched(video, false)
                                                playback.open(video)
                                                playback.fullscreen = true
                                            },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = CircleShape,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Icon(Icons.Rounded.Replay, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Restart", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = { playback.markEpisodeWatched(video, !isWatched) },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isWatched) Color(0xFF1E8E3E).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isWatched) Color(0xFF1E8E3E) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Icon(
                                            if (isWatched) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            null,
                                            Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (isWatched) "Watched" else "Mark Done", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = { confirmDelete = true },
                                        modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Two-Column Tablet Ergonomics Layout: File Specs & Playback Status (Left) + Technical Inspector (Right)
            item {
                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left Column: File Details & Resume History (Weight 1.6)
                        Column(
                            modifier = Modifier.weight(1.6f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            MovieFileOverviewCard(video, state, context)
                            MovieAudioSubtitleCard(video)
                        }

                        // Right Column: Technical Inspector & Offline Badge (Weight 1.0)
                        Column(
                            modifier = Modifier.weight(1.0f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TechnicalInspectorCard(sampleVideo = video)
                            OfflineVerificationCard()
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MovieFileOverviewCard(video, state, context)
                        MovieAudioSubtitleCard(video)
                        TechnicalInspectorCard(sampleVideo = video)
                        OfflineVerificationCard()
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
            title = { Text("Permanently delete movie?") },
            text = { Text("This will permanently delete \"${video.name}\" ($sizeStr) from your device's internal storage. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (playback.video?.identity == video.identity) {
                            playback.close()
                        }
                        library.deleteVideo(video)
                        confirmDelete = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete permanently")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MovieFileOverviewCard(video: LocalVideo, state: LocalLibraryState, context: Context) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.FolderOpen, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text("Local File Specs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            TechRow("Filename", video.name)
            TechRow("Folder", state.folders.firstOrNull { it.uri == video.folder }?.name ?: "Local Storage")
            TechRow("File Size", if (video.bytes >= 0) Formatter.formatFileSize(context, video.bytes) else "Unavailable")
            TechRow("Duration", if (video.duration > 0) timecode(video.duration / 1000) else "Unavailable")
            TechRow("Resolution", if (video.width > 0 && video.height > 0) "${video.width} × ${video.height} pixels" else "Unavailable")
            TechRow("Format / Mime", video.mime.ifEmpty { "video/mkv" })
        }
    }
}

@Composable
private fun MovieAudioSubtitleCard(video: LocalVideo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Subtitles, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text("Audio & Subtitle Tracks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            TechRow("Audio Decoder", "FFmpeg 6.1 (libavcodec)")
            TechRow("Audio Streams", "Multi-channel 5.1 / Stereo Passthrough")
            TechRow("Subtitle Engine", "libass / System CaptionStyle Engine")
            TechRow("Supported Formats", "SubRip (.srt), SubStation Alpha (.ass), PGS")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovieBadgesFlow(metadata: MovieMetadata, video: LocalVideo) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                "Featured Film",
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Quality / Codec Pill
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            val qualityText = when {
                video.width >= 3840 -> "4K UHD"
                video.width >= 1920 -> "1080p HEVC"
                video.name.contains("hevc", true) || video.name.contains("x265", true) -> "HEVC"
                else -> "HD"
            }
            Text(
                qualityText,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        // MPAA Rating Pill
        metadata.mpaa?.let { mpaa ->
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

        // Star Rating Pill
        metadata.rating?.let { rating ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Rounded.Star, null, Modifier.size(12.dp), tint = Color(0xFFF9AB00))
                    Text(rating, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Year Pill
        metadata.year?.let { yr ->
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

        // Duration Pill
        if (video.duration > 0) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Schedule, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        timecode(video.duration / 1000),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
