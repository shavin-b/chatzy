package com.orderflow.autoresponder.presentation.settings

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orderflow.autoresponder.core.security.SecureStorage
import com.orderflow.autoresponder.core.security.TokenValidator
import com.orderflow.autoresponder.core.util.BatteryOptimizationHelper
import com.orderflow.autoresponder.core.util.DeviceBrandHelper
import com.orderflow.autoresponder.core.util.Result
import com.orderflow.autoresponder.data.repository.MetaAccountInfo
import com.orderflow.autoresponder.data.repository.MetaCredentialRepository
import com.orderflow.autoresponder.data.repository.MetaPhoneNumber
import com.orderflow.autoresponder.device.service.DeviceManager
import com.orderflow.autoresponder.domain.model.MetaCredentials
import com.orderflow.autoresponder.domain.repository.WhatsAppCloudApiRepository
import com.orderflow.autoresponder.service.NotificationServiceWatchdog
import com.orderflow.autoresponder.service.WhatsAppNotificationListener
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SettingsUiState(
    // Business Profile
    val businessName: String = "",
    val whatsappNumber: String = "",
    val isProfileSyncing: Boolean = false,

    // Persisted credentials
    val credentials: MetaCredentials = MetaCredentials(),

    // Legacy test connection
    val isTestingConnection: Boolean = false,
    val testResultMessage: String? = null,
    val isTestSuccess: Boolean = false,
    val validationErrorMessage: String? = null,
    val isNotificationAccessGranted: Boolean = false,
    val isNotificationAccessRestricted: Boolean = false,
    val isBatteryOptimizationDisabled: Boolean = false,
    val useCloudApi: Boolean = false,

    // OEM Brand & Background Optimization
    val deviceBrandName: String = "",
    val hasOemAutostartSettings: Boolean = false,
    val brandGuidanceSteps: List<String> = emptyList(),
    val isNotificationListenerAlive: Boolean = false,

    // ── Wizard state ──────────────────────────────────────────────────────────
    /** Current active wizard step (1..4) */
    val wizardStep: Int = 1,

    // Step 1 – Token verification
    val isVerifyingToken: Boolean = false,
    val tokenVerified: Boolean = false,
    val verifiedAccount: MetaAccountInfo? = null,
    val tokenError: String? = null,

    // Step 2 – Phone numbers
    val isFetchingPhoneNumbers: Boolean = false,
    val fetchedPhoneNumbers: List<MetaPhoneNumber> = emptyList(),
    val selectedPhoneNumber: MetaPhoneNumber? = null,
    val phoneNumberError: String? = null,

    // Step 3 – Business account (auto-filled)
    val isFetchingBusinessId: Boolean = false,

    // Step 4 – Webhook token
    /** Whether to show the in-app WebView sheet */
    val showWebViewSheet: Boolean = false,

    /** Snackbar / banner message */
    val bannerMessage: String? = null,
    val bannerIsSuccess: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage,
    private val cloudApiRepository: WhatsAppCloudApiRepository,
    private val metaCredentialRepository: MetaCredentialRepository,
    private val deviceManager: DeviceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        checkNotificationAccess()
        checkBatteryOptimization()
        loadBrandInfo()
    }

    private fun loadBrandInfo() {
        _uiState.value = _uiState.value.copy(
            deviceBrandName = DeviceBrandHelper.getBrandDisplayName(),
            hasOemAutostartSettings = DeviceBrandHelper.hasSpecialAutostartSettings(),
            brandGuidanceSteps = DeviceBrandHelper.getBrandGuidanceSteps()
        )
    }

    fun openBrandSettings() {
        DeviceBrandHelper.openBrandSettings(context)
    }

    fun requestIgnoreBatteryOptimization() {
        BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
        checkBatteryOptimization()
    }

    fun recoverNotificationService() {
        val success = NotificationServiceWatchdog.recoverService(context)
        checkNotificationAccess()
        _uiState.value = _uiState.value.copy(
            bannerMessage = if (success) "Notification service rebind dispatched!" else "Rebind failed. Please check system settings.",
            bannerIsSuccess = success
        )
    }

    // ── Business Profile ──────────────────────────────────────────────────────

    fun updateBusinessProfile(name: String, number: String) {
        val phone = number.trim().replace(" ", "")
        val normalizedPhone = if (phone.startsWith("+")) phone else if (phone.startsWith("0")) "+94${phone.drop(1)}" else "+$phone"
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProfileSyncing = true)
            
            secureStorage.setBusinessName(name)
            secureStorage.setWhatsAppNumber(normalizedPhone)
            secureStorage.setProfileSyncPending(true)
            
            // Sync with Firestore
            deviceManager.syncProfile()
            
            _uiState.value = _uiState.value.copy(
                businessName = name,
                whatsappNumber = normalizedPhone,
                isProfileSyncing = false,
                bannerMessage = "Profile updated successfully!",
                bannerIsSuccess = true
            )
        }
    }

    // ── Existing functions ────────────────────────────────────────────────────

    fun checkNotificationAccess() {
        val isGranted = NotificationServiceWatchdog.isNotificationAccessGranted(context)
        val isAlive = NotificationServiceWatchdog.isConnected

        // Android 13+ (API 33) "Restricted Setting" detection:
        val isRestricted = !isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

        _uiState.value = _uiState.value.copy(
            isNotificationAccessGranted = isGranted,
            isNotificationAccessRestricted = isRestricted,
            isNotificationListenerAlive = isAlive
        )
    }

    fun checkBatteryOptimization() {
        val isIgnoring = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
        _uiState.value = _uiState.value.copy(isBatteryOptimizationDisabled = isIgnoring)
    }

    private fun loadSettings() {
        val creds = secureStorage.getMetaCredentials()
        val useCloud = secureStorage.useCloudApi()
        val hasToken = creds.accessToken.isNotBlank()

        _uiState.value = _uiState.value.copy(
            businessName = secureStorage.getBusinessName(),
            whatsappNumber = secureStorage.getWhatsAppNumber(),
            credentials = creds,
            useCloudApi = useCloud,
            tokenVerified = hasToken,
            wizardStep = if (hasToken) 4 else 1
        )
    }

    fun setUseCloudApi(useCloud: Boolean) {
        secureStorage.setUseCloudApi(useCloud)
        _uiState.value = _uiState.value.copy(useCloudApi = useCloud)
    }

    fun saveCredentials(credentials: MetaCredentials) {
        if (!TokenValidator.isValidPhoneNumberId(credentials.phoneNumberId)) {
            _uiState.value = _uiState.value.copy(validationErrorMessage = "Invalid Phone Number ID format")
            return
        }
        if (!TokenValidator.isValidAccessToken(credentials.accessToken)) {
            _uiState.value = _uiState.value.copy(validationErrorMessage = "Invalid Meta Access Token format")
            return
        }

        secureStorage.saveMetaCredentials(credentials)
        _uiState.value = _uiState.value.copy(
            credentials = credentials,
            validationErrorMessage = null,
            bannerMessage = "Credentials saved successfully!",
            bannerIsSuccess = true
        )
    }

    fun testApiConnection(credentials: MetaCredentials) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingConnection = true, testResultMessage = null)
            when (val result = cloudApiRepository.testConnection(credentials)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isTestingConnection = false,
                        testResultMessage = "Meta WhatsApp Cloud API connection verified successfully!",
                        isTestSuccess = true
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isTestingConnection = false,
                        testResultMessage = "Connection failed: ${result.message}",
                        isTestSuccess = false
                    )
                }
                else -> {}
            }
        }
    }

    // ── Wizard functions ──────────────────────────────────────────────────────

    fun verifyToken(token: String) {
        if (token.isBlank()) {
            _uiState.value = _uiState.value.copy(tokenError = "Access token cannot be empty")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isVerifyingToken = true,
                tokenError = null,
                tokenVerified = false,
                verifiedAccount = null
            )
            when (val result = metaCredentialRepository.verifyToken(token)) {
                is Result.Success -> {
                    val businessResult = metaCredentialRepository.fetchBusinessAccountId(token)
                    val businessId = (businessResult as? Result.Success)?.data ?: ""

                    val updatedCreds = _uiState.value.credentials.copy(
                        accessToken = token,
                        businessAccountId = businessId
                    )
                    _uiState.value = _uiState.value.copy(
                        isVerifyingToken = false,
                        tokenVerified = true,
                        verifiedAccount = result.data,
                        credentials = updatedCreds,
                        tokenError = null,
                        wizardStep = 2
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isVerifyingToken = false,
                        tokenVerified = false,
                        tokenError = result.message ?: "Token verification failed"
                    )
                }
                else -> {}
            }
        }
    }

    fun fetchPhoneNumbers() {
        val token = _uiState.value.credentials.accessToken
        if (token.isBlank()) {
            _uiState.value = _uiState.value.copy(phoneNumberError = "Please verify your access token first")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isFetchingPhoneNumbers = true,
                phoneNumberError = null,
                fetchedPhoneNumbers = emptyList()
            )
            when (val result = metaCredentialRepository.fetchPhoneNumbers(token)) {
                is Result.Success -> {
                    val phones = result.data
                    val autoSelected = if (phones.size == 1) phones.first() else null
                    val updatedCreds = if (autoSelected != null) {
                        _uiState.value.credentials.copy(phoneNumberId = autoSelected.phoneNumberId)
                    } else {
                        _uiState.value.credentials
                    }
                    _uiState.value = _uiState.value.copy(
                        isFetchingPhoneNumbers = false,
                        fetchedPhoneNumbers = phones,
                        selectedPhoneNumber = autoSelected,
                        credentials = updatedCreds,
                        phoneNumberError = null,
                        wizardStep = if (autoSelected != null) 3 else 2
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isFetchingPhoneNumbers = false,
                        phoneNumberError = result.message ?: "Failed to fetch phone numbers"
                    )
                }
                else -> {}
            }
        }
    }

    fun selectPhoneNumber(phone: MetaPhoneNumber) {
        val updatedCreds = _uiState.value.credentials.copy(phoneNumberId = phone.phoneNumberId)
        _uiState.value = _uiState.value.copy(
            selectedPhoneNumber = phone,
            credentials = updatedCreds,
            wizardStep = 3
        )
    }

    fun updateBusinessAccountId(id: String) {
        val updatedCreds = _uiState.value.credentials.copy(businessAccountId = id)
        _uiState.value = _uiState.value.copy(credentials = updatedCreds)
        if (id.isNotBlank()) {
            _uiState.value = _uiState.value.copy(wizardStep = maxOf(_uiState.value.wizardStep, 4))
        }
    }

    fun generateWebhookToken() {
        val random = "orderflow_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val updatedCreds = _uiState.value.credentials.copy(webhookVerifyToken = random)
        _uiState.value = _uiState.value.copy(credentials = updatedCreds)
    }

    fun updateWebhookToken(token: String) {
        val updatedCreds = _uiState.value.credentials.copy(webhookVerifyToken = token)
        _uiState.value = _uiState.value.copy(credentials = updatedCreds)
    }

    fun showWebViewSheet() {
        _uiState.value = _uiState.value.copy(showWebViewSheet = true)
    }

    fun hideWebViewSheet() {
        _uiState.value = _uiState.value.copy(showWebViewSheet = false)
    }

    fun onTokenCopiedFromWebView(token: String) {
        _uiState.value = _uiState.value.copy(
            showWebViewSheet = false,
            credentials = _uiState.value.credentials.copy(accessToken = token)
        )
    }

    fun goToStep(step: Int) {
        if (step in 1..4) {
            _uiState.value = _uiState.value.copy(wizardStep = step)
        }
    }

    fun clearBanner() {
        _uiState.value = _uiState.value.copy(bannerMessage = null, testResultMessage = null)
    }

    fun saveAndTest() {
        val creds = _uiState.value.credentials
        saveCredentials(creds)
        testApiConnection(creds)
    }
}
