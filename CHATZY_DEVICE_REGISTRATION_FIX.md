# Chatzy device registration diagnosis and review steps

## Evidence and limits (2026-11-12)

- The open Android app has no product flavors or source-set Firebase override. The only `app/**/google-services.json` is `app/google-services.json`: project `chatzy-d8ba4`, package `com.orderflow.autoresponder`. Generated debug Google Services resources also say `chatzy-d8ba4`. No credentials were changed. The build guard is exercised with `-PexpectedFirebaseProjectId=chatzy-d8ba4`.
- The locally generated debug BuildConfig has `DEVICE_ENROLLMENT_ENABLED=false`. A physical phone became available later in the investigation. Its installed APK was pulled read-only: resource inspection confirms `project_id=chatzy-d8ba4`, and DEX BuildConfig inspection confirms `DEBUG=false`, `DEVICE_ENROLLMENT_ENABLED=false`. Version is 1.0.1 (code 2). The installed and enrollment review APK signing-certificate SHA-256 digests match; the installed APK was not replaced.
- Default builds use direct Firestore writes with no device authentication. Only opt-in `-PchatzyDeviceEnrollment=true` builds sign in anonymously, obtain physical Super Admin approval, refresh a `device_id` custom claim and verify a live server-owned `deviceBindings/<OF-ID>` record before device operations.
- Both paths use the default Firebase app, `(default)` Firestore database, `devices/<stableDeviceId>`. Existing nonblank IDs are reused from encrypted preferences; a first install creates `OF-` plus Android ID (UUID fallback). Local setup is profile completion, not authenticated enrollment.
- `OrderFlowApp.onCreate` starts DeviceManager even before profile setup. It initializes identity, observes remote state, writes device metadata, syncs FCM, and schedules the 15-minute heartbeat. Setup saves the profile locally and invokes `syncProfile`. FCM's `onNewToken` saves locally first and updates the same document. No messages, sender lists, or reply history are registration payloads.
- The separate `shavin-b/chatzy_adminConsole` repository was cloned read-only under ignored `.artifacts/registration-admin-review`. Its Firebase initializer requires `chatzy-d8ba4` and uses the default database; its device query is `devices orderBy(lastSeen, desc)` and supports Timestamp values. Its current source has no enrollment approval panel. Deployed Vercel environment variables, the signed-in dashboard, live rules and private device documents were not accessible for authenticated inspection.
- Read-only GET probes of `https://us-central1-chatzy-d8ba4.cloudfunctions.net/{requestDeviceEnrollment,completeDeviceEnrollment,approveDeviceEnrollment,listDeviceEnrollmentRequests}` each returned HTTP 404. The expected callable URLs are unavailable; authenticated deployment inventory was not available to prove whether functions exist in another region/project.
- Restarting the installed phone app confirmed the failure in live Logcat: project `chatzy-d8ba4`, device `OF-5bcd4b380ae84024`, `Write failed at devices/OF-5bcd4b380ae84024: PERMISSION_DENIED`, followed by `Device identity sync failed: PERMISSION_DENIED`. The device read listener is also denied. This confirms the unauthenticated legacy path/rules mismatch, not a successful write to another project. Numeric lastSeen by itself does NOT explain zero devices: orderBy excludes MISSING fields, not numeric fields. The independent enrollment defect was that approval provisioned a document without lastSeen and completion only issued claims. No successful live enrollment or dashboard appearance is claimed.

## Fix

1. Authenticated `completeDeviceEnrollment` now verifies binding and approval transactionally and writes `lastSeen` using an Admin SDK server timestamp. It updates only presence on existing records, preserving every subscription, activation, lock and unknown legacy field. If the approved record is missing, it recreates it with the existing suspended defaults. Claim issuance remains restricted to the approved anonymous UID.
2. Android registration and heartbeat lastSeen values are converted centrally to `FieldValue.serverTimestamp()`. Token/profile-only updates do not invent a new presence timestamp. Legacy timestamp documents remain readable by the typed device model.
3. Registration no longer uploads local subscription defaults; heartbeats no longer upload cached admin locks. `registerDevice` also avoids serializing authoritative defaults. Enrolled writes retain the existing telemetry allowlist and update only server-provisioned documents.
4. The heartbeat is scheduled before any network wait. Failed FCM repository Results no longer produce false success logs. Enrollment polling, Firestore listeners and writes report sanitized operation/path/error-code diagnostics without tokens, profile contents or exception payloads. Coroutine cancellation propagates.

No default rollout switch, application ID, credentials, launcher resources, UI, Room schema, WhatsApp handling, Local Mode behavior or authentication authority was changed. Fixing the client alone cannot make an unauthenticated legacy build pass admin-only Firestore rules.

## Deployment prerequisites for OWNER REVIEW ONLY

These commands are instructions, not actions performed by this change. Do not publish global/public writes or let a client claim an OF-ID. A new anonymous UID alone is not ownership.

1. In Firebase Console select **chatzy-d8ba4**. Under Authentication enable Anonymous when ready for supervised enrollment. Preserve installation Auth data; do not clear app data/uninstall to retry. Verify `admins/<SuperAdminAuthUID>.role` is exactly `Super Admin`. Use no administrator credentials in Android.
2. Build the reviewed backend from this Android repository, not the separate frontend-only admin console repository:

   ```powershell
   npm run build --prefix admin_web/functions
   firebase deploy --project chatzy-d8ba4 --config admin_web/firebase.json --only "functions:requestDeviceEnrollment,functions:completeDeviceEnrollment,functions:approveDeviceEnrollment,functions:listDeviceEnrollmentRequests"
   ```

   All four are HTTPS callables in **us-central1**. In particular `completeDeviceEnrollment` contains the registration fix. Normal Firebase Functions runtime identity supplies Admin SDK credentials. Review billing/IAM prerequisites independently; do not copy a service-account key into the app or frontend.
3. Review the `deviceEnrollmentRequests` index (`state ASC`, `expiresAt ASC`) in `admin_web/firestore.indexes.json`. If needed, deploy ONLY indexes using the explicit project/config and `--only firestore:indexes` after review.
4. Review device-specific rules against the currently deployed admin rules. The existing tested candidate is `security-tests/firestore.staged.rules`; no new broad rule or rule deployment was made. Keep admin reads. Device update permission must require BOTH `request.auth.token.device_id == id` AND `deviceBindings/id.authUid == request.auth.uid` AND `active == true`. Binding records and all pairing metadata must be server-owned. The owning installation needs GET access to its binding for authorization checks. Permit only existing telemetry fields (`phoneModel`, `manufacturer`, `androidVersion`, `appVersion`, `appBuildNumber`, `installationDate`, `userName`, `businessName`, `phoneNumber`, `lastSeen`, `lastSync`, `fcmToken`, `lastCommandId`, `lastCommandStatus`) on its own device; no client create/delete. Backend approval/completion creates the record. Deny client changes to admin profiles/roles, identity, activation, subscription and lock authority. **Do not deploy `admin_web/firebase.json` with `--only firestore:rules`: it still points to the old rules file.** Merge only the reviewed device/binding permissions into the live admin rules, or separately review the complete staged rules before publishing.
5. Supply the existing Super Admin approval UI for the pilot. This repository already contains `admin_web/src/components/DeviceEnrollmentApproval.tsx` integrated in Settings behind `VITE_DEVICE_ENROLLMENT_ENABLED=true`. Run that reviewed local admin frontend with genuine Chatzy Web app configuration, or separately port it into the deployed console. The deployed console repository currently has no such feature; turning on a Vercel variable alone will not add it. Do not insert binding records by hand. Compare the ENTIRE pairing ID shown by the physical phone before approving.
6. Coordinate the above before installing an enrollment build. Keep the default legacy APK for existing Local Mode users. Secure remote commands need the existing callable command transport; the unchanged legacy Worker lacks its ordering metadata. No command transport or Worker deployment is part of this registration fix.

## Phone acceptance test after review/deployment

From the Android project root, build and upgrade the same package without clearing data:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug -PchatzyDeviceEnrollment=true -PexpectedFirebaseProjectId=chatzy-d8ba4
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb logcat -v time | Select-String 'DeviceRegistration'
```

Use `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"` instead of `adb` if it is not on PATH. The Logcat tag is **[OrderFlow]:DeviceRegistration**. Read the project's own ID from Settings or the existing DeviceManager diagnostic; do not invent a new document ID.

1. Open the app and complete the existing business profile setup if required. In Settings choose **Request approval**. A Super Admin loads pending requests, compares the entire phone pairing ID and approves it, then the phone owner chooses **Check approval**. The existing background poll also recovers within roughly 30 seconds while the app process is running.
2. Verify the startup diagnostic says `project=chatzy-d8ba4 database=(default) collection=devices enrollment=true`. Check Firebase Authentication for the same installation's anonymous UID, then `deviceBindings/<OF-ID>` for that UID and `active: true`. Never log/copy an ID token or FCM token.
3. In Firestore **(default)** inspect **devices/<OF-ID>**. Confirm `lastSeen` has Firestore type **timestamp**, is recent, and appears after the approved phone completes enrollment. FCM and profile telemetry arrive on the same document. No sender or message content should be present. New devices retain the existing Suspended policy until an administrator deliberately grants access; this fix does not auto-activate subscriptions.
4. Reload https://chatzy-admin-console.vercel.app/ and confirm the same OF-ID appears in Total Devices/device list. A Suspended device can still appear in the list; status is separate from registration.
5. Record the ID and admin-controlled fields, force-stop/reopen the phone app, and confirm that same document's lastSeen advances without another document, subscription renewal, activation reset or lock change. Allow at least 15 minutes for periodic heartbeats (Android may defer background work); check again after a foreground reopen if deferred. Test disconnect/reconnect and reopen for recovery. Do not clear Auth or preferences.
6. Diagnostic interpretation: `firestore/PERMISSION_DENIED` means missing/wrong device claim/binding or unpublished device-specific rules; `functions/NOT_FOUND` means the callable is unavailable at the configured target; `functions/FAILED_PRECONDITION` usually means physical approval is pending/revoked/conflicting; `auth/ERROR_OPERATION_NOT_ALLOWED` means Anonymous Auth is disabled. `UNAVAILABLE` is a connectivity failure, not proof of rules denial. `enrollment=false` means the installed APK is still the unauthenticated legacy build. All failures should be fixed at their actual boundary, never by opening anonymous device writes.

## Local verification

- Default Android: `testDebugUnitTest assembleDebug -PexpectedFirebaseProjectId=chatzy-d8ba4` passed, 43 tests, 0 failures.
- Enrollment Android: same tasks with `-PchatzyDeviceEnrollment=true` passed, 43 tests, 0 failures. Saved as `.artifacts/registration-fix/app-debug-enrollment-review.apk` for review only. A final default `assembleDebug` passed, restoring `app/build/outputs/apk/debug/app-debug.apk` to legacy mode.
- Backend: TypeScript build and all 8 existing Functions/auth utility tests passed.
- Local Auth/Firestore: all 50 security/integration tests passed, 0 failures/skips. These use only `demo-chatzy-security` and loopback emulators, never production. They cover approved completion with a Timestamp, the dashboard's ordered query, repeated completion/heartbeat on one document, preservation of existing fields, rejection of unapproved ownership, scoped real Auth-token access, cross-device denial and revocation. They exercise the real service and Firebase SDK/rules, not a deployed callable HTTP endpoint or Android end-to-end enrollment.
- Physical phone: opened/restarted the existing installation twice, confirmed its project/build flag and observed permission denial on both starts, with the SAME `OF-5bcd4b380ae84024` ID. No replacement installation or data clearing was performed. Android emulator startup was attempted but did not reach usable boot; the helper emulator was stopped. Successful registration/dashboard checks remain blocked by the missing expected backend endpoints and the legacy installed build.
- Diff review: no UI, launcher, engine, listener, Room, Firebase configuration, build configuration or deployed rules edits. Production Functions/rules remain unchanged until owner review.

## Changed files

- `admin_web/functions/src/enrollment.ts`: authenticated completion registers presence.
- `app/src/main/java/com/orderflow/autoresponder/device/auth/DeviceEnrollment.kt`: callable failure diagnostics.
- `app/src/main/java/com/orderflow/autoresponder/device/auth/DeviceWritePolicy.kt`: server timestamp payload normalization.
- `app/src/main/java/com/orderflow/autoresponder/device/auth/DeviceRegistrationDiagnostics.kt`: new sanitized diagnostics.
- `app/src/main/java/com/orderflow/autoresponder/device/repository/FirebaseDeviceRepository.kt`: safe registration payload, timestamp conversion, reported failures.
- `app/src/main/java/com/orderflow/autoresponder/device/service/DeviceManager.kt`: early heartbeat scheduling, configuration/failure diagnostics, no subscription overwrite.
- `app/src/main/java/com/orderflow/autoresponder/device/service/HeartbeatWorker.kt`: presence-only heartbeat; preserve cancellation.
- `app/src/main/java/com/orderflow/autoresponder/device/messaging/OrderFlowFcmService.kt`: inspect failed token-write Results instead of logging success.
- `app/src/main/java/com/orderflow/autoresponder/device/model/DeviceModels.kt`: accept old numeric and new Timestamp lastSeen values.
- `app/src/test/java/com/orderflow/autoresponder/DeviceWritePolicyTest.kt`: timestamp/authority payload regression tests.
- `security-tests/enrollment.test.cjs`: presence, idempotency, preservation and authorized heartbeat integration tests.
- `CHATZY_DEVICE_REGISTRATION_FIX.md`: evidence, deployment review, acceptance steps and results.

Unrelated pre-existing `.idea` modifications and crash logs were left untouched. Local review APKs, the pulled installed APK, cloned frontend source and helper logs are under ignored `.artifacts` and are not source changes.
