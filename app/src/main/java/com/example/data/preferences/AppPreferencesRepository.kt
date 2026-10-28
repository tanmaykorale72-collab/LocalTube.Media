package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cineflow_preferences")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class SortOrder(val label: String) {
    RECENTLY_MODIFIED("Newest First"),
    TITLE_ASC("Title (A–Z)"),
    DURATION_DESC("Longest Duration"),
    SIZE_DESC("Largest File Size"),
    RECENTLY_PLAYED("Recently Watched")
}

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val pauseWatchHistory: Boolean = false,
    val scanNestedSubfolders: Boolean = true,
    val includeAudioFiles: Boolean = true,
    val shortsMaxDurationSec: Int = 90,
    val includeHorizontalShorts: Boolean = true,
    val autoPlayNext: Boolean = true,
    val resumePlaybackPosition: Boolean = true,
    val sortOrder: SortOrder = SortOrder.RECENTLY_MODIFIED
)

class AppPreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PAUSE_WATCH_HISTORY = booleanPreferencesKey("pause_watch_history")
        val SCAN_NESTED = booleanPreferencesKey("scan_nested_subfolders")
        val INCLUDE_AUDIO = booleanPreferencesKey("include_audio_files")
        val SHORTS_MAX_SEC = intPreferencesKey("shorts_max_duration_sec")
        val INCLUDE_HORIZONTAL_SHORTS = booleanPreferencesKey("include_horizontal_shorts")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val RESUME_POSITION = booleanPreferencesKey("resume_playback_position")
        val SORT_ORDER = stringPreferencesKey("sort_order")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name
        val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.DARK)

        val sortStr = prefs[Keys.SORT_ORDER] ?: SortOrder.RECENTLY_MODIFIED.name
        val sortOrder = runCatching { SortOrder.valueOf(sortStr) }.getOrDefault(SortOrder.RECENTLY_MODIFIED)

        AppPreferences(
            themeMode = themeMode,
            pauseWatchHistory = prefs[Keys.PAUSE_WATCH_HISTORY] ?: false,
            scanNestedSubfolders = prefs[Keys.SCAN_NESTED] ?: true,
            includeAudioFiles = prefs[Keys.INCLUDE_AUDIO] ?: true,
            shortsMaxDurationSec = prefs[Keys.SHORTS_MAX_SEC] ?: 90,
            includeHorizontalShorts = prefs[Keys.INCLUDE_HORIZONTAL_SHORTS] ?: true,
            autoPlayNext = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
            resumePlaybackPosition = prefs[Keys.RESUME_POSITION] ?: true,
            sortOrder = sortOrder
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setPauseWatchHistory(paused: Boolean) {
        context.dataStore.edit { it[Keys.PAUSE_WATCH_HISTORY] = paused }
    }

    suspend fun setScanNestedSubfolders(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SCAN_NESTED] = enabled }
    }

    suspend fun setIncludeAudioFiles(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INCLUDE_AUDIO] = enabled }
    }

    suspend fun setShortsMaxDurationSec(seconds: Int) {
        context.dataStore.edit { it[Keys.SHORTS_MAX_SEC] = seconds.coerceIn(15, 300) }
    }

    suspend fun setIncludeHorizontalShorts(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INCLUDE_HORIZONTAL_SHORTS] = enabled }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun setResumePlaybackPosition(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_POSITION] = enabled }
    }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { it[Keys.SORT_ORDER] = order.name }
    }
}
