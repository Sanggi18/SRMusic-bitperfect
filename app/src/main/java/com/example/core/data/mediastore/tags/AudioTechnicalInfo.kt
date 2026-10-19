package com.example.core.data.mediastore.tags

data class AudioTechnicalInfo(
    val format: String,
    val bitDepth: String,
    val sampleRate: String,
    val bitrate: String,
    val channels: String,
    val fileSizeFormatted: String = ""
)
