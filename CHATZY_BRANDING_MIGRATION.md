# Chatzy branding and compatibility migration

Owner: SHAVIN SOLUTIONS (PVT) LTD. Product: Chatzy. Tagline: Smart Replies. Better Business.
Prepared 2026-11-09. This is a code preparation report, not a production approval.

## Architecture audit and scope
- Android `app/`: Compose/Material 3, ViewModels, domain use cases, repository interfaces, Room 6, Hilt, Retrofit/OkHttp, WorkManager, Firebase. NotificationListener -> parser -> per-conversation lock -> deduplication -> rule evaluation -> ordered RemoteInput replies, optional Cloud API fallback. DeviceManager/HeartbeatWorker and FCM service synchronize subscription/lock state independently.
- React `admin_web/`: eight existing route patterns, Firebase Auth and real-time Firestore SDK, device/subscription/user/activity/settings screens. Commands POST to the configured Cloudflare endpoint; the callable Functions implementation is an alternative, not the browser's current transport.
- `admin_web/functions/`: Admin SDK, callable sendDeviceCommand and getAdminRole, server role checks and FCM delivery. ADC derives project from deployment; no hardcoded replacement project was added.
- `admin_web/cloudflare-worker/`: Firebase JWT verification, admins role lookup, Firestore REST, OAuth service-account exchange, FCM HTTP v1. Main entry remains TypeScript. Existing Worker deployment name is retained to avoid changing the working URL.
- Separate `ADMIN app/`: independent `com.orderflow.admin` Android application, Kotlin 1.9.22/Compose compiler 1.5.8, Hilt/kapt, Firebase, DataStore, Coil, Paging, Biometrics. Not included in root settings.gradle.kts. It is a legacy admin client, not the React site. Deployment intent has not been supplied: source/configuration are left unchanged. It must participate in any future authorization cutover or be explicitly retired.

## Changed
- Chatzy Android label/dashboard/onboarding/privacy/About copy and notification branding. Blue/teal Material accents, original vector speech-bubble C identity, legacy and adaptive launchers, monochrome status icon. Old raster source art is retained but no longer referenced by active app UI/manifest.
- React login/sidebar identity, shared brand configuration, favicon/title/description, genuine company defaults and blank support details. Existing data in settings/global is not overwritten automatically.
- AppConstants centralizes Android brand/support/privacy configuration. Installed Android version comes from PackageManager; Gradle remains authoritative. npm package metadata remains authoritative for web/backend versions.
- Firebase environment examples now contain blank values; working ignored local configuration is unchanged. Android validates package/project identity in Google Services processing; optional expectedFirebaseProjectId CI guard prevents wrong-project release builds. Worker mandatory-binding checks replace the old fallback. Credential/message body logs are disabled or sanitized.
- Room destructive fallback removed, with no version/entity/DAO/migration/filename change. Current v6 data needs no migration. Unsupported historical versions fail without deleting data; historical schema recovery is a release gate.
- Isolated staged Firestore rules and emulator tests, Worker tests, read-only field-hash manifest/compare utility, compatibility verification script and migration/rollback guides.

## Preserved contract
`com.orderflow.autoresponder`, Kotlin namespace/classes, `orderflow_autoresponder.db`, Room version 6, entities/DAOs and existing 4->5 migration, SharedPreferences filename/keys, OF- device IDs and generation, notification channel and WorkManager identifiers, Firestore collection/field names, command values/statuses, all existing tests and engine behavior. Release version/signing/keystores unchanged. No deployments or remote data mutations were performed.

## Checkpoint and validation
Branch: chatzy/branding-migration. Pre-existing staged and unstaged edits were retained. A restricted source-only checkpoint and initial git status are in ignored `.artifacts/chatzy-baseline/`. It includes user's prior edits; it is not a clean HEAD snapshot. Do not use git reset/checkout to roll back the entire working tree.

See CHATZY_TEST_RESULTS.md for actual checks. Review all changed files against the source checkpoint; do not mistake the user's earlier reliability/privacy changes for this migration's changes.

## Pending
Genuine target Firebase project/config, rotated server credentials, support email/phone/site/published policy, signing owner approval and higher Play versionCode, physical-device upgrade/background/reply/FCM tests, historical Room schemas/migrations, security enrollment rollout and legal/platform review. No zero-regression guarantee is inferred from static checks.

Validation completed: 47 tests passed (33 Android, 5 Worker, 3 manifest, 6 emulator rules), React/Functions/Worker builds and Android debug/release/AAB packaging passed. Release artifacts are unsigned. See CHATZY_TEST_RESULTS.md for limits and actual commands.
