package com.example.lyrics

import com.example.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class LyricsRepository {

    companion object {
        private const val MAX_CACHE_ENTRIES = 20
    }

    private data class CacheKey(
        val songId: Long,
        val audioSize: Long,
        val audioModified: Long,
        val sidecarModified: Long
    )

    private val embeddedReader = EmbeddedLyricsReader()
    private val sidecarReader = SidecarLyricsReader()
    private val mutex = Mutex()

    private val cache = object : LinkedHashMap<CacheKey, LyricsDocument>(
        MAX_CACHE_ENTRIES + 1,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<CacheKey, LyricsDocument>
        ): Boolean {
            return size > MAX_CACHE_ENTRIES
        }
    }

    suspend fun getCached(song: Song): LyricsDocument? = withContext(Dispatchers.IO) {
        mutex.withLock {
            cache[buildCacheKey(song)]
        }
    }

    suspend fun load(song: Song): LyricsDocument = withContext(Dispatchers.IO) {
        val key = buildCacheKey(song)

        mutex.withLock { cache[key] }?.let { return@withContext it }

        val result = loadUncached(song)

        return@withContext mutex.withLock {
            cache[key] ?: run {
                cache[key] = result
                result
            }
        }
    }

    private fun loadUncached(song: Song): LyricsDocument {
        val dataPath = song.dataPath
        if (dataPath.isNullOrBlank()) {
            return LyricsDocument(
                songId = song.id,
                content = LyricsContent.None
            )
        }

        val file = File(dataPath)
        if (!file.isFile || !file.canRead()) {
            return LyricsDocument(
                songId = song.id,
                content = LyricsContent.None
            )
        }

        embeddedReader.read(song.id, dataPath)?.let { return it }
        sidecarReader.read(song.id, dataPath)?.let { return it }

        return LyricsDocument(
            songId = song.id,
            content = LyricsContent.None
        )
    }

    private fun buildCacheKey(song: Song): CacheKey {
        val dp = song.dataPath
        val audio = if (!dp.isNullOrBlank()) File(dp) else null
        val sidecar = if (audio?.isFile == true) sidecarReader.findSidecar(audio) else null

        return CacheKey(
            songId = song.id,
            audioSize = if (audio?.isFile == true) audio.length() else (song.size ?: 0L),
            audioModified = if (audio?.isFile == true) audio.lastModified() else (song.dateModified ?: 0L),
            sidecarModified = sidecar?.lastModified() ?: 0L
        )
    }
}
