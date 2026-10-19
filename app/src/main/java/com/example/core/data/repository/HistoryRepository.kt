package com.example.core.data.repository

import android.content.Context
import com.example.core.data.database.AppDatabase
import com.example.core.data.database.history.RecentPlayDao
import com.example.core.data.database.history.RecentPlayEntity
import com.example.core.data.database.song.SongDao
import com.example.core.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

interface HistoryRepository {
    val historySongIdsFlow: StateFlow<List<Long>>
    val mostPlayedSongsFlow: Flow<List<Song>>
    val totalPlayCountFlow: Flow<Int>
    val totalListenedSecondsFlow: StateFlow<Long>
    val allPlayCountsFlow: Flow<Map<Long, Int>>
    fun addListenedSeconds(seconds: Long)
    suspend fun recordSongPlay(songId: Long)
    suspend fun clearHistory()
}

class HistoryRepositoryImpl(
    private val context: Context,
    private val songDao: SongDao = AppDatabase.getInstance(context).songDao(),
    private val recentPlayDao: RecentPlayDao = AppDatabase.getInstance(context).recentPlayDao()
) : HistoryRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val historySongIdsFlow: StateFlow<List<Long>> = recentPlayDao.observeRecent()
        .map { list ->
            val cutoff = System.currentTimeMillis() - 86400000L
            list.filter { it.playedAt >= cutoff }.take(50).map { it.songId }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val statsPrefs = context.getSharedPreferences("srmusic_stats_prefs", Context.MODE_PRIVATE)
    private val _totalListenedSecondsFlow = MutableStateFlow(statsPrefs.getLong("total_listened_seconds", 0L))
    override val totalListenedSecondsFlow: StateFlow<Long> = _totalListenedSecondsFlow.asStateFlow()

    override val mostPlayedSongsFlow: Flow<List<Song>> = songDao.getMostPlayedSongs(20)
        .map { list -> list.map { it.toSong() } }
        .flowOn(Dispatchers.IO)

    override val totalPlayCountFlow: Flow<Int> = songDao.getTotalPlayCount()
        .flowOn(Dispatchers.IO)

    override val allPlayCountsFlow: Flow<Map<Long, Int>> = songDao.getAllPlayCounts()
        .map { list -> list.associate { it.songId to it.count } }
        .flowOn(Dispatchers.IO)

    override fun addListenedSeconds(seconds: Long) {
        if (seconds <= 0) return
        val current = _totalListenedSecondsFlow.value
        val updated = current + seconds
        statsPrefs.edit().putLong("total_listened_seconds", updated).apply()
        _totalListenedSecondsFlow.value = updated
    }

    override suspend fun recordSongPlay(songId: Long): Unit = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        // 1. Increment play count in Room for Most Played
        songDao.incrementPlayCount(songId, now)

        // 2. Record in Room recent_plays
        recentPlayDao.upsert(RecentPlayEntity(songId, now))
        recentPlayDao.deleteOlderThan(now - 86400000L)
        recentPlayDao.trimToMax50()
    }

    override suspend fun clearHistory(): Unit = withContext(Dispatchers.IO) {
        recentPlayDao.clearAll()
        songDao.clearPlayCounts()
    }
}
