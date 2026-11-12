# Chatzy database schema and compatibility contract

Source-code inventory as of 2026-11-09. This is not a query of production. Unknown fields, subcollections and historical values must be inventoried before migration. Preserve all fields and their stored types, not only this documented subset.

## Firestore collections
| Path | Identity and ownership | Notes |
|---|---|---|
| devices/{deviceId} | Existing OF- ID; Android device telemetry; admin subscription/lock authority in future staged contract | Preserve document ID, subscription expiry and every lock alias. Current client writes are not yet least privilege. |
| deviceCommands/{commandId} | UUID command ID; Worker/Functions create, target device acknowledges | command LOCK/UNLOCK/SYNC; status PENDING/SENT/RECEIVED/SUCCESS/FAILED. Do not replay archived commands. |
| admins/{uid} | Firebase Auth UID | role exactly Super Admin/Admin/Support. Auth export/import is separate from Firestore. |
| subscriptions/{subscriptionId} | Existing history ID, usually sub_DEVICE_TIME | oldExpiryDate/newExpiryDate and daysAdded must be preserved. |
| logs/{logId} | Existing audit ID | category/title/description/performedBy; backend may add commandId/result. |
| notifications/{notificationId} | Separate Android admin notification record | Preserve targetDeviceId null for broadcasts and type/status values. No React notifications route in baseline. |
| settings/global | Global admin configuration | Existing data takes precedence over new brand defaults; operator review required. |

Timestamps in devices/commands can be epoch-millisecond numbers or Firestore Timestamp objects. Web normalization accepts both; migrate without coercion. Command backend createdAt/sentAt are Firestore Timestamps, Android processedAt is epoch ms. Nullable/missing legacy fields are supported. Never replace subscriptionEnd with the current date or reset it during re-registration. Unknown/extra fields and subcollections must survive managed export/import.

## Source field definitions
The following definitions are extracted from the existing Kotlin and TypeScript contracts. Some overlap is intentional: Android client, legacy Android admin and web normalize different optional/legacy fields. Computed UI fields such as computedStatus, hasFcmToken and online state are not evidence of a persisted field.

### DeviceInfo - app/src/main/java/com/orderflow/autoresponder/device/model/DeviceModels.kt

| Field | Kotlin type |
|---|---|
| deviceId | `String` |
| phoneModel | `String` |
| manufacturer | `String` |
| androidVersion | `String` |
| appVersion | `String` |
| appBuildNumber | `Int` |
| installationDate | `Long` |
| userName | `String` |
| businessName | `String` |
| firstSeen | `Long?` |
| lastSeen | `Long?` |
| lastSync | `Long?` |
| status | `String` |
| lockStatus | `String` |
| lockReason | `String` |
| adminLock | `Boolean` |
| adminLocked | `Boolean` |
| isLocked | `Boolean` |
| generatedUuid | `String` |
| lastCommandId | `String?` |
| lastCommandStatus | `String?` |
| fcmToken | `String` |
| phoneNumber | `String` |
| subscriptionStart | `Long?` |
| subscriptionEnd | `Long?` |
| subscriptionStatus | `String` |

### DeviceCommand - app/src/main/java/com/orderflow/autoresponder/device/model/DeviceModels.kt

| Field | Kotlin type |
|---|---|
| commandId | `String` |
| deviceId | `String` |
| command | `String` |
| status | `String` |
| processedAt | `Long?` |
| errorMessage | `String?` |

### AdminSettings - ADMIN app/app/src/main/java/com/orderflow/admin/domain/model/AdminSettings.kt

| Field | Kotlin type |
|---|---|
| companyName | `String` |
| companyLogoUrl | `String` |
| supportEmail | `String` |
| supportPhone | `String` |
| defaultSubscriptionDays | `Int` |
| notificationTemplates | `List<String>` |
| themeMode | `String` |
| autoBackupEnabled | `Boolean` |

### AdminUser - ADMIN app/app/src/main/java/com/orderflow/admin/domain/model/AdminUser.kt

| Field | Kotlin type |
|---|---|
| adminId | `String` |
| email | `String` |
| name | `String` |
| role | `String` |
| lastLogin | `Long` |
| avatarUrl | `String?` |

### DashboardStats - ADMIN app/app/src/main/java/com/orderflow/admin/domain/model/DashboardStats.kt

| Field | Kotlin type |
|---|---|
| totalDevices | `Int` |
| onlineDevices | `Int` |
| offlineDevices | `Int` |
| expiredDevices | `Int` |
| expiringSoonDevices | `Int` |
| activeTodayDevices | `Int` |
| weeklyInstalls | `List<Int>` |
| monthlyInstalls | `Int` |
| topActiveBusinesses | `List<String>` |

### Device - ADMIN app/app/src/main/java/com/orderflow/admin/domain/model/Device.kt

| Field | Kotlin type |
|---|---|
| deviceId | `String` |
| phoneModel | `String` |
| manufacturer | `String` |
| androidVersion | `String` |
| appVersion | `String` |
| userName | `String` |
| businessName | `String` |
| phoneNumber | `String` |
| installationDate | `Long` |
| activationDate | `Long` |
| subscriptionStart | `Long` |
| subscriptionEnd | `Long` |
| daysRemaining | `Long` |
| status | `String` |
| lastSeen | `Long` |
| lastSync | `Long` |
| fcmToken | `String` |
| isOnline | `Boolean` |
| generatedUuid | `String` |

### LogEntry - ADMIN app/app/src/main/java/com/orderflow/admin/domain/model/LogEntry.kt

| Field | Kotlin type |
|---|---|
| logId | `String` |
| timestamp | `Long` |
| title | `String` |
| description | `String` |
| category | `String` |
| performedBy | `String` |
| deviceId | `String?` |

### NotificationItem - ADMIN app/app/src/main/java/com/orderflow/admin/domain/model/NotificationItem.kt

| Field | Kotlin type |
|---|---|
| notificationId | `String` |
| title | `String` |
| body | `String` |
| type | `String` |
| targetDeviceId | `String?` |
| sentAt | `Long` |
| sentBy | `String` |
| status | `String` |

### Subscription - ADMIN app/app/src/main/java/com/orderflow/admin/domain/model/Subscription.kt

| Field | Kotlin type |
|---|---|
| subscriptionId | `String` |
| deviceId | `String` |
| businessName | `String` |
| updatedBy | `String` |
| updatedAt | `Long` |
| oldExpiryDate | `Long` |
| newExpiryDate | `Long` |
| daysAdded | `Int` |
| actionType | `String` |

### AdminUser - admin_web/src/types/index.ts

| Field (`?` = optional) | TypeScript type |
|---|---|
| adminId | `string` |
| email | `string` |
| name | `string` |
| role | `AdminRole` |
| lastLogin | `number` |
| avatarUrl? | `string | null` |

### Device - admin_web/src/types/index.ts

| Field (`?` = optional) | TypeScript type |
|---|---|
| deviceId | `string` |
| phoneModel | `string` |
| manufacturer | `string` |
| androidVersion | `string` |
| appVersion | `string` |
| appBuildNumber? | `number` |
| userName | `string` |
| businessName | `string` |
| phoneNumber | `string` |
| installationDate | `number` |
| activationDate | `number` |
| subscriptionStart | `number | Timestamp | null` |
| subscriptionEnd | `number | Timestamp | null` |
| daysRemaining | `number` |
| status | `DeviceStatus | string` |
| lastSeen | `number | Timestamp | null` |
| lastSync | `number | Timestamp | null` |
| firstSeen | `number | Timestamp | null` |
| fcmToken | `string` |
| isOnline? | `boolean` |
| generatedUuid | `string` |
| adminLocked? | `boolean` |

### DeviceCommand - admin_web/src/types/index.ts

| Field (`?` = optional) | TypeScript type |
|---|---|
| commandId | `string` |
| deviceId | `string` |
| command | `CommandType` |
| status | `CommandStatus` |
| createdAt | `number | Timestamp | null` |
| sentAt? | `number | Timestamp | null` |
| processedAt? | `number | Timestamp | null` |
| errorMessage? | `string | null` |
| adminUid? | `string` |
| adminEmail? | `string` |

### SubscriptionRecord - admin_web/src/types/index.ts

| Field (`?` = optional) | TypeScript type |
|---|---|
| subscriptionId | `string` |
| deviceId | `string` |
| businessName | `string` |
| updatedBy | `string` |
| updatedAt | `number` |
| oldExpiryDate | `number` |
| newExpiryDate | `number` |
| daysAdded | `number` |
| actionType | `string` |

### LogEntry - admin_web/src/types/index.ts

| Field (`?` = optional) | TypeScript type |
|---|---|
| logId | `string` |
| timestamp | `number` |
| title | `string` |
| description | `string` |
| category | `string` |
| performedBy | `string` |
| deviceId? | `string | null` |

### AdminSettings - admin_web/src/types/index.ts

| Field (`?` = optional) | TypeScript type |
|---|---|
| companyName | `string` |
| companyLogoUrl | `string` |
| supportEmail | `string` |
| supportPhone | `string` |
| defaultSubscriptionDays | `number` |
| notificationTemplates | `string[]` |
| themeMode | `string` |
| autoBackupEnabled | `boolean` |
| onlineThresholdMinutes? | `number` |

### Additional backend/write fields

- deviceCommands: commandId, deviceId, command, status, createdAt, sentAt, processedAt, errorMessage, adminUid, adminEmail.
- Backend logs may add commandId and result to LogEntry; preserve them.
- Device lock compatibility aliases include adminLock, adminLocked, isLocked, lockStatus and lockReason. DeviceManager reads legacy status variants and timestamp representations; do not normalize by deleting aliases during branding.
- Existing devices may include activationDate, daysRemaining and isOnline from admin clients; these are not uniformly written by Android DeviceInfo. Preserve unknown fields.

## Local Room (unchanged schema)

Database file: orderflow_autoresponder.db. Version: 6. exportSchema remains false to avoid a branding schema change. Existing explicit migration: 4->5. The previous destructive fallback has been removed; historical schemas are required before a tested path from older installations can be released. No synthetic 5->6 migration is invented from assumptions.

### CustomerEntity

| Field | Type |
|---|---|
| phone | `String` |
| name | `String` |
| totalMessages | `Int` |
| lastInteractionTime | `Long` |
| notes | `String` |

### MessageLogEntity

| Field | Type |
|---|---|
| id | `Long` |
| senderPhone | `String` |
| senderName | `String` |
| incomingMessage | `String` |
| replyMessage | `String` |
| ruleId | `Long?` |
| timestamp | `Long` |
| status | `String` |
| errorMessage | `String?` |

### ProcessedNotificationEntity

| Field | Type |
|---|---|
| fingerprint | `String` |
| packageName | `String` |
| sender | `String` |
| message | `String` |
| timestamp | `Long` |
| replySent | `Boolean` |
| processedAt | `Long` |

### RuleEntity

| Field | Type |
|---|---|
| id | `Long` |
| ruleName | `String` |
| keywordsCsv | `String` |
| replyMessagesJson | `String` |
| matchOption | `String` |
| delaySeconds | `Int` |
| replySequential | `Boolean` |
| isActive | `Boolean` |
| isPauseForContact | `Boolean` |
| pauseDurationMinutes | `Int` |
| priority | `Int` |
| caseSensitive | `Boolean` |
| enabledForGroups | `Boolean` |
| enabledForPrivate | `Boolean` |
| replyToSpecificContacts | `Boolean` |
| specificContactsCsv | `String` |
| excludeSpecificContacts | `Boolean` |
| excludedContactsCsv | `String` |
| initialDelaySeconds | `Int` |
| createdAt | `Long` |
| businessHoursEnabled | `Boolean` |
| startHour | `Int` |
| startMinute | `Int` |
| endHour | `Int` |
| endMinute | `Int` |
| activeDaysCsv | `String` |

### RuleMessageEntity

| Field | Type |
|---|---|
| id | `Long` |
| ruleId | `Long` |
| message | `String` |
| position | `Int` |
| isEnabled | `Boolean` |

Room tables: auto_reply_rules, rule_messages, customers, message_logs, processed_notifications. Preserve primary keys, reply positions, foreign-key cascade behavior and indexes. rule_messages.ruleId references auto_reply_rules.id. replyMessagesJson is a retained legacy column; do not drop it for branding. No migration runs for same-schema v6 upgrades.

## Preferences and identifiers

Preferences filename orderflow_secure_prefs and all existing keys remain unchanged, including device_id, fcm_token, profile flags, subscription status/end, admin lock/reason, processed command IDs, responder/cloud-mode flags and Meta credentials. Preserve OF- generation and the cached device ID on upgrade. Existing Disconnect Device explicitly resets registration; never use it as a Firebase migration step. Local logs and cache controls do not delete cloud records.

## Validation

Use managed export/import for actual data backup/move, Auth export/import for UIDs, and the read-only manifest utility for collection counts and per-field hash comparison. Snapshot stable source/target windows before FCM token refresh. Document expected telemetry/token drift separately with operator approval; never exclude critical subscription/role/lock fields silently. A successful comparison cannot establish correct live authorization or remote delivery.

Manifest limitation: SDK JavaScript numbers do not distinguish Firestore integer/double wire encodings. Managed export/import remains authoritative for stored types. Reference hashes include project identity; investigate cross-project references explicitly.


## Prepared device enrollment metadata (not deployed)
See CHATZY_DEVICE_ENROLLMENT.md for server-owned deviceEnrollmentRequests/{UUID}, deviceEnrollments/{Auth UID}, and deviceBindings/{OF-ID}. Existing collections/IDs remain unchanged. Functions add server-owned desiredCommandId to device records. No Room schema/version changes are required.
