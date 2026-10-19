package com.example.core.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.data.database.history.PlayCountEntity
import com.example.core.data.database.history.RecentPlayDao
import com.example.core.data.database.history.RecentPlayEntity
import com.example.core.data.database.playlist.PlaylistDao
import com.example.core.data.database.playlist.PlaylistEntity
import com.example.core.data.database.playlist.PlaylistSongCrossRef
import com.example.core.data.database.song.FavoriteEntity
import com.example.core.data.database.song.SongDao
import com.example.core.data.database.song.SongEntity

@Database(
    entities = [
        SongEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        PlayCountEntity::class,
        RecentPlayEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun recentPlayDao(): RecentPlayDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "srmusic_database"
                )
                    .fallbackToDestructiveMigrationFrom(1, 2, 3)
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
