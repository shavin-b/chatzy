package com.orderflow.admin.data.repository

import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.data.model.toDomain
import com.orderflow.admin.data.remote.FirestoreDataSource
import com.orderflow.admin.domain.model.Device
import com.orderflow.admin.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val firestoreSource: FirestoreDataSource
) : DeviceRepository {

    override fun getDevicesStream(): Flow<List<Device>> {
        return firestoreSource.getDevicesStream().map { dtos ->
            dtos.map { it.toDomain() }
        }
    }

    override fun getDeviceByIdStream(deviceId: String): Flow<Device?> {
        return firestoreSource.getDeviceByIdStream(deviceId).map { it?.toDomain() }
    }

    override suspend fun updateDeviceStatus(deviceId: String, status: String): Resource<Unit> {
        firestoreSource.updateDeviceStatus(deviceId, status)
        return Resource.Success(Unit)
    }

    override suspend fun updateDeviceSubscription(
        deviceId: String,
        newExpiryTimestamp: Long,
        actionType: String,
        daysAdded: Int
    ): Resource<Unit> {
        firestoreSource.updateDeviceSubscription(deviceId, newExpiryTimestamp, actionType, daysAdded)
        return Resource.Success(Unit)
    }

    override suspend fun deactivateDevice(deviceId: String): Resource<Unit> {
        firestoreSource.updateDeviceStatus(deviceId, "Suspended")
        return Resource.Success(Unit)
    }

    override suspend fun reactivateDevice(deviceId: String): Resource<Unit> {
        firestoreSource.updateDeviceStatus(deviceId, "Active")
        return Resource.Success(Unit)
    }
}
