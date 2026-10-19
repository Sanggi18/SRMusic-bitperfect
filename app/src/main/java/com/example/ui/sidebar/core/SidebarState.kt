package com.example.ui.sidebar.core

import android.net.Uri
import com.example.core.data.database.playlist.PlaylistEntity
import com.example.core.model.Song

enum class SidebarChildScreen {
    NONE,
    PLAYLIST,
    FAVORITES,
    HISTORY
}

data class CollectionUiState(
    val favoriteSongs: List<Song> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val playlistSongsMap: Map<Long, List<Song>> = emptyMap(),
    val historySongs: List<Song> = emptyList(),
    val statusMessage: String? = null
)

sealed interface CollectionAction {
    data class ToggleFavorite(val songId: Long) : CollectionAction
    data class CreatePlaylist(val name: String, val onCreated: ((Long) -> Unit)? = null) : CollectionAction
    data class RenamePlaylist(val playlistId: Long, val newName: String) : CollectionAction
    data class DeletePlaylist(val playlistId: Long) : CollectionAction
    data class DeleteMultiplePlaylists(val playlistIds: List<Long>) : CollectionAction
    data class AddSongToPlaylist(val playlistId: Long, val songId: Long) : CollectionAction
    data class AddSongsToPlaylist(val playlistId: Long, val songIds: List<Long>) : CollectionAction
    data class RemoveSongFromPlaylist(val playlistId: Long, val songId: Long) : CollectionAction
    data class ExportPlaylist(val uri: Uri, val songs: List<Song>) : CollectionAction
    data class ImportPlaylist(val uri: Uri, val allSongs: List<Song>) : CollectionAction
    data object ClearHistory : CollectionAction
    data object ClearStatusMessage : CollectionAction
}
