package com.example.earlink.event

data class ResolvedGesture(
    val gestureType: String, // e.g. "SINGLE_TAP", "LEFT_LONG_PRESS", etc.
    val rawKey: String       // e.g. "MEDIA_PLAY_PAUSE", "VOLUME_UP", etc.
)
