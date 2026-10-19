package com.example.ui.lyrics

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.lyrics.components.LyricAutoScrollEffects
import com.example.ui.lyrics.components.LyricDisplayRow
import com.example.ui.lyrics.components.rememberLyricClock
import com.example.ui.lyrics.core.LYRIC_SCROLL_ANCHOR_FRACTION
import com.example.ui.lyrics.core.LYRIC_SETTLE_MS
import com.example.lyrics.LyricLine
import com.example.ui.lyrics.core.LyricScrollRun
import com.example.ui.lyrics.core.buildLyricDisplayItems
import com.example.ui.lyrics.core.buildScrollPlan
import com.example.ui.lyrics.core.findActiveDisplayIndex

@Composable
fun LyricViewer(
    lyrics: List<LyricLine>,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    activeColor: Color = MaterialTheme.colorScheme.onSurface,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val listState = rememberLazyListState()

    val displayItems = remember(lyrics) {
        buildLyricDisplayItems(lyrics)
    }

    val lyricClock = rememberLyricClock(
        lyrics = lyrics,
        positionMs = positionMs,
        isPlaying = isPlaying,
    )

    val activeDisplayIndex by remember(displayItems, lyricClock) {
        derivedStateOf(structuralEqualityPolicy()) {
            findActiveDisplayIndex(
                items = displayItems,
                positionMs = lyricClock.longValue,
            )
        }
    }

    val scrollPlan by remember(displayItems, lyricClock) {
        derivedStateOf(structuralEqualityPolicy()) {
            buildScrollPlan(
                items = displayItems,
                positionMs = lyricClock.longValue,
            )
        }
    }

    var pinnedMode by remember(lyrics) { mutableStateOf(false) }

    var scrollRun by remember(lyrics) {
        mutableStateOf(
            LyricScrollRun(
                id = 0,
                deltaPx = 0f,
                durationMs = LYRIC_SETTLE_MS.toLong(),
                focusDisplayIndex = -1,
            )
        )
    }

    val scrollElapsedMs = remember(lyrics) { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier = modifier) {
        val bottomPadding = maxHeight * (1f - LYRIC_SCROLL_ANCHOR_FRACTION)

        LyricAutoScrollEffects(
            listState = listState,
            displayItems = displayItems,
            activeDisplayIndex = activeDisplayIndex,
            pinnedMode = pinnedMode,
            onPinnedModeChange = { pinnedMode = it },
            scrollRun = scrollRun,
            onScrollRunChange = { scrollRun = it },
            scrollElapsedMs = scrollElapsedMs,
        )

        val focusDisplayIndex = scrollPlan.targetDisplayIndex
            .takeIf { it in displayItems.indices }
            ?: activeDisplayIndex

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = if (pinnedMode) bottomPadding else 0.dp,
                start = 24.dp,
                end = 24.dp,
            ),
        ) {
            itemsIndexed(
                items = displayItems,
                key = { _, item -> item.key },
            ) { displayIndex, item ->
                LyricDisplayRow(
                    item = item,
                    displayIndex = displayIndex,
                    activeDisplayIndex = activeDisplayIndex,
                    focusDisplayIndex = focusDisplayIndex,
                    scrollRun = scrollRun,
                    scrollElapsedMs = scrollElapsedMs.floatValue,
                    lyricClock = lyricClock,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    accentColor = accentColor,
                    onSeek = onSeek,
                )
            }
        }
    }
}
