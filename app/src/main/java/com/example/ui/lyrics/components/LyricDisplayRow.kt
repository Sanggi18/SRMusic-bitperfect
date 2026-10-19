package com.example.ui.lyrics.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.common.theme.springPress
import com.example.ui.lyrics.core.LINE_STAGGER_FRACTION
import com.example.ui.lyrics.core.LINE_STAGGER_STEPS
import com.example.ui.lyrics.core.LYRIC_EASING
import com.example.ui.lyrics.core.LYRIC_LINE_ALPHA
import com.example.ui.lyrics.core.LYRIC_LINE_BLUR
import com.example.ui.lyrics.core.LYRIC_LINE_SPACING_DP
import com.example.ui.lyrics.core.LYRIC_SETTLE_MS
import com.example.ui.lyrics.core.LyricDisplayItem
import com.example.ui.lyrics.core.LyricScrollRun
import com.example.ui.lyrics.core.findActiveWordIndex
import kotlin.math.abs

@Composable
internal fun LyricDisplayRow(
    item: LyricDisplayItem,
    displayIndex: Int,
    activeDisplayIndex: Int,
    focusDisplayIndex: Int,
    scrollRun: LyricScrollRun,
    scrollElapsedMs: Float,
    lyricClock: MutableLongState,
    activeColor: Color,
    inactiveColor: Color,
    accentColor: Color,
    onSeek: (Long) -> Unit,
) {
    val isActive = displayIndex == activeDisplayIndex

    val distanceFromFocus = if (focusDisplayIndex >= 0) {
        abs(displayIndex - focusDisplayIndex)
    } else {
        LYRIC_LINE_ALPHA.lastIndex
    }

    val alphaIndex = distanceFromFocus.coerceAtMost(LYRIC_LINE_ALPHA.lastIndex)

    val rowAlpha by animateFloatAsState(
        targetValue = LYRIC_LINE_ALPHA[alphaIndex],
        animationSpec = tween(durationMillis = LYRIC_SETTLE_MS, easing = LYRIC_EASING),
        label = "lyric-row-alpha",
    )

    val rowScale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.98f,
        animationSpec = tween(durationMillis = LYRIC_SETTLE_MS, easing = LYRIC_EASING),
        label = "lyric-row-scale",
    )

    /*
     * GAUSSIAN BLUR: baris aktif tajam, baris non-aktif makin blur sesuai jarak.
     */
    val blurDistance = if (activeDisplayIndex >= 0) {
        abs(displayIndex - activeDisplayIndex)
    } else {
        0
    }

    val rowBlur by animateDpAsState(
        targetValue = when {
            item is LyricDisplayItem.GapItem -> 0.dp
            isActive -> 0.dp
            else -> LYRIC_LINE_BLUR[blurDistance.coerceAtMost(LYRIC_LINE_BLUR.lastIndex)]
        },
        animationSpec = tween(durationMillis = LYRIC_SETTLE_MS, easing = LYRIC_EASING),
        label = "lyric-row-blur",
    )

    val run = scrollRun

    val behind = if (run.deltaPx >= 0f) {
        displayIndex - run.focusDisplayIndex
    } else {
        run.focusDisplayIndex - displayIndex
    }

    val staggerDelayMs = behind.coerceIn(0, LINE_STAGGER_STEPS).toFloat() *
        LINE_STAGGER_FRACTION * run.durationMs

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = rowAlpha
                scaleX = rowScale
                scaleY = rowScale
                transformOrigin = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0.5f)
                translationY = if (staggerDelayMs <= 0f || run.id == 0) {
                    0f
                } else {
                    val elapsed = scrollElapsedMs
                    val wholeProgress = (elapsed /
                        run.durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    val delayedProgress = ((elapsed - staggerDelayMs) /
                        run.durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    run.deltaPx * (LYRIC_EASING.transform(wholeProgress) -
                        LYRIC_EASING.transform(delayedProgress))
                }
            }
            .blur(radius = rowBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded),
    ) {
        when (item) {
            is LyricDisplayItem.LineItem -> {
                val activeWordIndex = if (isActive) {
                    findActiveWordIndex(line = item.line, positionMs = lyricClock.longValue)
                } else {
                    -1
                }

                val lineInteractionSource = remember { MutableInteractionSource() }

                LyricLineContent(
                    line = item.line,
                    activeWordIndex = activeWordIndex,
                    isActive = isActive,
                    lyricClock = lyricClock,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RectangleShape)
                        .springPress(lineInteractionSource)
                        .clickable(
                            interactionSource = lineInteractionSource,
                            indication = null,
                        ) { onSeek(item.line.startMs) }
                        .padding(vertical = LYRIC_LINE_SPACING_DP.dp / 2),
                )
            }

            is LyricDisplayItem.GapItem -> {
                LyricGapContent(
                    gap = item,
                    lyricClock = lyricClock,
                    isActive = isActive,
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
