package com.example

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.preferences.ThemeMode
import com.example.player.PlayerDisplayMode
import com.example.ui.player.MiniPlayerBar
import com.example.ui.player.VideoPlayerOverlay
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShortsScreen
import com.example.ui.theme.CineFlowTheme
import com.example.viewmodel.CineFlowViewModel
import com.example.viewmodel.PrimaryDestination

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CineFlowApp()
        }
    }
}

@Composable
fun CineFlowApp(
    viewModel: CineFlowViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()

    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (uiState.preferences.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(playbackState.displayMode, useDarkTheme) {
        val activity = context as? Activity
        if (activity != null) {
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !useDarkTheme &&
                playbackState.displayMode != PlayerDisplayMode.FULLSCREEN_LANDSCAPE &&
                activeTab != PrimaryDestination.SHORTS
            insetsController.isAppearanceLightNavigationBars = !useDarkTheme &&
                playbackState.displayMode != PlayerDisplayMode.FULLSCREEN_LANDSCAPE

            if (playbackState.displayMode == PlayerDisplayMode.FULLSCREEN_LANDSCAPE) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {}
    }

    BackHandler(
        enabled = activeTab != PrimaryDestination.HOME &&
            playbackState.displayMode != PlayerDisplayMode.FULLSCREEN_LANDSCAPE &&
            playbackState.displayMode != PlayerDisplayMode.EMBEDDED_HALF
    ) {
        viewModel.selectTab(PrimaryDestination.HOME)
    }

    CineFlowTheme(darkTheme = useDarkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            val isFullscreen = playbackState.displayMode == PlayerDisplayMode.FULLSCREEN_LANDSCAPE

            if (!isFullscreen) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (playbackState.displayMode == PlayerDisplayMode.MINI &&
                                playbackState.currentMedia != null
                            ) {
                                MiniPlayerBar(
                                    playbackState = playbackState,
                                    onExpand = {
                                        viewModel.playbackController.setDisplayMode(
                                            PlayerDisplayMode.EMBEDDED_HALF
                                        )
                                    },
                                    onTogglePlayPause = {
                                        viewModel.playbackController.togglePlayPause()
                                    },
                                    onClose = {
                                        viewModel.playbackController.closePlayer()
                                    }
                                )
                            }

                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("main_bottom_navigation")
                            ) {
                                NavigationBarItem(
                                    selected = activeTab == PrimaryDestination.HOME,
                                    onClick = { viewModel.selectTab(PrimaryDestination.HOME) },
                                    icon = {
                                        Icon(
                                            imageVector = if (activeTab == PrimaryDestination.HOME) {
                                                Icons.Filled.Home
                                            } else {
                                                Icons.Outlined.Home
                                            },
                                            contentDescription = "Home"
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "Home",
                                            fontWeight = if (activeTab == PrimaryDestination.HOME) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.testTag("nav_tab_home")
                                )

                                NavigationBarItem(
                                    selected = activeTab == PrimaryDestination.SHORTS,
                                    onClick = { viewModel.selectTab(PrimaryDestination.SHORTS) },
                                    icon = {
                                        Icon(
                                            imageVector = if (activeTab == PrimaryDestination.SHORTS) {
                                                Icons.Filled.VideoLibrary
                                            } else {
                                                Icons.Outlined.VideoLibrary
                                            },
                                            contentDescription = "Shorts"
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "Shorts",
                                            fontWeight = if (activeTab == PrimaryDestination.SHORTS) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.testTag("nav_tab_shorts")
                                )

                                NavigationBarItem(
                                    selected = activeTab == PrimaryDestination.SETTINGS,
                                    onClick = { viewModel.selectTab(PrimaryDestination.SETTINGS) },
                                    icon = {
                                        Icon(
                                            imageVector = if (activeTab == PrimaryDestination.SETTINGS) {
                                                Icons.Filled.Settings
                                            } else {
                                                Icons.Outlined.Settings
                                            },
                                            contentDescription = "Settings"
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "Settings",
                                            fontWeight = if (activeTab == PrimaryDestination.SETTINGS) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.testTag("nav_tab_settings")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (activeTab) {
                            PrimaryDestination.HOME -> {
                                HomeScreen(
                                    uiState = uiState,
                                    onPlayMedia = { item ->
                                        viewModel.openMediaInPlayer(
                                            item = item,
                                            targetMode = PlayerDisplayMode.EMBEDDED_HALF
                                        )
                                    },
                                    onPlayMediaAtChapter = { item, chapter ->
                                        viewModel.openMediaInPlayer(
                                            item = item,
                                            targetMode = PlayerDisplayMode.EMBEDDED_HALF
                                        )
                                        viewModel.playbackController.seekTo(chapter.startTimeMs)
                                    },
                                    onSelectFilter = viewModel::setFilter,
                                    onSelectFolderFilter = viewModel::setFolderFilter,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onSortOrderChange = viewModel::setSortOrder,
                                    onNavigateToSettings = {
                                        viewModel.selectTab(PrimaryDestination.SETTINGS)
                                    }
                                )
                            }

                            PrimaryDestination.SHORTS -> {
                                ShortsScreen(
                                    shortsItems = uiState.shortsItems,
                                    onOpenInMainPlayer = { shortItem ->
                                        viewModel.openMediaInPlayer(
                                            item = shortItem,
                                            targetMode = PlayerDisplayMode.EMBEDDED_HALF
                                        )
                                    },
                                    onNavigateToSettings = {
                                        viewModel.selectTab(PrimaryDestination.SETTINGS)
                                    },
                                    onPauseBackgroundPlayer = {
                                        viewModel.playbackController.pause()
                                    }
                                )
                            }

                            PrimaryDestination.SETTINGS -> {
                                SettingsScreen(
                                    uiState = uiState,
                                    onFolderPicked = viewModel::onFolderSelectedFromPicker,
                                    onRescanAllFolders = viewModel::rescanAllFolders,
                                    onRescanSingleFolder = viewModel::rescanSingleFolder,
                                    onRemoveFolder = viewModel::removeFolder,
                                    onDismissBanner = viewModel::clearStatusBanner,
                                    onThemeModeChanged = viewModel::setThemeMode,
                                    onPauseHistoryChanged = viewModel::setPauseWatchHistory,
                                    onClearWatchHistory = viewModel::clearWatchHistory,
                                    onRemoveSingleFromHistory = viewModel::removeSingleItemFromHistory,
                                    onPlayHistoryItem = { historyItem ->
                                        viewModel.openMediaInPlayer(
                                            item = historyItem,
                                            targetMode = PlayerDisplayMode.EMBEDDED_HALF
                                        )
                                    },
                                    onClearHomeFeed = viewModel::clearHomeFeed,
                                    onScanNestedChanged = viewModel::setScanNestedSubfolders,
                                    onIncludeAudioChanged = viewModel::setIncludeAudioFiles
                                )
                            }
                        }
                    }
                }
            }

            if (playbackState.displayMode == PlayerDisplayMode.EMBEDDED_HALF ||
                playbackState.displayMode == PlayerDisplayMode.FULLSCREEN_LANDSCAPE
            ) {
                VideoPlayerOverlay(
                    playbackState = playbackState,
                    exoPlayerProvider = { viewModel.playbackController.exoPlayer },
                    onTogglePlayPause = { viewModel.playbackController.togglePlayPause() },
                    onSeekTo = { pos -> viewModel.playbackController.seekTo(pos) },
                    onSeekRelative = { delta -> viewModel.playbackController.seekRelative(delta) },
                    onToggle2xSpeed = { viewModel.playbackController.toggle2xSpeed() },
                    onStartHold2xSpeed = { viewModel.playbackController.startHold2xSpeed() },
                    onStopHold2xSpeed = { viewModel.playbackController.stopHold2xSpeed() },
                    onSetPlaybackSpeed = { spd -> viewModel.playbackController.setPlaybackSpeed(spd) },
                    onSetDisplayMode = { mode -> viewModel.playbackController.setDisplayMode(mode) },
                    onPlayNext = viewModel::playNextInQueue,
                    onPlayPrevious = viewModel::playPreviousInQueue,
                    onSelectQueueItem = { next ->
                        viewModel.openMediaInPlayer(next, playbackState.displayMode)
                    },
                    onAddChaptersBatch = { rawText ->
                        val current = playbackState.currentMedia ?: return@VideoPlayerOverlay
                        viewModel.addChaptersFromText(
                            mediaUri = current.uri,
                            rawTimestamps = rawText,
                            durationMs = playbackState.durationMs.coerceAtLeast(current.durationMs)
                        )
                    },
                    onAddChapterAtCurrentTime = { title ->
                        val current = playbackState.currentMedia ?: return@VideoPlayerOverlay
                        viewModel.addSingleChapterAtCurrentPosition(
                            mediaUri = current.uri,
                            startTimeMs = playbackState.currentPositionMs,
                            title = title
                        )
                    },
                    onDeleteChapter = viewModel::deleteChapter,
                    onFallbackProgress = viewModel.playbackController::onFallbackPlayerProgress,
                    onFallbackError = viewModel.playbackController::onFallbackPlayerError,
                    onClosePlayer = { viewModel.playbackController.closePlayer() }
                )
            }
        }
    }
}
