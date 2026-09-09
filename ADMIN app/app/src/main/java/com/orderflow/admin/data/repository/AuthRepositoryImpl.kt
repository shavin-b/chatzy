package com.orderflow.admin.data.repository

import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.core.security.DataStoreManager
import com.orderflow.admin.data.model.toDomain
import com.orderflow.admin.data.remote.FirebaseAuthSource
import com.orderflow.admin.domain.model.AdminUser
import com.orderflow.admin.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authSource: FirebaseAuthSource,
    private val dataStoreManager: DataStoreManager
) : AuthRepository {

    override fun getCurrentAdmin(): Flow<AdminUser?> {
        return authSource.getCurrentUserFlow().map { it?.toDomain() }
    }

    override suspend fun loginAdmin(email: String, pass: String, remember: Boolean): Resource<AdminUser> {
        val res = authSource.login(email, pass)
        return when (res) {
            is Resource.Success -> {
                val user = res.data?.toDomain()!!
                if (remember) {
                    dataStoreManager.saveSession(user.adminId, true)
                }
                Resource.Success(user)
            }
            is Resource.Error -> Resource.Error(res.message ?: "Authentication Failed")
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Resource<Unit> {
        return authSource.sendPasswordReset(email)
    }

    override suspend fun logoutAdmin(): Resource<Unit> {
        authSource.logout()
        dataStoreManager.clearSession()
        return Resource.Success(Unit)
    }

    override suspend fun updateAdminProfile(name: String, avatarUrl: String?): Resource<Unit> {
        return Resource.Success(Unit)
    }
}
