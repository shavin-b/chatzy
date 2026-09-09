package com.orderflow.autoresponder.core.util

object WhatsAppConstants {
    const val WHATSAPP_PKG = "com.whatsapp"
    const val WHATSAPP_W4B_PKG = "com.whatsapp.w4b"

    const val TARGET_PACKAGE = WHATSAPP_W4B_PKG
    val SUPPORTED_PACKAGES = setOf(WHATSAPP_W4B_PKG)

    /**
     * Strictly verifies whether the package is the target WhatsApp Business package.
     * Regular WhatsApp (com.whatsapp) is intentionally excluded from auto-reply.
     */
    fun isWhatsApp(packageName: String?): Boolean {
        return packageName == WHATSAPP_W4B_PKG
    }
}
