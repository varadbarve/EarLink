package com.example.earlink.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.media.VolumeProviderCompat
import com.example.earlink.event.EarLinkEventBus
import com.example.earlink.event.InputEvent
import com.example.earlink.rules.GestureResolver
import com.example.earlink.rules.RuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class EarLinkForegroundService : Service() {

    companion object {
        private const val TAG = "EarLinkService"
        private const val CHANNEL_ID = "earlink_service_channel"
        private const val NOTIFICATION_ID = 1

        var isRunning = false
            private set
    }

    private var mediaSession: MediaSessionCompat? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusListener: AudioManager.OnAudioFocusChangeListener? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating EarLinkForegroundService")
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        setupMediaSession()
        requestAudioFocus()
        isRunning = true

        // Subscribe to event flows
        serviceScope.launch {
            EarLinkEventBus.rawInputs.collect { inputEvent ->
                GestureResolver.resolveAndPost(inputEvent, serviceScope)
            }
        }

        serviceScope.launch {
            EarLinkEventBus.resolvedGestures.collect { resolvedGesture ->
                RuleEngine.processGesture(this@EarLinkForegroundService, resolvedGesture)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Starting EarLinkForegroundService")
        
        val notification = buildNotification("Listening for earbud gestures...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Keep media session active and request focus again just in case
        mediaSession?.isActive = true
        requestAudioFocus()

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Destroying EarLinkForegroundService")
        isRunning = false
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        releaseAudioFocus()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "EarLinkMediaSession").apply {
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP
                    )
                    .build()
            )

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                    val keyEvent = mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                        val keyCode = keyEvent.keyCode
                        Log.d(TAG, "Media button intercepted: keyCode=$keyCode")
                        
                        val rawKey = when (keyCode) {
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> "MEDIA_PLAY_PAUSE"
                            KeyEvent.KEYCODE_MEDIA_NEXT -> "MEDIA_NEXT"
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "MEDIA_PREVIOUS"
                            else -> null
                        }

                        if (rawKey != null) {
                            serviceScope.launch {
                                EarLinkEventBus.postRawInput(InputEvent(keyCode, rawKey))
                            }
                            return true // consume event
                        }
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent)
                }
            })

            // Setup custom VolumeProviderCompat to intercept volume keys
            val volumeProvider = object : VolumeProviderCompat(
                VOLUME_CONTROL_RELATIVE,
                100, // Max Volume
                50   // Default starting volume
            ) {
                override fun onAdjustVolume(direction: Int) {
                    // direction: 1 = Vol Up, -1 = Vol Down
                    Log.d(TAG, "Volume adjust intercepted: direction=$direction")
                    val rawKey = if (direction > 0) "VOLUME_UP" else "VOLUME_DOWN"
                    val keycode = if (direction > 0) KeyEvent.KEYCODE_VOLUME_UP else KeyEvent.KEYCODE_VOLUME_DOWN
                    
                    serviceScope.launch {
                        EarLinkEventBus.postRawInput(InputEvent(keycode, rawKey))
                    }
                }
            }
            
            // Read advanced capture configuration
            val prefs = getSharedPreferences("earlink_prefs", Context.MODE_PRIVATE)
            val enableAdvanced = prefs.getBoolean("enable_advanced_gesture_capture", false)
            if (enableAdvanced) {
                Log.d(TAG, "Advanced gesture capture enabled: Intercepting volume buttons")
                setPlaybackToRemote(volumeProvider)
            } else {
                Log.d(TAG, "Advanced gesture capture disabled: Volume buttons routing normally")
                setPlaybackToLocal(AudioManager.STREAM_MUSIC)
            }
            isActive = true
        }
    }

    private fun requestAudioFocus() {
        audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            Log.d(TAG, "Audio focus change: $focusChange")
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                // If we lose focus, try to request it back after a delay or remain active to intercept keys
                Log.d(TAG, "Lost audio focus, media keys might be rerouted by system.")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(audioFocusListener!!)
                .build()

            audioManager?.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
        } else if (audioFocusListener != null) {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(audioFocusListener)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EarLink Background Interceptor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps EarLink listening for hardware earbud buttons in the background"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EarLink Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
