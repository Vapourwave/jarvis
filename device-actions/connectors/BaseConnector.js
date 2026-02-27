import { failedResult } from './types.js';

export class BaseConnector {
  /**
   * @param {{ id: string, name: string, available?: boolean }} config
   */
  constructor(config) {
    this.id = config.id;
    this.name = config.name;
    this.available = config.available ?? true;
  }

  isAvailable() {
    return this.available;
  }

  /**
   * @param {Record<string, any>} _action
   */
  preview(_action) {
    throw new Error(`${this.name} must implement preview()`);
  }

  /**
   * @param {Record<string, any>} action
   * @param {{ dryRun?: boolean }} options
   */
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
        message: `Dry-run preview for ${this.name}`,
        preview,
      };
    }

    return failedResult(this.id, `${this.name} does not support execute yet.`, { preview });
  }
}
