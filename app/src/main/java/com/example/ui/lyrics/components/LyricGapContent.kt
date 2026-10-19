package com.example.ui.lyrics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.lyrics.core.GAP_DOT_COUNT
import com.example.ui.lyrics.core.GAP_DOT_REST_ALPHA
import com.example.ui.lyrics.core.GAP_REST_SCALE
import com.example.ui.lyrics.core.GAP_ROW_HEIGHT_DP
import com.example.ui.lyrics.core.LYRIC_EASING
import com.example.ui.lyrics.core.LyricDisplayItem

@Composable
internal fun LyricGapContent(
    gap: LyricDisplayItem.GapItem,
    lyricClock: MutableLongState,
    isActive: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val rowHeight by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isActive) 400 else 350,
            easing = LYRIC_EASING,
        ),
        label = "lyric-gap-height-factor",
    )

    val dotAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = LYRIC_EASING),
        label = "lyric-gap-alpha",
    )

    Box(
        modifier = modifier.height(GAP_ROW_HEIGHT_DP.dp * rowHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(width = 48.dp, height = 12.dp)
                .graphicsLayer { alpha = dotAlpha },
        ) {
            val now = lyricClock.longValue

            val progress = ((now - gap.startTimeMs).toFloat() /
                (gap.endTimeMs - gap.startTimeMs).coerceAtLeast(1L).toFloat())
                .coerceIn(0f, 1f)

            val radius = 4.dp.toPx()
            val dotSpacing = 6.dp.toPx()
            val dotStride = radius * 2f + dotSpacing

            repeat(GAP_DOT_COUNT) { dotIndex ->
                val lit = (progress * GAP_DOT_COUNT - dotIndex).coerceIn(0f, 1f)

                val alpha = GAP_DOT_REST_ALPHA + (1f - GAP_DOT_REST_ALPHA) * lit
                val dotScale = GAP_REST_SCALE + (1f - GAP_REST_SCALE) * lit

                drawCircle(
                    color = accentColor.copy(alpha = alpha),
                    radius = radius * dotScale,
                    center = Offset(
                        x = radius + dotIndex * dotStride,
                        y = size.height / 2f,
                    ),
                )
            }
        }
    }
}
