package com.orderflow.autoresponder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.orderflow.autoresponder.domain.model.AutoReplyMessage
import com.orderflow.autoresponder.domain.model.AutoReplyRule
import com.orderflow.autoresponder.domain.model.BusinessHours
import com.orderflow.autoresponder.domain.model.MatchOption

@Entity(tableName = "auto_reply_rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleName: String,
    val keywordsCsv: String,
    val replyMessagesJson: String,
    val matchOption: String,
    val delaySeconds: Int,
    val replySequential: Boolean,
    val isActive: Boolean,
    val isPauseForContact: Boolean,
    val pauseDurationMinutes: Int,
    val priority: Int = 0,
    val caseSensitive: Boolean = false,
    val enabledForGroups: Boolean = false,
    val enabledForPrivate: Boolean = true,
    val replyToSpecificContacts: Boolean = false,
    val specificContactsCsv: String = "",
    val excludeSpecificContacts: Boolean = false,
    val excludedContactsCsv: String = "",
    val initialDelaySeconds: Int = 0,
    val createdAt: Long,
    val businessHoursEnabled: Boolean,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val activeDaysCsv: String
)

fun RuleEntity.toDomainModel(messages: List<AutoReplyMessage> = emptyList()): AutoReplyRule {
    return AutoReplyRule(
        id = id,
        ruleName = ruleName,
        keywordsCsv = keywordsCsv,
        matchOption = try { MatchOption.valueOf(matchOption) } catch (e: Exception) { MatchOption.EXACT },
        initialDelaySeconds = initialDelaySeconds,
        delaySeconds = delaySeconds,
        replySequential = replySequential,
        isActive = isActive,
        isPauseForContact = isPauseForContact,
        pauseDurationMinutes = pauseDurationMinutes,
        priority = priority,
        caseSensitive = caseSensitive,
        enabledForGroups = enabledForGroups,
        enabledForPrivate = enabledForPrivate,
        replyToSpecificContacts = replyToSpecificContacts,
        specificContactsCsv = specificContactsCsv,
        excludeSpecificContacts = excludeSpecificContacts,
        excludedContactsCsv = excludedContactsCsv,
        createdAt = createdAt,
        businessHours = BusinessHours(
            isEnabled = businessHoursEnabled,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            activeDaysCsv = activeDaysCsv
        ),
        messages = messages
    )
}

fun AutoReplyRule.toEntity(): RuleEntity {
    return RuleEntity(
        id = id,
        ruleName = ruleName,
        keywordsCsv = keywordsCsv,
        replyMessagesJson = "", // Deprecated, will be ignored in favor of rule_messages table
        matchOption = matchOption.name,
        delaySeconds = delaySeconds,
        initialDelaySeconds = initialDelaySeconds,
        replySequential = replySequential,
        isActive = isActive,
        isPauseForContact = isPauseForContact,
        pauseDurationMinutes = pauseDurationMinutes,
        priority = priority,
        caseSensitive = caseSensitive,
        enabledForGroups = enabledForGroups,
        enabledForPrivate = enabledForPrivate,
        replyToSpecificContacts = replyToSpecificContacts,
        specificContactsCsv = specificContactsCsv,
        excludeSpecificContacts = excludeSpecificContacts,
        excludedContactsCsv = excludedContactsCsv,
        createdAt = createdAt,
        businessHoursEnabled = businessHours.isEnabled,
        startHour = businessHours.startHour,
        startMinute = businessHours.startMinute,
        endHour = businessHours.endHour,
        endMinute = businessHours.endMinute,
        activeDaysCsv = businessHours.activeDaysCsv
    )
}
