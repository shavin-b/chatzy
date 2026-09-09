package com.orderflow.autoresponder.data.repository

import com.orderflow.autoresponder.data.local.dao.MessageLogDao
import com.orderflow.autoresponder.data.local.entity.toDomainModel
import com.orderflow.autoresponder.data.local.entity.toEntity
import com.orderflow.autoresponder.domain.model.MessageLog
import com.orderflow.autoresponder.domain.repository.MessageLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

class MessageLogRepositoryImpl @Inject constructor(
    private val messageLogDao: MessageLogDao
) : MessageLogRepository {

    override fun getAllLogs(): Flow<List<MessageLog>> {
        return messageLogDao.getAllLogs().map { list -> list.map { it.toDomainModel() } }
    }

    override fun getLogsByPhone(phone: String): Flow<List<MessageLog>> {
        return messageLogDao.getLogsByPhone(phone).map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun insertLog(log: MessageLog): Long {
        return messageLogDao.insertLog(log.toEntity())
    }

    override suspend fun clearLogs() {
        messageLogDao.clearAllLogs()
    }

    override suspend fun getTodayRepliedCount(): Int {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return messageLogDao.getCountSince(calendar.timeInMillis)
    }

    override suspend fun getTotalCount(): Int {
        return messageLogDao.getTotalCount()
    }

    override suspend fun getCountByStatus(status: com.orderflow.autoresponder.domain.model.MessageStatus): Int {
        return messageLogDao.getCountByStatus(status.name)
    }

    override fun getRecentLogs(limit: Int): Flow<List<MessageLog>> {
        return messageLogDao.getRecentLogs(limit).map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun getLastSentTimestamp(phone: String): Long? {
        return messageLogDao.getLastSentTimestamp(phone)
    }
}
