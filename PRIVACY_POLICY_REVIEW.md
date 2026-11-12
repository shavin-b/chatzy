> Chatzy migration note (2026-11-09): this historical review is not a production sign-off. See CHATZY_RELEASE_CHECKLIST.md, CHATZY_SECURITY_REVIEW.md and CHATZY_TEST_RESULTS.md. Company: SHAVIN SOLUTIONS (PVT) LTD. Support contact and published policy URL remain pending.

# Privacy Policy Source Code Audit & Review

This document summarizes the technical source-code audit conducted for the **Chatzy Ã¢â‚¬â€œ WhatsApp Local Auto Responder** Privacy Policy (`privacy-policy.html`).

---

## VERIFIED DATA PRACTICES

Source-code verification across the repository confirms the following actual data handling behavior:

* **WhatsApp Notification Data**: Accessed via Android `NotificationListenerService` (`WhatsAppNotificationListener`). Read and evaluated locally on-device against rules stored in Room SQLite database.
* **On-Device Message Processing**: Incoming notification text, sender contact name/ID, and conversation title are processed 100% locally on-device. The local notification processing path does not upload notification text to Firestore. Optional Cloud API replies transmit resolved reply content and recipient identifiers to Meta, including incoming-message variables if configured.
* **Device Telemetry & Status**: Device ID (`OF-<android_id>`), FCM push token, phone brand/model, OS/app version, business name/number, lock/subscription status, and status heartbeats are transmitted over HTTPS/TLS to Google Firebase (Firestore & FCM) for remote device management.
* **Local Data Storage**: Auto-reply rules, execution logs (`message_logs`), notification deduplication cache (`processed_notifications`), and customer contacts are saved in the local Room SQLite database inside Android's private app sandbox.

---

## LOCAL MODE

* **Workflow**: `Incoming WhatsApp Notification &rarr; WhatsAppNotificationListener &rarr; ProcessIncomingMessageUseCase &rarr; EvaluateRuleUseCase &rarr; LocalReplyHelper (RemoteInput Reply)`
* **Off-Device Transmission**: Local message evaluation remains on-device, while Firebase receives registration/profile/telemetry data. Optional Meta mode and SDK analytics require separate disclosures.
* **Meta API Isolation**: Automatic local replies do not require Meta credentials. Explicit setup or connection-test actions can still contact Meta.

---

## FIREBASE / FCM

The following specific fields are transmitted over HTTPS/TLS to Google Firebase (Firestore & FCM) via `FirebaseDeviceRepository` and `DeviceManager`:

* `deviceId` / `generatedUuid` (Persistent device identifier)
* `fcmToken` (Firebase Cloud Messaging push token)
* `phoneModel`, `manufacturer`, `androidVersion`, `appVersion`, `appBuildNumber`, `installationDate`
* `userName`, `businessName`, `phoneNumber` (Configured business profile)
* `adminLocked`, `lockStatus`, `lockReason`, `subscriptionStatus`
* `lastSeen`, `lastSync` (Status heartbeat)
* `commandId`, `command`, `status`, `processedAt` (Remote command acknowledgements)

**Verification**: WhatsApp message content, notification body text, and customer conversation logs are **EXCLUDED** from all Firebase data structures.

---

## THIRD-PARTY SERVICES

The following active third-party infrastructure services are integrated into the application:

* **Google Firebase Firestore**: Remote device registration, status monitoring, and command synchronization.
* **Firebase Cloud Messaging (FCM)**: Real-time push notification delivery for administrative commands.

Firebase Analytics is included as a dependency. Verify actual SDK data collection and consent/disclosure settings; do not claim analytics is absent. No advertising or data broker integration was identified in the audited application code.

---

## META API

Chatzy contains preserved architecture for the Meta WhatsApp Cloud API (`MetaWhatsAppApiService`, `MetaCredentialFetchService`, `WhatsAppCloudApiRepositoryImpl`, `SendCloudReplyUseCase`, `MetaCredentialsWizard`, etc.).

* **Current Status**: Completely inactive in default Local Mode (`useCloudApi = false`).
* **Credentials**: No Meta access tokens, Phone Number IDs, or Meta credentials are required or transmitted during Local Mode operation.

---

## USER DATA DELETION

In-app controls under `Settings &rarr; Data & Device Management` execute the following code paths:

* **Clear Logs & Cache**: Calls `messageLogRepository.clearLogs()` and `processedNotificationRepository.clearAllNotifications()`, permanently deleting local `message_logs` and `processed_notifications` records from the Room SQLite database.
* **Disconnect Device**: Clears the persistent `deviceId` in `SecureStorage` (SharedPreferences) and re-initializes registration with Firebase.

---

## PUBLIC URL TO USE

Intended GitHub Pages public URL:

`[PUBLISHED_PRIVACY_POLICY_URL_PENDING]`

*(Note: Mark as intended URL until GitHub Pages publishing and verification are completed).*

---

## APP CONSTANT TO UPDATE

The public Privacy Policy URL is centralized in:

`com.orderflow.autoresponder.core.util.AppConstants.PRIVACY_POLICY_URL`

Current code value in [`AppConstants.kt`](file:///C:/Users/ASUS/Downloads/photo/Chatzy_AutoResponder_Local_Engine/app/src/main/java/com/orderflow/autoresponder/core/util/AppConstants.kt):

`const val PRIVACY_POLICY_URL = "" // pending verified publication`

Upon publishing `privacy-policy.html` to GitHub Pages, `AppConstants.PRIVACY_POLICY_URL` can be updated to point to the live HTML page URL:

`[PUBLISHED_PRIVACY_POLICY_URL_PENDING]`

---

## REMAINING MANUAL STEPS

1. Commit and push `privacy-policy.html` to the repository `[PUBLISHED_PRIVACY_POLICY_URL_PENDING]`.
2. Enable and configure **GitHub Pages** in the repository settings (Source: `main` branch or `/root`).
3. Verify the published URL (`[PUBLISHED_PRIVACY_POLICY_URL_PENDING]`) in a web browser.
4. Update `AppConstants.PRIVACY_POLICY_URL` in [`AppConstants.kt`](file:///C:/Users/ASUS/Downloads/photo/Chatzy_AutoResponder_Local_Engine/app/src/main/java/com/orderflow/autoresponder/core/util/AppConstants.kt) with the verified live URL.
5. Rebuild and test the application (`./gradlew app:assembleDebug`).
6. Enter the verified live Privacy Policy URL into Google Play Console's Store Listing settings.
7. Confirm that the Privacy Policy and Data Safety declarations in Google Play Console remain 100% consistent with `PLAY_STORE_DATA_SAFETY_NOTES.md`.


## Current Chatzy privacy review (2026-11-09)
The HTML is now an explicit publication draft owned by SHAVIN SOLUTIONS (PVT) LTD. It explains notification access, local Room processing, Firebase profile/device telemetry, FCM routing, optional Meta reply transmission, Analytics dependency, retention/deletion limitations and unconfigured business contact details. AppConstants.PRIVACY_POLICY_URL is blank; the app falls back to its in-app policy. No website or email has been invented or published. Supersede historical absolute-local/no-analytics claims with these findings. Final retention schedule, real contacts, SDK traffic review and legal/platform assessment remain pending.
