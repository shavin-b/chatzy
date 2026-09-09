package com.orderflow.admin.domain.model

data class NotificationItem(
    val notificationId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "Announcement", // Announcement, Maintenance Notice, Subscription Reminder, Custom Message
    val targetDeviceId: String? = null, // null for broadcast to all devices
    val sentAt: Long = System.currentTimeMillis(),
    val sentBy: String = "Admin",
    val status: String = "Sent"
)
