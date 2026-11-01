package com.jadzumi.video

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@Composable
fun FixMatchDialog(
    initialQuery: String,
    videoIdentity: String,
    currentMetadata: MovieMetadata,
    onMatchApplied: (OnlineMediaMetadata) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(initialQuery) }
    var isSearching by remember { mutableStateOf(false) }
    var candidates by remember { mutableStateOf<List<CandidateMatch>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val localPath = OnlineMetadataFetcher.copyCustomPosterToCache(context, uri, videoIdentity)
                val customMeta = OnlineMediaMetadata(
                    title = currentMetadata.title,
                    year = currentMetadata.year,
                    rating = currentMetadata.rating,
                    genres = currentMetadata.genres,
                    synopsis = currentMetadata.synopsis,
                    posterUrl = null,
                    backdropUrl = null,
                    localPosterPath = localPath
                )
                OnlineMetadataFetcher.setManualOverride(context, videoIdentity, customMeta)
                onMatchApplied(customMeta)
                onDismiss()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (initialQuery.isNotBlank()) {
            isSearching = true
            candidates = OnlineMetadataFetcher.searchCandidates(context, initialQuery)
            isSearching = false
            hasSearched = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Fix Match & Select Artwork",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Search Cinemeta/IMDb or pick custom art from gallery",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Search Bar & Gallery Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(100) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        label = { Text("Movie Title or IMDb ID") },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )

                    Button(
                        onClick = {
                            if (query.isNotBlank()) {
                                scope.launch {
                                    isSearching = true
                                    candidates = OnlineMetadataFetcher.searchCandidates(context, query)
                                    isSearching = false
                                    hasSearched = true
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text("Search", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Gallery Picker Quick Button
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Pick Custom Poster from Device Gallery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Candidate Results List
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        isSearching -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Searching Cinemeta & IMDb...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        hasSearched && candidates.isEmpty() -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.MovieFilter, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Spacer(Modifier.height(8.dp))
                                Text("No matches found for \"$query\"", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Try searching with the official English title or pick from gallery.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(candidates) { candidate ->
                                    CandidateMatchRow(
                                        candidate = candidate,
                                        onSelect = {
                                            scope.launch {
                                                val localPoster = candidate.posterUrl?.let { url ->
                                                    OnlineMetadataFetcher.downloadAndCachePoster(
                                                        context,
                                                        url,
                                                        "manual_" + OnlineMetadataFetcher.sanitizeKey(candidate.title)
                                                    )
                                                }
                                                val newMeta = OnlineMediaMetadata(
                                                    title = candidate.title,
                                                    year = candidate.year,
                                                    rating = candidate.rating,
                                                    genres = candidate.genres,
                                                    synopsis = candidate.synopsis,
                                                    posterUrl = candidate.posterUrl,
                                                    backdropUrl = candidate.backdropUrl,
                                                    localPosterPath = localPoster
                                                )
                                                OnlineMetadataFetcher.setManualOverride(context, videoIdentity, newMeta)
                                                onMatchApplied(newMeta)
                                                onDismiss()
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

@Composable
private fun CandidateMatchRow(
    candidate: CandidateMatch,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster thumbnail or placeholder
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                if (!candidate.posterUrl.isNullOrBlank()) {
                    SidecarImage(
                        uri = candidate.posterUrl,
                        fallbackVideo = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = candidate.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (candidate.year != null) {
                        Text(candidate.year, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (candidate.rating != null) {
                        Text("★ ${candidate.rating}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    }
                    if (candidate.genres.isNotEmpty()) {
                        Text(candidate.genres.take(2).joinToString(" · "), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (!candidate.synopsis.isNullOrBlank()) {
                    Text(
                        candidate.synopsis,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                }
            }

            // Apply Button
            FilledTonalButton(
                onClick = onSelect,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Select", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
