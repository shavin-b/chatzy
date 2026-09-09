package com.orderflow.admin.domain.model

data class Subscription(
    val subscriptionId: String = "",
    val deviceId: String = "",
    val businessName: String = "",
    val updatedBy: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val oldExpiryDate: Long = 0L,
    val newExpiryDate: Long = 0L,
    val daysAdded: Int = 0,
    val actionType: String = "Extend" // Extend, Reduce, Pause, Resume, Deactivate, Reactivate, Lifetime
)
