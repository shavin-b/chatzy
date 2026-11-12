# Current verification after enabling Anonymous sign-in

The earlier authentication blocker is resolved on the connected phone. The
rebuilt debug app successfully retrieves a Firebase ID token from the Anonymous
provider and verifies locally that its audience matches `chatzy-d8ba4`.
No token or claims were printed. The current blocker is the missing support route.

| Installed-build setting | Verified value |
| --- | --- |
| Build variant | debug |
| Firebase project ID | chatzy-d8ba4 |
| Android application ID | com.orderflow.autoresponder |
| Firebase app ID | 1:967851263632:android:618ea34de88164b50cba15 |
| Functions region / route | us-central1 / chatzySupport |

There are no variant-specific Google Services files under `app/src` and no
explicit FirebaseOptions/FirebaseApp initialization or emulator override in app
code. The root `app/google-services.json` supplies the generated debug resources.
The separate `ADMIN app` configuration does not configure this app. Installed APK
resources and the on-device probe agree on these values.

## Exact old message path

`HelpViewModel.send` or `retry` -> `submit` -> `SupportClient.ask` -> catch ->
`SupportRequestException` -> `SupportClient.failureMessage` -> the failed
HelpMessage.error field -> HelpScreen inline error text.

The previous message, "AI chat sign-in is blocked by server configuration", was
shared by three FirebaseAuthException.errorCode values:
`ERROR_ADMIN_RESTRICTED_OPERATION`, `ERROR_OPERATION_NOT_ALLOWED`, and
`ERROR_CONFIGURATION_NOT_FOUND`. It was not the backend HTTP error message.
A failed message keeps its existing error until Retry is attempted; changing a
Firebase Console setting does not retroactively update that UI state.

The updated code separates all three Auth messages, missing-endpoint errors and
backend-configuration errors. It also separates authentication, explicit ID-token
retrieval and callable stages in sanitized diagnostics. Authentication stays
required; project-mismatched tokens fail locally without logging the token.

## Actual Retry evidence

The updated test-only device probe uses the real HelpViewModel and SupportClient,
submits the requested question and retries the same message twice. Results:

- `retryStableMessage=true`, `inlineErrors=1`.
- Every attempt: `stage=id-token-ready audienceMatches=true provider=anonymous`.
- Every attempt then fails: `stage=callable code=functions/NOT_FOUND`.
- Final retry request ID: `06296eb0-a0dd-486d-bfda-18c6b38bcc0e`.
- A separate POST to the exact URL below returned HTTP 404:
  `https://us-central1-chatzy-d8ba4.cloudfunctions.net/chatzySupport`.

Firebase's callable SDK attaches the current Firebase ID token automatically.
The app uses the same default Firebase app for Auth and Functions. The backend
source uses onCall token verification, denies absent request.auth, and has no
Anonymous-provider rejection. The controlled HTTP test now explicitly uses
Anonymous-provider claims. Documentation:
https://firebase.google.com/docs/functions/callable . Live backend token
validation cannot be observed until that route exists in this project.

## Confirmed access blocker and next step

The Firebase CLI account in this workspace cannot manage the actual app project.
A metadata-only check (not secret access) returned HTTP 403 from Service Usage:
`Caller does not have required permission to use project chatzy-d8ba4`, identifying
missing `serviceusage.services.use`. No secret value was read. Granting this
permission alone does not necessarily grant deployment or Secret Manager access;
use an account with the appropriate existing project permissions.

An authorized administrator must verify/provision `GEMINI_API_KEY` in this
project's Firebase Secret Manager, set `CHATZY_SUPPORT_ENABLED=true` and the
free-tier-eligible `CHATZY_SUPPORT_MODEL` in
`admin_web/functions/.env.chatzy-d8ba4`, and deploy this local route. From
`admin_web`, run:

`npm --prefix functions run deploy:support -- --project chatzy-d8ba4`

Do not deploy to the CLI default `order-flow-34826`, change app credentials to
work around the missing route, disable authentication, or enable billing.

Debug build, test APK build, all 50 Android unit tests and all 19 backend tests
passed. Updated debug and test APKs installed successfully. A real Gemini reply
was not verified because the route is missing and this CLI account lacks project
access. Physical UI taps remain distinct from the real ViewModel Retry probe.

The remaining sections record the previous investigation and detailed setup;
this current result supersedes the older authentication-blocked state.

---

# Chatzy Help: integration and verification

## Confirmed authentication and deployment failures

The installed APK resource `project_id` is **chatzy-d8ba4**, verified by pulling the
installed APK and inspecting its resources with aapt2. The generated debug
Firebase resources agree. `FirebaseModule` uses the default Firebase app and
`us-central1`; SupportClient calls the callable named `chatzySupport`.

Actual endpoint:
`POST https://us-central1-chatzy-d8ba4.cloudfunctions.net/chatzySupport`

The real Android instrumentation probe returned
**auth/ERROR_ADMIN_RESTRICTED_OPERATION**, request ID
**7ee362a9-0cfb-49e9-83ce-1d98788b5eb5**. The sanitized support log says
`stage=authentication`; the callable was never invoked. This is the first failing
layer in the installed client flow: anonymous account creation is restricted by
Firebase Authentication's project configuration. It is not a generic network or
Gemini error. The client now exposes a specific configuration message instead of
masking it as temporary service unavailability.

Google documents this error for restricted user creation:
https://docs.cloud.google.com/identity-platform/docs/concepts-manage-users .
The existing app identity was reused if available; on this phone there was no
current Firebase Auth identity, so the existing anonymous sign-in path ran.

An HTTP POST to that exact route returned **HTTP 404**, with a generic gateway
page rather than a callable response. It did not reach the support handler or
Gemini. There was no handler request ID in that response. The backend CLI's
`.firebaserc` instead defaults to **order-flow-34826**. Its function listing
returned an empty array, and its support URL also returned 404. Listing functions
in chatzy-d8ba4 failed with the CLI credentials available in this session.
No deployed Gemini secret/model/provider quota could therefore be verified.

The Android and backend source contracts are aligned; this is not evidence of a
Gemini-generated error or an Android reply-decoding failure. After authentication is permitted, the server route
must exist in the Firebase project actually used by the app. The new support
project check derives that target from Android's variant-specific Google Services
configuration and requires an explicit matching project for deployment. It does
not change authentication, app Firebase configuration, device sync or CLI defaults
for unrelated functions.

## Request and response contract

- Firebase callable SDK uses POST, the `data` request envelope and the current
  Firebase Auth ID token in transport. Anonymous sign-in is used only if no
  current identity exists. Auth credentials never enter the Gemini payload.
- Request data: `requestId` (UUID correlation metadata) and `turns` (alternating
  `{role: user|model, text}` objects). At most six prior successful turns plus the
  current support question. UTF-8 budgeting removes complete old pairs if needed.
- Response envelope: `result`, containing `text`, `actions`, `topicIds` and
  `requestId`. The Android SDK unwraps `result` into callable `data`; Android
  validates `text` and allowlists action IDs before rendering buttons.
- Callable errors use Firebase status codes, with a sanitized `details.requestId`
  and fixed `reason`. Android logs its request ID, fixed stage, project/region/route
  and Firebase error code. Backend logs fixed stages and numeric provider status.
  Neither logs support text, provider bodies, tokens, auth UID, keys or stack traces.
- Client timeout: 30 seconds; provider timeout: 25 seconds; function: 35 seconds.
- Server limits: 7 turns, 1000 characters/user turn, 2000/model turn, 10000 total
  characters, 24 KB UTF-8 JSON; 3/minute and 20/UTC day per Auth UID and 100/UTC day
  per project, atomically reserved in client-inaccessible `supportQuota` documents.

## Remaining activation steps (no billing changes)

First, an administrator for **chatzy-d8ba4** must review Firebase Authentication /
Identity Platform user signup configuration. The observed error means client user
creation is restricted; permit the app's existing anonymous sign-in flow by
reviewing **client.permissions.disabledUserSignup** (must be false for client
account creation) in Identity Platform settings. Also ensure
**signIn.anonymous.enabled=true** (Firebase Console > Authentication > Sign-in
method > Anonymous). Do not change unrelated login providers, device approval,
Firestore rules or billing. If policy intentionally requires admin-provisioned
identities, supply that existing approved sign-in flow instead; do not bypass
this restriction in the client. Configuration reference:
https://docs.cloud.google.com/identity-platform/docs/reference/rest/v2/Config .
No Authentication policy changes were made by this task.

Then, from `admin_web`, with an account authorized for **chatzy-d8ba4**:

1. Confirm that Gemini Developer API model `gemini-2.5-flash-lite` is available
   on that project's free tier. The default is documented on Google's pricing
   page: https://ai.google.dev/gemini-api/docs/pricing . Project-specific access
   remains unverified; do not enable Gemini billing or substitute a paid model.
2. Provision or verify server secret **GEMINI_API_KEY** in that project's Firebase
   Secret Manager:
   `firebase functions:secrets:set GEMINI_API_KEY --project chatzy-d8ba4`
   Enter the key interactively. Never put it in Android or a dotenv file.
3. Copy `functions/.env.example` to **functions/.env.chatzy-d8ba4**. After confirming
   free-tier access, set **CHATZY_SUPPORT_ENABLED=true** and
   **CHATZY_SUPPORT_MODEL=gemini-2.5-flash-lite** there. These are non-secret
   Firebase Functions runtime parameters. Actual dotenv files are ignored.
4. Run `npm --prefix functions run support:check`, then deploy only this route:
   `npm --prefix functions run deploy:support -- --project chatzy-d8ba4`
   If Firebase requires a billing/plan upgrade, stop. No billing or project-plan
   changes are authorized or performed by this work.
5. Confirm the route with
   `firebase functions:list --project chatzy-d8ba4`. Then rerun the live client
   probe below and verify actual Sinhala/Singlish answers on an unlocked phone.

The route is implemented locally; the authentication restriction, route deployment
and project-specific secret/model verification still need administrator action. No production deployment or secret changes were
made. Mocks occur only in test code.

## Chat state and UI

Each question has a stable UUID and SENDING, FAILED or ANSWERED state. Acceptance
clears the composer and immediately adds one user message. Failures attach one
inline error and a Retry button to that message. Retry keeps its text, ID and
original context, clears the previous error, and uses the same in-flight guard.
Further failures replace the error. Success marks it answered and adds one AI
reply; answered messages cannot be retried. A newly typed draft survives an older
message's retry. Arrow and keyboard Send share one handler.

The transcript has independent scrolling and stable item keys. The composer
remains enabled for typing during requests, preserving IME focus, while Send is
busy-guarded. Existing adjustResize/edge-to-edge inset handling is retained.
First-use Google Gemini consent persists in device preferences. No messages,
customer records, WhatsApp text, logs or device tokens are automatically attached.

All offline banners, chips, generated answers, matcher code and fallback routing
were removed from Help. Verified help content remains server-side, with reference
parity checks against the unchanged raw JSON. Navigation buttons still open only
on taps through the existing allowlisted destinations.

## Checks

- Android: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline
  -PexpectedFirebaseProjectId=chatzy-d8ba4` passed, 50 unit tests.
- Backend: `npm run build`, `npm test`, `npm run support:check` passed, 19 tests.
- Controlled local HTTP test exercises Firebase callable POST/auth/envelopes,
  actual handler validation, provider payload and reply mapping, using test-only
  auth/provider doubles. It does not prove a live Gemini reply.
- Regression tests exercise initial failure, two failed retries, recovery to one
  reply, stable message identity, no error bubbles, no duplicate requests, retained
  drafts/context, UTF-8 limits, Sinhala/Singlish follow-ups and action allowlisting.
- Source search confirms no offline chat controls, matcher or fallback routing in
  the Help package. The connected phone was locked during attempted UI inspection;
  physical Send/keyboard, visible IME layout and a real displayed Gemini response
  still require an unlocked device and working deployed endpoint.

### Live client probe

A test-only Instrumentation runner can call the real Android SupportClient while
the phone is locked. It requires existing in-app consent and explicit
`-e liveSupport true`; it never prints question/reply text or credentials.

Build: `gradlew.bat :app:assembleDebugAndroidTest --offline -PchatzySupportProbe=true`

Install debug app and `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`,
then run:

`adb shell am instrument -w -r -e liveSupport true com.orderflow.autoresponder.test/com.orderflow.autoresponder.help.SupportProbeInstrumentation`

On success it checks the initial question and `ow enabled` through the real client,
while visual rendering remains a separate check. On failure it prints only the
project, sanitized error category and request ID (if an AI request was attempted).
Use `adb logcat -d -s ChatzySupport:I` to correlate only the sanitized support tag.

### On-device result

The app and opt-in test APK installed successfully. The live probe ran on the
connected phone and confirmed the authentication failure above with the actual
Firebase SDK. A real Gemini reply and follow-up were **not** verified: the probe
stops at the confirmed first failure, and the independently tested callable URL
also needs deployment. Visual Send/keyboard and IME checks remain blocked by the
locked screen. No logs containing chat text or credentials were collected.

## Files changed for this fix

- Android Help: `HelpViewModel.kt`, `HelpScreen.kt`, `SupportClient.kt`,
  `HelpModels.kt`; removed unused `HelpMatcher.kt` and its obsolete matcher tests.
- Android verification: `SupportChatTest.kt`, test-only
  `SupportProbeInstrumentation.kt`, `src/androidTest/AndroidManifest.xml`, and an
  opt-in `chatzySupportProbe` test-runner selector in `app/build.gradle.kts`.
- Backend: `functions/src/support.ts`, `functions/package.json`,
  `functions/scripts/deploy-support.cjs`, `support-contract.test.cjs`,
  `support-deployment.test.cjs`, `functions/.env.example`, and dotenv ignore rules.
- This document records deployment targets, contracts, evidence and remaining steps.

Existing changes outside Chatzy Help were left intact. Verified help content and
WhatsApp auto-reply, rule, customer and device-sync behavior were preserved.
