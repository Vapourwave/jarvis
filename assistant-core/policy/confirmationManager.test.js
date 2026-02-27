const test = require('node:test');
const assert = require('node:assert/strict');

const { ConfirmationManager, SECOND_CONFIRM_PHRASE } = require('./confirmationManager');
const { AuditStore } = require('./auditStore');
const { RiskCategory } = require('./riskCategories');

function createMockIo(responses) {
  const queue = [...responses];
  return {
    spoken: [],
    prompts: [],
    async speak(text) {
      this.spoken.push(text);
    },
    async showPrompt(prompt) {
      this.prompts.push(prompt);
    },
    async captureResponse() {
      return queue.length ? queue.shift() : null;
    },
  };
}

test('confirms normal risk on explicit yes', async () => {
  const auditStore = new AuditStore();
  const io = createMockIo([{ channel: 'voice', value: 'yes' }]);
  const manager = new ConfirmationManager({ auditStore, io });

  const result = await manager.requestConfirmation({
    actionId: 'a1',
    category: RiskCategory.MODIFY_OR_DELETE_DATA,
    target: 'project task #42',
    payloadSummary: 'Delete task permanently',
  });

  assert.equal(result.outcome, 'confirmed');
  assert.equal(auditStore.list()[0].outcome, 'confirmed');
});

test('requires two-step confirmation for very high risk', async () => {
  const auditStore = new AuditStore();
  const io = createMockIo([
    { channel: 'voice', value: 'yes' },
    { channel: 'voice', value: SECOND_CONFIRM_PHRASE },
  ]);
  const manager = new ConfirmationManager({ auditStore, io });

  const result = await manager.requestConfirmation({
    actionId: 'a2',
    category: RiskCategory.SEND_MESSAGE_OR_POST,
    target: 'email: finance@example.com',
    payloadSummary: 'Send wire request details',
  });

  assert.equal(result.outcome, 'confirmed');
  assert.equal(auditStore.list()[0].outcome, 'confirmed');
});

test('asks clarification for ambiguous input and cancels on no response', async () => {
  const auditStore = new AuditStore();
  const io = createMockIo([{ channel: 'voice', value: 'maybe' }, { channel: 'button', value: 'no' }]);
  const manager = new ConfirmationManager({ auditStore, io });

  const result = await manager.requestConfirmation({
    actionId: 'a3',
    category: RiskCategory.UPLOAD_OR_SHARE_FILE,
    target: 'shared drive',
    payloadSummary: 'Upload customer PII export',
  });

  assert.equal(result.outcome, 'cancelled');
  assert.match(auditStore.list()[0].transcript.join('\n'), /unclear response/);
});
