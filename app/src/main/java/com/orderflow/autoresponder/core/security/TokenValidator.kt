package com.orderflow.autoresponder.core.security

object TokenValidator {

    fun isValidPhoneNumberId(phoneId: String): Boolean {
        return phoneId.isNotBlank() && phoneId.all { it.isDigit() } && phoneId.length in 10..20
    }

    fun isValidAccessToken(token: String): Boolean {
        return token.isNotBlank() && token.length >= 20
    }

    fun isValidBusinessAccountId(accountId: String): Boolean {
        return accountId.isNotBlank() && accountId.all { it.isDigit() }
    }

    fun isValidWebhookVerifyToken(token: String): Boolean {
        return token.isNotBlank() && token.length >= 6
    }
}
