package com.orderflow.autoresponder.domain.usecase

import com.orderflow.autoresponder.core.util.Result
import com.orderflow.autoresponder.domain.repository.WhatsAppCloudApiRepository
import javax.inject.Inject

class SendCloudReplyUseCase @Inject constructor(
    private val repository: WhatsAppCloudApiRepository
) {
    suspend operator fun invoke(recipientPhone: String, messageText: String): Result<String> {
        if (recipientPhone.isBlank() || messageText.isBlank()) {
            return Result.Error(IllegalArgumentException("Recipient phone or message text cannot be empty"))
        }
        return repository.sendTextMessage(recipientPhone, messageText)
    }
}
