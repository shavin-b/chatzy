package com.orderflow.autoresponder.domain.model

data class MetaCredentials(
    val phoneNumberId: String = "",
    val accessToken: String = "",
    val businessAccountId: String = "",
    val webhookVerifyToken: String = ""
)
