package com.orderflow.admin.domain.usecase

import com.orderflow.admin.core.common.Constants
import com.orderflow.admin.core.common.DateUtils
import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.domain.model.Subscription
import com.orderflow.admin.domain.repository.DeviceRepository
import com.orderflow.admin.domain.repository.LogRepository
import com.orderflow.admin.domain.repository.SubscriptionRepository
import javax.inject.Inject

class SubscriptionUseCases @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val logRepository: LogRepository
) {
    suspend fun updateSubscription(
        deviceId: String,
        businessName: String,
        currentExpiry: Long,
        actionType: String, // Extend, Reduce, Pause, Resume, Deactivate, Reactivate, Lifetime
        days: Int
    ): Resource<Unit> {
        val newExpiry = when (actionType) {
            "Extend" -> DateUtils.addDaysToTimestamp(currentExpiry, days)
            "Reduce" -> DateUtils.subtractDaysFromTimestamp(currentExpiry, days)
            "Lifetime" -> DateUtils.getLifetimeTimestamp()
            "Pause", "Deactivate" -> currentExpiry
            "Resume", "Reactivate" -> DateUtils.addDaysToTimestamp(System.currentTimeMillis(), days.coerceAtLeast(30))
            else -> DateUtils.addDaysToTimestamp(currentExpiry, days)
        }

        val updateResult = deviceRepository.updateDeviceSubscription(
            deviceId = deviceId,
            newExpiryTimestamp = newExpiry,
            actionType = actionType,
            daysAdded = if (actionType == "Reduce") -days else days
        )

        if (updateResult is Resource.Success) {
            val record = Subscription(
                subscriptionId = System.currentTimeMillis().toString(),
                deviceId = deviceId,
                businessName = businessName,
                updatedBy = "Admin",
                updatedAt = System.currentTimeMillis(),
                oldExpiryDate = currentExpiry,
                newExpiryDate = newExpiry,
                daysAdded = if (actionType == "Reduce") -days else days,
                actionType = actionType
            )
            subscriptionRepository.recordSubscriptionUpdate(record)

            logRepository.addLog(
                title = "Subscription Updated",
                description = "Updated subscription for $businessName ($deviceId). Action: $actionType, Days: $days.",
                category = "Subscription",
                deviceId = deviceId
            )
        }

        return updateResult
    }
}
