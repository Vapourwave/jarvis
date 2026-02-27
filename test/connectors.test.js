import test from 'node:test';
import assert from 'node:assert/strict';

import {
  AppLauncherConnector,
  BrowserConnector,
  CapabilityRegistry,
  SmsConnector,
  WhatsAppConnector,
} from '../device-actions/connectors/index.js';

test('WhatsApp connector returns SMS fallback prompt when unavailable', async () => {
  const connector = new WhatsAppConnector({ installedApps: [] });
  const result = await connector.execute({ to: '+123', body: 'hello' });

  assert.equal(result.success, false);
  assert.equal(result.preview.fallbackPrompt, 'WhatsApp not installed, should I send via SMS instead?');
  assert.equal(result.preview.suggestedAction.connector, 'sms');
});

test('Browser connector executes query and supports opening full results', async () => {
  let openedUrl;
  const connector = new BrowserConnector({
    available: true,
    browserApi: {
      openUrl: async (url) => {
        openedUrl = url;
      },
    },
    searchApi: {
      query: async () => [
        { title: 'A', snippet: 'alpha', url: 'https://a.example' },
        { title: 'B', snippet: 'beta', url: 'https://b.example' },
      ],
    },
  });

  const result = await connector.execute({ query: 'test query', openFullResults: true });

  assert.equal(result.success, true);
  assert.equal(result.data.results.length, 2);
  assert.match(result.data.readback, /A — alpha/);
  assert.match(openedUrl, /google\.com\/search\?q=test%20query/);
});

test('connectors support dry-run previews', async () => {
  const sms = new SmsConnector({ available: true });
  const appLauncher = new AppLauncherConnector({ installedApps: ['calendar'] });

  const smsDryRun = await sms.execute({ to: '+123', body: 'hi' }, { dryRun: true });
  const appDryRun = await appLauncher.execute({ app: 'calendar' }, { dryRun: true });

  assert.equal(smsDryRun.executed, false);
  assert.equal(appDryRun.executed, false);
});

test('capability registry lists available connectors', () => {
  const registry = new CapabilityRegistry({
    deviceProfile: {
      installedApps: ['calendar', 'com.whatsapp'],
      hasSmsService: true,
      hasWebBrowser: true,
      hasWebSearch: true,
    },
  });

  const available = registry.listAvailableConnectors();
  const ids = available.map((entry) => entry.id).sort();

  assert.deepEqual(ids, ['appLauncher', 'browser', 'sms', 'whatsapp']);
});
