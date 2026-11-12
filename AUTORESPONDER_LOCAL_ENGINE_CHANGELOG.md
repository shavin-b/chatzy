> Chatzy migration note (2026-11-09): this historical review is not a production sign-off. See CHATZY_RELEASE_CHECKLIST.md, CHATZY_SECURITY_REVIEW.md and CHATZY_TEST_RESULTS.md. Company: SHAVIN SOLUTIONS (PVT) LTD. Support contact and published policy URL remain pending.

# Chatzy AutoResponder-style local engine

## Implemented

The Android app keeps the existing Compose UI and existing Firebase/Admin control layer, but the WhatsApp auto-reply path is now local-first.

### Reply flow

WhatsApp / WhatsApp Business notification
-> NotificationListenerService
-> WhatsAppNotificationParser
-> ProcessIncomingMessageUseCase
-> Room rules
-> EvaluateRuleUseCase
-> LocalReplyHelper via RemoteInput/PendingIntent
-> WhatsApp

### Important behavior changes

1. Both `com.whatsapp` and `com.whatsapp.w4b` are accepted.
2. A notification reply action is always attempted first.
3. Meta Cloud API is only a fallback when the existing Cloud API switch is explicitly enabled and a usable phone number is available.
4. Normal local replies therefore do not require internet access, Firebase, Meta tokens, or a webhook.
5. The existing local Room database, rules, delays, sequential replies, business hours, logs, subscription controls, and admin lock checks remain in place.
6. Notification-listener coroutine work is cancelled cleanly when the service is destroyed.

## UI policy

No UI redesign was performed. The current rules, logs, settings, customers, dashboard, and Cloud API screens remain intact.

## Kept intentionally

Firebase/FCM/device-management code remains available because it is used by Chatzy's subscription/admin device-control features. It is no longer required for the critical local reply transport.

## Testing note

The container used for this edit does not have a Gradle executable or Android SDK configured, so a full APK build could not be executed here. The source changes were inspected for consistency with the existing project structure.
