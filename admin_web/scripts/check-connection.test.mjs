import {test} from 'node:test';
import assert from 'node:assert/strict';
import {analyzeBundle} from './check-connection.mjs';

test('detects the deployed constant-folded missing-command configuration', () => {
  const result = analyzeBundle(`const config={projectId:'chatzy-d8ba4',apiKey:'TEST_ONLY_PRIVATE_VALUE'};
    async function send(){ {const error=Error('VITE_COMMAND_API_BASE_URL is not configured');throw error;} }`);
  assert.deepEqual(result.projects, ['chatzy-d8ba4']);
  assert.equal(result.commandConfigurationMissing, true);
  assert.equal(JSON.stringify(result).includes('TEST_ONLY_PRIVATE_VALUE'), false);
});

test('a conditional configuration guard is not classified as an unconditional failure', () => {
  const result = analyzeBundle(`async function send(base){if(!base){throw Error('VITE_COMMAND_API_BASE_URL is not configured')}return fetch(base)}`);
  assert.equal(result.commandConfigurationMissing, false);
});

test('reads encoded property names used in the deployed minified bundle', () => {
  const result = analyzeBundle(String.raw`const config={['\x70rojectId']:'chatzy-d8ba4'};`);
  assert.deepEqual(result.projects, ['chatzy-d8ba4']);
});

test('reports a missing production Firebase project separately from a different project', () => {
  const result = analyzeBundle(`const config={apiKey:'TEST_ONLY_VALUE',authDomain:'example.firebaseapp.com',projectId:void 0,appId:'test'};`);
  assert.deepEqual(result.projects, []);
  assert.deepEqual(result.missingFirebaseFields, ['projectId']);
  assert.equal(JSON.stringify(result).includes('TEST_ONLY_VALUE'), false);
});
