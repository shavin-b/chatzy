package com.orderflow.autoresponder.device.model

data class DeviceInfo(
    val deviceId: String = "",
    val phoneModel: String = "",
    val manufacturer: String = "",
    val androidVersion: String = "",
    val appVersion: String = "",
    val appBuildNumber: Int = 0,
    val installationDate: Long = 0,
    val userName: String = "",
    val businessName: String = "",
    val firstSeen: Long? = null,
    // Existing documents may contain epoch milliseconds; new writes use Firestore Timestamp.
    val lastSeen: Any? = null,
    val lastSync: Long? = null,
    val status: String = "Active", // Overall status
    val lockStatus: String = "UNLOCKED",
    val lockReason: String = "NONE",
    val adminLock: Boolean = false,
    val adminLocked: Boolean = false,
    val isLocked: Boolean = false,
    val generatedUuid: String = "",
    val lastCommandId: String? = null,
    val lastCommandStatus: String? = null,
    val fcmToken: String = "",
    val phoneNumber: String = "",
    val subscriptionStart: Long? = null,
    val subscriptionEnd: Long? = null,
    val subscriptionStatus: String = "ACTIVE"
)

data class DeviceCommand(
    val commandId: String = "",
    val deviceId: String = "",
    val command: String = "",
    val status: String = "PENDING",
    val processedAt: Long? = null,
    val errorMessage: String? = null
)
