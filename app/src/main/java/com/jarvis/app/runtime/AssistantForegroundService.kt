package com.jarvis.app.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class AssistantForegroundService : Service() {

    private lateinit var wakeOverlayController: WakeOverlayController

    override fun onCreate() {
        super.onCreate()
        wakeOverlayController = WakeOverlayController(this)
        ensureNotificationChannel()
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis running")
            .setContentText("Assistant runtime is active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_WAKE_HEARD -> wakeOverlayController.onWakeCommandHeard()
            ACTION_LISTENING -> wakeOverlayController.onListening()
            ACTION_THINKING -> wakeOverlayController.onThinking()
            ACTION_TEXT_OUTPUT -> wakeOverlayController.onTextResponse(
                intent.getStringExtra(EXTRA_TEXT).orEmpty()
            )
            ACTION_CONTROL_OUTPUT -> wakeOverlayController.onControlFlowReady()
            ACTION_TAP_FEEDBACK -> wakeOverlayController.showTapFeedback(
                intent.getFloatExtra(EXTRA_X, 0f),
                intent.getFloatExtra(EXTRA_Y, 0f)
            )
            ACTION_SWIPE_FEEDBACK -> wakeOverlayController.showSwipeFeedback(
                intent.getFloatExtra(EXTRA_X, 0f),
                intent.getFloatExtra(EXTRA_Y, 0f)
            )
            ACTION_DISMISS -> wakeOverlayController.dismiss()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        wakeOverlayController.dismiss()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Assistant runtime",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "assistant-runtime"
        private const val NOTIFICATION_ID = 101

        const val ACTION_WAKE_HEARD = "com.jarvis.app.runtime.action.WAKE_HEARD"
        const val ACTION_LISTENING = "com.jarvis.app.runtime.action.LISTENING"
        const val ACTION_THINKING = "com.jarvis.app.runtime.action.THINKING"
        const val ACTION_TEXT_OUTPUT = "com.jarvis.app.runtime.action.TEXT_OUTPUT"
        const val ACTION_CONTROL_OUTPUT = "com.jarvis.app.runtime.action.CONTROL_OUTPUT"
        const val ACTION_TAP_FEEDBACK = "com.jarvis.app.runtime.action.TAP_FEEDBACK"
        const val ACTION_SWIPE_FEEDBACK = "com.jarvis.app.runtime.action.SWIPE_FEEDBACK"
        const val ACTION_DISMISS = "com.jarvis.app.runtime.action.DISMISS"

        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_X = "extra_x"
        const val EXTRA_Y = "extra_y"
    }
}
