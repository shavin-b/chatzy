import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import ts from 'typescript';

/** Inspect only public configuration; never emit keys or full bundle excerpts. */
export function analyzeBundle(source) {
  const ast = ts.createSourceFile('dashboard.js', source, ts.ScriptTarget.Latest, true, ts.ScriptKind.JS);
  const projects = new Set();
  const missingFirebaseFields = new Set();
  let commandConfigurationMissing = false;
  const propertyName = node => ts.isComputedPropertyName(node) ? node.expression.text : node.text;
  const unconditionalThrow = block => block?.statements?.some(statement =>
    ts.isThrowStatement(statement) || ts.isBlock(statement) && unconditionalThrow(statement));
  function visit(node) {
    if (ts.isObjectLiteralExpression(node)) {
      const properties = node.properties.filter(ts.isPropertyAssignment);
      const names = properties.map(p => propertyName(p.name));
      if (names.includes('apiKey') && names.includes('authDomain') && names.includes('projectId')) {
        for (const p of properties) {
          if (ts.isVoidExpression(p.initializer) || ts.isIdentifier(p.initializer) && p.initializer.text === 'undefined' ||
              ts.isStringLiteralLike(p.initializer) && !p.initializer.text.trim()) {
            missingFirebaseFields.add(propertyName(p.name));
          }
        }
      }
    }
    if (ts.isPropertyAssignment(node) && propertyName(node.name) === 'projectId' && ts.isStringLiteralLike(node.initializer)) {
      projects.add(node.initializer.text);
    }
    if (ts.isStringLiteralLike(node) && node.text === 'VITE_COMMAND_API_BASE_URL is not configured') {
      let fn = node.parent;
      while (fn && !ts.isFunctionLike(fn)) fn = fn.parent;
      if (fn && unconditionalThrow(fn.body)) commandConfigurationMissing = true;
    }
    ts.forEachChild(node, visit);
  }
  visit(ast);
  return { projects: [...projects], missingFirebaseFields: [...missingFirebaseFields], commandConfigurationMissing };
}

export async function checkDashboard(url, expectedProject) {
  const target = new URL(url);
  if (target.protocol !== 'https:' || target.username || target.password || target.search) throw Error('Use the public HTTPS dashboard URL without credentials.');
  const fetchPublic = url => fetch(url, {signal: AbortSignal.timeout(20_000)});
  const response = await fetchPublic(target);
  if (!response.ok) throw Error(`Dashboard HTTP ${response.status}`);
  const html = await response.text();
  const result = { origin: target.origin, httpStatus: response.status, assets: [], projects: [], missingFirebaseFields: [], commandConfigurationMissing: false };
  for (const match of html.matchAll(/(?:src|href)=["']([^"']+\.js)["']/g)) {
    const asset = new URL(match[1], target);
    if (asset.origin !== target.origin) continue;
    const r = await fetchPublic(asset);
    result.assets.push({path: asset.pathname, status: r.status});
    if (!r.ok) continue;
    const config = analyzeBundle(await r.text());
    result.projects.push(...config.projects);
    result.missingFirebaseFields.push(...config.missingFirebaseFields);
    result.commandConfigurationMissing ||= config.commandConfigurationMissing;
  }
  result.projects = [...new Set(result.projects)];
  result.missingFirebaseFields = [...new Set(result.missingFirebaseFields)];
  result.projectMatchesAndroid = result.projects.length === 1 && result.projects[0] === expectedProject;
  result.passed = result.assets.length > 0 && result.assets.every(a => a.status === 200) && result.projectMatchesAndroid && result.missingFirebaseFields.length === 0 && !result.commandConfigurationMissing;
  return result;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const workspace = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
    const configs = ['app/src/debug/google-services.json', 'app/google-services.json'];
    const config = configs.map(p => path.join(workspace, p)).find(p => fs.existsSync(p));
    if (!config) throw Error('Android Firebase configuration is missing.');
    const expectedProject = JSON.parse(fs.readFileSync(config, 'utf8')).project_info.project_id;
    const result = await checkDashboard(process.argv[2] || 'https://chatzy-admin-console.vercel.app/', expectedProject);
    console.log(JSON.stringify(result, null, 2));
    if (!result.passed) process.exitCode = 1;
  } catch (error) {
    console.error(`Connection check failed: ${error.name}`);
    process.exitCode = 1;
  }
}
