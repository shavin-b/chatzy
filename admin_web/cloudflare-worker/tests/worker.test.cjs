const {before,after,test}=require('node:test');
const assert=require('node:assert/strict');
const {build}=require('esbuild');
const {mkdtemp,rm}=require('node:fs/promises');
const {tmpdir}=require('node:os');
const {join}=require('node:path');
const {pathToFileURL}=require('node:url');
let worker,dir,originalFetch;
const bindings={FIREBASE_PROJECT_ID:'demo-chatzy-test',FIREBASE_SERVICE_ACCOUNT_EMAIL:'test@example.invalid',FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY:'test-only-not-a-key',ALLOWED_ORIGINS:'http://localhost:5173'};
before(async()=>{
  dir=await mkdtemp(join(tmpdir(),'chatzy-worker-test-'));
  const outfile=join(dir,'worker.mjs');
  await build({entryPoints:['src/index.ts'],bundle:true,platform:'browser',format:'esm',outfile});
  worker=(await import(pathToFileURL(outfile))).default;
  originalFetch=global.fetch;
  global.fetch=()=>{throw new Error('Network prohibited in Worker unit tests');};
});
after(async()=>{global.fetch=originalFetch;await rm(dir,{recursive:true,force:true});});
const request=(headers={})=>new Request('https://unit-test.invalid/api/device-command',{method:'POST',headers});
test('missing project fails closed with a configuration error and no secret values',async()=>{
  const result=await worker.fetch(request(),{...bindings,FIREBASE_PROJECT_ID:''},{});
  assert.equal(result.status,503);
  const body=await result.json();assert.equal(body.error,'CONFIG_ERROR');
  assert.equal(JSON.stringify(body).includes(bindings.FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY),false);
});
test('missing credentials cannot be hidden by the health endpoint',async()=>{
  const result=await worker.fetch(new Request('https://unit-test.invalid/health'),{},{});
  assert.equal(result.status,503);
});
test('configured endpoint still requires authentication',async()=>{
  const result=await worker.fetch(request(),bindings,{});assert.equal(result.status,401);
});
test('malformed bearer token cannot reach Firestore or FCM',async()=>{
  const result=await worker.fetch(request({Authorization:'Bearer malformed'}),bindings,{});assert.equal(result.status,401);
});
test('preflight remains supported for the configured admin origin',async()=>{
  const result=await worker.fetch(new Request('https://unit-test.invalid/api/device-command',{method:'OPTIONS',headers:{Origin:'http://localhost:5173'}}),bindings,{});
  assert.equal(result.status,204);assert.equal(result.headers.get('Access-Control-Allow-Origin'),'http://localhost:5173');
});
test('Chatzy deployment matches the Android project and accepts the live dashboard origin',async()=>{
  const fs=require('node:fs');
  const config=JSON.parse(fs.readFileSync('wrangler.chatzy.jsonc','utf8'));
  const android=JSON.parse(fs.readFileSync('../../app/google-services.json','utf8'));
  assert.equal(config.vars.FIREBASE_PROJECT_ID,android.project_info.project_id);
  const origin='https://chatzy-admin-console.vercel.app';
  assert.ok(config.vars.ALLOWED_ORIGINS.split(',').includes(origin));
  const result=await worker.fetch(new Request('https://unit-test.invalid/api/device-command',{method:'OPTIONS',headers:{Origin:origin}}),{...bindings,...config.vars},{});
  assert.equal(result.status,204);
  assert.equal(result.headers.get('Access-Control-Allow-Origin'),origin);
});
