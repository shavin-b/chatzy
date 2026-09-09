package com.orderflow.admin.domain.model

data class Device(
    val deviceId: String = "",
    val phoneModel: String = "",
    val manufacturer: String = "",
    val androidVersion: String = "",
    val appVersion: String = "",
    val userName: String = "",
    val businessName: String = "",
    val phoneNumber: String = "",
    val installationDate: Long = System.currentTimeMillis(),
    val activationDate: Long = System.currentTimeMillis(),
    val subscriptionStart: Long = System.currentTimeMillis(),
    val subscriptionEnd: Long = System.currentTimeMillis(),
    val daysRemaining: Long = 0,
    val status: String = "Active", // Active, Expiring Soon, Expired, Suspended, Offline, Uninstalled
    val lastSeen: Long = System.currentTimeMillis(),
    val lastSync: Long = System.currentTimeMillis(),
    val fcmToken: String = "",
    val isOnline: Boolean = false,
    val generatedUuid: String = ""
)
