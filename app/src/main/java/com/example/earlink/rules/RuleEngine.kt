package com.example.earlink.rules

import android.content.Context
import android.util.Log
import com.example.earlink.actions.ActionManager
import com.example.earlink.database.AppDatabase
import com.example.earlink.database.LogEntity
import com.example.earlink.event.Event
import com.example.earlink.event.EventStore
import com.example.earlink.event.ResolvedGesture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object RuleEngine {
    private const val TAG = "EarLinkRuleEngine"
    private val scope = CoroutineScope(Dispatchers.IO)

    fun processGesture(context: Context, resolvedGesture: ResolvedGesture) {
        val gestureType = resolvedGesture.gestureType
        val rawKey = resolvedGesture.rawKey
        
        Log.d(TAG, "Processing gesture: gestureType=$gestureType, rawKey=$rawKey")

        scope.launch {
            val db = AppDatabase.getDatabase(context)
            
            // 1. Log the event in database
            val logMsg = "$gestureType ($rawKey)"
            db.logDao().insertLog(LogEntity(event = logMsg))

            // 2. Log event in real-time memory store
            val event = Event(eventType = gestureType, rawKey = rawKey)
            EventStore.addEvent(event)

            // 3. Find active profile
            val activeProfile = db.profileDao().getActiveProfile()
            if (activeProfile != null) {
                // Find mapping for eventType
                val mappings = db.mappingDao().getMappingsForProfile(activeProfile.id)
                val mapping = mappings.find { it.eventType == gestureType }
                if (mapping != null) {
                    ActionManager.executeAction(context, mapping.actionType, mapping.actionData, rawKey)
                } else {
                    // Fallback to default
                    ActionManager.executeAction(context, "DEFAULT", "", rawKey)
                }
            } else {
                // No profile active -> perform default action (proxy)
                ActionManager.executeAction(context, "DEFAULT", "", rawKey)
            }
        }
    }
}
