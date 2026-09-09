package com.orderflow.admin.domain.usecase

import com.orderflow.admin.core.common.Constants
import com.orderflow.admin.core.common.DateUtils
import com.orderflow.admin.domain.model.DashboardStats
import com.orderflow.admin.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DashboardUseCases @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    fun getDashboardStatsStream(): Flow<DashboardStats> {
        return deviceRepository.getDevicesStream().map { devices ->
            val total = devices.size
            val online = devices.count { DateUtils.isOnline(it.lastSeen) }
            val offline = total - online
            val expired = devices.count { DateUtils.calculateDaysRemaining(it.subscriptionEnd) <= 0 }
            val expiringSoon = devices.count {
                val rem = DateUtils.calculateDaysRemaining(it.subscriptionEnd)
                rem in 1..Constants.EXPIRING_SOON_DAYS
            }
            val activeToday = devices.count {
                val diff = System.currentTimeMillis() - it.lastSeen
                diff <= 24 * 60 * 60 * 1000L
            }

            val topBusinesses = devices
                .sortedByDescending { it.lastSeen }
                .take(5)
                .map { it.businessName }

            DashboardStats(
                totalDevices = total,
                onlineDevices = online,
                offlineDevices = offline,
                expiredDevices = expired,
                expiringSoonDevices = expiringSoon,
                activeTodayDevices = activeToday,
                weeklyInstalls = listOf(12, 18, 25, 20, 32, 28, 40),
                monthlyInstalls = total,
                topActiveBusinesses = topBusinesses
            )
        }
    }
}
