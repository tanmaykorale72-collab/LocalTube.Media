package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ChapterEntity
import com.example.data.local.MediaItemEntity
import com.example.data.preferences.SortOrder
import com.example.data.preferences.ThemeMode
import com.example.ui.components.FormatUtils
import com.example.ui.components.MediaArtworkCanvas
import com.example.ui.components.MediaCard
import com.example.ui.theme.CardToneForestEnd
import com.example.ui.theme.CardToneForestStart
import com.example.ui.theme.ChipSelectedTextDark
import com.example.ui.theme.ChipSelectedWhite
import com.example.ui.theme.ChipUnselectedDark
import com.example.ui.theme.ChipUnselectedTextDark
import com.example.ui.theme.ProgressRed
import com.example.ui.theme.SageTeal
import com.example.viewmodel.HomeFeedFilter
import com.example.viewmodel.HomeUiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onPlayMedia: (MediaItemEntity) -> Unit,
    onPlayMediaAtChapter: (MediaItemEntity, ChapterEntity) -> Unit,
    onSelectFilter: (HomeFeedFilter) -> Unit,
    onSelectFolderFilter: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var inspectMediaItem by remember { mutableStateOf<MediaItemEntity?>(null) }

    val distinctFolders = remember(uiState.allItems) {
        uiState.allItems.map { it.folderName }.distinct().sorted()
    }

    val systemDark = isSystemInDarkTheme()
    val isDark = when (uiState.preferences.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen_root")
    ) {
        // Top Bar matching the reference screenshot:
        // Left: Sage-Teal rounded-square play icon ONLY (no text name attached)
        // Right: Search icon and 3-dot vertical menu icon
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sage-Teal Play Badge — icon only, no app name text
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SageTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "LocalTube",
                        tint = Color(0xFF0F1917),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Search Toggle
                IconButton(
                    onClick = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded && uiState.searchQuery.isNotEmpty()) {
                            onSearchQueryChange("")
                        }
                    },
                    modifier = Modifier.testTag("home_search_toggle")
                ) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search media library",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                // 3-dot Menu for Sort Order
                Box {
                    IconButton(
                        onClick = { isSortMenuOpen = true },
                        modifier = Modifier.testTag("home_sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Sort options",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    DropdownMenu(
                        expanded = isSortMenuOpen,
                        onDismissRequest = { isSortMenuOpen = false }
                    ) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.label,
                                        fontWeight = if (uiState.preferences.sortOrder == order) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                },
                                onClick = {
                                    onSortOrderChange(order)
                                    isSortMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            // Expandable Search Input
            AnimatedVisibility(visible = isSearchExpanded || uiState.searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "Search titles, folders, artists, or chapters…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("home_search_input")
                )
            }

            // Filter Chips Row matching the screenshot ("All" white pill, unselected dark charcoal pills)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Folder chips right after "All" just like category chips in the screenshot
                val allSelected = uiState.selectedFilter == HomeFeedFilter.ALL && uiState.selectedFolderFilter == null
                item {
                    FilterChip(
                        selected = allSelected,
                        onClick = {
                            onSelectFolderFilter(null)
                            onSelectFilter(HomeFeedFilter.ALL)
                        },
                        label = {
                            Text(
                                text = "All",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDark) ChipSelectedWhite else MaterialTheme.colorScheme.onBackground,
                            selectedLabelColor = if (isDark) ChipSelectedTextDark else MaterialTheme.colorScheme.background,
                            containerColor = if (isDark) ChipUnselectedDark else MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (isDark) ChipUnselectedTextDark else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null,
                        modifier = Modifier.testTag("filter_chip_all")
                    )
                }

                // Dynamic folder chips (matching "Technology", "Privacy", "Development" in screenshot)
                items(distinctFolders) { folderName ->
                    val selected = uiState.selectedFolderFilter == folderName
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFolderFilter(folderName) },
                        label = {
                            Text(
                                text = folderName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDark) ChipSelectedWhite else MaterialTheme.colorScheme.onBackground,
                            selectedLabelColor = if (isDark) ChipSelectedTextDark else MaterialTheme.colorScheme.background,
                            containerColor = if (isDark) ChipUnselectedDark else MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (isDark) ChipUnselectedTextDark else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )
                }

                // Media type filter chips
                items(HomeFeedFilter.entries.filter { it != HomeFeedFilter.ALL }) { filter ->
                    val isSelected = uiState.selectedFilter == filter && uiState.selectedFolderFilter == null
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onSelectFolderFilter(null)
                            onSelectFilter(filter)
                        },
                        label = {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDark) ChipSelectedWhite else MaterialTheme.colorScheme.onBackground,
                            selectedLabelColor = if (isDark) ChipSelectedTextDark else MaterialTheme.colorScheme.background,
                            containerColor = if (isDark) ChipUnselectedDark else MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (isDark) ChipUnselectedTextDark else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null,
                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                    )
                }
            }
        }

        // Revoked folder access notice
        if (uiState.hasRevokedFolders) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToSettings)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Access to one or more media folders was revoked. Tap to re-authorize in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Main Feed Content
        if (uiState.allItems.isEmpty()) {
            EmptyLibraryGuidanceView(
                isScanning = uiState.scanProgress.isScanning,
                scanFolder = uiState.scanProgress.currentFolder,
                onNavigateToSettings = onNavigateToSettings,
                modifier = Modifier.fillMaxSize()
            )
        } else if (uiState.filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No matching media in this filter",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try selecting 'All' or clearing your search query.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 10.dp,
                    bottom = 110.dp
                ),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_media_feed")
            ) {
                // "Continue watching" shelf matching the screenshot
                if (uiState.selectedFilter == HomeFeedFilter.ALL &&
                    uiState.selectedFolderFilter == null &&
                    uiState.searchQuery.isBlank() &&
                    uiState.continueWatchingItems.isNotEmpty()
                ) {
                    item(key = "continue_watching_shelf") {
                        ContinueWatchingShelf(
                            items = uiState.continueWatchingItems,
                            onPlayMedia = onPlayMedia,
                            modifier = Modifier.widthIn(max = 680.dp)
                        )
                    }
                }

                items(
                    items = uiState.filteredItems,
                    key = { it.uri }
                ) { item ->
                    val itemChapters = uiState.chaptersByMediaUri[item.uri].orEmpty()
                    MediaCard(
                        item = item,
                        chapters = itemChapters,
                        onClick = { onPlayMedia(item) },
                        onMoreClick = { inspectMediaItem = item },
                        modifier = Modifier.widthIn(max = 680.dp)
                    )
                }
            }
        }
    }

    // Media File Details & Chapters Sheet
    val sheetItem = inspectMediaItem
    if (sheetItem != null) {
        val sheetChapters = uiState.chaptersByMediaUri[sheetItem.uri].orEmpty()
        ModalBottomSheet(
            onDismissRequest = { inspectMediaItem = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            MediaDetailsBottomSheetContent(
                item = sheetItem,
                chapters = sheetChapters,
                onPlayNow = {
                    inspectMediaItem = null
                    onPlayMedia(sheetItem)
                },
                onJumpToChapter = { chapter ->
                    inspectMediaItem = null
                    onPlayMediaAtChapter(sheetItem, chapter)
                }
            )
        }
    }
}

/**
 * Matches the "Continue watching" horizontal cards from the reference screenshot:
 * - Rounded 16:9 thumbnail with centered play icon, bottom-right duration badge ("12:04"),
 *   and bottom red progress bar (#E53935)
 * - Below thumbnail: 2-line title + 1-line folder/category subtitle
 */
@Composable
private fun ContinueWatchingShelf(
    items: List<MediaItemEntity>,
    onPlayMedia: (MediaItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Continue watching",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(items, key = { "cw_${it.uri}" }) { item ->
                val fraction = if (item.durationMs > 0L) {
                    (item.lastPlayedPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Column(
                    modifier = Modifier
                        .width(172.dp)
                        .clickable { onPlayMedia(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardToneForestEnd)
                    ) {
                        val hasThumb = item.thumbnailPath != null && File(item.thumbnailPath).exists()
                        if (hasThumb) {
                            AsyncImage(
                                model = File(item.thumbnailPath!!),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else {
                            MediaArtworkCanvas(
                                title = item.title,
                                isAudio = item.isAudio,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (item.durationMs > 0L) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 6.dp, bottom = 8.dp),
                                color = Color(0xD90A0A0A),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    text = FormatUtils.formatDuration(item.durationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(3.dp),
                            color = ProgressRed,
                            trackColor = Color.White.copy(alpha = 0.16f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.folderName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryGuidanceView(
    isScanning: Boolean,
    scanFolder: String,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    CardToneForestStart,
                                    CardToneForestEnd
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barY = size.height * 0.82f
                        val barStart = size.width * 0.12f
                        val barEnd = size.width * 0.88f
                        val totalW = barEnd - barStart
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.18f),
                            topLeft = Offset(barStart, barY),
                            size = Size(totalW, 4.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                        drawRoundRect(
                            color = ProgressRed,
                            topLeft = Offset(barStart, barY),
                            size = Size(totalW * 0.45f, 4.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SageTeal,
                        contentColor = Color(0xFF0F1917),
                        modifier = Modifier.size(width = 52.dp, height = 44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (isScanning) {
                        "Scanning Local Media…"
                    } else {
                        "Your Local Media Feed is Ready"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isScanning) {
                        "Indexing videos, Shorts, and audio in $scanFolder"
                    } else {
                        "Authorize your device media folders in Settings to automatically populate your Home feed and Shorts with local videos, movies, and audio."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onNavigateToSettings,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("empty_home_open_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Settings to Configure Folders",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaDetailsBottomSheetContent(
    item: MediaItemEntity,
    chapters: List<ChapterEntity>,
    onPlayNow: () -> Unit,
    onJumpToChapter: (ChapterEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.folderName} · ${item.resolutionLabel} · ${FormatUtils.formatFileSize(item.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onPlayNow,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayCircleFilled, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "File Information",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Filename: ${item.fileName}\nFormat: ${item.mimeType}\nDuration: ${FormatUtils.formatDuration(item.durationMs)}" +
                if (item.width > 0 && item.height > 0) "\nDimensions: ${item.width} × ${item.height}" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (chapters.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Chapters (${chapters.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            chapters.forEach { chapter ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onJumpToChapter(chapter) },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = FormatUtils.formatDuration(chapter.startTimeMs),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
