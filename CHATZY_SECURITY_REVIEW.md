> Latest authorization review: CHATZY_FIRESTORE_RULES_REVIEW.md. Local callable command policy and website role validation are now hardened; deployment remains unchanged. The notes below include earlier audit findings.

# Chatzy security review and staged rollout

Status: NOT approved for commercial production. No existing live rules were tightened during branding. No service-account key was printed, copied into documentation, used for a remote request or rotated automatically.

## Immediate operator action
The private key supplied with the project must be treated as compromised. Revoke that specific IAM key, issue replacement least-privilege credentials, update Worker secrets, inspect Cloud Audit Logs for misuse and audit historical Git/archive exposure. A .gitignore change does not revoke a key or erase historical copies. Existing local .dev.vars and .env.local remain untouched for compatibility, and must be replaced by the owner with newly rotated values. Do not build production with the supplied key.

Git ignores cover local env/vars, service-account patterns, PEM/key/keystore and token text files, node_modules, Wrangler state and generated output. Example env files are intentionally tracked and contain blank settings. Android/Web Firebase client configuration is public project configuration, not administrative authority. Service-account credentials are server-only. Request-body logging is disabled for Android Meta networking; Worker OAuth/FCM error logs no longer print raw response bodies. Never log access tokens, private keys or FCM tokens. Existing local debug/message diagnostics still need a separate retention/privacy review.

## Existing rule differences and risks
| File | Existing behavior | Consequence |
|---|---|---|
| root firestore.rules | device and command read/create/update gated only by nonempty document ID | unauthenticated data access/tampering; no admin collection coverage |
| admin_web/firestore.rules | devices public read/create/update, commands admin read/create but public update, logs public create; role-based other collections | subscription/token tampering, forged acks/audit records |
| ADMIN app/firestore.rules | devices public read/create/update, logs public create; no deviceCommands match | same device exposure plus potential command permission failures if deployed |

Only admin_web/firebase.json currently points to admin_web/firestore.rules. Do not assume all three files are active in a live project. Determine the actually deployed rules in Console. No rule file was silently replaced or merged.

## Staged least-privilege target
security-tests/firestore.staged.rules is deliberately isolated, NOT referenced by production firebase.json. It denies unauthenticated access, scopes device reads/heartbeat/acks to a trusted device_id custom claim, prevents subscription/lock changes by devices, makes command creation server-only, protects admin roles/settings, and restricts Support to reads. It provisions devices through a server enrollment flow that is NOT implemented in this branding release. The baseline Android registerDevice writes administrative default fields, so simply enabling Auth or deploying these rules would break registration/heartbeat behavior.

1. First build and test server-owned enrollment/device identity binding. Anonymous Firebase Auth alone is insufficient: anyone could otherwise claim an existing OF- ID. Require an authenticated owner/admin-approved enrollment or another reviewed proof; issue device_id custom claims server-side. Preserve installed OF- IDs and bind once. App Check is additional attestation/rate-abuse protection, not user authorization or a replacement for claims.
2. Adapt a future client registration write contract: server provisions protected subscription/lock fields, device writes only allowed telemetry and its FCM token. DeviceManager currently writes lock aliases during registration and heartbeat; reconcile all aliases with server-authoritative ownership before enabling staged rules. Preserve background retries and command acks.
3. Test both legacy and authenticated clients in separate emulator projects. Stage App Check monitoring before enforcement and provide supported recovery. Never enable enforcement based only on build success.
4. Ship authenticated-capable clients against a compatibility environment with telemetry monitoring. Handle the separate Android admin app as an explicit cohort. Upgrade claims/token refresh and backend authorization must be verified before narrowing rules.
5. Enforce the staged target only in isolated Chatzy environment after all compatibility tests, pilot enrollment, role/field tests and explicit deployment approval. Never leave global unauthenticated fallback writes beside the strict rules; those would bypass the protection.
6. Run security-tests npm ci && npm test; tests use demo-chatzy-security and the local emulator only. Actual results are recorded separately.

## Retained risks requiring separate work
- Callable Functions currently accepts Support for remote commands, while the Worker restricts commands to Admin/Super Admin. Align the two server-side role policies in the staged rollout and test both entry points; Firestore rules cannot constrain Admin SDK authority. This existing authorization behavior was not silently changed by branding.
- Hardcoded incoming-message activation/suspension controls lack authenticated sender authorization; early automation guard prevents activation while blocked. Preserve behavior for this migration, but remove or replace through an authorized admin-channel rollout before commercial release.
- Partial reply sequences are only deduplicated after complete success; retry can duplicate earlier replies. No engine rewrite was performed.
- Meta credentials remain in sandboxed SharedPreferences, not encrypted by SecureStorage. Keystore-backed storage needs a tested credential migration rather than renaming preference files.
- Room schema 6 lacks complete historical migration coverage; destructive fallback removed to protect data. Older installs require verified schema recovery/migrations before rollout.
- Auth observers/error recovery, immutable audit log writes, subscription transactionality and FCM acknowledgement races are existing concerns; branding does not prove correctness.
- Firebase Analytics dependency is present. The merged debug/release manifests contain no explicit Analytics deactivation flag. Verify SDK defaults, actual telemetry and consent/legal disclosure. No assertion of no analytics is made.

## Policy review (2026-11-09)
Reviewed official Google Play developer policy/permissions and WhatsApp business terms/messaging guidelines. Prominent notification disclosure/consent, accurate data safety/privacy policy and foreground-service justification remain release gates. Local RemoteInput automation is not automatically approved because it works technically; WhatsApp guidelines address unofficial/impermissible automation and account abuse. Obtain a product-specific assessment before marketing compliance. No Play or WhatsApp approval is claimed.
- https://support.google.com/googleplay/android-developer/answer/18265346
- https://support.google.com/googleplay/android-developer/answer/16558241
- https://www.whatsapp.com/legal/messaging-guidelines
- https://www.whatsapp.com/legal/terms-of-service
- https://firebase.google.com/docs/rules/unit-tests
