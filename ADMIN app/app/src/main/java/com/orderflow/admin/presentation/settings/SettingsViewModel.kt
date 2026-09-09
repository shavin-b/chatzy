package com.orderflow.admin.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.core.security.DataStoreManager
import com.orderflow.admin.domain.model.AdminSettings
import com.orderflow.admin.domain.usecase.AuthUseCases
import com.orderflow.admin.domain.usecase.SettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AdminSettings = AdminSettings(),
    val isSaved: Boolean = false,
    val isLoggedOut: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsUseCases: SettingsUseCases,
    private val authUseCases: AuthUseCases,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsUseCases.getSettingsStream().collectLatest { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
    }

    fun onDefaultDaysChanged(days: Int) {
        val updated = _uiState.value.settings.copy(defaultSubscriptionDays = days)
        _uiState.value = _uiState.value.copy(settings = updated)
    }

    fun onThemeModeChanged(theme: String) {
        val updated = _uiState.value.settings.copy(themeMode = theme)
        _uiState.value = _uiState.value.copy(settings = updated)
        viewModelScope.launch {
            dataStoreManager.setThemeMode(theme)
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            settingsUseCases.saveSettings(_uiState.value.settings)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authUseCases.logout()
            _uiState.value = _uiState.value.copy(isLoggedOut = true)
        }
    }
}
