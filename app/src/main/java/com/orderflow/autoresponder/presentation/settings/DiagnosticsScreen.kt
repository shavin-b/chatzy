package com.orderflow.autoresponder.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orderflow.autoresponder.presentation.components.AppTopBar
import com.orderflow.autoresponder.presentation.theme.BrandDarkBackground
import com.orderflow.autoresponder.presentation.theme.BrandGreen
import com.orderflow.autoresponder.presentation.theme.BrandTextPrimary
import com.orderflow.autoresponder.presentation.theme.BrandTextSecondary
import com.orderflow.autoresponder.presentation.theme.StatusFailed
import com.orderflow.autoresponder.presentation.theme.StatusSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    Scaffold(
        topBar = {
            AppTopBar(
                title = "System Diagnostics",
                navigationIcon = Icons.Default.ArrowBack,
                onNavigationClick = onNavigateBack,
                actionIcon = Icons.Default.Refresh,
                onActionClick = { viewModel.refreshDiagnostics() }
            )
        },
        containerColor = BrandDarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            DiagnosticSectionTitle("Device Identity")
            DiagnosticItem(
                label = "Device ID",
                status = state.deviceId
            )
            DiagnosticItem(
                label = "Remote Lock",
                status = state.lockStatus,
                isSuccess = state.lockStatus == "UNLOCKED"
            )
            DiagnosticItem(
                label = "Subscription",
                status = state.subscriptionStatus,
                isSuccess = state.subscriptionStatus == "ACTIVE"
            )
            Spacer(modifier = Modifier.height(16.dp))

            DiagnosticSectionTitle("Background Services")
            DiagnosticItem(
                label = "Notification Listener",
                status = if (state.isListenerRunning) "Running" else "Stopped",
                isSuccess = state.isListenerRunning
            )
            DiagnosticItem(
                label = "Notification Access",
                status = if (state.notificationAccessGranted) "Granted" else "Denied",
                isSuccess = state.notificationAccessGranted
            )
            DiagnosticItem(
                label = "Battery Optimization",
                status = if (state.batteryOptimizationDisabled) "Disabled (Safe)" else "Enabled (Risky)",
                isSuccess = state.batteryOptimizationDisabled
            )

            Spacer(modifier = Modifier.height(24.dp))
            DiagnosticSectionTitle("Auto-Responder Stats")
            DiagnosticItem(label = "Messages Detected", status = "${state.messagesDetected}")
            DiagnosticItem(label = "Duplicates Ignored", status = "${state.messagesIgnoredDuplicate}")
            DiagnosticItem(label = "Rules Matched", status = "${state.rulesMatched}")
            DiagnosticItem(label = "Reply Attempts", status = "${state.replyAttempts}")
            DiagnosticItem(label = "Reply Successes", status = "${state.replySuccesses}", isSuccess = state.replySuccesses > 0)
            DiagnosticItem(label = "Reply Failures", status = "${state.replyFailures}", isSuccess = if (state.replyFailures > 0) false else null)
            
            val lastProcessed = if (state.lastProcessedTime > 0) dateFormat.format(Date(state.lastProcessedTime)) else "Never"
            DiagnosticItem(label = "Last Activity", status = lastProcessed)

            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BrandTextSecondary.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = BrandTextSecondary)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Technical Info",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrandTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Chatzy uses Android NotificationListenerService to intercept and reply to WhatsApp messages. If auto-replies stop working, please toggle Notification Access OFF and ON again in system settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandTextSecondary,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.resetLockState() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Device Lock (Debug)")
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = BrandTextPrimary,
        fontSize = 18.sp
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun DiagnosticItem(
    label: String,
    status: String,
    isSuccess: Boolean? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = BrandTextSecondary, style = MaterialTheme.typography.bodyLarge)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSuccess != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isSuccess) StatusSuccess else StatusFailed)
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            Text(
                text = status,
                color = if (isSuccess == true) StatusSuccess else if (isSuccess == false) StatusFailed else BrandTextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
    HorizontalDivider(color = BrandTextSecondary.copy(alpha = 0.1f))
}
