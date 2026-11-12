const fs = require('node:fs');
const path = require('node:path');
const {spawnSync} = require('node:child_process');

function appProject(root, variant = 'debug') {
  if (!['debug', 'release'].includes(variant)) throw new Error('Use debug or release.');
  const candidates = [`app/src/${variant}/google-services.json`, 'app/google-services.json'];
  const file = candidates.map(p => path.join(root, p)).find(p => fs.existsSync(p));
  if (!file) throw new Error('Missing genuine Android google-services.json.');
  const config = JSON.parse(fs.readFileSync(file, 'utf8'));
  const project = config.project_info?.project_id;
  if (typeof project !== 'string' || !/^[a-z][a-z0-9-]+$/.test(project)) throw new Error('Invalid Firebase project ID.');
  if (!config.client?.some(c => c.client_info?.android_client_info?.package_name === 'com.orderflow.autoresponder')) throw new Error('Android package mismatch.');
  return project;
}
module.exports = {appProject};
if (require.main === module) {
  const root = path.resolve(__dirname, '../../..');
  const args = process.argv.slice(2);
  const project = appProject(root, args.includes('--release') ? 'release' : 'debug');
  console.log(`Support target: ${project}; region: us-central1; route: chatzySupport`);
  const cliDefault = JSON.parse(fs.readFileSync(path.join(root, 'admin_web/.firebaserc'), 'utf8')).projects.default;
  if (cliDefault !== project) console.log(`CLI default ${cliDefault} differs; use the explicit support project.`);
  if (args.includes('--deploy')) {
    const explicit = args[args.indexOf('--project') + 1];
    if (!args.includes('--project') || explicit !== project) throw new Error(`Require --project ${project} to avoid deploying to the wrong Firebase project.`);
    const result = spawnSync(process.platform === 'win32' ? 'firebase.cmd' : 'firebase',
      ['deploy', '--only', 'functions:chatzySupport', '--project', project],
      {cwd: path.join(root, 'admin_web'), stdio: 'inherit', shell: process.platform === 'win32'});
    process.exit(result.status ?? 1);
  }
}
