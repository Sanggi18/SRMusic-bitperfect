package com.example.core.util

import com.example.core.model.AppSortCriteria
import com.example.core.model.Song
import com.example.core.util.ArtistHelper
import com.example.core.util.SortHelper
import com.example.ui.library.ArtworkRef
import com.example.ui.library.artist.ArtistUiModel

object ArtistFilter {
    fun buildArtistsList(
        songs: List<Song>,
        searchQuery: String,
        sortCriteria: AppSortCriteria,
        isAscending: Boolean
    ): List<ArtistUiModel> {
        val grouped = songs.groupBy { ArtistHelper.extractMainArtist(it.artist) }
            .filterKeys { it.isNotBlank() && it != "<unknown>" && !it.equals("unknown artist", ignoreCase = true) }
            .map { (artistName, artistSongs) ->
                val representativeSong = artistSongs.firstOrNull { it.artworkUri != null || !it.dataPath.isNullOrBlank() } ?: artistSongs.first()
                ArtistUiModel(
                    id = representativeSong.id,
                    name = artistName,
                    songCount = artistSongs.size,
                    artwork = ArtworkRef(
                        artworkUri = representativeSong.artworkUri,
                        contentUri = representativeSong.contentUri
                    )
                )
            }

        val filtered = if (searchQuery.isBlank()) grouped else {
            grouped.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        val sorted = when (sortCriteria) {
            AppSortCriteria.ARTIST, AppSortCriteria.TITLE -> filtered.sortedWith { a, b -> SortHelper.compareTitles(a.name, b.name) }
            AppSortCriteria.COUNT -> filtered.sortedWith { a, b ->
                val countCmp = a.songCount.compareTo(b.songCount)
                if (countCmp != 0) countCmp else SortHelper.compareTitles(a.name, b.name)
            }
            else -> filtered.sortedWith { a, b -> SortHelper.compareTitles(a.name, b.name) }
        }

        return if (isAscending) sorted else sorted.reversed()
    }
}
