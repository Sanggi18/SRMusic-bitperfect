package com.example.ui.lyrics.core

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.dp

internal const val LYRIC_SETTLE_MS = 300
internal const val LYRIC_LINE_SPACING_DP = 14

internal const val LINE_SCROLL_MIN_MS = 350L
internal const val LINE_SCROLL_MAX_MS = 500L
internal const val LINE_STAGGER_STEPS = 3
internal const val LINE_STAGGER_FRACTION = 0.06f

internal const val LYRIC_FONT_SIZE_SP = 30
internal const val LYRIC_LINE_HEIGHT_SP = 56

internal const val LYRIC_SCROLL_ANCHOR_FRACTION = 0.20f

internal const val WORD_RISE_DP = 2f
internal const val WORD_RISE_MIN_MS = 250f
internal const val WORD_RISE_MAX_MS = 700f
internal const val WORD_SETTLE_MS = 700f
internal const val WORD_LIFT_QUICK_MS = 150f
internal const val WORD_LIFT_HELD_MS = 900f
internal const val WORD_LIFT_FLOOR = 0.25f

internal const val GAP_DOT_COUNT = 3
internal const val GAP_ROW_HEIGHT_DP = 36

internal const val GAP_MIN_DURATION_MS = 5_000L
internal const val INITIAL_GAP_MIN_DURATION_MS = 5_000L

internal const val GAP_DOT_REST_ALPHA = 0.25f
internal const val GAP_REST_SCALE = 0.80f

internal val LYRIC_EASING = CubicBezierEasing(0.41f, 0f, 0.12f, 0.99f)

internal val LYRIC_LINE_ALPHA = floatArrayOf(1f, 0.80f, 0.70f, 0.58f, 0.46f)

/*
 * Gaussian blur levels: baris aktif tajam, makin jauh dari baris aktif makin blur.
 */
internal val LYRIC_LINE_BLUR = arrayOf(0.dp, 1.dp, 1.dp, 1.7.dp, 2.4.dp)
