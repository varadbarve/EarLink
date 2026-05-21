package com.example.earlink.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

object WebSocketClient {
    private const val TAG = "EarLinkWebSocketClient"

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var connectionJob: Job? = null
    
    private var serverIp = ""
    private var serverPort = 8765

    fun startConnecting(ip: String, port: Int) {
        serverIp = ip
        serverPort = port
        if (connectionJob == null || connectionJob?.isCompleted == true) {
            connectionJob = CoroutineScope(Dispatchers.IO).launch {
                while (true) {
                    if (_status.value == ConnectionStatus.DISCONNECTED && serverIp.isNotBlank()) {
                        connect()
                    }
                    delay(5000) // Retry every 5 seconds if disconnected
                }
            }
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _status.value = ConnectionStatus.DISCONNECTED
    }

    private fun connect() {
        if (serverIp.isBlank()) return
        
        _status.value = ConnectionStatus.CONNECTING
        Log.d(TAG, "Connecting to ws://$serverIp:$serverPort")

        client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(0, TimeUnit.MILLISECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("ws://$serverIp:$serverPort")
            .build()

        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connection opened")
                _status.value = ConnectionStatus.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message: $text")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Closing connection: $code - $reason")
                _status.value = ConnectionStatus.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Connection failure: ${t.message}")
                _status.value = ConnectionStatus.DISCONNECTED
            }
        })
    }

    fun sendCommand(command: String) {
        val ws = webSocket
        if (ws != null && _status.value == ConnectionStatus.CONNECTED) {
            val jsonPayload = "{\"command\": \"$command\"}"
            ws.send(jsonPayload)
            Log.d(TAG, "Sent payload: $jsonPayload")
        } else {
            Log.w(TAG, "WebSocket not connected. Dropping command: $command")
        }
    }
}
