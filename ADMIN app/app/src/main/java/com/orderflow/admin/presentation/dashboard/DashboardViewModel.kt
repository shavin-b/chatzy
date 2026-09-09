package com.orderflow.admin.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orderflow.admin.domain.model.AdminUser
import com.orderflow.admin.domain.model.DashboardStats
import com.orderflow.admin.domain.model.Device
import com.orderflow.admin.domain.model.LogEntry
import com.orderflow.admin.domain.usecase.AuthUseCases
import com.orderflow.admin.domain.usecase.DashboardUseCases
import com.orderflow.admin.domain.usecase.DeviceUseCases
import com.orderflow.admin.domain.usecase.SubscriptionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val adminUser: AdminUser? = null,
    val stats: DashboardStats = DashboardStats(),
    val recentDevices: List<Device> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authUseCases: AuthUseCases,
    private val dashboardUseCases: DashboardUseCases,
    private val deviceUseCases: DeviceUseCases
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        authUseCases.getCurrentAdmin(),
        dashboardUseCases.getDashboardStatsStream(),
        deviceUseCases.getDevicesStream()
    ) { admin, stats, devices ->
        DashboardUiState(
            adminUser = admin ?: AdminUser(name = "Super Admin", role = "Super Admin"),
            stats = stats,
            recentDevices = devices.take(4),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )
}
