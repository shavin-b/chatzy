package com.orderflow.admin.data.model

import com.orderflow.admin.core.common.DateUtils
import com.orderflow.admin.domain.model.Device

data class DeviceDto(
    val deviceId: String = "",
    val phoneModel: String = "",
    val manufacturer: String = "",
    val androidVersion: String = "",
    val appVersion: String = "",
    val userName: String = "",
    val businessName: String = "",
    val phoneNumber: String = "",
    val installationDate: Long = 0L,
    val activationDate: Long = 0L,
    val subscriptionStart: Long = 0L,
    val subscriptionEnd: Long = 0L,
    val daysRemaining: Long = 0L,
    val status: String = "Active",
    val lastSeen: Long = 0L,
    val lastSync: Long = 0L,
    val fcmToken: String = "",
    val isOnline: Boolean = false,
    val generatedUuid: String = ""
)

fun DeviceDto.toDomain(): Device {
    val online = DateUtils.isOnline(lastSeen)
    val remDays = DateUtils.calculateDaysRemaining(subscriptionEnd)
    val computedStatus = if (remDays <= 0) "Expired" else status

    return Device(
        deviceId = deviceId,
        phoneModel = phoneModel,
        manufacturer = manufacturer,
        androidVersion = androidVersion,
        appVersion = appVersion,
        userName = userName,
        businessName = businessName,
        phoneNumber = phoneNumber,
        installationDate = installationDate,
        activationDate = activationDate,
        subscriptionStart = subscriptionStart,
        subscriptionEnd = subscriptionEnd,
        daysRemaining = remDays,
        status = computedStatus,
        lastSeen = lastSeen,
        lastSync = lastSync,
        fcmToken = fcmToken,
        isOnline = online,
        generatedUuid = generatedUuid
    )
}
