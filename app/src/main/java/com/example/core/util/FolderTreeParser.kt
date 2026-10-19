package com.example.core.util

import com.example.core.model.Song
import com.example.ui.library.ArtworkRef
import com.example.ui.library.folder.FolderUiModel

object FolderTreeParser {

    fun getSongRelativeDirectory(song: Song): String {
        val rp = song.relativePath
        val dp = song.dataPath
        val path = when {
            !rp.isNullOrBlank() -> rp.trim('/')
            !dp.isNullOrBlank() -> {
                val dir = dp.substringBeforeLast('/', "")
                when {
                    dir.contains("/Music/") -> dir.substringAfter("/Music/").trim('/')
                    dir.contains("/Music") -> dir.substringAfter("/Music").trim('/')
                    dir.contains("/sdcard/") -> dir.substringAfter("/sdcard/").trim('/')
                    dir.contains("/storage/emulated/0/") -> dir.substringAfter("/storage/emulated/0/").trim('/')
                    else -> dir.trim('/')
                }
            }
            else -> ""
        }
        return path.removePrefix("Music/").removePrefix("Music").trim('/')
    }

    fun parseFolderContent(
        allSongs: List<Song>,
        effectiveRelativePath: String,
        searchQuery: String
    ): Pair<List<FolderUiModel>, List<Song>> {
        val normalizedCurrentPath = effectiveRelativePath.trim('/')

        val songsUnderCurrent = allSongs.filter { song ->
            val songDir = getSongRelativeDirectory(song)
            if (normalizedCurrentPath.isEmpty()) {
                true
            } else {
                songDir == normalizedCurrentPath || songDir.startsWith("$normalizedCurrentPath/")
            }
        }

        // Direct songs in this folder
        val directSongs = songsUnderCurrent.filter { song ->
            val songDir = getSongRelativeDirectory(song)
            songDir == normalizedCurrentPath
        }.let { list ->
            if (searchQuery.isBlank()) list else list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                (it.displayName?.contains(searchQuery, ignoreCase = true) == true)
            }
        }

        // Discover immediate child subfolders
        val subfolderMap = mutableMapOf<String, MutableList<Song>>()
        songsUnderCurrent.forEach { song ->
            val songDir = getSongRelativeDirectory(song)
            if (songDir != normalizedCurrentPath && songDir.isNotEmpty()) {
                val remainingPath = if (normalizedCurrentPath.isEmpty()) {
                    songDir
                } else {
                    songDir.removePrefix("$normalizedCurrentPath/").trim('/')
                }
                val immediateChild = remainingPath.substringBefore('/')
                if (immediateChild.isNotEmpty()) {
                    subfolderMap.getOrPut(immediateChild) { mutableListOf() }.add(song)
                }
            }
        }

        val subfolderModels = subfolderMap.map { (folderName, songsInSubfolder) ->
            val subfolderRelPath = if (normalizedCurrentPath.isEmpty()) folderName else "$normalizedCurrentPath/$folderName"

            // Count immediate nested sub-subfolders
            val nestedSubCount = songsInSubfolder.map { getSongRelativeDirectory(it) }
                .filter { it.startsWith("$subfolderRelPath/") }
                .map { it.removePrefix("$subfolderRelPath/").substringBefore('/') }
                .distinct()
                .size

            val repSong = songsInSubfolder.firstOrNull { it.artworkUri != null || !it.dataPath.isNullOrBlank() } ?: songsInSubfolder.firstOrNull()

            FolderUiModel(
                path = subfolderRelPath,
                name = folderName,
                songCount = songsInSubfolder.size,
                subfolderCount = nestedSubCount,
                artwork = ArtworkRef(
                    artworkUri = repSong?.artworkUri,
                    contentUri = repSong?.contentUri
                )
            )
        }.sortedBy { it.name.lowercase() }.let { list ->
            if (searchQuery.isBlank()) list else list.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }

        return Pair(subfolderModels, directSongs)
    }

    fun getSongsInFolder(allSongs: List<Song>, folderPath: String): List<Song> {
        val targetRelPath = folderPath.trim('/')
        return allSongs.filter { s ->
            val dir = getSongRelativeDirectory(s)
            dir == targetRelPath || dir.startsWith("$targetRelPath/")
        }
    }
}

