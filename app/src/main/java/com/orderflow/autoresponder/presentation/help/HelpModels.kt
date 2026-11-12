package com.orderflow.autoresponder.presentation.help

enum class HelpAction(val id: String, val label: String) {
    SERVICE_STATUS("service_status", "Open Service status"),
    RULES("rules", "View rules"),
    NOTIFICATION_ACCESS("notification_access", "Open notification access"),
    SETTINGS("settings", "Open Settings");

    companion object {
        fun fromId(id: String): HelpAction? = entries.firstOrNull { it.id == id }
    }
}

data class HelpReply(val text: String, val actions: List<HelpAction> = emptyList())
