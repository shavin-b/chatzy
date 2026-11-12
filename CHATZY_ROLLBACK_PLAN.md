# Chatzy rollback plan

No production traffic or data was changed by this task. Branding and Firebase cutover must remain separate releases.

## Workspace rollback
The branch chatzy/branding-migration contains uncommitted work, including user changes that predate this task. The ignored source-only checkpoint at .artifacts/chatzy-baseline/files/ captures those initial source versions. Review the manifest/status and restore only files changed by this migration, one at a time, preserving subsequent user edits. Newly introduced Chatzy files can be removed only after review. Do not use git reset --hard, git clean or whole-tree checkout. The rejected broad checkpoint was not used; no credential backup was made.

## Branding release rollback
Keep the previously approved signed binary, build metadata and release notes outside the repo. Android normally cannot install a lower versionCode update: create a forward release with higher versionCode using the same package and original signing key. Do not uninstall/clear data, change the database filename, re-enable destructive migration or regenerate OF- IDs. Verify restored UI over current Room v6 data. Unsupported older schemas must remain protected and require the historical migration gate.

## Firebase cutover rollback (future manual operation)
- Keep legacy Firebase project, Auth, Worker endpoint, Functions and backups available. Do not revoke all legacy runtime credentials accidentally while rotating the compromised key; replace each service's credential with a scoped new one.
- Before switching a pilot, record its device ID, subscriptions, admin locks, command state and data validation manifest. Retain each environment's config securely in separate CI/workspaces.
- If health/authorization/command acknowledgement or data checks fail, stop expanding the cohort and freeze admin writes. Restore the web env to its approved matching legacy project/Worker and deploy only with explicit approval.
- A web config rollback does NOT move installed target-configured Android clients. Supply a higher-version signed rollback update with legacy Google Services config, preserving Room/preferences/OF- ID. Refresh/upload FCM tokens for the restored sender and reauthenticate admins against the matching project.
- Reconcile subscription/expiry/lock changes made after cutover before resuming writes. Use approved per-record reconciliation; never blindly import a target backup over live legacy data. Do not replay old remote commands.
- Validate source/target counts and critical field hashes, test commands and monitor acknowledgement lag. Record the incident and retention/access decisions for migration artifacts.

No destructive rollback commands or automatic production migrations are included.
