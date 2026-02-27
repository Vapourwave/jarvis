package assistant.core.nlu

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
enum class ActionType {
    OPEN_APP,
    CALL_CONTACT,
    SEND_MESSAGE,
    OPEN_URL,
    WEB_SEARCH
}

@Serializable
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

@Serializable
data class ActionPlan(
    val actionType: ActionType,
    val parameters: JsonObject,
    val requiresConfirmation: Boolean,
    val riskLevel: RiskLevel
)

@Serializable
data class DeviceContext(
    val installedApps: Set<String>,
    val contacts: Set<String>,
    val allowedActions: Set<ActionType> = ActionType.entries.toSet()
)
