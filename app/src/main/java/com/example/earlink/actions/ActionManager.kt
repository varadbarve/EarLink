package com.example.earlink.actions

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.earlink.network.WebSocketClient
import com.example.earlink.service.MediaSessionProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

object ActionManager {
    private const val TAG = "EarLinkActionManager"
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    fun initTts(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.getDefault())
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Language is not supported or missing data")
                    } else {
                        isTtsInitialized = true
                    }
                } else {
                    Log.e(TAG, "TTS Initialization failed")
                }
            }
        }
    }

    fun speak(context: Context, text: String) {
        initTts(context)
        if (isTtsInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "EarLinkTTS")
        } else {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    fun executeAction(context: Context, actionType: String, actionData: String, rawKey: String) {
        Log.d(TAG, "Executing action: type=$actionType, data=$actionData, rawKey=$rawKey")
        
        when (actionType.uppercase()) {
            "DEFAULT" -> {
                handleDefaultAction(context, rawKey)
            }
            "OPEN_APP" -> {
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage(actionData)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } else {
                        Log.e(TAG, "Cannot get launch intent for package: $actionData")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch app $actionData", e)
                }
            }
            "TOGGLE_DND" -> {
                try {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (notificationManager.isNotificationPolicyAccessGranted) {
                        val currentFilter = notificationManager.currentInterruptionFilter
                        val newFilter = if (currentFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                            NotificationManager.INTERRUPTION_FILTER_PRIORITY
                        } else {
                            NotificationManager.INTERRUPTION_FILTER_ALL
                        }
                        notificationManager.setInterruptionFilter(newFilter)
                        val statusText = if (newFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) "DND Enabled" else "DND Disabled"
                        speak(context, statusText)
                    } else {
                        Log.w(TAG, "DND Policy Access not granted.")
                        Toast.makeText(context, "Grant DND Policy access first", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to toggle DND", e)
                }
            }
            "START_RECORDING" -> {
                Log.d(TAG, "Recording requested")
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    speak(context, "Microphone permission not granted")
                } else {
                    AssistantCoordinator.startVoiceAssistantFlow(context)
                }
            }
            "LAUNCH_ASSISTANT" -> {
                try {
                    val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch assistant", e)
                }
            }
            "SEND_NOTIFICATION" -> {
                speak(context, actionData.ifBlank { "Notification from EarLink" })
            }
            "DESKTOP_COMMAND" -> {
                WebSocketClient.sendCommand(actionData)
            }
            else -> {
                Log.e(TAG, "Unknown action type: $actionType")
            }
        }
    }

    private fun handleDefaultAction(context: Context, rawKey: String) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (rawKey) {
            "VOLUME_UP" -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            }
            "VOLUME_DOWN" -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            }
            "MEDIA_PLAY_PAUSE" -> {
                MediaSessionProxy.forwardMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }
            "MEDIA_NEXT" -> {
                MediaSessionProxy.forwardMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
            }
            "MEDIA_PREVIOUS" -> {
                MediaSessionProxy.forwardMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }
            else -> {
                Log.w(TAG, "No default implementation for key: $rawKey")
            }
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isTtsInitialized = false
    }
}
