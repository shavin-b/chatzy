package com.orderflow.autoresponder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.orderflow.autoresponder.domain.model.MessageLog
import com.orderflow.autoresponder.domain.model.MessageStatus

@Entity(tableName = "message_logs")
data class MessageLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderPhone: String,
    val senderName: String,
    val incomingMessage: String,
    val replyMessage: String,
    val ruleId: Long?,
    val timestamp: Long,
    val status: String,
    val errorMessage: String?
)

fun MessageLogEntity.toDomainModel(): MessageLog {
    return MessageLog(
        id = id,
        senderPhone = senderPhone,
        senderName = senderName,
        incomingMessage = incomingMessage,
        replyMessage = replyMessage,
        ruleId = ruleId,
        timestamp = timestamp,
        status = try { MessageStatus.valueOf(status) } catch (e: Exception) { MessageStatus.SENT },
        errorMessage = errorMessage
    )
}

fun MessageLog.toEntity(): MessageLogEntity {
    return MessageLogEntity(
        id = id,
        senderPhone = senderPhone,
        senderName = senderName,
        incomingMessage = incomingMessage,
        replyMessage = replyMessage,
        ruleId = ruleId,
        timestamp = timestamp,
        status = status.name,
        errorMessage = errorMessage
    )
}
