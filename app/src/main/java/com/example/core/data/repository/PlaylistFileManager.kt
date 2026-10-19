package com.example.core.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.core.model.Song
import java.io.File
import java.io.InputStream

/**
 * M3U playlist file import/export. Lives next to PlaylistRepository (its only consumer).
 */
object PlaylistFileManager {

    data class ImportResult(
        val playlistName: String,
        val matchedSongs: List<Song>,
        val totalCount: Int
    )

    fun exportPlaylistContent(songs: List<Song>): ByteArray {
        val sb = StringBuilder()
        sb.append("#EXTM3U\n")
        for (song in songs) {
            val durationSec = if (song.duration > 0) song.duration / 1000 else -1
            sb.append("#EXTINF:$durationSec,${song.artist} - ${song.title}\n")
            sb.append(song.dataPath?.takeIf { it.isNotBlank() } ?: song.contentUri.toString())
            sb.append('\n')
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    fun parsePlaylistFile(
        context: Context,
        uri: Uri,
        allSongs: List<Song>
    ): ImportResult? {
        val fileName = getFileName(context, uri) ?: "Imported Playlist"
        val fallbackName = fileName.substringBeforeLast('.')

        val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
        val rawContent = inputStream.use { it.bufferedReader(Charsets.UTF_8).readText() }
        if (rawContent.isBlank()) return null

        return parseM3u(rawContent, fallbackName, allSongs)
    }

    private fun parseM3u(
        content: String,
        fallbackName: String,
        allSongs: List<Song>
    ): ImportResult {
        val lines = content.lines()
        val matchedSongs = mutableListOf<Song>()
        var pendingArtist: String? = null
        var pendingTitle: String? = null
        var totalEntries = 0

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                val info = trimmed.substringAfter("#EXTINF:")
                val commaIndex = info.indexOf(',')
                val metadata = if (commaIndex != -1) info.substring(commaIndex + 1).trim() else info.trim()
                if (metadata.contains(" - ")) {
                    pendingArtist = metadata.substringBefore(" - ").trim()
                    pendingTitle = metadata.substringAfter(" - ").trim()
                } else {
                    pendingTitle = metadata
                    pendingArtist = null
                }
            } else if (!trimmed.startsWith("#")) {
                totalEntries++
                val match = findMatchingSong(
                    pathOrUri = trimmed,
                    artist = pendingArtist,
                    title = pendingTitle,
                    allSongs = allSongs
                )
                if (match != null && !matchedSongs.any { it.id == match.id }) {
                    matchedSongs.add(match)
                }
                pendingArtist = null
                pendingTitle = null
            }
        }

        return ImportResult(
            playlistName = fallbackName,
            matchedSongs = matchedSongs,
            totalCount = totalEntries
        )
    }

    private fun findMatchingSong(
        pathOrUri: String,
        artist: String?,
        title: String?,
        allSongs: List<Song>
    ): Song? {
        if (pathOrUri.isNotBlank()) {
            allSongs.firstOrNull { it.dataPath?.equals(pathOrUri, ignoreCase = true) == true }?.let { return it }
            allSongs.firstOrNull { it.contentUri.toString().equals(pathOrUri, ignoreCase = true) }?.let { return it }
            val fileName = File(pathOrUri).name
            if (fileName.isNotBlank()) {
                allSongs.firstOrNull { it.dataPath?.let { dp -> File(dp).name.equals(fileName, ignoreCase = true) } == true }?.let { return it }
            }
        }
        if (!title.isNullOrBlank() && !artist.isNullOrBlank()) {
            allSongs.firstOrNull {
                it.title.equals(title, ignoreCase = true) && it.artist.equals(artist, ignoreCase = true)
            }?.let { return it }
        }
        if (!title.isNullOrBlank()) {
            allSongs.firstOrNull { it.title.equals(title, ignoreCase = true) }?.let { return it }
        }
        return null
    }

    fun getFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            return cursor.getString(index)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return uri.path?.let { File(it).name }
    }
}
