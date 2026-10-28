package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.MediaItemEntity
import com.example.data.local.ScannedFolderEntity
import com.example.data.preferences.ThemeMode
import com.example.ui.components.FormatUtils
import com.example.ui.components.MediaArtworkCanvas
import com.example.ui.theme.ProgressRed
import com.example.viewmodel.HomeUiState
import java.io.File

private enum class SettingsSubPage {
    MAIN,
    HISTORY,
    FOLDERS
}

/**
 * YouTube-inspired flat, edge-to-edge Settings screen with clean list rows,
 * thin section dividers, radio dialog for Appearance, and dedicated sub-screens
 * for Watch History and Media Folders.
 */
@Composable
fun SettingsScreen(
    uiState: HomeUiState,
    onFolderPicked: (Uri) -> Unit,
    onRescanAllFolders: () -> Unit,
    onRescanSingleFolder: (String) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onDismissBanner: () -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onPauseHistoryChanged: (Boolean) -> Unit,
    onClearWatchHistory: () -> Unit,
    onRemoveSingleFromHistory: (String) -> Unit,
    onPlayHistoryItem: (MediaItemEntity) -> Unit,
    onClearHomeFeed: () -> Unit,
    onScanNestedChanged: (Boolean) -> Unit,
    onIncludeAudioChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            onFolderPicked(treeUri)
        }
    }

    var currentSubPage by remember { mutableStateOf(SettingsSubPage.MAIN) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearFeedDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = currentSubPage != SettingsSubPage.MAIN) {
        currentSubPage = SettingsSubPage.MAIN
    }

    when (currentSubPage) {
        SettingsSubPage.HISTORY -> {
            YouTubeHistorySubScreen(
                historyItems = uiState.watchHistoryItems,
                isHistoryPaused = uiState.preferences.pauseWatchHistory,
                onBack = { currentSubPage = SettingsSubPage.MAIN },
                onPlayItem = onPlayHistoryItem,
                onRemoveItem = onRemoveSingleFromHistory,
                onTogglePauseHistory = onPauseHistoryChanged,
                onRequestClearAll = { showClearHistoryDialog = true }
            )
        }

        SettingsSubPage.FOLDERS -> {
            YouTubeFoldersSubScreen(
                folders = uiState.scannedFolders,
                isScanning = uiState.scanProgress.isScanning,
                scanFolder = uiState.scanProgress.currentFolder,
                scannedCount = uiState.scanProgress.scannedFilesCount,
                onBack = { currentSubPage = SettingsSubPage.MAIN },
                onAddFolder = { folderPickerLauncher.launch(null) },
                onRescanAll = onRescanAllFolders,
                onRescanFolder = onRescanSingleFolder,
                onRemoveFolder = onRemoveFolder
            )
        }

        SettingsSubPage.MAIN -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .testTag("settings_screen_root")
            ) {
                // Clean YouTube-style Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )

                // Subtle status toast bar when an action finishes
                AnimatedVisibility(visible = uiState.statusBannerMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDismissBanner)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.statusBannerMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Active scanning progress bar at top of Settings
                AnimatedVisibility(visible = uiState.scanProgress.isScanning) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Scanning ${uiState.scanProgress.currentFolder} (${uiState.scanProgress.scannedFilesCount} files)…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // SECTION 1: General
                    item {
                        YouTubeSectionHeader("General")
                    }

                    item {
                        val themeLabel = when (uiState.preferences.themeMode) {
                            ThemeMode.DARK -> "Dark theme"
                            ThemeMode.LIGHT -> "Light theme"
                            ThemeMode.SYSTEM -> "Use device theme"
                        }
                        YouTubeSettingsRow(
                            icon = Icons.Outlined.Brightness6,
                            title = "Appearance",
                            subtitle = themeLabel,
                            onClick = { showAppearanceDialog = true },
                            showChevron = true,
                            testTag = "settings_appearance_row"
                        )
                    }

                    item {
                        YouTubeSettingsDivider()
                        YouTubeSectionHeader("History & privacy")
                    }

                    // Watch history sub-screen row
                    item {
                        val historySub = if (uiState.watchHistoryItems.isEmpty()) {
                            "No watched videos yet"
                        } else {
                            "${uiState.watchHistoryItems.size} videos watched · Tap to view or manage"
                        }
                        YouTubeSettingsRow(
                            icon = Icons.Outlined.History,
                            title = "Watch history",
                            subtitle = historySub,
                            onClick = { currentSubPage = SettingsSubPage.HISTORY },
                            showChevron = true,
                            testTag = "settings_watch_history_row"
                        )
                    }

                    // Pause watch history toggle row
                    item {
                        YouTubeSettingsSwitchRow(
                            icon = Icons.Outlined.PauseCircle,
                            title = "Pause watch history",
                            subtitle = if (uiState.preferences.pauseWatchHistory) {
                                "Watch history is paused"
                            } else {
                                "Save watched progress and Continue watching items"
                            },
                            checked = uiState.preferences.pauseWatchHistory,
                            onCheckedChange = onPauseHistoryChanged,
                            testTag = "toggle_pause_history"
                        )
                    }

                    // Clear watch history row
                    item {
                        YouTubeSettingsRow(
                            icon = Icons.Outlined.DeleteOutline,
                            title = "Clear all watch history",
                            subtitle = "Reset all red progress bars and clear Continue watching",
                            onClick = { showClearHistoryDialog = true },
                            testTag = "settings_clear_history_button"
                        )
                    }

                    // Clear home feed row
                    item {
                        YouTubeSettingsRow(
                            icon = Icons.Outlined.RestoreFromTrash,
                            title = "Clear home feed",
                            subtitle = "Remove all indexed folders and reset Home and Shorts",
                            onClick = { showClearFeedDialog = true },
                            testTag = "settings_clear_home_feed_button"
                        )
                    }

                    item {
                        YouTubeSettingsDivider()
                        YouTubeSectionHeader("Local library & folders")
                    }

                    // Add media folder row (direct SAF picker)
                    item {
                        YouTubeSettingsRow(
                            icon = Icons.Outlined.Add,
                            title = "Add media folder",
                            subtitle = "Select a local folder to scan videos, movies, and audio",
                            onClick = { folderPickerLauncher.launch(null) },
                            testTag = "settings_add_folder_button"
                        )
                    }

                    // Manage connected folders row
                    item {
                        val foldersSub = if (uiState.scannedFolders.isEmpty()) {
                            "No folders connected"
                        } else {
                            "${uiState.scannedFolders.size} folder(s) connected · ${uiState.allItems.size} media items"
                        }
                        YouTubeSettingsRow(
                            icon = Icons.Outlined.Folder,
                            title = "Manage media folders",
                            subtitle = foldersSub,
                            onClick = { currentSubPage = SettingsSubPage.FOLDERS },
                            showChevron = true,
                            testTag = "settings_manage_folders_row"
                        )
                    }

                    // Rescan library row
                    item {
                        YouTubeSettingsRow(
                            icon = Icons.Outlined.Refresh,
                            title = "Rescan library",
                            subtitle = "Refresh folders for newly downloaded or moved files",
                            onClick = {
                                if (uiState.scannedFolders.isEmpty()) {
                                    folderPickerLauncher.launch(null)
                                } else {
                                    onRescanAllFolders()
                                }
                            },
                            testTag = "settings_rescan_all_button"
                        )
                    }

                    // Scan nested subfolders toggle
                    item {
                        YouTubeSettingsSwitchRow(
                            icon = Icons.Outlined.SubdirectoryArrowRight,
                            title = "Scan nested subfolders",
                            subtitle = "Automatically index media inside subdirectories",
                            checked = uiState.preferences.scanNestedSubfolders,
                            onCheckedChange = onScanNestedChanged,
                            testTag = "toggle_scan_nested"
                        )
                    }

                    // Include audio files toggle
                    item {
                        YouTubeSettingsSwitchRow(
                            icon = Icons.Outlined.LibraryMusic,
                            title = "Include audio files",
                            subtitle = "Show MP3, FLAC, WAV, M4A, and OGG files in library",
                            checked = uiState.preferences.includeAudioFiles,
                            onCheckedChange = onIncludeAudioChanged,
                            testTag = "toggle_include_audio"
                        )
                    }

                    item {
                        YouTubeSettingsDivider()
                        YouTubeSectionHeader("Video player & gestures")
                    }

                    item {
                        YouTubeSettingsRow(
                            icon = Icons.Outlined.Speed,
                            title = "Hold video for 2× speed",
                            subtitle = "Press and hold anywhere on the video player to play at 2× speed",
                            onClick = {}
                        )
                    }

                    item {
                        YouTubeSettingsRow(
                            icon = Icons.Outlined.PlayCircle,
                            title = "Double-tap to seek",
                            subtitle = "10 seconds (left side to rewind, right side to fast-forward)",
                            onClick = {}
                        )
                    }

                    item {
                        YouTubeSettingsDivider()
                        YouTubeSectionHeader("About")
                    }

                    item {
                        YouTubeSettingsRow(
                            icon = Icons.Outlined.Info,
                            title = "LocalTube version",
                            subtitle = "1.1 (Offline Media3 & Hardware Decoder Fallback)",
                            onClick = {}
                        )
                    }
                }
            }
        }
    }

    // YouTube-Style Appearance Radio Dialog
    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    YouTubeRadioOption(
                        label = "Use device theme",
                        selected = uiState.preferences.themeMode == ThemeMode.SYSTEM,
                        onClick = {
                            onThemeModeChanged(ThemeMode.SYSTEM)
                            showAppearanceDialog = false
                        },
                        testTag = "theme_option_system"
                    )
                    YouTubeRadioOption(
                        label = "Light theme",
                        selected = uiState.preferences.themeMode == ThemeMode.LIGHT,
                        onClick = {
                            onThemeModeChanged(ThemeMode.LIGHT)
                            showAppearanceDialog = false
                        },
                        testTag = "theme_option_light"
                    )
                    YouTubeRadioOption(
                        label = "Dark theme",
                        selected = uiState.preferences.themeMode == ThemeMode.DARK,
                        onClick = {
                            onThemeModeChanged(ThemeMode.DARK)
                            showAppearanceDialog = false
                        },
                        testTag = "theme_option_dark"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppearanceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Watch History Confirmation Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Clear watch history?") },
            text = {
                Text("Your LocalTube watch history and Continue watching progress will be cleared from this device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        onClearWatchHistory()
                    }
                ) {
                    Text(
                        text = "Clear watch history",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Home Feed Confirmation Dialog
    if (showClearFeedDialog) {
        AlertDialog(
            onDismissRequest = { showClearFeedDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Clear home feed?") },
            text = {
                Text("This removes all connected folders from LocalTube and resets your Home and Shorts feeds. Your personal media files on your device are never deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearFeedDialog = false
                        onClearHomeFeed()
                    }
                ) {
                    Text(
                        text = "Clear home feed",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearFeedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun YouTubeHistorySubScreen(
    historyItems: List<MediaItemEntity>,
    isHistoryPaused: Boolean,
    onBack: () -> Unit,
    onPlayItem: (MediaItemEntity) -> Unit,
    onRemoveItem: (String) -> Unit,
    onTogglePauseHistory: (Boolean) -> Unit,
    onRequestClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Watch history",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (historyItems.isNotEmpty()) {
                TextButton(onClick = onRequestClearAll) {
                    Text("Clear all")
                }
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )

        YouTubeSettingsSwitchRow(
            icon = Icons.Outlined.PauseCircle,
            title = "Pause watch history",
            subtitle = if (isHistoryPaused) "History is paused" else "Recording watch history",
            checked = isHistoryPaused,
            onCheckedChange = onTogglePauseHistory,
            testTag = "history_page_pause_switch"
        )

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )

        if (historyItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Videos you watch will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(historyItems, key = { "hist_${it.uri}" }) { item ->
                    val fraction = if (item.durationMs > 0L) {
                        (item.lastPlayedPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayItem(item) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(136.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                        ) {
                            val hasThumb = item.thumbnailPath != null && File(item.thumbnailPath).exists()
                            if (hasThumb) {
                                AsyncImage(
                                    model = File(item.thumbnailPath!!),
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                MediaArtworkCanvas(
                                    title = item.title,
                                    isAudio = item.isAudio,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (item.durationMs > 0L) {
                                Surface(
                                    color = Color(0xD90A0A0A),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = FormatUtils.formatDuration(item.durationMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (fraction > 0f) {
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(2.5.dp),
                                    color = ProgressRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${item.folderName} · ${FormatUtils.formatRelativeTime(item.lastPlayedAtMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        IconButton(onClick = { onRemoveItem(item.uri) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove from watch history",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubeFoldersSubScreen(
    folders: List<ScannedFolderEntity>,
    isScanning: Boolean,
    scanFolder: String,
    scannedCount: Int,
    onBack: () -> Unit,
    onAddFolder: () -> Unit,
    onRescanAll: () -> Unit,
    onRescanFolder: (String) -> Unit,
    onRemoveFolder: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Media folders",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAddFolder) {
                Text("+ Add folder")
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )

        if (isScanning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Scanning $scanFolder ($scannedCount files)…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (folders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No media folders connected",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap '+ Add folder' in the top right to select your Movies, Download, or Music folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    YouTubeSettingsRow(
                        icon = Icons.Outlined.Refresh,
                        title = "Rescan all folders",
                        subtitle = "Check all connected folders for new or deleted files",
                        onClick = onRescanAll
                    )
                    YouTubeSettingsDivider()
                    YouTubeSectionHeader("Connected folders")
                }

                items(folders, key = { it.treeUri }) { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = if (folder.isAccessValid) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (folder.isAccessValid) {
                                    "${folder.videoCount} videos · ${folder.audioCount} audio · Scanned ${FormatUtils.formatRelativeTime(folder.lastScannedAtMs)}"
                                } else {
                                    "Access revoked — tap Add folder to re-authorize"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (folder.isAccessValid) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                        if (folder.isAccessValid) {
                            IconButton(onClick = { onRescanFolder(folder.treeUri) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Rescan folder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onRemoveFolder(folder.treeUri) }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Remove folder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubeSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun YouTubeSettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 6.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}

@Composable
private fun YouTubeSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showChevron: Boolean = false,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (testTag != null) it.testTag(testTag) else it }
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showChevron) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun YouTubeSettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun YouTubeRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
