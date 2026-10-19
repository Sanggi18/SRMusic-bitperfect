package com.example.core.util

import android.content.Context
import com.example.core.model.Song
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {
    private val MONTH_NAMES_FULL = arrayOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    private val MONTH_NAMES_SHORT = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    )

    fun formatDuration(millis: Long): String {
        if (millis <= 0L) return "00:00"
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats date string (e.g. "2022-08-22", "2022-08-16 08:33:12 UTC", "2026/02/12", "2024", "2023-05")
     * into readable Indonesian date format: "22 Agu", "12 Feb", "1 Mar", "Mei 2023", or "2024".
     */
    fun formatReleaseDateTag(rawTag: String?, shortMonth: Boolean = true): String? {
        if (rawTag.isNullOrBlank()) return null
        val trimmed = rawTag.trim()

        // 1. Match YYYY-MM-DD or YYYY-MM-DDTHH:mm:ss... or YYYY/MM/DD
        val isoRegex = Regex("""^(\d{4})[-/.](0?[1-9]|1[0-2])[-/.](0?[1-9]|[12]\d|3[01])""")
        val isoMatch = isoRegex.find(trimmed)
        if (isoMatch != null) {
            val month = isoMatch.groupValues[2].toIntOrNull() ?: return null
            val day = isoMatch.groupValues[3].toIntOrNull() ?: return null
            val monthName = getIndonesianMonth(month, shortMonth)
            return "$day $monthName"
        }

        // 2. Match DD-MM-YYYY or DD/MM/YYYY
        val dmyRegex = Regex("""^(0?[1-9]|[12]\d|3[01])[-/.](0?[1-9]|1[0-2])[-/.](\d{4})""")
        val dmyMatch = dmyRegex.find(trimmed)
        if (dmyMatch != null) {
            val day = dmyMatch.groupValues[1].toIntOrNull() ?: return null
            val month = dmyMatch.groupValues[2].toIntOrNull() ?: return null
            val monthName = getIndonesianMonth(month, shortMonth)
            return "$day $monthName"
        }

        // 3. Match YYYY-MM or YYYY/MM
        val ymRegex = Regex("""^(\d{4})[-/.](0?[1-9]|1[0-2])$""")
        val ymMatch = ymRegex.find(trimmed)
        if (ymMatch != null) {
            val year = ymMatch.groupValues[1]
            val month = ymMatch.groupValues[2].toIntOrNull() ?: return null
            val monthName = getIndonesianMonth(month, shortMonth)
            return "$monthName $year"
        }

        // 4. Match 4-digit Year (e.g. "2024", "2019", "1998")
        val yearRegex = Regex("""\b(19\d{2}|20\d{2})\b""")
        val yearMatch = yearRegex.find(trimmed)
        if (yearMatch != null) {
            return yearMatch.value
        }

        return null
    }

    fun getIndonesianMonth(month1to12: Int, shortMonth: Boolean = true): String {
        val index = (month1to12 - 1).coerceIn(0, 11)
        return if (shortMonth) MONTH_NAMES_SHORT[index] else MONTH_NAMES_FULL[index]
    }

    /**
     * Fallback to timestamp if raw tag is missing.
     */
    fun formatTimestampFallback(song: Song, shortMonth: Boolean = true): String {
        val da = song.dateAdded
        val dm = song.dateModified
        val timestamp = when {
            da != null && da > 0L -> da
            dm != null && dm > 0L -> dm
            else -> 0L
        }

        if (timestamp > 0L) {
            val millis = if (timestamp < 100_000_000_000L) timestamp * 1000L else timestamp
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = cal.get(Calendar.MONTH) + 1
            val monthName = getIndonesianMonth(month, shortMonth)
            return "$day $monthName"
        }

        return ""
    }

    /**
     * Mengambil tanggal rilis dari cache memori atau fallback MediaStore secara instan
     * TANPA membuka file audio di Main Thread.
     */
    fun formatQuickSongReleaseDate(song: Song, shortMonth: Boolean = true): String {
        com.example.core.data.mediastore.tags.ReleaseDateExtractor.getCachedReleaseDate(song.id)?.let {
            return it
        }
        val y = song.year
        if (y != null && y in 1900..2100) {
            return y.toString()
        }
        return formatTimestampFallback(song, shortMonth)
    }

    /**
     * Formats song release date into 3-letter month format without year: "12 Feb", "22 Agu", "1 Mar", or year "2024".
     */
    fun formatSongReleaseDate(context: Context, song: Song, shortMonth: Boolean = true): String {
        // 1. Try file metadata tag first
        val tagDate = com.example.core.data.mediastore.tags.ReleaseDateExtractor.extractReleaseDateTag(
            context,
            song
        )
        if (tagDate.isNotBlank()) {
            return tagDate
        }

        // 2. Try MediaStore year if valid
        val y = song.year
        if (y != null && y in 1900..2100) {
            return y.toString()
        }

        // 3. Fallback to timestamp if available
        val fallback = formatTimestampFallback(song, shortMonth)
        if (fallback.isNotBlank()) {
            return fallback
        }

        return ""
    }
}
