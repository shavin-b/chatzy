package com.orderflow.autoresponder.domain.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VariableResolver {

    /**
     * Resolves variables in the reply text.
     * Supported: {sender}, {message}, {time}, {date}, {chat}
     */
    fun resolve(
        template: String,
        senderName: String,
        incomingMessage: String,
        chatName: String? = null
    ): String {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return template
            .replace("{sender}", senderName, ignoreCase = true)
            .replace("{message}", incomingMessage, ignoreCase = true)
            .replace("{chat}", chatName ?: senderName, ignoreCase = true)
            .replace("{time}", timeFormat.format(now), ignoreCase = true)
            .replace("{date}", dateFormat.format(now), ignoreCase = true)
            // Legacy support if %name% was used
            .replace("%name%", senderName, ignoreCase = true)
            .replace("%phone%", senderName, ignoreCase = true)
    }
}
