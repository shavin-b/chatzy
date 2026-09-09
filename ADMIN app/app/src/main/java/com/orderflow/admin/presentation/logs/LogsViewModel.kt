package com.orderflow.admin.presentation.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orderflow.admin.domain.model.LogEntry
import com.orderflow.admin.domain.repository.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogsUiState(
    val logs: List<LogEntry> = emptyList(),
    val filterCategory: String = "All",
    val isLoading: Boolean = false
)

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logRepository: LogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState(isLoading = true))
    val uiState: StateFlow<LogsUiState> = _uiState

    init {
        loadLogs()
    }

    fun onCategorySelected(category: String) {
        _uiState.value = _uiState.value.copy(filterCategory = category)
    }

    private fun loadLogs() {
        viewModelScope.launch {
            logRepository.getLogsStream().collectLatest { list ->
                _uiState.value = _uiState.value.copy(logs = list, isLoading = false)
            }
        }
    }
}
