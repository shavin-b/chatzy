const {test}=require('node:test');
const assert=require('node:assert/strict');
const {digest,compare}=require('./firestore-manifest.cjs');
test('field digests ignore map order but preserve values and array order',()=>{
  assert.equal(digest({a:1,b:2}),digest({b:2,a:1}));
  assert.notEqual(digest([1,2]),digest([2,1]));
  assert.notEqual(digest(123),digest('123'));
});
test('comparison catches expiry changes and missing devices',()=>{
  const source={counts:{devices:1},documents:{'devices/OF-A':{subscriptionEnd:digest(123)}}};
  assert.equal(compare(source,structuredClone(source)).equal,true);
  const changed=structuredClone(source);changed.documents['devices/OF-A'].subscriptionEnd=digest(124);
  assert.equal(compare(source,changed).equal,false);
  assert.equal(compare(source,{counts:{devices:0},documents:{}}).equal,false);
});
