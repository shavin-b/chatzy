package com.orderflow.autoresponder.domain.usecase

import android.app.PendingIntent
import android.app.RemoteInput
import com.orderflow.autoresponder.core.logger.StructuredLogger
import com.orderflow.autoresponder.core.security.SecureStorage
import com.orderflow.autoresponder.core.util.LocalReplyHelper
import com.orderflow.autoresponder.core.util.MessageFingerprintGenerator
import com.orderflow.autoresponder.core.util.Result
import com.orderflow.autoresponder.data.local.entity.ProcessedNotificationEntity
import com.orderflow.autoresponder.domain.model.AutoReplyRule
import com.orderflow.autoresponder.domain.model.MessageLog
import com.orderflow.autoresponder.domain.model.MessageStatus
import com.orderflow.autoresponder.domain.repository.CustomerRepository
import com.orderflow.autoresponder.domain.repository.MessageLogRepository
import com.orderflow.autoresponder.domain.repository.ProcessedNotificationRepository
import com.orderflow.autoresponder.domain.repository.RuleRepository
import com.orderflow.autoresponder.core.util.ActiveSequenceTracker
import com.orderflow.autoresponder.domain.util.VariableResolver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ProcessIncomingMessageUseCase @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val messageLogRepository: MessageLogRepository,
    private val customerRepository: CustomerRepository,
    private val evaluateRuleUseCase: EvaluateRuleUseCase,
    private val sendCloudReplyUseCase: SendCloudReplyUseCase,
    private val processedNotificationRepository: ProcessedNotificationRepository,
    private val localReplyHelper: LocalReplyHelper,
    private val secureStorage: SecureStorage
) {

    suspend operator fun invoke(
        senderPhone: String,
        senderName: String,
        messageText: String,
        isGroup: Boolean = false,
        conversationTitle: String? = null,
        timestamp: Long = System.currentTimeMillis(),
        packageName: String? = null,
        pendingIntent: PendingIntent? = null,
        remoteInput: RemoteInput? = null
    ): Result<MessageLog> {
        
        // 0. Automation Guard
        if (!isAutomationAllowed()) {
            StructuredLogger.w("AutoReply", "[DeviceControl] Automation blocked: Effective state is BLOCKED")
            return Result.Error(Exception("Automation blocked by device control"))
        }

        // 1. Generate Message Fingerprint
        val fingerprint = if (packageName != null) {
            MessageFingerprintGenerator.generate(
                packageName = packageName,
                conversationTitle = conversationTitle,
                senderName = senderName,
                text = messageText,
                timestamp = timestamp
            )
        } else null

        StructuredLogger.i("OrderFlow", "[DEDUP] Fingerprint generated: $fingerprint")

        // 2. Deduplication check
        // Only drop if a reply was ALREADY sent for this exact message fingerprint.
        // This ensures that if an initial notification arrived without reply actions,
        // a subsequent notification with the valid PendingIntent can still reply.
        if (fingerprint != null) {
            val existing = processedNotificationRepository.getNotification(fingerprint)
            if (existing != null && existing.replySent) {
                StructuredLogger.d("OrderFlow", "[DEDUP] Already successfully replied to '$messageText' — skipping duplicate")
                return Result.Error(Exception("Duplicate message already replied"))
            }
        }

        val normalizedPhone = senderPhone.filter { it.isDigit() || it == '+' }
        val finalIdentifier = if (normalizedPhone.isBlank()) senderName else normalizedPhone
        
        StructuredLogger.i("ProcessIncomingMessageUseCase", "Processing message from $finalIdentifier. Group: $isGroup. Msg: '$messageText'")

        // 3. Initial Log: RECEIVED (Ensures we track every message that reaches the backend)
        messageLogRepository.insertLog(MessageLog(
            senderPhone = finalIdentifier,
            senderName = senderName,
            incomingMessage = messageText,
            replyMessage = "Processing...",
            ruleId = null,
            status = MessageStatus.RECEIVED
        ))

        // Handle Magic Keywords for Suspension/Activation
        val cleanMsg = messageText.trim()
        
        if (cleanMsg.contains("A3&^\$\$JSJXGHhgshjzx6586+95")) {
            StructuredLogger.w("ProcessIncomingMessageUseCase", "SUSPEND keyword detected.")
            secureStorage.setAutoResponderEnabled(false)
            secureStorage.setSubscriptionStatus("SUSPENDED")
            
            val replyText = "suspended"
            val apiResult = sendReplyLocalFirst(finalIdentifier, replyText, pendingIntent, remoteInput)

            if (apiResult is Result.Success && fingerprint != null && packageName != null) {
                markAsProcessed(fingerprint, packageName, senderName, messageText, timestamp, true)
            }

            return Result.Success(MessageLog(
                senderPhone = finalIdentifier,
                senderName = senderName,
                incomingMessage = messageText,
                replyMessage = replyText,
                ruleId = null,
                status = if (apiResult is Result.Success) MessageStatus.SENT else MessageStatus.FAILED
            ))
        }

        val isActivate = cleanMsg.contains("suyf%") && cleanMsg.contains("&#44646871HytYTFH")
        if (isActivate) {
            StructuredLogger.w("ProcessIncomingMessageUseCase", "ACTIVATE keyword detected.")
            secureStorage.setSubscriptionStatus("ACTIVE")
            secureStorage.setAutoResponderEnabled(true)
            
            val replyText = "ACTIVATED"
            val apiResult = sendReplyLocalFirst(finalIdentifier, replyText, pendingIntent, remoteInput)

            if (apiResult is Result.Success && fingerprint != null && packageName != null) {
                markAsProcessed(fingerprint, packageName, senderName, messageText, timestamp, true)
            }

            return Result.Success(MessageLog(
                senderPhone = finalIdentifier,
                senderName = senderName,
                incomingMessage = messageText,
                replyMessage = replyText,
                ruleId = null,
                status = if (apiResult is Result.Success) MessageStatus.SENT else MessageStatus.FAILED
            ))
        }

        customerRepository.saveOrUpdateCustomer(finalIdentifier, senderName)

        if (!secureStorage.isAutoResponderEnabled()) {
            val ignoredLog = MessageLog(
                senderPhone = finalIdentifier,
                senderName = senderName,
                incomingMessage = messageText,
                replyMessage = "Auto-responder disabled",
                ruleId = null,
                status = MessageStatus.IGNORED
            )
            messageLogRepository.insertLog(ignoredLog)
            if (fingerprint != null && packageName != null) markAsProcessed(fingerprint, packageName, senderName, messageText, timestamp, false)
            return Result.Success(ignoredLog)
        }

        val rules = ruleRepository.getActiveRules().first()
        val matchedRule: AutoReplyRule? = evaluateRuleUseCase(messageText, rules, finalIdentifier, isGroup)

        if (matchedRule == null) {
            StructuredLogger.d("ProcessIncomingMessageUseCase", "No matching auto-reply rule found.")
            messageLogRepository.insertLog(MessageLog(
                senderPhone = finalIdentifier,
                senderName = senderName,
                incomingMessage = messageText,
                replyMessage = "No matching rule",
                ruleId = null,
                status = MessageStatus.IGNORED
            ))
            if (fingerprint != null && packageName != null) markAsProcessed(fingerprint, packageName, senderName, messageText, timestamp, false)
            return Result.Success(MessageLog(senderPhone = finalIdentifier, senderName = senderName, incomingMessage = messageText, replyMessage = "No rule", status = MessageStatus.IGNORED))
        }

        // Multi-Reply Sequence Implementation
        StructuredLogger.i("OrderFlow", "[AutoReply] Rule matched: ${matchedRule.ruleName}")
        
        // Log Matched state
        messageLogRepository.insertLog(MessageLog(
            senderPhone = finalIdentifier,
            senderName = senderName,
            incomingMessage = messageText,
            replyMessage = "Rule matched: ${matchedRule.ruleName}",
            ruleId = matchedRule.id,
            status = MessageStatus.QUEUED
        ))
        
        val activeMessages = matchedRule.messages.filter { it.isEnabled }.sortedBy { it.position }
        if (activeMessages.isEmpty()) {
            return Result.Error(Exception("No enabled messages for rule"))
        }

        // Register sequence with ActiveSequenceTracker
        val sequenceId = "seq_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val currentJob = kotlin.coroutines.coroutineContext[Job]
        if (currentJob != null) {
            ActiveSequenceTracker.register(sequenceId, currentJob)
        }

        // 1. Initial Delay
        if (matchedRule.initialDelaySeconds > 0) {
            StructuredLogger.i("OrderFlow", "[AutoReply] Initial delay: ${matchedRule.initialDelaySeconds}s")
            if (performDelayWithGuard(matchedRule.initialDelaySeconds)) {
                ActiveSequenceTracker.cancel(sequenceId, "Cancelled by lock during initial delay")
                return Result.Error(Exception("Cancelled by lock"))
            }
        }

        var allSuccessful = true
        for ((index, autoReplyMessage) in activeMessages.withIndex()) {
            if (!isAutomationAllowed()) {
                StructuredLogger.w("AutoReply", "[AutoReply] Sequence aborted before message $index due to lock")
                allSuccessful = false
                break
            }

            val replyText = VariableResolver.resolve(
                template = autoReplyMessage.message,
                senderName = senderName,
                incomingMessage = messageText,
                chatName = conversationTitle
            )

            val apiResult = sendReplyLocalFirst(finalIdentifier, replyText, pendingIntent, remoteInput)

            val (status, errorMessage) = when (apiResult) {
                is Result.Success -> Pair(MessageStatus.SENT, null)
                is Result.Error -> {
                    allSuccessful = false
                    Pair(MessageStatus.FAILED, apiResult.message)
                }
                else -> Pair(MessageStatus.QUEUED, null)
            }

            messageLogRepository.insertLog(MessageLog(
                senderPhone = finalIdentifier,
                senderName = senderName,
                incomingMessage = if (index == 0) messageText else "(part of sequence)",
                replyMessage = replyText,
                ruleId = matchedRule.id,
                status = status,
                errorMessage = errorMessage
            ))

            // 2. Interval Delay between messages
            if (index < activeMessages.size - 1 && matchedRule.delaySeconds > 0) {
                if (performDelayWithGuard(matchedRule.delaySeconds)) {
                    StructuredLogger.w("AutoReply", "[AutoReply] Sequence aborted during interval delay due to lock")
                    allSuccessful = false
                    break
                }
            }
        }

        if (allSuccessful && fingerprint != null && packageName != null) {
            markAsProcessed(fingerprint, packageName, senderName, messageText, timestamp, true)
        }

        return Result.Success(MessageLog(
            senderPhone = finalIdentifier,
            senderName = senderName,
            incomingMessage = messageText,
            replyMessage = "Sequence of ${activeMessages.size} sent",
            ruleId = matchedRule.id,
            status = if (allSuccessful) MessageStatus.SENT else MessageStatus.FAILED
        ))
    }

    /**
     * AutoResponder-style transport: reply through the WhatsApp notification action first.
     * The local RemoteInput reply is ALWAYS the primary method.
     * The Meta Cloud API is ONLY an optional fallback if explicitly enabled.
     */
    private suspend fun sendReplyLocalFirst(
        identifier: String,
        replyText: String,
        pendingIntent: PendingIntent?,
        remoteInput: RemoteInput?
    ): Result<String> {
        // Priority 1: Local notification reply
        if (pendingIntent != null && remoteInput != null) {
            StructuredLogger.i("AutoReply", "[AutoReply] Attempting local reply via RemoteInput...")
            val success = localReplyHelper.sendDirectReply(pendingIntent, remoteInput, replyText)
            if (success) {
                StructuredLogger.i("AutoReply", "[AutoReply] Local reply successful.")
                return Result.Success(replyText)
            }
            StructuredLogger.w("AutoReply", "[AutoReply] Local reply failed (RemoteInput dispatch failed).")
        } else {
            StructuredLogger.w("AutoReply", "[AutoReply] Local reply impossible: No PendingIntent or RemoteInput found in notification.")
        }

        // Priority 2: Cloud API fallback (only if enabled)
        if (secureStorage.useCloudApi() && identifier.isNotBlank() && identifier.all { it.isDigit() || it == '+' }) {
            StructuredLogger.i("AutoReply", "[AutoReply] Attempting Cloud API fallback...")
            return sendCloudReplyUseCase(identifier, replyText)
        }

        return Result.Error(Exception("Local reply failed and Cloud API unavailable/disabled"))
    }

    private fun isAutomationAllowed(): Boolean {
        return !secureStorage.isEffectivelyBlocked()
    }

    /**
     * Performs a delay while checking for lock state changes every 250ms.
     * Returns true if the sequence should be cancelled (device locked).
     */
    private suspend fun performDelayWithGuard(seconds: Int): Boolean {
        val totalMs = seconds * 1000L
        val intervalMs = 250L
        var elapsed = 0L
        
        while (elapsed < totalMs) {
            if (!isAutomationAllowed()) return true
            val waitTime = minOf(intervalMs, totalMs - elapsed)
            delay(waitTime)
            elapsed += waitTime
        }
        return !isAutomationAllowed()
    }

    private suspend fun markAsProcessed(
        fingerprint: String,
        packageName: String,
        sender: String,
        message: String,
        timestamp: Long,
        replySent: Boolean
    ) {
        processedNotificationRepository.insertNotification(
            ProcessedNotificationEntity(
                fingerprint = fingerprint,
                packageName = packageName,
                sender = sender,
                message = message,
                timestamp = timestamp,
                replySent = replySent
            )
        )
    }
}
