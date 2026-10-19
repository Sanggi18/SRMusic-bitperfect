package com.decent.usbaudio.media3

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import com.decent.usbaudio.UsbAudioDevice
import com.decent.usbaudio.UsbAudioStream
import com.example.core.audio.dac.DacVolumeMode
import java.nio.ByteBuffer

/**
 * ExoPlayer [AudioSink] that sends PCM directly to a USB Audio Class DAC
 * via isochronous transfers, bypassing the Android audio stack.
 *
 * Implements:
 * - SPEC-05: PCM Integrity & USB Volume Delay Fix (no software gain on bit-perfect PCM,
 *            immediate hardware volume via UAC Feature Unit, perceptual volume curve).
 * - SPEC-06: Independent Bit Perfect & Exclusive USB Toggles (effective state separation,
 *            safe fallback when Exclusive USB is OFF).
 */
@OptIn(UnstableApi::class)
class UsbAudioSink(
    private val delegate: DefaultAudioSink,
    private val context: Context,
    private val config: UsbAudioSinkConfig = UsbAudioSinkConfig(),
    private val onUsbOutputUnavailable: () -> Unit = {},
    private val onUsbOutputActive: ((sampleRate: Int, channelCount: Int, bitDepth: Int, deviceName: String, volMode: DacVolumeMode, gain: Float) -> Unit)? = null
) : ForwardingAudioSink(delegate) {

    private var usbAudioStream: UsbAudioStream? = null
    private val usbAudioDevice = UsbAudioDevice.getInstance(context)
    private var usbStreamingThread: UsbStreamingThread? = null

    private var currentEncoding: Int = C.ENCODING_PCM_16BIT
    private var currentSampleRate: Int = 0
    private var currentChannelCount: Int = 0

    // Volume state (SPEC-05: Hardware volume path only; PCM is never modified)
    private val volumeLock = Any()
    @Volatile private var pendingVolume: Float = 1f

    private var delegateMuted: Boolean = false
    private var handleBufferCallCount: Long = 0

    private var usbStartMediaTimeUs: Long = 0L
    private var usbStartMediaTimeNeedsInit: Boolean = true
    private var usbStartFrames: Long = 0L
    private var startMediaTimeNeedsSync: Boolean = false
    private var handledEndOfStream: Boolean = false
    private var isReleasingNormal: Boolean = false
    private var hasReportedUnavailable: Boolean = false
    private var volumeReceiver: BroadcastReceiver? = null

    override fun configure(audioSinkConfig: AudioSink.AudioSinkConfig) {
        val inputFormat = audioSinkConfig.format
        val enc = inputFormat.pcmEncoding
        currentEncoding = enc
        handleBufferCallCount = 0
        hasReportedUnavailable = false
        handledEndOfStream = false
        startMediaTimeNeedsSync = true
        val sr = inputFormat.sampleRate.takeIf { it > 0 }
        val ch = inputFormat.channelCount.takeIf { it > 0 }

        Log.i(TAG, "configure: pcmEncoding=${when(enc) {
            C.ENCODING_PCM_FLOAT -> "FLOAT"
            C.ENCODING_PCM_16BIT -> "16BIT"
            C.ENCODING_PCM_24BIT -> "24BIT"
            C.ENCODING_PCM_32BIT -> "32BIT"
            else -> "UNKNOWN($enc)"
        }} rate=${inputFormat.sampleRate} ch=${inputFormat.channelCount} bitPerfect=${config.bitPerfectEnabled} volMode=${config.dacVolumeMode}")

        var usbConfiguredSuccessfully = false
        if (config.bitPerfectEnabled && sr != null && ch != null) {
            val device = usbAudioDevice.findUsbAudioDevice()
            if (device != null && usbAudioDevice.hasPermission(device)) {
                val success = configureUsbBitPerfect(sr, ch, enc)
                if (success) {
                    usbStartMediaTimeNeedsInit = true
                    usbStartFrames = 0L
                    startMediaTimeNeedsSync = false
                    handledEndOfStream = false
                    if (config.forceRouteToSpeaker) forceMediaToSpeaker()
                    super.configure(audioSinkConfig)
                    muteDelegateIfNeeded()
                    Log.i(TAG, "Delegate configured (muted, routed to speaker for dummy sync)")
                    usbConfiguredSuccessfully = true
                    return
                } else {
                    Log.w(TAG, "configureUsbBitPerfect failed; falling back to normal output")
                }
            } else if (device != null) {
                Log.w(TAG, "USB DAC found but permission not granted; falling back to normal output")
            } else {
                Log.w(TAG, "No USB DAC device found; falling back to normal output")
            }
        }

        super.configure(audioSinkConfig)
        if (usbAudioStream != null && !config.bitPerfectEnabled) {
            releaseUsbStream()
        }

        if (config.bitPerfectEnabled && !usbConfiguredSuccessfully) {
            unmuteDelegateIfNeeded()
            clearForcedRouting()
            if (!isReleasingNormal && !hasReportedUnavailable) {
                hasReportedUnavailable = true
                onUsbOutputUnavailable()
            }
        }
    }

    override fun handleBuffer(
        buffer: ByteBuffer,
        presentationTimeUs: Long,
        encodedAccessUnitCount: Int
    ): Boolean {
        val stream = usbAudioStream
        if (config.bitPerfectEnabled && stream?.isAlive == true) {
            muteDelegateIfNeeded()

            if (usbStartMediaTimeNeedsInit || startMediaTimeNeedsSync) {
                usbStartMediaTimeUs = maxOf(0L, presentationTimeUs)
                usbStartFrames = stream.framesWritten
                usbStartMediaTimeNeedsInit = false
                startMediaTimeNeedsSync = false
                handledEndOfStream = false
                Log.i(TAG, "Sync media time: startMediaTimeUs=$usbStartMediaTimeUs, usbStartFrames=$usbStartFrames")
            }

            val thread = usbStreamingThread ?: return true

            if (thread.queueSize() >= QUEUE_BACKPRESSURE_THRESHOLD) {
                return false
            }

            handleBufferCallCount++
            val snapshot: ByteBuffer = buffer.slice().order(buffer.order())

            // SPEC-05: Preserve 100% bit-perfect PCM integrity.
            // Samples are transferred directly to driver representation without software gain scaling.
            if (currentEncoding == C.ENCODING_PCM_FLOAT) {
                val totalSamples = snapshot.remaining() / 4
                if (totalSamples > 0) {
                    val floatBuf = FloatArray(totalSamples)
                    snapshot.asFloatBuffer().get(floatBuf)
                    if (handleBufferCallCount <= 3) {
                        Log.i(TAG, "handleBuffer #$handleBufferCallCount: FLOAT samples=$totalSamples (PCM intact)")
                    }
                    thread.enqueue(floatBuf)
                }
            } else {
                val remaining = snapshot.remaining()
                if (remaining > 0) {
                    val rawBytes = ByteArray(remaining)
                    snapshot.get(rawBytes)
                    if (handleBufferCallCount <= 3) {
                        val bps = PcmUtils.bytesPerSample(currentEncoding)
                        Log.i(TAG, "handleBuffer #$handleBufferCallCount: RAW ${bps * 8}bit bytes=$remaining (PCM intact)")
                    }
                    thread.enqueueRaw(rawBytes, currentEncoding)
                }
            }

            buffer.position(buffer.limit())
            return true
        }

        unmuteDelegateIfNeeded()
        return super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
    }

    override fun getCurrentPositionUs(sourceEnded: Boolean): Long {
        if (config.bitPerfectEnabled) {
            val stream = usbAudioStream
            if (stream != null && stream.isAlive) {
                if (usbStartMediaTimeNeedsInit) return AudioSink.CURRENT_POSITION_NOT_SET
                val frames = stream.framesWritten
                val sr = currentSampleRate
                return if (sr > 0) {
                    val framesPlayedThisTrack = maxOf(0L, frames - usbStartFrames)
                    usbStartMediaTimeUs + framesPlayedThisTrack * C.MICROS_PER_SECOND / sr
                } else AudioSink.CURRENT_POSITION_NOT_SET
            }
        }
        return super.getCurrentPositionUs(sourceEnded)
    }

    override fun handleDiscontinuity() {
        super.handleDiscontinuity()
        startMediaTimeNeedsSync = true
        handledEndOfStream = false
        Log.i(TAG, "handleDiscontinuity: marked startMediaTimeNeedsSync=true, reset handledEndOfStream")
    }

    override fun hasPendingData(): Boolean {
        if (config.bitPerfectEnabled && usbAudioStream?.isAlive == true) {
            return usbStreamingThread?.hasPendingData() == true
        }
        return super.hasPendingData()
    }

    override fun isEnded(): Boolean {
        if (config.bitPerfectEnabled && usbAudioStream?.isAlive == true) {
            return handledEndOfStream && !hasPendingData()
        }
        return super.isEnded()
    }

    override fun playToEndOfStream() {
        handledEndOfStream = true
        super.playToEndOfStream()
    }

    override fun play() {
        super.play()
        usbStreamingThread?.resumeStreaming()
    }

    override fun pause() {
        usbStreamingThread?.pauseStreaming()
        super.pause()
    }

    override fun setVolume(volume: Float) {
        val safeVol = if (volume.isFinite()) volume.coerceIn(0f, 1f) else 0f
        synchronized(volumeLock) {
            pendingVolume = safeVol
        }

        if (config.bitPerfectEnabled && usbAudioStream?.isAlive == true) {
            muteDelegateIfNeeded()
            // SPEC-05: Update DAC hardware volume immediately without delay
            applyHardwareVolume()
        } else {
            unmuteDelegateIfNeeded()
            super.setVolume(safeVol)
        }
    }

    override fun flush() {
        super.flush()
        usbStreamingThread?.flush()
        usbAudioStream?.flush()
        usbStartMediaTimeNeedsInit = true
        usbStartFrames = 0L
        startMediaTimeNeedsSync = false
        handledEndOfStream = false
    }

    override fun reset() {
        super.reset()
        usbStartMediaTimeNeedsInit = true
        usbStartFrames = 0L
        startMediaTimeNeedsSync = false
        handledEndOfStream = false
    }

    override fun release() {
        isReleasingNormal = true
        try {
            releaseUsbStream()
        } finally {
            super.release()
            isReleasingNormal = false
        }
    }

    /**
     * Calculates the target DAC volume ratio (0.0 to 1.0) using current Android system volume
     * and player volume, applying a quadratic perceptual curve to fix low volume levels being too loud.
     */
    private fun calculateEffectiveHardwareVolume(): Float {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return pendingVolume.coerceIn(0f, 1f)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val isMute = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        } else {
            curVol <= 0
        }
        if (isMute || curVol <= 0) return 0f

        val linearRatio = (curVol.toFloat() / maxVol).coerceIn(0f, 1f)
        val safePending = if (pendingVolume.isFinite()) pendingVolume.coerceIn(0f, 1f) else 0f

        // Quadratic curve mapping: fixes low volume levels being too loud on DACs
        val combinedLinear = (linearRatio * safePending).coerceIn(0f, 1f)
        return (combinedLinear * combinedLinear).coerceIn(0f, 1f)
    }

    /**
     * Applies the current effective volume directly to the DAC's UAC Feature Unit.
     */
    private fun applyHardwareVolume(): Boolean {
        val volRatio = calculateEffectiveHardwareVolume()
        val ok = usbAudioDevice.setHardwareVolume(volRatio)
        if (!ok) {
            Log.e(TAG, "USB_FALLBACK reason=VOLUME_CONTROL_FAILED: Hardware volume command failed on DAC for ratio=$volRatio")
            releaseUsbStream()
            if (!hasReportedUnavailable) {
                hasReportedUnavailable = true
                onUsbOutputUnavailable()
            }
            return false
        }
        return true
    }

    /**
     * Initializes hardware volume safety on the DAC before any PCM is streamed.
     * Fails if DAC does not have UAC hardware volume support.
     */
    private fun initializeSafeVolume(): Boolean {
        if (!usbAudioDevice.hasHardwareVolumeSupport()) {
            Log.e(TAG, "USB_FALLBACK reason=VOLUME_CONTROL_FAILED: DAC has no UAC Feature Unit volume control. Aborting direct USB for hearing safety.")
            return false
        }
        if (!applyHardwareVolume()) {
            Log.e(TAG, "USB_FALLBACK reason=VOLUME_CONTROL_FAILED: Initial hardware volume control transfer failed. Aborting direct USB for hearing safety.")
            return false
        }
        Log.i(TAG, "Volume safety initialized via hardware DAC control")
        return true
    }

    private fun onSystemVolumeChanged() {
        if (config.bitPerfectEnabled && usbAudioStream?.isAlive == true) {
            applyHardwareVolume()
        }
    }

    private fun registerVolumeReceiver() {
        if (volumeReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    if (streamType == AudioManager.STREAM_MUSIC || streamType == -1) {
                        onSystemVolumeChanged()
                    }
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            volumeReceiver = receiver
            Log.d(TAG, "Volume receiver registered")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register volume receiver: ${e.message}")
        }
    }

    private fun unregisterVolumeReceiver() {
        volumeReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {}
            volumeReceiver = null
            Log.d(TAG, "Volume receiver unregistered")
        }
    }

    private fun configureUsbBitPerfect(sampleRate: Int, channelCount: Int, encoding: Int): Boolean {
        if (sampleRate == currentSampleRate && channelCount == currentChannelCount
            && usbAudioStream?.isAlive == true) {
            Log.d(TAG, "USB stream cached for rate=$sampleRate ch=$channelCount — reusing")
            applyHardwareVolume()
            return true
        }

        if (usbAudioStream != null) releaseUsbStream()

        val usbDevice = usbAudioDevice.findUsbAudioDevice() ?: run {
            Log.w(TAG, "configureUsbBitPerfect: no USB audio device found")
            return false
        }

        var deviceInfo = usbAudioDevice.openDevice(usbDevice)
        if (deviceInfo == null) {
            Log.e(TAG, "Failed to open USB device")
            return false
        }

        // SPEC-05: Initialize safe volume on DAC hardware before creating stream or sending PCM
        if (!initializeSafeVolume()) {
            usbAudioDevice.closeDevice()
            return false
        }

        val bitDepth = deviceInfo.bestBitDepth
        val altSetting = deviceInfo.bestAltSetting
        Log.i(
            TAG,
            "Bit-perfect setup: alt=$altSetting usb=${bitDepth}bit clockSource=0x${deviceInfo.clockSourceId.toString(16)}"
        )

        var stream = UsbAudioStream(
            fd = deviceInfo.fd,
            interfaceId = deviceInfo.interfaceId,
            endpointOut = deviceInfo.endpointOutAddress,
            endpointFeedback = deviceInfo.endpointFeedbackAddress,
            sampleRate = sampleRate,
            channelCount = channelCount,
            bitDepth = bitDepth,
            maxPacketSize = deviceInfo.maxPacketSize
        )

        if (!stream.isReady) {
            Log.e(TAG, "USB stream creation failed")
            stream.release()
            usbAudioDevice.closeDevice()
            return false
        }

        // xHCI transition sequence
        if (!usbAudioDevice.setAltSetting(0)) {
            Log.w(TAG, "setAlt(0) failed — reopening device...")
            usbAudioDevice.closeDevice()
            stream.release()
            deviceInfo = usbAudioDevice.openDevice(usbDevice)
            if (deviceInfo == null) {
                Log.e(TAG, "Device reopen failed")
                return false
            }
            stream = UsbAudioStream(
                fd = deviceInfo.fd,
                interfaceId = deviceInfo.interfaceId,
                endpointOut = deviceInfo.endpointOutAddress,
                endpointFeedback = deviceInfo.endpointFeedbackAddress,
                sampleRate = sampleRate,
                channelCount = channelCount,
                bitDepth = bitDepth,
                maxPacketSize = deviceInfo.maxPacketSize
            )
            if (!stream.isReady) {
                Log.e(TAG, "USB stream recreation failed after reopen")
                stream.release()
                usbAudioDevice.closeDevice()
                return false
            }
            Log.i(TAG, "Device reopened with fresh fd=${deviceInfo.fd}")
        }

        usbAudioDevice.setSampleRate(sampleRate)
        val clockValid = usbAudioDevice.readClockValid()
        Log.i(TAG, "SET_CUR=$sampleRate, CLOCK_VALID=$clockValid")

        usbAudioDevice.setAltSetting(0)
        val altResult = usbAudioDevice.setAltSetting(altSetting)
        Log.i(TAG, "setAlt($altSetting) result=$altResult")

        val startResult = stream.start()
        Log.i(TAG, "stream.start() result=$startResult")
        if (!startResult) {
            Log.e(TAG, "Failed to start USB stream")
            stream.release()
            usbAudioDevice.closeDevice()
            return false
        }

        currentSampleRate = sampleRate
        currentChannelCount = channelCount
        usbAudioStream = stream

        usbStreamingThread = UsbStreamingThread(stream).also { it.start() }

        registerVolumeReceiver()
        val curVolRatio = calculateEffectiveHardwareVolume()
        onUsbOutputActive?.invoke(sampleRate, channelCount, bitDepth, deviceInfo.deviceName, config.dacVolumeMode, curVolRatio)
        Log.i(
            TAG,
            "USB bit-perfect stream ACTIVE: rate=$sampleRate ch=$channelCount bits=$bitDepth device=${deviceInfo.deviceName} volumeRatio=$curVolRatio"
        )
        return true
    }

    private fun releaseUsbStream() {
        unregisterVolumeReceiver()
        usbStartMediaTimeNeedsInit = true
        usbStartFrames = 0L
        startMediaTimeNeedsSync = false
        handledEndOfStream = false
        val thread = usbStreamingThread
        usbStreamingThread = null
        thread?.stop()

        val stream = usbAudioStream
        usbAudioStream = null
        stream?.stop()
        if (stream != null) {
            val drained = stream.drainUrbs()
            Log.i(TAG, "USB stream drained $drained URBs")
            stream.release()
        }

        // SPEC-08: Explicitly restore DAC hardware volume to unity gain (1.0 = 0 dB)
        // before releasing device, so normal Android audio stack plays at expected levels.
        if (usbAudioDevice.hasHardwareVolumeSupport()) {
            usbAudioDevice.setHardwareVolume(1.0f)
            Log.i(TAG, "Restored DAC hardware volume to 1.0 (0 dB) before release")
        }

        clearForcedRouting()
        unmuteDelegateIfNeeded()
        usbAudioDevice.closeDevice()
        Log.i(TAG, "USB audio stream released, forced routing cleared, and USB device closed")
    }

    private fun muteDelegateIfNeeded() {
        if (!delegateMuted) {
            super.setVolume(0f)
            delegateMuted = true
        }
    }

    private fun unmuteDelegateIfNeeded() {
        if (delegateMuted) {
            super.setVolume(pendingVolume)
            delegateMuted = false
            Log.i(TAG, "Delegate unmuted, volume restored to $pendingVolume")
        }
    }

    private fun forceMediaToSpeaker() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val speaker = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speaker != null) {
                delegate.setPreferredDevice(speaker)
                Log.i(TAG, "Delegate routed to speaker")
            }
        } catch (e: Exception) {
            Log.w(TAG, "forceMediaToSpeaker failed: ${e.message}")
        }
    }

    private fun clearForcedRouting() {
        try {
            delegate.setPreferredDevice(null)
            Log.i(TAG, "Delegate preferred device cleared")
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "UsbAudioSink"
        private const val QUEUE_BACKPRESSURE_THRESHOLD = 96
    }
}
