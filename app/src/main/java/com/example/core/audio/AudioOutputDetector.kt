package com.example.core.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

enum class AudioOutputType {
    SPEAKER,
    WIRED,
    BLUETOOTH,
    USB
}

data class AudioOutputInfo(
    val displayName: String,
    val type: AudioOutputType
) {
    val isUsbDac: Boolean get() = type == AudioOutputType.USB
}

object AudioOutputDetector {
    private fun cleanDacName(raw: String): String {
        var name = raw
        val dashIndex = name.lastIndexOf(" - ")
        if (dashIndex != -1) {
            name = name.substring(dashIndex + 3)
        }
        return name.trim().takeIf { it.isNotEmpty() } ?: raw.trim()
    }

    fun getCurrentOutput(context: Context): AudioOutputInfo {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        val usb = outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
        if (usb != null) {
            val raw = usb.productName?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val displayName = if (raw != null) "USB Audio : ${cleanDacName(raw)}" else "USB DAC"
            return AudioOutputInfo(displayName = displayName, type = AudioOutputType.USB)
        }

        if (audioManager.isBluetoothA2dpOn) {
            val bt = outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            val name = bt?.productName?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                ?: "Bluetooth Audio"
            return AudioOutputInfo(displayName = name, type = AudioOutputType.BLUETOOTH)
        }

        if (audioManager.isWiredHeadsetOn) {
            val wired = outputs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            }
            val name = wired?.productName?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                ?: "Wired Headset"
            return AudioOutputInfo(displayName = name, type = AudioOutputType.WIRED)
        }

        return AudioOutputInfo(displayName = "Phone Speaker", type = AudioOutputType.SPEAKER)
    }
}
