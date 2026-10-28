package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChapterEntity
import com.example.data.local.CineFlowDatabase
import com.example.data.local.MediaItemEntity
import com.example.data.local.ScannedFolderEntity
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.preferences.SortOrder
import com.example.data.preferences.ThemeMode
import com.example.data.scanner.MediaScannerService
import com.example.data.scanner.ScanProgress
import com.example.player.MediaPlaybackController
import com.example.player.PlayerDisplayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PrimaryDestination {
    HOME,
    SHORTS,
    SETTINGS
}

enum class HomeFeedFilter(val label: String) {
    ALL("All"),
    VIDEOS("Videos"),
    MOVIES_LONG("Movies & Long"),
    AUDIO("Audio"),
    WITH_CHAPTERS("With Chapters"),
    CONTINUE_WATCHING("Continue Watching")
}

data class HomeUiState(
    val allItems: List<MediaItemEntity> = emptyList(),
    val filteredItems: List<MediaItemEntity> = emptyList(),
    val continueWatchingItems: List<MediaItemEntity> = emptyList(),
    val watchHistoryItems: List<MediaItemEntity> = emptyList(),
    val shortsItems: List<MediaItemEntity> = emptyList(),
    val chaptersByMediaUri: Map<String, List<ChapterEntity>> = emptyMap(),
    val scannedFolders: List<ScannedFolderEntity> = emptyList(),
    val hasRevokedFolders: Boolean = false,
    val selectedFilter: HomeFeedFilter = HomeFeedFilter.ALL,
    val selectedFolderFilter: String? = null,
    val searchQuery: String = "",
    val preferences: AppPreferences = AppPreferences(),
    val scanProgress: ScanProgress = ScanProgress(),
    val statusBannerMessage: String? = null
)

class CineFlowViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CineFlowDatabase.getInstance(application)
    private val mediaDao = database.mediaDao()
    private val preferencesRepo = AppPreferencesRepository(application)
    private val scannerService = MediaScannerService(application, mediaDao)

    private val _activeTab = MutableStateFlow(PrimaryDestination.HOME)
    val activeTab: StateFlow<PrimaryDestination> = _activeTab.asStateFlow()

    private val _selectedFilter = MutableStateFlow(HomeFeedFilter.ALL)
    private val _selectedFolderFilter = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _scanProgress = MutableStateFlow(ScanProgress())
    private val _statusBanner = MutableStateFlow<String?>(null)

    val playbackController = MediaPlaybackController(
        context = application,
        scope = viewModelScope,
        onPersistProgress = { uri, posMs, incrementCount ->
            viewModelScope.launch {
                val prefs = preferencesRepo.preferencesFlow.first()
                // Respect "Pause Watch History" setting
                if (!prefs.pauseWatchHistory) {
                    mediaDao.updatePlaybackProgress(
                        uri = uri,
                        positionMs = posMs,
                        incrementCount = if (incrementCount) 1 else 0
                    )
                }
            }
        }
    )

    val playbackState = playbackController.state

    val uiState: StateFlow<HomeUiState> = combine(
        mediaDao.observeAllMediaItems(),
        mediaDao.observeAllChapters(),
        mediaDao.observeScannedFolders(),
        preferencesRepo.preferencesFlow,
        combine(
            _selectedFilter,
            _selectedFolderFilter,
            _searchQuery,
            _scanProgress,
            _statusBanner
        ) { filter, folderFilter, query, progress, banner ->
            FilterAndStatusBundle(filter, folderFilter, query, progress, banner)
        }
    ) { items, allChapters, folders, prefs, bundle ->
        val chaptersMap = allChapters.groupBy { it.mediaUri }

        val visibleItems = if (prefs.includeAudioFiles) {
            items
        } else {
            items.filter { !it.isAudio }
        }

        val sortedItems = when (prefs.sortOrder) {
            SortOrder.RECENTLY_MODIFIED -> visibleItems.sortedByDescending { it.lastModifiedMs }
            SortOrder.TITLE_ASC -> visibleItems.sortedBy { it.title.lowercase() }
            SortOrder.DURATION_DESC -> visibleItems.sortedByDescending { it.durationMs }
            SortOrder.SIZE_DESC -> visibleItems.sortedByDescending { it.sizeBytes }
            SortOrder.RECENTLY_PLAYED -> visibleItems.sortedByDescending { it.lastPlayedAtMs }
        }

        val filtered = sortedItems.filter { item ->
            val matchesFolder = bundle.folderFilter == null || item.folderName == bundle.folderFilter
            val matchesQuery = bundle.query.isBlank() ||
                item.title.contains(bundle.query, ignoreCase = true) ||
                item.folderName.contains(bundle.query, ignoreCase = true) ||
                (item.artistOrAlbum?.contains(bundle.query, ignoreCase = true) == true) ||
                (chaptersMap[item.uri]?.any { ch -> ch.title.contains(bundle.query, ignoreCase = true) } == true)

            val matchesCategory = when (bundle.filter) {
                HomeFeedFilter.ALL -> true
                HomeFeedFilter.VIDEOS -> !item.isAudio
                HomeFeedFilter.MOVIES_LONG -> !item.isAudio && item.durationMs >= 10_000L && !item.isShortCandidate
                HomeFeedFilter.AUDIO -> item.isAudio
                HomeFeedFilter.WITH_CHAPTERS -> !chaptersMap[item.uri].isNullOrEmpty()
                HomeFeedFilter.CONTINUE_WATCHING -> item.lastPlayedPositionMs > 2_000L
            }

            matchesFolder && matchesQuery && matchesCategory
        }

        val continueWatching = visibleItems
            .filter { it.lastPlayedPositionMs > 2_000L && (it.durationMs == 0L || it.lastPlayedPositionMs < it.durationMs - 2_000L) }
            .sortedByDescending { it.lastPlayedAtMs }

        val watchHistory = visibleItems
            .filter { it.lastPlayedAtMs > 0L }
            .sortedByDescending { it.lastPlayedAtMs }

        val shortsList = visibleItems.filter { item ->
            !item.isAudio && MediaScannerService.isShortVideoCandidate(
                durationMs = item.durationMs,
                width = item.width,
                height = item.height,
                maxShortSec = prefs.shortsMaxDurationSec,
                includeHorizontalShorts = prefs.includeHorizontalShorts
            )
        }

        HomeUiState(
            allItems = sortedItems,
            filteredItems = filtered,
            continueWatchingItems = continueWatching,
            watchHistoryItems = watchHistory,
            shortsItems = shortsList,
            chaptersByMediaUri = chaptersMap,
            scannedFolders = folders,
            hasRevokedFolders = folders.any { !it.isAccessValid },
            selectedFilter = bundle.filter,
            selectedFolderFilter = bundle.folderFilter,
            searchQuery = bundle.query,
            preferences = prefs,
            scanProgress = bundle.progress,
            statusBannerMessage = bundle.banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        verifyAndRefreshFoldersOnStartup()
    }

    private fun verifyAndRefreshFoldersOnStartup() {
        viewModelScope.launch {
            val folders = mediaDao.getScannedFoldersOnce()
            if (folders.isEmpty()) return@launch
            val prefs = preferencesRepo.preferencesFlow.first()
            for (folder in folders) {
                val valid = scannerService.isFolderAccessValid(folder.treeUri)
                if (!valid) {
                    mediaDao.upsertScannedFolder(folder.copy(isAccessValid = false, mediaCount = 0))
                    mediaDao.deleteMediaItemsForFolder(folder.treeUri)
                    _statusBanner.value = "Folder access to '${folder.displayName}' was revoked by Android. Please re-select the folder in Settings."
                } else {
                    scannerService.scanFolder(folder.treeUri, prefs) { progress ->
                        _scanProgress.value = progress
                    }
                }
            }
            _scanProgress.value = ScanProgress(isScanning = false)
        }
    }

    fun selectTab(tab: PrimaryDestination) {
        _activeTab.value = tab
        if (tab == PrimaryDestination.SHORTS &&
            playbackState.value.displayMode == PlayerDisplayMode.EMBEDDED_HALF
        ) {
            playbackController.setDisplayMode(PlayerDisplayMode.MINI)
        }
    }

    fun setFilter(filter: HomeFeedFilter) {
        _selectedFilter.value = filter
    }

    fun setFolderFilter(folderName: String?) {
        _selectedFolderFilter.update { current ->
            if (current == folderName) null else folderName
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun onFolderSelectedFromPicker(treeUri: Uri) {
        viewModelScope.launch {
            scannerService.takePersistableFolderPermission(treeUri)
            val prefs = preferencesRepo.preferencesFlow.first()
            val result = scannerService.scanFolder(treeUri.toString(), prefs) { progress ->
                _scanProgress.value = progress
            }
            result.onSuccess { folder ->
                _statusBanner.value = "Added '${folder.displayName}' (${folder.videoCount} videos, ${folder.audioCount} audio)."
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Could not scan the selected folder."
            }
        }
    }

    fun rescanAllFolders() {
        viewModelScope.launch {
            val prefs = preferencesRepo.preferencesFlow.first()
            scannerService.rescanAllFolders(prefs) { progress ->
                _scanProgress.value = progress
            }
            _statusBanner.value = "Home feed refreshed."
        }
    }

    fun rescanSingleFolder(treeUri: String) {
        viewModelScope.launch {
            val prefs = preferencesRepo.preferencesFlow.first()
            val result = scannerService.scanFolder(treeUri, prefs) { progress ->
                _scanProgress.value = progress
            }
            result.onFailure { err ->
                _statusBanner.value = err.localizedMessage
            }
        }
    }

    fun removeFolder(treeUri: String) {
        viewModelScope.launch {
            if (playbackState.value.currentMedia?.folderTreeUri == treeUri) {
                playbackController.closePlayer()
            }
            mediaDao.deleteMediaItemsForFolder(treeUri)
            mediaDao.deleteScannedFolder(treeUri)
            _statusBanner.value = "Folder removed from library."
        }
    }

    /**
     * Clears Watch History & Continue Watching progress without removing the media library.
     */
    fun clearWatchHistory() {
        viewModelScope.launch {
            mediaDao.clearAllWatchHistory()
            _statusBanner.value = "Watch history and Continue Watching cleared."
        }
    }

    fun removeSingleItemFromHistory(uri: String) {
        viewModelScope.launch {
            mediaDao.removeSingleItemFromHistory(uri)
        }
    }

    /**
     * Clears the entire Home feed & resets authorized folders so the user can start fresh.
     */
    fun clearHomeFeed() {
        viewModelScope.launch {
            playbackController.closePlayer()
            mediaDao.clearAllMediaItems()
            mediaDao.clearAllChapters()
            mediaDao.clearAllScannedFolders()
            _selectedFolderFilter.value = null
            _selectedFilter.value = HomeFeedFilter.ALL
            _statusBanner.value = "Home feed cleared."
        }
    }

    fun setPauseWatchHistory(paused: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setPauseWatchHistory(paused)
            _statusBanner.value = if (paused) {
                "Watch history paused."
            } else {
                "Watch history resumed."
            }
        }
    }

    fun openMediaInPlayer(
        item: MediaItemEntity,
        targetMode: PlayerDisplayMode = PlayerDisplayMode.EMBEDDED_HALF,
        loop: Boolean = false
    ) {
        val currentState = uiState.value
        val chapters = currentState.chaptersByMediaUri[item.uri].orEmpty()
        val queue = currentState.filteredItems.ifEmpty { currentState.allItems }
        playbackController.playMedia(
            item = item,
            chapters = chapters,
            queue = queue,
            resumeSavedPosition = currentState.preferences.resumePlaybackPosition && !loop,
            targetMode = targetMode,
            loop = loop
        )
    }

    fun playNextInQueue() {
        val pState = playbackState.value
        val current = pState.currentMedia ?: return
        val queue = pState.queue
        if (queue.isEmpty()) return
        val idx = queue.indexOfFirst { it.uri == current.uri }
        val nextItem = if (idx in 0 until queue.lastIndex) queue[idx + 1] else queue.first()
        openMediaInPlayer(nextItem, targetMode = pState.displayMode)
    }

    fun playPreviousInQueue() {
        val pState = playbackState.value
        val current = pState.currentMedia ?: return
        if (pState.currentPositionMs > 4_000L) {
            playbackController.seekTo(0L)
            return
        }
        val queue = pState.queue
        if (queue.isEmpty()) return
        val idx = queue.indexOfFirst { it.uri == current.uri }
        val prevItem = if (idx > 0) queue[idx - 1] else queue.last()
        openMediaInPlayer(prevItem, targetMode = pState.displayMode)
    }

    fun addChaptersFromText(mediaUri: String, rawTimestamps: String, durationMs: Long) {
        viewModelScope.launch {
            val parsed = MediaScannerService.parseTimestampLines(
                mediaUri = mediaUri,
                rawText = rawTimestamps,
                durationMs = durationMs,
                isAuto = false
            )
            if (parsed.isNotEmpty()) {
                mediaDao.insertChapters(parsed)
                val updated = mediaDao.getChaptersForMediaOnce(mediaUri)
                if (playbackState.value.currentMedia?.uri == mediaUri) {
                    playbackController.updateChaptersForCurrentMedia(updated)
                }
            }
        }
    }

    fun addSingleChapterAtCurrentPosition(mediaUri: String, startTimeMs: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            mediaDao.insertChapter(
                ChapterEntity(
                    mediaUri = mediaUri,
                    startTimeMs = startTimeMs.coerceAtLeast(0L),
                    title = title.trim(),
                    isAutoExtracted = false
                )
            )
            val updated = mediaDao.getChaptersForMediaOnce(mediaUri)
            if (playbackState.value.currentMedia?.uri == mediaUri) {
                playbackController.updateChaptersForCurrentMedia(updated)
            }
        }
    }

    fun deleteChapter(chapter: ChapterEntity) {
        viewModelScope.launch {
            mediaDao.deleteChapterById(chapter.id)
            val updated = mediaDao.getChaptersForMediaOnce(chapter.mediaUri)
            if (playbackState.value.currentMedia?.uri == chapter.mediaUri) {
                playbackController.updateChaptersForCurrentMedia(updated)
            }
        }
    }

    // Settings updates
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepo.setThemeMode(mode) }
    }

    fun setScanNestedSubfolders(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setScanNestedSubfolders(enabled)
            rescanAllFolders()
        }
    }

    fun setIncludeAudioFiles(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setIncludeAudioFiles(enabled)
            if (enabled) rescanAllFolders()
        }
    }

    fun setResumePlaybackPosition(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.setResumePlaybackPosition(enabled) }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { preferencesRepo.setSortOrder(order) }
    }

    override fun onCleared() {
        super.onCleared()
        playbackController.release()
    }

    private data class FilterAndStatusBundle(
        val filter: HomeFeedFilter,
        val folderFilter: String?,
        val query: String,
        val progress: ScanProgress,
        val banner: String?
    )
}
