package com.example.data.scanner

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.ChapterEntity
import com.example.data.local.MediaDao
import com.example.data.local.MediaItemEntity
import com.example.data.local.ScannedFolderEntity
import com.example.data.preferences.AppPreferences
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScanProgress(
    val isScanning: Boolean = false,
    val currentFolder: String = "",
    val scannedFilesCount: Int = 0,
    val errorMessage: String? = null
)

class MediaScannerService(
    private val context: Context,
    private val mediaDao: MediaDao
) {
    private val videoExtensions = setOf(
        "mp4", "mkv", "webm", "mov", "avi", "3gp", "m4v", "ts", "flv", "mpeg", "mpg"
    )
    private val audioExtensions = setOf(
        "mp3", "m4a", "flac", "wav", "ogg", "aac", "opus", "wma", "m4b"
    )
    private val sidecarExtensions = setOf(
        "txt", "chapters", "nfo", "vtt", "srt"
    )

    private val thumbnailCacheDir: File by lazy {
        File(context.cacheDir, "thumbnails").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Takes persistable read permission for a SAF tree URI selected via OpenDocumentTree.
     */
    fun takePersistableFolderPermission(treeUri: Uri): Boolean {
        return try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
            true
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if the app still holds valid read access to a previously authorized folder URI.
     */
    fun isFolderAccessValid(treeUriString: String): Boolean {
        val uri = treeUriString.toUri()
        if (uri.scheme == "file") {
            val file = File(uri.path ?: return false)
            return file.exists() && file.canRead()
        }
        val persistedPermissions = context.contentResolver.persistedUriPermissions
        val hasPersisted = persistedPermissions.any { it.uri == uri && it.isReadPermission }
        val docFile = DocumentFile.fromTreeUri(context, uri)
        return hasPersisted && docFile != null && docFile.exists() && docFile.canRead()
    }

    /**
     * Scans a single authorized folder URI (SAF tree URI or local file URI) and updates Room DB.
     */
    suspend fun scanFolder(
        treeUriString: String,
        preferences: AppPreferences,
        onProgress: (ScanProgress) -> Unit = {}
    ): Result<ScannedFolderEntity> = withContext(Dispatchers.IO) {
        try {
            val uri = treeUriString.toUri()
            val rootDoc: DocumentFile? = if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) DocumentFile.fromFile(file) else null
            } else {
                DocumentFile.fromTreeUri(context, uri)
            }

            val folderTitle = resolveFolderDisplayName(uri, rootDoc)

            if (rootDoc == null || !rootDoc.exists() || !rootDoc.canRead()) {
                val revokedFolder = ScannedFolderEntity(
                    treeUri = treeUriString,
                    displayName = folderTitle,
                    isAccessValid = false,
                    mediaCount = 0,
                    videoCount = 0,
                    audioCount = 0,
                    lastScannedAtMs = System.currentTimeMillis()
                )
                mediaDao.upsertScannedFolder(revokedFolder)
                mediaDao.deleteMediaItemsForFolder(treeUriString)
                return@withContext Result.failure(
                    SecurityException("Access to folder '$folderTitle' was revoked or folder is inaccessible. Please re-select the folder in Settings.")
                )
            }

            onProgress(
                ScanProgress(
                    isScanning = true,
                    currentFolder = folderTitle,
                    scannedFilesCount = 0
                )
            )

            // Fetch existing items for this folder to preserve playback history and cached thumbnails
            val existingItemsByUri = mediaDao.getMediaItemsForFolder(treeUriString)
                .associateBy { it.uri }

            val discoveredMedia = mutableListOf<MediaItemEntity>()
            val discoveredAutoChapters = mutableMapOf<String, List<ChapterEntity>>()
            var counter = 0

            fun traverseDirectory(directory: DocumentFile, currentFolderName: String, depth: Int) {
                val children = try {
                    directory.listFiles()
                } catch (e: Exception) {
                    emptyArray()
                }

                // Group sidecar text files by base filename for chapter matching
                val sidecarMap = mutableMapOf<String, DocumentFile>()
                for (child in children) {
                    if (!child.isDirectory) {
                        val name = child.name ?: continue
                        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                        if (ext in sidecarExtensions) {
                            val base = name.substringBeforeLast('.')
                                .removeSuffix(".chapters")
                                .lowercase(Locale.ROOT)
                            sidecarMap[base] = child
                        }
                    }
                }

                for (child in children) {
                    if (child.isDirectory) {
                        if (preferences.scanNestedSubfolders && depth < 12) {
                            val subName = child.name?.takeIf { it.isNotBlank() } ?: currentFolderName
                            traverseDirectory(child, subName, depth + 1)
                        }
                    } else if (child.isFile && child.canRead()) {
                        val fileName = child.name ?: continue
                        val lowerName = fileName.lowercase(Locale.ROOT)
                        if (lowerName.startsWith(".") ||
                            lowerName.startsWith("~") ||
                            lowerName.contains(".pending-") ||
                            lowerName.contains(".trashed-") ||
                            lowerName.endsWith(".part") ||
                            lowerName.endsWith(".crdownload") ||
                            child.length() <= 512L
                        ) {
                            continue
                        }

                        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                        val mime = child.type?.lowercase(Locale.ROOT) ?: ""

                        val isVideo = mime.startsWith("video/") || ext in videoExtensions
                        val isAudio = !isVideo && (mime.startsWith("audio/") || ext in audioExtensions)

                        if (!isVideo && !(isAudio && preferences.includeAudioFiles)) {
                            continue
                        }

                        val fileUriStr = child.uri.toString()
                        val lastMod = child.lastModified()
                        val fileSize = child.length()
                        val existing = existingItemsByUri[fileUriStr]

                        counter++
                        if (counter % 2 == 0 || counter == 1) {
                            onProgress(
                                ScanProgress(
                                    isScanning = true,
                                    currentFolder = currentFolderName,
                                    scannedFilesCount = counter
                                )
                            )
                        }

                        val baseName = fileName.substringBeforeLast('.')
                        val sidecarDoc = sidecarMap[baseName.lowercase(Locale.ROOT)]

                        // Reuse metadata if file hasn't changed and thumbnail still exists
                        val canReuseMetadata = existing != null &&
                            existing.lastModifiedMs == lastMod &&
                            existing.sizeBytes == fileSize &&
                            existing.durationMs > 0L &&
                            (existing.isAudio || (existing.thumbnailPath != null && File(existing.thumbnailPath).exists()))

                        val mediaEntity = if (canReuseMetadata && existing != null) {
                            val isShort = !existing.isAudio && isShortVideoCandidate(
                                durationMs = existing.durationMs,
                                width = existing.width,
                                height = existing.height,
                                maxShortSec = preferences.shortsMaxDurationSec,
                                includeHorizontalShorts = preferences.includeHorizontalShorts
                            )
                            existing.copy(
                                folderName = currentFolderName,
                                isShortCandidate = isShort
                            )
                        } else {
                            extractMediaEntity(
                                docFile = child,
                                treeUriString = treeUriString,
                                folderName = currentFolderName,
                                fileName = fileName,
                                isAudio = isAudio,
                                existing = existing,
                                preferences = preferences
                            )
                        }

                        if (mediaEntity != null) {
                            discoveredMedia.add(mediaEntity)

                            // Extract chapters from sidecar file or MP4 metadata
                            val extractedChapters = extractChaptersForMedia(
                                mediaUri = fileUriStr,
                                mediaDoc = child,
                                sidecarDoc = sidecarDoc,
                                durationMs = mediaEntity.durationMs
                            )
                            if (extractedChapters.isNotEmpty()) {
                                discoveredAutoChapters[fileUriStr] = extractedChapters
                            }
                        }
                    }
                }
            }

            traverseDirectory(rootDoc, folderTitle, 0)

            // Deduplicate by URI
            val uniqueMedia = discoveredMedia.distinctBy { it.uri }
            val discoveredUris = uniqueMedia.map { it.uri }.toSet()

            // Remove deleted or moved files that belonged to this folder
            val removedUris = existingItemsByUri.keys.filter { it !in discoveredUris }
            if (removedUris.isNotEmpty()) {
                removedUris.chunked(200).forEach { chunk ->
                    mediaDao.deleteMediaItemsByUris(chunk)
                    mediaDao.deleteChaptersForMediaUris(chunk)
                }
            }

            if (uniqueMedia.isNotEmpty()) {
                mediaDao.upsertMediaItems(uniqueMedia)
            }

            // Update auto-extracted chapters while keeping user-created chapters intact
            for ((mediaUri, chapters) in discoveredAutoChapters) {
                mediaDao.deleteAutoChaptersForMedia(mediaUri)
                mediaDao.insertChapters(chapters)
            }

            val videoCount = uniqueMedia.count { !it.isAudio }
            val audioCount = uniqueMedia.count { it.isAudio }

            val updatedFolder = ScannedFolderEntity(
                treeUri = treeUriString,
                displayName = folderTitle,
                isAccessValid = true,
                mediaCount = uniqueMedia.size,
                videoCount = videoCount,
                audioCount = audioCount,
                lastScannedAtMs = System.currentTimeMillis()
            )
            mediaDao.upsertScannedFolder(updatedFolder)

            onProgress(ScanProgress(isScanning = false, currentFolder = folderTitle, scannedFilesCount = uniqueMedia.size))
            Result.success(updatedFolder)
        } catch (e: Exception) {
            onProgress(ScanProgress(isScanning = false, errorMessage = e.localizedMessage ?: "Failed to scan folder"))
            Result.failure(e)
        }
    }

    /**
     * Rescans all saved folders in Room database.
     */
    suspend fun rescanAllFolders(
        preferences: AppPreferences,
        onProgress: (ScanProgress) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val folders = mediaDao.getScannedFoldersOnce()
        for (folder in folders) {
            scanFolder(folder.treeUri, preferences, onProgress)
        }
        onProgress(ScanProgress(isScanning = false))
    }

    private fun extractMediaEntity(
        docFile: DocumentFile,
        treeUriString: String,
        folderName: String,
        fileName: String,
        isAudio: Boolean,
        existing: MediaItemEntity?,
        preferences: AppPreferences
    ): MediaItemEntity? {
        val fileUri = docFile.uri
        val fileUriStr = fileUri.toString()
        val retriever = MediaMetadataRetriever()

        var durationMs = 0L
        var width = 0
        var height = 0
        var metaTitle: String? = null
        var artistOrAlbum: String? = null
        var thumbnailPath: String? = existing?.thumbnailPath

        try {
            if (fileUri.scheme == "file") {
                retriever.setDataSource(fileUri.path)
            } else {
                retriever.setDataSource(context, fileUri)
            }

            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L

            val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0

            if (rotation == 90 || rotation == 270) {
                width = rawHeight
                height = rawWidth
            } else {
                width = rawWidth
                height = rawHeight
            }

            metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }

            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }
            artistOrAlbum = listOfNotNull(artist, album).joinToString(" • ").takeIf { it.isNotBlank() }

            // Generate cached thumbnail bitmap
            if (thumbnailPath == null || !File(thumbnailPath).exists()) {
                thumbnailPath = generateAndCacheThumbnail(retriever, fileUriStr, durationMs, isAudio)
            }
        } catch (e: Exception) {
            // Even if MediaMetadataRetriever partially fails on an exotic container, keep playable URI if size > 0
            if (docFile.length() <= 0L) {
                return null
            }
        } finally {
            runCatching { retriever.release() }
        }

        val cleanedTitle = metaTitle ?: cleanFileNameToTitle(fileName)
        val resolutionLabel = computeResolutionLabel(isAudio, width, height, fileName)
        val isShort = !isAudio && isShortVideoCandidate(
            durationMs = durationMs,
            width = width,
            height = height,
            maxShortSec = preferences.shortsMaxDurationSec,
            includeHorizontalShorts = preferences.includeHorizontalShorts
        )

        return MediaItemEntity(
            uri = fileUriStr,
            folderTreeUri = treeUriString,
            folderName = folderName,
            title = cleanedTitle,
            fileName = fileName,
            mimeType = docFile.type ?: if (isAudio) "audio/*" else "video/*",
            isAudio = isAudio,
            isShortCandidate = isShort,
            durationMs = durationMs,
            sizeBytes = docFile.length(),
            width = width,
            height = height,
            resolutionLabel = resolutionLabel,
            artistOrAlbum = artistOrAlbum,
            thumbnailPath = thumbnailPath,
            lastModifiedMs = docFile.lastModified(),
            lastPlayedPositionMs = existing?.lastPlayedPositionMs ?: 0L,
            lastPlayedAtMs = existing?.lastPlayedAtMs ?: 0L,
            playCount = existing?.playCount ?: 0
        )
    }

    private fun generateAndCacheThumbnail(
        retriever: MediaMetadataRetriever,
        uriString: String,
        durationMs: Long,
        isAudio: Boolean
    ): String? {
        return try {
            val hash = sha256Short(uriString)
            val outFile = File(thumbnailCacheDir, "thumb_$hash.jpg")
            if (outFile.exists() && outFile.length() > 0) {
                return outFile.absolutePath
            }

            val rawBitmap: Bitmap? = if (isAudio) {
                val artBytes = retriever.embeddedPicture
                if (artBytes != null) {
                    BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                } else null
            } else {
                // Sample at 15% of video duration (or 1s) to avoid black fade-in frames
                val sampleTimeUs = if (durationMs > 2000L) {
                    (durationMs * 150L).coerceAtMost(15_000_000L)
                } else {
                    0L
                }
                retriever.getFrameAtTime(sampleTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(0L)
            }

            if (rawBitmap != null) {
                val scaled = scaleBitmapDown(rawBitmap, 640)
                FileOutputStream(outFile).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
                }
                if (scaled != rawBitmap) scaled.recycle()
                rawBitmap.recycle()
                outFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDimension && h <= maxDimension) return bitmap
        val ratio = w.toFloat() / h.toFloat()
        val (targetW, targetH) = if (w > h) {
            maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    /**
     * Parses real chapters from either a sidecar file (e.g. video.chapters.txt / video.txt)
     * or embedded timestamp lines in metadata. Never fabricates chapters when none exist.
     */
    private fun extractChaptersForMedia(
        mediaUri: String,
        mediaDoc: DocumentFile,
        sidecarDoc: DocumentFile?,
        durationMs: Long
    ): List<ChapterEntity> {
        val chapters = mutableListOf<ChapterEntity>()

        // 1. Check sidecar file if present
        if (sidecarDoc != null && sidecarDoc.canRead()) {
            val text = runCatching {
                context.contentResolver.openInputStream(sidecarDoc.uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()

            if (!text.isNullOrBlank()) {
                chapters.addAll(parseTimestampLines(mediaUri, text, durationMs, isAuto = true))
            }
        }

        // 2. If no sidecar chapters found, check if filename or metadata description contains timestamps
        if (chapters.isEmpty()) {
            val retriever = MediaMetadataRetriever()
            try {
                if (mediaDoc.uri.scheme == "file") {
                    retriever.setDataSource(mediaDoc.uri.path)
                } else {
                    retriever.setDataSource(context, mediaDoc.uri)
                }
                // Check common metadata fields where creators embed timestamp chapters
                val writerField = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER)
                val genreField = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                for (candidate in listOfNotNull(writerField, genreField)) {
                    val parsed = parseTimestampLines(mediaUri, candidate, durationMs, isAuto = true)
                    if (parsed.size >= 2) {
                        chapters.addAll(parsed)
                        break
                    }
                }
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }
        }

        return chapters.sortedBy { it.startTimeMs }.distinctBy { it.startTimeMs }
    }

    companion object {
        private val timestampRegex = Regex(
            """(?:^|\s)(?:(\d{1,2}):)?(\d{1,2}):(\d{2})(?:\.\d{1,3})?\s*[-–—:|]?\s*(.+)"""
        )

        fun parseTimestampLines(
            mediaUri: String,
            rawText: String,
            durationMs: Long,
            isAuto: Boolean = false
        ): List<ChapterEntity> {
            val result = mutableListOf<ChapterEntity>()
            val lines = rawText.lines()
            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty() || line.contains("-->")) continue
                val match = timestampRegex.find(line) ?: continue
                val hours = match.groupValues[1].toLongOrNull() ?: 0L
                val minutes = match.groupValues[2].toLongOrNull() ?: 0L
                val seconds = match.groupValues[3].toLongOrNull() ?: 0L
                val title = match.groupValues[4].trim().trim('-', '–', '—', ':', '|', ' ')
                if (title.isEmpty()) continue

                val startMs = ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L
                if (durationMs <= 0L || startMs < durationMs) {
                    result.add(
                        ChapterEntity(
                            mediaUri = mediaUri,
                            startTimeMs = startMs,
                            title = title,
                            isAutoExtracted = isAuto
                        )
                    )
                }
            }
            return result.sortedBy { it.startTimeMs }.distinctBy { it.startTimeMs }
        }

        fun isShortVideoCandidate(
            durationMs: Long,
            width: Int,
            height: Int,
            maxShortSec: Int,
            includeHorizontalShorts: Boolean
        ): Boolean {
            val isVertical = width > 0 && height > width
            val maxDurationMs = maxShortSec * 1000L
            val isShortDuration = durationMs in 1_000L..maxDurationMs
            return when {
                isVertical && durationMs <= 180_000L -> true
                includeHorizontalShorts && isShortDuration -> true
                else -> false
            }
        }

        fun cleanFileNameToTitle(fileName: String): String {
            val withoutExt = fileName.substringBeforeLast('.')
            return withoutExt
                .replace('_', ' ')
                .replace('.', ' ')
                .replace(Regex("""\s+"""), " ")
                .trim()
                .ifEmpty { fileName }
        }

        fun computeResolutionLabel(
            isAudio: Boolean,
            width: Int,
            height: Int,
            fileName: String
        ): String {
            val ext = fileName.substringAfterLast('.', "").uppercase(Locale.ROOT)
            if (isAudio) {
                return if (ext.isNotEmpty()) "$ext Audio" else "Audio"
            }
            val maxDim = maxOf(width, height)
            val minDim = minOf(width, height)
            return when {
                maxDim >= 3840 || minDim >= 2160 -> "4K UHD"
                maxDim >= 2560 || minDim >= 1440 -> "1440p QHD"
                maxDim >= 1920 || minDim >= 1080 -> if (height > width) "1080p Vertical" else "1080p FHD"
                maxDim >= 1280 || minDim >= 720 -> if (height > width) "720p Vertical" else "720p HD"
                maxDim > 0 -> "${width}×${height}"
                ext.isNotEmpty() -> "$ext Video"
                else -> "Local Video"
            }
        }
    }

    private fun resolveFolderDisplayName(uri: Uri, docFile: DocumentFile?): String {
        docFile?.name?.takeIf { it.isNotBlank() }?.let { return it }
        if (uri.scheme == "file") {
            return File(uri.path ?: "").name.ifEmpty { "Local Folder" }
        }
        return runCatching {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            docId.substringAfterLast(':').substringAfterLast('/').ifEmpty { docId }
        }.getOrDefault("Media Folder")
    }

    private fun sha256Short(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.take(10).joinToString("") { "%02x".format(it) }
    }
}
