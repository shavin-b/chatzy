package com.orderflow.admin.presentation.subscription

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.domain.model.Device
import com.orderflow.admin.domain.usecase.DeviceUseCases
import com.orderflow.admin.domain.usecase.SubscriptionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val device: Device? = null,
    val selectedAction: String = "Extend", // Extend, Reduce, Pause, Resume, Deactivate, Reactivate, Lifetime
    val customDays: Int = 30,
    val isLoading: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val deviceUseCases: DeviceUseCases,
    private val subscriptionUseCases: SubscriptionUseCases,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deviceId: String = checkNotNull(savedStateHandle["deviceId"])
    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState

    init {
        loadDevice()
    }

    private fun loadDevice() {
        viewModelScope.launch {
            deviceUseCases.getDeviceDetailsStream(deviceId).collectLatest { device ->
                _uiState.value = _uiState.value.copy(device = device)
            }
        }
    }

    fun onActionSelected(action: String) {
        _uiState.value = _uiState.value.copy(selectedAction = action)
    }

    fun onCustomDaysChanged(days: Int) {
        _uiState.value = _uiState.value.copy(customDays = days)
    }

    fun applySubscriptionUpdate() {
        val device = _uiState.value.device ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val res = subscriptionUseCases.updateSubscription(
                deviceId = device.deviceId,
                businessName = device.businessName,
                currentExpiry = device.subscriptionEnd,
                actionType = _uiState.value.selectedAction,
                days = _uiState.value.customDays
            )
            when (res) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, showSuccessDialog = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun dismissSuccessDialog() {
        _uiState.value = _uiState.value.copy(showSuccessDialog = false)
    }
}
