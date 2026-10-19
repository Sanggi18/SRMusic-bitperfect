package com.example.core.data.mediastore.tags

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import com.example.core.model.Song
import com.example.core.util.TimeUtils
import java.io.File
import android.util.LruCache

object ReleaseDateExtractor {

    private const val RELEASE_DATE_CACHE_CAPACITY = 1024

    // Menyimpan hasil ekstraksi tanggal rilis termasuk string kosong "" sebagai negative cache,
    // dengan kapasitas terbatas 1024 entri (LRU) agar penggunaan memori terkontrol dan entri usang otomatis terdepak.
    private val releaseDateCache =
        LruCache<Long, String>(RELEASE_DATE_CACHE_CAPACITY)

    fun getCachedReleaseDate(songId: Long): String? =
        releaseDateCache.get(songId)

    fun extractReleaseDateTag(context: Context, song: Song): String {
        releaseDateCache.get(song.id)?.let { return it }

        val computed = computeReleaseDateInternal(context, song)
        releaseDateCache.put(song.id, computed)
        return computed
    }

    private fun computeReleaseDateInternal(context: Context, song: Song): String {
        // 1. TagLib native (1x buka file untuk semua kandidat tag tanggal, sangat cepat & bebas Jaudiotagger)
        val tagDp = song.dataPath
        if (!tagDp.isNullOrBlank()) {
            val file = File(tagDp)
            if (file.isFile && file.canRead()) {
                val rawDate = TaglibHelper.getFirstProperty(
                    file,
                    "DATE",
                    "TDRC",
                    "TDRL",
                    "TDOR",
                    "TDAT",
                    "ORIGINALDATE",
                    "ORIGINALYEAR",
                    "YEAR",
                    "TYER",
                    "©day"
                )
                if (!rawDate.isNullOrBlank()) {
                    val formatted = TimeUtils.formatReleaseDateTag(rawDate, shortMonth = true)
                    if (!formatted.isNullOrBlank()) {
                        return formatted
                    }
                }
            }
        }

        // 2. MediaMetadataRetriever fallback
        val retriever = MediaMetadataRetriever()
        try {
            if (!tagDp.isNullOrBlank() && File(tagDp).exists()) {
                retriever.setDataSource(tagDp)
            } else {
                retriever.setDataSource(context, song.contentUri)
            }
            val date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            if (!date.isNullOrBlank()) {
                val formatted = TimeUtils.formatReleaseDateTag(date, shortMonth = true)
                if (!formatted.isNullOrBlank()) {
                    return formatted
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val yearMeta = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                if (!yearMeta.isNullOrBlank()) {
                    val formatted = TimeUtils.formatReleaseDateTag(yearMeta, shortMonth = true)
                    if (!formatted.isNullOrBlank()) {
                        return formatted
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        // 3. Fallback ke MediaStore song.year (tanpa I/O)
        song.year?.let { y ->
            if (y in 1900..2100) {
                return y.toString()
            }
        }

        // 4. Fallback ke timestamp file
        val fallback = TimeUtils.formatTimestampFallback(song)
        if (fallback.isNotBlank()) {
            return fallback
        }

        return ""
    }
}
