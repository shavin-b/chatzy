package com.orderflow.autoresponder.domain.repository

import com.orderflow.autoresponder.domain.model.MessageLog
import kotlinx.coroutines.flow.Flow

interface MessageLogRepository {
    fun getAllLogs(): Flow<List<MessageLog>>
    fun getLogsByPhone(phone: String): Flow<List<MessageLog>>
    suspend fun insertLog(log: MessageLog): Long
    suspend fun clearLogs()
    suspend fun getTodayRepliedCount(): Int
    suspend fun getTotalCount(): Int
    suspend fun getCountByStatus(status: com.orderflow.autoresponder.domain.model.MessageStatus): Int
    fun getRecentLogs(limit: Int): Flow<List<MessageLog>>
    suspend fun getLastSentTimestamp(phone: String): Long?
}
