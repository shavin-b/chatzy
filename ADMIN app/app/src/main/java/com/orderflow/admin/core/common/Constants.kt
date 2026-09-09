package com.orderflow.admin.core.common

object Constants {
    const val COLLECTION_ADMINS = "admins"
    const val COLLECTION_USERS = "users"
    const val COLLECTION_DEVICES = "devices"
    const val COLLECTION_SUBSCRIPTIONS = "subscriptions"
    const val COLLECTION_LOGS = "logs"
    const val COLLECTION_NOTIFICATIONS = "notifications"
    const val COLLECTION_SETTINGS = "settings"

    const val DATASTORE_NAME = "orderflow_admin_prefs"
    const val PREF_KEY_ADMIN_TOKEN = "admin_token"
    const val PREF_KEY_REMEMBER_LOGIN = "remember_login"
    const val PREF_KEY_THEME_MODE = "theme_mode"
    const val PREF_KEY_DEFAULT_SUB_DAYS = "default_sub_days"

    // Roles
    const val ROLE_SUPER_ADMIN = "Super Admin"
    const val ROLE_ADMIN = "Admin"
    const val ROLE_SUPPORT = "Support"

    // Device Status
    const val STATUS_ACTIVE = "Active"
    const val STATUS_EXPIRING_SOON = "Expiring Soon"
    const val STATUS_EXPIRED = "Expired"
    const val STATUS_SUSPENDED = "Suspended"
    const val STATUS_OFFLINE = "Offline"
    const val STATUS_UNINSTALLED = "Uninstalled"

    // Days Thresholds
    const val EXPIRING_SOON_DAYS = 7
}
