import { BaseConnector } from './BaseConnector.js';
import { failedResult, successResult } from './types.js';

export class WhatsAppConnector extends BaseConnector {
  /**
   * @param {{ installedApps?: string[], packageName?: string, shareApi?: { share: (payload: { targetApp: string, to?: string, body: string }) => Promise<{ threadId?: string }> } }} [config]
   */
  constructor(config = {}) {
    super({ id: 'whatsapp', name: 'WhatsApp Connector', available: true });
    this.packageName = config.packageName ?? 'com.whatsapp';
    this.installedApps = config.installedApps ?? [];
    this.shareApi = config.shareApi;
  }

  isAvailable() {
    return this.installedApps.includes(this.packageName);
  }

  /**
   * @param {{ to?: string, body?: string }} action
   */
  preview(action) {
    const body = action?.body?.trim();
    const to = action?.to?.trim();

    if (!body) {
      return {
        id: 'wa-missing-body',
        connector: this.id,
        summary: 'WhatsApp share requires a message body.',
        canExecute: false,
      };
    }

    if (!this.isAvailable()) {
      return {
        id: 'wa-not-installed',
        connector: this.id,
        summary: 'WhatsApp is not installed.',
        canExecute: false,
        fallbackPrompt: 'WhatsApp not installed, should I send via SMS instead?',
        suggestedAction: {
          connector: 'sms',
          action: { to, body },
        },
      };
    }

    return {
      id: 'wa-share',
      connector: this.id,
      summary: to
        ? `Share WhatsApp message to ${to}: "${body}"`
        : `Share WhatsApp message: "${body}"`,
      canExecute: true,
      details: { to, body, packageName: this.packageName },
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
        message: 'WhatsApp dry-run preview generated.',
        preview,
      };
    }

    if (!this.shareApi?.share) {
      return successResult(this.id, 'Share intent created for WhatsApp.', {
        data: {
          intent: 'android.intent.action.SEND',
          package: preview.details.packageName,
          body: preview.details.body,
          to: preview.details.to,
        },
        preview,
      });
    }

    const response = await this.shareApi.share({
      targetApp: preview.details.packageName,
      to: preview.details.to,
      body: preview.details.body,
    });

    return successResult(this.id, 'Shared message via WhatsApp.', {
      data: { threadId: response.threadId },
      preview,
    });
  }
}
