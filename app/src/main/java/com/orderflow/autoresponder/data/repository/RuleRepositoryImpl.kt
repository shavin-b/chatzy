package com.orderflow.autoresponder.data.repository

import com.orderflow.autoresponder.data.local.dao.RuleDao
import com.orderflow.autoresponder.data.local.entity.toDomainModel
import com.orderflow.autoresponder.data.local.entity.toEntity
import com.orderflow.autoresponder.domain.model.AutoReplyRule
import com.orderflow.autoresponder.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RuleRepositoryImpl @Inject constructor(
    private val ruleDao: RuleDao
) : RuleRepository {

    override fun getAllRules(): Flow<List<AutoReplyRule>> {
        return ruleDao.getAllRulesWithMessages().map { list -> list.map { it.toDomainModel() } }
    }

    override fun getActiveRules(): Flow<List<AutoReplyRule>> {
        return ruleDao.getActiveRulesWithMessages().map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun getRuleById(id: Long): AutoReplyRule? {
        return ruleDao.getRuleByIdWithMessages(id)?.toDomainModel()
    }

    override suspend fun insertRule(rule: AutoReplyRule): Long {
        val ruleEntity = rule.toEntity()
        val messageEntities = rule.messages.map { it.toEntity() }
        return ruleDao.saveRuleWithMessages(ruleEntity, messageEntities)
    }

    override suspend fun updateRule(rule: AutoReplyRule) {
        val ruleEntity = rule.toEntity()
        val messageEntities = rule.messages.map { it.toEntity() }
        ruleDao.saveRuleWithMessages(ruleEntity, messageEntities)
    }

    override suspend fun deleteRule(rule: AutoReplyRule) {
        ruleDao.deleteRule(rule.toEntity())
    }

    override suspend fun toggleRuleActive(ruleId: Long, isActive: Boolean) {
        ruleDao.updateRuleActiveState(ruleId, isActive)
    }
}
