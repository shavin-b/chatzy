package com.orderflow.autoresponder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orderflow.autoresponder.data.local.entity.MessageLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageLogDao {

    @Query("SELECT * FROM message_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<MessageLogEntity>>

    @Query("SELECT * FROM message_logs WHERE senderPhone = :phone ORDER BY timestamp DESC")
    fun getLogsByPhone(phone: String): Flow<List<MessageLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MessageLogEntity): Long

    @Query("DELETE FROM message_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM message_logs WHERE status = 'SENT' AND timestamp >= :startTimestamp")
    suspend fun getCountSince(startTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM message_logs")
    suspend fun getTotalCount(): Int

    @Query("SELECT * FROM message_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<MessageLogEntity>>

    @Query("SELECT COUNT(*) FROM message_logs WHERE status = :status")
    suspend fun getCountByStatus(status: String): Int

    @Query("SELECT MAX(timestamp) FROM message_logs WHERE senderPhone = :phone AND status = 'SENT'")
    suspend fun getLastSentTimestamp(phone: String): Long?
}
