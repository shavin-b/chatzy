package com.orderflow.autoresponder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.orderflow.autoresponder.domain.model.AutoReplyMessage

@Entity(
    tableName = "rule_messages",
    foreignKeys = [
        ForeignKey(
            entity = RuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ruleId")]
)
data class RuleMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: Long,
    val message: String,
    val position: Int,
    val isEnabled: Boolean = true
)

fun RuleMessageEntity.toDomainModel(): AutoReplyMessage {
    return AutoReplyMessage(
        id = id,
        ruleId = ruleId,
        message = message,
        position = position,
        isEnabled = isEnabled
    )
}

fun AutoReplyMessage.toEntity(): RuleMessageEntity {
    return RuleMessageEntity(
        id = id,
        ruleId = ruleId,
        message = message,
        position = position,
        isEnabled = isEnabled
    )
}
