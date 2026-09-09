# OrderFlow Admin Console (Android)

**OrderFlow Admin Console** is a production-ready Android application built for administrators to monitor, manage, and control all installed OrderFlow client applications in real time via **Firebase Cloud Firestore** and **Firebase Cloud Messaging**.

---

## Technical Stack & Architecture

- **Language**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose with Material Design 3 (Dark & Light Glassmorphism Theme)
- **Architecture**: MVVM + Clean Architecture (Presentation, Domain, Data, Core, DI)
- **Dependency Injection**: Dagger Hilt 2.50
- **Asynchronous & Streams**: Kotlin Coroutines & Flow (Real-time Firestore `callbackFlow` snapshot listeners)
- **Backend & Database**: Firebase Auth, Cloud Firestore, Firebase Cloud Messaging (FCM)
- **Security & Storage**: Preferences DataStore, AndroidX Biometrics
- **UI Libraries**: Canvas Custom Charts (Pie/Donut, Bar, Line), Coil Image Loader, Timber Logging

---

## Database Collections Schema (`Firestore`)

1. **`admins/`**: Administrator profiles (`adminId`, `email`, `name`, `role`, `lastLogin`)
2. **`devices/`**: Client application installations:
   - `deviceId`, `phoneModel`, `manufacturer`, `androidVersion`, `appVersion`, `userName`, `businessName`, `phoneNumber`, `installationDate`, `activationDate`, `subscriptionStart`, `subscriptionEnd`, `daysRemaining`, `status` (`Active`, `Expiring Soon`, `Expired`, `Suspended`, `Offline`, `Uninstalled`), `lastSeen`, `lastSync`, `fcmToken`, `isOnline`, `generatedUuid`
3. **`subscriptions/`**: Subscription update history & logs
4. **`logs/`**: Audit activity timeline
5. **`notifications/`**: FCM push notifications queue and broadcast logs
6. **`settings/`**: Global console parameters & defaults

---

## Features

- **Dashboard**:
  - Live statistics grid (Total Devices, Online, Offline, Expired, Expiring Soon, Active Today).
  - Custom Compose Canvas Donut Chart for Subscription Distribution.
  - Custom Compose Canvas Bar Chart for Weekly Installations.
  - Recent Client Devices feed.
- **Searchable & Filterable Device List**:
  - Multi-attribute search (Business Name, Phone Number, Device ID, Owner Name, App Version).
  - One-tap status filters (Only Active, Only Expired, Only Offline, Only Online, Expiring Soon).
  - Shimmer loading state & empty state illustrations.
- **Device Detail View**:
  - Full hardware and software specifications.
  - Generated UUID IMEI alternative.
  - Real-time online/offline connection heartbeat indicator.
  - Quick action subscription buttons (+7, +30, +90, +180, +365 Days, Lifetime).
- **Subscription Editor**:
  - Custom day modifier and preset extensions.
  - Instant recalculation of days remaining & status without app restart.
  - Real-time Firestore sync trigger to client applications.
- **Push Announcement Broadcast**:
  - Send targeted or global FCM push notifications (Announcement, Maintenance Notice, Subscription Reminder, Custom Message).
- **Audit Logs Timeline**:
  - Color-coded activity feed of all admin and system transactions.
- **Security & Biometrics**:
  - Firebase Authentication, Remember Login preference via DataStore, Fingerprint/Face Biometric Quick Login.

---

## How to Build & Run

1. Open the project folder in **Android Studio Jellyfish / Ladybug (2024.1+)**.
2. Replace `app/google-services.json` with your real Firebase Project configuration file from the Firebase Console.
3. Deploy Firestore rules using the included `firestore.rules` file:
   ```bash
   firebase deploy --only firestore:rules
   ```
4. Build and run on an Android Device or Emulator running Android 8.0 (API 26) or higher.
