package com.example.ui.home.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.data.preferences.AppPreferences
import com.example.core.data.repository.FavoritesRepository
import com.example.core.data.repository.HistoryRepository
import com.example.core.data.repository.SongRepository
import com.example.core.model.Song
import com.example.core.playback.PlaybackManager
import com.example.ui.common.action.SongActionDispatcher
import com.example.ui.library.core.SongAction
import com.example.ui.player.core.PlayerAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class HomeStats(
    val totalPlayCount: Int,
    val totalListenedSeconds: Long,
    val songPlayCounts: Map<Long, Int>
)

private data class HomeOffsets(
    val remixOffset: Int,
    val genreOffset: Int,
    val selectedDailyGenreIndex: Int
)

class HomeViewModel(
    private val songRepository: SongRepository,
    private val historyRepository: HistoryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val appPreferences: AppPreferences,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _playerEvents = MutableSharedFlow<PlayerAction>(extraBufferCapacity = 64)
    val playerEvents: SharedFlow<PlayerAction> = _playerEvents.asSharedFlow()

    private val _shuffleTracks = MutableStateFlow<List<Song>>(emptyList())
    private var isShuffleSessionActive = false

    private val _remixOffset = MutableStateFlow(0)
    private val _genreOffset = MutableStateFlow(0)
    private val _selectedDailyGenreIndex = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            songRepository.allSongsFlow.collect { songs ->
                if (songs.isNotEmpty() && _shuffleTracks.value.isEmpty()) {
                    val savedIds = appPreferences.getShuffleTrackIds()
                    val savedSongs = savedIds.mapNotNull { id -> songs.find { it.id == id } }
                    if (savedSongs.isNotEmpty() && (savedSongs.size == savedIds.size || savedSongs.size == 14 || savedSongs.size == songs.size)) {
                        _shuffleTracks.value = savedSongs
                    } else {
                        val newBatch = if (songs.size <= 14) songs.shuffled() else songs.shuffled().take(14)
                        _shuffleTracks.value = newBatch
                        appPreferences.setShuffleTrackIds(newBatch.map { it.id })
                    }
                }
            }
        }

        viewModelScope.launch {
            playbackManager.onPlaybackEndedEvent.collect {
                if (isShuffleSessionActive) {
                    val allSongs = songRepository.getAllSongs()
                    if (allSongs.isNotEmpty()) {
                        val prevBatchIds = _shuffleTracks.value.map { it.id }.toSet()
                        val candidates = allSongs.filter { it.id !in prevBatchIds }
                        val nextBatch = if (candidates.size >= 14) {
                            candidates.shuffled().take(14)
                        } else if (allSongs.size <= 14) {
                            allSongs.shuffled()
                        } else {
                            (candidates.shuffled() + allSongs.filter { it.id in prevBatchIds }.shuffled()).take(14)
                        }
                        _shuffleTracks.value = nextBatch
                        appPreferences.setShuffleTrackIds(nextBatch.map { it.id })
                        playbackManager.playSong(nextBatch[0], nextBatch, isShuffleSession = true)
                    }
                }
            }
        }
    }

    private val statsFlow = combine(
        historyRepository.totalPlayCountFlow,
        historyRepository.totalListenedSecondsFlow,
        historyRepository.allPlayCountsFlow
    ) { count, seconds, countsMap ->
        HomeStats(count, seconds, countsMap)
    }

    private val offsetsFlow = combine(
        _remixOffset,
        _genreOffset,
        _selectedDailyGenreIndex
    ) { r, g, s ->
        HomeOffsets(r, g, s)
    }

private data class LibrarySnapshot(
    val allSongs: List<Song>,
    val songById: Map<Long, Song>,
    val recentSongs: List<Song>,
    val albumsCount: Int,
    val artistsCount: Int
)

    private val librarySnapshotFlow = songRepository.allSongsFlow.map { allSongs ->
        LibrarySnapshot(
            allSongs = allSongs,
            songById = allSongs.associateBy { it.id },
            recentSongs = allSongs.sortedByDescending { it.dateModified }.take(50),
            albumsCount = allSongs.asSequence().map { it.album }.distinct().count(),
            artistsCount = allSongs.asSequence().map { it.artist }.distinct().count()
        )
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<HomeUiState> = combine(
        librarySnapshotFlow,
        historyRepository.historySongIdsFlow,
        statsFlow,
        offsetsFlow,
        _shuffleTracks
    ) { snapshot, historySongIds, stats, offsets, currentShuffleTracks ->
        val allSongs = snapshot.allSongs
        val installSeed = 42L
        val historySongs = historySongIds.mapNotNull { id -> snapshot.songById[id] }
        val remixQueue = HomeContentGenerator.generateRemixQueue(allSongs, installSeed, offsets.remixOffset)
        val dailyGenres = HomeContentGenerator.generateDailyGenres(allSongs, installSeed, offsets.genreOffset)

        val top1Artist = HomeContentGenerator.computeTopArtist(allSongs, stats.songPlayCounts)
        val top1Album = HomeContentGenerator.computeTopAlbum(allSongs, stats.songPlayCounts)
        val top1Song = HomeContentGenerator.computeTopSong(allSongs, stats.songPlayCounts)
        val topMostPlayed = allSongs.sortedByDescending { stats.songPlayCounts[it.id] ?: 0 }.take(20)

        HomeUiState(
            songs = allSongs,
            totalPlayCount = stats.totalPlayCount,
            totalListenedSeconds = stats.totalListenedSeconds,
            songPlayCounts = stats.songPlayCounts,
            shuffleTracks = currentShuffleTracks,
            remixQueue = remixQueue,
            dailyGenres = dailyGenres,
            selectedDailyGenreIndex = offsets.selectedDailyGenreIndex,
            recentSongs = snapshot.recentSongs,
            top1Artist = top1Artist,
            top1Album = top1Album,
            top1Song = top1Song,
            topMostPlayed = topMostPlayed,
            totalAlbumsCount = snapshot.albumsCount,
            totalArtistsCount = snapshot.artistsCount,
            isLoading = false
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.PlaySong -> {
                isShuffleSessionActive = false
                _playerEvents.tryEmit(PlayerAction.PlaySong(action.song, action.playlist))
            }
            is HomeAction.PlayShuffleSession -> {
                isShuffleSessionActive = true
                val batch = if (_shuffleTracks.value.isNotEmpty()) _shuffleTracks.value else action.tracks
                _shuffleTracks.value = batch
                _playerEvents.tryEmit(PlayerAction.PlayShuffleSession(action.startSong, sessionBatch = batch))
            }
            is HomeAction.RefreshShuffleTracks -> {
                viewModelScope.launch {
                    val allSongs = songRepository.getAllSongs()
                    if (allSongs.isNotEmpty()) {
                        val currentIds = _shuffleTracks.value.map { it.id }.toSet()
                        val candidates = allSongs.filter { it.id !in currentIds }
                        val newBatch = if (candidates.size >= 14) {
                            candidates.shuffled().take(14)
                        } else if (allSongs.size <= 14) {
                            allSongs.shuffled()
                        } else {
                            (candidates.shuffled() + allSongs.filter { it.id in currentIds }.shuffled()).take(14)
                        }
                        _shuffleTracks.value = newBatch
                        appPreferences.setShuffleTrackIds(newBatch.map { it.id })
                    }
                }
            }
            is HomeAction.NextDailyGenre -> {
                _genreOffset.value += 1
            }
            is HomeAction.HandleSongAction -> {
                val handled = SongActionDispatcher.dispatch(
                    action = action.action,
                    song = action.song,
                    playerEvents = _playerEvents,
                    favoritesRepository = favoritesRepository,
                    scope = viewModelScope
                )
                if (!handled) {
                    when (action.action) {
                        SongAction.ADD_TO_PLAYLIST -> { /* Handled externally via NavigationOverlays playlist dialog */ }
                        SongAction.REMOVE_FROM_FAVORITES -> {
                            viewModelScope.launch { favoritesRepository.toggleFavorite(action.song.id) }
                        }
                        SongAction.GO_TO_ARTIST,
                        SongAction.GO_TO_ALBUM,
                        SongAction.GO_TO_FOLDER -> { /* Handled at AppNavigation routing layer */ }
                        SongAction.SONG_INFO -> { /* Handled locally via SongItemInfoSheet dialog */ }
                        SongAction.DELETE -> { /* Handled via system media delete request */ }
                        SongAction.PLAY,
                        SongAction.PLAY_NEXT,
                        SongAction.ADD_TO_QUEUE,
                        SongAction.TOGGLE_FAVORITE -> { /* Handled by SongActionDispatcher */ }
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            songRepository: SongRepository,
            historyRepository: HistoryRepository,
            favoritesRepository: FavoritesRepository,
            appPreferences: AppPreferences,
            playbackManager: PlaybackManager
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(
                        songRepository = songRepository,
                        historyRepository = historyRepository,
                        favoritesRepository = favoritesRepository,
                        appPreferences = appPreferences,
                        playbackManager = playbackManager
                    ) as T
                }
            }
    }
}
