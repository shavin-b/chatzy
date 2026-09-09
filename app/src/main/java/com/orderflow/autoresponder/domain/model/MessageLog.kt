package com.orderflow.autoresponder.domain.model

data class MessageLog(
    val id: Long = 0,
    val senderPhone: String,
    val senderName: String,
    val incomingMessage: String,
    val replyMessage: String,
    val ruleId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val errorMessage: String? = null
)

enum class MessageStatus {
    RECEIVED,
    SENT,
    FAILED,
    QUEUED,
    IGNORED,
    DUPLICATE,
    SKIPPED
}
