package com.jarvis.assistant.core

import com.jarvis.device.actions.DeviceAction

sealed class PolicyDecision {
    data object Allowed : PolicyDecision()
    data class Blocked(val reason: String) : PolicyDecision()
}

class ExecutionPolicy {
    fun evaluate(action: DeviceAction, explicitConfirmation: Boolean): PolicyDecision {
        return if (action.requiresExplicitConfirmation && !explicitConfirmation) {
            PolicyDecision.Blocked(
                "Action '${action.describe()}' is blocked until explicit confirmation is obtained."
            )
        } else {
            PolicyDecision.Allowed
        }
    }
}
