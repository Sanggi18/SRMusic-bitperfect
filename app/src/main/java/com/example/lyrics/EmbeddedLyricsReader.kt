package com.example.lyrics

import com.example.core.data.mediastore.tags.TaglibHelper
import java.io.File

class EmbeddedLyricsReader {

    fun read(songId: Long, dataPath: String): LyricsDocument? {
        val file = File(dataPath)
        if (!file.isFile || !file.canRead()) return null

        val rawLyrics = TaglibHelper.getLyrics(file)
        if (rawLyrics.isNullOrBlank()) return null

        val parsed = LrcParser.parse(rawLyrics)
        return parsed.toDocument(songId)
    }
}
