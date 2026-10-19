package com.example.core.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.core.audio.AudioOutputApi
import com.example.core.model.AppFolderStyle
import com.example.core.model.AppGridMode
import com.example.core.model.AppSortCriteria
import com.example.core.audio.dac.DacSampleRateMode
import com.example.core.audio.dac.DacVolumeMode
import com.example.core.audio.dac.DsdGainCompensation
import com.example.core.audio.dac.DsdOutputMode
import com.example.core.model.NowPlayingProgressStyle
import com.example.core.model.NowPlayingTheme
import com.example.core.model.TrackInfoAlignment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "srmusic_prefs"
        private const val KEY_BOTTOM_TABS = "bottom_tabs"
        private const val KEY_ROOT_FOLDER_DISPLAY = "root_folder_display"
        private const val KEY_SONG_SORT_CRITERIA = "song_sort_criteria"
        private const val KEY_SONG_IS_ASCENDING = "song_is_ascending"
        private const val KEY_ARTIST_SORT_CRITERIA = "artist_sort_criteria"
        private const val KEY_ARTIST_IS_ASCENDING = "artist_is_ascending"
        private const val KEY_ARTIST_GRID_MODE = "artist_grid_mode"
        private const val KEY_ALBUM_SORT_CRITERIA = "album_sort_criteria"
        private const val KEY_ALBUM_IS_ASCENDING = "album_is_ascending"
        private const val KEY_ALBUM_GRID_MODE = "album_grid_mode"
        private const val KEY_FOLDER_STYLE = "folder_style"
        private const val KEY_GAPLESS_PLAYBACK = "gapless_playback"
        private const val KEY_HIGH_PRECISION_RESAMPLING = "high_precision_resampling"
        private const val KEY_AUDIO_FOCUS_DUCKING = "audio_focus_ducking"
        private const val KEY_REPLAY_GAIN = "replay_gain"
        private const val KEY_AUDIO_OUTPUT_API = "audio_output_api"
        private const val KEY_DAC_VOLUME_MODE = "dac_volume_mode"
        private const val KEY_DSD_OUTPUT_MODE = "dsd_output_mode"
        private const val KEY_DSD_GAIN_COMPENSATION = "dsd_gain_compensation"
        private const val KEY_DAC_SAMPLE_RATE_MODE = "dac_sample_rate_mode"
        private const val KEY_DAC_FIXED_SAMPLE_RATE = "dac_fixed_sample_rate"
        private const val KEY_DAC_USB_BUFFER_SIZE = "dac_usb_buffer_size"
        private const val KEY_SHUFFLE_TRACK_IDS = "saved_shuffle_track_ids"
        private const val KEY_NOW_PLAYING_PROGRESS_STYLE = "now_playing_progress_style"
        private const val KEY_NOW_PLAYING_THEME = "now_playing_theme"
        private const val KEY_TRACK_INFO_ALIGNMENT = "track_info_alignment"
        private const val KEY_FIRST_LAUNCH_SETUP_COMPLETED = "first_launch_setup_completed"

        val DEFAULT_TAB_IDS = listOf("home", "song", "artist", "album", "folder")
    }

    private val _nowPlayingProgressStyleFlow = MutableStateFlow(loadNowPlayingProgressStyle())
    val nowPlayingProgressStyleFlow: StateFlow<NowPlayingProgressStyle> = _nowPlayingProgressStyleFlow.asStateFlow()

    private val _nowPlayingThemeFlow = MutableStateFlow(loadNowPlayingTheme())
    val nowPlayingThemeFlow: StateFlow<NowPlayingTheme> = _nowPlayingThemeFlow.asStateFlow()

    private val _trackInfoAlignmentFlow = MutableStateFlow(loadTrackInfoAlignment())
    val trackInfoAlignmentFlow: StateFlow<TrackInfoAlignment> = _trackInfoAlignmentFlow.asStateFlow()

    private val _enabledTabIdsFlow = MutableStateFlow(loadEnabledTabIds())
    val enabledTabIdsFlow: StateFlow<List<String>> = _enabledTabIdsFlow.asStateFlow()

    private val _rootFolderDisplayFlow = MutableStateFlow(prefs.getString(KEY_ROOT_FOLDER_DISPLAY, "/sdcard/Music/") ?: "/sdcard/Music/")
    val rootFolderDisplayFlow: StateFlow<String> = _rootFolderDisplayFlow.asStateFlow()

    private val _gaplessPlaybackFlow = MutableStateFlow(prefs.getBoolean(KEY_GAPLESS_PLAYBACK, false))
    val gaplessPlaybackFlow: StateFlow<Boolean> = _gaplessPlaybackFlow.asStateFlow()

    private val _highPrecisionResamplingFlow = MutableStateFlow(prefs.getBoolean(KEY_HIGH_PRECISION_RESAMPLING, false))
    val highPrecisionResamplingFlow: StateFlow<Boolean> = _highPrecisionResamplingFlow.asStateFlow()

    private val _audioFocusDuckingFlow = MutableStateFlow(prefs.getBoolean(KEY_AUDIO_FOCUS_DUCKING, false))
    val audioFocusDuckingFlow: StateFlow<Boolean> = _audioFocusDuckingFlow.asStateFlow()

    private val _replayGainFlow = MutableStateFlow(prefs.getBoolean(KEY_REPLAY_GAIN, false))
    val replayGainFlow: StateFlow<Boolean> = _replayGainFlow.asStateFlow()

    private val _audioOutputApiFlow = MutableStateFlow(loadAudioOutputApi())
    val audioOutputApiFlow: StateFlow<AudioOutputApi> = _audioOutputApiFlow.asStateFlow()

    private val _dacVolumeModeFlow = MutableStateFlow(loadDacVolumeMode())
    val dacVolumeModeFlow: StateFlow<DacVolumeMode> = _dacVolumeModeFlow.asStateFlow()

    private val _dsdOutputModeFlow = MutableStateFlow(loadDsdOutputMode())
    val dsdOutputModeFlow: StateFlow<DsdOutputMode> = _dsdOutputModeFlow.asStateFlow()

    private val _dsdGainCompensationFlow = MutableStateFlow(loadDsdGainCompensation())
    val dsdGainCompensationFlow: StateFlow<DsdGainCompensation> = _dsdGainCompensationFlow.asStateFlow()

    private val _dacSampleRateModeFlow = MutableStateFlow(loadDacSampleRateMode())
    val dacSampleRateModeFlow: StateFlow<DacSampleRateMode> = _dacSampleRateModeFlow.asStateFlow()

    private val _dacFixedSampleRateFlow = MutableStateFlow(prefs.getInt(KEY_DAC_FIXED_SAMPLE_RATE, 48000))
    val dacFixedSampleRateFlow: StateFlow<Int> = _dacFixedSampleRateFlow.asStateFlow()

    private val _dacUsbBufferSizeFlow = MutableStateFlow(prefs.getInt(KEY_DAC_USB_BUFFER_SIZE, 256))
    val dacUsbBufferSizeFlow: StateFlow<Int> = _dacUsbBufferSizeFlow.asStateFlow()

    private fun loadEnabledTabIds(): List<String> {
        val saved = prefs.getString(KEY_BOTTOM_TABS, null)
        if (!saved.isNullOrBlank()) {
            val list = saved.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            if (list.size >= 2) return list
        }
        return DEFAULT_TAB_IDS
    }

    fun setEnabledTabIds(tabIds: List<String>) {
        val encoded = tabIds.joinToString(",") { it.lowercase() }
        prefs.edit().putString(KEY_BOTTOM_TABS, encoded).apply()
        _enabledTabIdsFlow.value = tabIds
    }

    fun setRootFolderDisplay(path: String) {
        prefs.edit().putString(KEY_ROOT_FOLDER_DISPLAY, path).apply()
        _rootFolderDisplayFlow.value = path
    }

    fun getSongSortCriteria(): AppSortCriteria {
        val name = prefs.getString(KEY_SONG_SORT_CRITERIA, AppSortCriteria.TITLE.name) ?: AppSortCriteria.TITLE.name
        return try { AppSortCriteria.valueOf(name) } catch (_: Exception) { AppSortCriteria.TITLE }
    }

    fun setSongSortCriteria(criteria: AppSortCriteria) {
        prefs.edit().putString(KEY_SONG_SORT_CRITERIA, criteria.name).apply()
    }

    fun getSongIsAscending(): Boolean = prefs.getBoolean(KEY_SONG_IS_ASCENDING, true)

    fun setSongIsAscending(isAscending: Boolean) {
        prefs.edit().putBoolean(KEY_SONG_IS_ASCENDING, isAscending).apply()
    }

    fun getArtistSortCriteria(): AppSortCriteria {
        val name = prefs.getString(KEY_ARTIST_SORT_CRITERIA, AppSortCriteria.ARTIST.name) ?: AppSortCriteria.ARTIST.name
        return try { AppSortCriteria.valueOf(name) } catch (_: Exception) { AppSortCriteria.ARTIST }
    }

    fun setArtistSortCriteria(criteria: AppSortCriteria) {
        prefs.edit().putString(KEY_ARTIST_SORT_CRITERIA, criteria.name).apply()
    }

    fun getArtistIsAscending(): Boolean = prefs.getBoolean(KEY_ARTIST_IS_ASCENDING, true)

    fun setArtistIsAscending(isAscending: Boolean) {
        prefs.edit().putBoolean(KEY_ARTIST_IS_ASCENDING, isAscending).apply()
    }

    fun getArtistGridMode(): AppGridMode {
        val name = prefs.getString(KEY_ARTIST_GRID_MODE, AppGridMode.GRID_2.name) ?: AppGridMode.GRID_2.name
        return try { AppGridMode.valueOf(name) } catch (_: Exception) { AppGridMode.GRID_2 }
    }

    fun setArtistGridMode(mode: AppGridMode) {
        prefs.edit().putString(KEY_ARTIST_GRID_MODE, mode.name).apply()
    }

    fun getAlbumSortCriteria(): AppSortCriteria {
        val name = prefs.getString(KEY_ALBUM_SORT_CRITERIA, AppSortCriteria.ALBUM.name) ?: AppSortCriteria.ALBUM.name
        return try { AppSortCriteria.valueOf(name) } catch (_: Exception) { AppSortCriteria.ALBUM }
    }

    fun setAlbumSortCriteria(criteria: AppSortCriteria) {
        prefs.edit().putString(KEY_ALBUM_SORT_CRITERIA, criteria.name).apply()
    }

    fun getAlbumIsAscending(): Boolean = prefs.getBoolean(KEY_ALBUM_IS_ASCENDING, true)

    fun setAlbumIsAscending(isAscending: Boolean) {
        prefs.edit().putBoolean(KEY_ALBUM_IS_ASCENDING, isAscending).apply()
    }

    fun getAlbumGridMode(): AppGridMode {
        val name = prefs.getString(KEY_ALBUM_GRID_MODE, AppGridMode.GRID_2.name) ?: AppGridMode.GRID_2.name
        return try { AppGridMode.valueOf(name) } catch (_: Exception) { AppGridMode.GRID_2 }
    }

    fun setAlbumGridMode(mode: AppGridMode) {
        prefs.edit().putString(KEY_ALBUM_GRID_MODE, mode.name).apply()
    }

    fun getFolderStyle(): AppFolderStyle {
        val name = prefs.getString(KEY_FOLDER_STYLE, AppFolderStyle.ICON.name) ?: AppFolderStyle.ICON.name
        return try { AppFolderStyle.valueOf(name) } catch (_: Exception) { AppFolderStyle.ICON }
    }

    fun setFolderStyle(style: AppFolderStyle) {
        prefs.edit().putString(KEY_FOLDER_STYLE, style.name).apply()
    }

    fun setGaplessPlayback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GAPLESS_PLAYBACK, enabled).apply()
        _gaplessPlaybackFlow.value = enabled
    }

    fun setHighPrecisionResampling(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIGH_PRECISION_RESAMPLING, enabled).apply()
        _highPrecisionResamplingFlow.value = enabled
    }

    fun setAudioFocusDucking(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUDIO_FOCUS_DUCKING, enabled).apply()
        _audioFocusDuckingFlow.value = enabled
    }

    fun setReplayGain(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REPLAY_GAIN, enabled).apply()
        _replayGainFlow.value = enabled
    }

    private fun loadAudioOutputApi(): AudioOutputApi {
        val name = prefs.getString(KEY_AUDIO_OUTPUT_API, AudioOutputApi.AUDIOTRACK.name)
        return try { AudioOutputApi.valueOf(name ?: AudioOutputApi.AUDIOTRACK.name) } catch (_: Exception) { AudioOutputApi.AUDIOTRACK }
    }

    fun setAudioOutputApi(api: AudioOutputApi) {
        prefs.edit().putString(KEY_AUDIO_OUTPUT_API, api.name).apply()
        _audioOutputApiFlow.value = api
    }

    private fun loadDacVolumeMode(): DacVolumeMode {
        val name = prefs.getString(KEY_DAC_VOLUME_MODE, DacVolumeMode.SOFTWARE.name)
        return try { DacVolumeMode.valueOf(name ?: DacVolumeMode.SOFTWARE.name) } catch (_: Exception) { DacVolumeMode.SOFTWARE }
    }

    fun setDacVolumeMode(mode: DacVolumeMode) {
        prefs.edit().putString(KEY_DAC_VOLUME_MODE, mode.name).apply()
        _dacVolumeModeFlow.value = mode
    }

    fun getDacVolumeMode(): DacVolumeMode = _dacVolumeModeFlow.value

    private fun loadDsdOutputMode(): DsdOutputMode {
        val name = prefs.getString(KEY_DSD_OUTPUT_MODE, DsdOutputMode.AUTO.name)
        return try { DsdOutputMode.valueOf(name ?: DsdOutputMode.AUTO.name) } catch (_: Exception) { DsdOutputMode.AUTO }
    }

    fun setDsdOutputMode(mode: DsdOutputMode) {
        prefs.edit().putString(KEY_DSD_OUTPUT_MODE, mode.name).apply()
        _dsdOutputModeFlow.value = mode
    }

    private fun loadDsdGainCompensation(): DsdGainCompensation {
        val name = prefs.getString(KEY_DSD_GAIN_COMPENSATION, DsdGainCompensation.PLUS_0DB.name)
        return try { DsdGainCompensation.valueOf(name ?: DsdGainCompensation.PLUS_0DB.name) } catch (_: Exception) { DsdGainCompensation.PLUS_0DB }
    }

    fun setDsdGainCompensation(gain: DsdGainCompensation) {
        prefs.edit().putString(KEY_DSD_GAIN_COMPENSATION, gain.name).apply()
        _dsdGainCompensationFlow.value = gain
    }

    private fun loadDacSampleRateMode(): DacSampleRateMode {
        val name = prefs.getString(KEY_DAC_SAMPLE_RATE_MODE, DacSampleRateMode.FOLLOW_SOURCE.name)
        return try { DacSampleRateMode.valueOf(name ?: DacSampleRateMode.FOLLOW_SOURCE.name) } catch (_: Exception) { DacSampleRateMode.FOLLOW_SOURCE }
    }

    fun setDacSampleRateMode(mode: DacSampleRateMode) {
        prefs.edit().putString(KEY_DAC_SAMPLE_RATE_MODE, mode.name).apply()
        _dacSampleRateModeFlow.value = mode
    }

    fun setDacFixedSampleRate(sampleRate: Int) {
        prefs.edit().putInt(KEY_DAC_FIXED_SAMPLE_RATE, sampleRate).apply()
        _dacFixedSampleRateFlow.value = sampleRate
    }

    fun setDacUsbBufferSize(frames: Int) {
        prefs.edit().putInt(KEY_DAC_USB_BUFFER_SIZE, frames).apply()
        _dacUsbBufferSizeFlow.value = frames
    }

    fun loadShuffleTrackIds(): List<Long> {
        val str = prefs.getString(KEY_SHUFFLE_TRACK_IDS, null) ?: return emptyList()
        return str.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    fun setShuffleTrackIds(ids: List<Long>) {
        val str = ids.joinToString(",")
        prefs.edit().putString(KEY_SHUFFLE_TRACK_IDS, str).apply()
    }

    fun getShuffleTrackIds(): List<Long> = loadShuffleTrackIds()

    private fun loadNowPlayingProgressStyle(): NowPlayingProgressStyle {
        val name = prefs.getString(KEY_NOW_PLAYING_PROGRESS_STYLE, NowPlayingProgressStyle.ROUNDED_BAR.name)
        return try { NowPlayingProgressStyle.valueOf(name ?: NowPlayingProgressStyle.ROUNDED_BAR.name) } catch (_: Exception) { NowPlayingProgressStyle.ROUNDED_BAR }
    }

    fun setNowPlayingProgressStyle(style: NowPlayingProgressStyle) {
        prefs.edit().putString(KEY_NOW_PLAYING_PROGRESS_STYLE, style.name).apply()
        _nowPlayingProgressStyleFlow.value = style
    }

    private fun loadNowPlayingTheme(): NowPlayingTheme {
        val name = prefs.getString(KEY_NOW_PLAYING_THEME, NowPlayingTheme.DEFAULT.name)
        return try { NowPlayingTheme.valueOf(name ?: NowPlayingTheme.DEFAULT.name) } catch (_: Exception) { NowPlayingTheme.DEFAULT }
    }

    fun setNowPlayingTheme(theme: NowPlayingTheme) {
        prefs.edit().putString(KEY_NOW_PLAYING_THEME, theme.name).apply()
        _nowPlayingThemeFlow.value = theme
    }

    private fun loadTrackInfoAlignment(): TrackInfoAlignment {
        val name = prefs.getString(KEY_TRACK_INFO_ALIGNMENT, TrackInfoAlignment.LEFT.name)
        return try { TrackInfoAlignment.valueOf(name ?: TrackInfoAlignment.LEFT.name) } catch (_: Exception) { TrackInfoAlignment.LEFT }
    }

    fun setTrackInfoAlignment(alignment: TrackInfoAlignment) {
        prefs.edit().putString(KEY_TRACK_INFO_ALIGNMENT, alignment.name).apply()
        _trackInfoAlignmentFlow.value = alignment
    }

    fun isFirstLaunchSetupCompleted(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH_SETUP_COMPLETED, false)

    fun setFirstLaunchSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_SETUP_COMPLETED, completed).apply()
    }
}
