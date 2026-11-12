# Chatzy Admin Console

Owner: SHAVIN SOLUTIONS (PVT) LTD. Tagline: Manage Your Chatzy Business.

The existing React/Vite/TypeScript/Tailwind application is retained. Brand defaults are in src/config/brand.ts; Firebase settings are centralized in src/services/firebase.ts. index.html carries the company title/description; public/favicon.svg and Android vectors share the original Chatzy C/speech-bubble geometry. Dark slate surfaces, existing teal controls, tables and spacing remain.

## Route and data contract
| Route | Existing function |
|---|---|
| /login | Firebase email/password sign-in and admin profile verification |
| / | real-time totals, subscription statistics, recent installations, weekly chart |
| /devices | device/business/phone/ID search, filters and status monitoring |
| /devices/:deviceId | profile, timestamps, app version, subscriptions and LOCK/UNLOCK/SYNC tracking |
| /subscriptions | active/expired views, extension/custom days, subscription history |
| /activity | logs and remote command status |
| /users | admin profiles/roles |
| /settings | global settings and Firebase connection status |

All original route patterns, onSnapshot listeners and real data operations remain; no mock devices were introduced. Notifications are a legacy Android admin feature and Firestore collection: there was no React notifications route in the audited baseline. This migration does not claim one was present or add a replacement workflow. Existing Android admin sources are unchanged pending deployment scope confirmation.

## Configuration and operation
Copy genuine Web app values into ignored .env.local or CI environment. Missing required Web config fails initialization with key names only. Set VITE_COMMAND_API_BASE_URL to the actual matching Worker. Optional VITE_SUPPORT_EMAIL/PHONE, VITE_PRIVACY_POLICY_URL and VITE_WEBSITE_URL remain blank until business details are supplied. Existing Firestore settings/global values take precedence over defaults; an authorized operator must review old company/support values explicitly. No remote settings were automatically overwritten.

Browser commands still call POST /api/device-command with a Firebase ID token. Worker validates RS256 token/project and admin role before loading a device token and sending FCM. Callable Functions remain available as a separate supported backend path. Support must remain read-only in the staged rules/backend rollout.

Run npm ci, npm run lint and npm run build. Run emulator tests from ../security-tests and backend tests in functions/ and cloudflare-worker/. Real sign-in, command delivery and browser navigation require genuine accounts and a test device. Existing lint warnings and bundle-size warning are reported in CHATZY_TEST_RESULTS.md.

## Existing limitations retained for compatibility
- React has no automated browser E2E suite in this migration; static route preservation is not interactive verification.
- Audit logging currently creates a record then attempts to update logId, while deployed rules may deny updates. Review this pre-existing behavior before immutable rules rollout; do not weaken log protection to hide it.
- Subscription writes/history/log writes are multiple operations, not one transaction. Concurrent admin changes and partial failures require a separate consistency improvement.
- Local callable command policy now matches the Worker (Admin/Super Admin only); existing deployed code has not been changed. See CHATZY_FIRESTORE_RULES_REVIEW.md.
- Node 20 is the declared Functions runtime; verify callable runtime behavior with its deployed toolchain before release.
