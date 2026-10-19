package com.example.core.playback

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.decent.usbaudio.UsbAudioDevice
import com.decent.usbaudio.UsbAudioPermissionHelper
import com.example.MainActivity
import com.example.SRMusicApp
import com.example.core.audio.AudioOutputApi
import com.example.core.audio.dac.DacVolumeMode
import com.example.core.data.preferences.EqualizerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MusicService : MediaSessionService() {

    companion object {
        private const val TAG = "SRMusicAudioDiag"

        @Volatile
        var activePlayer: ExoPlayer? = null
            private set

        @Volatile
        private var instance: MusicService? = null

        fun requestAudioOutputApiChange(api: AudioOutputApi) {
            val service = instance ?: return
            Handler(service.mainLooper).post {
                service.onUserRequestedAudioOutputApiChange(api)
            }
        }

        fun requestBitPerfectChange(enabled: Boolean) {
            val service = instance ?: return
            Handler(service.mainLooper).post {
                service.onUserRequestedBitPerfectChange(enabled)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private var equalizerEngine: EqualizerAudioEngine? = null

    // Separate persistent user preference from temporary effective backend
    private var userSelectedAudioApi: AudioOutputApi = AudioOutputApi.AUDIOTRACK
    private var effectiveAudioApi: AudioOutputApi = AudioOutputApi.AUDIOTRACK

    // User preference vs effective runtime status for Bit Perfect
    private var userBitPerfectEnabled: Boolean = false
    private var effectiveBitPerfectActive: Boolean = false

    // Guard against concurrent or re-entrant player rebuilds and track pending requests
    private var isRebuilding = false
    private var pendingRebuildRequested = false
    private var playerGeneration: Long = 0L
    private var activationRequestId: Long = 0L

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                Log.i(TAG, "ACTION_USB_DEVICE_DETACHED received")
                if (effectiveBitPerfectActive) {
                    onUsbStreamUnavailable(reason = "DETACHED")
                }
            } else if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                Log.i(TAG, "ACTION_USB_DEVICE_ATTACHED received")
                if (userBitPerfectEnabled && !effectiveBitPerfectActive) {
                    val deviceManager = UsbAudioDevice.getInstance(this@MusicService)
                    val usbDevice = deviceManager.findUsbAudioDevice()
                    if (usbDevice != null) {
                        if (deviceManager.hasPermission(usbDevice)) {
                            Log.i(TAG, "USB_ATTACH_RECONNECT: Permission already granted; activating Bit Perfect")
                            rebuildPlayer(userSelectedAudioApi, isBitPerfect = true, force = true)
                        } else {
                            val reqId = ++activationRequestId
                            deviceManager.requestPermission(usbDevice) { granted ->
                                Handler(mainLooper).post {
                                    if (reqId == activationRequestId && userBitPerfectEnabled) {
                                        if (granted) {
                                            Log.i(TAG, "USB_ATTACH_PERMISSION_GRANTED: Activating direct USB Exclusive")
                                            rebuildPlayer(userSelectedAudioApi, isBitPerfect = true, force = true)
                                        } else {
                                            Log.w(TAG, "USB_ATTACH_BLOCKED reason=PERMISSION_DENIED")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onUserRequestedAudioOutputApiChange(api: AudioOutputApi) {
        userSelectedAudioApi = api
        Log.i(TAG, "USER_SELECT_AUDIO_API: $api")
        rebuildPlayer(api, isBitPerfect = effectiveBitPerfectActive)
    }

    private fun onUserRequestedBitPerfectChange(enabled: Boolean) {
        userBitPerfectEnabled = enabled
        val reqId = ++activationRequestId
        Log.i(TAG, "BITPERFECT_REQUEST enabled=$enabled")

        if (enabled) {
            val deviceManager = UsbAudioDevice.getInstance(this)
            val usbDevice = deviceManager.findUsbAudioDevice()
            if (usbDevice == null) {
                Log.w(TAG, "BITPERFECT_BLOCKED reason=NO_DAC: No USB DAC connected")
                if (effectiveBitPerfectActive) {
                    rebuildPlayer(userSelectedAudioApi, isBitPerfect = false, force = true)
                }
                return
            }
            if (!deviceManager.hasPermission(usbDevice)) {
                deviceManager.requestPermission(usbDevice) { granted ->
                    Handler(mainLooper).post {
                        if (reqId == activationRequestId && userBitPerfectEnabled) {
                            if (granted) {
                                Log.i(TAG, "BITPERFECT_PERMISSION_GRANTED: Activating direct USB Exclusive")
                                rebuildPlayer(userSelectedAudioApi, isBitPerfect = true, force = true)
                            } else {
                                Log.w(TAG, "BITPERFECT_BLOCKED reason=PERMISSION_DENIED")
                                if (effectiveBitPerfectActive) {
                                    rebuildPlayer(userSelectedAudioApi, isBitPerfect = false, force = true)
                                }
                            }
                        } else {
                            Log.i(TAG, "BITPERFECT_REQUEST cancelled or superseded before permission response; ignoring")
                        }
                    }
                }
                return
            }
            rebuildPlayer(userSelectedAudioApi, isBitPerfect = true, force = true)
        } else {
            // Bit Perfect OFF: Teardown direct USB and return to normal Android output
            rebuildPlayer(userSelectedAudioApi, isBitPerfect = false, force = true)
        }
    }

    private fun onUsbStreamUnavailable(reason: String = "CONFIGURE_FAILED") {
        Log.w(TAG, "USB_FALLBACK reason=$reason; falling back to normal output $userSelectedAudioApi")
        effectiveBitPerfectActive = false
        if (::player.isInitialized) {
            rebuildPlayer(userSelectedAudioApi, isBitPerfect = false, force = true)
        }
    }

    @OptIn(UnstableApi::class)
    private fun createPlayer(api: AudioOutputApi, isBitPerfect: Boolean): ExoPlayer {
        val currentGen = ++playerGeneration
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val appPrefs = (application as? SRMusicApp)?.appPreferences
        val dacVolumeMode = appPrefs?.getDacVolumeMode() ?: DacVolumeMode.SOFTWARE
        val dacExclusiveUsb = isBitPerfect

        val renderersFactory = FfmpegRenderersFactory(
            context = this,
            audioOutputApi = api,
            isBitPerfectEnabled = isBitPerfect,
            dacVolumeMode = dacVolumeMode,
            dacExclusiveUsb = dacExclusiveUsb,
            onInputFormatConfigured = { format ->
                Log.d(
                    TAG,
                    "DECODER_OUTPUT_PCM: rate=${format.sampleRate} ch=${format.channelCount} enc=${format.pcmEncoding}"
                )
            },
            onEffectiveOutputConfigured = { outputConfig ->
                Log.d(
                    TAG,
                    "EFFECTIVE_OUTPUT_CONFIG: rate=${outputConfig.sampleRate} mask=${outputConfig.channelMask} enc=${outputConfig.encoding}"
                )
            },
            onBackendFailure = { failedApi, reason ->
                Handler(mainLooper).post {
                    if (effectiveAudioApi == failedApi && failedApi != AudioOutputApi.AUDIOTRACK) {
                        Log.w(
                            TAG,
                            "FALLBACK requested=$failedApi reason=$reason; rebuilding to AudioTrack"
                        )
                        rebuildPlayer(AudioOutputApi.AUDIOTRACK, isBitPerfect = false, force = true)
                    }
                }
            },
            onUsbOutputUnavailable = {
                Handler(mainLooper).post {
                    onUsbStreamUnavailable(reason = "CONFIGURE_FAILED")
                }
            },
            onUsbOutputActive = { sampleRate, channelCount, bitDepth, deviceName, volMode, gain ->
                Handler(mainLooper).post {
                    effectiveBitPerfectActive = true
                    Log.i(
                        TAG,
                        "USB_STREAM_ACTIVE rate=$sampleRate ch=$channelCount bits=$bitDepth device=$deviceName volMode=$volMode gain=$gain"
                    )
                }
            }
        )

        return ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
            }
    }

    private fun setupPlayerListener(exoPlayer: ExoPlayer, eqEngine: EqualizerAudioEngine) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != 0 && audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    eqEngine.attachToSession(audioSessionId)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val sid = exoPlayer.audioSessionId
                    if (sid != 0 && sid != C.AUDIO_SESSION_ID_UNSET) {
                        eqEngine.attachToSession(sid)
                    }
                    Log.i(
                        TAG,
                        "PLAYER_READY backend=$effectiveAudioApi isBitPerfect=$effectiveBitPerfectActive audioSessionId=$sid posMs=${exoPlayer.currentPosition}"
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(
                    TAG,
                    "PLAYER_ERROR backend=$effectiveAudioApi errorCode=${error.errorCode} message=${error.message}",
                    error
                )
            }
        })
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this

        val eqPrefs = EqualizerPreferences(this)
        val eqConfig = eqPrefs.loadEqualizerConfig()
        val initialApi = eqConfig.selectedAudioOutputApi
        val initialBitPerfect = eqConfig.isBitPerfectEnabled
        userSelectedAudioApi = initialApi
        effectiveAudioApi = initialApi
        userBitPerfectEnabled = initialBitPerfect

        val startWithBitPerfect = initialBitPerfect && UsbAudioDevice.getInstance(this).let {
            val d = it.findUsbAudioDevice()
            d != null && it.hasPermission(d)
        }
        effectiveBitPerfectActive = false

        Log.i(TAG, "INIT_PLAYER initialApi=$initialApi bitPerfectRequested=$initialBitPerfect attemptBitPerfect=$startWithBitPerfect")

        val eqEngine = EqualizerAudioEngine(this, eqPrefs)
        equalizerEngine = eqEngine

        player = createPlayer(initialApi, startWithBitPerfect)
        setupPlayerListener(player, eqEngine)
        activePlayer = player

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        // Register USB attach & detach receiver
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }

        // Register callback for when device becomes available
        UsbAudioPermissionHelper.onDeviceAvailable = { device ->
            Handler(mainLooper).post {
                if (userBitPerfectEnabled && !effectiveBitPerfectActive) {
                    Log.i(TAG, "USB_RECONNECT_ATTEMPT: DAC attached (${device.productName}); attempting Bit Perfect activation")
                    rebuildPlayer(userSelectedAudioApi, isBitPerfect = true, force = true)
                }
            }
        }

        val appPrefs = (application as? SRMusicApp)?.appPreferences
        var previousVolMode: DacVolumeMode? = null
        serviceScope.launch {
            appPrefs?.dacVolumeModeFlow?.collect { mode ->
                if (previousVolMode == null) {
                    previousVolMode = mode
                    return@collect
                }
                if (previousVolMode == mode) return@collect
                previousVolMode = mode

                if (effectiveBitPerfectActive) {
                    Log.i(TAG, "DAC volume mode changed to $mode while Bit Perfect active; rebuilding player to apply")
                    rebuildPlayer(userSelectedAudioApi, isBitPerfect = true, force = true)
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    @OptIn(UnstableApi::class)
    fun rebuildPlayerWithApi(api: AudioOutputApi) {
        rebuildPlayer(api, effectiveBitPerfectActive)
    }

    @OptIn(UnstableApi::class)
    fun rebuildPlayer(api: AudioOutputApi, isBitPerfect: Boolean, force: Boolean = false) {
        if (isRebuilding) {
            Log.d(TAG, "rebuildPlayer already in progress, queuing pending rebuild request (api=$api, bitPerfect=$isBitPerfect)")
            pendingRebuildRequested = true
            return
        }
        if (!force && api == effectiveAudioApi && isBitPerfect == effectiveBitPerfectActive && ::player.isInitialized) {
            Log.d(TAG, "SWITCH ignored: API unchanged ($api) and BitPerfect unchanged ($isBitPerfect)")
            return
        }

        isRebuilding = true
        try {
            Log.i(TAG, "SWITCH_START requestedApi=$api bitPerfect=$isBitPerfect force=$force (previous api=$effectiveAudioApi bitPerfect=$effectiveBitPerfectActive)")
            val startTimeMs = SystemClock.elapsedRealtime()

            val oldPlayer = player
            val session = mediaSession ?: return

            // 1. Snapshot all playlist items and playback state
            val count = oldPlayer.mediaItemCount
            val mediaItems = ArrayList<MediaItem>(count)
            for (i in 0 until count) {
                mediaItems.add(oldPlayer.getMediaItemAt(i))
            }
            val currentIndex = oldPlayer.currentMediaItemIndex
            val currentPosition = oldPlayer.currentPosition
            val playWhenReady = oldPlayer.playWhenReady
            val playbackState = oldPlayer.playbackState
            val repeatMode = oldPlayer.repeatMode
            val shuffleModeEnabled = oldPlayer.shuffleModeEnabled
            val playbackParameters = oldPlayer.playbackParameters

            // Detach and release old player before creating new player
            // This cleanly stops streaming, drains URBs, releases sinks, and closes USB device
            // without resource conflicts or closing the new player's device connection.
            oldPlayer.stop()
            oldPlayer.release()

            // 2. Build new ExoPlayer with the requested backend and sink
            val newPlayer = createPlayer(api, isBitPerfect)

            // 3. Attach new player to MediaSession
            session.setPlayer(newPlayer)

            // 4. Restore configuration & playlist
            newPlayer.repeatMode = repeatMode
            newPlayer.shuffleModeEnabled = shuffleModeEnabled
            newPlayer.playbackParameters = playbackParameters

            if (mediaItems.isNotEmpty()) {
                newPlayer.setMediaItems(mediaItems, currentIndex, currentPosition)
                if (playbackState != Player.STATE_IDLE) {
                    newPlayer.prepare()
                }
            }
            newPlayer.playWhenReady = playWhenReady

            // 5. Setup audio session listener
            equalizerEngine?.let { setupPlayerListener(newPlayer, it) }

            // 6. Update reference fields
            player = newPlayer
            activePlayer = newPlayer
            effectiveAudioApi = api
            if (!isBitPerfect) {
                effectiveBitPerfectActive = false
            }

            val elapsedMs = SystemClock.elapsedRealtime() - startTimeMs
            Log.i(
                TAG,
                "SWITCH_DONE newEffectiveBackend=$api requestedBitPerfect=$isBitPerfect items=${newPlayer.mediaItemCount} index=${newPlayer.currentMediaItemIndex} posMs=${newPlayer.currentPosition} playWhenReady=$playWhenReady elapsedMs=$elapsedMs"
            )
        } finally {
            isRebuilding = false
            if (pendingRebuildRequested) {
                pendingRebuildRequested = false
                val targetBitPerfect = userBitPerfectEnabled && UsbAudioDevice.getInstance(this).let {
                    val d = it.findUsbAudioDevice()
                    d != null && it.hasPermission(d)
                }
                Log.i(TAG, "Executing queued rebuild with latest user state: api=$userSelectedAudioApi bitPerfect=$targetBitPerfect")
                Handler(mainLooper).post {
                    rebuildPlayer(userSelectedAudioApi, isBitPerfect = targetBitPerfect, force = true)
                }
            }
        }
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        serviceScope.cancel()
        try {
            unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
        UsbAudioPermissionHelper.onDeviceAvailable = null

        equalizerEngine?.destroy()
        equalizerEngine = null
        activePlayer = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        UsbAudioDevice.getInstance(this).closeDevice()
        super.onDestroy()
    }
}
