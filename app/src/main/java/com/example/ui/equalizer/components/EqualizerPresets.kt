package com.example.ui.equalizer.components

import kotlin.math.roundToInt

val templatePresets = listOf(
    "Jazz", "Pop", "Rock", "Classical", "Acoustic", "Electronic", "Bass & Treble",
    "Hip Hop", "Dance", "R&B", "Metal", "Lounge", "Spoken Word", "Deep Bass", "Bright Vocal"
)

val presetBands = mapOf(
    "Flat" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    "Bass boost" to listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
    "Vocal boost" to listOf(-2f, -1f, 0f, 2f, 4f, 4f, 3f, 1f, 0f, -1f),
    "Treble boost" to listOf(0f, 0f, 0f, 0f, 0f, 1f, 2f, 4f, 6f, 6f),
    "Bass & Treble" to listOf(5f, 4f, 2f, 0f, -1f, 0f, 2f, 4f, 5f, 5f),
    "Rock" to listOf(5f, 3f, 1f, 0f, -1f, 1f, 3f, 4f, 5f, 4f),
    "Pop" to listOf(1f, 2f, 3f, 2f, 0f, -1f, 1f, 2f, 3f, 3f),
    "Jazz" to listOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f),
    "Classical" to listOf(4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 3f, 4f),
    "Acoustic" to listOf(3f, 2f, 1f, 1f, 2f, 2f, 3f, 3f, 2f, 1f),
    "Electronic" to listOf(6f, 5f, 2f, 0f, -2f, 2f, 1f, 3f, 5f, 6f),
    "Hip Hop" to listOf(5f, 4f, 1f, 2f, -1f, -1f, 1f, 0f, 2f, 3f),
    "Dance" to listOf(6f, 5f, 2f, 0f, 0f, 2f, 3f, 4f, 4f, 3f),
    "R&B" to listOf(4f, 5f, 3f, 1f, -1f, 1f, 2f, 2f, 3f, 3f),
    "Metal" to listOf(5f, 4f, 2f, -1f, -3f, 0f, 3f, 5f, 6f, 5f),
    "Lounge" to listOf(-1f, 1f, 2f, 3f, 1f, 0f, -1f, 1f, 2f, 1f),
    "Spoken Word" to listOf(-4f, -2f, 0f, 2f, 5f, 5f, 4f, 2f, 0f, -2f),
    "Deep Bass" to listOf(7f, 6f, 4f, 1f, 0f, 0f, 0f, 0f, 0f, 0f),
    "Bright Vocal" to listOf(-3f, -2f, -1f, 1f, 4f, 5f, 4f, 3f, 2f, 1f)
)

fun Float.formatDb(): String {
    val rounded = (this * 10).roundToInt() / 10f
    return if (rounded > 0) "+${rounded} dB" else if (rounded < 0) "${rounded} dB" else "0 dB"
}
