package com.orderflow.autoresponder.presentation.settings

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orderflow.autoresponder.core.security.SecureStorage
import com.orderflow.autoresponder.domain.model.MessageStatus
import com.orderflow.autoresponder.domain.repository.MessageLogRepository
import com.orderflow.autoresponder.domain.repository.ProcessedNotificationRepository
import com.orderflow.autoresponder.domain.repository.RuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosticsUiState(
    val deviceId: String = "",
    val lockStatus: String = "Unknown",
    val subscriptionStatus: String = "Unknown",
    val isListenerRunning: Boolean = false,
    val notificationAccessGranted: Boolean = false,
    val batteryOptimizationDisabled: Boolean = false,
    val totalRules: Int = 0,
    val activeRules: Int = 0,
    val totalProcessedMessages: Int = 0,
    val messagesDetected: Int = 0,
    val messagesIgnoredDuplicate: Int = 0,
    val rulesMatched: Int = 0,
    val replyAttempts: Int = 0,
    val replySuccesses: Int = 0,
    val replyFailures: Int = 0,
    val lastProcessedTime: Long = 0,
    val lastAutoReplyTime: Long = 0
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ruleRepository: RuleRepository,
    private val messageLogRepository: MessageLogRepository,
    private val processedNotificationRepository: ProcessedNotificationRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        refreshDiagnostics()
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            val pkgName = context.packageName
            val listeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            val isAccessGranted = listeners?.contains(pkgName) == true

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val isBatteryIgnoring = powerManager?.isIgnoringBatteryOptimizations(pkgName) ?: false

            val allRules = ruleRepository.getAllRules().first()
            val activeRules = allRules.filter { it.isActive }

            val totalProcessed = messageLogRepository.getTotalCount()
            val duplicates = messageLogRepository.getCountByStatus(MessageStatus.DUPLICATE)
            val successes = messageLogRepository.getCountByStatus(MessageStatus.SENT)
            val failures = messageLogRepository.getCountByStatus(MessageStatus.FAILED)
            val ignored = messageLogRepository.getCountByStatus(MessageStatus.IGNORED)
            
            val lastLog = messageLogRepository.getRecentLogs(1).first().firstOrNull()

            _uiState.value = DiagnosticsUiState(
                deviceId = secureStorage.getDeviceId() ?: "Unknown",
                lockStatus = if (secureStorage.isAdminLocked()) "LOCKED" else "UNLOCKED",
                subscriptionStatus = secureStorage.getSubscriptionStatus(),
                isListenerRunning = isAccessGranted,
                notificationAccessGranted = isAccessGranted,
                batteryOptimizationDisabled = isBatteryIgnoring,
                totalRules = allRules.size,
                activeRules = activeRules.size,
                totalProcessedMessages = totalProcessed,
                messagesDetected = totalProcessed,
                messagesIgnoredDuplicate = duplicates,
                rulesMatched = successes + failures, // Approximation: SENT/FAILED had matches
                replyAttempts = successes + failures,
                replySuccesses = successes,
                replyFailures = failures,
                lastProcessedTime = lastLog?.timestamp ?: 0L,
                lastAutoReplyTime = lastLog?.timestamp ?: 0L
            )
        }
    }

    fun resetLockState() {
        viewModelScope.launch {
            secureStorage.setAdminLocked(false)
            secureStorage.setLockReason("MANUAL_RESET")
            secureStorage.setSubscriptionStatus("ACTIVE")
            refreshDiagnostics()
        }
    }
}
