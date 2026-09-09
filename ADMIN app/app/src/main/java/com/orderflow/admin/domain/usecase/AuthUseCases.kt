package com.orderflow.admin.domain.usecase

import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.domain.model.AdminUser
import com.orderflow.admin.domain.repository.AuthRepository
import com.orderflow.admin.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthUseCases @Inject constructor(
    private val authRepository: AuthRepository,
    private val logRepository: LogRepository
) {
    fun getCurrentAdmin(): Flow<AdminUser?> = authRepository.getCurrentAdmin()

    suspend fun login(email: String, pass: String, remember: Boolean): Resource<AdminUser> {
        if (email.isBlank() || pass.isBlank()) {
            return Resource.Error("Email and Password cannot be empty.")
        }
        val result = authRepository.loginAdmin(email, pass, remember)
        if (result is Resource.Success) {
            logRepository.addLog(
                title = "Admin Login",
                description = "Admin ${result.data?.email} logged in successfully.",
                category = "Auth"
            )
        }
        return result
    }

    suspend fun logout(): Resource<Unit> {
        val result = authRepository.logoutAdmin()
        logRepository.addLog(
            title = "Admin Logout",
            description = "Admin logged out of console.",
            category = "Auth"
        )
        return result
    }

    suspend fun resetPassword(email: String): Resource<Unit> {
        if (email.isBlank()) return Resource.Error("Please enter your email address.")
        return authRepository.sendPasswordResetEmail(email)
    }
}
