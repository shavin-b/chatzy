package com.orderflow.admin.data.repository

import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.data.model.toDomain
import com.orderflow.admin.data.model.toDto
import com.orderflow.admin.data.remote.FirestoreDataSource
import com.orderflow.admin.domain.model.AdminSettings
import com.orderflow.admin.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val firestoreSource: FirestoreDataSource
) : SettingsRepository {

    override fun getAdminSettingsStream(): Flow<AdminSettings> {
        return firestoreSource.getSettingsStream().map { it.toDomain() }
    }

    override suspend fun updateSettings(settings: AdminSettings): Resource<Unit> {
        firestoreSource.updateSettings(settings.toDto())
        return Resource.Success(Unit)
    }
}
