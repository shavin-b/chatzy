package com.orderflow.autoresponder.device.repository

import com.orderflow.autoresponder.device.model.DeviceCommand
import com.orderflow.autoresponder.device.model.DeviceInfo
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    suspend fun registerDevice(deviceInfo: DeviceInfo): Result<Unit>
    suspend fun updateDeviceInfo(deviceId: String, updates: Map<String, Any>): Result<Unit>
    suspend fun acknowledgeCommand(command: DeviceCommand): Result<Unit>
    fun getDeviceSubscriptionFlow(deviceId: String): Flow<DeviceInfo?>
    fun getDeviceRawFlow(deviceId: String): Flow<Map<String, Any>?>
}
