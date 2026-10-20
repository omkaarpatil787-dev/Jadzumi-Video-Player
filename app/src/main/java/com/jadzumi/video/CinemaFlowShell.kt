package com.jadzumi.video

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Cinema Flow Navigation Rail for Tablet & Desktop Screens (Stitch aside.nav-rail)
 */
@Composable
fun CinemaFlowNavRail(
    route: String,
    onNavigate: (String) -> Unit,
    isCompact: Boolean,
    theme: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalCinemaFlowColors.current
    val railWidth = if (isCompact) 80.dp else 236.dp

    Surface(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .cinemaGlassSurface(CinemaCorners.Xl),
        color = Color.Transparent,
        shape = CinemaCorners.Xl
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 20.dp, horizontal = if (isCompact) 8.dp else 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Brand Block
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = if (isCompact) Alignment.CenterHorizontally else Alignment.Start
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = if (isCompact) 0.dp else 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GeminiGlowingJadzumiLogo(
                        darkSurface = theme != "Light",
                        size = 36.dp
                    )
                    if (!isCompact) {
                        Column {
                            Text(
                                "JADZUMI",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                letterSpacing = 0.12.sp,
                                color = colors.inkPrimary
                            )
                            Text(
                                "CINEMA FLOW",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.18.sp,
                                color = colors.accentPrimary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 2. Navigation Items Grouped by Category
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isCompact) {
                    Text(
                        "DISCOVER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.14.sp,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp)
                    )
                }
                CinemaRailItem(
                    label = "Home",
                    icon = Icons.Rounded.Home,
                    isSelected = route == "Home" || route == "Library",
                    isCompact = isCompact,
                    onClick = { onNavigate("Home") }
                )

                if (!isCompact) {
                    Text(
                        "MEDIA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.14.sp,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 2.dp)
                    )
                }
                CinemaRailItem(
                    label = "Movies",
                    icon = Icons.Rounded.Movie,
                    isSelected = route == "Movies",
                    isCompact = isCompact,
                    onClick = { onNavigate("Movies") }
                )
                CinemaRailItem(
                    label = "TV Shows",
                    icon = Icons.Rounded.Tv,
                    isSelected = route == "TV Shows",
                    isCompact = isCompact,
                    onClick = { onNavigate("TV Shows") }
                )
                CinemaRailItem(
                    label = "Folders",
                    icon = Icons.Rounded.Folder,
                    isSelected = route == "Folders",
                    isCompact = isCompact,
                    onClick = { onNavigate("Folders") }
                )

                if (!isCompact) {
                    Text(
                        "SYSTEM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.14.sp,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 2.dp)
                    )
                }
                CinemaRailItem(
                    label = "Settings",
                    icon = Icons.Rounded.Settings,
                    isSelected = route == "Settings",
                    isCompact = isCompact,
                    onClick = { onNavigate("Settings") }
                )
            }

            // 3. Footer Offline Status Badge
            CinemaRailOfflineFooter(isCompact = isCompact)
        }
    }
}

@Composable
private fun CinemaRailItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalCinemaFlowColors.current
    val targetBg = if (isSelected) colors.pillActiveBg else Color.Transparent
    val targetColor = if (isSelected) colors.accentPrimary else colors.inkSecondary
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.05f else 1.0f, label = "railItemScale")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(CinemaCorners.Md)
            .then(
                if (isSelected) {
                    Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    colors.accentSoft.copy(alpha = 0.28f),
                                    colors.accentLavender.copy(alpha = 0.18f)
                                )
                            ),
                            CinemaCorners.Md
                        )
                        .border(1.dp, colors.glassBorder, CinemaCorners.Md)
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (isCompact) 0.dp else 14.dp),
        contentAlignment = if (isCompact) Alignment.Center else Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = targetColor,
                modifier = Modifier
                    .size(22.dp)
                    .scale(scale)
            )
            if (!isCompact) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = targetColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CinemaRailOfflineFooter(isCompact: Boolean) {
    val colors = LocalCinemaFlowColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulseDot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    Surface(
        shape = CinemaCorners.Md,
        color = colors.glassCard.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, colors.glassBorder.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (isCompact) 8.dp else 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34D399).copy(alpha = dotAlpha))
            )
            if (!isCompact) {
                Text(
                    "100% Offline Mode",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkSecondary
                )
            }
        }
    }
}

/**
 * Cinema Flow Top App Bar with frosted search capsule and sync icon
 */
@Composable
fun CinemaFlowTopAppBar(
    route: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    selectedId: String?,
    onBack: () -> Unit,
    onSync: () -> Unit,
    isSyncing: Boolean,
    canSync: Boolean,
    isTablet: Boolean,
    greetingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalCinemaFlowColors.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .cinemaGlassSurface(CinemaCorners.Xl),
        color = Color.Transparent,
        shape = CinemaCorners.Xl
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Back button or Greeting or Route title
            if (selectedId != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CinemaCorners.Md)
                        .background(colors.glassCard)
                        .border(1.dp, colors.glassBorder, CinemaCorners.Md)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.inkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary,
                    modifier = Modifier.weight(1f)
                )
            } else if (greetingContent != null && (route == "Home" || route == "Library")) {
                Box(modifier = Modifier.weight(1f)) {
                    greetingContent()
                }
            } else {
                Text(
                    text = route,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Middle/Right: Glass Search Capsule
            Surface(
                modifier = Modifier
                    .widthIn(min = if (isTablet) 240.dp else 160.dp, max = if (isTablet) 420.dp else 240.dp)
                    .height(42.dp),
                shape = CinemaCorners.Pill,
                color = colors.glassCard,
                border = BorderStroke(1.dp, colors.glassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = colors.inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search videos, shows...",
                                color = colors.inkMuted,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.inkPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(colors.accentPrimary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = onClearQuery,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = colors.inkMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Sync Button
            IconButton(
                onClick = onSync,
                enabled = !isSyncing && canSync,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CinemaCorners.Md)
                    .background(colors.glassCard)
                    .border(1.dp, colors.glassBorder, CinemaCorners.Md)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.accentPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = "Sync library",
                        tint = colors.accentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
