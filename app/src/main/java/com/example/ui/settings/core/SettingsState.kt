package com.example.ui.settings.core
import com.example.ui.settings.components.*
import com.example.ui.common.components.*

import com.example.core.model.AppThemeMode
import com.example.core.data.mediastore.scan.LibraryScanOptions
import com.example.core.audio.dac.DacSampleRateMode
import com.example.core.audio.dac.DacVolumeMode
import com.example.core.audio.dac.DsdGainCompensation
import com.example.core.audio.dac.DsdOutputMode
import com.example.core.model.NowPlayingProgressStyle
import com.example.core.model.NowPlayingTheme
import com.example.core.model.TrackInfoAlignment

sealed interface SettingsAction {
    data class SetThemeMode(val mode: AppThemeMode) : SettingsAction
    data class SetDynamicColor(val enabled: Boolean) : SettingsAction
    data class SetArtworkAccent(val enabled: Boolean) : SettingsAction
    data class SetAccentColor(val colorArgb: Long) : SettingsAction
    data class SetCustomBackgroundColor(val colorArgb: Long?) : SettingsAction
    data class SetCustomFontColor(val colorArgb: Long?) : SettingsAction
    data class SetCustomThemePresetId(val presetId: String) : SettingsAction
    data class SetEnabledTabIds(val tabIds: List<String>) : SettingsAction
    data class SetRootFolderDisplay(val path: String) : SettingsAction
    data class SetScanOptions(val options: LibraryScanOptions) : SettingsAction
    data class SetAutoScanEnabled(val enabled: Boolean) : SettingsAction
    data class AddIncludedFolder(val path: String) : SettingsAction
    data class RemoveIncludedFolder(val path: String) : SettingsAction
    data class AddBlacklistedFolder(val path: String) : SettingsAction
    data class RemoveBlacklistedFolder(val path: String) : SettingsAction
    data class SetFilterShortAudio(val enabled: Boolean, val minDurationSeconds: Int) : SettingsAction
    data class SetGaplessPlayback(val enabled: Boolean) : SettingsAction
    data class SetHighPrecisionResampling(val enabled: Boolean) : SettingsAction
    data class SetAudioFocusDucking(val enabled: Boolean) : SettingsAction
    data class SetReplayGain(val enabled: Boolean) : SettingsAction
    data class SetDacVolumeMode(val mode: DacVolumeMode) : SettingsAction
    data class SetDsdOutputMode(val mode: DsdOutputMode) : SettingsAction
    data class SetDsdGainCompensation(val gain: DsdGainCompensation) : SettingsAction
    data class SetDacSampleRateMode(val mode: DacSampleRateMode) : SettingsAction
    data class SetDacFixedSampleRate(val sampleRate: Int) : SettingsAction
    data class SetDacUsbBufferSize(val frames: Int) : SettingsAction
    data class SetNowPlayingProgressStyle(val style: NowPlayingProgressStyle) : SettingsAction
    data class SetNowPlayingTheme(val theme: NowPlayingTheme) : SettingsAction
    data class SetTrackInfoAlignment(val alignment: TrackInfoAlignment) : SettingsAction
    data class SetCustomFont(val path: String?, val name: String?) : SettingsAction
}

data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val isDynamicColor: Boolean = true,
    val isArtworkAccent: Boolean = false,
    val accentColorArgb: Long = 0xFF64B5F6L, // Soft Light Blue
    val customBackgroundColorArgb: Long? = null,
    val customFontColorArgb: Long? = null,
    val customThemePresetId: String = "nordic_night",
    val customFontPath: String? = null,
    val customFontName: String? = null,
    val enabledTabIds: List<String> = listOf("home", "song", "artist", "album", "folder"),
    val scanOptions: LibraryScanOptions = LibraryScanOptions(),
    val autoScanEnabled: Boolean = true,
    val gaplessPlayback: Boolean = false,
    val highPrecisionResampling: Boolean = false,
    val audioFocusDucking: Boolean = false,
    val replayGain: Boolean = false,
    val dacVolumeMode: DacVolumeMode = DacVolumeMode.SOFTWARE,
    val dsdOutputMode: DsdOutputMode = DsdOutputMode.AUTO,
    val dsdGainCompensation: DsdGainCompensation = DsdGainCompensation.PLUS_0DB,
    val dacSampleRateMode: DacSampleRateMode = DacSampleRateMode.FOLLOW_SOURCE,
    val dacFixedSampleRate: Int = 48000,
    val dacUsbBufferSize: Int = 256,
    val nowPlayingProgressStyle: NowPlayingProgressStyle = NowPlayingProgressStyle.ROUNDED_BAR,
    val nowPlayingTheme: NowPlayingTheme = NowPlayingTheme.DEFAULT,
    val trackInfoAlignment: TrackInfoAlignment = TrackInfoAlignment.LEFT
)
