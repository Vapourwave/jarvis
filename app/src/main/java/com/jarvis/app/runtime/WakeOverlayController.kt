package com.jarvis.app.runtime

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager

class WakeOverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var overlayView: WakeOverlayView? = null

    fun onWakeCommandHeard() {
        ensureViewAttached()
        overlayView?.showWakeListening()
    }

    fun onListening() {
        ensureViewAttached()
        overlayView?.showWakeListening()
    }

    fun onThinking() {
        ensureViewAttached()
        overlayView?.showThinking()
    }

    fun onTextResponse(text: String) {
        ensureViewAttached()
        overlayView?.showTextResponse(text)
    }

    fun onControlFlowReady() {
        ensureViewAttached()
        overlayView?.showControlMode()
    }

    fun showTapFeedback(x: Float, y: Float) {
        overlayView?.addTap(x, y)
    }

    fun showSwipeFeedback(x: Float, y: Float) {
        overlayView?.addSwipe(x, y)
    }

    fun dismiss() {
        overlayView?.let {
            windowManager.removeView(it)
        }
        overlayView = null
    }

    private fun ensureViewAttached() {
        if (overlayView != null || !canDrawOverlay()) return

        val view = WakeOverlayView(context)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(view, params)
        overlayView = view
    }

    private fun canDrawOverlay(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
}
