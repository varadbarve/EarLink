package com.example.earlink.event

data class InputEvent(
    val keyCode: Int,
    val rawKey: String,
    val deviceName: String = "TWS Earbud",
    val timestamp: Long = System.currentTimeMillis()
)
