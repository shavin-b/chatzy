package com.orderflow.autoresponder.domain.model

import android.service.notification.StatusBarNotification

data class IncomingWhatsAppMessage(
    val packageName: String,
    val conversationTitle: String?,
    val senderName: String?,
    val messageText: String,
    val timestamp: Long,
    val notificationKey: String?,
    val isGroup: Boolean = false,
    val sbn: StatusBarNotification
)
