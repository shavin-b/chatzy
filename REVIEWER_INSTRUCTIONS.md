> Chatzy migration note (2026-11-09): this historical review is not a production sign-off. See CHATZY_RELEASE_CHECKLIST.md, CHATZY_SECURITY_REVIEW.md and CHATZY_TEST_RESULTS.md. Company: SHAVIN SOLUTIONS (PVT) LTD. Support contact and published policy URL remain pending.

# Google Play Store Reviewer Instructions

**App Name**: Chatzy Ã¢â‚¬â€œ WhatsApp Local Auto Responder  
**Package Name**: `com.orderflow.autoresponder`  
**Test Mode**: Local Auto Responder Mode (No Meta / Cloud API credentials required)

---

## Overview for App Reviewers

Chatzy is a local WhatsApp auto-responder. It uses Android's `NotificationListenerService` to detect incoming WhatsApp Business notifications, matches incoming message text against user-configured local rules on the device, and dispatches responses directly through WhatsApp's notification reply function (`RemoteInput`).

**No external server, API keys, or Meta WhatsApp Cloud API credentials are required to test or review this application.**

---

## Step-by-Step 14-Point Testing Workflow

1. **Install and Launch**: Install the Chatzy application on an Android test device that has **WhatsApp** or **WhatsApp Business** installed, and launch Chatzy.
2. **Initial Onboarding**: On the initial onboarding screen, enter any test Business Name (e.g., `Test Electronics`) and WhatsApp Business Number (e.g., `+1234567890`), then tap **Continue**.
3. **Navigate to Settings**: On the Dashboard screen, tap the **Notification Access Required** card or tap the **Settings** gear icon.
4. **Open Disclosure**: Tap **Grant** next to Notification Access.
5. **Review What is Accessed**: Read the in-app disclosure detailing that Chatzy accesses incoming WhatsApp Business notifications, message text, and sender contact names.
6. **Review Why Access is Required**: Read the explanation detailing that access is required to match incoming messages against your active rules and dispatch responses.
7. **Accept Disclosure**: Tap the affirmative consent button: **I Understand - Enable Access**.
8. **Enable System Permission**: In the Android Notification Access system settings screen that opens, locate **Chatzy Engine Service** and toggle access **ON**.
9. **Return & Verify Status**: Return to Chatzy. Confirm that Notification Access status now displays **Granted** and the Master Status toggle displays **ACTIVE**.
10. **Configure Auto-Reply Rule**: From the Dashboard, tap **Auto-Reply Rules**, then tap **+ Add Rule** (or edit default rule):
    * **Rule Name**: `Test Greeting`
    * **Trigger Keyword**: `hello` (Match Option: *Contains*)
    * **Reply Message**: `Hello! Thank you for reaching out to Chatzy.`
    * Tap **Save Rule**.
11. **Receive WhatsApp Message**: From a secondary phone or WhatsApp account, send a message containing `hello` to the test device's WhatsApp Business account.
12. **Verify Notification Detection**: Observe Chatzy detecting the incoming notification in the background.
13. **Verify RemoteInput Automatic Reply**: Verify that Chatzy evaluates the rule and dispatches the configured reply via WhatsApp's notification reply action (`RemoteInput`). Check the sender phone to confirm receipt of the automatic reply.
14. **Test Data Management & Privacy Policy**: Open **Settings** to test:
    * **In-App Policy**: View the Privacy & Data Safety Policy summary dialog.
    * **Clear Logs & Cache**: Delete local message logs and notification cache with confirmation.
    * **Disconnect Device**: Reset local device registration ID with confirmation.

---

## Technical Architecture Notes for Reviewers

* **Local Mode Independence**: Chatzy operates 100% locally on-device. No Meta credentials or Cloud API setup are needed.
* **Background Reliability**: Chatzy runs a low-priority ongoing foreground service (`remoteMessaging`) to maintain listener IPC connection stability during device standby.
* **Privacy Assurance**: Notification contents and message text are processed on-device and are never uploaded off-device or shared with third parties.
