package com.orderflow.autoresponder.data.local.dao

import androidx.room.*
import com.orderflow.autoresponder.data.local.entity.RuleEntity
import com.orderflow.autoresponder.data.local.entity.RuleMessageEntity
import com.orderflow.autoresponder.data.local.entity.RuleWithMessages
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RuleDao {

    @Transaction
    @Query("SELECT * FROM auto_reply_rules ORDER BY createdAt DESC")
    abstract fun getAllRulesWithMessages(): Flow<List<RuleWithMessages>>

    @Transaction
    @Query("SELECT * FROM auto_reply_rules WHERE isActive = 1 ORDER BY createdAt DESC")
    abstract fun getActiveRulesWithMessages(): Flow<List<RuleWithMessages>>

    @Transaction
    @Query("SELECT * FROM auto_reply_rules WHERE id = :id LIMIT 1")
    abstract suspend fun getRuleByIdWithMessages(id: Long): RuleWithMessages?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRule(rule: RuleEntity): Long

    @Update
    abstract suspend fun updateRule(rule: RuleEntity)

    @Delete
    abstract suspend fun deleteRule(rule: RuleEntity)

    @Query("UPDATE auto_reply_rules SET isActive = :isActive WHERE id = :ruleId")
    abstract suspend fun updateRuleActiveState(ruleId: Long, isActive: Boolean)

    @Query("DELETE FROM rule_messages WHERE ruleId = :ruleId")
    abstract suspend fun deleteMessagesByRuleId(ruleId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMessages(messages: List<RuleMessageEntity>)

    @Transaction
    open suspend fun saveRuleWithMessages(rule: RuleEntity, messages: List<RuleMessageEntity>): Long {
        val ruleId = insertRule(rule)
        deleteMessagesByRuleId(ruleId)
        val messagesToInsert = messages.map { it.copy(ruleId = ruleId) }
        insertMessages(messagesToInsert)
        return ruleId
    }
}
