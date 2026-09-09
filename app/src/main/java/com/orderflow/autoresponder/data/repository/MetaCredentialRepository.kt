package com.orderflow.autoresponder.data.repository

import com.google.gson.Gson
import com.orderflow.autoresponder.core.util.Result
import com.orderflow.autoresponder.data.remote.MetaCredentialFetchService
import com.orderflow.autoresponder.data.remote.dto.MetaApiErrorResponse
import com.orderflow.autoresponder.data.remote.dto.MetaPhoneNumberDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain model carrying verified account information.
 */
data class MetaAccountInfo(
    val userId: String,
    val accountName: String
)

/**
 * Domain model for a WhatsApp Business phone number entry.
 */
data class MetaPhoneNumber(
    val phoneNumberId: String,
    val displayPhoneNumber: String,
    val verifiedName: String,
    val isVerified: Boolean
)

/**
 * Repository that uses [MetaCredentialFetchService] to verify Meta access tokens
 * and auto-fetch WhatsApp Business credentials from the Graph API.
 */
@Singleton
class MetaCredentialRepository @Inject constructor(
    private val fetchService: MetaCredentialFetchService
) {
    private val gson = Gson()

    /**
     * Verify the access token by calling /me.
     * Returns [MetaAccountInfo] on success or a [Result.Error] with a human-readable message.
     */
    suspend fun verifyToken(token: String): Result<MetaAccountInfo> {
        return try {
            val response = fetchService.getMe(accessToken = token)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.Success(MetaAccountInfo(userId = body.id, accountName = body.name))
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.Error(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.Error(Exception("Network error: ${e.localizedMessage}"))
        }
    }

    /**
     * Fetch the list of WhatsApp Business phone numbers linked to this token.
     */
    suspend fun fetchPhoneNumbers(token: String): Result<List<MetaPhoneNumber>> {
        return try {
            val response = fetchService.getPhoneNumbers(accessToken = token)
            if (response.isSuccessful && response.body() != null) {
                val phones = response.body()!!.data.map { it.toDomain() }
                if (phones.isEmpty()) {
                    Result.Error(Exception("No WhatsApp Business phone numbers found for this account."))
                } else {
                    Result.Success(phones)
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.Error(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.Error(Exception("Network error: ${e.localizedMessage}"))
        }
    }

    /**
     * Fetch Business Account ID using /me/businesses.
     * Returns the first business ID found, or null if none.
     */
    suspend fun fetchBusinessAccountId(token: String): Result<String?> {
        return try {
            val response = fetchService.getBusinesses(accessToken = token)
            if (response.isSuccessful && response.body() != null) {
                val businessId = response.body()!!.data.firstOrNull()?.id
                Result.Success(businessId)
            } else {
                // Non-fatal — not all tokens have business access
                Result.Success(null)
            }
        } catch (e: Exception) {
            Result.Success(null)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseErrorMessage(errorBodyString: String?): String {
        if (errorBodyString.isNullOrBlank()) return "Unknown Meta API error"
        return try {
            val errResponse = gson.fromJson(errorBodyString, MetaApiErrorResponse::class.java)
            errResponse.error?.message ?: "Unknown Meta API error"
        } catch (e: Exception) {
            "Meta API error: $errorBodyString"
        }
    }

    private fun MetaPhoneNumberDto.toDomain() = MetaPhoneNumber(
        phoneNumberId = id,
        displayPhoneNumber = displayPhoneNumber,
        verifiedName = verifiedName,
        isVerified = codeVerificationStatus.equals("VERIFIED", ignoreCase = true)
    )
}
