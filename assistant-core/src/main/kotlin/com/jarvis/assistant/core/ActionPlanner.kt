package com.jarvis.assistant.core

import com.jarvis.device.actions.DeviceAction
import com.jarvis.device.actions.WebSearchAction

class ActionPlanner {
    fun plan(parsedIntent: ParsedIntent): List<DeviceAction> {
        // Placeholder planning logic.
        return listOf(WebSearchAction(parsedIntent.rawCommand))
    }
}
