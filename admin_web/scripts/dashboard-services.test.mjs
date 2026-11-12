import {test} from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import ts from 'typescript';
import {createRequire} from 'node:module';
import React from 'react';
import {renderToStaticMarkup} from 'react-dom/server';
const requireDependency = createRequire(import.meta.url);

// Execute the actual TypeScript modules with controlled React/Firebase/network boundaries.
// No production credentials, browser storage, Firestore documents or devices are used.
function load(relative, dependencies, extra = {}) {
  const source = fs.readFileSync(new URL(relative, import.meta.url), 'utf8');
  const code = ts.transpileModule(source, {fileName: relative, compilerOptions: {module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, jsx: ts.JsxEmit.ReactJSX}}).outputText;
  const module = {exports: {}};
  vm.runInNewContext(code, {module, exports: module.exports,
    require: name => { if (!(name in dependencies)) throw Error(`Unexpected test dependency: ${name}`); return dependencies[name]; },
    console: {error() {}, log() {}}, ...extra});
  return module.exports;
}

function commandHarness(options = {}) {
  const states = []; const timers = new Map(); const effects = [];
  let nextTimer = 0; let sends = 0; let stops = 0; let onSnapshot;
  const react = {
    useState(value) { const index = states.length; states.push(value); return [value, next => { states[index] = typeof next === 'function' ? next(states[index]) : next; }]; },
    useRef: value => ({current: value}),
    useEffect: effect => effects.push(effect()),
  };
  const commands = {
    async sendDeviceCommand() { sends++; return options.send ? options.send() : {commandId: 'test-command'}; },
    subscribeToCommand(_id, callback) { onSnapshot = callback; if (options.immediate) callback({status: 'SUCCESS'}); return () => { stops++; }; },
  };
  const {useDeviceCommand} = load('../src/hooks/useDeviceCommand.ts', {
    react, '../services/commands': commands,
    '../services/subscriptions': {writeAuditLog: async () => {}},
    './useAuth': {useAuth: () => ({user: {}, admin: {email: 'test@example.invalid'}})},
  }, {setTimeout: callback => { const id = ++nextTimer; timers.set(id, callback); return id; }, clearTimeout: id => timers.delete(id)});
  // eslint-disable-next-line react-hooks/rules-of-hooks -- Test-only execution with the controlled React boundary above.
  const hook = useDeviceCommand();
  return {hook, states, timers, effects, snapshot: value => onSnapshot(value), sends: () => sends, stops: () => stops};
}

test('a sent command with no acknowledgement reaches offline_queued despite the initial idle render', async () => {
  const h = commandHarness();
  await h.hook.issueCommand('test-device', 'LOCK');
  assert.equal(h.states[0], 'sent');
  for (const callback of [...h.timers.values()]) callback();
  assert.equal(h.states[0], 'offline_queued');
  assert.equal(h.stops(), 1);
  assert.equal(h.timers.size, 0);
});

test('success and failure acknowledgements cancel the offline timeout', async () => {
  for (const [status, state] of [['SUCCESS', 'success'], ['FAILED', 'failed']]) {
    const h = commandHarness();
    await h.hook.issueCommand('test-device', 'UNLOCK');
    h.snapshot({status, errorMessage: 'Test-only failure'});
    assert.equal(h.states[0], state);
    assert.equal(h.timers.size, 0);
    assert.equal(h.stops(), 1);
  }
});

test('an immediate cached acknowledgement also releases the subscription and timeout', async () => {
  const h = commandHarness({immediate: true});
  await h.hook.issueCommand('test-device', 'SYNC');
  assert.equal(h.states[0], 'success');
  assert.equal(h.stops(), 1);
  assert.equal(h.timers.size, 0);
});

test('rapid duplicate command submissions send only once', async () => {
  const h = commandHarness();
  await Promise.all([h.hook.issueCommand('test-device', 'LOCK'), h.hook.issueCommand('test-device', 'LOCK')]);
  assert.equal(h.sends(), 1);
  h.hook.resetState();
  await h.hook.issueCommand('test-device', 'UNLOCK');
  assert.equal(h.sends(), 2);
});

test('reset during a pending request cannot recreate an obsolete subscription', async () => {
  let resolve;
  const pending = new Promise(r => { resolve = r; });
  const h = commandHarness({send: () => pending});
  const request = h.hook.issueCommand('test-device', 'LOCK');
  h.hook.resetState();
  resolve({commandId: 'test-command'});
  await request;
  assert.equal(h.states[0], 'idle');
  assert.equal(h.timers.size, 0);
});

function subscriptionHarness() {
  const device = {status: 'Active', subscriptionEnd: 2_000_000, adminLocked: true};
  const records = [];
  const services = load('../src/services/subscriptions.ts', {
    'firebase/firestore': {doc: (_db, collection, id) => ({collection, id}), collection: (_db, name) => name,
      setDoc: async (ref, data) => records.push({ref, data}), addDoc: async () => ({id: 'test-log'})},
    './firebase': {db: {}},
    './devices': {
      updateDeviceSubscription: async (_id, expiry, status) => { device.subscriptionEnd = expiry; device.status = status; },
      updateDeviceStatus: async (_id, status) => { device.status = status; },
    },
  }, {Date: class extends Date { static now() { return 1_000_000; } }});
  return {device, records, services};
}

test('Activate and Suspend preserve expiry and lock authority in both device and history', async () => {
  for (const status of ['Active', 'Suspended']) {
    const h = subscriptionHarness();
    await h.services.setSubscriptionStatus({deviceId: 'test-device', businessName: 'Test', status,
      adminEmail: 'test@example.invalid', currentExpiryMs: h.device.subscriptionEnd});
    assert.equal(h.device.subscriptionEnd, 2_000_000);
    assert.equal(h.device.adminLocked, true);
    assert.equal(h.device.status, status);
    const history = h.records.find(r => r.ref.collection === 'subscriptions').data;
    assert.equal(history.oldExpiryDate, 2_000_000);
    assert.equal(history.newExpiryDate, 2_000_000);
  }
});

test('subscription extensions retain remaining time and never change lock authority', async () => {
  const h = subscriptionHarness();
  await h.services.extendSubscription({deviceId: 'test-device', businessName: 'Test', currentExpiryMs: 2_000_000,
    daysToAdd: 7, adminEmail: 'test@example.invalid', adminUid: 'test-admin', newStatus: 'Active'});
  assert.equal(h.device.subscriptionEnd, 2_000_000 + 7 * 86400000);
  assert.equal(h.device.adminLocked, true);
});

test('device subscription includes missing-lastSeen records and sorts normalized times', () => {
  let receive; let query;
  const {subscribeToDevices} = load('../src/services/devices.ts', {
    'firebase/firestore': {
      collection: (_db, name) => ({collection: name}),
      onSnapshot: (ref, callback) => { query = ref; receive = callback; return () => {}; },
      orderBy: () => { throw Error('Server ordering excludes missing lastSeen'); },
    },
    './firebase': {db: {}},
    '../lib/dateUtils': {toEpochMs: value => value?.toMillis ? value.toMillis() : value ?? null,
      isOnline: () => false, daysRemaining: () => 0, computeSubscriptionStatus: () => 'Active'},
  });
  let devices;
  subscribeToDevices(value => { devices = value; }, () => {});
  assert.equal(query.collection, 'devices');
  receive({empty: false, docs: [
    {id: 'missing', data: () => ({})},
    {id: 'numeric', data: () => ({lastSeen: 100})},
    {id: 'timestamp', data: () => ({lastSeen: {toMillis: () => 300}})},
  ]});
  assert.deepEqual(Array.from(devices, d => d.deviceId), ['timestamp', 'numeric', 'missing']);
  assert.equal(devices[2].lastSeen, null);
});

test('device read failures retain a distinct permission error, not an empty successful result', () => {
  const states = []; let fail;
  const {useDevices} = load('../src/hooks/useDevices.ts', {
    react: {
      useState: initial => {const i = states.length; states.push(initial); return [initial, next => { states[i] = next; }];},
      useEffect: callback => callback(),
    },
    '../services/devices': {subscribeToDevices: (_success, error) => {fail = error; return () => {}; }},
    '../lib/dateUtils': {},
  });
  // eslint-disable-next-line react-hooks/rules-of-hooks -- Controlled hook boundary; no production React session.
  useDevices();
  fail({code: 'permission-denied'});
  assert.match(states[2], /access denied/);
  assert.equal(states[1], false);
});

test('dashboard and Devices page never render no-registration claims after a database error', () => {
  const dependencies = {
    react: React, 'react/jsx-runtime': requireDependency('react/jsx-runtime'),
    'lucide-react': requireDependency('lucide-react'),
    'react-router-dom': {Link: props => React.createElement('a', {href: props.to}, props.children)},
    '../hooks/useDevices': {
      useDevices: () => ({devices: [], loading: false, error: 'Device list access denied.'}),
      useDashboardStats: () => ({}),
    },
    '../hooks/useAuth': {useAuth: () => ({admin: {name: 'Test'}})},
    '../lib/dateUtils': {}, '../lib/cn': {cn: (...values) => values.filter(Boolean).join(' ')},
    '../components/StatusBadge': {},
  };
  for (const [file, name] of [['Dashboard', 'DashboardPage'], ['Devices', 'DevicesPage']]) {
    const component = load(`../src/pages/${file}.tsx`, dependencies)[name];
    const html = renderToStaticMarkup(React.createElement(component));
    assert.ok(html.includes('Device list access denied.'));
    assert.equal(html.includes('No devices registered'), false);
    assert.equal(html.includes('0 devices registered'), false);
  }
});
