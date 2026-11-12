import type { Env } from './types';

let cachedAccessToken: string | null = null;
let cachedTokenExpiry = 0;
let cachedCredentialIdentity = "";

// Base64Url encoder
function base64UrlEncode(data: Uint8Array | string): string {
  const bytes = typeof data === 'string' ? new TextEncoder().encode(data) : data;
  let base64 = btoa(String.fromCharCode(...bytes));
  return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

// Convert PKCS#8 PEM string to CryptoKey
async function importPrivateKey(pemKey: string): Promise<CryptoKey> {
  if (pemKey.startsWith('AIza') || !pemKey.includes('PRIVATE KEY')) {
    throw new Error(
      'FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY is set to an API key or placeholder instead of an RSA Private Key. Please download a Service Account JSON file from Firebase Console → Project Settings → Service Accounts, copy the "private_key" field, and add it to .dev.vars / Cloudflare Worker Secrets.'
    );
  }

  const cleanPem = pemKey
    .replace(/\\n/g, '')
    .replace(/[\r\n]/g, '')
    .replace(/-----BEGIN PRIVATE KEY-----/g, '')
    .replace(/-----END PRIVATE KEY-----/g, '')
    .replace(/[^A-Za-z0-9+/=]/g, '');

  let binaryDer: Uint8Array;
  try {
    binaryDer = Uint8Array.from(atob(cleanPem), (c) => c.charCodeAt(0));
  } catch {
    throw new Error(
      'Failed to base64-decode FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY. Ensure the private_key value from your Firebase Service Account JSON file is correctly pasted.'
    );
  }

  const keyBuffer = binaryDer.buffer.slice(
    binaryDer.byteOffset,
    binaryDer.byteOffset + binaryDer.byteLength
  );

  try {
    return await crypto.subtle.importKey(
      'pkcs8',
      keyBuffer as ArrayBuffer,
      { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
      false,
      ['sign']
    );
  } catch (err: any) {
    throw new Error(
      `Invalid PKCS8 Private Key: ${err?.message || err}. Ensure you used the full "private_key" string from Firebase Service Account JSON including BEGIN/END PRIVATE KEY.`
    );
  }
}

export async function getGoogleOAuthToken(env: Env): Promise<string> {
  const now = Math.floor(Date.now() / 1000);

  // Return cached token if valid for at least 5 more minutes
  const identity = `${env.FIREBASE_PROJECT_ID}:${env.FIREBASE_SERVICE_ACCOUNT_EMAIL}`;
  if (cachedAccessToken && cachedCredentialIdentity === identity && now < cachedTokenExpiry - 300) {
    return cachedAccessToken;
  }

  const clientEmail = env.FIREBASE_SERVICE_ACCOUNT_EMAIL;
  const privateKeyPem = env.FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY;

  if (!clientEmail || !privateKeyPem) {
    throw new Error('FIREBASE_SERVICE_ACCOUNT_EMAIL and FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY secrets must be configured.');
  }

  // Header & Claims
  const header = { alg: 'RS256', typ: 'JWT' };
  const claims = {
    iss: clientEmail,
    sub: clientEmail,
    aud: 'https://oauth2.googleapis.com/token',
    scope: 'https://www.googleapis.com/auth/cloud-platform https://www.googleapis.com/auth/firebase.messaging',
    iat: now,
    exp: now + 3600,
  };

  const headerB64 = base64UrlEncode(JSON.stringify(header));
  const claimsB64 = base64UrlEncode(JSON.stringify(claims));
  const unsignedToken = `${headerB64}.${claimsB64}`;

  // Sign with Private Key using Web Crypto API
  const key = await importPrivateKey(privateKeyPem);
  const signatureBuffer = await crypto.subtle.sign(
    'RSASSA-PKCS1-v1_5',
    key,
    new TextEncoder().encode(unsignedToken)
  );

  const signatureB64 = base64UrlEncode(new Uint8Array(signatureBuffer));
  const signedJwt = `${unsignedToken}.${signatureB64}`;

  // Exchange JWT for OAuth Access Token
  const tokenResponse = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: signedJwt,
    }),
  });

  if (!tokenResponse.ok) {
    console.error('[getGoogleOAuthToken] Token exchange failed; status:', tokenResponse.status);
    throw new Error(`Google OAuth token exchange failed: ${tokenResponse.status}`);
  }

  const tokenData = (await tokenResponse.json()) as { access_token: string; expires_in: number };
  cachedCredentialIdentity = identity;
  cachedAccessToken = tokenData.access_token;
  cachedTokenExpiry = now + tokenData.expires_in;

  return cachedAccessToken;
}
