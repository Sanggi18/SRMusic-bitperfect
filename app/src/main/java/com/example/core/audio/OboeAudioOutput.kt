package com.example.core.audio

import android.media.AudioDeviceInfo
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioOutput
import androidx.media3.exoplayer.audio.AudioOutputProvider
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@UnstableApi
class OboeAudioOutput(
    private val outputConfig: AudioOutputProvider.OutputConfig,
    private val requestedApi: AudioOutputApi,
    private val oboeEngine: OboeEngine,
    private val onBackendFailure: (String) -> Unit
) : AudioOutput {

    companion object {
        private const val TAG = "SRMusicAudioDiag"
    }

    private val sampleRate: Int = outputConfig.sampleRate
    private val channelCount: Int = Integer.bitCount(outputConfig.channelMask).coerceAtLeast(2)
    private val encoding: Int = outputConfig.encoding
    private val bytesPerSample: Int = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
    private val bytesPerFrame: Int = channelCount * bytesPerSample

    private val configuredSessionId: Int = outputConfig.audioSessionId
    private val listeners = CopyOnWriteArrayList<AudioOutput.Listener>()

    private var currentPlaybackParameters: PlaybackParameters = PlaybackParameters.DEFAULT
    private var positionAnchorSet: Boolean = false
    private var anchorConsumedFrames: Long = 0L

    // Internal tracking states
    private val outputFailed = AtomicBoolean(false)
    private val totalFramesWritten = AtomicLong(0L)
    private val lastConsumedFrames = AtomicLong(0L)
    private var lastProgressTimeMs: Long = SystemClock.elapsedRealtime()
    private var playbackRequested: Boolean = false

    // Diagnostic rate-limited logging timestamps
    private var firstWriteLogged: Boolean = false
    private var lastWriteLogTimeMs: Long = 0L
    private var lastConsumeLogTimeMs: Long = 0L

    fun getActualApi(): AudioOutputApi = oboeEngine.getActualApi()

    private fun markOutputFailed(reason: String) {
        if (outputFailed.compareAndSet(false, true)) {
            Log.e(TAG, "OUTPUT_FAILURE reason=$reason")
            onBackendFailure(reason)
        }
    }

    override fun play() {
        if (outputFailed.get()) return

        val actualApi = getActualApi()
        Log.i(TAG, "PLAY requestedApi=$requestedApi actualApi=$actualApi")

        if (!oboeEngine.start()) {
            markOutputFailed("nativeStart returned false")
            return
        }

        playbackRequested = true
        lastConsumedFrames.set(oboeEngine.getConsumedFrames())
        lastProgressTimeMs = SystemClock.elapsedRealtime()
    }

    override fun pause() {
        Log.i(TAG, "PAUSE requestedApi=$requestedApi")
        playbackRequested = false
        oboeEngine.pause()
    }

    override fun stop() {
        Log.i(TAG, "STOP requestedApi=$requestedApi")
        playbackRequested = false
        oboeEngine.stop()
    }

    override fun flush() {
        Log.i(TAG, "FLUSH requestedApi=$requestedApi")
        oboeEngine.flush()
        positionAnchorSet = false
        anchorConsumedFrames = 0L
        totalFramesWritten.set(0L)
        lastConsumedFrames.set(0L)
        lastProgressTimeMs = SystemClock.elapsedRealtime()
    }

    override fun release() {
        Log.i(TAG, "RELEASE requestedApi=$requestedApi")
        playbackRequested = false
        oboeEngine.release()
        for (listener in listeners) {
            listener.onReleased()
        }
        listeners.clear()
    }

    override fun write(
        buffer: ByteBuffer,
        encodedAccessUnitCount: Int,
        presentationTimeUs: Long
    ): Boolean {
        if (outputFailed.get()) return false

        val remaining = buffer.remaining()
        if (remaining == 0) return true

        val alignedBytes = (remaining / bytesPerFrame) * bytesPerFrame
        if (alignedBytes == 0) {
            return false
        }

        if (!positionAnchorSet) {
            anchorConsumedFrames = oboeEngine.getConsumedFrames()
            positionAnchorSet = true
        }

        val currentPosition = buffer.position()
        val directBuffer = if (buffer.isDirect) {
            buffer
        } else {
            val direct = ByteBuffer.allocateDirect(alignedBytes)
            val dup = buffer.duplicate()
            dup.limit(currentPosition + alignedBytes)
            direct.put(dup)
            direct.flip()
            direct
        }

        val offset = if (buffer.isDirect) currentPosition else 0
        val written = oboeEngine.writePcm(directBuffer, offset, alignedBytes)

        if (written < 0) {
            markOutputFailed("nativeWritePcm failed")
            return false
        }

        if (written == 0) {
            // Ring buffer is full; backpressure
            return false
        }

        if (written % bytesPerFrame != 0) {
            markOutputFailed("nativeWritePcm returned non-frame-aligned byte count: $written (bpf=$bytesPerFrame)")
            return false
        }

        buffer.position(currentPosition + written)
        val newTotalWritten = totalFramesWritten.addAndGet((written / bytesPerFrame).toLong())

        val now = SystemClock.elapsedRealtime()
        if (!firstWriteLogged) {
            firstWriteLogged = true
            lastWriteLogTimeMs = now
            Log.i(TAG, "PCM_WRITE firstWrite bytesAccepted=$written totalWrittenFrames=$newTotalWritten")
        } else if (now - lastWriteLogTimeMs >= 1000L) {
            lastWriteLogTimeMs = now
            Log.i(TAG, "PCM_WRITE bytesAccepted=$written totalWrittenFrames=$newTotalWritten")
        }

        val posUs = getPositionUs()
        for (listener in listeners) {
            listener.onPositionAdvancing(posUs)
        }

        return buffer.remaining() == 0
    }

    override fun getPositionUs(): Long {
        val currentConsumed = oboeEngine.getConsumedFrames()

        val now = SystemClock.elapsedRealtime()
        if (now - lastConsumeLogTimeMs >= 1000L && currentConsumed > 0L) {
            lastConsumeLogTimeMs = now
            Log.i(TAG, "PCM_CONSUME frames=$currentConsumed")
        }

        if (!positionAnchorSet) {
            return 0L
        }
        val framesSinceAnchor = (currentConsumed - anchorConsumedFrames).coerceAtLeast(0L)
        return (framesSinceAnchor * 1_000_000L) / sampleRate
    }

    override fun getSampleRate(): Int = sampleRate

    override fun getBufferSizeInFrames(): Long = oboeEngine.getBufferSizeInFrames()

    override fun getAudioSessionId(): Int {
        val sessionId = oboeEngine.getAudioSessionId()
        return if (sessionId > 0 && sessionId != C.AUDIO_SESSION_ID_UNSET) {
            sessionId
        } else if (configuredSessionId > 0 && configuredSessionId != C.AUDIO_SESSION_ID_UNSET) {
            configuredSessionId
        } else {
            0
        }
    }

    override fun isOffloadedPlayback(): Boolean = false

    override fun isStalled(): Boolean {
        if (outputFailed.get()) return true
        if (!playbackRequested) return false

        val consumed = oboeEngine.getConsumedFrames()
        val written = totalFramesWritten.get()

        if (written > consumed) {
            val previousConsumed = lastConsumedFrames.get()
            val now = SystemClock.elapsedRealtime()

            if (consumed > previousConsumed) {
                lastConsumedFrames.set(consumed)
                lastProgressTimeMs = now
            } else if (now - lastProgressTimeMs > 750L) {
                markOutputFailed("Audio callback stalled (no consumed frame progress for 750ms)")
                return true
            }
        } else {
            lastProgressTimeMs = SystemClock.elapsedRealtime()
        }

        return false
    }

    override fun setVolume(volume: Float) {
        // Media3 volume is a linear gain in [0, 1]. Keep invalid values from
        // propagating into the native realtime callback.
        val safeVolume = if (volume.isFinite()) volume.coerceIn(0f, 1f) else 1f
        Log.i(TAG, "SET_VOLUME requested=$volume safeVolume=$safeVolume requestedApi=$requestedApi")
        oboeEngine.setVolume(safeVolume)
    }

    override fun getPlaybackParameters(): PlaybackParameters = currentPlaybackParameters

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        currentPlaybackParameters = playbackParameters
    }

    override fun addListener(listener: AudioOutput.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: AudioOutput.Listener) {
        listeners.remove(listener)
    }

    override fun setOffloadDelayPadding(delayInFrames: Int, paddingInFrames: Int) {}

    override fun setOffloadEndOfStream() {}

    override fun attachAuxEffect(effectId: Int) {}

    override fun setAuxEffectSendLevel(level: Float) {}

    override fun setPreferredDevice(audioDeviceInfo: AudioDeviceInfo?) {}
}
