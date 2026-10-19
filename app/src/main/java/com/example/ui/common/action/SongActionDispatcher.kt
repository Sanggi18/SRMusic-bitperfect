package com.example.ui.common.action

import com.example.core.data.repository.FavoritesRepository
import com.example.core.model.Song
import com.example.ui.library.core.SongAction
import com.example.ui.player.core.PlayerAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

object SongActionDispatcher {

    fun dispatch(
        action: SongAction,
        song: Song,
        playerEvents: MutableSharedFlow<PlayerAction>,
        favoritesRepository: FavoritesRepository,
        scope: CoroutineScope,
        playlistContext: List<Song>? = null
    ): Boolean {
        return when (action) {
            SongAction.PLAY -> {
                val queue = playlistContext ?: listOf(song)
                playerEvents.tryEmit(PlayerAction.PlaySong(song, queue))
                true
            }
            SongAction.PLAY_NEXT -> {
                playerEvents.tryEmit(PlayerAction.PlayNext(song))
                true
            }
            SongAction.ADD_TO_QUEUE -> {
                playerEvents.tryEmit(PlayerAction.AddToQueue(song))
                true
            }
            SongAction.TOGGLE_FAVORITE -> {
                scope.launch {
                    favoritesRepository.toggleFavorite(song.id)
                }
                true
            }
            else -> false
        }
    }
}
