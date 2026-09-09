package com.orderflow.admin.presentation.devicelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orderflow.admin.domain.model.Device
import com.orderflow.admin.domain.usecase.DeviceUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceListUiState(
    val searchQuery: String = "",
    val filter: String = "All", // All, Only Active, Only Expired, Only Offline, Only Online, Expiring Soon
    val sortBy: String = "Business Name", // Business Name, Remaining Days, Last Seen, Install Date
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val deviceUseCases: DeviceUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceListUiState(isLoading = true))
    val uiState: StateFlow<DeviceListUiState> = _uiState

    init {
        loadDevices()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadDevices()
    }

    fun onFilterSelected(filter: String) {
        _uiState.value = _uiState.value.copy(filter = filter)
        loadDevices()
    }

    fun onSortBySelected(sortBy: String) {
        _uiState.value = _uiState.value.copy(sortBy = sortBy)
        loadDevices()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            loadDevices()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    private fun loadDevices() {
        viewModelScope.launch {
            deviceUseCases.getDevicesStream(
                searchQuery = _uiState.value.searchQuery,
                filter = _uiState.value.filter,
                sortBy = _uiState.value.sortBy
            ).collectLatest { list ->
                _uiState.value = _uiState.value.copy(devices = list, isLoading = false)
            }
        }
    }
}
