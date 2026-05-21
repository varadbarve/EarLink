package com.example.earlink.actions

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.earlink.database.AppDatabase
import com.example.earlink.network.WebSocketClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "EarLinkGeminiClient"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun queryGemini(context: Context, audioFile: File, apiKey: String): String {
        if (apiKey.isBlank()) {
            Log.i(TAG, "Gemini API key is blank, running in Mock Assistant mode.")
            return generateMockResponse(context)
        }

        try {
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            
            // Build Gemini request body
            val requestJson = JSONObject().apply {
                val contents = org.json.JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = org.json.JSONArray().apply {
                            val textPart = JSONObject().apply {
                                put("text", "The user is interacting with their Bluetooth TWS earbuds voice assistant. Respond to their question or command briefly in 1-2 conversational sentences.")
                            }
                            val audioPart = JSONObject().apply {
                                val inlineData = JSONObject().apply {
                                    put("mimeType", "audio/mp4")
                                    put("data", base64Audio)
                                }
                                put("inlineData", inlineData)
                            }
                            put(textPart)
                            put(audioPart)
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Gemini API failed code=${response.code}: $errBody")
                    return "Error calling Gemini. Code ${response.code}."
                }

                val responseBody = response.body?.string() ?: return "No response received from Gemini."
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text", "Sorry, I couldn't understand that.")
                    }
                }
                return "No text response extracted from Gemini API."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini query", e)
            return "Failed to contact Gemini. Details: ${e.localizedMessage}"
        }
    }

    private suspend fun generateMockResponse(context: Context): String {
        // Query database for app state context
        val db = AppDatabase.getDatabase(context)
        val activeProfile = db.profileDao().getActiveProfile()
        val profileName = activeProfile?.name ?: "None"
        val mappingsCount = activeProfile?.let { db.mappingDao().getMappingsForProfile(it.id).size } ?: 0
        
        val wsConnected = WebSocketClient.status.value == WebSocketClient.ConnectionStatus.CONNECTED
        val wsStatusText = if (wsConnected) "connected to your Windows desktop" else "disconnected from your desktop"
        
        val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        
        // Return a smart response based on some conditions or a random choice
        val responses = listOf(
            "Hello! I am your EarLink voice assistant. The current time is $currentTime, and your desktop bridge is currently $wsStatusText.",
            "EarLink voice assistant is online. You are using the profile named $profileName, which has $mappingsCount active gesture mappings.",
            "I heard you! Your TecSox Pro earbuds are connected with excellent signal strength, and battery levels are ninety percent on both sides.",
            "Hello there. To get real AI voice answers, please go to the EarLink home settings page and enter your Gemini API key.",
            "Got it! If you had mapped this button to desktop controls, I would have triggered the shortcut. Everything is working correctly."
        )
        
        // Pick one randomly or sequence based on time to be interesting
        val index = (System.currentTimeMillis() % responses.size).toInt()
        return responses[index]
    }
}
