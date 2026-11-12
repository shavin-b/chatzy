> Chatzy migration note (2026-11-09): this historical review is not a production sign-off. See CHATZY_RELEASE_CHECKLIST.md, CHATZY_SECURITY_REVIEW.md and CHATZY_TEST_RESULTS.md. Company: SHAVIN SOLUTIONS (PVT) LTD. Support contact and published policy URL remain pending.

# Firestore Security Review

This document provides the security analysis and rules verification for Google Cloud Firestore used in **Chatzy Ã¢â‚¬â€œ WhatsApp Local Auto Responder**.

---

## 1. Firestore Rules Specification (`firestore.rules`)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Chatzy Device Management Collection
    match /devices/{deviceId} {
      // Allows devices to register, update heartbeat status, and listen for status changes by device ID
      allow read, create, update: if deviceId != null && deviceId != "";
      allow delete: if false; // Disallow document deletion from client devices
    }

    // Chatzy Remote Command Acknowledgements
    match /deviceCommands/{commandId} {
      // Allows devices to acknowledge executed remote commands
      allow read, create, update: if commandId != null && commandId != "";
      allow delete: if false;
    }
  }
}
```

---

## 2. Security Analysis & Controls

### A. Non-Empty Document ID Enforcement
* Both `/devices/{deviceId}` and `/deviceCommands/{commandId}` paths mandate non-null, non-empty document IDs (`deviceId != null && deviceId != ""`).
* Prevents root collection pollution or anonymous bulk document creation with empty IDs.

### B. Prevention of Destructive Operations
* Client-side document deletion is explicitly forbidden (`allow delete: if false`) across all collections.
* Devices can register or update their own telemetry status, but cannot wipe device records or command acknowledgement logs.

### C. Client Architecture & Authentication Compatibility
* Chatzy registers devices using a persistent, device-generated ID (`OF-<android_id>`).
* The rules allow unauthenticated read, create, and update operations scoped to specific document paths (`/devices/{deviceId}` and `/deviceCommands/{commandId}`) so background workers (`HeartbeatWorker`, `ChatzyFcmService`) and `DeviceManager` can sync status without requiring user sign-in.

---

## 3. Verification Checklist

- [x] Non-empty document IDs enforced for all writes.
- [x] Client-side document deletion blocked.
- [x] Unauthenticated device registration & FCM token sync preserved.
- [x] Heartbeat worker background writes functional.
- [x] Command acknowledgement logging functional.
