package com.example.ui.equalizer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.example.core.audio.AudioOutputApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt


@Composable
fun BitPerfectCard(
    isBitPerfectEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBitPerfectEnabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .testTag("bit_perfect_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bit-Perfect USB",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isBitPerfectEnabled) "Direct USB DAC Output" else "Bypass Android mixer to USB DAC",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = isBitPerfectEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("bit_perfect_switch")
            )
        }
    }
}

@Composable
fun AudioOutputCard(
    selectedApi: com.example.core.audio.AudioOutputApi,
    onApiSelected: (com.example.core.audio.AudioOutputApi) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { showDialog = true }
            .testTag("audio_output_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Audio Output",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selectedApi.label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Audio Output") },
            text = {
                Column {
                    com.example.core.audio.AudioOutputApi.entries.forEach { api ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onApiSelected(api)
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(api.label, style = MaterialTheme.typography.bodyLarge)
                                if (api == com.example.core.audio.AudioOutputApi.OPENSL_ES) {
                                    Text(
                                        "Deprecated di API 26+, wrapper AAudio",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            RadioButton(
                                selected = selectedApi == api,
                                onClick = {
                                    onApiSelected(api)
                                    showDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun PreampCard(
    preampGain: Float,
    isEqActive: Boolean,
    onPreampGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Preamp Gain",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = preampGain.formatDb(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
            ExpressiveTrackSlider(
                value = preampGain,
                onValueChange = onPreampGainChange,
                valueRange = 0f..12f,
                enabled = isEqActive,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ToneControlCard(
    bassBoost: Float,
    vocalBoost: Float,
    trebleBoost: Float,
    isEqActive: Boolean,
    onBassBoostChange: (Float) -> Unit,
    onVocalBoostChange: (Float) -> Unit,
    onTrebleBoostChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Tone Control",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            // Bass Boost Row
            ToneSliderRow(
                title = "Bass Boost",
                value = bassBoost,
                onValueChange = onBassBoostChange,
                enabled = isEqActive
            )

            // Vocal Boost Row
            ToneSliderRow(
                title = "Vocal Boost",
                value = vocalBoost,
                onValueChange = onVocalBoostChange,
                enabled = isEqActive
            )

            // Treble Boost Row
            ToneSliderRow(
                title = "Treble Boost",
                value = trebleBoost,
                onValueChange = onTrebleBoostChange,
                enabled = isEqActive
            )
        }
    }
}

@Composable
private fun ToneSliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(88.dp)
        )
        ExpressiveTrackSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..10f,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.formatDb(),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled && value > 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp)
        )
    }
}

/**
 * Material 3 Expressive Slider Component (Thick pill track, vertical bar thumb, end-dot indicator).
 */
@Composable
fun ExpressiveTrackSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color? = null,
    inactiveColor: Color? = null
) {
    var isUserDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val currentFraction = if (isUserDragging) {
        dragFraction.coerceIn(0f, 1f)
    } else {
        ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
    }

    val primary = activeColor ?: MaterialTheme.colorScheme.primary
    val trackActive = if (enabled) primary else primary.copy(alpha = 0.38f)
    val trackInactive = inactiveColor ?: (if (enabled) primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    val thumbColor = if (enabled) primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    BoxWithConstraints(
        modifier = modifier
            .height(32.dp)
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val widthPx = size.width.toFloat()
                    if (widthPx > 0f) {
                        val initialFraction = (down.position.x / widthPx).coerceIn(0f, 1f)
                        isUserDragging = true
                        dragFraction = initialFraction
                        val newValue = valueRange.start + initialFraction * rangeSpan
                        onValueChange((newValue * 10).roundToInt() / 10f)
                    }

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointerChange = event.changes.firstOrNull() ?: break

                            if (!pointerChange.pressed) {
                                pointerChange.consume()
                                break
                            } else {
                                pointerChange.consume()
                                if (widthPx > 0f) {
                                    val frac = (pointerChange.position.x / widthPx).coerceIn(0f, 1f)
                                    dragFraction = frac
                                    val newValue = valueRange.start + frac * rangeSpan
                                    onValueChange((newValue * 10).roundToInt() / 10f)
                                }
                            }
                        }
                    } finally {
                        isUserDragging = false
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2f

            val trackHeightPx = 13.dp.toPx()
            val trackRadiusPx = trackHeightPx / 2f
            val trackTop = centerY - (trackHeightPx / 2f)

            val progressX = (canvasWidth * currentFraction).coerceIn(0f, canvasWidth)

            // 1. Draw Inactive Track
            val fullTrackPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = trackTop,
                        right = canvasWidth,
                        bottom = trackTop + trackHeightPx,
                        cornerRadius = CornerRadius(trackRadiusPx, trackRadiusPx)
                    )
                )
            }
            drawPath(path = fullTrackPath, color = trackInactive)

            // Inactive End-Dot Indicator
            val dotRadiusPx = 2.75.dp.toPx()
            val dotCenter = Offset(canvasWidth - 10.dp.toPx(), centerY)
            drawCircle(color = trackActive.copy(alpha = 0.5f), radius = dotRadiusPx, center = dotCenter)

            // 2. Draw Active Track
            if (progressX > 0f) {
                val activeTrackPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = 0f,
                            top = trackTop,
                            right = progressX,
                            bottom = trackTop + trackHeightPx,
                            cornerRadius = CornerRadius(trackRadiusPx, trackRadiusPx)
                        )
                    )
                }
                drawPath(path = activeTrackPath, color = trackActive)
            }

            // 3. Draw Vertical Bar / Pill Thumb
            val thumbWidthPx = 4.dp.toPx()
            val thumbHeightPx = if (isUserDragging) 26.dp.toPx() else 24.dp.toPx()
            val thumbRadiusCornerPx = 2.dp.toPx()
            val thumbX = (progressX - (thumbWidthPx / 2f)).coerceIn(0f, canvasWidth - thumbWidthPx)
            val thumbY = centerY - (thumbHeightPx / 2f)

            val thumbPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = thumbX,
                        top = thumbY,
                        right = thumbX + thumbWidthPx,
                        bottom = thumbY + thumbHeightPx,
                        cornerRadius = CornerRadius(thumbRadiusCornerPx, thumbRadiusCornerPx)
                    )
                )
            }
            drawPath(path = thumbPath, color = thumbColor)
        }
    }
}
