package com.orderflow.admin.domain.usecase

import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.domain.model.NotificationItem
import com.orderflow.admin.domain.repository.LogRepository
import com.orderflow.admin.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NotificationUseCases @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val logRepository: LogRepository
) {
    fun getNotificationsStream(): Flow<List<NotificationItem>> = notificationRepository.getNotificationsStream()

    suspend fun sendNotification(
        title: String,
        body: String,
        type: String,
        targetDeviceId: String? = null
    ): Resource<Unit> {
        if (title.isBlank() || body.isBlank()) {
            return Resource.Error("Title and message content cannot be empty.")
        }
        val notification = NotificationItem(
            notificationId = System.currentTimeMillis().toString(),
            title = title,
            body = body,
            type = type,
            targetDeviceId = targetDeviceId,
            sentAt = System.currentTimeMillis(),
            sentBy = "Admin",
            status = "Sent"
        )
        val result = notificationRepository.sendPushNotification(notification)
        if (result is Resource.Success) {
            logRepository.addLog(
                title = "Notification Sent",
                description = "Push notification '$title' sent to ${targetDeviceId ?: "All Devices"}.",
                category = "Notification",
                deviceId = targetDeviceId
            )
        }
        return result
    }
}
