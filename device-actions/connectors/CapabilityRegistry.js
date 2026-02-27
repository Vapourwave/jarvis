import { SmsConnector } from './SmsConnector.js';
import { WhatsAppConnector } from './WhatsAppConnector.js';
import { BrowserConnector } from './BrowserConnector.js';
import { AppLauncherConnector } from './AppLauncherConnector.js';

export class CapabilityRegistry {
  /**
   * @param {{
   *  deviceProfile?: { installedApps?: string[], hasSmsService?: boolean, hasWebBrowser?: boolean, hasWebSearch?: boolean },
   *  adapters?: {
   *    smsApi?: { send: (payload: { to: string, body: string }) => Promise<{ id: string }> },
   *    shareApi?: { share: (payload: { targetApp: string, to?: string, body: string }) => Promise<{ threadId?: string }> },
   *    browserApi?: { openUrl: (url: string) => Promise<void> },
   *    searchApi?: { query: (q: string) => Promise<Array<{ title: string, snippet: string, url: string }>> },
   *    launcherApi?: { launch: (app: string) => Promise<void> }
   *  }
   * }} [config]
   */
  constructor(config = {}) {
    this.deviceProfile = {
      installedApps: config.deviceProfile?.installedApps ?? [],
      hasSmsService: config.deviceProfile?.hasSmsService ?? false,
      hasWebBrowser: config.deviceProfile?.hasWebBrowser ?? false,
      hasWebSearch: config.deviceProfile?.hasWebSearch ?? false,
    };

    this.adapters = config.adapters ?? {};
  }

  buildConnectors() {
    const installedApps = this.deviceProfile.installedApps;

    return {
      sms: new SmsConnector({
        available: this.deviceProfile.hasSmsService,
        api: this.adapters.smsApi,
      }),
      whatsapp: new WhatsAppConnector({
        installedApps,
        shareApi: this.adapters.shareApi,
      }),
      browser: new BrowserConnector({
        available: this.deviceProfile.hasWebBrowser,
        browserApi: this.adapters.browserApi,
        searchApi: this.deviceProfile.hasWebSearch ? this.adapters.searchApi : undefined,
      }),
      appLauncher: new AppLauncherConnector({
        installedApps,
        launcherApi: this.adapters.launcherApi,
      }),
    };
  }

  listAvailableConnectors() {
    const connectors = this.buildConnectors();

    return Object.values(connectors)
      .filter((connector) => connector.isAvailable())
      .map((connector) => ({ id: connector.id, name: connector.name }));
  }

  getConnector(id) {
    return this.buildConnectors()[id];
  }
}
