package com.example.lyrics

data class LyricWord(
    val startMs: Long,
    val text: String
)

data class LyricLine(
    val startMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList()
)

sealed interface LyricsContent {
    data class Synced(
        val lines: List<LyricLine>
    ) : LyricsContent

    data class Plain(
        val text: String
    ) : LyricsContent

    data object None : LyricsContent
}

data class LyricsDocument(
    val songId: Long,
    val content: LyricsContent
)

fun LyricsContent.toDocument(songId: Long): LyricsDocument? = when (this) {
    is LyricsContent.Synced -> LyricsDocument(songId = songId, content = this)
    is LyricsContent.Plain -> LyricsDocument(songId = songId, content = this)
    LyricsContent.None -> null
}
