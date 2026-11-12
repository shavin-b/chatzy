# Chatzy device visibility: verified failure and local fixes

## Fresh on-device result

The Samsung phone is connected and authorized again. Reopening the actual app
reproduced these sanitized diagnostics for its existing ID:

```text
project=chatzy-d8ba4 database=(default) collection=devices enrollment=false
operation=write path=devices/OF-5bcd4b380ae84024 code=firestore/PERMISSION_DENIED
operation=observe path=devices/OF-5bcd4b380ae84024 code=firestore/PERMISSION_DENIED
operation=fcm-sync path=devices/OF-5bcd4b380ae84024 code=firestore/PERMISSION_DENIED
```

A test-only instrumentation probe then used this same installation's Firebase
Auth identity and the real Firestore/Functions SDKs:

```text
project=chatzy-d8ba4
hasAuthIdentity=true
anonymous=true
tokenAudienceMatches=true
deviceClaimMatches=false
enrollmentEnabled=false
deviceReadCode=PERMISSION_DENIED
completion=functions/NOT_FOUND
```

**Sign-in works; device-scoped authorization does not.** The phone has no matching
`device_id` claim, its Firestore access is denied, and the trusted enrollment
completion route is unavailable. The probe tried the existing server-authorized
completion route; it did not approve itself, forge claims or bypass permissions.
It could not complete registration.

This is a verified registration/authorization failure in the phone's path. It is
not a guess based on a separate browser startup error or an incorrect Android
Firebase file. The supplied google-services.json is still unchanged and correct
for project chatzy-d8ba4, Android package com.orderflow.autoresponder.

No authenticated administrator read of the live device document was available.
Because the phone's read is denied, this does not prove whether that production
document is absent or already exists without recent telemetry.

## Code fixes completed locally

1. `admin_web/src/services/devices.ts`: subscribe to the devices collection without
   server-side orderBy(lastSeen), then sort normalized lastSeen locally. Firestore
   orderBy silently omitted existing documents without that field. Those documents
   now appear as never-seen/offline instead of disappearing. Numeric and Timestamp
   values remain compatible; no fields are fabricated or written by this change.
2. `admin_web/src/hooks/useDevices.ts`: distinguish permission-denied and network
   errors, record only sanitized error codes, and avoid presenting stale data as
   successful counts.
3. `Dashboard.tsx` and `Devices.tsx`: database errors no longer also show
   "No devices registered yet" or a misleading zero registered count. The error
   stays visible and counts are marked unavailable.
4. Added regressions against the real device service, hook and server-rendered
   React pages for missing-lastSeen records, sorting and read-error handling.
5. Added the test-only `DeviceRegistrationProbe.kt` and an explicit
   `-e deviceRegistration true` runner option. It inspects only this installation's
   ID, relevant authorization booleans and server codes; never credentials,
   customer records, support text, logs or FCM token values.

These dashboard changes are built locally, **not published to Vercel**. They
remove real visibility/error-reporting bugs but cannot grant device permissions.

## Remaining production fix

The prepared registration backend must be deployed to **chatzy-d8ba4**, followed by
its existing device approval/authorization flow. The Firebase CLI account available
here still fails to list that project's functions. An authorized CLI account is
required before deployment can be completed. No production rules, account roles
or bindings were changed by this task.

After signing in through the normal authorized Firebase CLI flow, build and deploy
only the reviewed enrollment functions from this workspace:

```powershell
npm --prefix admin_web/functions run build
firebase deploy --project chatzy-d8ba4 --config admin_web/firebase.json --only "functions:requestDeviceEnrollment,functions:completeDeviceEnrollment,functions:approveDeviceEnrollment,functions:listDeviceEnrollmentRequests"
```

Do not deploy the old permissive rules referenced by admin_web/firebase.json.
Coordinate the tested device-scoped rules, existing Super Admin approval UI and
Android enrollment build as described in CHATZY_DEVICE_REGISTRATION_FIX.md. An
Anonymous identity alone is not ownership of a device ID. The existing flow
compares a physical pairing ID before issuing the scoped claim. Preserve the
existing ID, profile, subscriptions, lock state and app data.

Publish the reviewed frontend fixes through the actual dashboard deployment
repository/project. Merely changing local files does not update Vercel.

No billing changes, unauthenticated device writes, manual self-approval or data
resets were performed. The default app was not blindly switched to enrollment mode
while its completion endpoint is missing.

## Verification

- Android debug build, test APK build and 50 unit tests passed.
- Updated debug and test APKs installed successfully, keeping app data.
- Real device probe ran successfully and exposed the denied authorization/missing
  completion endpoint above. Its success status means the probe completed, not
  that registration succeeded.
- Dashboard: 14 regression tests passed; TypeScript/Vite production build passed.
- Lint: no errors; two existing Fast Refresh export warnings.
- Auth/Firestore emulator suite exercises registration, approval, scoped access,
  revocation, preserved authority fields, and both ordered-query exclusion and
  unfiltered collection inclusion. No production data are used.

The device appearing in the signed-in live admin panel is **not yet verified**.
The remaining blocker is deploying/authorizing the registration backend with
an account that can manage chatzy-d8ba4, not another Firebase key replacement.

Final emulator result for this run: **52 passed, 0 failed, 0 skipped**.
