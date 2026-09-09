package com.orderflow.autoresponder.domain.repository

import com.orderflow.autoresponder.domain.model.AutoReplyRule
import kotlinx.coroutines.flow.Flow

interface RuleRepository {
    fun getAllRules(): Flow<List<AutoReplyRule>>
    fun getActiveRules(): Flow<List<AutoReplyRule>>
    suspend fun getRuleById(id: Long): AutoReplyRule?
    suspend fun insertRule(rule: AutoReplyRule): Long
    suspend fun updateRule(rule: AutoReplyRule)
    suspend fun deleteRule(rule: AutoReplyRule)
    suspend fun toggleRuleActive(ruleId: Long, isActive: Boolean)
}
