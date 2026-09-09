package com.orderflow.admin.data.model

import com.orderflow.admin.domain.model.Subscription

data class SubscriptionDto(
    val subscriptionId: String = "",
    val deviceId: String = "",
    val businessName: String = "",
    val updatedBy: String = "",
    val updatedAt: Long = 0L,
    val oldExpiryDate: Long = 0L,
    val newExpiryDate: Long = 0L,
    val daysAdded: Int = 0,
    val actionType: String = ""
)

fun SubscriptionDto.toDomain(): Subscription {
    return Subscription(
        subscriptionId = subscriptionId,
        deviceId = deviceId,
        businessName = businessName,
        updatedBy = updatedBy,
        updatedAt = updatedAt,
        oldExpiryDate = oldExpiryDate,
        newExpiryDate = newExpiryDate,
        daysAdded = daysAdded,
        actionType = actionType
    )
}

fun Subscription.toDto(): SubscriptionDto {
    return SubscriptionDto(
        subscriptionId = subscriptionId,
        deviceId = deviceId,
        businessName = businessName,
        updatedBy = updatedBy,
        updatedAt = updatedAt,
        oldExpiryDate = oldExpiryDate,
        newExpiryDate = newExpiryDate,
        daysAdded = daysAdded,
        actionType = actionType
    )
}
