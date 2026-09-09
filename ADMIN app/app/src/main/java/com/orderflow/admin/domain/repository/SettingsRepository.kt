package com.orderflow.admin.domain.repository

import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.domain.model.AdminSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getAdminSettingsStream(): Flow<AdminSettings>
    suspend fun updateSettings(settings: AdminSettings): Resource<Unit>
}
