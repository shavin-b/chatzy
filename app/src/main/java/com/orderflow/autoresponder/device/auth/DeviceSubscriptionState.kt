package com.orderflow.autoresponder.device.auth

/** In enrolled mode, admin dashboard subscription status and expiry are server authority. */
object DeviceSubscriptionState {
    fun resolve(status: String?, legacyStatus: String?, expiry: Long?, now: Long): String {
        fun normalize(value: String?) = value?.trim()?.uppercase()?.replace(' ', '_')
        val display = normalize(status)
        val effective = if (display in setOf("ACTIVE", "SUSPENDED", "EXPIRED", "EXPIRING_SOON", "TRIAL")) display else normalize(legacyStatus)
        if (effective == "SUSPENDED") return "SUSPENDED"
        if (expiry != null && expiry > 0 && expiry <= now) return "EXPIRED"
        return if (effective in setOf("ACTIVE", "EXPIRING_SOON", "TRIAL", "EXPIRED")) effective!! else "SUSPENDED"
    }
}
