package com.example.ui.player

import android.view.ViewGroup
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.local.ChapterEntity
import com.example.data.local.MediaItemEntity
import com.example.player.PlaybackUiState
import com.example.player.PlayerDisplayMode
import com.example.ui.components.ChapterTimelineBar
import com.example.ui.components.FormatUtils
import com.example.ui.components.MediaArtworkCanvas
import com.example.ui.theme.ProgressRed
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerOverlay(
    playbackState: PlaybackUiState,
    exoPlayerProvider: () -> ExoPlayer,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onToggle2xSpeed: () -> Unit,
    onStartHold2xSpeed: () -> Unit,
    onStopHold2xSpeed: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetDisplayMode: (PlayerDisplayMode) -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onSelectQueueItem: (MediaItemEntity) -> Unit,
    onAddChaptersBatch: (String) -> Unit,
    onAddChapterAtCurrentTime: (String) -> Unit,
    onDeleteChapter: (ChapterEntity) -> Unit,
    onFallbackProgress: (Long, Long) -> Unit,
    onFallbackError: () -> Unit,
    onClosePlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val media = playbackState.currentMedia ?: return

    BackHandler(enabled = playbackState.displayMode == PlayerDisplayMode.FULLSCREEN_LANDSCAPE) {
        onSetDisplayMode(PlayerDisplayMode.EMBEDDED_HALF)
    }
    BackHandler(enabled = playbackState.displayMode == PlayerDisplayMode.EMBEDDED_HALF) {
        onSetDisplayMode(PlayerDisplayMode.MINI)
    }

    when (playbackState.displayMode) {
        PlayerDisplayMode.HIDDEN -> Unit
        PlayerDisplayMode.MINI -> Unit
        PlayerDisplayMode.FULLSCREEN_LANDSCAPE -> {
            FullscreenLandscapePlayerView(
                media = media,
                playbackState = playbackState,
                exoPlayerProvider = exoPlayerProvider,
                onTogglePlayPause = onTogglePlayPause,
                onSeekTo = onSeekTo,
                onSeekRelative = onSeekRelative,
                onToggle2xSpeed = onToggle2xSpeed,
                onStartHold2xSpeed = onStartHold2xSpeed,
                onStopHold2xSpeed = onStopHold2xSpeed,
                onExitFullscreen = { onSetDisplayMode(PlayerDisplayMode.EMBEDDED_HALF) },
                onPlayNext = onPlayNext,
                onPlayPrevious = onPlayPrevious,
                onFallbackProgress = onFallbackProgress,
                onFallbackError = onFallbackError,
                modifier = modifier
            )
        }
        PlayerDisplayMode.EMBEDDED_HALF -> {
            EmbeddedHalfScreenPlayerView(
                media = media,
                playbackState = playbackState,
                exoPlayerProvider = exoPlayerProvider,
                onTogglePlayPause = onTogglePlayPause,
                onSeekTo = onSeekTo,
                onSeekRelative = onSeekRelative,
                onToggle2xSpeed = onToggle2xSpeed,
                onStartHold2xSpeed = onStartHold2xSpeed,
                onStopHold2xSpeed = onStopHold2xSpeed,
                onSetPlaybackSpeed = onSetPlaybackSpeed,
                onEnterFullscreen = { onSetDisplayMode(PlayerDisplayMode.FULLSCREEN_LANDSCAPE) },
                onMinimizeToMini = { onSetDisplayMode(PlayerDisplayMode.MINI) },
                onPlayNext = onPlayNext,
                onPlayPrevious = onPlayPrevious,
                onSelectQueueItem = onSelectQueueItem,
                onAddChaptersBatch = onAddChaptersBatch,
                onAddChapterAtCurrentTime = onAddChapterAtCurrentTime,
                onDeleteChapter = onDeleteChapter,
                onFallbackProgress = onFallbackProgress,
                onFallbackError = onFallbackError,
                onClosePlayer = onClosePlayer,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun EmbeddedHalfScreenPlayerView(
    media: MediaItemEntity,
    playbackState: PlaybackUiState,
    exoPlayerProvider: () -> ExoPlayer,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onToggle2xSpeed: () -> Unit,
    onStartHold2xSpeed: () -> Unit,
    onStopHold2xSpeed: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onEnterFullscreen: () -> Unit,
    onMinimizeToMini: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onSelectQueueItem: (MediaItemEntity) -> Unit,
    onAddChaptersBatch: (String) -> Unit,
    onAddChapterAtCurrentTime: (String) -> Unit,
    onDeleteChapter: (ChapterEntity) -> Unit,
    onFallbackProgress: (Long, Long) -> Unit,
    onFallbackError: () -> Unit,
    onClosePlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddChaptersDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("embedded_player_screen")
    ) {
        // Top 16:9 Interactive Video / Audio Surface
        InteractiveVideoSurface(
            media = media,
            playbackState = playbackState,
            exoPlayerProvider = exoPlayerProvider,
            isFullscreen = false,
            onTogglePlayPause = onTogglePlayPause,
            onSeekTo = onSeekTo,
            onSeekRelative = onSeekRelative,
            onToggle2xSpeed = onToggle2xSpeed,
            onStartHold2xSpeed = onStartHold2xSpeed,
            onStopHold2xSpeed = onStopHold2xSpeed,
            onToggleFullscreen = onEnterFullscreen,
            onMinimizePlayer = onMinimizeToMini,
            onSwipeUpGesture = onEnterFullscreen,
            onSwipeDownGesture = onMinimizeToMini,
            onPlayNext = onPlayNext,
            onPlayPrevious = onPlayPrevious,
            onFallbackProgress = onFallbackProgress,
            onFallbackError = onFallbackError,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )

        // Scrollable Details, Speed Controls, Chapters, and Up Next Queue
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (playbackState.errorMessage != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Format Notice",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = playbackState.errorMessage,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = onPlayNext) {
                                Text("Next")
                            }
                        }
                    }
                }
            }

            // Title & Folder Metadata
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${media.folderName} · ${media.resolutionLabel} · ${FormatUtils.formatFileSize(media.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Playback Speed & Fullscreen Controls
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onToggle2xSpeed,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("player_2x_speed_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (playbackState.playbackSpeed >= 1.95f) "2× Speed ON" else "2× Speed"
                            )
                        }

                        OutlinedButton(
                            onClick = onEnterFullscreen,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("player_enter_fullscreen_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fullscreen")
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = onClosePlayer,
                            modifier = Modifier.testTag("player_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close player"
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)) { spd ->
                            val isSelected = kotlin.math.abs(playbackState.playbackSpeed - spd) < 0.05f
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetPlaybackSpeed(spd) },
                                label = { Text("${spd}×") }
                            )
                        }
                    }
                }
            }

            // CHAPTERS & TIMESTAMPS SECTION
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (playbackState.chapters.isNotEmpty()) {
                                        Icons.Default.Bookmark
                                    } else {
                                        Icons.Default.BookmarkBorder
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (playbackState.chapters.isNotEmpty()) {
                                            "Chapters (${playbackState.chapters.size})"
                                        } else {
                                            "Chapters & Timestamps"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (playbackState.activeChapter != null) {
                                        Text(
                                            text = "Current: ${playbackState.activeChapter.title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { showAddChaptersDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("player_supply_chapters_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        if (playbackState.chapters.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(playbackState.chapters, key = { "${it.startTimeMs}_${it.title}" }) { chapter ->
                                    val isCurrent = playbackState.activeChapter?.startTimeMs == chapter.startTimeMs
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isCurrent) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        modifier = Modifier
                                            .width(185.dp)
                                            .clickable { onSeekTo(chapter.startTimeMs) }
                                            .testTag("chapter_chip_${chapter.startTimeMs}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Surface(
                                                    color = if (isCurrent) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        Color.Black.copy(alpha = 0.35f)
                                                    },
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = FormatUtils.formatDuration(chapter.startTimeMs),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (isCurrent) {
                                                            MaterialTheme.colorScheme.onPrimary
                                                        } else {
                                                            Color.White
                                                        },
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = chapter.title,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (isCurrent) {
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    },
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (!chapter.isAutoExtracted) {
                                                IconButton(
                                                    onClick = { onDeleteChapter(chapter) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete custom chapter",
                                                        modifier = Modifier.size(16.dp)
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
            }

            // UP NEXT LOCAL QUEUE
            if (playbackState.queue.size > 1) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Up Next (${playbackState.queue.size - 1})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                items(
                    items = playbackState.queue.filter { it.uri != media.uri }.take(15),
                    key = { "queue_${it.uri}" }
                ) { nextItem ->
                    UpNextQueueRow(
                        item = nextItem,
                        onClick = { onSelectQueueItem(nextItem) }
                    )
                }
            }
        }
    }

    if (showAddChaptersDialog) {
        SupplyChaptersDialog(
            currentPositionMs = playbackState.currentPositionMs,
            onDismiss = { showAddChaptersDialog = false },
            onAddCurrentTimestamp = { title ->
                onAddChapterAtCurrentTime(title)
                showAddChaptersDialog = false
            },
            onParseTimestampBlock = { block ->
                onAddChaptersBatch(block)
                showAddChaptersDialog = false
            }
        )
    }
}

@Composable
private fun FullscreenLandscapePlayerView(
    media: MediaItemEntity,
    playbackState: PlaybackUiState,
    exoPlayerProvider: () -> ExoPlayer,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onToggle2xSpeed: () -> Unit,
    onStartHold2xSpeed: () -> Unit,
    onStopHold2xSpeed: () -> Unit,
    onExitFullscreen: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onFallbackProgress: (Long, Long) -> Unit,
    onFallbackError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("fullscreen_landscape_player")
    ) {
        InteractiveVideoSurface(
            media = media,
            playbackState = playbackState,
            exoPlayerProvider = exoPlayerProvider,
            isFullscreen = true,
            onTogglePlayPause = onTogglePlayPause,
            onSeekTo = onSeekTo,
            onSeekRelative = onSeekRelative,
            onToggle2xSpeed = onToggle2xSpeed,
            onStartHold2xSpeed = onStartHold2xSpeed,
            onStopHold2xSpeed = onStopHold2xSpeed,
            onToggleFullscreen = onExitFullscreen,
            onMinimizePlayer = onExitFullscreen,
            onSwipeUpGesture = {},
            onSwipeDownGesture = onExitFullscreen,
            onPlayNext = onPlayNext,
            onPlayPrevious = onPlayPrevious,
            onFallbackProgress = onFallbackProgress,
            onFallbackError = onFallbackError,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun InteractiveVideoSurface(
    media: MediaItemEntity,
    playbackState: PlaybackUiState,
    exoPlayerProvider: () -> ExoPlayer,
    isFullscreen: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onToggle2xSpeed: () -> Unit,
    onStartHold2xSpeed: () -> Unit,
    onStopHold2xSpeed: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onMinimizePlayer: () -> Unit,
    onSwipeUpGesture: () -> Unit,
    onSwipeDownGesture: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onFallbackProgress: (Long, Long) -> Unit,
    onFallbackError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var seekOverlaySide by remember { mutableStateOf<Int?>(null) } // -1 for Left (-10s), +1 for Right (+10s)
    var cumulativeVerticalDrag by remember { mutableFloatStateOf(0f) }
    var fallbackVideoViewRef by remember { mutableStateOf<VideoView?>(null) }

    // Poll native VideoView progress if fallback player is active
    LaunchedEffect(playbackState.usePlatformFallbackPlayer, playbackState.isPlaying) {
        while (playbackState.usePlatformFallbackPlayer && playbackState.isPlaying) {
            val vv = fallbackVideoViewRef
            if (vv != null && vv.isPlaying) {
                onFallbackProgress(vv.currentPosition.toLong(), vv.duration.toLong())
            }
            delay(250L)
        }
    }

    // Auto-hide controls after 3.2 seconds while playing
    LaunchedEffect(controlsVisible, playbackState.isPlaying, playbackState.isHold2xActive) {
        if (controlsVisible && playbackState.isPlaying && !playbackState.isHold2xActive) {
            delay(3200L)
            controlsVisible = false
        }
    }

    LaunchedEffect(seekOverlaySide) {
        if (seekOverlaySide != null) {
            delay(650L)
            seekOverlaySide = null
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .testTag("video_surface_gesture_box")
            // 1. Tap, Double-Tap Left/Right (±10s), and YouTube's Press-and-Hold for 2× Speed!
            .pointerInput(media.uri) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                    },
                    onDoubleTap = { offset ->
                        val isLeftHalf = offset.x < size.width / 2f
                        if (isLeftHalf) {
                            seekOverlaySide = -1
                            onSeekRelative(-10_000L)
                            fallbackVideoViewRef?.let { vv ->
                                vv.seekTo((vv.currentPosition - 10_000).coerceAtLeast(0))
                            }
                        } else {
                            seekOverlaySide = 1
                            onSeekRelative(10_000L)
                            fallbackVideoViewRef?.let { vv ->
                                vv.seekTo((vv.currentPosition + 10_000).coerceAtMost(vv.duration.coerceAtLeast(1)))
                            }
                        }
                    },
                    onLongPress = {
                        // YouTube Hold-for-2x Speed activated!
                        controlsVisible = false
                        onStartHold2xSpeed()
                    },
                    onPress = {
                        try {
                            tryAwaitRelease()
                        } finally {
                            onStopHold2xSpeed()
                        }
                    }
                )
            }
            // 2. Vertical Swipe Gesture to enter Fullscreen (Swipe Up) or return to Embedded/Mini (Swipe Down)
            .pointerInput(isFullscreen) {
                detectVerticalDragGestures(
                    onDragStart = {
                        cumulativeVerticalDrag = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        cumulativeVerticalDrag += dragAmount
                    },
                    onDragEnd = {
                        val thresholdPx = 55.dp.toPx()
                        if (cumulativeVerticalDrag < -thresholdPx && !isFullscreen) {
                            onSwipeUpGesture()
                        } else if (cumulativeVerticalDrag > thresholdPx) {
                            onSwipeDownGesture()
                        }
                        cumulativeVerticalDrag = 0f
                    }
                )
            }
    ) {
        if (media.isAudio) {
            MediaArtworkCanvas(
                title = media.title,
                isAudio = true,
                modifier = Modifier.fillMaxSize()
            )
        } else if (playbackState.usePlatformFallbackPlayer) {
            // Native Android VideoView hardware fallback when a codec requires platform MediaPlayer
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setVideoURI(media.uri.toUri())
                        setOnPreparedListener { mp ->
                            mp.isLooping = playbackState.isLooping
                            if (playbackState.currentPositionMs > 0L) {
                                seekTo(playbackState.currentPositionMs.toInt())
                            }
                            if (playbackState.isPlaying) start()
                        }
                        setOnErrorListener { _, _, _ ->
                            onFallbackError()
                            true
                        }
                        fallbackVideoViewRef = this
                    }
                },
                update = { vv ->
                    fallbackVideoViewRef = vv
                    if (playbackState.isPlaying && !vv.isPlaying) {
                        runCatching { vv.start() }
                    } else if (!playbackState.isPlaying && vv.isPlaying) {
                        runCatching { vv.pause() }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        player = runCatching { exoPlayerProvider() }.getOrNull()
                    }
                },
                update = { playerView ->
                    val current = runCatching { exoPlayerProvider() }.getOrNull()
                    if (playerView.player != current) {
                        playerView.player = current
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // YouTube-Style Top-Center Floating Pill when Holding Finger on Video for 2× Speed
        AnimatedVisibility(
            visible = playbackState.isHold2xActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
        ) {
            Surface(
                color = Color(0xD90A0A0A),
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2× Speed",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Double-Tap Left (-10s) & Right (+10s) Visual Ripple Feedback
        AnimatedVisibility(
            visible = seekOverlaySide == -1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(0.42f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "-10 seconds",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = seekOverlaySide == 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.42f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.28f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Advance 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "+10 seconds",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Player Controls Overlay
        AnimatedVisibility(
            visible = (controlsVisible || !playbackState.isPlaying) && !playbackState.isHold2xActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.48f))
            ) {
                // Top Control Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onMinimizePlayer,
                        modifier = Modifier.testTag("player_minimize_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isFullscreen) "Exit fullscreen" else "Minimize player",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = media.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (playbackState.activeChapter != null) {
                            Text(
                                text = playbackState.activeChapter.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD6ECE6),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Quick 2x Speed Pill inside video overlay
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (playbackState.playbackSpeed >= 1.95f) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Black.copy(alpha = 0.6f)
                        },
                        contentColor = if (playbackState.playbackSpeed >= 1.95f) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            Color.White
                        },
                        modifier = Modifier
                            .clickable(onClick = onToggle2xSpeed)
                            .testTag("overlay_2x_speed_pill")
                    ) {
                        Text(
                            text = if (playbackState.playbackSpeed >= 1.95f) "2× ON" else "${playbackState.playbackSpeed}×",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Center Transport Controls: Prev, -10s, Play/Pause, +10s, Next
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPlayPrevious,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous track",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            seekOverlaySide = -1
                            onSeekRelative(-10_000L)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_rewind_10_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White,
                        modifier = Modifier
                            .size(62.dp)
                            .clickable(onClick = onTogglePlayPause)
                            .testTag("player_play_pause_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) {
                                    Icons.Default.Pause
                                } else {
                                    Icons.Default.PlayArrow
                                },
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            seekOverlaySide = 1
                            onSeekRelative(10_000L)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_forward_10_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = onPlayNext,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next track",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Bottom Timeline & Fullscreen Row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    if (isFullscreen && playbackState.chapters.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            items(playbackState.chapters) { ch ->
                                val active = playbackState.activeChapter?.startTimeMs == ch.startTimeMs
                                Surface(
                                    color = if (active) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Black.copy(alpha = 0.65f)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { onSeekTo(ch.startTimeMs) }
                                ) {
                                    Text(
                                        text = "${FormatUtils.formatDuration(ch.startTimeMs)} ${ch.title}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) MaterialTheme.colorScheme.onPrimary else Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    ChapterTimelineBar(
                        currentPositionMs = playbackState.currentPositionMs,
                        durationMs = playbackState.durationMs.coerceAtLeast(media.durationMs),
                        chapters = playbackState.chapters,
                        onSeekTo = { target ->
                            onSeekTo(target)
                            fallbackVideoViewRef?.seekTo(target.toInt())
                        },
                        playedColor = ProgressRed,
                        thumbColor = ProgressRed
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val totalDur = playbackState.durationMs.coerceAtLeast(media.durationMs)
                        val timeString = "${FormatUtils.formatDuration(playbackState.currentPositionMs)} / ${FormatUtils.formatDuration(totalDur)}"
                        val chapterSuffix = playbackState.activeChapter?.let { " · ${it.title}" } ?: ""
                        Text(
                            text = timeString + chapterSuffix,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("overlay_fullscreen_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) {
                                    Icons.Default.FullscreenExit
                                } else {
                                    Icons.Default.Fullscreen
                                },
                                contentDescription = if (isFullscreen) "Exit fullscreen" else "Enter fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    playbackState: PlaybackUiState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val media = playbackState.currentMedia ?: return
    val totalDur = playbackState.durationMs.coerceAtLeast(media.durationMs).coerceAtLeast(1L)
    val fraction = (playbackState.currentPositionMs.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onExpand)
            .testTag("mini_player_bar"),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                ) {
                    val hasThumb = media.thumbnailPath != null && File(media.thumbnailPath).exists()
                    if (hasThumb) {
                        AsyncImage(
                            model = File(media.thumbnailPath!!),
                            contentDescription = media.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        MediaArtworkCanvas(
                            title = media.title,
                            isAudio = media.isAudio,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sub = playbackState.activeChapter?.title ?: media.folderName
                    Text(
                        text = "$sub · ${FormatUtils.formatDuration(playbackState.currentPositionMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play"
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("mini_player_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close mini player"
                    )
                }
            }

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = ProgressRed
            )
        }
    }
}

@Composable
private fun UpNextQueueRow(
    item: MediaItemEntity,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 62.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                        color = Color(0xD9090D16),
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
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${item.folderName} · ${item.resolutionLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SupplyChaptersDialog(
    currentPositionMs: Long,
    onDismiss: () -> Unit,
    onAddCurrentTimestamp: (String) -> Unit,
    onParseTimestampBlock: (String) -> Unit
) {
    var singleTitle by remember { mutableStateOf("") }
    var multiLineText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Supply Video Chapters & Timestamps")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Bookmark current position (${FormatUtils.formatDuration(currentPositionMs)}) or paste YouTube-style timestamp lines:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = singleTitle,
                    onValueChange = { singleTitle = it },
                    label = {
                        Text("Chapter title at ${FormatUtils.formatDuration(currentPositionMs)}")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (singleTitle.isNotBlank()) {
                    Button(
                        onClick = { onAddCurrentTimestamp(singleTitle) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Chapter at ${FormatUtils.formatDuration(currentPositionMs)}")
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = multiLineText,
                    onValueChange = { multiLineText = it },
                    label = { Text("Or paste timestamp list (MM:SS Title)") },
                    placeholder = {
                        Text("00:00 Introduction\n01:15 Main Topic\n03:40 Conclusion")
                    },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (multiLineText.isNotBlank()) {
                        onParseTimestampBlock(multiLineText)
                    } else if (singleTitle.isNotBlank()) {
                        onAddCurrentTimestamp(singleTitle)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Save Chapters")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
