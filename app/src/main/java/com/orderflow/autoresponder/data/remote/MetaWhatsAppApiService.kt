package com.orderflow.autoresponder.data.remote

import com.orderflow.autoresponder.data.remote.dto.MetaSendMessageRequest
import com.orderflow.autoresponder.data.remote.dto.MetaSendMessageResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface MetaWhatsAppApiService {

    @POST("v19.0/{phone_number_id}/messages")
    suspend fun sendMessage(
        @Path("phone_number_id") phoneNumberId: String,
        @Header("Authorization") authorization: String,
        @Body request: MetaSendMessageRequest
    ): Response<MetaSendMessageResponse>
}
