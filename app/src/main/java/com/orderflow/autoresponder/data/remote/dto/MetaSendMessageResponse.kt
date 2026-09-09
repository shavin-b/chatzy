package com.orderflow.autoresponder.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MetaSendMessageResponse(
    @SerializedName("messaging_product") val messagingProduct: String?,
    @SerializedName("contacts") val contacts: List<ContactResponse>?,
    @SerializedName("messages") val messages: List<MessageResponse>?
)

data class ContactResponse(
    @SerializedName("input") val input: String?,
    @SerializedName("wa_id") val waId: String?
)

data class MessageResponse(
    @SerializedName("id") val id: String?
)
