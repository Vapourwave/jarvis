/**
 * Risk categories used by policy checks before running user-impacting actions.
 */
const RiskCategory = Object.freeze({
  SEND_MESSAGE_OR_POST: "send_message_or_post",
  MODIFY_OR_DELETE_DATA: "modify_or_delete_data",
  UPLOAD_OR_SHARE_FILE: "upload_or_share_file",
  FINANCIAL_OR_ACCOUNT_ACTION: "financial_or_account_action",
  OTHER: "other",
});

/**
 * Categories that always require a second step confirmation.
 */
const VERY_HIGH_RISK_CATEGORIES = new Set([
  RiskCategory.SEND_MESSAGE_OR_POST,
  RiskCategory.FINANCIAL_OR_ACCOUNT_ACTION,
]);

function isVeryHighRisk(category) {
  return VERY_HIGH_RISK_CATEGORIES.has(category);
}

module.exports = {
  RiskCategory,
  VERY_HIGH_RISK_CATEGORIES,
  isVeryHighRisk,
};
