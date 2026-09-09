package com.orderflow.admin.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orderflow.admin.domain.model.AdminUser
import com.orderflow.admin.domain.usecase.AuthUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _adminUser = MutableStateFlow<AdminUser?>(null)
    val adminUser: StateFlow<AdminUser?> = _adminUser

    init {
        viewModelScope.launch {
            authUseCases.getCurrentAdmin().collectLatest { user ->
                _adminUser.value = user ?: AdminUser(name = "Master Admin", role = "Super Admin", email = "admin@orderflow.app")
            }
        }
    }
}
