package com.orderflow.autoresponder.domain.usecase

import com.orderflow.autoresponder.core.logger.StructuredLogger
import com.orderflow.autoresponder.core.util.Result
import com.orderflow.autoresponder.domain.repository.MessageLogRepository
import javax.inject.Inject

class SyncMessagesUseCase @Inject constructor(
    private val messageLogRepository: MessageLogRepository
) {
    suspend operator fun invoke(): Result<Boolean> {
        return try {
            val count = messageLogRepository.getTodayRepliedCount()
            StructuredLogger.i("SyncMessagesUseCase", "Today total auto-replied count: $count")
            Result.Success(true)
        } catch (e: Exception) {
            StructuredLogger.e("SyncMessagesUseCase", "Sync error", e)
            Result.Error(e)
        }
    }
}
