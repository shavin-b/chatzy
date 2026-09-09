package com.orderflow.admin.domain.model

data class AdminUser(
    val adminId: String = "",
    val email: String = "",
    val name: String = "Admin",
    val role: String = "Admin", // Super Admin, Admin, Support
    val lastLogin: Long = System.currentTimeMillis(),
    val avatarUrl: String? = null
)
