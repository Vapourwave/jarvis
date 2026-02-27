package com.jarvis.app.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class JarvisNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Process actionable notifications after user approval.
    }
}
