package com.orderflow.autoresponder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orderflow.autoresponder.data.local.entity.ProcessedNotificationEntity

@Dao
interface ProcessedNotificationDao {
    @Query("SELECT * FROM processed_notifications WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getNotification(fingerprint: String): ProcessedNotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: ProcessedNotificationEntity)

    @Query("SELECT * FROM processed_notifications WHERE sender = :sender AND message = :normalizedMessage AND timestamp > :since LIMIT 1")
    suspend fun findDuplicate(sender: String, normalizedMessage: String, since: Long): ProcessedNotificationEntity?

    @Query("DELETE FROM processed_notifications WHERE processedAt < :timestamp")
    suspend fun clearOldNotifications(timestamp: Long)

    @Query("DELETE FROM processed_notifications")
    suspend fun clearAllNotifications()
}
