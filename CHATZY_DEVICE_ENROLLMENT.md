# Chatzy device enrollment: local implementation and next steps

Project: chatzy-d8ba4. Prepared 2026-11-09. No deployment, rules publication, Console edit or production data operation was performed. Worker code/configuration and genuine Firebase configuration remain untouched.

## Safe rollout switches

Android secure enrollment is enabled ONLY with `-PchatzyDeviceEnrollment=true`; the default remains the existing legacy path. Web Settings approval controls require `VITE_DEVICE_ENROLLMENT_ENABLED=true`. Secure remote commands require `VITE_USE_CALLABLE_DEVICE_COMMANDS=true` to use Functions, since the unchanged Worker does not maintain the new command ordering metadata. Actual .env.local and Worker configuration were not edited. Do not install the opt-in APK against production before a coordinated pilot is authorized.

## Ownership protocol

1. The Android installation signs into Firebase anonymously and retains its UID through the Firebase SDK. It keeps its cached OF- ID, package, preferences and Room database.
2. A device can request a random pairing ID but cannot claim ownership by supplying an OF- ID. Requests expire after 30 minutes and reuse the current pending request for that UID.
3. A Super Admin compares the entire pairing ID with the physical device and approves it. The backend transaction enforces one UID per device and one device per UID; concurrent conflicting approvals have only one winner.
4. Completion checks the approved binding and issues a signed device_id claim only to that installation UID. Android force-refreshes its token.
5. Firestore rules require BOTH the scoped claim and a live active binding matching request.auth.uid. Revoking the binding denies device access even with an otherwise valid old claim. Clients cannot directly create or edit binding/approval records.

A matching device ID, email, anonymous UID or pending request alone is never proof of ownership. Administrators must not approve requests merely because the OF- ID looks familiar.

## Files and behavior

- `app/.../device/auth/DeviceEnrollment.kt`: persistent anonymous identity, pairing request, completion, refresh, live binding checks and server command validation.
- `DeviceWritePolicy.kt`: enrolled registration/heartbeat only transmit allowed telemetry; subscription, lock, identity and desired-command authority are excluded.
- `DeviceSubscriptionState.kt`: enrolled mode uses recognized dashboard subscription status, otherwise legacy subscriptionStatus, and evaluates server expiry. Unknown states fail closed. Historic inconsistent statuses must be reviewed before enrollment. Expiry is saved even when status text stays unchanged.
- `FirebaseDeviceRepository.kt`: authenticate before enrolled reads/writes, update only provisioned devices, acknowledge only existing matching commands, and never write authoritative lock fields in secure mode. Legacy transport is preserved by default.
- `DeviceManager.kt`: schedules background recovery while approval is pending, waits for enrollment before cloud registration/listeners, preserves nonblank OF- IDs and original generation format, and removes raw document logging containing FCM tokens.
- `HeartbeatWorker.kt`: returns retry on repository Result.failure. Enrolled writes are filtered centrally.
- `OrderFlowFcmService.kt`: enrolled commands must match server-created records; superseded LOCK/UNLOCK are ignored. Persistent dedup completion follows accepted acknowledgement, allowing lost acks to retry.
- Android Settings has opt-in Request approval/Check approval controls. It shows a pairing ID, never a JWT or administrator credential. Enrolled Disconnect retains identity and requires supervised recovery.
- `admin_web/functions/src/enrollment.ts`: request, approve and complete service. index.ts exports requestDeviceEnrollment, completeDeviceEnrollment, approveDeviceEnrollment and listDeviceEnrollmentRequests in us-central1. Only Super Admin may list/approve; device operations require anonymous-installation authentication.
- `admin_web/functions/src/commandAuthority.ts`: server-owned desired lock state and all lock aliases are committed with command creation. desiredCommandId prevents a stale FCM command from undoing a newer decision. Marking SENT preserves an early SUCCESS acknowledgement.
- Web Settings has an opt-in Super Admin approval panel and opt-in callable command transport. Existing routes and Worker transport defaults remain intact.
- Exact candidate rules: `security-tests/firestore.staged.rules`. Deployment firebase.json still points to the old deployment rules, not this candidate.
- Composite index prepared in `admin_web/firestore.indexes.json`: deviceEnrollmentRequests, state ASC and expiresAt ASC. Not deployed.

The Android Firebase Functions SDK is added under the existing BOM. ApplicationId/namespace com.orderflow.autoresponder, version/signing settings, Room filename/version/entities/DAOs, customer data and auto-reply engine remain unchanged.

## Data preservation and new metadata

Existing device records are not written during approval, including all subscription values/types, lock fields and unknown historical fields. Tests compare the entire existing record. New devices start Suspended/SUSPENDED with no expiry until an administrator grants a subscription. No subscription duration or price was invented.

Server-only metadata collections:

| Path | Fields |
|---|---|
| deviceEnrollmentRequests/{UUID} | requestId, authUid, deviceId, state, createdAt/expiresAt epoch ms, approvedBy after approval |
| deviceEnrollments/{UID} | current pairing request/identity/state mapping; one UID cannot switch IDs |
| deviceBindings/{OF-ID} | deviceId, authUid, active, approvedBy, approvedAt epoch ms |

Existing collection names/IDs are retained. The Functions command path adds server-owned desiredCommandId to device records. No Room migration is needed.

## Command behavior and Worker boundary

The new Functions path saves desired lock authority BEFORE FCM dispatch. Therefore a push failure does not roll back authorized admin intent; a connected device can receive it through Firestore. Delivery status and desired state are separate. All legacy lock aliases are synchronized by the server, including UNLOCK.

Do not use the unchanged Worker for an enrolled LOCK/UNLOCK pilot: it lacks desiredCommandId and the new lock-alias handling and still has its previous project binding. The prepared opt-in Functions transport avoids changing the Worker. A future Worker migration needs separate review.

## Verification

- Initial Auth/Firestore emulator suite: 46 passed.
- Final real-token integration run: 47 passed, 0 failed, 0 skipped, including real anonymous-token claim refresh and immediate binding revocation.
- Android default-mode tests/debug build: passed, 41 tests (33 original plus 8 payload/subscription tests).
- Android opt-in secure-mode tests/debug build: passed, 41 tests.
- Final default-mode rebuild: passed; standard debug output is restored to default legacy mode. The secure review APK is retained in ignored `.artifacts/chatzy-enrollment/app-debug-secure-review.apk` only for explicit review.
- Functions TypeScript build passed; 8 Functions/web-auth/utility tests passed.
- React lint/build passed with the existing three lint warnings and bundle-size warning.
- All 52 fingerprinted engine/Room/Worker/configuration files remain unchanged.

Run `npm run build --prefix admin_web/functions`, then `npm test --prefix security-tests`. Auth and Firestore run on loopback ports 9098 and 8088 with demo-chatzy-security. Test files run serially to avoid fixture interference. No genuine project credentials are used. The first added real-token test found a test namespace mismatch; the harness was aligned to one demo project, not weakened. Logs are under ignored `.artifacts/chatzy-enrollment/`.

The suite tests actual backend transactions, real anonymous Auth Emulator sign-in/token refresh and scoped Firestore SDK access/revocation. It is not a hosted callable HTTP/IAM test. No APK was installed or run, no real FCM was sent, and no physical-device/OEM/background/upgrade behavior was claimed verified.

## Exact Firebase Console steps for the next approved pilot

Do not deploy Functions or publish rules yet. These are manual prerequisites, not authorization for production changes by this task.

1. Select chatzy-d8ba4. Confirm the default Firestore database and existing admins/{exact Auth UID} document with role string Super Admin. Keep Email/Password enabled for your administrator.
2. Under Authentication -> Sign-in method, enable Anonymous when ready for the pilot. If Identity Platform automatic cleanup of anonymous users is enabled, disable it for long-lived enrolled devices or first implement a linked durable-identity flow. Never enter admin credentials into the Android app.
3. Keep the genuine Android registration/package/signing identity and current google-services.json. Do not uninstall, clear app data, rename the Room database or regenerate OF- IDs. Record SHA-256 signing fingerprints for later Play Integrity/App Check preparation.
4. Review Functions billing/API availability with the owner. Rotate the compromised service-account key and audit IAM before any server rollout. Functions use their runtime identity; do not place a server key in Android, VITE variables, Firestore documents or logs.
5. Review the prepared state/expiresAt composite index. It must be created during a later approved deployment or via Firestore Indexes. Do not manually insert binding/custom-claim records to bypass a missing enrollment backend.
6. Before migrating, reconcile existing device subscriptions/expiry/lock values with trusted business records. Old public rules may have allowed tampering; preservation alone does not establish historical correctness. Keep the old environment available for rollback.
7. Only after explicit pilot approval, deploy reviewed Functions/indexes to an explicit target, enable the two Web opt-in flags in that environment, and install the same-package opt-in APK without clearing data. Compare and approve the full physical-device pairing ID. Existing subscriptions must remain unchanged; new devices require an admin grant.
8. Publish the staged rules only under a separate later authorization after authenticated registration, heartbeat, FCM acknowledgement, reboot/reconnect and subscription tests pass for supported clients. Never combine them with global unauthenticated bypass rules. Mixed legacy clients need a separate legacy environment or a reviewed migration window.

## Remaining risks and recovery gates

- Existing deployed legacy rules remain unchanged; this local code does not secure the live project. Do not activate one half of the migration alone.
- App Check/attestation, public-endpoint quotas and expired-request cleanup remain pre-commercial gates. Request reuse per UID does not stop anonymous-account/Sybil abuse.
- Reinstall/cleared Auth state may produce another UID. Conflicting or revoked bindings fail closed; no self-rebinding or automatic takeover is included. A supervised audited compare-and-swap transfer/recovery workflow is required before broad rollout. Do not delete bindings to force enrollment.
- Local auto-replies retain cached policy offline. This is cloud authorization, not tamper-resistant licensing on rooted devices. Expiry is reevaluated on snapshots/reconnect, not guaranteed at the exact instant offline.
- Enrolled clients need connectivity to verify bindings/commands. Lost acks retry; supervised repair of a revoked session may require app restart. Physical-device integration remains a release gate.
- Existing device documents expose FCM tokens to authorized dashboard roles and that device; splitting token storage needs a future compatible schema change.
- Admin SDK bypasses rules. Protect IAM/runtime identities/callable roles and physical approvals. Existing message-based local controls and preference credential storage were not rewritten.

## Rollback

Local builds without chatzyDeviceEnrollment preserve the legacy path. After a future strict-rules rollout, reverting to an unauthenticated binary is not a safe rollback: use a higher-version same-package authenticated fix and supervised recovery. Preserve signing key, cached IDs and all local data. No rollback or migration was executed here.

References: Firebase anonymous Android Auth, custom claims, Auth Emulator and Functions local-emulator documentation: https://firebase.google.com/docs/auth/android/anonymous-auth ; https://firebase.google.com/docs/auth/admin/custom-claims ; https://firebase.google.com/docs/emulator-suite/connect_auth ; https://firebase.google.com/docs/functions/local-emulator

Signing gate: the retained secure review APK is debug-signed and must not be used to replace a release-signed customer app. A future upgrade pilot must use the original signing identity and an approved versionCode; never uninstall/clear data to work around a signing mismatch. Hosted callable/physical-device integration, recovery transfer and anti-abuse rollout remain pending.
