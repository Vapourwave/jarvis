import { BaseConnector } from './BaseConnector.js';
import { failedResult, successResult } from './types.js';

export class AppLauncherConnector extends BaseConnector {
  /**
   * @param {{ installedApps?: string[], launcherApi?: { launch: (app: string) => Promise<void> } }} [config]
   */
  constructor(config = {}) {
    super({ id: 'appLauncher', name: 'App Launcher Connector', available: true });
    this.installedApps = config.installedApps ?? [];
    this.launcherApi = config.launcherApi;
  }

  isAvailable() {
    return this.installedApps.length > 0;
  }

  /**
   * @param {{ app: string }} action
   */
  preview(action) {
    const app = action?.app?.trim();

    if (!app) {
      return {
        id: 'app-launch-missing-app',
        connector: this.id,
        summary: 'App launcher requires an app name or package id.',
        canExecute: false,
      };
    }

    if (!this.installedApps.includes(app)) {
      return {
        id: 'app-not-installed',
        connector: this.id,
        summary: `${app} is not installed on this device.`,
        canExecute: false,
        details: { app, installedApps: this.installedApps },
      };
    }

    return {
      id: 'app-launch',
      connector: this.id,
      summary: `Launch app: ${app}`,
      canExecute: true,
      details: { app },
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
        message: 'App launcher dry-run preview generated.',
        preview,
      };
    }

    if (this.launcherApi?.launch) {
      await this.launcherApi.launch(preview.details.app);
    }

    return successResult(this.id, `${preview.details.app} launched.`, {
      data: { app: preview.details.app },
      preview,
    });
  }
}
