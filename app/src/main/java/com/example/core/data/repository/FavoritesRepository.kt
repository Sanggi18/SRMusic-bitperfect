package com.example.core.data.repository

import android.content.Context
import com.example.core.data.database.AppDatabase
import com.example.core.data.database.song.FavoriteEntity
import com.example.core.data.database.song.SongDao
import com.example.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface FavoritesRepository {
    val favoriteSongsFlow: Flow<List<Song>>
    suspend fun toggleFavorite(songId: Long): Boolean
}

class FavoritesRepositoryImpl(
    private val songDao: SongDao
) : FavoritesRepository {

    constructor(context: Context) : this(AppDatabase.getInstance(context).songDao())

    override val favoriteSongsFlow: Flow<List<Song>> = songDao.getFavoriteSongs()
        .map { list -> list.map { it.toSong() } }
        .flowOn(Dispatchers.IO)

    override suspend fun toggleFavorite(songId: Long): Boolean = withContext(Dispatchers.IO) {
        val currentlyFav = songDao.isFavorite(songId)
        if (currentlyFav) {
            songDao.deleteFavorite(songId)
            false
        } else {
            songDao.insertFavorite(FavoriteEntity(songId = songId))
            true
        }
    }
}
