package com.example.core.data.mediastore

import android.content.Context
import com.example.core.data.preferences.LibraryPreferences
import com.example.core.data.database.AppDatabase
import com.example.core.data.database.song.SongDao
import com.example.core.data.mediastore.scan.LibraryScanOptions
import com.example.core.data.mediastore.scan.MediaStoreScanner

interface LibraryRepository {
    val libraryPreferences: LibraryPreferences
    fun getScanOptions(): LibraryScanOptions
    fun updateScanOptions(options: LibraryScanOptions)
    suspend fun syncLibrary(
        options: LibraryScanOptions = getScanOptions(),
        onProgress: ((count: Int) -> Unit)? = null
    ): SyncResult
}

class LibraryRepositoryImpl(
    private val context: Context,
    private val songDao: SongDao = AppDatabase.getInstance(context).songDao(),
    private val scanner: MediaStoreScanner = MediaStoreScanner(context),
    override val libraryPreferences: LibraryPreferences = LibraryPreferences(context)
) : LibraryRepository {

    private val syncEngine = LibrarySyncEngine(songDao, scanner)

    override fun getScanOptions(): LibraryScanOptions = libraryPreferences.getScanOptions()

    override fun updateScanOptions(options: LibraryScanOptions) {
        libraryPreferences.updateScanOptions(options)
    }

    override suspend fun syncLibrary(
        options: LibraryScanOptions,
        onProgress: ((count: Int) -> Unit)?
    ): SyncResult {
        return syncEngine.reconcile(options, onProgress)
    }
}
