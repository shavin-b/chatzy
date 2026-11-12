import { readFile } from 'node:fs/promises';
import { before, beforeEach, after, test } from 'node:test';
import assert from 'node:assert/strict';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, getDocs, collection, setDoc, updateDoc, deleteDoc, addDoc, Timestamp } from 'firebase/firestore';
let env;
const profile = (id, role) => ({adminId:id,name:id,email:`${id}@example.invalid`,role,lastLogin:0});
before(async () => {
  if (!process.env.FIRESTORE_EMULATOR_HOST) throw new Error('Emulator required; production access prohibited');
  env = await initializeTestEnvironment({projectId:'demo-chatzy-security',firestore:{
    rules: await readFile(new URL('./firestore.staged.rules',import.meta.url),'utf8')
  }});
});
beforeEach(async()=>{
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async context=>{
    const db=context.firestore();
    for(const [id,role] of [['root','Super Admin'],['admin','Admin'],['support','Support'],['wrongcase','super admin']]) {
      await setDoc(doc(db,'admins',id),profile(id,role));
    }
    await setDoc(doc(db,'admins','missingrole'),{name:'missingrole'});
    await setDoc(doc(db,'deviceBindings','OF-A'),{authUid:'device-a',active:true});
    await setDoc(doc(db,'deviceBindings','OF-B'),{authUid:'device-b',active:true});
    await setDoc(doc(db,'devices','OF-A'),{deviceId:'OF-A',subscriptionEnd:42,subscriptionStatus:'ACTIVE',status:'Active',adminLocked:true,lockStatus:'LOCKED',lockReason:'ADMIN',fcmToken:'test-only-token'});
    await setDoc(doc(db,'devices','OF-B'),{deviceId:'OF-B',subscriptionEnd:null,status:'Active'});
    await setDoc(doc(db,'deviceCommands','cmd'),{commandId:'cmd',deviceId:'OF-A',command:'LOCK',status:'SENT'});
    await setDoc(doc(db,'settings','global'),{companyName:'SHAVIN SOLUTIONS (PVT) LTD'});
    await setDoc(doc(db,'logs','existing'),{logId:'existing',timestamp:100,performedBy:'admin@example.invalid',description:'immutable'});
    await setDoc(doc(db,'subscriptions','sub'),{deviceId:'OF-A'});
    await setDoc(doc(db,'notifications','notice'),{title:'test'});
  });
});
after(async()=>{await env?.cleanup();});
const dbFor=(uid,token={})=>env.authenticatedContext(uid,{email:`${uid}@example.invalid`,...token}).firestore();
const device=()=>dbFor('device-a',{device_id:'OF-A'});
const sub=(actor='admin')=>({subscriptionId:'new-sub',deviceId:'OF-A',businessName:'test',updatedBy:`${actor}@example.invalid`,updatedAt:100,oldExpiryDate:42,newExpiryDate:999,daysAdded:30,actionType:'Extend'});

test('anonymous clients cannot read admin-sensitive collections',async()=>{
  const db=env.unauthenticatedContext().firestore();
  for(const [c,id] of [['admins','root'],['devices','OF-A'],['deviceCommands','cmd'],['settings','global'],['logs','existing'],['subscriptions','sub'],['notifications','notice']]) {
    await assertFails(getDoc(doc(db,c,id)));
  }
});
test('ordinary authenticated users cannot read admin-sensitive data or list admins',async()=>{
  const db=dbFor('ordinary');
  for(const [c,id] of [['admins','root'],['devices','OF-A'],['deviceCommands','cmd'],['settings','global'],['logs','existing'],['subscriptions','sub'],['notifications','notice']]) await assertFails(getDoc(doc(db,c,id)));
  await assertFails(getDocs(collection(db,'admins')));
});

test('sign-in without an approved device binding cannot create dashboard presence',async()=>{
  const db=dbFor('new-installation');
  await assertFails(setDoc(doc(db,'devices','OF-NEW'),{deviceId:'OF-NEW',lastSeen:Date.now()}));
});
test('ordinary users cannot bootstrap themselves or someone else as admin',async()=>{
  const db=dbFor('ordinary');
  await assertFails(setDoc(doc(db,'admins','ordinary'),profile('ordinary','Super Admin')));
  await assertFails(setDoc(doc(db,'admins','another'),profile('another','Admin')));
  await assertFails(updateDoc(doc(db,'admins','root'),{role:'Support'}));
});
test('missing and incorrectly cased roles confer no access',async()=>{
  for(const uid of ['wrongcase','missingrole']) {
    await assertFails(getDoc(doc(dbFor(uid),'devices','OF-A')));
    await assertFails(updateDoc(doc(dbFor(uid),'admins',uid),{lastLogin:100}));
  }
});
test('recognized admins can read dashboard data and list users',async()=>{
  for(const uid of ['root','admin','support']) {
    await assertSucceeds(getDoc(doc(dbFor(uid),'devices','OF-A')));
    await assertSucceeds(getDocs(collection(dbFor(uid),'admins')));
  }
});
test('exact Super Admin can create valid admin profiles',async()=>{
  await assertSucceeds(setDoc(doc(dbFor('root'),'admins','new'),profile('new','Admin')));
});
test('Super Admin cannot create mismatched IDs or invalid roles',async()=>{
  await assertFails(setDoc(doc(dbFor('root'),'admins','new'),profile('different','Admin')));
  await assertFails(setDoc(doc(dbFor('root'),'admins','new'),profile('new','Owner')));
});
test('Admin and Support cannot grant roles or promote themselves',async()=>{
  for(const uid of ['admin','support']) {
    await assertFails(updateDoc(doc(dbFor(uid),'admins',uid),{role:'Super Admin'}));
    await assertFails(setDoc(doc(dbFor(uid),'admins','new'),profile('new','Admin')));
  }
});
test('authorized admins can update only their own lastLogin without role escalation',async()=>{
  for(const uid of ['root','admin','support']) await assertSucceeds(updateDoc(doc(dbFor(uid),'admins',uid),{lastLogin:100}));
  await assertFails(updateDoc(doc(dbFor('support'),'admins','support'),{lastLogin:101,role:'Super Admin'}));
  await assertFails(updateDoc(doc(dbFor('admin'),'admins','support'),{lastLogin:101}));
  await assertFails(updateDoc(doc(dbFor('support'),'admins','support'),{lastLogin:'invalid'}));
});
test('Super Admin cannot accidentally delete or demote their own profile',async()=>{
  await assertFails(deleteDoc(doc(dbFor('root'),'admins','root')));
  await assertFails(updateDoc(doc(dbFor('root'),'admins','root'),{role:'Support'}));
  await assertSucceeds(updateDoc(doc(dbFor('root'),'admins','support'),{role:'Admin'}));
});
test('ordinary and Support users cannot edit subscriptions or create history',async()=>{
  for(const uid of ['ordinary','support']) {
    await assertFails(updateDoc(doc(dbFor(uid),'devices','OF-A'),{subscriptionEnd:999,status:'Active',lastSync:100}));
    await assertFails(setDoc(doc(dbFor(uid),'subscriptions','new-sub'),sub(uid)));
  }
});
test('existing Admin subscription update payload including lastSync is allowed',async()=>{
  await assertSucceeds(updateDoc(doc(dbFor('admin'),'devices','OF-A'),{subscriptionEnd:999,status:'Active',lastSync:100}));
  await assertSucceeds(updateDoc(doc(dbFor('admin'),'devices','OF-B'),{status:'Suspended',lastSync:100}));
  await assertSucceeds(updateDoc(doc(dbFor('admin'),'devices','OF-A'),{subscriptionEnd:Timestamp.fromMillis(999)}));
  await assertFails(updateDoc(doc(dbFor('admin'),'devices','OF-A'),{subscriptionEnd:'tomorrow'}));
});
test('Admin can create history only under their own identity',async()=>{
  await assertSucceeds(setDoc(doc(dbFor('admin'),'subscriptions','new-sub'),sub()));
  await assertFails(setDoc(doc(dbFor('admin'),'subscriptions','other'),{...sub('root'),subscriptionId:'other'}));
});
test('devices can read themselves and heartbeat but not another device',async()=>{
  await assertSucceeds(getDoc(doc(device(),'devices','OF-A')));
  await assertSucceeds(updateDoc(doc(device(),'devices','OF-A'),{lastSeen:100,lastSync:100,fcmToken:'renewed-test-token'}));
  await assertFails(getDoc(doc(device(),'devices','OF-B')));
  await assertFails(updateDoc(doc(device(),'devices','OF-B'),{lastSeen:100}));
});
test('full heartbeat preserves authoritative lock and expiry; stale local locks are denied',async()=>{
  await assertSucceeds(updateDoc(doc(device(),'devices','OF-A'),{lastSeen:100,lastSync:100,adminLocked:true,lockStatus:'LOCKED',lockReason:'ADMIN',fcmToken:'renewed-test-token'}));
  assert.equal((await getDoc(doc(device(),'devices','OF-A'))).data().subscriptionEnd,42);
  await assertFails(updateDoc(doc(device(),'devices','OF-A'),{lastSeen:101,adminLocked:false,lockStatus:'UNLOCKED',lockReason:'NONE'}));
});
test('authenticated devices cannot change subscription or lock authority',async()=>{
  for(const patch of [{subscriptionEnd:999},{subscriptionStatus:'ACTIVE'},{status:'Active'},{adminLocked:false},{deviceId:'OF-B'},{adminLock:false},{isLocked:false}]) {
    // Ensure protected fields actually change rather than reassigning their current value.
    const updates={...patch};if('subscriptionStatus' in updates)updates.subscriptionStatus='EXPIRED';if('status' in updates)updates.status='Suspended';
    await assertFails(updateDoc(doc(device(),'devices','OF-A'),updates));
  }
});
test('client registration cannot bypass trusted enrollment even with a device claim',async()=>{
  await assertFails(setDoc(doc(dbFor('device-new',{device_id:'OF-NEW'}),'devices','OF-NEW'),{deviceId:'OF-NEW',lastSeen:100}));
});
test('ordinary user email or self-declared role does not confer device authority',async()=>{
  await assertFails(updateDoc(doc(dbFor('ordinary',{role:'Super Admin',admin:true}),'devices','OF-A'),{lastSeen:100}));
});
test('legacy unauthenticated registration and heartbeat are explicitly incompatible',async()=>{
  const db=env.unauthenticatedContext().firestore();
  await assertFails(setDoc(doc(db,'devices','OF-NEW'),{deviceId:'OF-NEW',subscriptionStatus:'ACTIVE'}));
  await assertFails(updateDoc(doc(db,'devices','OF-A'),{lastSeen:100}));
});
test('existing Android registration payload contains forbidden subscription state',async()=>{
  await assertFails(setDoc(doc(device(),'devices','OF-A'),{deviceId:'OF-A',lastSeen:100,subscriptionStatus:'SUSPENDED'},{merge:true}));
});
test('command acknowledgement preserves unchanged Android identity fields',async()=>{
  await assertSucceeds(setDoc(doc(device(),'deviceCommands','cmd'),{commandId:'cmd',deviceId:'OF-A',command:'LOCK',status:'SUCCESS',processedAt:100},{merge:true}));
});
test('another device cannot acknowledge commands or alter command payloads',async()=>{
  await assertFails(updateDoc(doc(dbFor('device-b',{device_id:'OF-B'}),'deviceCommands','cmd'),{status:'SUCCESS',processedAt:100}));
  await assertFails(updateDoc(doc(device(),'deviceCommands','cmd'),{command:'UNLOCK',status:'SUCCESS',processedAt:100}));
  await assertFails(updateDoc(doc(device(),'deviceCommands','cmd'),{status:'SENT',processedAt:100}));
});
test('missing command records cannot be created by acknowledgement clients',async()=>{
  await assertFails(setDoc(doc(device(),'deviceCommands','missing'),{deviceId:'OF-A',command:'LOCK',status:'SUCCESS',processedAt:100}));
});
test('all client roles including Super Admin cannot create remote commands',async()=>{
  for(const uid of ['ordinary','support','admin','root']) await assertFails(setDoc(doc(dbFor(uid),'deviceCommands','forged'),{deviceId:'OF-A',command:'LOCK'}));
});
test('existing browser log creation and one-time logId binding are allowed',async()=>{
  const db=dbFor('admin');
  const ref=await assertSucceeds(addDoc(collection(db,'logs'),{logId:'',timestamp:100,performedBy:'admin@example.invalid',title:'Subscription Extended',description:'test'}));
  await assertSucceeds(setDoc(ref,{logId:ref.id},{merge:true}));
  await assertFails(updateDoc(ref,{description:'tampered'}));
  await assertFails(updateDoc(ref,{logId:'other'}));
  assert.equal((await getDoc(ref)).data().description,'test');
});
test('logs cannot impersonate another admin and Support cannot write logs',async()=>{
  await assertFails(setDoc(doc(dbFor('admin'),'logs','new'),{logId:'new',timestamp:100,performedBy:'root@example.invalid'}));
  await assertFails(setDoc(doc(dbFor('support'),'logs','new'),{logId:'new',timestamp:100,performedBy:'support@example.invalid'}));
  await assertFails(deleteDoc(doc(dbFor('root'),'logs','existing')));
});
test('settings are writable only by Super Admin',async()=>{
  for(const uid of ['ordinary','admin','support']) await assertFails(setDoc(doc(dbFor(uid),'settings','global'),{companyName:'test'}));
  await assertSucceeds(setDoc(doc(dbFor('root'),'settings','global'),{companyName:'SHAVIN SOLUTIONS (PVT) LTD'}));
});
test('Support cannot mutate notifications; Admin can create announcements',async()=>{
  await assertFails(setDoc(doc(dbFor('support'),'notifications','new'),{title:'test'}));
  await assertSucceeds(setDoc(doc(dbFor('admin'),'notifications','new'),{title:'test'}));
});
test('unknown collections and nested paths are denied',async()=>{
  await assertFails(setDoc(doc(dbFor('root'),'unrecognized','new'),{value:1}));
  await assertFails(setDoc(doc(device(),'devices','OF-A','private','new'),{value:1}));
});

test('stale or forged scoped claims cannot bypass live device bindings',async()=>{
  await assertFails(getDoc(doc(dbFor('other',{device_id:'OF-A'}),'devices','OF-A')));
  await env.withSecurityRulesDisabled(async context=>{await updateDoc(doc(context.firestore(),'deviceBindings','OF-A'),{active:false});});
  await assertFails(getDoc(doc(device(),'devices','OF-A')));
  await assertFails(updateDoc(doc(device(),'devices','OF-A'),{lastSeen:999}));
});
test('clients cannot create or edit enrollment authority records',async()=>{
  for(const uid of ['ordinary','root','device-a']) {
    await assertFails(setDoc(doc(dbFor(uid),'deviceBindings','OF-A'),{authUid:uid,active:true}));
    await assertFails(setDoc(doc(dbFor(uid),'deviceEnrollments',uid),{state:'APPROVED'}));
    await assertFails(setDoc(doc(dbFor(uid),'deviceEnrollmentRequests','forged'),{state:'APPROVED'}));
  }
});
