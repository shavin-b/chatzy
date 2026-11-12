// Exercise the actual website auth service in a VM with Firebase reads mocked.
// No Firebase SDK initialization, credentials, or network access are available here.
const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
const ts=require('typescript');
const source=fs.readFileSync(path.resolve(__dirname,'../../src/services/auth.ts'),'utf8');
function service(data,exists=true) {
  const exports={};
  const js=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText;
  vm.runInNewContext(js,{exports,require(name){
    if(name==='firebase/auth')return {};
    if(name==='firebase/firestore')return {
      doc:(_db,_collection,uid)=>({uid}),
      getDoc:async ref=>({id:ref.uid,exists:()=>exists,data:()=>data})
    };
    if(name==='./firebase')return {db:{},auth:{}};
    throw new Error(`Unexpected import prohibited: ${name}`);
  }});
  return exports;
}
test('actual website auth service rejects missing, invalid and malformed roles',async()=>{
  for(const role of [undefined,null,'','super admin','Super Admin ','Owner',true,{},['Admin']]) {
    assert.equal(await service({role}).getAdminProfile('uid'),null);
  }
});
test('actual website auth service accepts only exact known roles and uses document UID',async()=>{
  for(const role of ['Super Admin','Admin','Support']) {
    const profile=await service({role,adminId:'untrusted-other-id',email:'test@example.invalid',name:'test',lastLogin:0}).getAdminProfile('actual-uid');
    assert.equal(profile.role,role);assert.equal(profile.adminId,'actual-uid');
  }
});
test('actual website auth service denies a missing admin document',async()=>{
  assert.equal(await service({},false).getAdminProfile('ordinary'),null);
});
