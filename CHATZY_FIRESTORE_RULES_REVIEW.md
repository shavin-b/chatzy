> New local enrollment implementation: see CHATZY_DEVICE_ENROLLMENT.md. It is opt-in and not deployed. The earlier analysis below describes the pre-enrollment client.

# Chatzy Firestore rules and authorization review

Project: chatzy-d8ba4. Prepared locally on 2026-11-09. **Not deployed or published.** No production records or Cloudflare Worker files were changed. The actual cloud admin document and deployed rules were not read; this review verifies the code contract and emulator behavior, not your live account.

## Exact files

- Candidate rules: `security-tests/firestore.staged.rules`.
- Emulator tests: `security-tests/rules.test.mjs`.
- Emulator-only configuration: `security-tests/firebase.json`; runner: `security-tests/run-emulator.cjs`.
- Deployment configuration remains `admin_web/firebase.json` pointing to existing `admin_web/firestore.rules`; it does NOT reference the candidate.
- Website role validation changed in `admin_web/src/services/auth.ts`.
- Callable command authorization changed in `admin_web/functions/src/index.ts`, using `admin_web/functions/src/authorization.ts`; tests in `admin_web/functions/scripts/authorization.test.cjs`.

Do not copy/publish the candidate into the live rules editor yet. Do not merge it with public-access legacy rules: an overlapping allow rule could bypass the restrictions.

## Verify your administrator document locally in Firebase Console

Authentication's UID must be the exact document ID at `admins/{AUTH_UID}` in chatzy-d8ba4. A matching email alone is insufficient. The authorization field must be the string **Super Admin**, with this capitalization and one space. `super admin`, `SuperAdmin`, `Super Admin `, booleans and custom `isAdmin` profile flags do not qualify.

| Field | Type / value | Requirement |
|---|---|---|
| role | string: Super Admin | Required for Super Admin authorization |
| name | nonempty string | Required by candidate admin create/update validation and user listing |
| email | nonempty string matching the Firebase Authentication user's email | Required by candidate validation; matching Auth token email is needed for actor-bound audit/history writes |
| lastLogin | number, integer epoch milliseconds; 0 initially is valid | Required by candidate validation; the website updates this after login |
| adminId | string equal to AUTH_UID | Optional in candidate; website derives identity from the document ID, not this field |
| avatarUrl | string or null | Optional |

Current authorization depends on UID/document existence and an exact recognized role. The candidate imposes the display/schema fields when privileged clients create or edit profiles; populate missing fields before enabling it. No passwords, Firebase keys, service account material or arbitrary secrets belong in admins documents. First Super Admin provisioning remains a trusted Console/Admin SDK operation; ordinary users cannot bootstrap themselves through client rules.

The website previously cast any existing profile to an AdminUser and defaulted missing roles to Support. It now rejects missing/unknown/malformed roles before admitting a dashboard session. Existing valid Super Admin/Admin/Support profiles retain their behavior. UI checks alone are not a data-security boundary; the candidate enforces authorization in Firestore.

## Access policy in the candidate

| Principal | Read access | Client writes |
|---|---|---|
| Unauthenticated | none | none |
| Ordinary signed-in user, no valid admin profile/device claim | none to management collections | none; cannot create its own admin profile, grant roles or change subscriptions |
| Support | existing admin dashboard collections | only its own integer lastLogin; no management operations |
| Admin | existing admin dashboard collections | subscription fields/history, own login metadata, own-actor audit creation, notification creation; no role/settings management or direct commands |
| Super Admin | existing admin dashboard collections | validated admin management, settings, subscription/history/notification management and audit creation; own demotion/deletion blocked |
| Authenticated device with trusted device_id claim | its own device document and own command records | allowed telemetry and acks; cannot set subscription/lock authority, enroll itself or create commands |
| Trusted backend | Admin SDK/IAM authority | provisioning, command creation and server-owned subscription/lock authority; requires its own authorization checks |

All unknown collections/subcollections default to denied. Device documents include FCM tokens and are read in full by authorized dashboard roles and the owning device. Rules cannot hide individual fields within a readable document. Separating token storage from Support-visible data requires a future compatible schema/backend change.

## Existing dashboard compatibility addressed

- Subscription writes include `lastSync` along with `subscriptionEnd` and `status`; the previous candidate omitted lastSync and would reject valid operations. This candidate allows it for Admin/Super Admin.
- Expiry patches accept existing number/Timestamp/null representations and reject invalid string values; unchanged legacy fields are not revalidated on unrelated patches.
- Recognized admins can update their own lastLogin without permission to change their role or others' login metadata. The previous candidate allowed only Super Admin profile writes, breaking other roles' lastLogin updates.
- Browser audit creation uses addDoc with logId='', then binds the generated ID through setDoc. The candidate permits that one-time logId-only binding by the original actor, while denying content changes/deletion and actor impersonation. It does not claim browser-authored logs are server-attested audit evidence.
- Existing command acknowledgement identity fields can be resent unchanged with set+merge. Altering deviceId/command or creating a missing command record is denied.

## Android compatibility blocker: do not publish yet

The unchanged Android application does not authenticate device Firestore operations. It uses device IDs, which are identifiers, not credentials. Firebase rules cannot distinguish a legitimate unauthenticated OF- device from an attacker submitting the same ID. An unauthenticated compatibility allow rule would reopen the same data path to attackers.

The candidate therefore intentionally rejects legacy clients. Keeping both existing unauthenticated writes and guaranteed authenticated authorization is not achievable with rules alone. Existing deployed rule files were left unchanged so this preparation does not interrupt current devices; the existing public-access risks remain until an authorized rollout.

Specific call sites to address before candidate activation:

1. DeviceManager.registerOrUpdateDevice uses set+merge and includes subscriptionStatus from local preferences. Server enrollment must preserve existing OF- IDs/subscriptions, provision missing records safely and prevent devices from asserting subscription authority. Identity-only telemetry patches must not change protected fields.
2. HeartbeatWorker writes lastSeen/lastSync plus adminLocked/lockStatus/lockReason. An authenticated heartbeat with unchanged authoritative lock values can pass; stale local values cannot overwrite server intent and are denied. Separate local reported state from authoritative state or adjust payloads with a reviewed compatible rollout.
3. FirebaseDeviceRepository.acknowledgeCommand sends status/processedAt plus identity fields. Updates to existing matching commands can pass after authentication; missing-command creation is blocked. It also writes authoritative device lock fields after a successful LOCK/UNLOCK. Move that authority to a trusted backend without losing acknowledgement behavior. No such Android/backend enrollment implementation is claimed here.
4. HeartbeatWorker currently ignores a returned repository Result.failure and can report WorkManager success on a permission denial. Publishing stricter rules without testing device error reporting can silently stop synchronization. This existing Android code was not modified.
5. Legacy separate Android admin application must be reviewed as a distinct client before role/schema enforcement, not treated as the React website.

Use Firebase Authentication with server-issued, installation-bound device_id custom claims. Enabling anonymous Auth alone must not allow a user to claim an arbitrary existing OF- ID. Require reviewed owner/admin-approved enrollment and recovery, then monitor before enforcing. App Check can add attestation/abuse protection but does not replace ownership authorization.

## Backend authorization

Worker already requires a valid Firebase ID token and exactly Admin or Super Admin for remote commands. Its code and configuration are unchanged.

Callable Functions previously accepted Support for remote commands. Local source now uses the same management role policy as Worker. All other callable authentication, device lookup and FCM flow remain. Exact role-policy tests reject Support, missing/unknown/malformed/case/whitespace variants. No callable deployment or remote invocation occurred. Existing deployed Functions retain their previous behavior until an explicitly authorized release.

Admin SDK bypasses Firestore rules, so candidate rules cannot secure a misconfigured callable, Worker, IAM key or enrollment server. Rotate the supplied compromised service-account key before any server rollout. Worker bindings/.dev.vars and Firebase CLI aliases still need their separately authorized Chatzy environment alignment; this task did not alter them.

## Verification and limitations

- Emulator suite: **29 passed, 0 failed, 0 skipped**. Runs only local Firestore using demo-chatzy-security, not chatzy-d8ba4. Expected PERMISSION_DENIED output represents denial assertions.
- Functions build: passed. Functions test command: **8 passed** (3 actual website auth-service tests with mocked Firebase reads, 2 backend authorization policy tests and 3 existing manifest tests).
- React TypeScript/production build: passed. Lint: passed with three existing warnings; existing >500 kB bundle warning retained.
- Fingerprint verification: 139 protected Android, Worker, environment and deployment-rule/configuration files unchanged.
- No live Auth sign-in, live administrator document read, deployed rules inspection, physical-device test, FCM dispatch, production data operation or deployment was executed.
- No Android rebuild needed because no Android files changed. Prior Room/auto-reply behavior remains outside this rules test's runtime coverage.

Test command from repository root: `npm test --prefix security-tests`. Functions: `npm run build --prefix admin_web/functions` then `npm test --prefix admin_web/functions`. Web: `npm run lint --prefix admin_web` and `npm run build --prefix admin_web`. Emulator output is recorded in ignored `.artifacts/chatzy-rules-review/emulator-results.log`.

## Exactly what to do next

1. In Firebase Console for chatzy-d8ba4, compare your Auth UID and admin document against the field table above. Confirm exact role, email and types. No need to share passwords or private/server credentials.
2. Review the candidate and test results without clicking Publish or changing deployment rule paths. Confirm the intended role matrix, including Support's read-only management policy and Admin's subscription authority.
3. Next development prerequisite is authenticated Android enrollment plus adjusted registration/heartbeat/lock acknowledgement ownership. Preserve package, Room database, cached OF- IDs, subscriptions and expiry dates. Test supported older installations and reconnect/reboot/FCM behavior in an isolated environment.
4. Validate both authorized-admin and ordinary-user browser sessions against the candidate in an isolated test environment; test every existing dashboard write, not only sign-in. Preserve server audit/actor binding and indexes. Plan a backward-compatible rollout or keep legacy fleet in the old project until migrated; never add public bypasses to the new strict project.
5. Configure matching Chatzy backend identity/secrets/CLI target only under separate authorization, rotate compromised credentials, test enrollment/roles/commands/acknowledgements, then request explicit approval for deployment. Nothing in this report authorizes publishing the candidate now.

## Primary references checked

- https://firebase.google.com/docs/firestore/security/rules-fields
- https://firebase.google.com/docs/firestore/security/rules-conditions
- https://firebase.google.com/docs/auth/admin/custom-claims
- https://firebase.google.com/docs/firestore/security/test-rules-emulator
