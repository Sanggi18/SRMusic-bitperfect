package com.example.ui.lyrics.core

import com.example.lyrics.LyricLine
import kotlin.math.min

internal fun buildLyricDisplayItems(lines: List<LyricLine>): List<LyricDisplayItem> {
    if (lines.isEmpty()) return emptyList()

    val result = mutableListOf<LyricDisplayItem>()
    val firstLine = lines.first()

    if (firstLine.startMs >= INITIAL_GAP_MIN_DURATION_MS) {
        result += LyricDisplayItem.GapItem(
            startTimeMs = 0L,
            endTimeMs = firstLine.startMs,
        )
    }

    lines.forEachIndexed { index, line ->
        result += LyricDisplayItem.LineItem(
            sourceIndex = index,
            line = line,
        )

        val nextLine = lines.getOrNull(index + 1) ?: return@forEachIndexed

        if (nextLine.startMs <= line.startMs) return@forEachIndexed

        val estimatedLineEnd = estimateLineEndTimeMs(
            line = line,
            nextLineStartMs = nextLine.startMs,
        )

        val estimatedGapDuration = nextLine.startMs - estimatedLineEnd

        if (estimatedLineEnd < nextLine.startMs && estimatedGapDuration >= GAP_MIN_DURATION_MS) {
            result += LyricDisplayItem.GapItem(
                startTimeMs = estimatedLineEnd,
                endTimeMs = nextLine.startMs,
            )
        }
    }

    return result
}

internal fun findActiveDisplayIndex(
    items: List<LyricDisplayItem>,
    positionMs: Long,
): Int {
    for (index in items.indices) {
        val item = items[index]

        if (item is LyricDisplayItem.GapItem &&
            positionMs >= item.startTimeMs &&
            positionMs < item.endTimeMs
        ) {
            return index
        }
    }

    var activeIndex = -1

    for (index in items.indices) {
        val item = items[index]

        if (item is LyricDisplayItem.LineItem) {
            if (item.line.startMs <= positionMs) {
                activeIndex = index
            } else {
                break
            }
        }
    }

    return activeIndex
}

internal fun isWordSyncedLine(line: LyricLine): Boolean {
    if (line.words.size < 2) return false

    for (index in 1 until line.words.size) {
        if (line.words[index].startMs > line.words[index - 1].startMs) {
            return true
        }
    }

    return false
}

internal fun estimateWordDurationMs(line: LyricLine, wordIndex: Int): Long {
    val word = line.words.getOrNull(wordIndex) ?: return 500L
    val previousWord = line.words.getOrNull(wordIndex - 1) ?: return 500L

    val interval = word.startMs - previousWord.startMs

    return if (interval > 0L) {
        interval.coerceIn(250L, 900L)
    } else {
        500L
    }
}

internal fun estimateLineEndTimeMs(line: LyricLine, nextLineStartMs: Long): Long {
    if (isWordSyncedLine(line)) {
        val lastWordIndex = line.words.lastIndex
        val lastWord = line.words.getOrNull(lastWordIndex) ?: return line.startMs

        val estimatedEnd = lastWord.startMs + estimateWordDurationMs(
            line = line,
            wordIndex = lastWordIndex,
        )

        return estimatedEnd.coerceIn(
            minimumValue = line.startMs,
            maximumValue = nextLineStartMs,
        )
    }

    val interval = (nextLineStartMs - line.startMs).coerceAtLeast(0L)

    val estimatedDuration = (interval * 0.68f).toLong().coerceIn(800L, 2_200L)

    return (line.startMs + estimatedDuration).coerceAtMost(nextLineStartMs)
}

internal fun buildScrollPlan(
    items: List<LyricDisplayItem>,
    positionMs: Long,
): LyricScrollPlan {
    val activeIndex = findActiveDisplayIndex(items = items, positionMs = positionMs)

    val activeItem = items.getOrNull(activeIndex)

    if (activeItem is LyricDisplayItem.GapItem) {
        val nextLineIndex = (activeIndex + 1 until items.size)
            .firstOrNull { items[it] is LyricDisplayItem.LineItem }

        if (nextLineIndex == null) {
            return LyricScrollPlan(
                targetDisplayIndex = activeIndex,
            )
        }

        val nextLine = items[nextLineIndex] as LyricDisplayItem.LineItem

        val leadMs = (activeItem.endTimeMs - activeItem.startTimeMs)
            .coerceAtLeast(0L)
            .coerceIn(LINE_SCROLL_MIN_MS, LINE_SCROLL_MAX_MS)

        val targetIndex = if (nextLine.line.startMs <= positionMs + leadMs) {
            nextLineIndex
        } else {
            activeIndex
        }

        return LyricScrollPlan(
            targetDisplayIndex = targetIndex,
        )
    }

    var currentLineIndex = -1
    var currentLine: LyricDisplayItem.LineItem? = null

    for (index in items.indices) {
        val item = items[index]

        if (item is LyricDisplayItem.LineItem && item.line.startMs <= positionMs) {
            currentLineIndex = index
            currentLine = item
        }
    }

    if (currentLine == null) {
        val firstLineIndex = items.indexOfFirst { it is LyricDisplayItem.LineItem }

        if (firstLineIndex < 0) {
            return LyricScrollPlan(
                targetDisplayIndex = -1,
            )
        }

        val firstLine = items[firstLineIndex] as LyricDisplayItem.LineItem

        val targetIndex = if (firstLine.line.startMs <= positionMs + LINE_SCROLL_MAX_MS) {
            firstLineIndex
        } else {
            -1
        }

        return LyricScrollPlan(
            targetDisplayIndex = targetIndex,
        )
    }

    var nextLineIndex = -1
    var nextLine: LyricDisplayItem.LineItem? = null

    for (index in currentLineIndex + 1 until items.size) {
        val item = items[index]

        if (item is LyricDisplayItem.LineItem) {
            nextLineIndex = index
            nextLine = item
            break
        }
    }

    if (nextLine == null) {
        return LyricScrollPlan(
            targetDisplayIndex = currentLineIndex,
        )
    }

    val estimatedLineEnd = estimateLineEndTimeMs(
        line = currentLine.line,
        nextLineStartMs = nextLine.line.startMs,
    )

    val interLineGap = (nextLine.line.startMs - estimatedLineEnd).coerceAtLeast(0L)

    val leadMs = interLineGap.coerceIn(LINE_SCROLL_MIN_MS, LINE_SCROLL_MAX_MS)

    val targetIndex = if (nextLine.line.startMs <= positionMs + leadMs) {
        nextLineIndex
    } else {
        currentLineIndex
    }

    return LyricScrollPlan(
        targetDisplayIndex = targetIndex,
    )
}

internal fun buildWordStartOffsets(line: LyricLine): List<Int> {
    val offsets = ArrayList<Int>(line.words.size)
    var offset = 0

    line.words.forEachIndexed { index, word ->
        offsets += offset
        offset += word.text.length

        if (index < line.words.lastIndex) {
            offset += 1
        }
    }

    return offsets
}

internal fun inferredWordEndTimeMs(line: LyricLine, wordIndex: Int): Long {
    val word = line.words.getOrNull(wordIndex) ?: return 0L
    val nextWord = line.words.getOrNull(wordIndex + 1)

    if (nextWord != null && nextWord.startMs > word.startMs) {
        return nextWord.startMs
    }

    return word.startMs + estimateWordDurationMs(line = line, wordIndex = wordIndex)
}

internal fun calculateWordLift(
    line: LyricLine,
    wordIndex: Int,
    positionMs: Long,
): Float {
    val word = line.words.getOrNull(wordIndex) ?: return 0f

    val inferredEnd = inferredWordEndTimeMs(line = line, wordIndex = wordIndex)

    val wordDuration = (inferredEnd - word.startMs).coerceAtLeast(1L)

    val riseDuration = wordDuration.toFloat().coerceIn(WORD_RISE_MIN_MS, WORD_RISE_MAX_MS)

    val rising = ((positionMs - word.startMs).toFloat() / riseDuration).coerceIn(0f, 1f)

    val falling = (1f - (positionMs - inferredEnd).toFloat() / WORD_SETTLE_MS).coerceIn(0f, 1f)

    val heldThrough = ((wordDuration.toFloat() - WORD_LIFT_QUICK_MS) /
        (WORD_LIFT_HELD_MS - WORD_LIFT_QUICK_MS)).coerceIn(0f, 1f)

    val liftHeight = WORD_LIFT_FLOOR + (1f - WORD_LIFT_FLOOR) * smoothStep(heldThrough)

    return liftHeight * smoothStep(min(rising, falling))
}

internal fun smoothStep(fraction: Float): Float {
    val value = fraction.coerceIn(0f, 1f)
    return value * value * (3f - 2f * value)
}
