package com.example.core.data.mediastore.tags

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.LruCache
import com.example.core.model.Song
import com.kyant.taglib.TagLib
import java.io.File
import java.util.Locale

data class AudioStreamProps(
    val format: String,
    val sampleRateHz: Int?,
    val bitrateKbps: Int?,
    val bitDepth: Int? = null
) {
    fun formatSampleRate(): String? {
        val hz = sampleRateHz ?: return null
        val khz = hz / 1000.0
        return if (khz % 1.0 == 0.0) {
            "${khz.toInt()} kHz"
        } else {
            String.format(Locale.US, "%.1f kHz", khz)
        }
    }
}

object TaglibHelper {

    /**
     * Membuka file 1 kali saja dan mengembalikan seluruh propertyMap dari TagLib.
     */
    fun getPropertyMap(file: File): Map<String, Array<String>>? {
        return try {
            if (!file.exists() || !file.canRead()) return null
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                TagLib.getMetadata(pfd.dup().detachFd())?.propertyMap
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Mengambil nilai pertama yang tidak kosong dari daftar kandidat key
     * hanya dengan 1 kali buka file.
     */
    fun getFirstProperty(file: File, vararg keys: String): String? {
        val map = getPropertyMap(file) ?: return null
        for (targetKey in keys) {
            val direct = map[targetKey]?.firstOrNull()?.takeIf { it.isNotBlank() }
            if (direct != null) return direct

            val caseInsensitive = map.entries
                .firstOrNull { it.key.equals(targetKey, ignoreCase = true) }
                ?.value
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
            if (caseInsensitive != null) return caseInsensitive
        }
        return null
    }

    /**
     * Ambil lirik embedded (LYRICS / UNSYNCEDLYRICS / SYLT) dalam 1 kali buka file.
     */
    fun getLyrics(file: File): String? {
        return getFirstProperty(
            file,
            "LYRICS",
            "UNSYNCEDLYRICS",
            "UNSYNCED LYRICS",
            "SYLT",
            "©lyr"
        )
    }

    /** Ambil artwork bytes pertama via taglib, null jika gagal. */
    fun getArtworkBytes(file: File): ByteArray? {
        return try {
            if (!file.exists() || !file.canRead()) return null
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                TagLib.getPictures(pfd.dup().detachFd())
                    .firstOrNull()?.data
                    ?.takeIf { it.isNotEmpty() }
            }
        } catch (_: Exception) {
            null
        }
    }

    private val streamPropsCache = LruCache<Long, AudioStreamProps>(64)

    fun getAudioStreamProps(song: Song, context: Context): AudioStreamProps {
        streamPropsCache.get(song.id)?.let { return it }

        var sampleRate: Int? = null
        var bitrate: Int? = null
        var format = song.displayName?.let { dn ->
            if (dn.contains('.')) dn.substringAfterLast('.').uppercase() else null
        } ?: song.dataPath?.let { dp ->
            if (dp.contains('.')) dp.substringAfterLast('.').uppercase() else null
        } ?: "MP3"
        if (format.contains("AAC", ignoreCase = true) || format.contains("MP4", ignoreCase = true)) {
            format = "M4A"
        }

        val retriever = MediaMetadataRetriever()
        try {
            val dp = song.dataPath
            if (!dp.isNullOrBlank() && File(dp).exists()) {
                retriever.setDataSource(dp)
            } else if (song.contentUri != android.net.Uri.EMPTY) {
                retriever.setDataSource(context, song.contentUri)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
            }
            bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.let { it / 1000 }
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        if (bitrate == null && song.duration > 0 && (song.size ?: 0L) > 0) {
            bitrate = (((song.size ?: 0L) * 8) / song.duration).toInt()
        }

        val result = AudioStreamProps(
            format = format,
            sampleRateHz = sampleRate,
            bitrateKbps = bitrate
        )
        streamPropsCache.put(song.id, result)
        return result
    }
}
