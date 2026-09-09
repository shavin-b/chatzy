package com.orderflow.autoresponder.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MetaSendMessageRequest(
    @SerializedName("messaging_product") val messagingProduct: String = "whatsapp",
    @SerializedName("recipient_type") val recipientType: String = "individual",
    @SerializedName("to") val toPhone: String,
    @SerializedName("type") val type: String = "text",
    @SerializedName("text") val text: TextPayload
)

data class TextPayload(
    @SerializedName("preview_url") val previewUrl: Boolean = false,
    @SerializedName("body") val body: String
)
