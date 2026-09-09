import type { VerifiedFirebaseToken } from './types';

export interface GoogleJwk {
  kty: string;
  alg?: string;
  use?: string;
  kid: string;
  n: string;
  e: string;
}

interface GoogleJwksResponse {
  keys: GoogleJwk[];
}

// In-memory cache for Google JWKs to avoid fetching on every request
let googleJwkCache: { [kid: string]: GoogleJwk } | null = null;
let googleJwkExpiry = 0;

async function fetchGoogleJwks(): Promise<{ [kid: string]: GoogleJwk }> {
  const now = Date.now();
  if (googleJwkCache && now < googleJwkExpiry) {
    return googleJwkCache;
  }

  console.log('[auth] Fetching Google public signing keys (JWKS)...');

  const res = await fetch(
    'https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com'
  );

  if (!res.ok) {
    throw new Error(`Failed to fetch Google JWKS public keys: HTTP ${res.status}`);
  }

  // Parse Cache-Control max-age header
  const cacheControl = res.headers.get('cache-control') || '';
  const maxAgeMatch = cacheControl.match(/max-age=(\d+)/);
  const maxAgeSec = maxAgeMatch ? parseInt(maxAgeMatch[1], 10) : 3600;

  const data = (await res.json()) as GoogleJwksResponse;
  if (!data.keys || !Array.isArray(data.keys)) {
    throw new Error('Invalid Google JWKS response structure');
  }

  const keyMap: { [kid: string]: GoogleJwk } = {};
  for (const key of data.keys) {
    if (key.kid) {
      keyMap[key.kid] = key;
    }
  }

  googleJwkCache = keyMap;
  googleJwkExpiry = now + maxAgeSec * 1000;
  return keyMap;
}

// Convert Google JWK to Web Crypto CryptoKey
async function importJwkToCryptoKey(jwk: GoogleJwk): Promise<CryptoKey> {
  const webCryptoJwk = {
    kty: jwk.kty,
    alg: jwk.alg || 'RS256',
    use: jwk.use || 'sig',
    kid: jwk.kid,
    n: jwk.n,
    e: jwk.e,
    ext: false,
  } as JsonWebKey;

  return crypto.subtle.importKey(
    'jwk',
    webCryptoJwk,
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
    false,
    ['verify']
  );
}

// Base64Url decode helper
function base64UrlDecode(str: string): string {
  let base64 = str.replace(/-/g, '+').replace(/_/g, '/');
  while (base64.length % 4 !== 0) {
    base64 += '=';
  }
  return atob(base64);
}

export async function verifyFirebaseIdToken(
  idToken: string,
  projectId: string
): Promise<VerifiedFirebaseToken> {
  console.log('[auth] Verifying Firebase ID token...');

  const parts = idToken.split('.');
  if (parts.length !== 3) {
    throw new Error('Invalid JWT format: token must contain 3 parts');
  }

  const [headerB64, payloadB64, signatureB64] = parts;

  // Parse Header & Payload
  let header: { alg?: string; kid?: string };
  let payload: {
    iss?: string;
    aud?: string;
    sub?: string;
    user_id?: string;
    email?: string;
    exp?: number;
    iat?: number;
    auth_time?: number;
  };

  try {
    header = JSON.parse(base64UrlDecode(headerB64));
    payload = JSON.parse(base64UrlDecode(payloadB64));
  } catch {
    throw new Error('Failed to parse JWT header or payload JSON');
  }

  // 1. Check Algorithm
  if (header.alg !== 'RS256') {
    throw new Error(`Invalid JWT algorithm: expected RS256, got ${header.alg}`);
  }

  // 2. Check Key ID
  if (!header.kid || typeof header.kid !== 'string') {
    throw new Error('JWT missing key ID (kid)');
  }

  // 3. Check Expiration & Timestamps
  const nowSec = Math.floor(Date.now() / 1000);
  if (!payload.exp || typeof payload.exp !== 'number') {
    throw new Error('JWT payload missing expiration claim (exp)');
  }
  if (payload.exp <= nowSec) {
    throw new Error(`Firebase ID token is expired (exp: ${payload.exp}, now: ${nowSec})`);
  }

  // Allow up to 5 minutes clock skew for iat and auth_time
  const clockSkewSec = 300;
  if (payload.iat && typeof payload.iat === 'number' && payload.iat > nowSec + clockSkewSec) {
    throw new Error('Firebase ID token issued in the future');
  }
  if (payload.auth_time && typeof payload.auth_time === 'number' && payload.auth_time > nowSec + clockSkewSec) {
    throw new Error('Firebase ID token auth_time is in the future');
  }

  // 4. Check Issuer
  const expectedIssuer = `https://securetoken.google.com/${projectId}`;
  if (payload.iss !== expectedIssuer) {
    throw new Error(`Invalid token issuer: expected ${expectedIssuer}, got ${payload.iss}`);
  }

  // 5. Check Audience
  if (payload.aud !== projectId) {
    throw new Error(`Invalid token audience: expected ${projectId}, got ${payload.aud}`);
  }

  // 6. Check Subject (UID)
  const uid = payload.sub || payload.user_id;
  if (!uid || typeof uid !== 'string' || uid.trim() === '') {
    throw new Error('Token payload missing valid subject (sub/uid)');
  }

  // 7. Verify Signature against Google JWKS
  const jwkMap = await fetchGoogleJwks();
  const matchedJwk = jwkMap[header.kid];
  if (!matchedJwk) {
    throw new Error(`Google signing key not found for kid: ${header.kid}`);
  }

  console.log(`[auth] Matched signing key by kid: ${header.kid}`);

  // Import JWK to Web Crypto Key
  const cryptoKey = await importJwkToCryptoKey(matchedJwk);

  // Prepare signed data (header.payload)
  const dataToVerify = new TextEncoder().encode(`${headerB64}.${payloadB64}`);

  // Decode Base64Url signature
  const sigBinary = Uint8Array.from(
    atob(signatureB64.replace(/-/g, '+').replace(/_/g, '/')),
    (c) => c.charCodeAt(0)
  );

  const isValidSignature = await crypto.subtle.verify(
    'RSASSA-PKCS1-v1_5',
    cryptoKey,
    sigBinary,
    dataToVerify
  );

  if (!isValidSignature) {
    throw new Error('Firebase ID token signature verification failed');
  }

  console.log(`[auth] Firebase ID token verified for UID: ${uid}`);

  return {
    uid,
    email: payload.email || '',
    iss: payload.iss,
    aud: payload.aud,
    exp: payload.exp,
    iat: payload.iat || nowSec,
    auth_time: payload.auth_time || nowSec,
  };
}
