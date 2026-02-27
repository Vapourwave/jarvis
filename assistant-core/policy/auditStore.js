/**
 * @typedef {Object} AuditEvent
 * @property {string} actionId
 * @property {string} category
 * @property {string} target
 * @property {string} payloadSummary
 * @property {string[]} transcript
 * @property {"confirmed" | "cancelled" | "timeout" | "clarification_failed"} outcome
 * @property {string} timestamp
 */

class AuditStore {
  constructor() {
    /** @type {AuditEvent[]} */
    this.events = [];
  }

  /** @param {AuditEvent} event */
  save(event) {
    this.events.push(event);
  }

  /** @returns {AuditEvent[]} */
  list() {
    return [...this.events];
  }
}

module.exports = {
  AuditStore,
};
