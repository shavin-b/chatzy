package com.orderflow.autoresponder.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orderflow.autoresponder.core.security.SecureStorage
import com.orderflow.autoresponder.device.service.DeviceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val businessName: String = "",
    val whatsappNumber: String = "",
    val isSubmitting: Boolean = false,
    val nameError: String? = null,
    val phoneError: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val secureStorage: SecureStorage,
    private val deviceManager: DeviceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onBusinessNameChange(name: String) {
        _uiState.value = _uiState.value.copy(businessName = name, nameError = null)
    }

    fun onWhatsAppNumberChange(number: String) {
        _uiState.value = _uiState.value.copy(whatsappNumber = number, phoneError = null)
    }

    fun submitProfile() {
        val name = _uiState.value.businessName.trim()
        val phone = _uiState.value.whatsappNumber.trim().replace(" ", "")

        if (name.isEmpty()) {
            _uiState.value = _uiState.value.copy(nameError = "Business Name is required")
            return
        }

        if (phone.isEmpty() || phone.length < 9) {
            _uiState.value = _uiState.value.copy(phoneError = "Valid WhatsApp Number is required")
            return
        }

        // Canonical normalization
        val normalizedPhone = if (phone.startsWith("+")) phone else if (phone.startsWith("0")) "+94${phone.drop(1)}" else "+$phone"

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            
            // 1. Save Locally (Profile Completion)
            secureStorage.setBusinessName(name)
            secureStorage.setWhatsAppNumber(normalizedPhone)
            secureStorage.setProfileCompleted(true)
            secureStorage.setProfileSyncPending(true)

            // 2. Trigger Background Sync
            deviceManager.syncProfile()
            
            _uiState.value = _uiState.value.copy(isSubmitting = false)
        }
    }
}
