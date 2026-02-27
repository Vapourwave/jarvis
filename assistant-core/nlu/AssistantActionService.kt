package assistant.core.nlu

import assistant.core.execution.ExecutionOrchestrator
import assistant.core.execution.ExecutionOutcome

class AssistantActionService(
    private val intentParser: IntentParser,
    private val executionOrchestrator: ExecutionOrchestrator
) {
    fun handleTranscript(transcript: String, deviceContext: DeviceContext): ExecutionOutcome {
        val plan = intentParser.parse(transcript, deviceContext)
        return executionOrchestrator.orchestrate(plan, deviceContext)
    }
}
