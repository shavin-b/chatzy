> Chatzy migration note (2026-11-09): this historical review is not a production sign-off. See CHATZY_RELEASE_CHECKLIST.md, CHATZY_SECURITY_REVIEW.md and CHATZY_TEST_RESULTS.md. Company: SHAVIN SOLUTIONS (PVT) LTD. Support contact and published policy URL remain pending.

# Foreground Service Technical Review

This document provides the technical analysis and Play Console declaration details for the foreground service used in **Chatzy Ã¢â‚¬â€œ WhatsApp Local Auto Responder**.

---

## 1. Technical Audit Summary

| Component | Technical Details |
| :--- | :--- |
| **Service Class** | `com.orderflow.autoresponder.service.WhatsAppNotificationListener` |
| **Base Class** | `android.service.notification.NotificationListenerService` |
| **Foreground Service Type** | `remoteMessaging` (`android:foregroundServiceType="remoteMessaging"`) |
| **Manifest Permissions** | `android.permission.FOREGROUND_SERVICE`<br>`android.permission.FOREGROUND_SERVICE_REMOTE_MESSAGING` |
| **Protected Service Permission** | `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` |
| **Notification Channel** | `orderflow_engine_channel` (`IMPORTANCE_LOW`) |
| **Ongoing Notification Title** | "Chatzy Engine Active" |
| **Ongoing Notification Text** | "Monitoring WhatsApp Business notifications..." |

---

## 2. Technical Justification & Necessity

### Why Foreground Execution is Required
While Android's `NotificationListenerService` is a system-managed service, non-foreground listener processes are frequently terminated by aggressive OEM power management systems (e.g., Xiaomi MIUI/HyperOS, Oppo ColorOS, Vivo Funtouch, Huawei EMUI, Samsung OneUI) during low-memory conditions or Doze mode.

Promoting `WhatsAppNotificationListener` to a persistent foreground service guarantees that:
1. The IPC (Inter-Process Communication) connection between the Android system notification manager and Chatzy remains active when the screen is off or the app is in the background.
2. Auto-reply rules evaluate immediately upon receiving a WhatsApp Business notification.
3. Automated responses are dispatched without delay via Android `RemoteInput`.

---

## 3. Runtime Lifecycle & Behavior

* **Start Mechanism**: Invoked automatically in `onCreate()` when the service is bound by the Android system or rebound by `NotificationServiceWatchdog`.
* **API Compatibility**:
  * On **Android 14+ (API 34Ã¢â‚¬â€œ36)**, `startForeground()` is called with `ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING`.
  * On **Android 13 and below**, `startForeground()` is called with standard notification.
* **Service Recovery**: If the service is terminated or task removed, `onTaskRemoved()` and `NotificationServiceWatchdog` trigger `requestRebind()` to restore listener connectivity.
* **Stop Mechanism**: The service stops if the user disables Notification Access in Android system settings or disables the master toggle in Chatzy.

---

## 4. Data Processing & External Transmission

* **Processed Data**: Incoming WhatsApp Business notification text, sender contact name/ID, conversation title.
* **Processing Location**: 100% on-device local processing.
* **External Transmission**: **NONE**. The foreground service does not upload notification text or message content to any remote server or external service.

---

## 5. Required Google Play Console Declaration

When submitting the application in Google Play Console, enter the following responses for the **Foreground Services Declaration**:

1. **Service Type Selected**: `Remote Messaging` (`FOREGROUND_SERVICE_REMOTE_MESSAGING`).
2. **Video / Use-Case Demonstration Description**:
   > "Chatzy is a local WhatsApp auto-responder app. The foreground service (remoteMessaging type) maintains a persistent listener connection with the Android system to detect incoming WhatsApp notifications and dispatch automated replies in real time while the phone is idle or in doze mode."
3. **Core Feature Justification**:
   > "Without persistent foreground service execution, aggressive OEM battery management terminates the notification listener process in the background, causing missed incoming messages and failed auto-replies."
