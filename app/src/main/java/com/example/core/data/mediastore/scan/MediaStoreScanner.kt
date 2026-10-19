package com.example.core.data.mediastore.scan

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.core.data.database.song.SongEntity
import com.example.core.data.mediastore.tags.GenreUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MediaStore scanner, rovia-style:
 * - Exactly ONE query per scan.
 * - Zero per-song file I/O (no MediaMetadataRetriever, no Jaudiotagger during scan).
 * - Everything comes from the cursor; column indices are cached once.
 */
class MediaStoreScanner(private val context: Context) {

    suspend fun scanMediaStore(
        options: LibraryScanOptions,
        onProgress: ((count: Int) -> Unit)? = null
    ): List<SongEntity> = withContext(Dispatchers.IO) {
        val collection: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projectionList = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.RELATIVE_PATH
        )
        // GENRE column only exists on API 30+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projectionList.add(MediaStore.Audio.Media.GENRE)
        }
        val projection = projectionList.toTypedArray()

        val minDurationMs = if (options.filterShortAudio) {
            (options.minDurationSeconds * 1000L).coerceAtLeast(1000L)
        } else {
            1000L
        }
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")
        val items = mutableListOf<SongEntity>()

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                val relativePathCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else {
                    -1
                }

                while (cursor.moveToNext()) {
                    // Duration already filtered by SQL (DURATION >= ?); no re-check needed.
                    val duration = cursor.getLong(durationCol)

                    val displayName = cursor.getString(displayNameCol)
                    if (displayName?.startsWith(".") == true) continue

                    val dataPath = if (dataCol != -1) cursor.getString(dataCol) else null
                    val relativePath = resolveRelativePath(cursor, relativePathCol, dataPath)

                    val filterDataPath = dataPath.orEmpty()
                    val filterRelativePath = relativePath.orEmpty()
                    if (ScanPathFilter.isPathBlacklisted(filterDataPath, filterRelativePath, options.blacklistedFolders)) continue
                    if (!ScanPathFilter.isPathIncluded(filterDataPath, filterRelativePath, options.includedFolders)) continue

                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol)
                    val rawArtist = cursor.getString(artistCol)
                    val rawAlbum = cursor.getString(albumCol)
                    val albumId = cursor.getLong(albumIdCol)

                    val title = rawTitle.takeUnless { it.isNullOrBlank() }
                        ?: displayName?.substringBeforeLast(".")
                        ?: "Unknown Track"
                    val artist = rawArtist.takeUnless { it.isNullOrBlank() || it == "<unknown>" }
                        ?: "Unknown Artist"
                    val album = rawAlbum.takeUnless { it.isNullOrBlank() || it == "<unknown>" }
                        ?: "Unknown Album"

                    // Genre: cursor only. Never open the audio file during scan.
                    val rawGenre = if (genreCol != -1 && !cursor.isNull(genreCol)) {
                        cursor.getString(genreCol)
                    } else {
                        null
                    }
                    val genre = if (GenreUtils.isValidGenre(rawGenre)) {
                        GenreUtils.cleanGenre(rawGenre!!)
                    } else {
                        null
                    }

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                    val artworkUri = ContentUris.withAppendedId(albumArtBaseUri, albumId)

                    items.add(
                        SongEntity(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            albumId = albumId,
                            contentUriString = contentUri.toString(),
                            artworkUriString = artworkUri.toString(),
                            trackNumber = if (!cursor.isNull(trackCol)) cursor.getInt(trackCol) else null,
                            year = if (!cursor.isNull(yearCol)) cursor.getInt(yearCol) else null,
                            size = if (!cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else null,
                            displayName = displayName,
                            relativePath = relativePath,
                            dataPath = dataPath,
                            dateAdded = if (dateAddedCol != -1 && !cursor.isNull(dateAddedCol)) cursor.getLong(dateAddedCol) else null,
                            dateModified = if (dateModifiedCol != -1 && !cursor.isNull(dateModifiedCol)) cursor.getLong(dateModifiedCol) else null,
                            genre = genre
                        )
                    )

                    if (items.size <= 20 || items.size % 20 == 0) onProgress?.invoke(items.size)
                }
            }
        } catch (e: Exception) {
            Log.e("MediaStoreScanner", "Error scanning media store", e)
        }

        items
    }

    /**
     * Prefer MediaStore RELATIVE_PATH (API 29+); fall back to parsing DATA on older devices.
     */
    private fun resolveRelativePath(cursor: Cursor, relativePathCol: Int, dataPath: String?): String? {
        if (relativePathCol != -1) {
            val rp = cursor.getString(relativePathCol)
            if (!rp.isNullOrBlank()) return rp.trim().trimEnd('/')
        }
        if (dataPath.isNullOrBlank()) return null
        val directoryPath = dataPath.substringBeforeLast('/', "")
        if (directoryPath.isBlank()) return null
        return when {
            directoryPath.contains("/Music/") -> "Music/" + directoryPath.substringAfter("/Music/")
            directoryPath.contains("/sdcard/") -> directoryPath.substringAfter("/sdcard/")
            directoryPath.contains("/storage/emulated/0/") -> directoryPath.substringAfter("/storage/emulated/0/")
            else -> directoryPath.trimStart('/')
        }.takeIf { it.isNotBlank() }
    }
}
