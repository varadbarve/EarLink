package com.example.earlink.actions

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AssistantCoordinator {
    private const val TAG = "EarLinkAssistantCoord"
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startVoiceAssistantFlow(context: Context) {
        Log.d(TAG, "Starting voice assistant recording flow")
        scope.launch {
            try {
                val recorder = VoiceRecorder(context)
                ActionManager.speak(context, "Listening")
                delay(1200) // Wait for TTS speaking to finish
                
                val audioFile = recorder.startRecording()
                if (audioFile == null) {
                    ActionManager.speak(context, "Failed to start recording")
                    return@launch
                }
                
                delay(4000) // Record for 4 seconds
                
                val stoppedFile = recorder.stopRecording()
                if (stoppedFile == null || !stoppedFile.exists()) {
                    ActionManager.speak(context, "No audio recorded")
                    return@launch
                }
                
                ActionManager.speak(context, "Processing")
                
                val prefs = context.getSharedPreferences("earlink_prefs", Context.MODE_PRIVATE)
                val apiKey = prefs.getString("gemini_api_key", "") ?: ""
                
                val response = GeminiClient.queryGemini(context, stoppedFile, apiKey)
                ActionManager.speak(context, response)
            } catch (e: Exception) {
                Log.e(TAG, "Error in coordinated voice assistant flow", e)
                ActionManager.speak(context, "Voice assistant error")
            }
        }
    }
}
