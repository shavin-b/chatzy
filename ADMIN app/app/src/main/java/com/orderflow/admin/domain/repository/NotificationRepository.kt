package com.orderflow.admin.domain.repository

import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.domain.model.NotificationItem
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotificationsStream(): Flow<List<NotificationItem>>
    suspend fun sendPushNotification(notification: NotificationItem): Resource<Unit>
}
