package com.decent.usbaudio.media3

import com.example.core.audio.dac.DacVolumeMode

/**
 * Configuration for [UsbAudioSink].
 *
 * @param bitPerfectEnabled When true, audio is sent directly to the USB DAC
 *        bypassing the Android audio stack entirely.
 * @param forceRouteToSpeaker When true, the delegate AudioTrack is routed to
 *        the built-in speaker to prevent AudioFlinger from opening the USB device.
 * @param dacVolumeMode Volume mode to use: SOFTWARE or HARDWARE (DAC).
 * @param dacExclusiveUsb When true, exclusive access to the USB device is allowed.
 */
data class UsbAudioSinkConfig(
    val bitPerfectEnabled: Boolean = true,
    val forceRouteToSpeaker: Boolean = true,
    val dacVolumeMode: DacVolumeMode = DacVolumeMode.SOFTWARE,
    val dacExclusiveUsb: Boolean = true
)
