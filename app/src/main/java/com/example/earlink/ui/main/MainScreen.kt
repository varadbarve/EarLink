package com.example.earlink.ui.main

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.earlink.data.DefaultDataRepository
import com.example.earlink.database.LogEntity
import com.example.earlink.database.MappingEntity
import com.example.earlink.database.ProfileEntity
import com.example.earlink.event.Event
import com.example.earlink.network.WebSocketClient
import com.example.earlink.service.EarLinkNotificationListener

// Premium Palette Design Tokens
val DarkBg = Color(0xFF0C0C0E)
val CardBg = Color(0xFF141419)
val CardBorder = Color(0xFF22222B)
val TextPrimary = Color(0xFFF3F3F5)
val TextSecondary = Color(0xFFA5A5B2)

val AccentPurple = Color(0xFFBB86FC)
val AccentPurpleDark = Color(0xFF6200EE)
val AccentCyan = Color(0xFF03DAC6)
val StatusGreen = Color(0xFF00E676)
val StatusRed = Color(0xFFFF5252)

enum class AppTab(val title: String) {
    HOME("Home"),
    MAPPINGS("Mappings"),
    PROFILES("Profiles"),
    DEVICES("Devices"),
    LOGS("Logs")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel = viewModel {
        MainScreenViewModel(DefaultDataRepository(context.applicationContext))
    }

    LaunchedEffect(Unit) {
        viewModel.loadSettings(context.applicationContext)
    }

    // ViewModel State bindings
    val allProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val wsStatus by viewModel.wsStatus.collectAsStateWithLifecycle()
    val liveEvents by viewModel.liveEvents.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val enableAdvancedGestureCapture by viewModel.enableAdvancedGestureCapture.collectAsStateWithLifecycle()
    val serverIp by viewModel.serverIp.collectAsStateWithLifecycle()
    val serverPort by viewModel.serverPort.collectAsStateWithLifecycle()
    val activeProfileMappings by viewModel.activeProfileMappings.collectAsStateWithLifecycle()
    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(AppTab.HOME) }

    // Recheck notification listener permission dynamically
    var isNotificationPermissionGranted by remember {
        mutableStateOf(EarLinkNotificationListener.isConnected)
    }

    LaunchedEffect(liveEvents) {
        // Simple trick to sync permission state on event trigger/navigation
        isNotificationPermissionGranted = EarLinkNotificationListener.isConnected
    }

    Scaffold(
        modifier = modifier.fillMaxSize().background(DarkBg),
        bottomBar = {
            NavigationBar(
                containerColor = CardBg,
                tonalElevation = 8.dp,
                modifier = Modifier.border(1.dp, CardBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val icon = when (tab) {
                        AppTab.HOME -> Icons.Default.Home
                        AppTab.MAPPINGS -> Icons.Default.List
                        AppTab.PROFILES -> Icons.Default.Build
                        AppTab.DEVICES -> Icons.Default.Info
                        AppTab.LOGS -> Icons.Default.Refresh
                    }
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = { Icon(icon, contentDescription = tab.title, tint = if (isSelected) AccentCyan else TextSecondary) },
                        label = { Text(tab.title, color = if (isSelected) TextPrimary else TextSecondary, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = AccentPurple.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                AppTab.HOME -> HomeTab(
                    context = context,
                    viewModel = viewModel,
                    isServiceRunning = isServiceRunning,
                    enableAdvancedGestureCapture = enableAdvancedGestureCapture,
                    wsStatus = wsStatus,
                    serverIp = serverIp,
                    serverPort = serverPort,
                    geminiApiKey = geminiApiKey,
                    activeProfile = activeProfile,
                    liveEvents = liveEvents,
                    isNotificationPermissionGranted = isNotificationPermissionGranted,
                    onRefreshPermission = {
                        isNotificationPermissionGranted = EarLinkNotificationListener.isConnected
                    }
                )
                AppTab.MAPPINGS -> MappingsTab(
                    context = context,
                    viewModel = viewModel,
                    activeProfile = activeProfile,
                    activeProfileMappings = activeProfileMappings
                )
                AppTab.PROFILES -> ProfilesTab(
                    viewModel = viewModel,
                    allProfiles = allProfiles,
                    activeProfile = activeProfile
                )
                AppTab.DEVICES -> DevicesTab()
                AppTab.LOGS -> LogsTab(
                    viewModel = viewModel,
                    recentLogs = recentLogs
                )
            }
        }
    }
}

// ----------------------------------------------------
// TABS IMPLEMENTATION
// ----------------------------------------------------

@Composable
fun HomeTab(
    context: Context,
    viewModel: MainScreenViewModel,
    isServiceRunning: Boolean,
    enableAdvancedGestureCapture: Boolean,
    wsStatus: WebSocketClient.ConnectionStatus,
    serverIp: String,
    serverPort: Int,
    geminiApiKey: String,
    activeProfile: ProfileEntity?,
    liveEvents: List<Event>,
    isNotificationPermissionGranted: Boolean,
    onRefreshPermission: () -> Unit
) {
    val scrollState = rememberScrollState()
    var ipInput by remember { mutableStateOf(serverIp) }
    var portInput by remember { mutableStateOf(serverPort.toString()) }
    var apiKeyInput by remember { mutableStateOf(geminiApiKey) }

    LaunchedEffect(serverIp, serverPort, geminiApiKey) {
        ipInput = serverIp
        portInput = serverPort.toString()
        apiKeyInput = geminiApiKey
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Title Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        Brush.linearGradient(listOf(AccentPurple, AccentCyan)),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Logo", tint = Color.Black)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "EarLink",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Smart TWS controller bridge",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Active Profile Alert
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(if (activeProfile != null) AccentCyan else TextSecondary, RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (activeProfile != null) "Active Profile: ${activeProfile.name}" else "No Profile Active",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        // Foreground Service Status Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Gesture Interception Service", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = if (isServiceRunning) "Running (Listening for gestures)" else "Service Stopped",
                            color = if (isServiceRunning) StatusGreen else TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { isChecked ->
                            viewModel.updateServiceStatus(context, isChecked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentCyan,
                            checkedTrackColor = AccentCyan.copy(alpha = 0.5f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = CardBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = CardBorder)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Advanced Gesture Capture", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = "Hijacks volume keys to capture physical double-taps on earbuds. (May prevent normal volume behavior)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = enableAdvancedGestureCapture,
                        onCheckedChange = { isChecked ->
                            viewModel.updateAdvancedGestureCapture(context, isChecked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentCyan,
                            checkedTrackColor = AccentCyan.copy(alpha = 0.5f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = CardBorder
                        )
                    )
                }

                // If Notification access permission is not granted, show warning button
                if (!isNotificationPermissionGranted) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, StatusRed, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Warning", tint = StatusRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant Notification Listener Access", color = StatusRed, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Desktop Companion Connection Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Desktop Connection", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    // Connection Status indicator pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (wsStatus) {
                                    WebSocketClient.ConnectionStatus.CONNECTED -> StatusGreen.copy(alpha = 0.2f)
                                    WebSocketClient.ConnectionStatus.CONNECTING -> AccentPurple.copy(alpha = 0.2f)
                                    WebSocketClient.ConnectionStatus.DISCONNECTED -> StatusRed.copy(alpha = 0.2f)
                                }
                            )
                            .border(
                                1.dp,
                                when (wsStatus) {
                                    WebSocketClient.ConnectionStatus.CONNECTED -> StatusGreen
                                    WebSocketClient.ConnectionStatus.CONNECTING -> AccentPurple
                                    WebSocketClient.ConnectionStatus.DISCONNECTED -> StatusRed
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = wsStatus.name,
                            color = when (wsStatus) {
                                WebSocketClient.ConnectionStatus.CONNECTED -> StatusGreen
                                WebSocketClient.ConnectionStatus.CONNECTING -> AccentPurple
                                WebSocketClient.ConnectionStatus.DISCONNECTED -> StatusRed
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ipInput,
                    onValueChange = { ipInput = it },
                    label = { Text("Windows Server IP") },
                    placeholder = { Text("e.g. 192.168.1.100") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = CardBorder,
                        focusedLabelColor = AccentPurple,
                        unfocusedLabelColor = TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = portInput,
                    onValueChange = { portInput = it },
                    label = { Text("Port") },
                    placeholder = { Text("8765") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = CardBorder,
                        focusedLabelColor = AccentPurple,
                        unfocusedLabelColor = TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val port = portInput.toIntOrNull() ?: 8765
                            viewModel.updateWsConfig(context, ipInput, port)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Connect / Sync", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    if (wsStatus != WebSocketClient.ConnectionStatus.DISCONNECTED) {
                        Button(
                            onClick = { viewModel.disconnectWs() },
                            colors = ButtonDefaults.buttonColors(containerColor = CardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Disconnect", color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Gemini API Key Settings Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AI Voice Assistant", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "Configure your Gemini API key to enable real-time speech responses when triggering 'START_RECORDING' on your earbuds.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AI API Key (Optional, uses Mock if empty)") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = CardBorder,
                        focusedLabelColor = AccentPurple,
                        unfocusedLabelColor = TextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.updateGeminiApiKey(context, apiKeyInput.trim())
                        Toast.makeText(context, "API Key saved successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save API Key", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live Event Feed Section
        Text(
            text = "Live Gesture Feed",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            if (liveEvents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No gestures detected yet.\nTap on TWS earbud buttons to test.",
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Live feed", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Clear Feed",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { viewModel.clearLiveEvents() }
                        )
                    }
                    Divider(color = CardBorder)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(liveEvents) { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardBorder.copy(alpha = 0.4f))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(event.eventType, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(event.rawKey, color = TextSecondary, fontSize = 11.sp)
                                }
                                val formattedTime = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(event.timestamp)
                                Text(formattedTime, color = AccentCyan, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappingsTab(
    context: Context,
    viewModel: MainScreenViewModel,
    activeProfile: ProfileEntity?,
    activeProfileMappings: List<MappingEntity>
) {
    if (activeProfile == null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Please create and select a Profile first to configure mappings.",
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    var selectedGesture by remember { mutableStateOf("SINGLE_TAP") }
    var selectedAction by remember { mutableStateOf("DEFAULT") }
    var actionDataInput by remember { mutableStateOf("") }

    val gestureOptions = listOf(
        "SINGLE_TAP",
        "LEFT_DOUBLE_TAP",
        "RIGHT_DOUBLE_TAP",
        "LEFT_LONG_PRESS",
        "RIGHT_LONG_PRESS"
    )

    val actionOptions = listOf(
        "DEFAULT",
        "OPEN_APP",
        "TOGGLE_DND",
        "START_RECORDING",
        "LAUNCH_ASSISTANT",
        "SEND_NOTIFICATION",
        "DESKTOP_COMMAND"
    )

    var gestureExpanded by remember { mutableStateOf(false) }
    var actionExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configure Profile: ${activeProfile.name}",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        // Add Mapping form
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add Gesture Mapping", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                // Gesture Dropdown
                ExposedDropdownMenuBox(
                    expanded = gestureExpanded,
                    onExpandedChange = { gestureExpanded = !gestureExpanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedGesture,
                        onValueChange = {},
                        label = { Text("Select TWS Gesture") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gestureExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = CardBorder
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = gestureExpanded,
                        onDismissRequest = { gestureExpanded = false },
                        modifier = Modifier.background(CardBg)
                    ) {
                        gestureOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = TextPrimary) },
                                onClick = {
                                    selectedGesture = option
                                    gestureExpanded = false
                                }
                            )
                        }
                    }
                }

                // Action Dropdown
                ExposedDropdownMenuBox(
                    expanded = actionExpanded,
                    onExpandedChange = { actionExpanded = !actionExpanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedAction,
                        onValueChange = {},
                        label = { Text("Select Action") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = CardBorder
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = actionExpanded,
                        onDismissRequest = { actionExpanded = false },
                        modifier = Modifier.background(CardBg)
                    ) {
                        actionOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = TextPrimary) },
                                onClick = {
                                    selectedAction = option
                                    actionExpanded = false
                                }
                            )
                        }
                    }
                }

                // Action Data text field (if needed)
                if (selectedAction in listOf("OPEN_APP", "SEND_NOTIFICATION", "DESKTOP_COMMAND")) {
                    val labelText = when (selectedAction) {
                        "OPEN_APP" -> "App Package Name (e.g. com.spotify.music)"
                        "SEND_NOTIFICATION" -> "Speech Notification Text"
                        "DESKTOP_COMMAND" -> "Desktop Command Identifier (e.g. mute_discord)"
                        else -> "Action Data"
                    }
                    OutlinedTextField(
                        value = actionDataInput,
                        onValueChange = { actionDataInput = it },
                        label = { Text(labelText) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = CardBorder
                        )
                    )
                }

                Button(
                    onClick = {
                        viewModel.saveMapping(selectedGesture, selectedAction, actionDataInput)
                        actionDataInput = ""
                        Toast.makeText(context, "Mapping saved successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Gesture Mapping", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active mappings list
        Text("Active Mappings", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            if (activeProfileMappings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No mappings configured for this profile.\nAdd a mapping above.", color = TextSecondary, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeProfileMappings) { mapping ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardBorder.copy(alpha = 0.4f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mapping.eventType, color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Row {
                                    Text("Action: ", color = TextSecondary, fontSize = 11.sp)
                                    Text(mapping.actionType, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (mapping.actionData.isNotBlank()) {
                                    Text("Data: ${mapping.actionData}", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteMapping(mapping.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfilesTab(
    viewModel: MainScreenViewModel,
    allProfiles: List<ProfileEntity>,
    activeProfile: ProfileEntity?
) {
    var newProfileName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Manage Custom Profiles", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)

        // Create Profile Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Create New Profile", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    label = { Text("Profile Name (e.g. Gaming, Study)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = CardBorder
                    )
                )

                Button(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            viewModel.createProfile(newProfileName.trim())
                            newProfileName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Profile", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Profiles list
        Text("Profiles List", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            if (allProfiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No profiles available.\nCreate one above.", color = TextSecondary, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allProfiles) { profile ->
                        val isActive = activeProfile?.id == profile.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) AccentPurple.copy(alpha = 0.1f) else CardBorder.copy(alpha = 0.4f))
                                .border(1.dp, if (isActive) AccentPurple else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectProfile(profile.id) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                RadioButton(
                                    selected = isActive,
                                    onClick = { viewModel.selectProfile(profile.id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = AccentPurple)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(profile.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            IconButton(onClick = { viewModel.deleteProfile(profile) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DevicesTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("TWS Audio Devices", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Connected TWS", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("TecSox Pro Earbuds", color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(StatusGreen.copy(alpha = 0.2f))
                            .border(1.dp, StatusGreen, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("ACTIVE", color = StatusGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Divider(color = CardBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Battery Level", color = TextSecondary, fontSize = 12.sp)
                        Text("Left: 90% | Right: 90%", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Protocol", color = TextSecondary, fontSize = 12.sp)
                        Text("AAC / HD Audio", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Address", color = TextSecondary, fontSize = 12.sp)
                        Text("7C:D3:0A:11:F2:BC", color = TextPrimary, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Signal Strength", color = TextSecondary, fontSize = 12.sp)
                        Text("Excellent (-45 dBm)", color = StatusGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How gesture routing works", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "EarLink hooks into the standard Android media framework. When you tap your earbuds, the device sends standard volume or playback control events to Android. EarLink intercepts these events, executes any custom remapping you created, or proxies them to spotify/youtube if no mapping matches.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun LogsTab(
    viewModel: MainScreenViewModel,
    recentLogs: List<LogEntity>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("System Database Logs", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Button(
                onClick = { viewModel.clearLogs() },
                colors = ButtonDefaults.buttonColors(containerColor = CardBorder),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = StatusRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear Db", color = TextPrimary, fontSize = 12.sp)
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            if (recentLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No logs recorded yet.", color = TextSecondary, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(recentLogs) { log ->
                        val formattedTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault()).format(log.timestamp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardBorder.copy(alpha = 0.2f))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(log.event, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(formattedTime, color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
