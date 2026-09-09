package com.orderflow.autoresponder.domain.model

data class Subscription(
    val planName: String = "Standard Package",
    val isActive: Boolean = true,
    val expiryTimestamp: Long = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000),
    val maxRules: Int = 100,
    val cloudApiSyncEnabled: Boolean = true
)
