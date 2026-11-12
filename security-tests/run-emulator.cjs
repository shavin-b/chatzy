// Keep downloaded emulator binaries inside the ignored workspace, never a production project.
const { spawnSync } = require('node:child_process');
const path = require('node:path');
const result = spawnSync(process.execPath, [
  require.resolve('firebase-tools/lib/bin/firebase.js'),
  'emulators:exec', '--only', 'firestore,auth', '--project', 'demo-chatzy-security',
  '--config', 'firebase.json', 'node --test --test-concurrency=1 rules.test.mjs enrollment.test.cjs'
], {
  cwd: __dirname,
  stdio: 'inherit',
  env: { ...process.env, FIREBASE_EMULATORS_PATH: path.resolve(__dirname, '../.artifacts/firebase-emulators') }
});
process.exit(result.status ?? 1);
