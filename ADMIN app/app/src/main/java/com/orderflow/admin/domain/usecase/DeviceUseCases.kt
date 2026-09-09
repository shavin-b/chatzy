package com.orderflow.admin.domain.usecase

import com.orderflow.admin.core.common.Constants
import com.orderflow.admin.core.common.DateUtils
import com.orderflow.admin.domain.model.Device
import com.orderflow.admin.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DeviceUseCases @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    fun getDevicesStream(
        searchQuery: String = "",
        filter: String = "All",
        sortBy: String = "Business Name"
    ): Flow<List<Device>> {
        return deviceRepository.getDevicesStream().map { devices ->
            devices
                .filter { device ->
                    if (searchQuery.isBlank()) true
                    else {
                        device.businessName.contains(searchQuery, ignoreCase = true) ||
                                device.userName.contains(searchQuery, ignoreCase = true) ||
                                device.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                                device.deviceId.contains(searchQuery, ignoreCase = true) ||
                                device.appVersion.contains(searchQuery, ignoreCase = true)
                    }
                }
                .filter { device ->
                    val isOnline = DateUtils.isOnline(device.lastSeen)
                    val daysRemaining = DateUtils.calculateDaysRemaining(device.subscriptionEnd)
                    when (filter) {
                        "Only Active" -> device.status == Constants.STATUS_ACTIVE
                        "Only Expired" -> daysRemaining <= 0 || device.status == Constants.STATUS_EXPIRED
                        "Only Offline" -> !isOnline
                        "Only Online" -> isOnline
                        "Expiring Soon" -> daysRemaining in 1..Constants.EXPIRING_SOON_DAYS
                        else -> true
                    }
                }
                .sortedWith { d1, d2 ->
                    when (sortBy) {
                        "Business Name" -> d1.businessName.compareTo(d2.businessName, ignoreCase = true)
                        "Remaining Days" -> d2.subscriptionEnd.compareTo(d1.subscriptionEnd)
                        "Last Seen" -> d2.lastSeen.compareTo(d1.lastSeen)
                        "Install Date" -> d2.installationDate.compareTo(d1.installationDate)
                        else -> d1.businessName.compareTo(d2.businessName, ignoreCase = true)
                    }
                }
        }
    }

    fun getDeviceDetailsStream(deviceId: String): Flow<Device?> {
        return deviceRepository.getDeviceByIdStream(deviceId)
    }
}
