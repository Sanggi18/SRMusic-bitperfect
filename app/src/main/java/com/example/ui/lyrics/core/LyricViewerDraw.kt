package com.example.ui.lyrics.core

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.dp
import com.example.lyrics.LyricLine
import kotlin.math.max
import kotlin.math.min

internal fun collectFloatingSegments(
    layout: TextLayoutResult,
    line: LyricLine,
    wordOffsets: List<Int>,
    positionMs: Long,
): List<FloatingWordSegment> {
    val segments = ArrayList<FloatingWordSegment>()

    if (line.words.isEmpty()) return segments

    val textLength = layout.layoutInput.text.length

    if (textLength == 0) return segments

    for (visualLine in 0 until layout.lineCount) {
        val lineStart = layout.getLineStart(visualLine)
        val lineEnd = layout.getLineEnd(visualLine, visibleEnd = true)

        if (lineStart >= lineEnd) continue

        val physicalLeft = min(layout.getLineLeft(visualLine), layout.getLineRight(visualLine))
        val physicalRight = max(layout.getLineLeft(visualLine), layout.getLineRight(visualLine))
        val top = layout.getLineTop(visualLine)
        val bottom = layout.getLineBottom(visualLine)

        for (wordIndex in line.words.indices) {
            // wordOffsets dibangun dari line yang sama, indeks 1:1 dengan words.
            if (wordIndex >= wordOffsets.size) continue

            val word = line.words[wordIndex]
            val wordStart = wordOffsets[wordIndex]
            val wordEnd = wordStart + word.text.length

            val start = max(wordStart, lineStart)
            val end = min(wordEnd, lineEnd)

            if (start >= end) continue

            val lift = calculateWordLift(
                line = line,
                wordIndex = wordIndex,
                positionMs = positionMs,
            )

            if (lift <= 0.01f) continue

            val from = layout.horizontalPositionOnLine(offset = start, visualLine = visualLine)
            val to = layout.horizontalPositionOnLine(offset = end, visualLine = visualLine)

            val left = min(from, to).coerceIn(physicalLeft, physicalRight)
            val right = max(from, to).coerceIn(physicalLeft, physicalRight)

            if (right <= left) continue

            segments += FloatingWordSegment(
                visualLine = visualLine,
                left = left,
                right = right,
                top = top,
                bottom = bottom,
                lift = lift,
            )
        }
    }

    return segments
}

internal fun TextLayoutResult.horizontalPositionOnLine(
    offset: Int,
    visualLine: Int,
): Float {
    val lineStart = getLineStart(visualLine)
    val lineEnd = getLineEnd(visualLine, visibleEnd = true)
    val left = min(getLineLeft(visualLine), getLineRight(visualLine))
    val right = max(getLineLeft(visualLine), getLineRight(visualLine))
    val textLength = layoutInput.text.length

    val directionIndex = lineStart.coerceIn(0, (textLength - 1).coerceAtLeast(0))
    val isRtl = getParagraphDirection(directionIndex) == ResolvedTextDirection.Rtl

    return when {
        offset <= lineStart -> if (isRtl) right else left
        offset >= lineEnd -> if (isRtl) left else right
        else -> getHorizontalPosition(offset, usePrimaryDirection = true)
    }
}

internal fun ContentDrawScope.drawFloatingWords(
    layout: TextLayoutResult,
    line: LyricLine,
    wordOffsets: List<Int>,
    positionMs: Long,
) {
    val segments = collectFloatingSegments(
        layout = layout,
        line = line,
        wordOffsets = wordOffsets,
        positionMs = positionMs,
    )

    if (segments.isEmpty()) {
        drawContent()
        return
    }

    val peak = WORD_RISE_DP.dp.toPx()

    val flatPath = Path()
    var hasFlatContent = false

    for (visualLine in 0 until layout.lineCount) {
        val lineStart = layout.getLineStart(visualLine)
        val lineEnd = layout.getLineEnd(visualLine, visibleEnd = true)

        if (lineStart >= lineEnd) continue

        val left = min(layout.getLineLeft(visualLine), layout.getLineRight(visualLine))
        val right = max(layout.getLineLeft(visualLine), layout.getLineRight(visualLine))
        val top = layout.getLineTop(visualLine)
        val bottom = layout.getLineBottom(visualLine)

        val rowSegments = segments
            .filter { it.visualLine == visualLine }
            .sortedBy { it.left }

        if (rowSegments.isEmpty()) {
            flatPath.addRect(Rect(left = left, top = top, right = right, bottom = bottom))
            hasFlatContent = true
            continue
        }

        var edge = left

        for (segment in rowSegments) {
            val segmentLeft = segment.left.coerceIn(left, right)
            val segmentRight = segment.right.coerceIn(left, right)

            if (segmentLeft > edge) {
                flatPath.addRect(Rect(left = edge, top = top, right = segmentLeft, bottom = bottom))
                hasFlatContent = true
            }

            edge = max(edge, segmentRight)
        }

        if (edge < right) {
            flatPath.addRect(Rect(left = edge, top = top, right = right, bottom = bottom))
            hasFlatContent = true
        }
    }

    if (hasFlatContent) {
        clipPath(flatPath) {
            this@drawFloatingWords.drawContent()
        }
    }

    for (segment in segments) {
        clipRect(
            left = segment.left - 0.5f,
            top = segment.top - peak - 1f,
            right = segment.right + 0.5f,
            bottom = segment.bottom + 1f,
        ) {
            translate(top = -segment.lift * peak) {
                this@drawFloatingWords.drawContent()
            }
        }
    }
}
