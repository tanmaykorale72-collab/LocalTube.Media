package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM scanned_folders ORDER BY lastScannedAtMs DESC")
    fun observeScannedFolders(): Flow<List<ScannedFolderEntity>>

    @Query("SELECT * FROM scanned_folders")
    suspend fun getScannedFoldersOnce(): List<ScannedFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScannedFolder(folder: ScannedFolderEntity)

    @Query("DELETE FROM scanned_folders WHERE treeUri = :treeUri")
    suspend fun deleteScannedFolder(treeUri: String)

    @Query("DELETE FROM scanned_folders")
    suspend fun clearAllScannedFolders()

    @Query("SELECT * FROM media_items ORDER BY lastModifiedMs DESC, title ASC")
    fun observeAllMediaItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE lastPlayedAtMs > 0 ORDER BY lastPlayedAtMs DESC")
    fun observeWatchHistory(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE folderTreeUri = :treeUri")
    suspend fun getMediaItemsForFolder(treeUri: String): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE uri = :uri LIMIT 1")
    suspend fun getMediaItemByUri(uri: String): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMediaItems(items: List<MediaItemEntity>)

    @Query("DELETE FROM media_items WHERE folderTreeUri = :treeUri")
    suspend fun deleteMediaItemsForFolder(treeUri: String)

    @Query("DELETE FROM media_items WHERE uri IN (:uris)")
    suspend fun deleteMediaItemsByUris(uris: List<String>)

    @Query("DELETE FROM media_items")
    suspend fun clearAllMediaItems()

    @Query("UPDATE media_items SET lastPlayedPositionMs = 0, lastPlayedAtMs = 0, playCount = 0")
    suspend fun clearAllWatchHistory()

    @Query("UPDATE media_items SET lastPlayedPositionMs = 0, lastPlayedAtMs = 0 WHERE uri = :uri")
    suspend fun removeSingleItemFromHistory(uri: String)

    @Query(
        """
        UPDATE media_items 
        SET lastPlayedPositionMs = :positionMs, 
            lastPlayedAtMs = :playedAtMs,
            playCount = playCount + :incrementCount
        WHERE uri = :uri
        """
    )
    suspend fun updatePlaybackProgress(
        uri: String,
        positionMs: Long,
        playedAtMs: Long = System.currentTimeMillis(),
        incrementCount: Int = 0
    )

    @Query("SELECT * FROM media_chapters ORDER BY startTimeMs ASC")
    fun observeAllChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM media_chapters WHERE mediaUri = :mediaUri ORDER BY startTimeMs ASC")
    fun observeChaptersForMedia(mediaUri: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM media_chapters WHERE mediaUri = :mediaUri ORDER BY startTimeMs ASC")
    suspend fun getChaptersForMediaOnce(mediaUri: String): List<ChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)

    @Query("DELETE FROM media_chapters WHERE mediaUri = :mediaUri AND isAutoExtracted = 1")
    suspend fun deleteAutoChaptersForMedia(mediaUri: String)

    @Query("DELETE FROM media_chapters WHERE id = :chapterId")
    suspend fun deleteChapterById(chapterId: Long)

    @Query("DELETE FROM media_chapters WHERE mediaUri IN (:uris)")
    suspend fun deleteChaptersForMediaUris(uris: List<String>)

    @Query("DELETE FROM media_chapters")
    suspend fun clearAllChapters()
}

@Database(
    entities = [
        ScannedFolderEntity::class,
        MediaItemEntity::class,
        ChapterEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CineFlowDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: CineFlowDatabase? = null

        fun getInstance(context: Context): CineFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CineFlowDatabase::class.java,
                    "cineflow_offline_media.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
