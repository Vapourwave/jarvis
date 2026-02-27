package assistant.core.nlu

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.net.URI

class PlanValidator {
    fun validate(plan: ActionPlan, deviceContext: DeviceContext): ActionPlan {
        enforceAllowedAction(plan, deviceContext)
        val sanitizedParameters = sanitizeAndVerifyParameters(plan, deviceContext)

        return plan.copy(parameters = sanitizedParameters)
    }

    private fun enforceAllowedAction(plan: ActionPlan, deviceContext: DeviceContext) {
        if (plan.actionType !in deviceContext.allowedActions) {
            throw IllegalArgumentException("Action ${plan.actionType} is not in allowed-action whitelist.")
        }
    }

    private fun sanitizeAndVerifyParameters(plan: ActionPlan, deviceContext: DeviceContext): JsonObject {
        return when (plan.actionType) {
            ActionType.OPEN_APP -> {
                val appName = extractRequired(plan.parameters, "appName")
                verifyAppExists(appName, deviceContext)
                buildJsonObject {
                    put("appName", JsonPrimitive(appName))
                }
            }

            ActionType.CALL_CONTACT,
            ActionType.SEND_MESSAGE -> {
                val contactName = extractRequired(plan.parameters, "contactName")
                verifyContactExists(contactName, deviceContext)

                buildJsonObject {
                    put("contactName", JsonPrimitive(contactName))
                    plan.parameters["message"]?.let {
                        put("message", JsonPrimitive(sanitizeSearchTerm((it as JsonPrimitive).content)))
                    }
                }
            }

            ActionType.OPEN_URL -> {
                val rawUrl = extractRequired(plan.parameters, "url")
                val cleanUrl = sanitizeUrl(rawUrl)
                buildJsonObject {
                    put("url", JsonPrimitive(cleanUrl))
                }
            }

            ActionType.WEB_SEARCH -> {
                val rawTerm = extractRequired(plan.parameters, "query")
                buildJsonObject {
                    put("query", JsonPrimitive(sanitizeSearchTerm(rawTerm)))
                }
            }
        }
    }

    private fun extractRequired(parameters: JsonObject, key: String): String {
        val value = parameters[key] as? JsonPrimitive
            ?: throw IllegalArgumentException("Missing required parameter: $key")
        return value.content
    }

    private fun verifyAppExists(appName: String, deviceContext: DeviceContext) {
        if (appName !in deviceContext.installedApps) {
            throw IllegalArgumentException("App '$appName' is not installed on this device.")
        }
    }

    private fun verifyContactExists(contactName: String, deviceContext: DeviceContext) {
        if (contactName !in deviceContext.contacts) {
            throw IllegalArgumentException("Contact '$contactName' does not exist.")
        }
    }

    private fun sanitizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        val uri = URI(trimmed)
        if (uri.scheme !in setOf("http", "https")) {
            throw IllegalArgumentException("URL must use http or https.")
        }
        if (uri.host.isNullOrBlank()) {
            throw IllegalArgumentException("URL must include a valid host.")
        }
        return uri.normalize().toString()
    }

    private fun sanitizeSearchTerm(raw: String): String {
        return raw
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[\\u0000-\\u001F]"), "")
    }
}
