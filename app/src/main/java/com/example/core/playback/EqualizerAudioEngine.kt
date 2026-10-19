package com.example.core.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log
import com.example.core.data.preferences.EqualizerPreferences
import com.example.core.model.EqualizerConfig

class EqualizerAudioEngine(
    private val context: Context,
    private val prefs: EqualizerPreferences
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var sessionId: Int = 0

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        applyConfig(prefs.loadEqualizerConfig())
    }

    init {
        prefs.registerChangeListener(prefListener)
    }

    fun attachToSession(newSessionId: Int) {
        if (newSessionId == sessionId && equalizer != null) return
        if (newSessionId == 0) return

        Log.d("EqualizerAudioEngine", "Attaching to audio session: $newSessionId (previous: $sessionId)")
        release()
        sessionId = newSessionId

        try {
            equalizer = Equalizer(0, newSessionId)
        } catch (e: Exception) {
            Log.e("EqualizerAudioEngine", "Failed to initialize Equalizer for session $newSessionId", e)
            equalizer = null
        }

        try {
            bassBoost = BassBoost(0, newSessionId)
        } catch (e: Exception) {
            Log.e("EqualizerAudioEngine", "Failed to initialize BassBoost for session $newSessionId", e)
            bassBoost = null
        }

        applyConfig(prefs.loadEqualizerConfig())
    }

    fun applyConfig(config: EqualizerConfig) {
        val eq = equalizer
        val bb = bassBoost
        val active = config.isEqualizerEnabled

        try {
            eq?.enabled = active
        } catch (e: Exception) {
            Log.w("EqualizerAudioEngine", "Failed to set Equalizer enabled=$active", e)
        }

        try {
            bb?.enabled = active
        } catch (e: Exception) {
            Log.w("EqualizerAudioEngine", "Failed to set BassBoost enabled=$active", e)
        }

        if (!active || eq == null) return

        try {
            val deviceBands = eq.numberOfBands.toInt()
            if (deviceBands <= 0) return
            val uiBands = config.bands // 10 values, -15..+15 dB
            val levelRange = try { eq.bandLevelRange } catch (_: Exception) { null }

            for (i in 0 until deviceBands) {
                // Linear resample 10 UI bands → N device bands
                val pos = if (deviceBands == 1) 0f else i * (uiBands.size - 1).toFloat() / (deviceBands - 1)
                val lo = pos.toInt().coerceIn(0, uiBands.size - 1)
                val hi = (lo + 1).coerceIn(0, uiBands.size - 1)
                val frac = pos - lo
                var gainDb = uiBands[lo] * (1 - frac) + uiBands[hi] * frac
                gainDb += config.preampGain

                // Vocal boost: bell curve di tengah (puncak 0.5), 0..10 → 0..7.5 dB
                // Treble boost: ramp naik ke frekuensi tinggi, 0..10 → 0..7.5 dB
                val vocalDb = config.vocalBoost * 0.75f
                val trebleDb = config.trebleBoost * 0.75f
                val normPos = if (deviceBands == 1) 0.5f else i.toFloat() / (deviceBands - 1)
                val vocalWeight = (1f - kotlin.math.abs(normPos - 0.5f) * 2f).coerceIn(0f, 1f)
                val trebleWeight = ((normPos - 0.4f) / 0.6f).coerceIn(0f, 1f)
                gainDb += vocalDb * vocalWeight
                gainDb += trebleDb * trebleWeight

                val mb = (gainDb * 100).toInt()
                val clampedMb = if (levelRange != null && levelRange.size >= 2) {
                    mb.coerceIn(levelRange[0].toInt(), levelRange[1].toInt())
                } else mb
                eq.setBandLevel(i.toShort(), clampedMb.toShort())
            }

            // BassBoost: 0..10 → 0..1000
            bb?.let {
                if (it.strengthSupported) {
                    val strength = (config.bassBoost * 100).toInt().coerceIn(0, 1000).toShort()
                    it.setStrength(strength)
                }
            }
        } catch (e: Exception) {
            Log.e("EqualizerAudioEngine", "Error applying equalizer config", e)
        }
    }

    fun release() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        sessionId = 0
    }

    fun destroy() {
        prefs.unregisterChangeListener(prefListener)
        release()
    }
}
