package com.orderflow.autoresponder.presentation.dashboard

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orderflow.autoresponder.core.security.SecureStorage
import com.orderflow.autoresponder.domain.model.Subscription
import com.orderflow.autoresponder.domain.repository.CustomerRepository
import com.orderflow.autoresponder.domain.repository.MessageLogRepository
import com.orderflow.autoresponder.domain.repository.RuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import javax.inject.Inject

data class DashboardUiState(
    val isAutoResponderEnabled: Boolean = true,
    val isNotificationAccessGranted: Boolean = false,
    val totalRulesCount: Int = 0,
    val activeRulesCount: Int = 0,
    val todayRepliesCount: Int = 0,
    val totalCustomersCount: Int = 0,
    val subscription: Subscription = Subscription()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val messageLogRepository: MessageLogRepository,
    private val customerRepository: CustomerRepository,
    private val secureStorage: SecureStorage,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        val masterEnabled = secureStorage.isAutoResponderEnabled()
        val isAccessGranted = isNotificationServiceEnabled(context)

        combine(
            ruleRepository.getAllRules(),
            ruleRepository.getActiveRules()
        ) { allRules, activeRules ->
            val todayCount = messageLogRepository.getTodayRepliedCount()
            val customerCount = customerRepository.getCustomerCount()

            _uiState.value = DashboardUiState(
                isAutoResponderEnabled = masterEnabled,
                isNotificationAccessGranted = isAccessGranted,
                totalRulesCount = allRules.size,
                activeRulesCount = activeRules.size,
                todayRepliesCount = todayCount,
                totalCustomersCount = customerCount
            )
        }.launchIn(viewModelScope)
    }

    private fun isNotificationServiceEnabled(context: Context): Boolean {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = android.content.ComponentName.unflattenFromString(name)
                if (cn != null && pkgName == cn.packageName) {
                    return true
                }
            }
        }
        return false
    }

    fun toggleAutoResponder(enabled: Boolean) {
        secureStorage.setAutoResponderEnabled(enabled)
        _uiState.value = _uiState.value.copy(isAutoResponderEnabled = enabled)
    }
}
