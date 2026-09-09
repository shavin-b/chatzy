package com.orderflow.admin.domain.repository

import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.domain.model.AdminUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentAdmin(): Flow<AdminUser?>
    suspend fun loginAdmin(email: String, pass: String, remember: Boolean): Resource<AdminUser>
    suspend fun sendPasswordResetEmail(email: String): Resource<Unit>
    suspend fun logoutAdmin(): Resource<Unit>
    suspend fun updateAdminProfile(name: String, avatarUrl: String?): Resource<Unit>
}
