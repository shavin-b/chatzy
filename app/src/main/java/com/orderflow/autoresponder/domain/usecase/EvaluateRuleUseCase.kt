package com.orderflow.autoresponder.domain.usecase

import com.orderflow.autoresponder.core.logger.StructuredLogger
import com.orderflow.autoresponder.domain.model.AutoReplyRule
import com.orderflow.autoresponder.domain.model.BusinessHours
import com.orderflow.autoresponder.domain.model.MatchOption
import com.orderflow.autoresponder.domain.repository.MessageLogRepository
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class EvaluateRuleUseCase @Inject constructor(
    private val messageLogRepository: MessageLogRepository
) {

    suspend operator fun invoke(
        incomingMessage: String,
        rules: List<AutoReplyRule>,
        senderPhone: String,
        isGroup: Boolean = false
    ): AutoReplyRule? {
        val trimmedInput = incomingMessage.trim()
        val activeRules = rules.filter { it.isActive }
            .sortedByDescending { it.priority }

        for (rule in activeRules) {
            // 1. Group/Private chat support
            if (isGroup && !rule.enabledForGroups) continue
            if (!isGroup && !rule.enabledForPrivate) continue

            // 2. Contact filtering
            if (!isContactAllowed(senderPhone, rule)) continue

            // 3. Cooldown check
            if (rule.isPauseForContact && rule.pauseDurationMinutes > 0) {
                val lastReplyTime = messageLogRepository.getLastSentTimestamp(senderPhone) ?: 0L
                val elapsedMinutes = (System.currentTimeMillis() - lastReplyTime) / (1000 * 60)
                if (elapsedMinutes < rule.pauseDurationMinutes) {
                    StructuredLogger.d("EvaluateRuleUseCase", "Rule '${rule.ruleName}' skipped: Cooldown active for $senderPhone")
                    continue
                }
            }

            // 4. Business hours
            if (!isWithinBusinessHours(rule.businessHours)) {
                StructuredLogger.d("EvaluateRuleUseCase", "Rule '${rule.ruleName}' skipped: outside business hours")
                continue
            }

            // 5. Match keyword
            if (isMatch(trimmedInput, rule)) {
                StructuredLogger.i("EvaluateRuleUseCase", "Matched rule '${rule.ruleName}' for message: $trimmedInput")
                return rule
            }
        }

        // Check for fallback ALL or AWAY rule
        return activeRules.firstOrNull { 
            (it.matchOption == MatchOption.ALL || it.matchOption == MatchOption.AWAY) && 
            (!isGroup || it.enabledForGroups) &&
            (isGroup || it.enabledForPrivate) &&
            isContactAllowed(senderPhone, it)
        }
    }

    private fun isContactAllowed(phone: String, rule: AutoReplyRule): Boolean {
        // Specific Contacts Whitelist
        if (rule.replyToSpecificContacts) {
            val whitelist = rule.specificContactsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (whitelist.none { phone.contains(it) }) return false
        }

        // Excluded Contacts Blacklist
        if (rule.excludeSpecificContacts) {
            val blacklist = rule.excludedContactsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (blacklist.any { phone.contains(it) }) return false
        }

        return true
    }

    private fun isMatch(input: String, rule: AutoReplyRule): Boolean {
        val keywords = rule.keywordsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val ignoreCase = !rule.caseSensitive
        
        return when (rule.matchOption) {
            MatchOption.EXACT -> keywords.any { it.equals(input, ignoreCase = ignoreCase) }
            MatchOption.CONTAINS -> keywords.any { input.contains(it, ignoreCase = ignoreCase) }
            MatchOption.STARTS_WITH -> keywords.any { input.startsWith(it, ignoreCase = ignoreCase) }
            MatchOption.ENDS_WITH -> keywords.any { input.endsWith(it, ignoreCase = ignoreCase) }
            MatchOption.REGEX -> keywords.any { keyword ->
                try {
                    val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
                    Regex(keyword, options).containsMatchIn(input)
                } catch (e: Exception) {
                    false
                }
            }
            MatchOption.ALL -> true
            MatchOption.AWAY -> true
        }
    }

    private fun isWithinBusinessHours(hours: BusinessHours): Boolean {
        if (!hours.isEnabled) return true

        val calendar = Calendar.getInstance()
        val currentDay = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            Calendar.SUNDAY -> "SUN"
            else -> ""
        }

        val activeDays = hours.activeDaysCsv.split(",").map { it.trim().uppercase(Locale.getDefault()) }
        if (!activeDays.contains(currentDay)) {
            return false
        }

        val currentMinuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinuteOfDay = hours.startHour * 60 + hours.startMinute
        val endMinuteOfDay = hours.endHour * 60 + hours.endMinute

        return currentMinuteOfDay in startMinuteOfDay..endMinuteOfDay
    }
}
