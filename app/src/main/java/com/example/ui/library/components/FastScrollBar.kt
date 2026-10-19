package com.example.ui.library.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.ui.common.LocalMiniPlayerVisible
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

class ScrollMetrics(val avgItemPx: Float, val currentPx: Float, val maxScrollPx: Float)

private fun LazyListLayoutInfo.estimate(itemCount: Int): ScrollMetrics? {
    val visible = visibleItemsInfo
    if (visible.isEmpty() || itemCount <= 0) return null
    val avg = visible.sumOf { it.size }.toFloat() / visible.size + mainAxisItemSpacing
    if (avg <= 0f) return null
    val viewport = (viewportEndOffset - viewportStartOffset).toFloat()
    val total = avg * itemCount + beforeContentPadding + afterContentPadding
    val maxScroll = (total - viewport).coerceAtLeast(1f)
    val first = visible.first()
    val current = (first.index * avg - first.offset).coerceIn(0f, maxScroll)
    return ScrollMetrics(avg, current, maxScroll)
}

private fun LazyGridLayoutInfo.estimate(itemCount: Int): ScrollMetrics? {
    val visible = visibleItemsInfo
    if (visible.isEmpty() || itemCount <= 0) return null
    var maxCol = 0
    for (i in visible.indices) {
        if (visible[i].column > maxCol) maxCol = visible[i].column
    }
    val columns = (maxCol + 1).coerceAtLeast(1)
    val totalRows = ceil(itemCount / columns.toFloat()).toInt().coerceAtLeast(1)

    var sumHeight = 0
    for (i in visible.indices) {
        sumHeight += visible[i].size.height
    }
    val avg = (sumHeight.toFloat() / visible.size) + mainAxisItemSpacing
    if (avg <= 0f) return null

    val viewport = (viewportEndOffset - viewportStartOffset).toFloat()
    val total = avg * totalRows + beforeContentPadding + afterContentPadding
    val maxScroll = (total - viewport).coerceAtLeast(1f)
    val first = visible.first()
    val current = (first.row * avg - first.offset.y).coerceIn(0f, maxScroll)
    return ScrollMetrics(avg, current, maxScroll)
}

private interface FastScrollAdapter {
    val isScrollInProgress: Boolean
    val canScroll: Boolean
    fun estimateMetrics(itemCount: Int): ScrollMetrics?
    fun calculateIndexForFraction(fraction: Float, itemCount: Int, metrics: ScrollMetrics): Int
    suspend fun scrollToFraction(fraction: Float, itemCount: Int, metrics: ScrollMetrics)
    fun scrollPositionKey(): Any?
}

private class ListFastScrollAdapter(private val state: LazyListState) : FastScrollAdapter {
    override val isScrollInProgress: Boolean get() = state.isScrollInProgress
    override val canScroll: Boolean get() = state.canScrollForward || state.canScrollBackward
    override fun estimateMetrics(itemCount: Int): ScrollMetrics? = state.layoutInfo.estimate(itemCount)
    override fun calculateIndexForFraction(fraction: Float, itemCount: Int, metrics: ScrollMetrics): Int {
        return ((fraction * metrics.maxScrollPx) / metrics.avgItemPx).toInt().coerceIn(0, itemCount - 1)
    }
    override suspend fun scrollToFraction(fraction: Float, itemCount: Int, metrics: ScrollMetrics) {
        val info = state.layoutInfo
        val visible = info.visibleItemsInfo
        if (visible.isEmpty()) return

        val targetPx = fraction * metrics.maxScrollPx
        val targetIndex = (targetPx / metrics.avgItemPx).toInt().coerceIn(0, itemCount - 1)
        val targetOffsetPx = (targetPx - targetIndex * metrics.avgItemPx).roundToInt().coerceAtLeast(0)

        val targetItem = visible.firstOrNull { it.index == targetIndex }
        val firstVisible = visible.first()
        val lastVisible = visible.last()
        val nearRange = (firstVisible.index - visible.size)..(lastVisible.index + visible.size)

        when {
            targetItem != null -> {
                val delta = (targetItem.offset + targetOffsetPx - info.viewportStartOffset).toFloat()
                if (abs(delta) >= 0.5f) state.dispatchRawDelta(delta)
            }
            targetIndex in nearRange -> {
                val delta = targetPx - metrics.currentPx
                if (abs(delta) >= 0.5f) state.dispatchRawDelta(delta)
            }
            else -> state.requestScrollToItem(targetIndex, targetOffsetPx)
        }
    }
    override fun scrollPositionKey(): Any? = if (state.isScrollInProgress) state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset else null
}

private class GridFastScrollAdapter(private val state: LazyGridState) : FastScrollAdapter {
    override val isScrollInProgress: Boolean get() = state.isScrollInProgress
    override val canScroll: Boolean get() = state.canScrollForward || state.canScrollBackward
    override fun estimateMetrics(itemCount: Int): ScrollMetrics? = state.layoutInfo.estimate(itemCount)

    private fun getColumns(): Int {
        val visible = state.layoutInfo.visibleItemsInfo
        var maxCol = 0
        for (i in visible.indices) {
            if (visible[i].column > maxCol) maxCol = visible[i].column
        }
        return (maxCol + 1).coerceAtLeast(1)
    }

    override fun calculateIndexForFraction(fraction: Float, itemCount: Int, metrics: ScrollMetrics): Int {
        val columns = getColumns()
        val totalRows = ceil(itemCount / columns.toFloat()).toInt().coerceAtLeast(1)
        val targetRow = ((fraction * metrics.maxScrollPx) / metrics.avgItemPx).toInt().coerceIn(0, totalRows - 1)
        return (targetRow * columns).coerceIn(0, itemCount - 1)
    }

    override suspend fun scrollToFraction(fraction: Float, itemCount: Int, metrics: ScrollMetrics) {
        val info = state.layoutInfo
        val visible = info.visibleItemsInfo
        if (visible.isEmpty()) return

        val columns = getColumns()
        val totalRows = ceil(itemCount / columns.toFloat()).toInt().coerceAtLeast(1)

        val targetPx = fraction * metrics.maxScrollPx
        val targetRow = (targetPx / metrics.avgItemPx).toInt().coerceIn(0, totalRows - 1)
        val targetOffsetPx = (targetPx - targetRow * metrics.avgItemPx).roundToInt().coerceAtLeast(0)
        val targetIndex = (targetRow * columns).coerceIn(0, itemCount - 1)

        val targetItem = visible.firstOrNull { it.index == targetIndex }
        val firstVisible = visible.first()
        val lastVisible = visible.last()
        val nearRange = (firstVisible.index - visible.size)..(lastVisible.index + visible.size)

        when {
            targetItem != null -> {
                val delta = (targetItem.offset.y + targetOffsetPx - info.viewportStartOffset).toFloat()
                if (abs(delta) >= 0.5f) state.dispatchRawDelta(delta)
            }
            targetIndex in nearRange -> {
                val delta = targetPx - metrics.currentPx
                if (abs(delta) >= 0.5f) state.dispatchRawDelta(delta)
            }
            else -> state.requestScrollToItem(targetIndex, targetOffsetPx)
        }
    }
    override fun scrollPositionKey(): Any? = if (state.isScrollInProgress) state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset else null
}

@Composable
private fun FastScrollBarCore(
    adapter: FastScrollAdapter,
    itemCount: Int,
    modifier: Modifier = Modifier,
    previewTextProvider: ((Int) -> String)? = null,
    topPadding: Dp = 8.dp,
    hasMiniPlayer: Boolean = LocalMiniPlayerVisible.current,
    onDraggingChange: ((Boolean) -> Unit)? = null,
    onHighSpeedChange: ((Boolean) -> Unit)? = null,
    containerTestTag: String = "fast_scroll_bar"
) {
    val canScroll by remember {
        derivedStateOf { adapter.canScroll }
    }
    if (!canScroll) return

    val density = LocalDensity.current
    val count by rememberUpdatedState(itemCount)
    val provider by rememberUpdatedState(previewTextProvider)

    val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val effectiveBottomPadding = if (hasMiniPlayer) {
        142.dp + navBarBottomInset
    } else {
        80.dp + navBarBottomInset
    }

    val thumbHeight = 28.dp
    val thumbPx = with(density) { thumbHeight.toPx() }
    val topPx = with(density) { topPadding.toPx() }
    val bottomPx = with(density) { effectiveBottomPadding.toPx() }

    val currentTopPx by rememberUpdatedState(topPx)
    val currentBottomPx by rememberUpdatedState(bottomPx)

    var isDragging by remember { mutableStateOf(false) }
    var isFastScrollingActive by remember { mutableStateOf(false) }
    var isHighSpeedScrolling by remember { mutableStateOf(false) }
    var trackHeightPx by remember { mutableFloatStateOf(1f) }
    var previewHeightPx by remember { mutableFloatStateOf(1f) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var previewText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isHighSpeedScrolling) {
        onHighSpeedChange?.invoke(isHighSpeedScrolling)
    }

    LaunchedEffect(adapter) {
        var lastOffset = 0f
        var lastTime = System.currentTimeMillis()
        snapshotFlow {
            adapter.scrollPositionKey()
        }.collect { scrollPos ->
            val now = System.currentTimeMillis()
            val currentOffset = adapter.estimateMetrics(count)?.currentPx ?: 0f
            if (scrollPos != null) {
                val dt = (now - lastTime).coerceAtLeast(1)
                val distance = abs(currentOffset - lastOffset)
                val velocity = (distance / dt) * 1000f

                if (velocity > 2500f) {
                    isFastScrollingActive = true
                    isHighSpeedScrolling = true
                } else {
                    isHighSpeedScrolling = false
                }
            }
            lastOffset = currentOffset
            lastTime = now
        }
    }

    LaunchedEffect(isFastScrollingActive, adapter.isScrollInProgress, isDragging) {
        if (isFastScrollingActive && !adapter.isScrollInProgress && !isDragging) {
            delay(1200)
            isFastScrollingActive = false
            isHighSpeedScrolling = false
        }
    }

    val isVisible = isDragging || isFastScrollingActive
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "fast_scroll_alpha"
    )

    val scrollFraction by remember {
        derivedStateOf {
            val m = adapter.estimateMetrics(count) ?: return@derivedStateOf 0f
            (m.currentPx / m.maxScrollPx).coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(isDragging) {
        onDraggingChange?.invoke(isDragging)
        if (!isDragging) return@LaunchedEffect

        var lastFraction = -1f
        while (isDragging) {
            withFrameNanos { }
            val frac = dragFraction
            if (frac == lastFraction) continue
            lastFraction = frac

            val m = adapter.estimateMetrics(count) ?: continue
            adapter.scrollToFraction(frac, count, m)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .testTag(containerTestTag)
    ) {
        if (isDragging && !previewText.isNullOrBlank()) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 68.dp)
                    .graphicsLayer {
                        val maxH = (trackHeightPx - topPx - bottomPx - thumbPx).coerceAtLeast(0f)
                        val thumbCenter = topPx + maxH * dragFraction + thumbPx / 2f
                        translationY = (thumbCenter - previewHeightPx / 2f).coerceIn(
                            topPx, (trackHeightPx - bottomPx - previewHeightPx).coerceAtLeast(topPx)
                        )
                    }
                    .onSizeChanged { previewHeightPx = it.height.toFloat() }
                    .testTag("fast_scroll_preview")
            ) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = previewText!!,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 20.sp
                        )
                    )
                }
            }
        }

        if (alpha > 0.01f) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(44.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .onSizeChanged { trackHeightPx = it.height.toFloat().coerceAtLeast(1f) }
                    .pointerInput(isVisible) {
                        if (!isVisible) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val scrollable = (trackHeightPx - currentTopPx - currentBottomPx - thumbPx).coerceAtLeast(1f)
                            val currentFraction = if (isDragging) dragFraction else scrollFraction
                            val currentThumbTop = currentTopPx + scrollable * currentFraction

                            val touchTolerance = 24.dp.toPx()
                            if (down.position.y < (currentThumbTop - touchTolerance) ||
                                down.position.y > (currentThumbTop + thumbPx + touchTolerance)
                            ) {
                                return@awaitEachGesture
                            }

                            down.consume()
                            var lastIndex = -1

                            fun update(y: Float) {
                                val f = ((y - currentTopPx - thumbPx / 2f) / scrollable).coerceIn(0f, 1f)
                                dragFraction = f

                                val m = adapter.estimateMetrics(count) ?: return
                                val idx = adapter.calculateIndexForFraction(f, count, m)
                                if (idx != lastIndex) {
                                    lastIndex = idx
                                    val letter = provider?.invoke(idx)?.trim()
                                        ?.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
                                    previewText = letter
                                }
                            }

                            isDragging = true
                            update(down.position.y)
                            try {
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    change.consume()
                                    update(change.position.y)
                                } while (change.pressed)
                            } finally {
                                isDragging = false
                                previewText = null
                            }
                        }
                    }
                    .testTag("fast_scroll_scrollbar_container")
            ) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 6.dp)
                        .graphicsLayer {
                            val f = if (isDragging) dragFraction else scrollFraction
                            val maxH = (trackHeightPx - topPx - bottomPx - thumbPx).coerceAtLeast(0f)
                            translationY = topPx + maxH * f
                        }
                        .width(if (isDragging) 8.dp else 6.dp)
                        .height(thumbHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        .testTag("fast_scroll_thumb")
                )
            }
        }
    }
}

@Composable
fun FastScrollBar(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier,
    previewTextProvider: ((Int) -> String)? = null,
    topPadding: Dp = 8.dp,
    hasMiniPlayer: Boolean = LocalMiniPlayerVisible.current,
    onDraggingChange: ((Boolean) -> Unit)? = null,
    onHighSpeedChange: ((Boolean) -> Unit)? = null
) {
    val adapter = remember(listState) { ListFastScrollAdapter(listState) }
    FastScrollBarCore(
        adapter = adapter,
        itemCount = itemCount,
        modifier = modifier,
        previewTextProvider = previewTextProvider,
        topPadding = topPadding,
        hasMiniPlayer = hasMiniPlayer,
        onDraggingChange = onDraggingChange,
        onHighSpeedChange = onHighSpeedChange,
        containerTestTag = "fast_scroll_bar"
    )
}

@Composable
fun FastScrollBarGrid(
    gridState: LazyGridState,
    itemCount: Int,
    modifier: Modifier = Modifier,
    previewTextProvider: ((Int) -> String)? = null,
    topPadding: Dp = 8.dp,
    hasMiniPlayer: Boolean = LocalMiniPlayerVisible.current,
    onDraggingChange: ((Boolean) -> Unit)? = null,
    onHighSpeedChange: ((Boolean) -> Unit)? = null
) {
    val adapter = remember(gridState) { GridFastScrollAdapter(gridState) }
    FastScrollBarCore(
        adapter = adapter,
        itemCount = itemCount,
        modifier = modifier,
        previewTextProvider = previewTextProvider,
        topPadding = topPadding,
        hasMiniPlayer = hasMiniPlayer,
        onDraggingChange = onDraggingChange,
        onHighSpeedChange = onHighSpeedChange,
        containerTestTag = "fast_scroll_bar_grid"
    )
}
