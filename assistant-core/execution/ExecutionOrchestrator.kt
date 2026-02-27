package assistant.core.execution

import assistant.core.audit.ActionLogStore
import assistant.core.nlu.ActionPlan
import assistant.core.nlu.PlanValidator
import assistant.core.nlu.RiskLevel
import kotlinx.serialization.Serializable

interface ActionExecutor {
    fun execute(plan: ActionPlan): ExecutionOutcome
}

interface ConfirmationRequester {
    fun requestConfirmation(plan: ActionPlan): Boolean
}

class ExecutionOrchestrator(
    private val planValidator: PlanValidator,
    private val actionExecutor: ActionExecutor,
    private val confirmationRequester: ConfirmationRequester,
    private val actionLogStore: ActionLogStore
) {
    fun orchestrate(plan: ActionPlan, context: assistant.core.nlu.DeviceContext): ExecutionOutcome {
        val validatedPlan = planValidator.validate(plan, context)
        val outcome = when (validatedPlan.riskLevel) {
            RiskLevel.LOW -> actionExecutor.execute(validatedPlan)
            RiskLevel.MEDIUM,
            RiskLevel.HIGH -> handleConfirmation(validatedPlan)
        }

        actionLogStore.append(validatedPlan, outcome)
        return outcome
    }

    private fun handleConfirmation(plan: ActionPlan): ExecutionOutcome {
        val approved = confirmationRequester.requestConfirmation(plan)
        if (!approved) {
            return ExecutionOutcome(
                status = ExecutionStatus.CANCELLED,
                message = "User declined confirmation for ${plan.actionType}."
            )
        }

        return actionExecutor.execute(plan)
    }
}

@Serializable
enum class ExecutionStatus {
    EXECUTED,
    CANCELLED,
    FAILED
}

@Serializable
data class ExecutionOutcome(
    val status: ExecutionStatus,
    val message: String
)
