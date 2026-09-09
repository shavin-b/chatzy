package com.orderflow.admin.domain.usecase

import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.domain.model.AdminSettings
import com.orderflow.admin.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsUseCases @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    fun getSettingsStream(): Flow<AdminSettings> = settingsRepository.getAdminSettingsStream()

    suspend fun saveSettings(settings: AdminSettings): Resource<Unit> {
        return settingsRepository.updateSettings(settings)
    }
}
