package com.example.ui.settings.audio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.core.audio.dac.DacSampleRateMode
import com.example.core.audio.dac.DacVolumeMode
import com.example.core.audio.dac.DsdGainCompensation
import com.example.core.audio.dac.DsdOutputMode
import com.example.ui.common.ItemPosition
import com.example.ui.settings.components.SettingsCard
import com.example.ui.settings.components.SettingsGroupItem
import com.example.ui.settings.components.SettingsSectionHeader
import com.example.ui.settings.core.SettingsAction
import com.example.ui.settings.core.SettingsUiState

@Composable
private fun DacOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
fun DacConfigScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Volume DAC
        SettingsSectionHeader(title = "Volume Control")
        SettingsCard {
            SettingsGroupItem(position = ItemPosition.TOP) {
                DacOptionRow("Software", uiState.dacVolumeMode == DacVolumeMode.SOFTWARE) {
                    onAction(SettingsAction.SetDacVolumeMode(DacVolumeMode.SOFTWARE))
                }
            }
            SettingsGroupItem(position = ItemPosition.BOTTOM) {
                DacOptionRow("Hardware (DAC)", uiState.dacVolumeMode == DacVolumeMode.HARDWARE) {
                    onAction(SettingsAction.SetDacVolumeMode(DacVolumeMode.HARDWARE))
                }
            }
        }

        // 2. DSD Output
        SettingsSectionHeader(title = "DSD Output")
        SettingsCard {
            DsdOutputMode.entries.forEachIndexed { i, mode ->
                val pos = when (i) {
                    0 -> ItemPosition.TOP
                    DsdOutputMode.entries.lastIndex -> ItemPosition.BOTTOM
                    else -> ItemPosition.MIDDLE
                }
                SettingsGroupItem(position = pos) {
                    DacOptionRow(mode.label, uiState.dsdOutputMode == mode) {
                        onAction(SettingsAction.SetDsdOutputMode(mode))
                    }
                }
            }
        }

        // 3. DSD Gain
        SettingsSectionHeader(title = "DSD Gain Compensation")
        SettingsCard {
            SettingsGroupItem(position = ItemPosition.TOP) {
                DacOptionRow("+0 dB", uiState.dsdGainCompensation == DsdGainCompensation.PLUS_0DB) {
                    onAction(SettingsAction.SetDsdGainCompensation(DsdGainCompensation.PLUS_0DB))
                }
            }
            SettingsGroupItem(position = ItemPosition.BOTTOM) {
                DacOptionRow("+6 dB", uiState.dsdGainCompensation == DsdGainCompensation.PLUS_6DB) {
                    onAction(SettingsAction.SetDsdGainCompensation(DsdGainCompensation.PLUS_6DB))
                }
            }
        }

        // 4. Sample Rate
        SettingsSectionHeader(title = "Sample Rate")
        SettingsCard {
            SettingsGroupItem(position = ItemPosition.TOP) {
                DacOptionRow("Follow source", uiState.dacSampleRateMode == DacSampleRateMode.FOLLOW_SOURCE) {
                    onAction(SettingsAction.SetDacSampleRateMode(DacSampleRateMode.FOLLOW_SOURCE))
                }
            }
            SettingsGroupItem(position = ItemPosition.BOTTOM) {
                DacOptionRow("Fixed", uiState.dacSampleRateMode == DacSampleRateMode.FIXED) {
                    onAction(SettingsAction.SetDacSampleRateMode(DacSampleRateMode.FIXED))
                }
            }
        }
        if (uiState.dacSampleRateMode == DacSampleRateMode.FIXED) {
            SettingsCard {
                listOf(44100, 48000, 88200, 96000, 176400, 192000, 352800, 384000).forEachIndexed { i, sr ->
                    val pos = when (i) {
                        0 -> ItemPosition.TOP
                        7 -> ItemPosition.BOTTOM
                        else -> ItemPosition.MIDDLE
                    }
                    SettingsGroupItem(position = pos) {
                        DacOptionRow("${sr / 1000.0} kHz", uiState.dacFixedSampleRate == sr) {
                            onAction(SettingsAction.SetDacFixedSampleRate(sr))
                        }
                    }
                }
            }
        }

        // 5. USB Buffer
        SettingsSectionHeader(title = "USB Buffer Size")
        SettingsCard {
            listOf(64, 128, 256, 512, 1024).forEachIndexed { i, buf ->
                val pos = when (i) {
                    0 -> ItemPosition.TOP
                    4 -> ItemPosition.BOTTOM
                    else -> ItemPosition.MIDDLE
                }
                SettingsGroupItem(position = pos) {
                    DacOptionRow("$buf frames", uiState.dacUsbBufferSize == buf) {
                        onAction(SettingsAction.SetDacUsbBufferSize(buf))
                    }
                }
            }
        }

    }
}
