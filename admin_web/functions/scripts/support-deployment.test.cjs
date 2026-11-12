const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const {appProject} = require('./deploy-support.cjs');
test('support deployment follows Android variant precedence, never the unrelated CLI default', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'chatzy-support-project-'));
  if (path.dirname(path.resolve(root)) !== path.resolve(os.tmpdir()) || !path.basename(root).startsWith('chatzy-support-project-')) throw new Error('Unexpected fixture path');
  const write = (file, project) => {
    fs.mkdirSync(path.dirname(path.join(root, file)), {recursive: true});
    fs.writeFileSync(path.join(root, file), JSON.stringify({project_info: {project_id: project}, client: [{client_info: {android_client_info: {package_name: 'com.orderflow.autoresponder'}}}]}));
  };
  try {
    write('app/google-services.json', 'app-project');
    write('app/src/debug/google-services.json', 'debug-project');
    assert.equal(appProject(root, 'debug'), 'debug-project');
    assert.equal(appProject(root, 'release'), 'app-project');
  } finally {
    fs.rmSync(root, {recursive: true, force: true});
  }
});
