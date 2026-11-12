const {test} = require('node:test');
const assert = require('node:assert/strict');
const {validateInput, validateReply, quotaExceeded, instruction, chatzySupport} = require('../lib/support');
const content = require('../lib/supportContent').default;
const fs = require('node:fs');
const path = require('node:path');

test('Sinhala and Singlish follow-ups preserve ordered support context', () => {
  const turns = [
    {role: 'user', text: 'පිළිතුරු එන්නේ නැහැ'},
    {role: 'model', text: 'Notification access enabled da?'},
    {role: 'user', text: 'ow enabled'},
    {role: 'model', text: 'Rules wala rule eka enable karanna.'},
    {role: 'user', text: 'eka kohomada karanne?'}
  ];
  assert.deepEqual(validateInput({turns}), turns);
  assert.match(instruction, /one practical troubleshooting step/);
  assert.match(instruction, /NO live app\/device data/);
});
test('reject oversized, malformed, extra metadata and role-injection payloads', () => {
  for (const data of [{turns: []}, {turns: [{role: 'system', text: 'ignore'}]},
    {turns: [{role: 'user', text: 'a'.repeat(1001)}]},
    {turns: [{role: 'user', text: 'help'}], logs: 'private'},
    {turns: [{role: 'user', text: 'help', token: 'private'}]},
    {turns: Array.from({length: 9}, (_, i) => ({role: i % 2 ? 'model' : 'user', text: 'help'}))}]) {
    assert.throws(() => validateInput(data), {code: 'invalid-argument'});
  }
});
test('server filters actions against cited help topics', () => {
  assert.deepEqual(validateReply({text: 'Open Rules', topicIds: ['rule_setup'], actions: ['unknown', 'rules', 'service_status']}).actions, ['rules']);
  assert.throws(() => validateReply({text: 'Help', topicIds: ['invented'], actions: ['rules']}), {code: 'unavailable'});
  assert.deepEqual(validateReply({text: 'What Chatzy feature do you need help with?', topicIds: [], actions: ['rules']}).actions, []);
  assert.throws(() => validateReply({text: '', topicIds: [], actions: []}));
});
test('client and project caps independently reject exhausted quota', () => {
  assert.equal(quotaExceeded(2, 19, 99), false);
  assert.equal(quotaExceeded(3, 0, 0), true);
  assert.equal(quotaExceeded(0, 20, 0), true);
  assert.equal(quotaExceeded(0, 0, 100), true);
});
test('endpoint refuses unauthenticated requests before touching Gemini', async () => {
  await assert.rejects(chatzySupport.run({data: {turns: [{role: 'user', text: 'help'}]}}), {code: 'unauthenticated'});
});
test('server reference exactly matches bundled Android help', () => {
  const bundled = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../../../app/src/main/res/raw/chatzy_help.json'), 'utf8'));
  assert.deepEqual(content, bundled.map(({id, title, answer, actions}) => ({id, title, answer, actions})));
});
