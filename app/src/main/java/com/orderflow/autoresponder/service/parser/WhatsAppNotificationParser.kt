package com.orderflow.autoresponder.service.parser

import android.app.Notification
import android.app.Person
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.orderflow.autoresponder.core.logger.StructuredLogger
import com.orderflow.autoresponder.core.util.EchoReplyTracker
import com.orderflow.autoresponder.core.util.WhatsAppConstants
import com.orderflow.autoresponder.domain.model.IncomingWhatsAppMessage

object WhatsAppNotificationParser {

    @Suppress("DEPRECATION")
    fun parse(sbn: StatusBarNotification): IncomingWhatsAppMessage? {
        val packageName = sbn.packageName
        if (!WhatsAppConstants.isWhatsApp(packageName)) return null

        val notification = sbn.notification
        val extras = notification.extras ?: return null

        // 1. Filter out summary/group notifications (e.g. "3 new messages from 2 chats")
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isGroupSummary) {
            StructuredLogger.d("WhatsAppNotificationParser", "Ignored FLAG_GROUP_SUMMARY notification")
            return null
        }

        // 2. Extract Title candidate
        val rawTitle = extras.getString(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getString(Notification.EXTRA_CONVERSATION_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?: ""

        var messageText = ""
        var senderName = rawTitle
        var conversationTitle = rawTitle
        var isGroup = false
        var messageTimestamp = sbn.postTime

        // 3. Extract detailed message info using MessagingStyle (Primary path on modern Android/WhatsApp)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messages != null && messages.isNotEmpty()) {
                val lastMessage = messages.lastOrNull() as? Bundle
                if (lastMessage != null) {
                    val extractedText = lastMessage.getCharSequence("text")?.toString()
                    if (!extractedText.isNullOrBlank()) {
                        messageText = extractedText
                    }
                    messageTimestamp = lastMessage.getLong("time", sbn.postTime)

                    // Robust sender extraction: handles both group chats (where sender is filled)
                    // and 1-on-1 chats (where sender may be null and defaults to conversation title).
                    val rawSender = lastMessage.getCharSequence("sender")?.toString()
                    if (!rawSender.isNullOrBlank()) {
                        senderName = rawSender
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        try {
                            val personObj = lastMessage.get("sender_person")
                            if (personObj is Person) {
                                val name = personObj.name?.toString()
                                if (!name.isNullOrBlank()) senderName = name
                            } else if (personObj is Bundle) {
                                // AndroidX PersonCompat
                                val name = personObj.getCharSequence("name")?.toString()
                                if (!name.isNullOrBlank()) senderName = name
                            }
                        } catch (e: Exception) {
                            // Fallback to title
                        }
                    }
                }
            }

            val isGroupConversation = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
            val extractedConversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            if (!extractedConversationTitle.isNullOrBlank()) {
                conversationTitle = extractedConversationTitle
            }
            isGroup = isGroupConversation || (conversationTitle != senderName && conversationTitle.isNotBlank())
        }

        // 4. Fallback for BigTextStyle / plain notifications if MessagingStyle had no text
        if (messageText.isBlank()) {
            messageText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: ""
        }

        // WhatsApp specific: colon in title format like "Group Name: Sender"
        if (!isGroup && conversationTitle.contains(": ")) {
            isGroup = true
        }

        // 5. Validation & Filtering
        if (messageText.isBlank() || messageText.contains("Sensitive content hidden", ignoreCase = true)) {
            StructuredLogger.d("WhatsAppNotificationParser", "Ignored notification with empty or hidden message text")
            return null
        }

        // Ignore summary badges like "3 new messages" or "Checking for new messages"
        if (messageText.contains("new messages", ignoreCase = true) ||
            messageText.contains("WhatsApp Web", ignoreCase = true) ||
            messageText.contains("Backup in progress", ignoreCase = true)
        ) {
            StructuredLogger.d("WhatsAppNotificationParser", "Ignored system/summary text: '$messageText'")
            return null
        }

        // Loop Prevention: Check Echo Tracker (catches replies sent by this app within last 15s)
        if (EchoReplyTracker.isEcho(messageText)) {
            StructuredLogger.i("WhatsAppNotificationParser", "Loop prevention: Echo detected for '$messageText'. Skipping.")
            return null
        }

        // Loop Prevention: Plain-text "You: " check
        val selfDisplayName = extras.getCharSequence(Notification.EXTRA_SELF_DISPLAY_NAME)?.toString()
        if (messageText.startsWith("You:", ignoreCase = true) ||
            (!selfDisplayName.isNullOrBlank() && messageText.startsWith("$selfDisplayName:", ignoreCase = true))
        ) {
            StructuredLogger.i("WhatsAppNotificationParser", "Loop prevention: Self-sent message detected. Skipping.")
            return null
        }

        val finalSenderName = senderName.ifBlank { rawTitle.ifBlank { "Unknown" } }
        val finalConversationTitle = conversationTitle.ifBlank { finalSenderName }

        return IncomingWhatsAppMessage(
            packageName = packageName,
            conversationTitle = finalConversationTitle,
            senderName = finalSenderName,
            messageText = messageText,
            timestamp = messageTimestamp,
            notificationKey = sbn.key,
            isGroup = isGroup,
            sbn = sbn
        )
    }
}

