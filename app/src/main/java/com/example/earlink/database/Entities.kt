package com.example.earlink.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isActive: Boolean = false
)

@Entity(
    tableName = "mappings",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId"])]
)
data class MappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val eventType: String,  // e.g. "SINGLE_TAP", "LEFT_DOUBLE_TAP", "RIGHT_DOUBLE_TAP", "LEFT_LONG_PRESS", "RIGHT_LONG_PRESS"
    val actionType: String, // e.g. "DEFAULT", "OPEN_APP", "TOGGLE_DND", "START_RECORDING", "LAUNCH_ASSISTANT", "SEND_NOTIFICATION", "DESKTOP_COMMAND"
    val actionData: String  // e.g. Package name, or desktop command like "mute_discord"
)

@Entity(tableName = "event_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val event: String,
    val timestamp: Long = System.currentTimeMillis()
)
