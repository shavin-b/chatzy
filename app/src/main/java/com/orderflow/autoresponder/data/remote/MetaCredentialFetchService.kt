package com.orderflow.autoresponder.data.remote

import com.orderflow.autoresponder.data.remote.dto.MetaBusinessesResponse
import com.orderflow.autoresponder.data.remote.dto.MetaMeResponse
import com.orderflow.autoresponder.data.remote.dto.MetaPhoneNumbersResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Retrofit interface for Meta Graph API endpoints used to fetch and verify
 * WhatsApp Business API credentials.
 *
 * Base URL: https://graph.facebook.com/  (configured in NetworkModule)
 */
interface MetaCredentialFetchService {

    /**
     * Verify the access token and retrieve the associated account name.
     * GET /me?fields=id,name&access_token=<token>
     */
    @GET("v19.0/me")
    suspend fun getMe(
        @Query("fields")       fields: String = "id,name",
        @Query("access_token") accessToken: String
    ): Response<MetaMeResponse>

    /**
     * List all WhatsApp phone numbers linked to this access token.
     * GET /me/phone_numbers?fields=id,display_phone_number,verified_name,code_verification_status
     */
    @GET("v19.0/me/phone_numbers")
    suspend fun getPhoneNumbers(
        @Query("fields")       fields: String = "id,display_phone_number,verified_name,code_verification_status",
        @Query("access_token") accessToken: String
    ): Response<MetaPhoneNumbersResponse>

    /**
     * List businesses associated with the token (for Business Account ID).
     * GET /me/businesses?fields=id,name
     */
    @GET("v19.0/me/businesses")
    suspend fun getBusinesses(
        @Query("fields")       fields: String = "id,name",
        @Query("access_token") accessToken: String
    ): Response<MetaBusinessesResponse>
}
