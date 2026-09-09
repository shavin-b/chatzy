package com.orderflow.autoresponder.domain.repository

import com.orderflow.autoresponder.core.util.Result
import com.orderflow.autoresponder.domain.model.MetaCredentials

interface WhatsAppCloudApiRepository {
    suspend fun sendTextMessage(
        recipientPhone: String,
        messageText: String
    ): Result<String>

    suspend fun testConnection(credentials: MetaCredentials): Result<Boolean>
}
