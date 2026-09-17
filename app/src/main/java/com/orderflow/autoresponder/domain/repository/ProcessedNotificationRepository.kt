package com.orderflow.autoresponder.domain.repository

import com.orderflow.autoresponder.data.local.entity.ProcessedNotificationEntity

interface ProcessedNotificationRepository {
    suspend fun getNotification(fingerprint: String): ProcessedNotificationEntity?
    suspend fun insertNotification(notification: ProcessedNotificationEntity)
    suspend fun isAlreadyProcessed(fingerprint: String): Boolean
    suspend fun findDuplicate(sender: String, message: String, since: Long): ProcessedNotificationEntity?
    suspend fun clearOldNotifications(days: Int = 7)
}
