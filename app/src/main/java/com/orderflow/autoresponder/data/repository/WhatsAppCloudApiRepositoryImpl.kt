package com.orderflow.autoresponder.data.repository

import com.orderflow.autoresponder.core.logger.StructuredLogger
import com.orderflow.autoresponder.core.security.SecureStorage
import com.orderflow.autoresponder.core.util.Result
import com.orderflow.autoresponder.data.remote.MetaWhatsAppApiService
import com.orderflow.autoresponder.data.remote.dto.MetaSendMessageRequest
import com.orderflow.autoresponder.data.remote.dto.TextPayload
import com.orderflow.autoresponder.domain.model.MetaCredentials
import com.orderflow.autoresponder.domain.repository.WhatsAppCloudApiRepository
import javax.inject.Inject

class WhatsAppCloudApiRepositoryImpl @Inject constructor(
    private val apiService: MetaWhatsAppApiService,
    private val secureStorage: SecureStorage
) : WhatsAppCloudApiRepository {

    override suspend fun sendTextMessage(
        recipientPhone: String,
        messageText: String
    ): Result<String> {
        val credentials = secureStorage.getMetaCredentials()
        if (credentials.phoneNumberId.isBlank() || credentials.accessToken.isBlank()) {
            StructuredLogger.w("WhatsAppCloudApiRepositoryImpl", "Meta credentials are not configured")
            return Result.Error(IllegalStateException("Meta Phone Number ID or Access Token is missing in Settings."))
        }

        return try {
            val request = MetaSendMessageRequest(
                toPhone = recipientPhone,
                text = TextPayload(body = messageText)
            )

            val authHeader = "Bearer ${credentials.accessToken}"
            val response = apiService.sendMessage(
                phoneNumberId = credentials.phoneNumberId,
                authorization = authHeader,
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val messageId = body.messages?.firstOrNull()?.id ?: "SUCCESS_ID"
                StructuredLogger.i("WhatsAppCloudApiRepositoryImpl", "Successfully sent WhatsApp message. ID: $messageId")
                Result.Success(messageId)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                StructuredLogger.e("WhatsAppCloudApiRepositoryImpl", "API failure response: $errorBody")
                Result.Error(Exception("Meta API HTTP ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            StructuredLogger.e("WhatsAppCloudApiRepositoryImpl", "Exception sending Meta WhatsApp message", e)
            Result.Error(e)
        }
    }

    override suspend fun testConnection(credentials: MetaCredentials): Result<Boolean> {
        return try {
            if (credentials.phoneNumberId.isBlank() || credentials.accessToken.isBlank()) {
                return Result.Error(IllegalArgumentException("Credentials cannot be empty"))
            }
            val request = MetaSendMessageRequest(
                toPhone = credentials.phoneNumberId,
                text = TextPayload(body = "OrderFlow API Connection Verification Test")
            )
            val authHeader = "Bearer ${credentials.accessToken}"
            val response = apiService.sendMessage(
                phoneNumberId = credentials.phoneNumberId,
                authorization = authHeader,
                request = request
            )
            if (response.code() in 200..499) {
                Result.Success(true)
            } else {
                Result.Error(Exception("Server returned status code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
