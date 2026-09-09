package com.orderflow.admin.presentation.devicedetails

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

data class DeviceDetailsUiState(
    val device: Device? = null,
    val isLoading: Boolean = true,
    val actionMessage: String? = null
)

@HiltViewModel
class DeviceDetailsViewModel @Inject constructor(
    private val deviceUseCases: DeviceUseCases,
    private val subscriptionUseCases: SubscriptionUseCases,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deviceId: String = checkNotNull(savedStateHandle["deviceId"])
    private val _uiState = MutableStateFlow(DeviceDetailsUiState())
    val uiState: StateFlow<DeviceDetailsUiState> = _uiState

    init {
        loadDeviceDetails()
    }

    private fun loadDeviceDetails() {
        viewModelScope.launch {
            deviceUseCases.getDeviceDetailsStream(deviceId).collectLatest { device ->
                _uiState.value = _uiState.value.copy(device = device, isLoading = false)
            }
        }
    }

    fun quickExtendSubscription(days: Int) {
        val currentDevice = _uiState.value.device ?: return
        viewModelScope.launch {
            val res = subscriptionUseCases.updateSubscription(
                deviceId = currentDevice.deviceId,
                businessName = currentDevice.businessName,
                currentExpiry = currentDevice.subscriptionEnd,
                actionType = "Extend",
                days = days
            )
            if (res is Resource.Success) {
                _uiState.value = _uiState.value.copy(actionMessage = "Extended subscription by +$days days.")
            }
        }
    }

    fun setLifetimeSubscription() {
        val currentDevice = _uiState.value.device ?: return
        viewModelScope.launch {
            val res = subscriptionUseCases.updateSubscription(
                deviceId = currentDevice.deviceId,
                businessName = currentDevice.businessName,
                currentExpiry = currentDevice.subscriptionEnd,
                actionType = "Lifetime",
                days = 9999
            )
            if (res is Resource.Success) {
                _uiState.value = _uiState.value.copy(actionMessage = "Activated Lifetime Subscription!")
            }
        }
    }

    fun pauseSubscription() {
        val currentDevice = _uiState.value.device ?: return
        viewModelScope.launch {
            subscriptionUseCases.updateSubscription(
                deviceId = currentDevice.deviceId,
                businessName = currentDevice.businessName,
                currentExpiry = currentDevice.subscriptionEnd,
                actionType = "Pause",
                days = 0
            )
            _uiState.value = _uiState.value.copy(actionMessage = "Subscription Paused.")
        }
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }
}
