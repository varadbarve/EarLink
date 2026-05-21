package com.example.earlink.rules

import com.example.earlink.event.EarLinkEventBus
import com.example.earlink.event.InputEvent
import com.example.earlink.event.ResolvedGesture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object GestureResolver {
    fun resolveAndPost(event: InputEvent, scope: CoroutineScope) {
        scope.launch {
            val gestureType = when (event.rawKey) {
                "MEDIA_PLAY_PAUSE" -> "SINGLE_TAP"
                "VOLUME_DOWN" -> "LEFT_DOUBLE_TAP"
                "VOLUME_UP" -> "RIGHT_DOUBLE_TAP"
                "MEDIA_NEXT" -> "LEFT_LONG_PRESS"
                "MEDIA_PREVIOUS" -> "RIGHT_LONG_PRESS"
                else -> "UNKNOWN"
            }
            if (gestureType != "UNKNOWN") {
                EarLinkEventBus.postResolvedGesture(ResolvedGesture(gestureType, event.rawKey))
            }
        }
    }
}
