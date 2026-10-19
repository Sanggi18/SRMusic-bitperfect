package com.example.core.audio

import android.content.Context
import android.media.AudioFormat
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioOutput
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

@UnstableApi
class OboeAudioOutputProvider(
    context: Context,
    private val requestedApi: AudioOutputApi,
    private val onBackendFailure: (AudioOutputApi, String) -> Unit = { _, _ -> }
) : AudioOutputProvider {

    companion object {
        private const val TAG = "SRMusicAudioDiag"
    }

    private val fallbackProvider: AudioTrackAudioOutputProvider =
        AudioTrackAudioOutputProvider.Builder(context).build()

    private val listeners = CopyOnWriteArrayList<AudioOutputProvider.Listener>()
    private val failureReported = AtomicBoolean(false)

    private fun reportBackendFailure(reason: String) {
        if (failureReported.compareAndSet(false, true)) {
            Log.w(TAG, "FALLBACK requested=$requestedApi reason=$reason")
            onBackendFailure(requestedApi, reason)
        }
    }

    private fun isOboeSupported(formatConfig: AudioOutputProvider.FormatConfig): Boolean {
        if (requestedApi == AudioOutputApi.AUDIOTRACK ||
            formatConfig.enableOffload ||
            formatConfig.enableTunneling
        ) {
            return false
        }

        val format = formatConfig.format
        val sampleMimeType = format.sampleMimeType
        val isRawPcm = sampleMimeType == null ||
                sampleMimeType == MimeTypes.AUDIO_RAW ||
                format.pcmEncoding != C.ENCODING_INVALID

        if (!isRawPcm) return false

        val encoding = format.pcmEncoding
        val isEncodingSupported = encoding == C.ENCODING_PCM_16BIT || encoding == C.ENCODING_PCM_FLOAT
        val isChannelCountSupported = format.channelCount in listOf(1, 2, 4, 6, 8)
        val isSampleRateValid = format.sampleRate > 0

        return isEncodingSupported && isChannelCountSupported && isSampleRateValid
    }

    override fun getFormatSupport(formatConfig: AudioOutputProvider.FormatConfig): AudioOutputProvider.FormatSupport {
        val supported = isOboeSupported(formatConfig)
        Log.i(
            TAG,
            "PROVIDER_FORMAT_SUPPORT requested=$requestedApi rate=${formatConfig.format.sampleRate} channels=${formatConfig.format.channelCount} encoding=${formatConfig.format.pcmEncoding} supported=$supported"
        )

        if (!supported) {
            return fallbackProvider.getFormatSupport(formatConfig)
        }

        return AudioOutputProvider.FormatSupport.Builder()
            .setFormatSupportLevel(AudioOutputProvider.FORMAT_SUPPORTED_DIRECTLY)
            .setIsFormatSupportedForOffload(false)
            .setIsGaplessSupportedForOffload(false)
            .setIsSpeedChangeSupportedForOffload(false)
            .build()
    }

    override fun getOutputConfig(formatConfig: AudioOutputProvider.FormatConfig): AudioOutputProvider.OutputConfig {
        if (!isOboeSupported(formatConfig)) {
            Log.i(
                TAG,
                "PROVIDER_OUTPUT_CONFIG delegating to AudioTrack fallback requested=$requestedApi rate=${formatConfig.format.sampleRate} channels=${formatConfig.format.channelCount}"
            )
            return fallbackProvider.getOutputConfig(formatConfig)
        }

        val pcmEncoding = if (formatConfig.format.pcmEncoding == C.ENCODING_PCM_FLOAT) {
            C.ENCODING_PCM_FLOAT
        } else {
            C.ENCODING_PCM_16BIT
        }

        val channelMask = when (formatConfig.format.channelCount) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            4 -> AudioFormat.CHANNEL_OUT_QUAD
            6 -> AudioFormat.CHANNEL_OUT_5POINT1
            8 -> AudioFormat.CHANNEL_OUT_7POINT1_SURROUND
            else -> AudioFormat.CHANNEL_OUT_STEREO
        }

        val sampleRate = if (formatConfig.format.sampleRate > 0) formatConfig.format.sampleRate else 48000

        Log.i(
            TAG,
            "PROVIDER_OUTPUT_CONFIG requested=$requestedApi rate=$sampleRate channels=${formatConfig.format.channelCount} encoding=$pcmEncoding"
        )

        return AudioOutputProvider.OutputConfig.Builder()
            .setEncoding(pcmEncoding)
            .setSampleRate(sampleRate)
            .setChannelMask(channelMask)
            .setAudioAttributes(formatConfig.audioAttributes)
            .setAudioSessionId(formatConfig.audioSessionId)
            .setIsOffload(false)
            .setIsTunneling(false)
            .setUsePlaybackParameters(false)
            .build()
    }

    override fun getAudioOutput(outputConfig: AudioOutputProvider.OutputConfig): AudioOutput {
        if (requestedApi == AudioOutputApi.AUDIOTRACK || outputConfig.isOffload || outputConfig.isTunneling) {
            Log.i(TAG, "PROVIDER delegating to AudioTrack (requestedApi=$requestedApi)")
            return fallbackProvider.getAudioOutput(outputConfig)
        }

        val channelCount = Integer.bitCount(outputConfig.channelMask)
        if (channelCount !in listOf(1, 2, 4, 6, 8) ||
            (outputConfig.encoding != C.ENCODING_PCM_16BIT && outputConfig.encoding != C.ENCODING_PCM_FLOAT) ||
            outputConfig.sampleRate <= 0
        ) {
            Log.w(
                TAG,
                "PROVIDER unsupported config: rate=${outputConfig.sampleRate} channels=$channelCount encoding=${outputConfig.encoding}; delegating to AudioTrack"
            )
            return fallbackProvider.getAudioOutput(outputConfig)
        }

        Log.i(
            TAG,
            "PROVIDER requested=$requestedApi rate=${outputConfig.sampleRate} channels=$channelCount encoding=${outputConfig.encoding} sessionId=${outputConfig.audioSessionId}"
        )

        val engine = OboeEngine()

        val created = engine.create(
            api = requestedApi,
            sampleRate = outputConfig.sampleRate,
            channelCount = channelCount,
            encoding = outputConfig.encoding,
            sessionId = outputConfig.audioSessionId
        )

        val actualApi = if (created) engine.getActualApi() else AudioOutputApi.AUDIOTRACK
        Log.i(TAG, "NATIVE_CREATE success=$created actualApi=$actualApi")

        val actualApiMatches = created && actualApi == requestedApi

        if (!actualApiMatches) {
            engine.release()
            val reason = if (!created) {
                "nativeCreate failed"
            } else {
                "Actual API ($actualApi) does not match requested API ($requestedApi)"
            }
            reportBackendFailure(reason)
            return fallbackProvider.getAudioOutput(outputConfig)
        }

        return OboeAudioOutput(
            outputConfig = outputConfig,
            requestedApi = requestedApi,
            oboeEngine = engine,
            onBackendFailure = { reason -> reportBackendFailure(reason) }
        )
    }

    override fun addListener(listener: AudioOutputProvider.Listener) {
        listeners.add(listener)
        fallbackProvider.addListener(listener)
    }

    override fun removeListener(listener: AudioOutputProvider.Listener) {
        listeners.remove(listener)
        fallbackProvider.removeListener(listener)
    }

    override fun setClock(clock: Clock) {
        fallbackProvider.setClock(clock)
    }

    override fun release() {
        fallbackProvider.release()
        listeners.clear()
    }
}
