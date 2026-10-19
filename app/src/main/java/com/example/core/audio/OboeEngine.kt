package com.example.core.audio

import android.util.Log
import java.nio.ByteBuffer

class OboeEngine {
    companion object {
        private const val TAG = "SRMusic:Oboe"
        private var isLibraryLoaded = false

        init {
            try {
                System.loadLibrary("audio")
                isLibraryLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load libaudio.so", e)
                isLibraryLoaded = false
            }
        }
    }

    private external fun nativeCreate(
        apiOrdinal: Int,
        sampleRate: Int,
        channelCount: Int,
        encoding: Int,
        sessionId: Int
    ): Long

    private external fun nativeStart(handle: Long): Boolean
    private external fun nativePause(handle: Long)
    private external fun nativeStop(handle: Long)
    private external fun nativeFlush(handle: Long)
    private external fun nativeSetVolume(handle: Long, volume: Float)
    private external fun nativeWritePcm(
        handle: Long,
        byteBuffer: ByteBuffer,
        offset: Int,
        byteCount: Int
    ): Int

    private external fun nativeGetConsumedFrames(handle: Long): Long
    private external fun nativeGetBufferSizeInFrames(handle: Long): Long
    private external fun nativeGetAudioSessionId(handle: Long): Int
    private external fun nativeGetActualApi(handle: Long): Int
    private external fun nativeRelease(handle: Long)

    private var nativeHandle: Long = 0L

    val isCreated: Boolean
        get() = nativeHandle != 0L

    fun create(
        api: AudioOutputApi,
        sampleRate: Int,
        channelCount: Int,
        encoding: Int,
        sessionId: Int
    ): Boolean {
        release()
        if (!isLibraryLoaded) {
            Log.e(TAG, "OboeEngine cannot create: native library not loaded")
            return false
        }
        if (api == AudioOutputApi.AUDIOTRACK) {
            Log.w(TAG, "OboeEngine cannot create for AudioTrack (use Media3 default)")
            return false
        }
        return try {
            nativeHandle = nativeCreate(
                apiOrdinal = api.ordinal,
                sampleRate = sampleRate,
                channelCount = channelCount,
                encoding = encoding,
                sessionId = sessionId
            )
            nativeHandle != 0L
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "nativeCreate failed with UnsatisfiedLinkError", e)
            false
        }
    }

    fun start(): Boolean {
        if (!isLibraryLoaded || nativeHandle == 0L) return false
        return try {
            nativeStart(nativeHandle)
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "nativeStart failed", e)
            false
        }
    }

    fun pause() {
        if (isLibraryLoaded && nativeHandle != 0L) {
            try {
                nativePause(nativeHandle)
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "nativePause failed", e)
            }
        }
    }

    fun stop() {
        if (isLibraryLoaded && nativeHandle != 0L) {
            try {
                nativeStop(nativeHandle)
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "nativeStop failed", e)
            }
        }
    }

    fun flush() {
        if (isLibraryLoaded && nativeHandle != 0L) {
            try {
                nativeFlush(nativeHandle)
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "nativeFlush failed", e)
            }
        }
    }

    fun setVolume(volume: Float) {
        if (!isLibraryLoaded || nativeHandle == 0L) return
        val safeVolume = if (volume.isFinite()) volume.coerceIn(0f, 1f) else 1f
        try {
            nativeSetVolume(nativeHandle, safeVolume)
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "nativeSetVolume failed", e)
        }
    }

    fun writePcm(buffer: ByteBuffer, offset: Int, byteCount: Int): Int {
        if (!isLibraryLoaded || nativeHandle == 0L || byteCount <= 0) return 0
        return try {
            nativeWritePcm(nativeHandle, buffer, offset, byteCount)
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "nativeWritePcm failed", e)
            -1
        }
    }

    fun getConsumedFrames(): Long {
        if (!isLibraryLoaded || nativeHandle == 0L) return 0L
        return try {
            nativeGetConsumedFrames(nativeHandle)
        } catch (_: UnsatisfiedLinkError) {
            0L
        }
    }

    fun getBufferSizeInFrames(): Long {
        if (!isLibraryLoaded || nativeHandle == 0L) return 0L
        return try {
            nativeGetBufferSizeInFrames(nativeHandle)
        } catch (_: UnsatisfiedLinkError) {
            0L
        }
    }

    fun getAudioSessionId(): Int {
        if (!isLibraryLoaded || nativeHandle == 0L) return 0
        return try {
            nativeGetAudioSessionId(nativeHandle)
        } catch (_: UnsatisfiedLinkError) {
            0
        }
    }

    fun getActualApi(): AudioOutputApi {
        if (!isLibraryLoaded || nativeHandle == 0L) return AudioOutputApi.AUDIOTRACK
        return try {
            val ord = nativeGetActualApi(nativeHandle)
            AudioOutputApi.entries.getOrElse(ord) { AudioOutputApi.AUDIOTRACK }
        } catch (_: UnsatisfiedLinkError) {
            AudioOutputApi.AUDIOTRACK
        }
    }

    fun release() {
        if (isLibraryLoaded && nativeHandle != 0L) {
            try {
                nativeRelease(nativeHandle)
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "nativeRelease failed", e)
            }
            nativeHandle = 0L
        }
    }
}
