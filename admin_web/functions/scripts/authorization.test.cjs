const {test} = require('node:test');
const assert = require('node:assert/strict');
const {canSendDeviceCommand} = require('../lib/authorization.js');
test('server permits exact management roles', () => {
  assert.equal(canSendDeviceCommand('Super Admin'),true);
  assert.equal(canSendDeviceCommand('Admin'),true);
});
test('server denies Support, missing, malformed and unknown roles', () => {
  for (const role of ['Support','super admin','Super Admin ','Owner','',null,undefined,{},['Admin']]) {
    assert.equal(canSendDeviceCommand(role),false);
  }
});
