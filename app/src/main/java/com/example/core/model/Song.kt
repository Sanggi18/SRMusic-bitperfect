package com.example.core.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // in milliseconds
    val albumId: Long,
    val contentUri: Uri,
    val artworkUri: Uri,
    val trackNumber: Int? = null,
    val year: Int? = null,
    val size: Long? = null,
    val displayName: String? = null,
    val relativePath: String? = null,
    val dataPath: String? = null,
    val dateAdded: Long? = null,
    val dateModified: Long? = null,
    val genre: String? = null
)

