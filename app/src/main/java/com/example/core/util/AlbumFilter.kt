package com.example.core.util

import com.example.core.model.AppSortCriteria
import com.example.core.model.Song
import com.example.ui.library.ArtworkRef
import com.example.ui.library.album.AlbumUiModel

object AlbumFilter {
    fun buildAlbumsList(
        songs: List<Song>,
        searchQuery: String,
        sortCriteria: AppSortCriteria,
        isAscending: Boolean
    ): List<AlbumUiModel> {
        val grouped = songs.groupBy { it.album.trim() }
            .filterKeys { it.isNotBlank() && it != "<unknown>" && !it.equals("unknown album", ignoreCase = true) }
            .map { (albumName, albumSongs) ->
                val repSong = albumSongs.firstOrNull { !it.dataPath.isNullOrBlank() } ?: albumSongs.first()
                val validArtists = albumSongs.map { it.artist.trim() }
                    .filter { it.isNotBlank() && it != "<unknown>" && !it.equals("unknown artist", ignoreCase = true) }
                val resolvedArtist = if (validArtists.isNotEmpty()) {
                    validArtists.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: validArtists.first()
                } else {
                    repSong.artist.trim().takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Artist"
                }

                AlbumUiModel(
                    id = repSong.albumId.takeIf { it != -1L } ?: repSong.id,
                    name = albumName,
                    artist = resolvedArtist,
                    songCount = albumSongs.size,
                    artwork = ArtworkRef(
                        artworkUri = repSong.artworkUri,
                        contentUri = repSong.contentUri
                    )
                )
            }

        val filtered = if (searchQuery.isBlank()) grouped else {
            grouped.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
            }
        }

        return when (sortCriteria) {
            AppSortCriteria.ALBUM, AppSortCriteria.TITLE -> {
                if (isAscending) {
                    filtered.sortedWith { a, b ->
                        val nameCmp = SortHelper.compareAlbums(a.name, b.name)
                        if (nameCmp != 0) nameCmp else SortHelper.compareArtists(a.artist, b.artist)
                    }
                } else {
                    filtered.sortedWith { a, b ->
                        val nameCmp = SortHelper.compareAlbums(b.name, a.name)
                        if (nameCmp != 0) nameCmp else SortHelper.compareArtists(a.artist, b.artist)
                    }
                }
            }
            AppSortCriteria.ARTIST -> {
                if (isAscending) {
                    filtered.sortedWith { a, b ->
                        val artistCmp = SortHelper.compareArtists(a.artist, b.artist)
                        if (artistCmp != 0) artistCmp else SortHelper.compareAlbums(a.name, b.name)
                    }
                } else {
                    filtered.sortedWith { a, b ->
                        val artistCmp = SortHelper.compareArtists(b.artist, a.artist)
                        if (artistCmp != 0) artistCmp else SortHelper.compareAlbums(a.name, b.name)
                    }
                }
            }
            AppSortCriteria.COUNT -> {
                if (isAscending) {
                    filtered.sortedWith { a, b ->
                        val countCmp = a.songCount.compareTo(b.songCount)
                        if (countCmp != 0) countCmp else SortHelper.compareAlbums(a.name, b.name)
                    }
                } else {
                    filtered.sortedWith { a, b ->
                        val countCmp = b.songCount.compareTo(a.songCount)
                        if (countCmp != 0) countCmp else SortHelper.compareAlbums(a.name, b.name)
                    }
                }
            }
            else -> {
                if (isAscending) {
                    filtered.sortedWith { a, b ->
                        val nameCmp = SortHelper.compareAlbums(a.name, b.name)
                        if (nameCmp != 0) nameCmp else SortHelper.compareArtists(a.artist, b.artist)
                    }
                } else {
                    filtered.sortedWith { a, b ->
                        val nameCmp = SortHelper.compareAlbums(b.name, a.name)
                        if (nameCmp != 0) nameCmp else SortHelper.compareArtists(a.artist, b.artist)
                    }
                }
            }
        }
    }
}
