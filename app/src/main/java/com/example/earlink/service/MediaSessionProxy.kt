package com.example.earlink.service

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent

object MediaSessionProxy {
    private const val TAG = "EarLinkMediaSessionProxy"

    fun forwardMediaKey(context: Context, keyCode: Int) {
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(context, EarLinkNotificationListener::class.java)

        try {
            val controllers = manager.getActiveSessions(componentName)
            // Filter out our own controller to avoid infinite loops
            val targetController = controllers.firstOrNull { it.packageName != context.packageName }

            if (targetController != null) {
                Log.d(TAG, "Forwarding event to controller: ${targetController.packageName}")
                val transport = targetController.transportControls
                when (keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        val state = targetController.playbackState?.state
                        if (state == android.media.session.PlaybackState.STATE_PLAYING) {
                            transport.pause()
                        } else {
                            transport.play()
                        }
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> transport.skipToNext()
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> transport.skipToPrevious()
                    KeyEvent.KEYCODE_MEDIA_STOP -> transport.stop()
                    else -> {
                        // Fallback: send key events directly to target controller
                        val eventDown = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keyCode, 0)
                        val eventUp = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0)
                        targetController.dispatchMediaButtonEvent(eventDown)
                        targetController.dispatchMediaButtonEvent(eventUp)
                    }
                }
            } else {
                Log.w(TAG, "No other active media sessions found to proxy key event to.")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Notification listener permission not granted or bound.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error proxying media event", e)
        }
    }
}
