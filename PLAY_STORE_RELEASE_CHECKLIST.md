> Chatzy migration note (2026-11-09): this historical review is not a production sign-off. See CHATZY_RELEASE_CHECKLIST.md, CHATZY_SECURITY_REVIEW.md and CHATZY_TEST_RESULTS.md. Company: SHAVIN SOLUTIONS (PVT) LTD. Support contact and published policy URL remain pending.

# Google Play Store Release Checklist

This document details the configuration parameters, release tasks, and submission steps for releasing **Chatzy Ã¢â‚¬â€œ WhatsApp Local Auto Responder** on Google Play Store.

---

## 1. Release Parameters & Configuration Audit

- **Application ID**: `com.orderflow.autoresponder`
- **Version Name**: `1.0.0`
- **Version Code**: `1`
- **Compile SDK**: `36`
- **Target SDK**: `36` (Android 15 / 16 compliant)
- **Min SDK**: `23` (Android 6.0+)
- **Default Operating Mode**: `Local Mode` (Direct Notification Reply via `RemoteInput`)

---

## 2. In-App Disclosures & Privacy Policy Setup

- [x] **Notification Access Disclosure**: In-app disclosure dialog implemented in `SettingsScreen.kt` displaying clear purpose and affirmative consent (`I Understand - Enable Access`) before launching system settings.
- [x] **In-App Privacy Policy**: In-app policy dialog available in Settings describing Local Processing, Firebase Sync, and optional Meta API capability.
- [ ] **External Public Privacy Policy URL**:
  - The Privacy Policy URL (`[PUBLISHED_PRIVACY_POLICY_URL_PENDING]`) is centralized in `AppConstants.PRIVACY_POLICY_URL`.
  - Enter this live URL into Google Play Console prior to submission.

---

## 3. Permissions & Declarations Audit

- [x] `BIND_NOTIFICATION_LISTENER_SERVICE` (Core permission for detecting WhatsApp notifications)
- [x] `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_REMOTE_MESSAGING` (Declared for background service reliability)
- [x] `POST_NOTIFICATIONS` (Android 13+ notification posting)
- [x] `INTERNET` / `ACCESS_NETWORK_STATE` (Firebase device status sync)
- [x] `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (Optional background battery optimization prompt)

---

## 4. Release AAB Generation Instructions

To build the signed release Android App Bundle (AAB):

1. Open Android Studio.
2. Select **Build -> Generate Signed Bundle / APK...**
3. Choose **Android App Bundle** and click **Next**.
4. Select your production keystore file, key alias, and enter passwords.
5. Choose destination folder and select build variant **release**.
6. Click **Create**.
7. Locate the generated `.aab` file in `app/release/`.

---

## 5. Play Console Submission Checklist

1. **Upload AAB**: Upload the generated `.aab` file to **Production** or **Internal Testing** track in Google Play Console.
2. **App Content & Declarations**:
   - Complete **Data Safety Form** using guidelines in `PLAY_STORE_DATA_SAFETY_NOTES.md`.
   - Complete **Permissions Declaration** for Notification Listener & Foreground Service (`remoteMessaging`).
   - Provide link to live Privacy Policy URL.
3. **App Review Instructions**:
   - Copy testing steps from `REVIEWER_INSTRUCTIONS.md` into the **App Access / Testing Instructions** field in Play Console.
