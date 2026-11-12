// Verified bundled Chatzy help. Keep synchronized with Android raw/chatzy_help.json.
export default [
    {
        "id":  "auto_reply",
        "title":  "Auto-reply troubleshooting",
        "answer":  "Try these checks:\n1. Grant Chatzy Notification Access in Settings. Local replies support WhatsApp Business notifications with a reply action, not regular WhatsApp.\n2. Turn Auto-reply on at Home. Enable a rule, check its keywords and matching option, and add a reply message. Review case sensitivity, group/private settings, contact filters, pauses and business hours if configured.\n3. Test from another phone. Send a matching message to WhatsApp Business with the conversation closed so a notification appears. Use a fresh message, or wait over 30 seconds before repeating it; allow for configured delays.\n4. Open Service status (System Diagnostics) for permission and rule checks. For idle failures, see Background operation. Ask your administrator about access restrictions. Optional Cloud API fallback setup is in Settings.",
        "actions":  [
                        "notification_access",
                        "rules",
                        "service_status"
                    ]
    },
    {
        "id":  "notification_access",
        "title":  "Notification access",
        "answer":  "Tap Open notification access, find Chatzy, and enable its access. This lets Chatzy read WhatsApp Business notifications and use their reply action. Allowing Chatzy to display notifications is a different permission.\nAlternatively, use Chatzy Settings \u003e Notification Access \u003e Grant to read the permission explanation. If Android blocks the toggle on a sideloaded installation, look for Allow restricted settings in Chatzy\u0027s Android App info menu, if offered, then retry. Labels vary by phone.\nReturn to Service status to review the permission check. Permission alone does not prove a message was received or a reply sent.",
        "actions":  [
                        "notification_access",
                        "service_status",
                        "settings"
                    ]
    },
    {
        "id":  "rule_setup",
        "title":  "Set up a reply rule",
        "answer":  "Open Rules and tap +. Enter a Rule Name, choose a Matching Option, enter Keywords (comma separated), fill in Message 1, then Save Rule. Use Add Another Reply for a sequence. Enable the rule switch and Home\u0027s Auto-reply switch.\nEXACT matches the whole message; CONTAINS finds a keyword inside a longer message. Review Case Sensitive Matching and Enable for Group Chats. Higher priority rules are checked first.\nTest from another phone with a matching WhatsApp Business message that produces a notification.",
        "actions":  [
                        "rules"
                    ]
    },
    {
        "id":  "background_operation",
        "title":  "Background operation",
        "answer":  "In Settings \u003e Background Reliability \u0026 Setup, keep Notification Access enabled. Review Battery Optimization and use Allow Unrestricted if offered. Follow the phone-specific Background Protection steps when shown. Android can still delay background work.\nAvoid force-stopping Chatzy; reopen it after a force stop. If access is granted but replies stop, try Settings \u003e Repair Service Connection. Then test a new WhatsApp Business message from another phone with the screen off.\nService status shows existing permission and battery checks. This chat does not measure live service health.",
        "actions":  [
                        "settings",
                        "service_status"
                    ]
    },
    {
        "id":  "device_visibility",
        "title":  "Device missing from admin dashboard",
        "answer":  "A question alone cannot identify a sync failure. Check internet connectivity, reopen Chatzy, and review Business Profile in Settings. Share the Device ID from Service status with your account administrator; a local ID does not prove server registration.\nIf Settings shows Chatzy device approval, tap Request approval, show the pairing ID to your Super Admin, then Check approval. That panel appears only in builds with secure enrollment enabled.\nThe app syncs identity, profile and messaging-token data and schedules heartbeat retries; Android can delay them. Ask the administrator to check the correct dashboard/project, device record, approval and server-side errors. Help cannot verify these admin-only details. Do not reset registration as a first step.",
        "actions":  [
                        "settings",
                        "service_status"
                    ]
    }
];
