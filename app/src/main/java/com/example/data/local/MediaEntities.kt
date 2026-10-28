package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_folders")
data class ScannedFolderEntity(
    @PrimaryKey val treeUri: String,
    val displayName: String,
    val isAccessValid: Boolean = true,
    val mediaCount: Int = 0,
    val videoCount: Int = 0,
    val audioCount: Int = 0,
    val lastScannedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["folderTreeUri"]),
        Index(value = ["isAudio"]),
        Index(value = ["isShortCandidate"])
    ]
)
data class MediaItemEntity(
    @PrimaryKey val uri: String,
    val folderTreeUri: String,
    val folderName: String,
    val title: String,
    val fileName: String,
    val mimeType: String,
    val isAudio: Boolean,
    val isShortCandidate: Boolean,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val resolutionLabel: String,
    val artistOrAlbum: String?,
    val thumbnailPath: String?,
    val lastModifiedMs: Long,
    val lastPlayedPositionMs: Long = 0L,
    val lastPlayedAtMs: Long = 0L,
    val playCount: Int = 0
)

@Entity(
    tableName = "media_chapters",
    indices = [Index(value = ["mediaUri"])]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val mediaUri: String,
    val startTimeMs: Long,
    val title: String,
    val isAutoExtracted: Boolean = true
)
