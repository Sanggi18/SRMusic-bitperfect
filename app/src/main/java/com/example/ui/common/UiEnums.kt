package com.example.ui.common

enum class ArtworkShape {
    ROUNDED_SQUARE,
    CIRCLE
}

enum class ArtworkPlaceholderType {
    MUSIC,
    ARTIST,
    ALBUM,
    FOLDER,
    AUDIO_FILE
}

enum class YearSortOrder {
    DESCENDING, // Newest first (e.g. 2025 -> 1990)
    ASCENDING   // Oldest first (e.g. 1990 -> 2025)
}

enum class ItemPosition {
    TOP,
    MIDDLE,
    BOTTOM,
    SINGLE
}
