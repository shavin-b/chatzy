package com.orderflow.admin.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orderflow.admin.core.common.Resource
import com.orderflow.admin.domain.model.NotificationItem
import com.orderflow.admin.domain.usecase.NotificationUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val title: String = "",
    val body: String = "",
    val type: String = "Announcement", // Announcement, Maintenance Notice, Subscription Reminder, Custom Message
    val targetDeviceId: String? = null,
    val isSending: Boolean = false,
    val messageSent: Boolean = false,
    val errorMessage: String? = null,
    val history: List<NotificationItem> = emptyList()
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationUseCases: NotificationUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            notificationUseCases.getNotificationsStream().collectLatest { list ->
                _uiState.value = _uiState.value.copy(history = list)
            }
        }
    }

    fun onTitleChanged(title: String) { _uiState.value = _uiState.value.copy(title = title, errorMessage = null) }
    fun onBodyChanged(body: String) { _uiState.value = _uiState.value.copy(body = body, errorMessage = null) }
    fun onTypeSelected(type: String) { _uiState.value = _uiState.value.copy(type = type) }

    fun sendNotification() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, errorMessage = null)
            val res = notificationUseCases.sendNotification(
                title = _uiState.value.title,
                body = _uiState.value.body,
                type = _uiState.value.type,
                targetDeviceId = _uiState.value.targetDeviceId
            )
            when (res) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        messageSent = true,
                        title = "",
                        body = ""
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isSending = false, errorMessage = res.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearStatus() {
        _uiState.value = _uiState.value.copy(messageSent = false, errorMessage = null)
    }
}
