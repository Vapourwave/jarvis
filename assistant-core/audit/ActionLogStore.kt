package assistant.core.audit

import assistant.core.execution.ExecutionOutcome
import assistant.core.nlu.ActionPlan
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

class ActionLogStore(
    private val logPath: Path = Path.of("assistant-core", "audit", "action-log.jsonl"),
    private val json: Json = Json { prettyPrint = false }
) {
    init {
        Files.createDirectories(logPath.parent)
        if (!Files.exists(logPath)) {
            Files.createFile(logPath)
        }
    }

    fun append(plan: ActionPlan, result: ExecutionOutcome) {
        val record = ActionAuditRecord(
            timestamp = Instant.now().toString(),
            plan = plan,
            result = result
        )
        Files.writeString(
            logPath,
            json.encodeToString(record) + "\n",
            java.nio.file.StandardOpenOption.APPEND
        )
    }
}

@Serializable
data class ActionAuditRecord(
    val timestamp: String,
    val plan: ActionPlan,
    val result: ExecutionOutcome
)
