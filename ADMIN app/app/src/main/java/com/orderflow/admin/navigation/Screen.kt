package com.orderflow.admin.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object DeviceList : Screen("devices")
    object DeviceDetails : Screen("device_details/{deviceId}") {
        fun createRoute(deviceId: String) = "device_details/$deviceId"
    }
    object SubscriptionEditor : Screen("subscription_editor/{deviceId}") {
        fun createRoute(deviceId: String) = "subscription_editor/$deviceId"
    }
    object Notifications : Screen("notifications")
    object Logs : Screen("logs")
    object Analytics : Screen("analytics")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
}
