package com.example.earlink.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EarLinkEventBus {
    private val _rawInputs = MutableSharedFlow<InputEvent>(extraBufferCapacity = 64)
    val rawInputs = _rawInputs.asSharedFlow()

    private val _resolvedGestures = MutableSharedFlow<ResolvedGesture>(extraBufferCapacity = 64)
    val resolvedGestures = _resolvedGestures.asSharedFlow()

    suspend fun postRawInput(event: InputEvent) {
        _rawInputs.emit(event)
    }

    suspend fun postResolvedGesture(gesture: ResolvedGesture) {
        _resolvedGestures.emit(gesture)
    }
}
