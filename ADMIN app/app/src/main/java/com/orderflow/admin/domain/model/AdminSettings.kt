package com.orderflow.admin.domain.model

data class AdminSettings(
    val companyName: String = "OrderFlow Solutions",
    val companyLogoUrl: String = "",
    val supportEmail: String = "support@orderflow.app",
    val supportPhone: String = "+1 (800) 555-0199",
    val defaultSubscriptionDays: Int = 30,
    val notificationTemplates: List<String> = listOf(
        "Maintenance Alert: Scheduled maintenance in 1 hour.",
        "Subscription Notice: Your subscription expires soon.",
        "Feature Update: New OrderFlow client update available."
    ),
    val themeMode: String = "System",
    val autoBackupEnabled: Boolean = true
)
