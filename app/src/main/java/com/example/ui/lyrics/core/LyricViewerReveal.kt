package com.example.ui.lyrics.core

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.example.lyrics.LyricLine
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal fun ContentDrawScope.drawRevealedText(
    layout: TextLayoutResult,
    revealedCharacters: Float,
) {
    val textLength = layout.layoutInput.text.length

    if (textLength == 0 || revealedCharacters <= 0f) return

    if (revealedCharacters >= textLength) {
        this.drawContent()
        return
    }

    val revealPath = Path()
    var hasReveal = false

    for (visualLine in 0 until layout.lineCount) {
        val lineStart = layout.getLineStart(visualLine)
        val lineEnd = layout.getLineEnd(visualLine, visibleEnd = false)

        if (revealedCharacters <= lineStart) continue

        val lineLeft = min(layout.getLineLeft(visualLine), layout.getLineRight(visualLine))
        val lineRight = max(layout.getLineLeft(visualLine), layout.getLineRight(visualLine))
        val lineTop = layout.getLineTop(visualLine)
        val lineBottom = layout.getLineBottom(visualLine)

        if (lineRight <= lineLeft || lineBottom <= lineTop) continue

        if (revealedCharacters >= lineEnd) {
            revealPath.addRect(
                Rect(left = lineLeft, top = lineTop, right = lineRight, bottom = lineBottom)
            )
            hasReveal = true
            continue
        }

        val offset = floor(revealedCharacters).toInt().coerceIn(lineStart, lineEnd)
        val characterProgress = (revealedCharacters - offset).coerceIn(0f, 1f)

        val directionIndex = lineStart.coerceIn(0, (textLength - 1).coerceAtLeast(0))
        val isRtl = layout.getParagraphDirection(directionIndex) == ResolvedTextDirection.Rtl

        val currentX = layout.getHorizontalPosition(offset, usePrimaryDirection = true)
        val nextOffset = (offset + 1).coerceAtMost(textLength)
        val nextX = if (nextOffset >= textLength ||
            layout.getLineForOffset(nextOffset) != visualLine
        ) {
            if (isRtl) lineLeft else lineRight
        } else {
            layout.getHorizontalPosition(nextOffset, usePrimaryDirection = true)
        }

        val revealX = (currentX + (nextX - currentX) * characterProgress)
            .coerceIn(lineLeft, lineRight)

        val clipLeft = if (isRtl) revealX else lineLeft
        val clipRight = if (isRtl) lineRight else revealX

        if (clipRight > clipLeft) {
            revealPath.addRect(
                Rect(left = clipLeft, top = lineTop, right = clipRight, bottom = lineBottom)
            )
            hasReveal = true
        }
    }

    if (hasReveal) {
        clipPath(revealPath) {
            this@drawRevealedText.drawContent()
        }
    }
}

internal fun findActiveWordIndex(line: LyricLine, positionMs: Long): Int {
    if (line.words.isEmpty()) return -1

    var activeIndex = -1

    for (index in line.words.indices) {
        val word = line.words[index]

        if (word.startMs <= positionMs) {
            activeIndex = index
        } else {
            break
        }
    }

    return activeIndex
}

internal fun calculateRevealedCharacters(
    line: LyricLine,
    wordOffsets: List<Int>,
    positionMs: Long,
): Float {
    if (line.words.isEmpty()) return 0f

    val lastIndex = line.words.lastIndex
    val totalCharacters = wordOffsets[lastIndex] + line.words[lastIndex].text.length

    for (index in line.words.indices) {
        val word = line.words[index]
        val wordOffset = wordOffsets[index]

        if (positionMs < word.startMs) {
            return wordOffset.toFloat()
        }

        val nextWord = line.words.getOrNull(index + 1)

        if (nextWord != null) {
            val wordDuration = nextWord.startMs - word.startMs

            if (wordDuration > 0L) {
                if (positionMs < nextWord.startMs) {
                    val progress = ((positionMs - word.startMs).toFloat() /
                        wordDuration.toFloat()).coerceIn(0f, 1f)

                    return wordOffset + word.text.length * progress
                }

                continue
            }

            continue
        }

        val finalWordDuration = estimateWordDurationMs(line = line, wordIndex = index)

        val progress = ((positionMs - word.startMs).toFloat() /
            finalWordDuration.toFloat()).coerceIn(0f, 1f)

        return wordOffset + word.text.length * progress
    }

    return totalCharacters.toFloat()
}

internal fun buildLyricAnnotatedString(
    line: LyricLine,
    isActive: Boolean,
    activeWordIndex: Int,
    inactiveColor: Color,
    activeColor: Color,
    lineColor: Color,
    forceInactive: Boolean = false,
): AnnotatedString {
    return buildAnnotatedString {
        line.words.forEachIndexed { index, word ->
            val wordColor = when {
                forceInactive -> inactiveColor
                !isActive -> lineColor
                activeWordIndex < 0 -> inactiveColor
                index <= activeWordIndex -> activeColor
                else -> inactiveColor
            }

            pushStyle(SpanStyle(color = wordColor, fontWeight = FontWeight.Bold))
            append(word.text)

            if (index < line.words.lastIndex) {
                append(" ")
            }

            pop()
        }
    }
}
