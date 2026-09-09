package com.orderflow.autoresponder.domain.model

data class AutoReplyMessage(
    val id: Long = 0,
    val ruleId: Long = 0,
    val message: String,
    val position: Int = 0,
    val isEnabled: Boolean = true
)
