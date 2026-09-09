package com.orderflow.admin.presentation.login

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.core.security.BiometricHelper
import com.orderflow.admin.domain.usecase.AuthUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "admin@orderflow.app",
    val pass: String = "admin123",
    val rememberLogin: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val showResetDialog: Boolean = false,
    val resetEmail: String = "",
    val resetMessage: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authUseCases: AuthUseCases,
    private val biometricHelper: BiometricHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onPasswordChanged(pass: String) {
        _uiState.value = _uiState.value.copy(pass = pass, errorMessage = null)
    }

    fun onRememberChanged(remember: Boolean) {
        _uiState.value = _uiState.value.copy(rememberLogin = remember)
    }

    fun login() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val res = authUseCases.login(
                email = _uiState.value.email,
                pass = _uiState.value.pass,
                remember = _uiState.value.rememberLogin
            )
            when (res) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun triggerBiometricLogin(activity: FragmentActivity) {
        if (biometricHelper.isBiometricAvailable()) {
            biometricHelper.promptBiometric(
                activity = activity,
                onSuccess = {
                    login()
                },
                onError = { err ->
                    _uiState.value = _uiState.value.copy(errorMessage = err)
                }
            )
        } else {
            _uiState.value = _uiState.value.copy(errorMessage = "Biometric authentication not supported on this device.")
        }
    }

    fun showResetDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showResetDialog = show, resetEmail = _uiState.value.email)
    }

    fun onResetEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(resetEmail = email)
    }

    fun sendPasswordReset() {
        viewModelScope.launch {
            val res = authUseCases.resetPassword(_uiState.value.resetEmail)
            if (res is Resource.Success) {
                _uiState.value = _uiState.value.copy(resetMessage = "Reset link sent to ${_uiState.value.resetEmail}")
            } else {
                _uiState.value = _uiState.value.copy(resetMessage = res.message)
            }
        }
    }
}
