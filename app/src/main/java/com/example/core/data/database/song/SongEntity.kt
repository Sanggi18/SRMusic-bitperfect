package com.example.core.data.database.song

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val albumId: Long,
    val contentUriString: String,
    val artworkUriString: String,
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
