/* Read-only Firestore validation. Never imports, writes, or deletes remote data. */
const fs = require('node:fs');
const crypto = require('node:crypto');
const { initializeApp, applicationDefault, deleteApp } = require('firebase-admin/app');
const { getFirestore, Timestamp, GeoPoint, DocumentReference, FieldPath } = require('firebase-admin/firestore');
function canonical(value) {
  if (value === null) return null;
  if (value instanceof Timestamp) return { type:'timestamp', seconds:value.seconds, nanoseconds:value.nanoseconds };
  if (value instanceof GeoPoint) return { type:'geopoint', latitude:value.latitude, longitude:value.longitude };
  if (value instanceof DocumentReference) return { type:'reference', projectId:value.firestore.projectId, path:value.path };
  if (Buffer.isBuffer(value)) return { type:'bytes', value:value.toString('base64') };
  if (Array.isArray(value)) return value.map(canonical);
  if (typeof value === 'number' && !Number.isFinite(value)) return {type:'number',value:String(value)};
  if (typeof value === 'object') return Object.fromEntries(Object.keys(value).sort().map(k=>[k,canonical(value[k])]));
  return value;
}
const digest = value => crypto.createHash('sha256').update(JSON.stringify(canonical(value))).digest('hex');
function compare(a,b) {
  const keys=new Set([...Object.keys(a.documents),...Object.keys(b.documents)]);
  const mismatches=[...keys].filter(k=>JSON.stringify(a.documents[k])!==JSON.stringify(b.documents[k]));
  const countMatches=JSON.stringify(canonical(a.counts))===JSON.stringify(canonical(b.counts));
  return { equal:countMatches && mismatches.length===0, documentCount:keys.size, mismatches:mismatches.length, countMatches };
}
async function snapshot(projectId, output) {
  if (!projectId || !output || projectId.includes('<')) throw new Error('Supply a genuine --project and --output');
  const app=initializeApp({projectId,credential:applicationDefault()});
  try {
    const db=getFirestore(app), documents={}, counts={};
    async function scan(collection) {
      counts[collection.path]=0;
      let last;
      do {
        let query=collection.orderBy(FieldPath.documentId()).limit(500);
        if(last) query=query.startAfter(last);
        const page=await query.get();
        for(const doc of page.docs) {
          const data=doc.data();
          documents[doc.ref.path]=Object.fromEntries(Object.keys(data).sort().map(field=>[field,digest(data[field])]));
          counts[collection.path]++;
          for(const child of await doc.ref.listCollections()) await scan(child);
        }
        last=page.size===500 ? page.docs.at(-1) : undefined;
      } while(last);
    }
    const existing=await db.listCollections();
    const roots=new Map(existing.map(c=>[c.id,c]));
    for(const id of ['devices','deviceCommands','admins','subscriptions','logs','notifications','settings']) {
      if(!roots.has(id)) roots.set(id,db.collection(id));
    }
    for(const collection of roots.values()) await scan(collection);
    const manifest={projectId,createdAt:new Date().toISOString(),counts,documents};
    fs.writeFileSync(output,JSON.stringify(manifest,null,2),{flag:'wx',mode:0o600});
    console.log('Read-only manifest created:',Object.keys(documents).length,'documents. No field values printed.');
  } finally { await deleteApp(app); }
}
async function main(args) {
  if(args[0]==='--compare') {
    const result=compare(JSON.parse(fs.readFileSync(args[1],'utf8')),JSON.parse(fs.readFileSync(args[2],'utf8')));
    console.log(JSON.stringify(result));
    if(!result.equal) process.exitCode=1;
  } else {
    if (!args.includes('--project') || !args.includes('--output')) throw new Error('Explicit project/output flags required');
    await snapshot(args[args.indexOf('--project')+1],args[args.indexOf('--output')+1]);
  }
}
module.exports={canonical,digest,compare};
if(require.main===module) main(process.argv.slice(2)).catch(()=>{console.error('Manifest failed. Check explicit project, ADC read permissions, and new output path; no remote writes performed.');process.exitCode=1;});
