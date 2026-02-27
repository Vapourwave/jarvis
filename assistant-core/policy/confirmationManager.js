const { isVeryHighRisk } = require("./riskCategories");

const DEFAULT_TIMEOUT_MS = 15_000;
const SECOND_CONFIRM_PHRASE = "confirm send now";

/**
 * @typedef {Object} PromptResponse
 * @property {"voice" | "button"} channel
 * @property {string} value
 */

/**
 * @typedef {Object} PromptIO
 * @property {(text: string) => Promise<void>} speak
 * @property {(prompt: { title: string, body: string, options: string[] }) => Promise<void>} showPrompt
 * @property {(timeoutMs: number) => Promise<PromptResponse | null>} captureResponse
 */

class ConfirmationManager {
  /**
   * @param {{ auditStore: { save: (event: any) => void }, io: PromptIO }} dependencies
   */
  constructor({ auditStore, io }) {
    this.auditStore = auditStore;
    this.io = io;
  }

  /**
   * @param {{ actionId: string, category: string, target: string, payloadSummary: string, timeoutMs?: number }} action
   */
  async requestConfirmation(action) {
    const timeoutMs = action.timeoutMs ?? DEFAULT_TIMEOUT_MS;
    const transcript = [];

    const summary = `About to perform action on ${action.target}. Payload: ${action.payloadSummary}. Reply yes or no.`;
    await this.io.speak(summary);
    transcript.push(`assistant: ${summary}`);

    await this.io.showPrompt({
      title: "Confirm action",
      body: `Target: ${action.target}\nPayload: ${action.payloadSummary}`,
      options: ["Yes", "No"],
    });

    const firstResult = await this.#captureExplicitYesNo(timeoutMs, transcript);
    if (firstResult.outcome !== "confirmed") {
      this.#saveAudit(action, transcript, firstResult.outcome);
      return firstResult;
    }

    if (!isVeryHighRisk(action.category)) {
      this.#saveAudit(action, transcript, "confirmed");
      return { outcome: "confirmed" };
    }

    const secondPrompt = `This is a high-risk action. Say \"${SECOND_CONFIRM_PHRASE}\" or press Yes to continue.`;
    await this.io.speak(secondPrompt);
    transcript.push(`assistant: ${secondPrompt}`);
    await this.io.showPrompt({
      title: "Second confirmation required",
      body: `High-risk action for ${action.target}.`,
      options: ["Yes", "Cancel"],
    });

    const secondResponse = await this.io.captureResponse(timeoutMs);
    if (!secondResponse) {
      transcript.push("system: second confirmation timed out");
      this.#saveAudit(action, transcript, "timeout");
      return { outcome: "timeout", reason: "No second-step response" };
    }

    transcript.push(`${secondResponse.channel}: ${secondResponse.value}`);
    const normalizedSecond = secondResponse.value.trim().toLowerCase();
    const isValidSecondConfirm =
      normalizedSecond === "yes" || normalizedSecond === SECOND_CONFIRM_PHRASE;

    if (!isValidSecondConfirm) {
      this.#saveAudit(action, transcript, "cancelled");
      return { outcome: "cancelled", reason: "Second confirmation phrase missing" };
    }

    this.#saveAudit(action, transcript, "confirmed");
    return { outcome: "confirmed" };
  }

  async #captureExplicitYesNo(timeoutMs, transcript) {
    const firstResponse = await this.io.captureResponse(timeoutMs);

    if (!firstResponse) {
      transcript.push("system: confirmation timed out");
      return { outcome: "timeout", reason: "No confirmation response" };
    }

    transcript.push(`${firstResponse.channel}: ${firstResponse.value}`);
    const normalized = firstResponse.value.trim().toLowerCase();

    if (normalized === "yes") {
      return { outcome: "confirmed" };
    }

    if (normalized === "no" || normalized === "cancel") {
      return { outcome: "cancelled", reason: "User rejected action" };
    }

    const clarifyPrompt = 'I heard an unclear response. Please answer with explicit "yes" or "no".';
    await this.io.speak(clarifyPrompt);
    transcript.push(`assistant: ${clarifyPrompt}`);

    const clarification = await this.io.captureResponse(timeoutMs);
    if (!clarification) {
      transcript.push("system: clarification timed out");
      return { outcome: "timeout", reason: "Clarification response missing" };
    }

    transcript.push(`${clarification.channel}: ${clarification.value}`);
    const clarified = clarification.value.trim().toLowerCase();

    if (clarified === "yes") {
      return { outcome: "confirmed" };
    }

    if (clarified === "no" || clarified === "cancel") {
      return { outcome: "cancelled", reason: "User rejected during clarification" };
    }

    return { outcome: "clarification_failed", reason: "Ambiguous clarification response" };
  }

  #saveAudit(action, transcript, outcome) {
    this.auditStore.save({
      actionId: action.actionId,
      category: action.category,
      target: action.target,
      payloadSummary: action.payloadSummary,
      transcript,
      outcome,
      timestamp: new Date().toISOString(),
    });
  }
}

module.exports = {
  ConfirmationManager,
  DEFAULT_TIMEOUT_MS,
  SECOND_CONFIRM_PHRASE,
};
