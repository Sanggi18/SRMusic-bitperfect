package com.example.ui.player.components

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.core.audio.AudioOutputDetector
import com.example.core.audio.AudioOutputInfo

@Composable
fun rememberAudioOutputInfo(): AudioOutputInfo {
    val context = LocalContext.current
    var info by remember { mutableStateOf(AudioOutputDetector.getCurrentOutput(context)) }
    DisposableEffect(context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                info = AudioOutputDetector.getCurrentOutput(context)
            }
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                info = AudioOutputDetector.getCurrentOutput(context)
            }
        }
        audioManager.registerAudioDeviceCallback(callback, null)
        info = AudioOutputDetector.getCurrentOutput(context)
        onDispose { audioManager.unregisterAudioDeviceCallback(callback) }
    }
    return info
}
