package com.orderflow.autoresponder.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.orderflow.autoresponder.domain.model.AutoReplyRule

data class RuleWithMessages(
    @Embedded val rule: RuleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "ruleId"
    )
    val messages: List<RuleMessageEntity>
)

fun RuleWithMessages.toDomainModel(): AutoReplyRule {
    return rule.toDomainModel(messages.map { it.toDomainModel() })
}
