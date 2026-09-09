package com.orderflow.autoresponder.domain.model

data class Customer(
    val phone: String,
    val name: String,
    val totalMessages: Int = 1,
    val lastInteractionTime: Long = System.currentTimeMillis(),
    val notes: String = ""
)
