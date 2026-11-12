# Chatzy Help

Chatzy Help is an offline support conversation available from the Home info icon and the first button in Settings. It uses the existing Compose theme, AppTopBar, Navigation Compose routes, Hilt, ViewModel and StateFlow conventions.

## Editing answers

Edit `app/src/main/res/raw/chatzy_help.json`. Each topic has a stable `id`, a display `title`, `aliases`, an `answer`, and optional `actions`. Keep existing IDs stable. Aliases can contain English, Sinhala and Singlish. Provide at least two meaningful tokens per alias; avoid aliases made entirely of generic words. A JVM test checks every bundled alias against its expected topic.

Allowed action IDs are `service_status`, `rules`, `notification_access`, and `settings`. They map to existing app destinations and the same Android notification-access intent already used by Settings. Navigation happens only after a user taps an action. New action IDs require an explicit mapping in `HelpAction` and `NavGraph`; unknown IDs are rejected when loading the knowledge base.

Matching normalizes Unicode, case, punctuation and whitespace, preserves Sinhala combining marks, and compares meaningful token combinations against aliases. At least two matching tokens and topic-specific evidence are required. Close matches offer up to three topics instead of choosing a cause. Unknown input offers all common topics and asks the user to rephrase. Blank or punctuation-only input sends no message.

## Actual app behavior behind the answers

- `WhatsAppNotificationListener` and `ProcessIncomingMessageUseCase` accept `com.whatsapp.w4b` (WhatsApp Business). Local sending uses the notification's `PendingIntent` and `RemoteInput`. Cloud API is an optional fallback.
- `EvaluateRuleUseCase` checks active rules, priority, match options, chat/contact filters, cooldowns and business hours. `ProcessIncomingMessageUseCase` also applies the master switch, access restrictions, delays and duplicate suppression.
- Settings already launches `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`, provides battery/OEM guidance, and offers Repair Service Connection. Diagnostics displays the existing local permission/rule/device checks. Its listener indicator is derived from permission, so Help does not claim it proves live service health.
- `DeviceManager` creates a persistent local ID, syncs identity/profile and FCM-token data, and schedules heartbeat retries. `FirebaseDeviceRepository` writes the device record. Builds with secure enrollment use `DeviceEnrollment` and the Settings approval panel. The answers describe both configurations conditionally and leave server/project/approval checks to the account administrator. A local ID is not proof of a successful server sync.
- `AppConstants.SUPPORT_EMAIL` is empty. No support contact is invented.

## Data boundaries

The JSON uses the project's existing Gson dependency. No dependency, Android permission, AI API, backend or Firebase collection was added. HelpMatcher and HelpViewModel have no repository, logger, analytics or network calls. Support text is never passed to the WhatsApp rule processor, customer storage or message logs.

Messages and the draft live only in the Help destination's ViewModel. They survive rotation and returning from a linked screen while the Help back-stack entry exists. Closing Help discards its conversation; process death is not persisted. No SavedStateHandle, database or disk conversation history is used. Help displays guidance rather than inferred device state.

## Verification

Run `gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline`.

`HelpMatcherTest` covers English, Sinhala, requested Singlish examples, every bundled alias, reordered tokens, punctuation/whitespace and locale handling, ambiguous two/three-topic input, generic/unknown questions, empty input, optional JSON actions, allowlisted topic buttons and in-memory conversation/draft behavior.

Verified on the Pixel Android 16 emulator using the debug APK and normal app navigation:

- Home info icon and Settings both open Chatzy Help; Back returns correctly.
- All five quick-topic chips display the appropriate bundled answers.
- An unknown question offers common topics; `notification access and rule setup` offers the two matching topics.
- Open Service status, View rules, Open notification access and Open Settings reach their existing destinations only after tapping. Returning preserves the conversation.
- Rotating portrait to landscape preserves the current answer and an unsent `draft`, and rotating back works.
- These checks ran with airplane mode on and Wi-Fi confirmed disabled. The original airplane-mode, rotation and display settings were restored afterward.
- `assembleDebug` and all 55 JVM tests (12 Help tests, 43 existing tests) pass. Gradle tracks the knowledge-base JSON as a test input so editing answers or aliases invalidates the test result.

The connected Samsung phone accepted the debug install but was locked and later disconnected, so physical-phone UI, keyboard/font rendering and rotation checks remain to be repeated there. Sinhala and the requested Singlish examples were verified by JVM tests; Sinhala keyboard entry on a physical phone was not tested. Physical-device background auto-replies and server registration are existing features; this feature does not test or diagnose them automatically.
