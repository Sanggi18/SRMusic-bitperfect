package com.example.ui.lyrics.core

import com.example.lyrics.LyricLine

internal sealed interface LyricDisplayItem {
    val key: String

    data class LineItem(
        val sourceIndex: Int,
        val line: LyricLine
    ) : LyricDisplayItem {
        override val key: String get() = "line_${sourceIndex}_${line.startMs}"
    }

    data class GapItem(
        val startTimeMs: Long,
        val endTimeMs: Long
    ) : LyricDisplayItem {
        override val key: String get() = "gap_${startTimeMs}_${endTimeMs}"
    }
}

internal data class LyricScrollPlan(
    val targetDisplayIndex: Int
)

internal data class LyricScrollRun(
    val id: Int,
    val deltaPx: Float,
    val durationMs: Long,
    val focusDisplayIndex: Int
)

internal data class FloatingWordSegment(
    val visualLine: Int,
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
    val lift: Float
)
