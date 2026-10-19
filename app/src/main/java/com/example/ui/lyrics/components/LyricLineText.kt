package com.example.ui.lyrics.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.lyrics.core.LYRIC_EASING
import com.example.ui.lyrics.core.LYRIC_FONT_SIZE_SP
import com.example.ui.lyrics.core.LYRIC_LINE_HEIGHT_SP
import com.example.ui.lyrics.core.LYRIC_SETTLE_MS
import com.example.lyrics.LyricLine
import com.example.ui.lyrics.core.buildLyricAnnotatedString
import com.example.ui.lyrics.core.buildWordStartOffsets
import com.example.ui.lyrics.core.calculateRevealedCharacters
import com.example.ui.lyrics.core.drawFloatingWords
import com.example.ui.lyrics.core.drawRevealedText
import com.example.ui.lyrics.core.isWordSyncedLine
import kotlin.math.min

@Composable
internal fun LyricLineText(
    line: LyricLine,
    isActive: Boolean,
    isWordSynced: Boolean,
    lyricClock: MutableLongState,
    wordOffsets: List<Int>,
    regularText: AnnotatedString,
    dimText: AnnotatedString,
    sweptText: AnnotatedString,
    style: TextStyle,
    lineColor: Color,
    textLayoutResult: TextLayoutResult?,
    onTextLayout: (TextLayoutResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    var textWidthPx by remember(line) { mutableIntStateOf(0) }

    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        val availableWidthPx = with(density) { maxWidth.toPx() }

        val safeWidthPx = (availableWidthPx - with(density) { 12.dp.toPx() })
            .coerceAtLeast(1f)

        val adaptiveScale = if (isActive && textWidthPx > 0) {
            min(1.10f, safeWidthPx / textWidthPx.toFloat()).coerceAtLeast(1f)
        } else {
            1f
        }

        val textScale by animateFloatAsState(
            targetValue = adaptiveScale,
            animationSpec = tween(durationMillis = LYRIC_SETTLE_MS, easing = LYRIC_EASING),
            label = "lyric-text-scale",
        )

        val textModifier = Modifier.graphicsLayer {
            scaleX = textScale
            scaleY = textScale
            transformOrigin = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0.5f)
        }

        /*
         * Identical floating geometry is applied to each text layer.
         * The foreground retains its timestamp-driven reveal.
         */
        val floatingModifier = Modifier.drawWithContent {
            val layout = textLayoutResult

            if (layout == null || line.words.isEmpty()) {
                drawContent()
            } else {
                drawFloatingWords(
                    layout = layout,
                    line = line,
                    wordOffsets = wordOffsets,
                    positionMs = lyricClock.longValue,
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
        ) {
            when {
                isWordSynced && isActive -> {
                    Text(
                        text = dimText,
                        modifier = textModifier.then(floatingModifier),
                        color = lineColor,
                        style = style,
                        maxLines = 3,
                        onTextLayout = {
                            if (textWidthPx != it.size.width) {
                                textWidthPx = it.size.width
                            }
                            onTextLayout(it)
                        },
                    )

                    Text(
                        text = sweptText,
                        modifier = textModifier
                            .then(floatingModifier)
                            .drawWithContent {
                                val layout = textLayoutResult

                                if (layout == null) {
                                    drawContent()
                                } else {
                                    drawRevealedText(
                                        layout = layout,
                                        revealedCharacters = calculateRevealedCharacters(
                                            line = line,
                                            wordOffsets = wordOffsets,
                                            positionMs = lyricClock.longValue,
                                        ),
                                    )
                                }
                            },
                        color = lineColor,
                        style = style,
                        maxLines = 3,
                    )
                }

                isWordSynced -> {
                    Text(
                        text = dimText,
                        modifier = textModifier.then(floatingModifier),
                        color = lineColor,
                        style = style,
                        maxLines = 3,
                        onTextLayout = {
                            if (textWidthPx != it.size.width) {
                                textWidthPx = it.size.width
                            }
                            onTextLayout(it)
                        },
                    )
                }

                else -> {
                    Text(
                        text = regularText,
                        modifier = textModifier,
                        color = lineColor,
                        style = style,
                        maxLines = 3,
                        onTextLayout = {
                            if (textWidthPx != it.size.width) {
                                textWidthPx = it.size.width
                            }
                            onTextLayout(it)
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun LyricLineContent(
    line: LyricLine,
    activeWordIndex: Int,
    isActive: Boolean,
    lyricClock: MutableLongState,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
) {
    val lineColor by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = LYRIC_SETTLE_MS, easing = LYRIC_EASING),
        label = "lyric-line-color",
    )

    val style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
        fontSize = LYRIC_FONT_SIZE_SP.sp,
        lineHeight = LYRIC_LINE_HEIGHT_SP.sp,
        fontWeight = FontWeight.Bold,
    )

    val wordOffsets = remember(line) { buildWordStartOffsets(line) }
    val isWordSynced = remember(line) { isWordSyncedLine(line) }

    var textLayoutResult by remember(line) { mutableStateOf<TextLayoutResult?>(null) }

    val onTextLayout: (TextLayoutResult) -> Unit = { result ->
        val previousLayout = textLayoutResult

        if (previousLayout == null ||
            previousLayout.layoutInput != result.layoutInput ||
            previousLayout.size != result.size ||
            previousLayout.lineCount != result.lineCount
        ) {
            textLayoutResult = result
        }
    }

    val regularText = remember(
        line, isActive, activeWordIndex, inactiveColor, activeColor, lineColor,
    ) {
        buildLyricAnnotatedString(
            line = line,
            isActive = isActive,
            activeWordIndex = activeWordIndex,
            inactiveColor = inactiveColor,
            activeColor = activeColor,
            lineColor = lineColor,
        )
    }

    val dimText = remember(line, inactiveColor) {
        buildLyricAnnotatedString(
            line = line,
            isActive = false,
            activeWordIndex = -1,
            inactiveColor = inactiveColor,
            activeColor = activeColor,
            lineColor = inactiveColor,
            forceInactive = true,
        )
    }

    val sweptText = remember(line, activeWordIndex, inactiveColor, activeColor) {
        buildLyricAnnotatedString(
            line = line,
            isActive = true,
            activeWordIndex = activeWordIndex,
            inactiveColor = inactiveColor,
            activeColor = activeColor,
            lineColor = activeColor,
        )
    }

    LyricLineText(
        line = line,
        isActive = isActive,
        isWordSynced = isWordSynced,
        lyricClock = lyricClock,
        wordOffsets = wordOffsets,
        regularText = regularText,
        dimText = dimText,
        sweptText = sweptText,
        style = style,
        lineColor = lineColor,
        textLayoutResult = textLayoutResult,
        onTextLayout = onTextLayout,
        modifier = modifier,
    )
}
