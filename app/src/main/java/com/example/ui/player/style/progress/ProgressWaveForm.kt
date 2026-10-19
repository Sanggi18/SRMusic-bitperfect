package com.example.ui.player.style.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.PlaybackProgress
import com.example.core.model.Song
import com.example.ui.player.core.FastAudioWaveformExtractor
import com.example.core.util.TimeUtils
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.max

private const val WAVEFORM_BAR_COUNT = 100

/**
 * Waveform Progress Bar
 *
 * Parallel Execution Architecture:
 * - Next song triggered: Immediately begins extracting true acoustic waveform in background.
 * - Simultaneously: Begins closing previous waveform down to flat row of dots (up to 1000 ms).
 * - Instant Bloom Hand-off: As soon as audio data is ready, the closing animation is interrupted
 *   immediately (no waiting for full 1000 ms), and it gracefully blooms UP to peak acoustic heights (500 ms).
 */
@Composable
fun WaveformProgress(
    progress: PlaybackProgress,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    song: Song? = null,
    activeColor: Color? = null,
    inactiveColor: Color? = null,
    isPreview: Boolean = false
) {
    val context = LocalContext.current
    val flatBaseline = remember { FloatArray(WAVEFORM_BAR_COUNT) { 0f } }

    // Dummy bars statis untuk preview di settings (tanpa ekstraksi audio asli).
    // Template 40 nilai dari user, diulang (tile) untuk mengisi WAVEFORM_BAR_COUNT bar.
    val dummyBars = remember {
        val template = floatArrayOf(
            0.22f, 0.35f, 0.48f, 0.30f, 0.62f, 0.78f, 0.54f, 0.40f, 0.85f, 0.95f,
            0.70f, 0.50f, 0.66f, 0.88f, 0.74f, 0.45f, 0.32f, 0.58f, 0.82f, 0.92f,
            0.68f, 0.52f, 0.76f, 0.60f, 0.38f, 0.64f, 0.86f, 0.96f, 0.72f, 0.46f,
            0.58f, 0.80f, 0.66f, 0.42f, 0.74f, 0.90f, 0.62f, 0.48f, 0.34f, 0.24f
        )
        FloatArray(WAVEFORM_BAR_COUNT) { i -> template[i % template.size] }
    }

    var renderedBars by remember { mutableStateOf(flatBaseline) }

    // waveScale: 0f = flat line of circular dots, 1f = fully opened real waveform
    val waveScale = remember { Animatable(0f) }

    LaunchedEffect(song?.id, isPreview) {
        if (isPreview) {
            renderedBars = dummyBars
            waveScale.snapTo(1f)
            return@LaunchedEffect
        }

        val currentSong = song ?: run {
            if (waveScale.value > 0.01f) {
                waveScale.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
                )
            }
            renderedBars = flatBaseline
            return@LaunchedEffect
        }

        coroutineScope {
            val waveformFlow = FastAudioWaveformExtractor.getWaveformFlow(context.applicationContext, currentSong)

            // 1. Start fetching new song waveform in background
            var receivedData: FloatArray? = null
            val dataJob = launch {
                waveformFlow.first { it != null }.also { receivedData = it }
            }

            // 2. Smoothly close down to flat (fixed 1000 ms calm close)
            if (waveScale.value > 0.01f) {
                waveScale.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
                )
            } else {
                waveScale.snapTo(0f)
            }

            // 3. Ensure data is ready (instant if cached, waits if new)
            dataJob.join()

            // 4. Update to new audio data at flat position
            receivedData?.let {
                renderedBars = it
            }

            // 5. Blossom open to peak acoustic heights (500 ms)
            waveScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
    }

    val seekData = rememberProgressSeekState(progress, onSeek)
    val seekState = seekData.seekState
    val currentDuration = seekData.currentDuration
    val currentFraction = seekData.currentProgressValue
    val displayedPositionMs = seekData.displayedPositionMs

    val primaryActive = activeColor ?: MaterialTheme.colorScheme.primary
    val baseInactive = inactiveColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("now_playing_waveform")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .progressSeekGesture(
                    durationMs = currentDuration,
                    onSeek = { seekState.onSeekTriggered(it) },
                    onDraggingChange = { dragging, fraction -> seekState.onDraggingChange(dragging, fraction) }
                )
        ) {
            // Real Waveform Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .align(Alignment.Center)
            ) {
                val width = size.width
                val height = size.height
                if (width <= 0f || height <= 0f) return@Canvas

                val count = renderedBars.size
                val barSpacing = 1.5.dp.toPx()
                val totalSpacing = barSpacing * (count - 1)
                val barWidth = max(1.5.dp.toPx(), (width - totalSpacing) / count.toFloat())
                val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

                val safeFraction = currentFraction.coerceIn(0f, 1f)
                val playheadX = width * safeFraction
                val transitionWidth = (barWidth + barSpacing) * 2.5f
                val transitionStartX = (playheadX - transitionWidth).coerceAtLeast(0f)

                val progressBrush = if (safeFraction > 0f && safeFraction < 1f) {
                    Brush.horizontalGradient(
                        colors = listOf(primaryActive, baseInactive),
                        startX = transitionStartX,
                        endX = playheadX
                    )
                } else {
                    null
                }

                val centerY = height / 2f
                val animScale = waveScale.value

                for (i in 0 until count) {
                    val x = i * (barWidth + barSpacing)
                    val rawNormalized = renderedBars[i] // 0.0f .. 1.0f

                    // When animScale == 0f (loading / transition phase), effectiveAmp is 0f
                    // which cleanly draws barHeight = barWidth (minimal horizontal line of dots)
                    val effectiveAmp = rawNormalized * animScale
                    val barHeight = max(barWidth, height * effectiveAmp)
                    val top = centerY - (barHeight / 2f)

                    when {
                        safeFraction <= 0f -> {
                            drawRoundRect(
                                color = baseInactive,
                                topLeft = Offset(x, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = cornerRadius
                            )
                        }
                        safeFraction >= 1f -> {
                            drawRoundRect(
                                color = primaryActive,
                                topLeft = Offset(x, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = cornerRadius
                            )
                        }
                        x + barWidth <= transitionStartX -> {
                            drawRoundRect(
                                color = primaryActive,
                                topLeft = Offset(x, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = cornerRadius
                            )
                        }
                        x >= playheadX -> {
                            drawRoundRect(
                                color = baseInactive,
                                topLeft = Offset(x, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = cornerRadius
                            )
                        }
                        else -> {
                            drawRoundRect(
                                brush = progressBrush!!,
                                topLeft = Offset(x, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = cornerRadius
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        ProgressTimeRow(
            displayedPositionMs = displayedPositionMs,
            currentDuration = currentDuration,
            activeColor = activeColor
        )
    }
}
