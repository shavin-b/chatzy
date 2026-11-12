const {test} = require('node:test');
const assert = require('node:assert/strict');
const {onCall, HttpsError} = require('firebase-functions/v2/https');
const {initializeApp} = require('firebase-admin/app');
const {getAuth} = require('firebase-admin/auth');
const express = require('express');
const {createSupportHandler} = require('../lib/support');
const requestId = '77c4a131-c397-4b72-96d1-a09d8969b8eb';

function fixture(overrides = {}) {
  const logs = []; const calls = [];
  const dependencies = {
    configuration: () => ({enabled: true, model: 'gemini-2.5-flash-lite', secret: 'TEST_ONLY_SECRET'}),
    reserve: async () => {},
    fetch: async (url, options) => {
      calls.push({url, options});
      return new Response(JSON.stringify({candidates: [{content: {parts: [{text: JSON.stringify({text: 'Open Service status.', topicIds: ['auto_reply'], actions: ['service_status']})}]}}]}), {status: 200});
    },
    log: entry => logs.push(entry), ...overrides
  };
  return {handler: createSupportHandler(dependencies), logs, calls};
}
const data = {requestId, turns: [{role: 'user', text: 'auto reply wada nahane'}]};

test('controlled HTTP callable verifies POST/auth/envelopes and Gemini response contract', async () => {
  const app = initializeApp({projectId: 'support-contract-test'});
  const auth = getAuth(app);
  // Confined to this test process. Production always uses real Firebase verification.
  auth.verifyIdToken = async value => {
    assert.equal(value, 'TEST_ONLY_AUTH');
    return {uid: 'test-client', aud: 'support-contract-test',
      firebase: {sign_in_provider: 'anonymous'}};
  };
  const f = fixture();
  const web = express();
  web.use(express.json({limit: '32kb'}));
  web.all('/chatzySupport', onCall({region: 'us-central1'}, f.handler));
  const server = web.listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  const url = `http://127.0.0.1:${server.address().port}/chatzySupport`;
  try {
    const denied = await fetch(url, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({data})});
    assert.equal(denied.status, 401);
    assert.equal((await denied.json()).error.status, 'UNAUTHENTICATED');
    assert.equal(f.calls.length, 0);
    const response = await fetch(url, {method: 'POST', headers: {'Content-Type': 'application/json', Authorization: 'Bearer TEST_ONLY_AUTH'}, body: JSON.stringify({data})});
    assert.equal(response.status, 200);
    const body = await response.json();
    assert.equal(body.result.text, 'Open Service status.');
    assert.deepEqual(body.result.actions, ['service_status']);
    assert.equal(body.result.requestId, requestId);
    assert.equal(f.calls[0].url, 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent');
    const providerPayload = JSON.parse(f.calls[0].options.body);
    assert.deepEqual(providerPayload.contents, [{role: 'user', parts: [{text: 'auto reply wada nahane'}]}]);
    assert.equal(providerPayload.requestId, undefined);
    assert.ok(!f.calls[0].options.body.includes('TEST_ONLY_AUTH'));
    assert.ok(!f.calls[0].options.body.includes('test-client'));
    assert.ok(!JSON.stringify(f.logs).includes('auto reply'));
    assert.ok(!JSON.stringify(f.logs).includes('TEST_ONLY_SECRET'));
    assert.ok(f.logs.some(l => l.stage === 'complete' && l.requestId === requestId));
  } finally { await new Promise(resolve => server.close(resolve)); }
});

test('provider statuses remain actionable with sanitized correlated diagnostics', async () => {
  for (const [status, code] of [[401, 'failed-precondition'], [403, 'failed-precondition'], [404, 'failed-precondition'], [429, 'resource-exhausted'], [503, 'unavailable']]) {
    const f = fixture({fetch: async () => new Response('private provider body', {status})});
    await assert.rejects(f.handler({auth: {uid: 'test'}, data}), e => e.code === code && e.details.requestId === requestId);
    assert.equal(f.logs.at(-1).providerStatus, status);
    assert.ok(!JSON.stringify(f.logs).includes('private provider body'));
  }
});

test('disabled feature, missing secret and quota reject before provider call', async () => {
  for (const [enabled, secret, reason] of [[false, 'key', 'support-disabled'], [true, '', 'secret-missing']]) {
    const f = fixture({configuration: () => ({enabled, secret, model: 'gemini-2.5-flash-lite'})});
    await assert.rejects(f.handler({auth: {uid: 'test'}, data}), e => e.code === 'failed-precondition' && e.details.reason === reason);
    assert.equal(f.calls.length, 0);
  }
  const f = fixture({reserve: async () => { throw new HttpsError('resource-exhausted', 'quota'); }});
  await assert.rejects(f.handler({auth: {uid: 'test'}, data}), {code: 'resource-exhausted'});
  assert.equal(f.calls.length, 0);
});

test('malformed or blocked provider replies never become assistant messages', async () => {
  const f = fixture({fetch: async () => new Response('{"candidates":[]}', {status: 200})});
  await assert.rejects(f.handler({auth: {uid: 'test'}, data}), {code: 'unavailable'});
  assert.equal(f.logs.at(-1).stage, 'provider-decode');
});
