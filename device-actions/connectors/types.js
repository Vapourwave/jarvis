/**
 * @typedef {{ dryRun?: boolean }} ExecutionOptions
 * @typedef {{ id: string, connector: string, summary: string, details?: Record<string, any>, canExecute: boolean, fallbackPrompt?: string, suggestedAction?: Record<string, any> }} ActionPreview
 * @typedef {{ success: boolean, executed: boolean, connector: string, message: string, data?: Record<string, any>, preview?: ActionPreview }} ExecutionResult
 * @typedef {{ name: string, installedApps?: string[], hasSmsService?: boolean, hasWebBrowser?: boolean, hasWebSearch?: boolean }} DeviceProfile
 */

/**
 * @param {string} connector
 * @param {string} message
 * @param {Partial<ExecutionResult>} [extra]
 * @returns {ExecutionResult}
 */
export function successResult(connector, message, extra = {}) {
  return {
    success: true,
    executed: true,
    connector,
    message,
    ...extra,
  };
}

/**
 * @param {string} connector
 * @param {string} message
 * @param {Partial<ExecutionResult>} [extra]
 * @returns {ExecutionResult}
 */
export function failedResult(connector, message, extra = {}) {
  return {
    success: false,
    executed: false,
    connector,
    message,
    ...extra,
  };
}
