package com.example.ui.settings.core
import com.example.ui.settings.components.*
import com.example.ui.common.components.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.audio.dac.DacSampleRateMode
import com.example.core.audio.dac.DacVolumeMode
import com.example.core.audio.dac.DsdGainCompensation
import com.example.core.audio.dac.DsdOutputMode
import com.example.core.data.mediastore.scan.LibraryScanOptions
import com.example.core.data.preferences.AppPreferences
import com.example.core.data.preferences.LibraryPreferences
import com.example.core.data.preferences.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private data class TabsAndAudio1(
    val tabIds: List<String>,
    val gapless: Boolean,
    val resampling: Boolean
)

private data class Audio2AndLibrary(
    val ducking: Boolean,
    val replayGain: Boolean,
    val scanOptions: LibraryScanOptions,
    val autoScan: Boolean
)

private data class DacConfig(
    val volumeMode: DacVolumeMode,
    val dsdOutputMode: DsdOutputMode,
    val dsdGain: DsdGainCompensation,
    val sampleRateMode: DacSampleRateMode,
    val fixedSampleRate: Int,
    val usbBufferSize: Int
)

private data class NowPlayingStyleSettings(
    val progressStyle: com.example.core.model.NowPlayingProgressStyle,
    val theme: com.example.core.model.NowPlayingTheme,
    val alignment: com.example.core.model.TrackInfoAlignment
)

class SettingsViewModel(
    private val themePrefs: ThemePreferences,
    private val appPrefs: AppPreferences,
    private val libraryPrefs: LibraryPreferences
) : ViewModel() {

    private val nowPlayingStylesFlow = combine(
        appPrefs.nowPlayingProgressStyleFlow,
        appPrefs.nowPlayingThemeFlow,
        appPrefs.trackInfoAlignmentFlow
    ) { p, t, a ->
        NowPlayingStyleSettings(p, t, a)
    }

    private val dacConfigFlow = combine(
        appPrefs.dacVolumeModeFlow,
        appPrefs.dsdOutputModeFlow,
        appPrefs.dsdGainCompensationFlow,
        appPrefs.dacSampleRateModeFlow,
        combine(
            appPrefs.dacFixedSampleRateFlow,
            appPrefs.dacUsbBufferSizeFlow
        ) { fixedSr, bufSize ->
            Pair(fixedSr, bufSize)
        }
    ) { volumeMode, dsdOutput, dsdGain, srMode, (fixedSr, bufSize) ->
        DacConfig(volumeMode, dsdOutput, dsdGain, srMode, fixedSr, bufSize)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        themePrefs.themeConfigFlow,
        combine(
            appPrefs.enabledTabIdsFlow,
            appPrefs.gaplessPlaybackFlow,
            appPrefs.highPrecisionResamplingFlow
        ) { tabIds, gapless, resampling ->
            TabsAndAudio1(tabIds, gapless, resampling)
        },
        combine(
            appPrefs.audioFocusDuckingFlow,
            appPrefs.replayGainFlow,
            libraryPrefs.scanOptionsFlow,
            libraryPrefs.autoScanOnLaunchFlow
        ) { ducking, replayGain, scanOptions, autoScan ->
            Audio2AndLibrary(ducking, replayGain, scanOptions, autoScan)
        },
        nowPlayingStylesFlow,
        dacConfigFlow
    ) { themeConfig, part1, part2, styles, dacConfig ->
        SettingsUiState(
            themeMode = themeConfig.themeMode,
            isDynamicColor = themeConfig.isDynamicColor,
            isArtworkAccent = themeConfig.isArtworkAccent,
            accentColorArgb = themeConfig.accentColorArgb,
            customBackgroundColorArgb = themeConfig.customBackgroundColorArgb,
            customFontColorArgb = themeConfig.customFontColorArgb,
            customThemePresetId = themeConfig.customThemePresetId,
            customFontPath = themeConfig.customFontPath,
            customFontName = themeConfig.customFontName,
            enabledTabIds = part1.tabIds,
            gaplessPlayback = part1.gapless,
            highPrecisionResampling = part1.resampling,
            audioFocusDucking = part2.ducking,
            replayGain = part2.replayGain,
            scanOptions = part2.scanOptions,
            autoScanEnabled = part2.autoScan,
            dacVolumeMode = dacConfig.volumeMode,
            dsdOutputMode = dacConfig.dsdOutputMode,
            dsdGainCompensation = dacConfig.dsdGain,
            dacSampleRateMode = dacConfig.sampleRateMode,
            dacFixedSampleRate = dacConfig.fixedSampleRate,
            dacUsbBufferSize = dacConfig.usbBufferSize,
            nowPlayingProgressStyle = styles.progressStyle,
            nowPlayingTheme = styles.theme,
            trackInfoAlignment = styles.alignment
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetCustomFont -> themePrefs.setCustomFont(action.path, action.name)
            is SettingsAction.SetThemeMode -> {
                themePrefs.setThemeMode(action.mode)
                if (action.mode != com.example.core.model.AppThemeMode.CUSTOM) {
                    themePrefs.setCustomBackgroundColor(null)
                    themePrefs.setCustomFontColor(null)
                    themePrefs.setAccentColor(0xFF2196F3L)
                    themePrefs.setCustomThemePresetId("nordic_night")
                } else {
                    themePrefs.setDynamicColor(false)
                }
            }
            is SettingsAction.SetDynamicColor -> themePrefs.setDynamicColor(action.enabled)
            is SettingsAction.SetArtworkAccent -> themePrefs.setArtworkAccent(action.enabled)
            is SettingsAction.SetAccentColor -> themePrefs.setAccentColor(action.colorArgb)
            is SettingsAction.SetCustomBackgroundColor -> themePrefs.setCustomBackgroundColor(action.colorArgb)
            is SettingsAction.SetCustomFontColor -> themePrefs.setCustomFontColor(action.colorArgb)
            is SettingsAction.SetCustomThemePresetId -> themePrefs.setCustomThemePresetId(action.presetId)
            is SettingsAction.SetEnabledTabIds -> appPrefs.setEnabledTabIds(action.tabIds)
            is SettingsAction.SetRootFolderDisplay -> appPrefs.setRootFolderDisplay(action.path)
            is SettingsAction.SetScanOptions -> libraryPrefs.updateScanOptions(action.options)
            is SettingsAction.SetAutoScanEnabled -> libraryPrefs.setAutoScanOnLaunch(action.enabled)
            is SettingsAction.AddIncludedFolder -> libraryPrefs.addIncludedFolder(action.path)
            is SettingsAction.RemoveIncludedFolder -> libraryPrefs.removeIncludedFolder(action.path)
            is SettingsAction.AddBlacklistedFolder -> libraryPrefs.addBlacklistedFolder(action.path)
            is SettingsAction.RemoveBlacklistedFolder -> libraryPrefs.removeBlacklistedFolder(action.path)
            is SettingsAction.SetFilterShortAudio -> libraryPrefs.setFilterShortAudio(action.enabled, action.minDurationSeconds)
            is SettingsAction.SetGaplessPlayback -> appPrefs.setGaplessPlayback(action.enabled)
            is SettingsAction.SetHighPrecisionResampling -> appPrefs.setHighPrecisionResampling(action.enabled)
            is SettingsAction.SetAudioFocusDucking -> appPrefs.setAudioFocusDucking(action.enabled)
            is SettingsAction.SetReplayGain -> appPrefs.setReplayGain(action.enabled)
            is SettingsAction.SetDacVolumeMode -> appPrefs.setDacVolumeMode(action.mode)
            is SettingsAction.SetDsdOutputMode -> appPrefs.setDsdOutputMode(action.mode)
            is SettingsAction.SetDsdGainCompensation -> appPrefs.setDsdGainCompensation(action.gain)
            is SettingsAction.SetDacSampleRateMode -> appPrefs.setDacSampleRateMode(action.mode)
            is SettingsAction.SetDacFixedSampleRate -> appPrefs.setDacFixedSampleRate(action.sampleRate)
            is SettingsAction.SetDacUsbBufferSize -> appPrefs.setDacUsbBufferSize(action.frames)
            is SettingsAction.SetNowPlayingProgressStyle -> appPrefs.setNowPlayingProgressStyle(action.style)
            is SettingsAction.SetNowPlayingTheme -> appPrefs.setNowPlayingTheme(action.theme)
            is SettingsAction.SetTrackInfoAlignment -> appPrefs.setTrackInfoAlignment(action.alignment)
        }
    }

    companion object {
        fun provideFactory(
            themePrefs: ThemePreferences,
            appPrefs: AppPreferences,
            libraryPrefs: LibraryPreferences
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(themePrefs, appPrefs, libraryPrefs) as T
            }
        }
    }
}
