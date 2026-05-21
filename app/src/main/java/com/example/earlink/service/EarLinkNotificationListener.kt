package com.example.earlink.service

import android.service.notification.NotificationListenerService
import android.util.Log

class EarLinkNotificationListener : NotificationListenerService() {
    companion object {
        private const val TAG = "EarLinkNotificationListener"
        var isConnected = false
            private set

        var instance: EarLinkNotificationListener? = null
            private set
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
        isConnected = true
        instance = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
        isConnected = false
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isConnected = false
    }
}
