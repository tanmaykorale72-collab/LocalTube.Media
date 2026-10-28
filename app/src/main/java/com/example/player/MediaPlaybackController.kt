package com.example.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import com.example.data.local.ChapterEntity
import com.example.data.local.MediaItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PlayerDisplayMode {
    HIDDEN,
    MINI,
    EMBEDDED_HALF,
    FULLSCREEN_LANDSCAPE
}

data class PlaybackUiState(
    val currentMedia: MediaItemEntity? = null,
    val displayMode: PlayerDisplayMode = PlayerDisplayMode.HIDDEN,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isHold2xActive: Boolean = false,
    val isLooping: Boolean = false,
    val usePlatformFallbackPlayer: Boolean = false,
    val chapters: List<ChapterEntity> = emptyList(),
    val activeChapter: ChapterEntity? = null,
    val queue: List<MediaItemEntity> = emptyList(),
    val errorMessage: String? = null
)

class MediaPlaybackController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPersistProgress: (uri: String, positionMs: Long, incrementPlayCount: Boolean) -> Unit
) {
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer
        get() = _exoPlayer ?: createExoPlayer().also { _exoPlayer = it }

    private var progressPollingJob: Job? = null
    private var lastSavedTick = 0
    private var baseSpeedBeforeHold: Float = 1.0f

    @OptIn(UnstableApi::class)
    private fun createExoPlayer(): ExoPlayer {
        val appContext = context.applicationContext
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        // Enable software decoder fallback & flexible container extractors so real-device videos
        // with high-profile H.264/HEVC/MKV/AVI/MOV codecs play reliably without decoding errors
        val renderersFactory = DefaultRenderersFactory(appContext)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setConstantBitrateSeekingAlwaysEnabled(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(appContext, extractorsFactory)

        return ExoPlayer.Builder(appContext, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _state.update { it.copy(isPlaying = isPlaying, errorMessage = null) }
                        if (isPlaying) {
                            startProgressPolling()
                        } else {
                            persistCurrentProgress(incrementCount = false)
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val dur = duration.takeIf { it > 0 && it != C.TIME_UNSET }
                            ?: _state.value.currentMedia?.durationMs
                            ?: 0L
                        val pos = currentPosition.coerceAtLeast(0L)
                        _state.update { current ->
                            current.copy(
                                isBuffering = playbackState == Player.STATE_BUFFERING,
                                durationMs = dur,
                                currentPositionMs = pos,
                                activeChapter = findActiveChapter(current.chapters, pos)
                            )
                        }
                        if (playbackState == Player.STATE_ENDED && !_state.value.isLooping) {
                            persistCurrentProgress(incrementCount = false)
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        val currentMedia = _state.value.currentMedia
                        // If ExoPlayer encounters a codec/container issue on a video, seamlessly switch
                        // to Android's native platform VideoView / MediaPlayer pipeline
                        if (currentMedia != null && !currentMedia.isAudio && !_state.value.usePlatformFallbackPlayer) {
                            runCatching { stop() }
                            _state.update {
                                it.copy(
                                    usePlatformFallbackPlayer = true,
                                    isPlaying = true,
                                    isBuffering = false,
                                    errorMessage = null
                                )
                            }
                            return
                        }

                        val readableError = when (error.errorCode) {
                            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                            PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
                                "File missing or folder access expired. Rescan your folders in Settings."
                            else ->
                                "Unable to decode this file format on this device."
                        }
                        _state.update {
                            it.copy(
                                isPlaying = false,
                                isBuffering = false,
                                errorMessage = readableError
                            )
                        }
                    }
                })
            }
    }

    fun playMedia(
        item: MediaItemEntity,
        chapters: List<ChapterEntity>,
        queue: List<MediaItemEntity>,
        resumeSavedPosition: Boolean,
        targetMode: PlayerDisplayMode = PlayerDisplayMode.EMBEDDED_HALF,
        loop: Boolean = false
    ) {
        persistCurrentProgress(incrementCount = false)

        val startPos = if (resumeSavedPosition &&
            item.lastPlayedPositionMs > 2_000L &&
            (item.durationMs == 0L || item.lastPlayedPositionMs < item.durationMs - 3_000L)
        ) {
            item.lastPlayedPositionMs
        } else {
            0L
        }

        baseSpeedBeforeHold = 1.0f

        _state.update {
            it.copy(
                currentMedia = item,
                displayMode = targetMode,
                durationMs = item.durationMs,
                currentPositionMs = startPos,
                playbackSpeed = 1.0f,
                isHold2xActive = false,
                usePlatformFallbackPlayer = false,
                chapters = chapters,
                activeChapter = findActiveChapter(chapters, startPos),
                queue = queue,
                isLooping = loop,
                errorMessage = null
            )
        }

        try {
            val player = exoPlayer
            player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            player.playbackParameters = PlaybackParameters(1.0f)
            val mediaItem = MediaItem.fromUri(item.uri.toUri())
            player.setMediaItem(mediaItem, startPos)
            player.prepare()
            player.playWhenReady = true
            onPersistProgress(item.uri, startPos, true)
            startProgressPolling()
        } catch (e: Exception) {
            if (!item.isAudio) {
                _state.update {
                    it.copy(
                        usePlatformFallbackPlayer = true,
                        isPlaying = true,
                        errorMessage = null
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isPlaying = false,
                        errorMessage = "Unable to open audio file."
                    )
                }
            }
        }
    }

    fun onFallbackPlayerProgress(positionMs: Long, durationMs: Long) {
        _state.update { current ->
            val validDur = if (durationMs > 0L) durationMs else current.durationMs
            current.copy(
                currentPositionMs = positionMs.coerceAtLeast(0L),
                durationMs = validDur,
                activeChapter = findActiveChapter(current.chapters, positionMs)
            )
        }
    }

    fun onFallbackPlayerError() {
        _state.update {
            it.copy(
                isPlaying = false,
                errorMessage = "This video format is not supported by your device's hardware decoder."
            )
        }
    }

    fun updateChaptersForCurrentMedia(chapters: List<ChapterEntity>) {
        _state.update { current ->
            current.copy(
                chapters = chapters,
                activeChapter = findActiveChapter(chapters, current.currentPositionMs)
            )
        }
    }

    fun togglePlayPause() {
        val current = _state.value
        if (current.currentMedia == null) return
        if (current.usePlatformFallbackPlayer) {
            _state.update { it.copy(isPlaying = !it.isPlaying) }
            return
        }
        try {
            val player = exoPlayer
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
                player.playWhenReady = true
            } else if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        } catch (_: Exception) {
            _state.update { it.copy(isPlaying = !it.isPlaying) }
        }
    }

    fun pause() {
        runCatching { _exoPlayer?.pause() }
        _state.update { it.copy(isPlaying = false, isHold2xActive = false) }
        persistCurrentProgress(incrementCount = false)
    }

    fun seekTo(positionMs: Long) {
        val dur = _state.value.durationMs.coerceAtLeast(0L)
        val clamped = if (dur > 0L) positionMs.coerceIn(0L, dur) else positionMs.coerceAtLeast(0L)
        if (!_state.value.usePlatformFallbackPlayer) {
            runCatching { _exoPlayer?.seekTo(clamped) }
        }
        _state.update { current ->
            current.copy(
                currentPositionMs = clamped,
                activeChapter = findActiveChapter(current.chapters, clamped)
            )
        }
        persistCurrentProgress(incrementCount = false)
    }

    fun seekRelative(deltaMs: Long) {
        val currentPos = if (_state.value.usePlatformFallbackPlayer) {
            _state.value.currentPositionMs
        } else {
            runCatching { _exoPlayer?.currentPosition }.getOrNull() ?: _state.value.currentPositionMs
        }
        seekTo(currentPos + deltaMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        val validSpeed = speed.coerceIn(0.25f, 3.0f)
        baseSpeedBeforeHold = validSpeed
        runCatching {
            _exoPlayer?.playbackParameters = PlaybackParameters(validSpeed)
        }
        _state.update { it.copy(playbackSpeed = validSpeed, isHold2xActive = false) }
    }

    fun toggle2xSpeed() {
        val nextSpeed = if (_state.value.playbackSpeed >= 1.95f) 1.0f else 2.0f
        setPlaybackSpeed(nextSpeed)
    }

    /**
     * YouTube's Press-and-Hold 2x Speed gesture:
     * Activates 2x speed while finger is held down on the video, and restores prior speed when released.
     */
    fun startHold2xSpeed() {
        if (_state.value.isHold2xActive) return
        baseSpeedBeforeHold = _state.value.playbackSpeed
        runCatching {
            _exoPlayer?.playbackParameters = PlaybackParameters(2.0f)
        }
        _state.update { it.copy(playbackSpeed = 2.0f, isHold2xActive = true) }
    }

    fun stopHold2xSpeed() {
        if (!_state.value.isHold2xActive) return
        val restoreSpeed = baseSpeedBeforeHold.coerceIn(0.25f, 3.0f)
        runCatching {
            _exoPlayer?.playbackParameters = PlaybackParameters(restoreSpeed)
        }
        _state.update { it.copy(playbackSpeed = restoreSpeed, isHold2xActive = false) }
    }

    fun setDisplayMode(mode: PlayerDisplayMode) {
        if (mode == PlayerDisplayMode.HIDDEN) {
            closePlayer()
        } else {
            _state.update { it.copy(displayMode = mode) }
        }
    }

    fun closePlayer() {
        persistCurrentProgress(incrementCount = false)
        progressPollingJob?.cancel()
        runCatching { _exoPlayer?.stop() }
        _state.update {
            PlaybackUiState(playbackSpeed = 1.0f)
        }
    }

    fun release() {
        persistCurrentProgress(incrementCount = false)
        progressPollingJob?.cancel()
        runCatching { _exoPlayer?.release() }
        _exoPlayer = null
    }

    private fun startProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = scope.launch {
            while (isActive) {
                val player = _exoPlayer
                if (!_state.value.usePlatformFallbackPlayer && player != null && player.isPlaying) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.takeIf { it > 0 && it != C.TIME_UNSET }
                        ?: _state.value.durationMs
                    _state.update { current ->
                        current.copy(
                            currentPositionMs = pos,
                            durationMs = dur,
                            activeChapter = findActiveChapter(current.chapters, pos)
                        )
                    }
                    lastSavedTick++
                    if (lastSavedTick >= 10) {
                        lastSavedTick = 0
                        persistCurrentProgress(incrementCount = false)
                    }
                }
                delay(200L)
            }
        }
    }

    private fun persistCurrentProgress(incrementCount: Boolean) {
        val current = _state.value.currentMedia ?: return
        val pos = _state.value.currentPositionMs
        onPersistProgress(current.uri, pos, incrementCount)
    }

    companion object {
        fun findActiveChapter(chapters: List<ChapterEntity>, positionMs: Long): ChapterEntity? {
            if (chapters.isEmpty()) return null
            return chapters.lastOrNull { it.startTimeMs <= positionMs } ?: chapters.firstOrNull()
        }
    }
}
