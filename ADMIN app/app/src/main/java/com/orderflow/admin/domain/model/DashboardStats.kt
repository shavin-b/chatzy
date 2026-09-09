package com.orderflow.admin.domain.model

data class DashboardStats(
    val totalDevices: Int = 0,
    val onlineDevices: Int = 0,
    val offlineDevices: Int = 0,
    val expiredDevices: Int = 0,
    val expiringSoonDevices: Int = 0,
    val activeTodayDevices: Int = 0,
    val weeklyInstalls: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val monthlyInstalls: Int = 0,
    val topActiveBusinesses: List<String> = emptyList()
)
