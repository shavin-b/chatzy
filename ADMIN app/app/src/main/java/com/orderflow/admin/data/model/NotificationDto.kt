package com.orderflow.admin.data.model

import com.orderflow.admin.domain.model.NotificationItem

data class NotificationDto(
    val notificationId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    val targetDeviceId: String? = null,
    val sentAt: Long = 0L,
    val sentBy: String = "",
    val status: String = ""
)

fun NotificationDto.toDomain(): NotificationItem {
    return NotificationItem(
        notificationId = notificationId,
        title = title,
        body = body,
        type = type,
        targetDeviceId = targetDeviceId,
        sentAt = sentAt,
        sentBy = sentBy,
        status = status
    )
}

fun NotificationItem.toDto(): NotificationDto {
    return NotificationDto(
        notificationId = notificationId,
        title = title,
        body = body,
        type = type,
        targetDeviceId = targetDeviceId,
        sentAt = sentAt,
        sentBy = sentBy,
        status = status
    )
}
