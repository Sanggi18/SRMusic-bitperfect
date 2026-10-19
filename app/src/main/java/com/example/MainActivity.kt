package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import com.decent.usbaudio.UsbAudioPermissionHelper

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.let { UsbAudioPermissionHelper.handleIntent(this, it) }
        handleViewIntent(intent)
        enableEdgeToEdge()

        setContent {
            AppRoot()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.let { UsbAudioPermissionHelper.handleIntent(this, it) }
        handleViewIntent(intent)
    }

    private fun handleViewIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            val displayName = contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
            (application as SRMusicApp).playbackManager.playUri(uri, displayName)
        }
    }
}
