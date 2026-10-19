package com.example.core.data.repository

import android.content.Context
import com.example.core.data.database.AppDatabase
import com.example.core.data.database.song.SongDao
import com.example.core.data.database.song.SongEntity
import com.example.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface SongRepository {
    val allSongsFlow: Flow<List<Song>>
    val playCountsFlow: Flow<Map<Long, Int>>
    suspend fun getAllSongs(): List<Song>
}

class SongRepositoryImpl(
    private val context: Context,
    private val songDao: SongDao = AppDatabase.getInstance(context).songDao()
) : SongRepository {

    override val allSongsFlow: Flow<List<Song>> = songDao.getAllSongsWithFavorites()
        .map { list -> list.map { it.toSong() } }
        .flowOn(Dispatchers.IO)

    override val playCountsFlow: Flow<Map<Long, Int>> = songDao.getAllPlayCounts()
        .map { list -> list.associate { it.songId to it.count } }
        .flowOn(Dispatchers.IO)

    override suspend fun getAllSongs(): List<Song> = withContext(Dispatchers.IO) {
        songDao.getAllSongsWithFavoritesDirect().map { it.toSong() }
    }
}
