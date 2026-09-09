package com.orderflow.autoresponder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_notifications")
data class ProcessedNotificationEntity(
    @PrimaryKey val fingerprint: String,
    val packageName: String,
    val sender: String,
    val message: String,
    val timestamp: Long,
    val replySent: Boolean,
    val processedAt: Long = System.currentTimeMillis()
)
