package com.jarvis.app.automation

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class JarvisAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Hook for future UI automation based on approved assistant actions.
    }

    override fun onInterrupt() {
        // No-op.
    }
}
