package com.orderflow.admin.data.repository

import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.data.model.LogDto
import com.orderflow.admin.data.model.toDomain
import com.orderflow.admin.data.remote.FirestoreDataSource
import com.orderflow.admin.domain.model.LogEntry
import com.orderflow.admin.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepositoryImpl @Inject constructor(
    private val firestoreSource: FirestoreDataSource
) : LogRepository {

    override fun getLogsStream(): Flow<List<LogEntry>> {
        return firestoreSource.getLogsStream().map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun addLog(
        title: String,
        description: String,
        category: String,
        deviceId: String?
    ): Resource<Unit> {
        val log = LogDto(
            logId = System.currentTimeMillis().toString(),
            timestamp = System.currentTimeMillis(),
            title = title,
            description = description,
            category = category,
            performedBy = "Super Admin",
            deviceId = deviceId
        )
        firestoreSource.addLog(log)
        return Resource.Success(Unit)
    }
}
