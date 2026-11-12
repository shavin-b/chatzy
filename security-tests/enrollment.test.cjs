const {before,beforeEach,after,test}=require('node:test');
const assert=require('node:assert/strict');
const {createRequire}=require('node:module');
const requireFunctions=createRequire(require('node:path').resolve(__dirname,'../admin_web/functions/package.json'));
const {initializeApp,deleteApp}=requireFunctions('firebase-admin/app');
const {getFirestore,Timestamp}=requireFunctions('firebase-admin/firestore');
const {getAuth}=requireFunctions('firebase-admin/auth');
const {DeviceEnrollmentService}=requireFunctions('./lib/enrollment.js');
const {recordDeviceCommand,markDeviceCommandSent}=requireFunctions('./lib/commandAuthority.js');
let app,db,auth,service,a,b,ruleEnv;
const anon=uid=>({uid,anonymous:true});
before(async()=>{
  for(const key of ['FIRESTORE_EMULATOR_HOST','FIREBASE_AUTH_EMULATOR_HOST']) {
    if(!/^(127\.0\.0\.1|localhost):\d+$/.test(process.env[key]||'')) throw new Error('Local Auth and Firestore emulators required; production prohibited');
  }
  ruleEnv=await require('@firebase/rules-unit-testing').initializeTestEnvironment({projectId:'demo-chatzy-security',firestore:{rules:require('node:fs').readFileSync(require('node:path').join(__dirname,'firestore.staged.rules'),'utf8')}});
  app=initializeApp({projectId:'demo-chatzy-security'},'enrollment-tests');db=getFirestore(app);auth=getAuth(app);service=new DeviceEnrollmentService(db,auth);
});
beforeEach(async()=>{
  for(const name of ['admins','deviceBindings','deviceEnrollmentRequests','deviceEnrollments','devices','deviceCommands']) {
    const snapshot=await db.collection(name).get();if(!snapshot.empty){const batch=db.batch();snapshot.docs.forEach(d=>batch.delete(d.ref));await batch.commit();}
  }
  a=(await auth.createUser({})).uid;b=(await auth.createUser({})).uid;
  await db.doc('admins/root').set({role:'Super Admin'});await db.doc('admins/support').set({role:'Support'});await db.doc('admins/admin').set({role:'Admin'});
  await db.doc('devices/OF-EXISTING').set({deviceId:'OF-EXISTING',subscriptionEnd:Timestamp.fromMillis(5000000),subscriptionStatus:'ACTIVE',adminLocked:true,customLegacyField:'preserve-me'});
});
after(async()=>{if(app)await deleteApp(app);await ruleEnv?.cleanup();});
const request=()=>service.request(anon(a),'OF-EXISTING');
const rejectsCode=async(promise,code)=>assert.rejects(promise,error=>error.code===code);
test('anonymous UID cannot claim an ID without physical approval',async()=>{
  await request();await rejectsCode(service.complete(anon(a),'OF-EXISTING'),'failed-precondition');
  assert.equal((await auth.getUser(a)).customClaims?.device_id,undefined);
});
test('requests are idempotent and one UID cannot request another device',async()=>{
  const first=await request(),again=await request();assert.equal(first.requestId,again.requestId);
  await rejectsCode(service.request(anon(a),'OF-OTHER'),'permission-denied');
});
test('ordinary users, Support and Admin cannot approve pairing',async()=>{
  const pending=await request();
  for(const uid of ['ordinary','support','admin'])await rejectsCode(service.approve(uid,pending.requestId),'permission-denied');
});
test('Super Admin approval preserves every existing subscription and lock field',async()=>{
  const before=(await db.doc('devices/OF-EXISTING').get()).data();const pending=await request();
  await service.approve('root',pending.requestId);
  assert.deepEqual((await db.doc('devices/OF-EXISTING').get()).data(),before);
  await service.complete(anon(a),'OF-EXISTING');assert.equal((await auth.getUser(a)).customClaims.device_id,'OF-EXISTING');
});
test('approval is not a bearer enrollment code usable by another UID',async()=>{
  const pending=await request();await service.approve('root',pending.requestId);
  await rejectsCode(service.complete(anon(b),'OF-EXISTING'),'failed-precondition');
});
test('another installation cannot take over an already bound ID',async()=>{
  const pending=await request();await service.approve('root',pending.requestId);
  const attacker=await service.request(anon(b),'OF-EXISTING');
  await rejectsCode(service.approve('root',attacker.requestId),'already-exists');
  assert.equal((await db.doc('deviceBindings/OF-EXISTING').get()).data().authUid,a);
});
test('concurrent approvals have exactly one winner and cannot overwrite ownership',async()=>{
  const first=await request(),second=await service.request(anon(b),'OF-EXISTING');
  const results=await Promise.allSettled([service.approve('root',first.requestId),service.approve('root',second.requestId)]);
  assert.equal(results.filter(r=>r.status==='fulfilled').length,1);
  assert.ok([a,b].includes((await db.doc('deviceBindings/OF-EXISTING').get()).data().authUid));
});
test('expired pairing requests are rejected',async()=>{
  const pending=await request();await db.doc(`deviceEnrollmentRequests/${pending.requestId}`).update({expiresAt:0});
  await rejectsCode(service.approve('root',pending.requestId),'failed-precondition');
});
test('revoked bindings cannot obtain fresh claims or reactivate through approval',async()=>{
  const pending=await request();await service.approve('root',pending.requestId);
  await db.doc('deviceBindings/OF-EXISTING').update({active:false});
  await rejectsCode(service.complete(anon(a),'OF-EXISTING'),'failed-precondition');
});
test('new device is provisioned suspended without resetting existing ID format',async()=>{
  const pending=await service.request(anon(a),'OF-NEW');await service.approve('root',pending.requestId);
  const device=(await db.doc('devices/OF-NEW').get()).data();assert.equal(device.deviceId,'OF-NEW');assert.equal(device.subscriptionStatus,'SUSPENDED');assert.equal(device.subscriptionEnd,null);
});
test('enrollment rejects non-installation callers and invalid document paths',async()=>{
  await rejectsCode(service.request({uid:a,anonymous:false},'OF-EXISTING'),'permission-denied');
  await rejectsCode(service.request(anon(a),'OF-A/subcollection/evil'),'invalid-argument');
});
test('claim completion is idempotent and retains unrelated existing claims',async()=>{
  await auth.setCustomUserClaims(a,{testMarker:true});const pending=await request();await service.approve('root',pending.requestId);
  await service.complete(anon(a),'OF-EXISTING');await service.complete(anon(a),'OF-EXISTING');
  assert.deepEqual((await auth.getUser(a)).customClaims,{testMarker:true,device_id:'OF-EXISTING'});
});
test('server commands preserve subscriptions and own all lock aliases',async()=>{
  const record={deviceId:'OF-EXISTING',adminUid:'admin',command:'UNLOCK',status:'PENDING'};
  await recordDeviceCommand(db,'cmd',record);
  const device=(await db.doc('devices/OF-EXISTING').get()).data();assert.equal(device.subscriptionEnd.toMillis(),5000000);
  for(const field of ['adminLocked','adminLock','isLocked','locked'])assert.equal(device[field],false);
  assert.equal(device.desiredCommandId,'cmd');assert.equal(device.lockStatus,'UNLOCKED');
});
test('server denies non-management command creation',async()=>{
  await rejectsCode(recordDeviceCommand(db,'cmd',{deviceId:'OF-EXISTING',adminUid:'support',command:'LOCK',status:'PENDING'}),'permission-denied');
  assert.equal((await db.doc('deviceCommands/cmd').get()).exists,false);
});
test('FCM SENT update cannot overwrite an early successful acknowledgement',async()=>{
  await db.doc('deviceCommands/cmd').set({status:'SUCCESS'});
  await markDeviceCommandSent(db,'cmd',Timestamp.now());assert.equal((await db.doc('deviceCommands/cmd').get()).data().status,'SUCCESS');
});

test('real anonymous Auth token gains only approved scoped Firestore access and loses it on revocation',async()=>{
  const sdkApp=require('firebase/app'),sdkAuth=require('firebase/auth'),sdkFirestore=require('firebase/firestore');
  const client=sdkApp.initializeApp({projectId:'demo-chatzy-security',apiKey:'demo-emulator-only-key'},'integration-client');
  try {
    const identity=sdkAuth.getAuth(client);sdkAuth.connectAuthEmulator(identity,`http://${process.env.FIREBASE_AUTH_EMULATOR_HOST}`,{disableWarnings:true});
    const store=sdkFirestore.getFirestore(client);const [host,port]=process.env.FIRESTORE_EMULATOR_HOST.split(':');sdkFirestore.connectFirestoreEmulator(store,host,Number(port));
    const user=(await sdkAuth.signInAnonymously(identity)).user;
    const token=await user.getIdTokenResult(true);assert.equal(token.claims.aud,'demo-chatzy-security');const caller={uid:user.uid,anonymous:token.signInProvider==='anonymous'};
    await assert.rejects(sdkFirestore.getDocFromServer(sdkFirestore.doc(store,'devices','OF-EXISTING')),e=>e.code==='permission-denied');
    const pending=await service.request(caller,'OF-EXISTING');await service.approve('root',pending.requestId);await service.complete(caller,'OF-EXISTING');
    const approved=await user.getIdTokenResult(true);assert.equal(approved.claims.device_id,'OF-EXISTING');
    const ref=sdkFirestore.doc(store,'devices','OF-EXISTING');
    assert.equal((await sdkFirestore.getDocFromServer(ref)).data().customLegacyField,'preserve-me');
    await sdkFirestore.updateDoc(ref,{lastSeen:123,lastSync:123});
    await assert.rejects(sdkFirestore.updateDoc(ref,{subscriptionEnd:999}),e=>e.code==='permission-denied');
    await assert.rejects(sdkFirestore.updateDoc(ref,{adminLocked:false}),e=>e.code==='permission-denied');
    await db.doc('deviceBindings/OF-EXISTING').update({active:false});
    await assert.rejects(sdkFirestore.getDocFromServer(ref),e=>e.code==='permission-denied');
  } finally {await sdkApp.deleteApp(client);}
});
