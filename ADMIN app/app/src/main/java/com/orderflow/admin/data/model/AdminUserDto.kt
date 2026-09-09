package com.orderflow.admin.data.model

import com.orderflow.admin.domain.model.AdminUser

data class AdminUserDto(
    val adminId: String = "",
    val email: String = "",
    val name: String = "Admin",
    val role: String = "Super Admin",
    val lastLogin: Long = 0L,
    val avatarUrl: String? = null
)

fun AdminUserDto.toDomain(): AdminUser {
    return AdminUser(
        adminId = adminId,
        email = email,
        name = name,
        role = role,
        lastLogin = lastLogin,
        avatarUrl = avatarUrl
    )
}
