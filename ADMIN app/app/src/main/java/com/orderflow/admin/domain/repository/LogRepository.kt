package com.orderflow.admin.domain.repository

import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    fun getLogsStream(): Flow<List<LogEntry>>
    suspend fun addLog(title: String, description: String, category: String, deviceId: String? = null): Resource<Unit>
}
