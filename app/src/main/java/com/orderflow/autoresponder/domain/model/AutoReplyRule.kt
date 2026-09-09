package com.orderflow.autoresponder.domain.model

data class AutoReplyRule(
    val id: Long = 0,
    val ruleName: String,
    val keywordsCsv: String,
    val matchOption: MatchOption,
    val initialDelaySeconds: Int = 0,
    val delaySeconds: Int = 0,
    val replySequential: Boolean = false,
    val isActive: Boolean = true,
    val isPauseForContact: Boolean = false,
    val pauseDurationMinutes: Int = 0,
    val priority: Int = 0,
    val caseSensitive: Boolean = false,
    val enabledForGroups: Boolean = false,
    val enabledForPrivate: Boolean = true,
    val replyToSpecificContacts: Boolean = false,
    val specificContactsCsv: String = "",
    val excludeSpecificContacts: Boolean = false,
    val excludedContactsCsv: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val businessHours: BusinessHours = BusinessHours(),
    val messages: List<AutoReplyMessage> = emptyList()
)
