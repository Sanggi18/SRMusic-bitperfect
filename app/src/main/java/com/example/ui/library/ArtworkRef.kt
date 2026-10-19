package com.example.ui.library

import android.net.Uri

data class ArtworkRef(
    val artworkUri: Any? = null,
    val dataPath: String? = null,
    val albumId: Long? = null,
    val dateModified: Long = 0L,
    val contentUri: Uri? = null
)
