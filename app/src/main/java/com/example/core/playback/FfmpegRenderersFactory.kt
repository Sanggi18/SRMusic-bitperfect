package com.example.core.playback

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.media3.common.Format
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioOutput
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.decent.usbaudio.media3.UsbAudioSink
import com.decent.usbaudio.media3.UsbAudioSinkConfig
import com.example.core.audio.AudioOutputApi
import com.example.core.audio.OboeAudioOutputProvider
import com.example.core.audio.dac.DacVolumeMode

@UnstableApi
class FfmpegRenderersFactory(
    private val context: Context,
    private val audioOutputApi: AudioOutputApi = AudioOutputApi.AUDIOTRACK,
    private val isBitPerfectEnabled: Boolean = false,
    private val dacVolumeMode: DacVolumeMode = DacVolumeMode.SOFTWARE,
    private val dacExclusiveUsb: Boolean = true,
    private val onInputFormatConfigured: ((Format) -> Unit)? = null,
    private val onEffectiveOutputConfigured: ((AudioOutputProvider.OutputConfig) -> Unit)? = null,
    private val onBackendFailure: (AudioOutputApi, String) -> Unit = { _, _ -> },
    private val onUsbOutputUnavailable: () -> Unit = {},
    private val onUsbOutputActive: ((sampleRate: Int, channelCount: Int, bitDepth: Int, deviceName: String, volMode: DacVolumeMode, gain: Float) -> Unit)? = null
) : DefaultRenderersFactory(context) {

    companion object {
        private const val TAG = "SRMusicAudioDiag"
    }

    init {
        setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON)
        setEnableAudioTrackPlaybackParams(true)
    }

    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>
    ) {
        val ffmpegAvailable = FfmpegLibrary.isAvailable()

        if (ffmpegAvailable && (extensionRendererMode == EXTENSION_RENDERER_MODE_PREFER || extensionRendererMode == EXTENSION_RENDERER_MODE_ON)) {
            val extensionRendererIndex = out.size
            try {
                val ffmpegRenderer = FfmpegAudioRenderer(eventHandler, eventListener, audioSink)
                out.add(if (extensionRendererMode == EXTENSION_RENDERER_MODE_PREFER) 0 else extensionRendererIndex, ffmpegRenderer)
            } catch (e: Exception) {
                Log.e("SRMusic:Playback", "FfmpegRenderersFactory: Failed to instantiate FfmpegAudioRenderer", e)
            }
        }

        out.add(
            MediaCodecAudioRenderer(
                context,
                mediaCodecSelector,
                eventHandler,
                eventListener,
                audioSink
            )
        )
    }

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink {
        if (isBitPerfectEnabled) {
            val delegate = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(true)
                .build()
            val usbSink = UsbAudioSink(
                delegate = delegate,
                context = context,
                config = UsbAudioSinkConfig(
                    bitPerfectEnabled = true,
                    forceRouteToSpeaker = true,
                    dacVolumeMode = dacVolumeMode,
                    dacExclusiveUsb = dacExclusiveUsb
                ),
                onUsbOutputUnavailable = onUsbOutputUnavailable,
                onUsbOutputActive = onUsbOutputActive
            )
            return MonitoredForwardingAudioSink(usbSink) { inputFormat ->
                Log.d(
                    TAG,
                    "DECODER_INPUT_FORMAT rate=${inputFormat.sampleRate} channels=${inputFormat.channelCount} pcmEncoding=${inputFormat.pcmEncoding}"
                )
                onInputFormatConfigured?.invoke(inputFormat)
            }
        }

        val builder = DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)

        if (audioOutputApi == AudioOutputApi.AUDIOTRACK) {
            builder.setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            builder.setAudioOutputProvider(
                MonitoredAudioTrackOutputProvider(context) { outputConfig ->
                    Log.d(
                        TAG,
                        "EFFECTIVE_OUTPUT_CONFIG rate=${outputConfig.sampleRate} chMask=${outputConfig.channelMask} enc=${outputConfig.encoding}"
                    )
                    onEffectiveOutputConfigured?.invoke(outputConfig)
                }
            )
        } else {
            builder.setAudioOutputProvider(
                OboeAudioOutputProvider(
                    context = context,
                    requestedApi = audioOutputApi,
                    onBackendFailure = onBackendFailure
                )
            )
            builder.setEnableAudioTrackPlaybackParams(false)
        }

        val baseSink = builder.build()
        return MonitoredForwardingAudioSink(baseSink) { inputFormat ->
            Log.d(
                TAG,
                "DECODER_INPUT_FORMAT rate=${inputFormat.sampleRate} channels=${inputFormat.channelCount} pcmEncoding=${inputFormat.pcmEncoding}"
            )
            onInputFormatConfigured?.invoke(inputFormat)
        }
    }
}

@UnstableApi
private class MonitoredAudioTrackOutputProvider(
    context: Context,
    private val onOutputConfigEvaluated: (AudioOutputProvider.OutputConfig) -> Unit
) : AudioOutputProvider {
    private val delegate: AudioTrackAudioOutputProvider =
        AudioTrackAudioOutputProvider.Builder(context).build()

    override fun getFormatSupport(formatConfig: AudioOutputProvider.FormatConfig) =
        delegate.getFormatSupport(formatConfig)

    override fun getOutputConfig(formatConfig: AudioOutputProvider.FormatConfig): AudioOutputProvider.OutputConfig {
        val config = delegate.getOutputConfig(formatConfig)
        onOutputConfigEvaluated(config)
        return config
    }

    override fun getAudioOutput(outputConfig: AudioOutputProvider.OutputConfig): AudioOutput =
        delegate.getAudioOutput(outputConfig)

    override fun addListener(listener: AudioOutputProvider.Listener) = delegate.addListener(listener)
    override fun removeListener(listener: AudioOutputProvider.Listener) = delegate.removeListener(listener)
    override fun setClock(clock: Clock) = delegate.setClock(clock)
    override fun release() = delegate.release()
}

@UnstableApi
private class MonitoredForwardingAudioSink(
    delegate: AudioSink,
    private val onFormatConfigured: (Format) -> Unit
) : ForwardingAudioSink(delegate) {

    override fun configure(audioSinkConfig: AudioSink.AudioSinkConfig) {
        super.configure(audioSinkConfig)
        onFormatConfigured(audioSinkConfig.format)
    }
}
