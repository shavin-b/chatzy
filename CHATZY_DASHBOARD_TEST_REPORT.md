# Chatzy dashboard connection and test report

## Result

The live dashboard at https://chatzy-admin-console.vercel.app/ is online and uses
Firebase project **chatzy-d8ba4**, matching Android application
**com.orderflow.autoresponder**. The full production connection is **not working**
and cannot be marked verified. All **135 local automated tests** passed, but
production configuration, deployments and authenticated device checks remain.

## Live checks performed

An isolated headless Chrome session used a new empty browser profile. No existing
browser credentials, passwords, Firebase tokens or customer data were read.

| Check | Actual result |
| --- | --- |
| Dashboard root and JS/CSS assets | HTTP 200 |
| Direct /login, /devices, /subscriptions, /activity, /users, /settings | HTTP 200; signed-out protected routes redirect to /login |
| Login form, empty submission | Correct inline validation; zero Auth requests |
| Password visibility toggle | Shows/hides correctly |
| Browser runtime exceptions during public checks | None observed |
| Compiled dashboard Firebase project | chatzy-d8ba4; matches Android |
| Lock/Unlock/Sync command function in live bundle | Unconditionally throws VITE_COMMAND_API_BASE_URL is not configured |
| POST to Vercel /api/device-command without credentials | HTTP 405; this frontend URL is not the command backend |
| GET to Vercel /api/health | HTML page, not an API health response |
| Authenticated dashboard reads, actual role login | Not verified: no admin test session supplied |
| Live subscriptions/users/settings changes | Not performed against production data |
| Actual FCM dispatch and phone acknowledgement | Not verified: backend not configured, no authorized test session; ADB reports phone offline |

The deployed main bundle inspected was `/assets/index-sIju_j3Z.js`. Inspection
parsed the public JavaScript AST and reported only project/configuration status;
it did not print keys or full source containing Firebase configuration.

Unauthenticated POST probes to all of the following expected callable routes in
`us-central1-chatzy-d8ba4.cloudfunctions.net` returned **HTTP 404**, rather than
an existing handler's authentication rejection:

- sendDeviceCommand
- requestDeviceEnrollment
- completeDeviceEnrollment
- approveDeviceEnrollment
- listDeviceEnrollmentRequests
- chatzySupport

These probes sent an empty data object, no credentials and no customer/device
identifiers. They performed no lock, unlock, subscription or registration writes.

## Confirmed local fixes

1. `admin_web/src/hooks/useDeviceCommand.ts` captured the initial render's
   `uiState` inside its 30-second timeout. A command could stay at sent indefinitely
   because the callback never saw sent. The timeout now depends on its cancellable
   request generation instead. Terminal acknowledgements cancel it. Duplicate
   submissions are guarded, cached early acknowledgements release subscriptions,
   and a cancelled/reset request cannot recreate an obsolete listener.
2. `admin_web/src/services/subscriptions.ts` used
   `updateDeviceSubscription(deviceId, 0, status)` for Activate/Suspend. That
   erased subscription expiry. These actions now use status-only updates and log
   the existing expiry in both history fields. The subscription page supplies its
   current expiry. Lock fields remain untouched.
3. The command-result effect in `DeviceDetail.tsx` now declares its actual
   dependencies. SYNC audit records no longer incorrectly say Unlocked.
4. Added a separate, non-secret Worker deployment configuration,
   `admin_web/cloudflare-worker/wrangler.chatzy.jsonc`, with the actual Chatzy
   Firebase project and exact Vercel CORS origin. The original legacy
   `wrangler.jsonc` still targets order-flow-34826 and localhost; it was preserved
   rather than silently repurposing another deployment.
5. Added `admin_web/scripts/check-connection.mjs` to detect project mismatches,
   missing assets and the deployed unconditional CONFIG_ERROR. Run it after each
   deployment. It exits nonzero for the live site's current broken command setup;
   this is an observed configuration failure, not a passing end-to-end check.

Changes are local. The Vercel deployment and its environment were not changed.

## Automated checks

| Suite | Result |
| --- | --- |
| Android debug assemble and unit tests, expected project chatzy-d8ba4 | Build passed; 50 tests passed |
| Firebase Functions TypeScript build/tests | Build passed; 19 tests passed |
| Auth + Firestore emulator integration/security | 50 tests passed; no skips |
| Dashboard command/subscription/config regression tests | 10 tests passed |
| Worker authentication/config/CORS tests | 6 tests passed |
| Dashboard TypeScript/Vite production build | Passed |
| Dashboard lint | No errors; 2 pre-existing Fast Refresh export warnings |
| Original Worker and separate Chatzy Worker deployment dry-runs | Passed; no deployment |
| Live public-configuration check | Failed as expected: commandConfigurationMissing=true |

Emulator tests used **demo-chatzy-security** and loopback only, including actual
scoped Auth-token access, approval, dashboard ordered queries, repeat registration,
subscription/lock preservation, unauthorized writes, cross-device isolation and
revocation. They do not assert that those tested rules/functions are deployed.
Dashboard service tests execute the actual TypeScript against controlled React,
clock, Firebase and network boundaries. They are not authenticated browser tests.

## Complete the production connection

The correct setup depends on the intended existing command transport. Do not
point Android Firebase or VITE_COMMAND_API_BASE_URL at the Vercel frontend merely
to remove the configuration error.

### Existing Worker transport

1. Authenticate Cloudflare with the account that owns the command Worker. The
   session check in this task returned **login required**.
2. Provision the existing server-only bindings
   **FIREBASE_SERVICE_ACCOUNT_EMAIL** and
   **FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY** for the Chatzy project. Never place
   them in Vercel VITE_ variables, Android or committed dotenv files. From the
   Worker directory, use `wrangler secret put <binding> --config wrangler.chatzy.jsonc`
   and enter the values interactively through the normal authorized setup.
3. Review and deploy the prepared Chatzy target:
   `npm --prefix admin_web/cloudflare-worker run deploy:chatzy`
4. Obtain its actual deployed HTTPS base URL. Confirm GET /health returns JSON
   status ok, OPTIONS /api/device-command allows the exact Vercel origin, and an
   unauthenticated POST returns 401. Health alone does not verify Firestore/FCM.
5. Set Vercel Production **VITE_COMMAND_API_BASE_URL** to that actual base URL and
   redeploy the dashboard. Vite embeds this setting at build time; changing an
   environment variable without a new deployment does not fix the existing bundle.
6. Run `npm --prefix admin_web run check:connection`. Then use a normal signed-in
   test admin and approved test device for a real command and acknowledgement.

### Existing authenticated enrollment/callable rollout

If enabling the prepared secure device-enrollment mode, coordinate its existing
backend, rules, admin approval UI and Android build as described in
`CHATZY_DEVICE_REGISTRATION_FIX.md`. The legacy Worker lacks the authoritative
command metadata checked by secure enrollment mode; do not mix those transports.
Deploy the reviewed callable command/enrollment functions to **chatzy-d8ba4** and
use **VITE_USE_CALLABLE_DEVICE_COMMANDS=true** with
**VITE_DEVICE_ENROLLMENT_ENABLED=true** in the corresponding frontend build.
The installed default build is not automatically switched into that rollout.

The Firebase CLI account previously returned project-access HTTP 403 for
chatzy-d8ba4; an authorized project administrator is needed for that deployment.
Do not publish permissive legacy Firestore rules to make registration pass.
No billing, authentication policy, Firestore rules or production access changes
were made by these checks.

## Remaining live acceptance checks

Reconnect the Samsung phone to ADB and supply a normal signed-in test admin
session. Confirm the intended test device before issuing operational commands.
Then verify device ID/project equality, live lastSeen progression, FCM token
presence without exposing its value, Sync acknowledgement, Lock/Unlock and
restored original state. On test records only, verify activation/suspension retains
expiry, extension adds the intended days, activity/history updates, and role
restrictions. Existing production users, subscriptions and customer records must
not serve as disposable test fixtures.

Only after these checks pass can every requested live function be called verified.

## Changed/new files for this task

- `admin_web/src/hooks/useDeviceCommand.ts`
- `admin_web/src/pages/DeviceDetail.tsx`
- `admin_web/src/services/subscriptions.ts`
- `admin_web/src/pages/Subscriptions.tsx`
- `admin_web/scripts/check-connection.mjs`
- `admin_web/scripts/check-connection.test.mjs`
- `admin_web/scripts/dashboard-services.test.mjs`
- `admin_web/package.json`
- `admin_web/cloudflare-worker/wrangler.chatzy.jsonc`
- `admin_web/cloudflare-worker/package.json`
- `admin_web/cloudflare-worker/tests/worker.test.cjs`
- `admin_web/functions/scripts/support-deployment.test.cjs` (lint-safe fixture cleanup)
- This report. Existing unrelated workspace changes were retained.

Sanitized browser result artifact:
`.artifacts/dashboard-audit/public-smoke.json`.

Deployment session checks: Cloudflare requires login. Vercel CLI reported login
required or unavailable, and this workspace has no linked `.vercel/project.json`.
A dashboard login alone does not supply those deployment credentials. No deployment
was attempted without them. The isolated audit browser was closed after testing.
