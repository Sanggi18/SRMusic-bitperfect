package com.example.core.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.core.model.AppThemeMode
import com.example.core.model.ThemeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "srmusic_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_IS_DYNAMIC_COLOR = "is_dynamic_color"
        private const val KEY_IS_ARTWORK_ACCENT = "is_artwork_accent"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_CUSTOM_BG_COLOR = "custom_bg_color"
        private const val KEY_CUSTOM_FONT_COLOR = "custom_font_color"
        private const val KEY_CUSTOM_THEME_PRESET_ID = "custom_theme_preset_id"
        private const val KEY_CUSTOM_FONT_PATH = "custom_font_path"
        private const val KEY_CUSTOM_FONT_NAME = "custom_font_name"
        private const val DEFAULT_ACCENT_ARGB = 0xFF64B5F6L
    }

    private val _themeConfigFlow = MutableStateFlow(loadSettings())
    val themeConfigFlow: StateFlow<ThemeConfig> = _themeConfigFlow.asStateFlow()

    private fun loadSettings(): ThemeConfig {
        val savedModeStr = prefs.getString(KEY_THEME_MODE, AppThemeMode.LIGHT.name)
        val mode = try {
            AppThemeMode.valueOf(savedModeStr ?: AppThemeMode.LIGHT.name)
        } catch (_: Exception) {
            AppThemeMode.LIGHT
        }
        val dynamic = prefs.getBoolean(KEY_IS_DYNAMIC_COLOR, true)
        val artworkAccent = prefs.getBoolean(KEY_IS_ARTWORK_ACCENT, false)
        val accentVal = prefs.getLong(KEY_ACCENT_COLOR, DEFAULT_ACCENT_ARGB)
        val bgVal = prefs.getLong(KEY_CUSTOM_BG_COLOR, -1L)
        val fontVal = prefs.getLong(KEY_CUSTOM_FONT_COLOR, -1L)
        val presetId = prefs.getString(KEY_CUSTOM_THEME_PRESET_ID, "nordic_night") ?: "nordic_night"
        val fontPath = prefs.getString(KEY_CUSTOM_FONT_PATH, null)
        val fontName = prefs.getString(KEY_CUSTOM_FONT_NAME, null)

        return ThemeConfig(
            themeMode = mode,
            isDynamicColor = dynamic,
            isArtworkAccent = artworkAccent,
            accentColorArgb = accentVal,
            customBackgroundColorArgb = if (bgVal != -1L) bgVal else null,
            customFontColorArgb = if (fontVal != -1L) fontVal else null,
            customThemePresetId = presetId,
            customFontPath = fontPath,
            customFontName = fontName
        )
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeConfigFlow.value = _themeConfigFlow.value.copy(themeMode = mode)
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_DYNAMIC_COLOR, enabled).apply()
        _themeConfigFlow.value = _themeConfigFlow.value.copy(isDynamicColor = enabled)
    }

    fun setArtworkAccent(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_ARTWORK_ACCENT, enabled).apply()
        _themeConfigFlow.value = _themeConfigFlow.value.copy(isArtworkAccent = enabled)
    }

    fun setAccentColor(colorArgb: Long) {
        prefs.edit().putLong(KEY_ACCENT_COLOR, colorArgb).apply()
        _themeConfigFlow.value = _themeConfigFlow.value.copy(accentColorArgb = colorArgb)
    }

    fun setCustomBackgroundColor(colorArgb: Long?) {
        val value = colorArgb ?: -1L
        prefs.edit().putLong(KEY_CUSTOM_BG_COLOR, value).apply()
        _themeConfigFlow.value = _themeConfigFlow.value.copy(customBackgroundColorArgb = colorArgb)
    }

    fun setCustomFontColor(colorArgb: Long?) {
        val value = colorArgb ?: -1L
        prefs.edit().putLong(KEY_CUSTOM_FONT_COLOR, value).apply()
        _themeConfigFlow.value = _themeConfigFlow.value.copy(customFontColorArgb = colorArgb)
    }

    fun setCustomThemePresetId(presetId: String) {
        prefs.edit().putString(KEY_CUSTOM_THEME_PRESET_ID, presetId).apply()
        _themeConfigFlow.value = _themeConfigFlow.value.copy(customThemePresetId = presetId)
    }

    fun setCustomFont(path: String?, name: String?) {
        prefs.edit()
            .putString(KEY_CUSTOM_FONT_PATH, path)
            .putString(KEY_CUSTOM_FONT_NAME, name)
            .apply()
        _themeConfigFlow.value = _themeConfigFlow.value.copy(
            customFontPath = path,
            customFontName = name
        )
    }
}
