package com.orderflow.autoresponder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orderflow.autoresponder.data.local.entity.RuleMessageEntity

@Dao
interface RuleMessageDao {
    @Query("SELECT * FROM rule_messages WHERE ruleId = :ruleId ORDER BY position ASC")
    suspend fun getMessagesByRuleId(ruleId: Long): List<RuleMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<RuleMessageEntity>)

    @Query("DELETE FROM rule_messages WHERE ruleId = :ruleId")
    suspend fun deleteMessagesByRuleId(ruleId: Long)
}
