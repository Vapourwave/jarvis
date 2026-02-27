import { BaseConnector } from './BaseConnector.js';
import { failedResult, successResult } from './types.js';

export class SmsConnector extends BaseConnector {
  /**
   * @param {{ available?: boolean, api?: { send: (payload: { to: string, body: string }) => Promise<{ id: string }> } }} [config]
   */
  constructor(config = {}) {
    super({ id: 'sms', name: 'SMS Connector', available: config.available ?? true });
    this.api = config.api;
  }

  /**
   * @param {{ to?: string, body?: string }} action
   */
  preview(action) {
    const to = action?.to?.trim();
    const body = action?.body?.trim();

    if (!this.isAvailable()) {
      return {
        id: 'sms-unavailable',
        connector: this.id,
        summary: 'SMS is unavailable on this device.',
        canExecute: false,
      };
    }

    if (!to || !body) {
      return {
        id: 'sms-missing-fields',
        connector: this.id,
        summary: 'SMS requires both recipient and message body.',
        canExecute: false,
        details: { required: ['to', 'body'] },
      };
    }

    return {
      id: 'sms-send',
      connector: this.id,
      summary: `Send SMS to ${to}: "${body}"`,
      canExecute: true,
      details: { to, body },
    };
  }

  async execute(action, options = {}) {
    const preview = this.preview(action);
    if (!preview.canExecute) {
      return failedResult(this.id, preview.summary, { preview });
    }

    if (options.dryRun) {
      return {
        success: true,
        executed: false,
        connector: this.id,
        message: 'SMS dry-run preview generated.',
        preview,
      };
    }

    if (!this.api?.send) {
      return successResult(this.id, `SMS intent created for ${preview.details.to}.`, {
        data: { intent: 'android.intent.action.SENDTO', uri: `smsto:${preview.details.to}`, body: preview.details.body },
        preview,
      });
    }

    const response = await this.api.send({ to: preview.details.to, body: preview.details.body });
    return successResult(this.id, `SMS sent to ${preview.details.to}.`, { data: { messageId: response.id }, preview });
  }
}
