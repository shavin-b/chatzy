package com.orderflow.autoresponder.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orderflow.autoresponder.core.security.SecureStorage
import com.orderflow.autoresponder.device.auth.DeviceEnrollment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceEnrollmentViewModel @Inject constructor(
    private val enrollment: DeviceEnrollment,
    private val storage: SecureStorage
) : ViewModel() {
    val enabled get() = enrollment.enabled
    val status = enrollment.status
    val message = MutableStateFlow("")
    val busy = MutableStateFlow(false)
    fun act(request: Boolean) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                val deviceId = storage.getDeviceId()
                check(!deviceId.isNullOrBlank()) { "Wait for device identity to initialize." }
                if (request) enrollment.request(deviceId) else enrollment.ensureAuthorized(deviceId)
                message.value = ""
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { message.value = "Approval is not available yet. Check your connection and ask your Super Admin to verify the pairing ID." }
            finally { busy.value = false }
        }
    }
}

@Composable
fun DeviceEnrollmentPanel(viewModel: DeviceEnrollmentViewModel = hiltViewModel()) {
    if (!viewModel.enabled) return
    val status by viewModel.status.collectAsState()
    val message by viewModel.message.collectAsState()
    val busy by viewModel.busy.collectAsState()
    Column {
        Text("Chatzy device approval", style = MaterialTheme.typography.titleLarge)
        Text("Your Super Admin must compare this pairing ID with this device before approving it.")
        SelectionContainer { Text(status) }
        Button(onClick = { viewModel.act(true) }, enabled = !busy) { Text("Request approval") }
        Button(onClick = { viewModel.act(false) }, enabled = !busy) { Text("Check approval") }
        if (message.isNotBlank()) Text(message)
        Spacer(Modifier.height(24.dp))
    }
}
