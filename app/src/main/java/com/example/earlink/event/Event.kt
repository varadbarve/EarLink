package com.example.earlink.event

data class Event(
    val id: Long = 0,
    val eventType: String, // e.g. "SINGLE_TAP", "LEFT_DOUBLE_TAP", "RIGHT_DOUBLE_TAP", "LEFT_LONG_PRESS", "RIGHT_LONG_PRESS"
    val rawKey: String,    // e.g. "MEDIA_PLAY_PAUSE", "VOLUME_DOWN", "VOLUME_UP", "MEDIA_NEXT", "MEDIA_PREVIOUS"
    val timestamp: Long = System.currentTimeMillis()
)
