const {test}=require('node:test');
const assert=require('node:assert/strict');
const {Timestamp,GeoPoint}=require('firebase-admin/firestore');
const {digest}=require('./firestore-manifest.cjs');
test('manifest hashes preserve timestamp precision, bytes and geopoints',()=>{
  assert.notEqual(digest(new Timestamp(1,2)),digest(new Timestamp(1,3)));
  assert.notEqual(digest(new Timestamp(1,0)),digest(1000));
  assert.notEqual(digest(Buffer.from([1,2])),digest([1,2]));
  assert.notEqual(digest(new GeoPoint(6.9,79.8)),digest({latitude:6.9,longitude:79.8}));
});
