package com.example.ui.player.core
import com.example.ui.player.components.*
import com.example.ui.common.components.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.data.preferences.AppPreferences
import com.example.core.data.repository.FavoritesRepository
import com.example.core.playback.PlaybackManager
import com.example.core.model.PlaybackInfo
import com.example.core.model.PlaybackProgress
import com.example.core.data.mediastore.tags.AudioTechnicalInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class PlayerInternalFlags(
    val isExpanded: Boolean,
    val isDismissed: Boolean
)

private data class PlayerStyleSettings(
    val progressStyle: com.example.core.model.NowPlayingProgressStyle,
    val nowPlayingTheme: com.example.core.model.NowPlayingTheme,
    val trackInfoAlignment: com.example.core.model.TrackInfoAlignment
)

class PlayerViewModel(
    private val playbackManager: PlaybackManager,
    private val favoritesRepo: FavoritesRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _isExpanded = MutableStateFlow(false)
    private val _isDismissed = MutableStateFlow(false)

    private val flagsFlow = combine(
        _isExpanded,
        _isDismissed
    ) { exp, dis ->
        PlayerInternalFlags(exp, dis)
    }

    private val stylesFlow = combine(
        appPreferences.nowPlayingProgressStyleFlow,
        appPreferences.nowPlayingThemeFlow,
        appPreferences.trackInfoAlignmentFlow
    ) { p, t, a ->
        PlayerStyleSettings(p, t, a)
    }

    private val playbackModesFlow = combine(
        playbackManager.isShuffleEnabledFlow,
        playbackManager.repeatModeFlow
    ) { shuffle, repeat ->
        Pair(shuffle, repeat)
    }

    val playbackProgress: StateFlow<PlaybackProgress> = playbackManager.playbackProgress

    val uiState: StateFlow<PlayerUiState> = combine(
        playbackManager.playbackInfo,
        playbackManager.currentQueue,
        favoritesRepo.favoriteSongsFlow,
        flagsFlow,
        combine(stylesFlow, playbackModesFlow) { s, m -> Pair(s, m) }
    ) { info, queue, favs, flags, stylesAndModes ->
        val (styles, modes) = stylesAndModes
        val (isShuffle, rawRepeat) = modes
        val currentSong = info.currentSong
        val isFav = currentSong != null && favs.any { it.id == currentSong.id }
        val repeatMode = when (rawRepeat) {
            androidx.media3.common.Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            androidx.media3.common.Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }

        PlayerUiState(
            playbackInfo = info,
            playbackProgress = PlaybackProgress(),
            currentQueue = queue,
            isShuffle = isShuffle,
            repeatMode = repeatMode,
            isFavorite = isFav,
            technicalInfo = null, // Extracted on demand or per-song
            isExpanded = flags.isExpanded,
            isDismissed = flags.isDismissed,
            progressStyle = styles.progressStyle,
            nowPlayingTheme = styles.nowPlayingTheme,
            trackInfoAlignment = styles.trackInfoAlignment
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerUiState())

    fun onAction(action: PlayerAction) {
        when (action) {
            is PlayerAction.PlayPause -> playbackManager.togglePlayPause()
            is PlayerAction.Next -> playbackManager.skipToNext()
            is PlayerAction.Previous -> playbackManager.skipToPrevious()
            is PlayerAction.SeekTo -> playbackManager.seekTo(action.positionMs)
            is PlayerAction.ToggleShuffle -> {
                playbackManager.toggleShuffle()
            }
            is PlayerAction.ToggleRepeat -> {
                playbackManager.toggleRepeat()
            }
            is PlayerAction.ToggleFavorite -> {
                viewModelScope.launch {
                    favoritesRepo.toggleFavorite(action.songId)
                }
            }
            is PlayerAction.SelectQueueItem -> {
                playbackManager.playFromQueue(action.index)
            }
            is PlayerAction.PlaySong -> {
                _isDismissed.value = false
                playbackManager.playSong(action.song, action.playlist)
            }
            is PlayerAction.PlayNext -> playbackManager.playNext(action.song)
            is PlayerAction.PlayNextBatch -> playbackManager.playNextBatch(action.songs)
            is PlayerAction.AddToQueue -> playbackManager.addToQueue(action.song)
            is PlayerAction.AddToQueueBatch -> playbackManager.addToQueueBatch(action.songs)
            is PlayerAction.PlayShuffleSession -> {
                _isDismissed.value = false
                playbackManager.playSong(action.startSong, action.sessionBatch, isShuffleSession = true)
            }
            is PlayerAction.Expand -> {
                _isExpanded.value = true
                _isDismissed.value = false
            }
            is PlayerAction.Collapse -> {
                _isExpanded.value = false
            }
            is PlayerAction.Dismiss -> {
                _isDismissed.value = true
                _isExpanded.value = false
            }
            is PlayerAction.SetExpanded -> {
                _isExpanded.value = action.expanded
                if (action.expanded) _isDismissed.value = false
            }
            is PlayerAction.SetDismissed -> {
                _isDismissed.value = action.dismissed
                if (action.dismissed) _isExpanded.value = false
            }
            is PlayerAction.OpenEqualizer -> {
                // Handled in navigation/host layer
            }
        }
    }

    companion object {
        fun provideFactory(
            playbackManager: PlaybackManager,
            favoritesRepo: FavoritesRepository,
            appPreferences: AppPreferences
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlayerViewModel(playbackManager, favoritesRepo, appPreferences) as T
                }
            }
    }
}
