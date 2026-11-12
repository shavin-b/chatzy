package com.orderflow.autoresponder.data.repository

import com.orderflow.autoresponder.data.local.dao.ProcessedNotificationDao
import com.orderflow.autoresponder.data.local.entity.ProcessedNotificationEntity
import com.orderflow.autoresponder.domain.repository.ProcessedNotificationRepository
import javax.inject.Inject

class ProcessedNotificationRepositoryImpl @Inject constructor(
    private val dao: ProcessedNotificationDao
) : ProcessedNotificationRepository {
    
    override suspend fun getNotification(fingerprint: String): ProcessedNotificationEntity? {
        return dao.getNotification(fingerprint)
    }

    override suspend fun insertNotification(notification: ProcessedNotificationEntity) {
        dao.insertNotification(notification)
    }

    override suspend fun isAlreadyProcessed(fingerprint: String): Boolean {
        return dao.getNotification(fingerprint) != null
    }

    override suspend fun findDuplicate(sender: String, message: String, since: Long): ProcessedNotificationEntity? {
        return dao.findDuplicate(sender, message, since)
    }

    override suspend fun clearOldNotifications(days: Int) {
        val cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        dao.clearOldNotifications(cutoff)
    }

    override suspend fun clearAllNotifications() {
        dao.clearAllNotifications()
    }
}
