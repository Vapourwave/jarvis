import { BaseConnector } from './BaseConnector.js';
import { failedResult, successResult } from './types.js';

function summarizeResults(results = []) {
  if (!results.length) {
    return 'No relevant web results found.';
  }

  return results
    .slice(0, 3)
    .map((item, index) => `${index + 1}. ${item.title} — ${item.snippet}`)
    .join(' ');
}

export class BrowserConnector extends BaseConnector {
  /**
   * @param {{ available?: boolean, browserApi?: { openUrl: (url: string) => Promise<void> }, searchApi?: { query: (q: string) => Promise<Array<{ title: string, snippet: string, url: string }>> } }} [config]
   */
  constructor(config = {}) {
    super({
      id: 'browser',
      name: 'Browser Connector',
      available: config.available ?? true,
    });
    this.browserApi = config.browserApi;
    this.searchApi = config.searchApi;
  }

  /**
   * @param {{ query?: string, url?: string, openFullResults?: boolean }} action
   */
  preview(action) {
    const query = action?.query?.trim();
    const url = action?.url?.trim();

    if (!this.isAvailable()) {
      return {
        id: 'browser-unavailable',
        connector: this.id,
        summary: 'Browser functionality is unavailable on this device.',
        canExecute: false,
      };
    }

    if (!query && !url) {
      return {
        id: 'browser-invalid',
        connector: this.id,
        summary: 'Browser action requires either a query or a URL.',
        canExecute: false,
      };
    }

    if (url) {
      return {
        id: 'browser-open-url',
        connector: this.id,
        summary: `Open URL: ${url}`,
        canExecute: true,
        details: { url },
      };
    }

    return {
      id: 'browser-web-search',
      connector: this.id,
      summary: `Run web search for: "${query}"`,
      canExecute: true,
      details: {
        query,
        openFullResults: Boolean(action?.openFullResults),
      },
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
        message: 'Browser dry-run preview generated.',
        preview,
      };
    }

    if (preview.id === 'browser-open-url') {
      if (this.browserApi?.openUrl) {
        await this.browserApi.openUrl(preview.details.url);
      }

      return successResult(this.id, `Opened ${preview.details.url}.`, { preview, data: { url: preview.details.url } });
    }

    const query = preview.details.query;
    const results = this.searchApi?.query ? await this.searchApi.query(query) : [];
    const summary = summarizeResults(results);
    const payload = {
      query,
      results,
      readback: summary,
      openFullResults: preview.details.openFullResults,
    };

    if (preview.details.openFullResults) {
      const searchUrl = `https://www.google.com/search?q=${encodeURIComponent(query)}`;
      if (this.browserApi?.openUrl) {
        await this.browserApi.openUrl(searchUrl);
      }
      payload.fullResultsUrl = searchUrl;
    }

    return successResult(this.id, `Search complete. ${summary}`, { data: payload, preview });
  }
}
