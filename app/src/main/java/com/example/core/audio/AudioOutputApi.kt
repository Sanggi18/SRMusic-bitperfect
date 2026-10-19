package com.example.core.audio

enum class AudioOutputApi(val label: String) {
    AAUDIO("AAudio"),
    OPENSL_ES("OpenSL ES"),
    AUDIOTRACK("AudioTrack");

    companion object {
        val AAUDIOTRACK: AudioOutputApi get() = AUDIOTRACK
    }
}
