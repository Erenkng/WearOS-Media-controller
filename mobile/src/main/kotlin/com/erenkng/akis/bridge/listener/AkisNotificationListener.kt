package com.erenkng.akis.bridge.listener

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.erenkng.akis.bridge.bridgeGraph

/**
 * Yalnızca `MediaSessionManager.getActiveSessions` için var olan bildirim dinleyicisi.
 *
 * Gizlilik sözü (BRIEF Bölüm 11): bildirim içerikleri okunmaz ve saklanmaz.
 * `onNotificationPosted` / `onNotificationRemoved` bilerek boş bırakılmıştır; değiştirmeyin.
 */
class AkisNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        bridgeGraph.onListenerConnected()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        bridgeGraph.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Bilerek boş.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Bilerek boş.
    }
}
