package com.orderflow.admin.data.model

import com.orderflow.admin.domain.model.AdminSettings

data class SettingsDto(
    val companyName: String = "OrderFlow Solutions",
    val companyLogoUrl: String = "",
    val supportEmail: String = "support@orderflow.app",
    val supportPhone: String = "+1 (800) 555-0199",
    val defaultSubscriptionDays: Int = 30,
    val notificationTemplates: List<String> = emptyList(),
    val themeMode: String = "System",
    val autoBackupEnabled: Boolean = true
)

fun SettingsDto.toDomain(): AdminSettings {
    return AdminSettings(
        companyName = companyName,
        companyLogoUrl = companyLogoUrl,
        supportEmail = supportEmail,
        supportPhone = supportPhone,
        defaultSubscriptionDays = defaultSubscriptionDays,
        notificationTemplates = notificationTemplates.ifEmpty {
            listOf(
                "Maintenance Alert: Scheduled maintenance in 1 hour.",
                "Subscription Notice: Your subscription expires soon.",
                "Feature Update: New OrderFlow client update available."
            )
        },
        themeMode = themeMode,
        autoBackupEnabled = autoBackupEnabled
    )
}

fun AdminSettings.toDto(): SettingsDto {
    return SettingsDto(
        companyName = companyName,
        companyLogoUrl = companyLogoUrl,
        supportEmail = supportEmail,
        supportPhone = supportPhone,
        defaultSubscriptionDays = defaultSubscriptionDays,
        notificationTemplates = notificationTemplates,
        themeMode = themeMode,
        autoBackupEnabled = autoBackupEnabled
    )
}
