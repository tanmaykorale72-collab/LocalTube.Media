package com.example.ui.screens

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.local.MediaItemEntity
import com.example.ui.components.ChapterTimelineBar
import com.example.ui.theme.ProgressRed
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun ShortsScreen(
    shortsItems: List<MediaItemEntity>,
    onOpenInMainPlayer: (MediaItemEntity) -> Unit,
    onNavigateToSettings: () -> Unit,
    onPauseBackgroundPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (shortsItems.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .testTag("shorts_empty_state"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Short-Form Videos Found",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Short videos and vertical clips from your folders appear here automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateToSettings,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("shorts_open_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Settings")
                    }
                }
            }
        }
        return
    }

    LaunchedEffect(Unit) {
        onPauseBackgroundPlayer()
    }

    val pagerState = rememberPagerState(pageCount = { shortsItems.size })

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("shorts_pager_container")
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { pageIndex ->
            val shortItem = shortsItems[pageIndex]
            val isCurrentPage = pagerState.settledPage == pageIndex

            ShortVideoPage(
                item = shortItem,
                isActive = isCurrentPage,
                pageNumber = pageIndex + 1,
                totalCount = shortsItems.size,
                onExpandToFullPlayer = { onOpenInMainPlayer(shortItem) }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ShortVideoPage(
    item: MediaItemEntity,
    isActive: Boolean,
    pageNumber: Int,
    totalCount: Int,
    onExpandToFullPlayer: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isHold2xActive by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(item.durationMs.coerceAtLeast(1L)) }
    var seekRippleDirection by remember { mutableStateOf<Int?>(null) }

    val shortExoPlayer = remember(context) {
        val renderersFactory = DefaultRenderersFactory(context.applicationContext)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        ExoPlayer.Builder(context.applicationContext, renderersFactory).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(shortExoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val dur = shortExoPlayer.duration
                if (dur > 0 && dur != C.TIME_UNSET) {
                    durationMs = dur
                }
            }
        }
        shortExoPlayer.addListener(listener)
        onDispose {
            shortExoPlayer.removeListener(listener)
            runCatching { shortExoPlayer.release() }
        }
    }

    LaunchedEffect(item.uri, isActive) {
        if (isActive) {
            runCatching {
                shortExoPlayer.setMediaItem(MediaItem.fromUri(item.uri.toUri()))
                shortExoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
                shortExoPlayer.prepare()
                shortExoPlayer.playWhenReady = true
            }
        } else {
            runCatching {
                shortExoPlayer.pause()
            }
        }
    }

    LaunchedEffect(isActive, isPlaying) {
        while (isActive && isPlaying) {
            currentPosMs = runCatching { shortExoPlayer.currentPosition.coerceAtLeast(0L) }.getOrDefault(0L)
            delay(200L)
        }
    }

    LaunchedEffect(seekRippleDirection) {
        if (seekRippleDirection != null) {
            delay(650L)
            seekRippleDirection = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(item.uri) {
                detectTapGestures(
                    onTap = {
                        runCatching {
                            if (shortExoPlayer.isPlaying) {
                                shortExoPlayer.pause()
                            } else {
                                shortExoPlayer.play()
                            }
                        }
                    },
                    onDoubleTap = { offset ->
                        val isLeft = offset.x < size.width / 2f
                        val delta = if (isLeft) -10_000L else 10_000L
                        seekRippleDirection = if (isLeft) -1 else 1
                        runCatching {
                            val target = (shortExoPlayer.currentPosition + delta)
                                .coerceIn(0L, durationMs.coerceAtLeast(1L))
                            shortExoPlayer.seekTo(target)
                            currentPosMs = target
                        }
                    },
                    onLongPress = {
                        isHold2xActive = true
                        runCatching {
                            shortExoPlayer.playbackParameters = PlaybackParameters(2.0f)
                        }
                    },
                    onPress = {
                        try {
                            tryAwaitRelease()
                        } finally {
                            if (isHold2xActive) {
                                isHold2xActive = false
                                runCatching {
                                    shortExoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
                                }
                            }
                        }
                    }
                )
            }
    ) {
        if (item.thumbnailPath != null && File(item.thumbnailPath).exists()) {
            AsyncImage(
                model = File(item.thumbnailPath),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isActive) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = if (item.height >= item.width && item.width > 0) {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        player = shortExoPlayer
                    }
                },
                update = { view ->
                    view.player = shortExoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.80f)
                        )
                    )
                )
        )

        // Top Header & Hold-for-2x Speed Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shorts",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$pageNumber / $totalCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            if (isHold2xActive || playbackSpeed > 1.05f) {
                Surface(
                    color = Color(0xD90A0A0A),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2× Speed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = (!isPlaying || seekRippleDirection != null) && !isHold2xActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                contentColor = Color.White,
                modifier = Modifier.size(74.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (seekRippleDirection) {
                        -1 -> {
                            Icon(Icons.Default.FastRewind, contentDescription = "Rewind 10 seconds")
                            Text("-10s", style = MaterialTheme.typography.labelMedium)
                        }
                        1 -> {
                            Icon(Icons.Default.FastForward, contentDescription = "Advance 10 seconds")
                            Text("+10s", style = MaterialTheme.typography.labelMedium)
                        }
                        else -> {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play or pause",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }

        // Right-side Shorts Action Rail
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 92.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = if (playbackSpeed > 1.5f) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Black.copy(alpha = 0.55f)
                    },
                    contentColor = Color.White,
                    modifier = Modifier.size(50.dp)
                ) {
                    IconButton(
                        onClick = {
                            val next = if (playbackSpeed >= 1.9f) 1.0f else 2.0f
                            playbackSpeed = next
                            runCatching {
                                shortExoPlayer.playbackParameters = PlaybackParameters(next)
                            }
                        },
                        modifier = Modifier.testTag("shorts_2x_speed_button")
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = "Toggle 2x speed")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (playbackSpeed >= 1.9f) "2× ON" else "2×",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White,
                    modifier = Modifier.size(50.dp)
                ) {
                    IconButton(
                        onClick = {
                            runCatching { shortExoPlayer.pause() }
                            onExpandToFullPlayer()
                        },
                        modifier = Modifier.testTag("shorts_expand_player_button")
                    ) {
                        Icon(Icons.Default.OpenInFull, contentDescription = "Open in full player")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Player",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        // Bottom Metadata & Scrubber Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 68.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.folderName,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 68.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            ChapterTimelineBar(
                currentPositionMs = currentPosMs,
                durationMs = durationMs,
                chapters = emptyList(),
                onSeekTo = { targetMs ->
                    runCatching { shortExoPlayer.seekTo(targetMs) }
                    currentPosMs = targetMs
                },
                playedColor = ProgressRed,
                thumbColor = ProgressRed
            )
        }
    }
}
