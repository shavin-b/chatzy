> Chatzy migration note (2026-11-09): this historical review is not a production sign-off. See CHATZY_RELEASE_CHECKLIST.md, CHATZY_SECURITY_REVIEW.md and CHATZY_TEST_RESULTS.md. Company: SHAVIN SOLUTIONS (PVT) LTD. Support contact and published policy URL remain pending.

# Google Play Store Data Safety Notes

This document provides exact guidelines for completing the **Data Safety Form** in Google Play Console for **Chatzy Ã¢â‚¬â€œ WhatsApp Local Auto Responder**, based on a source-code audit.

---

## 1. Overview of Data Handling Architecture

Chatzy operates primarily in **Local Mode**. Notification processing and auto-reply evaluation occur entirely on the user's Android device. Device telemetry and remote administration status are synchronized over HTTPS with Google Firebase (Firestore & FCM).

---

## 2. Currently Collected & Processed Data

### A. WhatsApp Notifications & Messages (On-Device Local Processing)
* **Data Types**: Notification text, sender contact name, sender phone number, conversation title.
* **Processed On-Device**: Yes. Read by `WhatsAppNotificationListener` and evaluated by `ProcessIncomingMessageUseCase` against local rules. Auto-replies are dispatched via Android `RemoteInput`.
* **Stored On-Device**: Yes. Stored locally in Room SQLite database (`message_logs`, `processed_notifications`, `customers`).
* **Transmitted Off-Device**: **NO**. Source code verification confirms notification contents and message text are never uploaded to any remote server or shared with third parties.
* **Data Safety Declaration**: **Not Collected / Not Shared** off-device.

### B. Device & Diagnostic Identifiers (Firebase Communications)
* **Data Types**: Persistent Device ID (`deviceId`), FCM Push Token (`fcmToken`), Phone Manufacturer & Model (`manufacturer`, `phoneModel`), Android OS Version (`androidVersion`), App Version & Build Number (`appVersion`, `appBuildNumber`), Business Name & WhatsApp Number (`businessName`, `phoneNumber`), Subscription/Lock Status, Command Acknowledgements.
* **Transmitted Off-Device**: **YES** (to Google Firebase Firestore & FCM over HTTPS/TLS).
* **Purpose**: App Functionality, Account/Device Management, Remote Administration Commands (Lock/Unlock), FCM Push Synchronization.
* **Data Safety Declaration**:
  * **Collected**: Yes.
  * **Shared**: No (used solely for app operation via Google Firebase infrastructure).
  * **Ephemeral / Stored**: Stored securely in Firestore.
  * **Encryption in Transit**: Transmitted over HTTPS/TLS.

### C. Local Storage & User Data Controls (Room Database & SharedPreferences)
* **Data Types**: Message execution logs, notification deduplication cache, auto-reply rules, customer contact records.
* **Transmitted Off-Device**: **NO**. Saved in local Room SQLite database on the device.
* **User Control**:
  * **Clear Logs & Cache**: Clears `message_logs` and `processed_notifications` tables.
  * **Disconnect Device**: Resets local device registration ID and triggers clean registration sync.

---

## 3. Preserved / Optional Capabilities (Meta WhatsApp Cloud API)

* **Meta API Credentials**: Access Token, Phone Number ID, Business Account ID, Webhook Token.
* **Storage**: Stored in `SecureStorage` (SharedPreferences) only if explicitly configured by the user.
* **Local Mode Execution**: In default Local Mode (`useCloudApi = false`), no Meta APIs are called, no credentials are transmitted, and Meta setup is not required.

---

## 4. Summary Table for Play Console Data Safety Form

| Data Category | Data Type | Collected Off-Device? | Shared? | Purpose | Encrypted in Transit? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Messages** | Notification text, sender contact name | **No** (On-Device Only) | No | App Functionality (Local Auto-Reply) | N/A (On-Device Only) |
| **Device IDs** | Device ID, FCM Push Token | **Yes** (Firebase) | No | App Functionality & Device Admin | Yes (HTTPS/TLS) |
| **App Info** | App version, OS version, status heartbeat | **Yes** (Firebase) | No | Analytics & Service Diagnostics | Yes (HTTPS/TLS) |
| **Personal Info**| Business name, business phone number | **Yes** (Firebase) | No | Account/Device Management | Yes (HTTPS/TLS) |
