package com.example.core.data.mediastore.tags

/**
 * Minimal genre utilities. No genre list, no numeric ID3 mapping.
 * Modern tags store genre as text; MediaStore returns it as-is.
 */
object GenreUtils {

    fun isValidGenre(genre: String?): Boolean {
        if (genre.isNullOrBlank()) return false
        val lower = genre.trim().lowercase()
        return lower != "<unknown>" &&
            lower != "unknown" &&
            lower != "unknown genre" &&
            genre.trim().length in 2..50
    }

    fun cleanGenre(genre: String): String {
        return genre.trim().trim('"', '\'', ';', '/', '\\', '\u0000').trim()
    }
}
