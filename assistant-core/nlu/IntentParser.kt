package assistant.core.nlu

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface NluModel {
    fun generateActionPlan(transcript: String, deviceContext: DeviceContext): String
}

class IntentParser(
    private val nluModel: NluModel,
    private val json: Json = Json {
        ignoreUnknownKeys = false
        isLenient = false
        explicitNulls = false
    }
) {
    fun parse(transcript: String, deviceContext: DeviceContext): ActionPlan {
        val output = nluModel.generateActionPlan(transcript, deviceContext)
        return parseStrict(output)
    }

    fun parseStrict(rawOutput: String): ActionPlan {
        val normalized = rawOutput.trim()
        if (!normalized.startsWith("{") || !normalized.endsWith("}")) {
            throw IllegalArgumentException("Parser output must be a single JSON object matching ActionPlan schema.")
        }

        return try {
            json.decodeFromString<ActionPlan>(normalized)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("Parser output rejected: must match ActionPlan schema exactly.", e)
        }
    }

    fun actionPlanPrompt(transcript: String, deviceContext: DeviceContext): String =
        """
        Produce only strict JSON matching ActionPlan.
        Transcript: $transcript
        DeviceContext: ${json.encodeToString(deviceContext)}

        Required JSON shape:
        {
          "actionType": "OPEN_APP|CALL_CONTACT|SEND_MESSAGE|OPEN_URL|WEB_SEARCH",
          "parameters": {},
          "requiresConfirmation": true|false,
          "riskLevel": "LOW|MEDIUM|HIGH"
        }
        No markdown, no prose, no extra keys.
        """.trimIndent()
}
