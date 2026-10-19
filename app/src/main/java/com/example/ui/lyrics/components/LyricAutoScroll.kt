package com.example.ui.lyrics.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.withFrameNanos
import com.example.ui.lyrics.core.LYRIC_EASING
import com.example.ui.lyrics.core.LYRIC_SCROLL_ANCHOR_FRACTION
import com.example.ui.lyrics.core.LyricDisplayItem
import com.example.ui.lyrics.core.LyricScrollRun
import kotlinx.coroutines.yield
import kotlin.math.abs

@Composable
internal fun LyricAutoScrollEffects(
    listState: LazyListState,
    displayItems: List<LyricDisplayItem>,
    activeDisplayIndex: Int,
    pinnedMode: Boolean,
    onPinnedModeChange: (Boolean) -> Unit,
    scrollRun: LyricScrollRun,
    onScrollRunChange: (LyricScrollRun) -> Unit,
    scrollElapsedMs: MutableFloatState,
) {
    LaunchedEffect(scrollRun.id) {
        if (scrollRun.id == 0) {
            scrollElapsedMs.floatValue = 0f
            return@LaunchedEffect
        }

        scrollElapsedMs.floatValue = 0f

        val startedAt = withFrameNanos { it }

        while (true) {
            val now = withFrameNanos { it }
            val elapsed = (now - startedAt).coerceAtLeast(0L) / 1_000_000f

            scrollElapsedMs.floatValue = elapsed.coerceAtMost(scrollRun.durationMs.toFloat())

            if (elapsed >= scrollRun.durationMs.toFloat()) break
        }
    }

    LaunchedEffect(activeDisplayIndex, pinnedMode) {
        if (pinnedMode ||
            activeDisplayIndex < 0 ||
            activeDisplayIndex >= displayItems.size
        ) {
            return@LaunchedEffect
        }

        val layoutInfo = listState.layoutInfo
        val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset)
            .coerceAtLeast(1)
        val anchorPx = viewportHeight * LYRIC_SCROLL_ANCHOR_FRACTION

        val activeItem = layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == activeDisplayIndex }

        if (activeItem != null) {
            if (activeItem.offset >= anchorPx) {
                onPinnedModeChange(true)
            }
        } else if (activeDisplayIndex > 0) {
            onPinnedModeChange(true)
        }
    }

    // Fase 2: pin baris AKTIF di 20% viewport, SINKRON dengan highlight.
    // Scroll mulai BARENGAN dengan ganti highlight, tidak mendahului (fix delay).
    LaunchedEffect(activeDisplayIndex, pinnedMode) {
        val targetIndex = activeDisplayIndex

        if (!pinnedMode || targetIndex < 0 || targetIndex >= displayItems.size) {
            return@LaunchedEffect
        }

        val durationMs = 300

        yield()

        val layoutInfo = listState.layoutInfo
        val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset)
            .coerceAtLeast(1)
        val anchorPx = viewportHeight * LYRIC_SCROLL_ANCHOR_FRACTION

        val targetItem = layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == targetIndex }

        if (targetItem == null) {
            // Target off-screen: lompat langsung ke anchor, satu tahap saja (responsif).
            listState.animateScrollToItem(
                index = targetIndex,
                scrollOffset = -anchorPx.toInt(),
            )
            return@LaunchedEffect
        }

        val deltaPx = targetItem.offset - anchorPx

        if (abs(deltaPx) <= 1f) return@LaunchedEffect

        onScrollRunChange(
            LyricScrollRun(
                id = scrollRun.id + 1,
                deltaPx = deltaPx,
                durationMs = durationMs.toLong(),
                focusDisplayIndex = targetIndex,
            )
        )

        listState.animateScrollBy(
            value = deltaPx,
            animationSpec = tween(durationMillis = durationMs, easing = LYRIC_EASING),
        )
    }
}
