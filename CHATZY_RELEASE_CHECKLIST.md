# Chatzy release checklist

Status: preparation only; commercial release and Firebase cutover are blocked on the unchecked gates below.

## Compatibility
- [ ] Owner reviews migration diff against source checkpoint, preserving pre-existing user edits.
- [ ] Same package/applicationId and production signing key; assign an approved higher versionCode before distributing an update (current version unchanged).
- [ ] Same database filename/version, entities/DAOs, preferences keys, OF- ID, service/channel/work identifiers.
- [ ] Recover historical v4/v5 and any earlier installed schemas, implement and test complete nondestructive paths to v6. Do not reinstate destructive fallback.
- [ ] Install an upgrade over representative existing devices with known rules/messages/customers/logs; compare rows, preferences, OF- IDs and subscriptions before/after.
- [ ] Exercise notification permission denial/grant/revoke, notification parsing, keywords/match types, multi-reply order/delays, dedup/locks, group/private behavior and optional Cloud API.
- [ ] Exercise reboot, idle/Doze, OEM battery behavior, watchdog/WorkManager and notification-listener rebinding on physical devices.
- [ ] Exercise remote LOCK/UNLOCK/SYNC and mid-sequence cancellation with actual FCM acknowledgement.

## Build and web
- [ ] Review actual results in CHATZY_TEST_RESULTS.md; no failed/skipped step may be represented as passed.
- [ ] Clean Gradle unit/debug/release/bundle build; inspect merged manifest and icons on API 23 and adaptive-icon devices.
- [ ] Signed release APK/AAB validation under existing signing ownership; no keystore or password changes.
- [ ] React lint/type/build, route navigation, live Auth/roles, dashboard totals/chart, search/filter, subscription extension/custom days/history and commands verified in browser.
- [ ] Functions build and runtime load on Node 20; Worker type/test/dry-run; emulator authorization tests; isolated-target integration tests.

## Privacy, security and operations
- [ ] Revoke compromised key and rotate runtime secrets; investigate previous exposure.
- [ ] Supply genuine support email/phone, website and published privacy-policy URL; replace draft placeholders in AppConstants/web env and publish reviewed HTML.
- [ ] Validate Analytics SDK collection and optional Meta mode; accurately complete Google Play Data Safety declarations and prominent disclosure/consent.
- [ ] Resolve open production authorization vulnerabilities and unauthenticated message-based control before commercial release, with backward-compatible enrollment rollout.
- [ ] Complete platform/business/legal assessment; independent branding alone does not establish WhatsApp/Play approval.
- [ ] Set retention/deletion procedures, support ownership, billing/annual renewal policy and any relevant Play payment requirements. No payment gateway, auto-renewal or purchase verification was invented in this rebrand.
- [ ] Provision genuine Chatzy Firebase/FCM/Auth/Web/Worker/Functions resources, migrate Auth UIDs, indexes and roles; validate backups/counts/field hashes.
- [ ] Test target FCM re-registration and old-project coexistence; retain rollback environment and reconcile subscription/lock changes.
- [ ] Explicit owner approval for any deployment or production data migration. None performed by this task.
