package com.orderflow.autoresponder.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MetaWebhookPayload(
    @SerializedName("object") val objectType: String?,
    @SerializedName("entry") val entryList: List<WebhookEntry>?
)

data class WebhookEntry(
    @SerializedName("id") val id: String?,
    @SerializedName("changes") val changes: List<WebhookChange>?
)

data class WebhookChange(
    @SerializedName("field") val field: String?,
    @SerializedName("value") val value: WebhookValue?
)

data class WebhookValue(
    @SerializedName("messaging_product") val messagingProduct: String?,
    @SerializedName("metadata") val metadata: WebhookMetadata?,
    @SerializedName("contacts") val contacts: List<WebhookContact>?,
    @SerializedName("messages") val messages: List<WebhookMessage>?
)

data class WebhookMetadata(
    @SerializedName("display_phone_number") val displayPhoneNumber: String?,
    @SerializedName("phone_number_id") val phoneNumberId: String?
)

data class WebhookContact(
    @SerializedName("profile") val profile: WebhookProfile?,
    @SerializedName("wa_id") val waId: String?
)

data class WebhookProfile(
    @SerializedName("name") val name: String?
)

data class WebhookMessage(
    @SerializedName("from") val from: String?,
    @SerializedName("id") val id: String?,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("text") val text: WebhookTextMessage?
)

data class WebhookTextMessage(
    @SerializedName("body") val body: String?
)
