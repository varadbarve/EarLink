package com.example.earlink.ui.main

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.earlink.data.DataRepository
import com.example.earlink.database.LogEntity
import com.example.earlink.database.MappingEntity
import com.example.earlink.database.ProfileEntity
import com.example.earlink.event.Event
import com.example.earlink.event.EventStore
import com.example.earlink.network.WebSocketClient
import com.example.earlink.service.EarLinkForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(private val repository: DataRepository) : ViewModel() {

    val allProfiles: StateFlow<List<ProfileEntity>> = repository.allProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile: StateFlow<ProfileEntity?> = repository.activeProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentLogs: StateFlow<List<LogEntity>> = repository.recentLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wsStatus: StateFlow<WebSocketClient.ConnectionStatus> = WebSocketClient.status

    val liveEvents: StateFlow<List<Event>> = EventStore.events

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _serverIp = MutableStateFlow("")
    val serverIp: StateFlow<String> = _serverIp.asStateFlow()

    private val _serverPort = MutableStateFlow(8765)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _enableAdvancedGestureCapture = MutableStateFlow(false)
    val enableAdvancedGestureCapture: StateFlow<Boolean> = _enableAdvancedGestureCapture.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeProfileMappings: StateFlow<List<MappingEntity>> = activeProfile
        .flatMapLatest { profile ->
            if (profile != null) {
                repository.getMappingsForProfileFlow(profile.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("earlink_prefs", Context.MODE_PRIVATE)
        _serverIp.value = prefs.getString("server_ip", "") ?: ""
        _serverPort.value = prefs.getInt("server_port", 8765)
        _geminiApiKey.value = prefs.getString("gemini_api_key", "") ?: ""
        _enableAdvancedGestureCapture.value = prefs.getBoolean("enable_advanced_gesture_capture", false)
        _isServiceRunning.value = EarLinkForegroundService.isRunning
        
        if (_serverIp.value.isNotBlank()) {
            WebSocketClient.startConnecting(_serverIp.value, _serverPort.value)
        }
    }

    fun updateServiceStatus(context: Context, run: Boolean) {
        val intent = Intent(context, EarLinkForegroundService::class.java)
        if (run) {
            if (BuildVersionHelper.isAtLeastO()) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            context.stopService(intent)
        }
        _isServiceRunning.value = run
    }

    fun updateAdvancedGestureCapture(context: Context, enabled: Boolean) {
        _enableAdvancedGestureCapture.value = enabled
        val prefs = context.getSharedPreferences("earlink_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("enable_advanced_gesture_capture", enabled).apply()
        if (_isServiceRunning.value) {
            updateServiceStatus(context, false)
            updateServiceStatus(context, true)
        }
    }

    fun updateWsConfig(context: Context, ip: String, port: Int) {
        _serverIp.value = ip
        _serverPort.value = port
        val prefs = context.getSharedPreferences("earlink_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("server_ip", ip).putInt("server_port", port).apply()
        
        WebSocketClient.startConnecting(ip, port)
    }

    fun updateGeminiApiKey(context: Context, key: String) {
        _geminiApiKey.value = key
        val prefs = context.getSharedPreferences("earlink_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("gemini_api_key", key).apply()
    }

    fun disconnectWs() {
        WebSocketClient.disconnect()
    }

    fun selectProfile(profileId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setActiveProfile(profileId)
        }
    }

    fun createProfile(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProfile(name)
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProfile(profile)
        }
    }

    fun saveMapping(eventType: String, actionType: String, actionData: String) {
        val profile = activeProfile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMapping(profile.id, eventType, actionType, actionData)
        }
    }

    fun deleteMapping(mappingId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMapping(mappingId)
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLogs()
        }
    }

    fun clearLiveEvents() {
        EventStore.clear()
    }
}

object BuildVersionHelper {
    fun isAtLeastO(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
    }
}
