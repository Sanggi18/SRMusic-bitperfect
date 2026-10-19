package com.example

import android.app.Application
import com.example.core.data.preferences.AppPreferences
import com.example.core.data.preferences.EqualizerPreferences
import com.example.core.data.preferences.LibraryPreferences
import com.example.core.data.preferences.ThemePreferences
import com.example.core.data.repository.FavoritesRepository
import com.example.core.data.repository.FavoritesRepositoryImpl
import com.example.core.data.repository.HistoryRepository
import com.example.core.data.repository.HistoryRepositoryImpl
import com.example.core.data.mediastore.LibraryRepository
import com.example.core.data.mediastore.LibraryRepositoryImpl
import com.example.core.playback.PlaybackManager
import com.example.core.data.repository.PlaylistRepository
import com.example.core.data.repository.PlaylistRepositoryImpl
import com.example.core.data.repository.SongRepository
import com.example.core.data.repository.SongRepositoryImpl

class SRMusicApp : Application() {
    val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepositoryImpl(this)
    }

    val songRepository: SongRepository by lazy {
        SongRepositoryImpl(this)
    }

    val libraryRepository: LibraryRepository by lazy {
        LibraryRepositoryImpl(this)
    }

    val historyRepository: HistoryRepository by lazy {
        HistoryRepositoryImpl(this)
    }

    val playbackManager: PlaybackManager by lazy {
        PlaybackManager(this, historyRepository, songRepository, equalizerPreferences)
    }

    val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepositoryImpl(this)
    }

    val appPreferences: AppPreferences by lazy {
        AppPreferences(this)
    }

    val themePreferences: ThemePreferences by lazy {
        ThemePreferences(this)
    }

    val equalizerPreferences: EqualizerPreferences by lazy {
        EqualizerPreferences(this)
    }

    val libraryPreferences: LibraryPreferences by lazy {
        LibraryPreferences(this)
    }

    /**
     * Flag cold start proses: hanya bernilai true jika splash perakitan logo
     * sudah pernah ditampilkan dalam siklus hidup proses aplikasi saat ini.
     */
    var hasShownColdStartSplash: Boolean = false
}
