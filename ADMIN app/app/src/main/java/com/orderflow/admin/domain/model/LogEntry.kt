package com.orderflow.admin.domain.model

data class LogEntry(
    val logId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val title: String = "",
    val description: String = "",
    val category: String = "System", // Subscription Updated, Device Registered, Device Activated, Login, Logout, Last Sync, App Updated, Notification Sent
    val performedBy: String = "Admin",
    val deviceId: String? = null
)
