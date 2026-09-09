package com.orderflow.admin.domain.repository

import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.domain.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun getDevicesStream(): Flow<List<Device>>
    fun getDeviceByIdStream(deviceId: String): Flow<Device?>
    suspend fun updateDeviceStatus(deviceId: String, status: String): Resource<Unit>
    suspend fun updateDeviceSubscription(
        deviceId: String,
        newExpiryTimestamp: Long,
        actionType: String,
        daysAdded: Int
    ): Resource<Unit>
    suspend fun deactivateDevice(deviceId: String): Resource<Unit>
    suspend fun reactivateDevice(deviceId: String): Resource<Unit>
}
