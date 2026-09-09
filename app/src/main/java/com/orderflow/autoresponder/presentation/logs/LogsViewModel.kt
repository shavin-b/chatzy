package com.orderflow.autoresponder.presentation.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orderflow.autoresponder.domain.model.MessageLog
import com.orderflow.autoresponder.domain.repository.MessageLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogsUiState(
    val logs: List<MessageLog> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val messageLogRepository: MessageLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    init {
        loadLogs()
    }

    private fun loadLogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            messageLogRepository.getAllLogs().collect { logsList ->
                _uiState.value = LogsUiState(logs = logsList, isLoading = false)
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            messageLogRepository.clearLogs()
        }
    }
}
