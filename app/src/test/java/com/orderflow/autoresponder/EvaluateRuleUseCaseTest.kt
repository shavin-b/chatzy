package com.orderflow.autoresponder

import com.orderflow.autoresponder.domain.model.AutoReplyRule
import com.orderflow.autoresponder.domain.model.MatchOption
import com.orderflow.autoresponder.domain.model.MessageLog
import com.orderflow.autoresponder.domain.model.MessageStatus
import com.orderflow.autoresponder.domain.repository.MessageLogRepository
import com.orderflow.autoresponder.domain.usecase.EvaluateRuleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class EvaluateRuleUseCaseTest {

    private lateinit var evaluateRuleUseCase: EvaluateRuleUseCase
    private val fakeRepo = object : MessageLogRepository {
        override fun getAllLogs(): Flow<List<MessageLog>> = flowOf(emptyList())
        override fun getLogsByPhone(phone: String): Flow<List<MessageLog>> = flowOf(emptyList())
        override suspend fun insertLog(log: MessageLog): Long = 1L
        override suspend fun clearLogs() {}
        override suspend fun getTodayRepliedCount(): Int = 0
        override suspend fun getTotalCount(): Int = 0
        override suspend fun getCountByStatus(status: MessageStatus): Int = 0
        override fun getRecentLogs(limit: Int): Flow<List<MessageLog>> = flowOf(emptyList())
        override suspend fun getLastSentTimestamp(phone: String): Long? = null
    }

    @Before
    fun setup() {
        evaluateRuleUseCase = EvaluateRuleUseCase(fakeRepo)
    }

    @Test
    fun exactMatch_successAndFailure() = runBlocking {
        val rule = AutoReplyRule(
            id = 1,
            ruleName = "Price Rule",
            keywordsCsv = "price, cost",
            matchOption = MatchOption.EXACT
        )
        val rules = listOf(rule)

        val match1 = evaluateRuleUseCase("price", rules, "123456789")
        assertNotNull("Exact match 'price' should match", match1)
        assertEquals(1L, match1?.id)

        val match2 = evaluateRuleUseCase("what is the price?", rules, "123456789")
        assertNull("Exact match should not match full sentence", match2)
    }

    @Test
    fun containsMatch_success() = runBlocking {
        val rule = AutoReplyRule(
            id = 2,
            ruleName = "Discount Rule",
            keywordsCsv = "discount, sale",
            matchOption = MatchOption.CONTAINS
        )
        val rules = listOf(rule)

        val match = evaluateRuleUseCase("Do you have any discount today?", rules, "123456789")
        assertNotNull(match)
        assertEquals(2L, match?.id)
    }

    @Test
    fun startsWithAndEndsWithMatch() = runBlocking {
        val startRule = AutoReplyRule(
            id = 3,
            ruleName = "Hello Rule",
            keywordsCsv = "hello, hi",
            matchOption = MatchOption.STARTS_WITH
        )
        val endRule = AutoReplyRule(
            id = 4,
            ruleName = "Thanks Rule",
            keywordsCsv = "thanks, bye",
            matchOption = MatchOption.ENDS_WITH
        )
        val rules = listOf(startRule, endRule)

        val startMatch = evaluateRuleUseCase("hi there how are you", rules, "123456789")
        assertEquals(3L, startMatch?.id)

        val endMatch = evaluateRuleUseCase("okay see you bye", rules, "123456789")
        assertEquals(4L, endMatch?.id)
    }

    @Test
    fun regexMatch_success() = runBlocking {
        val rule = AutoReplyRule(
            id = 5,
            ruleName = "Order ID Rule",
            keywordsCsv = "ORD-[0-9]{4}",
            matchOption = MatchOption.REGEX
        )
        val rules = listOf(rule)

        val match = evaluateRuleUseCase("Status for ORD-9821 please", rules, "123456789")
        assertNotNull("Regex pattern ORD-[0-9]{4} should match ORD-9821", match)
        assertEquals(5L, match?.id)

        val noMatch = evaluateRuleUseCase("Status for ORD-AB please", rules, "123456789")
        assertNull(noMatch)
    }

    @Test
    fun priorityOrdering_higherPriorityWins() = runBlocking {
        val lowPriorityRule = AutoReplyRule(
            id = 10,
            ruleName = "General Help",
            keywordsCsv = "help",
            matchOption = MatchOption.CONTAINS,
            priority = 1
        )
        val highPriorityRule = AutoReplyRule(
            id = 20,
            ruleName = "VIP Help",
            keywordsCsv = "help",
            matchOption = MatchOption.CONTAINS,
            priority = 10
        )
        val rules = listOf(lowPriorityRule, highPriorityRule)

        val match = evaluateRuleUseCase("I need help", rules, "123456789")
        assertNotNull(match)
        assertEquals("Higher priority rule must win", 20L, match?.id)
    }

    @Test
    fun fallbackAllRule_matchesWhenNoKeywordsMatch() = runBlocking {
        val keywordRule = AutoReplyRule(
            id = 1,
            ruleName = "Specific",
            keywordsCsv = "apple",
            matchOption = MatchOption.EXACT
        )
        val fallbackRule = AutoReplyRule(
            id = 99,
            ruleName = "Default Fallback",
            keywordsCsv = "",
            matchOption = MatchOption.ALL
        )
        val rules = listOf(keywordRule, fallbackRule)

        val match = evaluateRuleUseCase("something completely different", rules, "123456789")
        assertNotNull(match)
        assertEquals("Fallback ALL rule should match unmatched message", 99L, match?.id)
    }

    @Test
    fun groupAndPrivateChatFiltering() = runBlocking {
        val privateOnlyRule = AutoReplyRule(
            id = 1,
            ruleName = "Private Only",
            keywordsCsv = "info",
            matchOption = MatchOption.CONTAINS,
            enabledForPrivate = true,
            enabledForGroups = false
        )
        val groupOnlyRule = AutoReplyRule(
            id = 2,
            ruleName = "Group Only",
            keywordsCsv = "info",
            matchOption = MatchOption.CONTAINS,
            enabledForPrivate = false,
            enabledForGroups = true
        )
        val rules = listOf(privateOnlyRule, groupOnlyRule)

        val privateMatch = evaluateRuleUseCase("info please", rules, "123456789", isGroup = false)
        assertEquals("Private chat should match private-enabled rule", 1L, privateMatch?.id)

        val groupMatch = evaluateRuleUseCase("info please", rules, "123456789", isGroup = true)
        assertEquals("Group chat should match group-enabled rule", 2L, groupMatch?.id)
    }

    @Test
    fun contactWhitelistAndBlacklist() = runBlocking {
        val whitelistRule = AutoReplyRule(
            id = 1,
            ruleName = "VIP Only",
            keywordsCsv = "deal",
            matchOption = MatchOption.CONTAINS,
            replyToSpecificContacts = true,
            specificContactsCsv = "9876543210, 1122334455"
        )
        val rules = listOf(whitelistRule)

        val allowedMatch = evaluateRuleUseCase("deal", rules, "9876543210")
        assertNotNull("Whitelisted contact should match", allowedMatch)

        val blockedMatch = evaluateRuleUseCase("deal", rules, "5555555555")
        assertNull("Non-whitelisted contact should not match", blockedMatch)
    }
}
