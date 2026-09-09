package com.orderflow.autoresponder.presentation.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReplyAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orderflow.autoresponder.presentation.components.AppTopBar
import com.orderflow.autoresponder.presentation.theme.BrandAccent
import com.orderflow.autoresponder.presentation.theme.BrandDarkBackground
import com.orderflow.autoresponder.presentation.theme.BrandGreen
import com.orderflow.autoresponder.presentation.theme.BrandTextPrimary
import com.orderflow.autoresponder.presentation.theme.BrandTextSecondary
import com.orderflow.autoresponder.presentation.theme.StatusFailed
import com.orderflow.autoresponder.presentation.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.checkNotificationAccess()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Settings",
                navigationIcon = Icons.Default.ArrowBack,
                onNavigationClick = onNavigateBack
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
            Text(
                text = "Background Reliability & Setup",
                style = MaterialTheme.typography.titleLarge,
                color = BrandTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Designed to run reliably in the background, subject to Android and device power-management restrictions. To improve automatic replies while your phone is idle, keep Notification Access enabled and allow OrderFlow to use background battery resources as recommended by your device.",
                style = MaterialTheme.typography.bodySmall,
                color = BrandTextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            PermissionRow(
                title = "Notification Access",
                subtitle = "Required for automatic replies",
                statusText = if (state.isNotificationAccessGranted) "Granted" else "Action Required",
                statusColor = if (state.isNotificationAccessGranted) BrandGreen else StatusFailed,
                buttonText = if (!state.isNotificationAccessGranted) "Grant" else null,
                icon = Icons.Default.NotificationsActive,
                onActionClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )

            // Android 13+ Restricted Setting hint card
            AnimatedVisibility(visible = state.isNotificationAccessRestricted) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(androidx.compose.ui.graphics.Color(0xFFFF8F00).copy(alpha = 0.12f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color(0xFFFF8F00),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Android 13+ — Restricted Setting",
                                style = MaterialTheme.typography.labelMedium,
                                color = androidx.compose.ui.graphics.Color(0xFFFF8F00)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "If tapping \"Grant\" opens the page but the toggle appears greyed-out or does nothing, tap the ⋮ menu (top-right) and select \"Allow Restricted Settings\", then try again.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandTextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            PermissionRow(
                title = "Battery Optimization",
                subtitle = "Recommended for better reliability while idle",
                statusText = if (state.isBatteryOptimizationDisabled) "Unrestricted" else "Optimized (Recommended to allow)",
                statusColor = if (state.isBatteryOptimizationDisabled) BrandGreen else androidx.compose.ui.graphics.Color(0xFFFF8F00),
                buttonText = if (!state.isBatteryOptimizationDisabled) "Allow Unrestricted" else null,
                icon = Icons.Default.BatteryChargingFull,
                onActionClick = {
                    viewModel.requestIgnoreBatteryOptimization()
                }
            )

            // OEM Brand Background Protection Card
            if (state.hasOemAutostartSettings) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandTextSecondary.copy(alpha = 0.08f))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = BrandAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${state.deviceBrandName} — Background Protection",
                            style = MaterialTheme.typography.titleSmall,
                            color = BrandTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    state.brandGuidanceSteps.forEachIndexed { idx, step ->
                        Text(
                            text = "${idx + 1}. $step",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandTextSecondary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.openBrandSettings() },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Configure Background Settings", color = BrandDarkBackground)
                    }
                }
            }

            // Watchdog Service Self-Healing tool
            if (state.isNotificationAccessGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.recoverNotificationService() },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Repair Service Connection", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reply Method Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandTextSecondary.copy(alpha = 0.1f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BrandAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.useCloudApi) Icons.Default.Cloud else Icons.Default.ReplyAll,
                        contentDescription = null,
                        tint = BrandAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reply Method",
                        color = BrandTextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (state.useCloudApi) "Cloud API (Official)" else "Local (Direct Reply)",
                        color = BrandTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = state.useCloudApi,
                    onCheckedChange = { viewModel.setUseCloudApi(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BrandGreen,
                        checkedTrackColor = BrandGreen.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = BrandTextSecondary.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Business Profile",
                style = MaterialTheme.typography.titleLarge,
                color = BrandTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your business name and WhatsApp number shown on the dashboard.",
                style = MaterialTheme.typography.bodySmall,
                color = BrandTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            var tempName by remember(state.businessName) { mutableStateOf(state.businessName) }
            var tempNumber by remember(state.whatsappNumber) { mutableStateOf(state.whatsappNumber) }

            OutlinedTextField(
                value = tempName,
                onValueChange = { tempName = it },
                label = { Text("Business Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = customTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = tempNumber,
                onValueChange = { tempNumber = it },
                label = { Text("WhatsApp Business Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = customTextFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.updateBusinessProfile(tempName, tempNumber) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                enabled = !state.isProfileSyncing && (tempName != state.businessName || tempNumber != state.whatsappNumber)
            ) {
                if (state.isProfileSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Save Profile Changes")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = BrandTextSecondary.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "WhatsApp Business Meta API Setup",
                style = MaterialTheme.typography.titleLarge,
                color = BrandTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Configure your Meta developer account to send verified WhatsApp messages.",
                style = MaterialTheme.typography.bodySmall,
                color = BrandTextSecondary
            )

            // Result / Error Banner
            val bannerText = state.testResultMessage ?: state.validationErrorMessage ?: state.bannerMessage
            val isBannerSuccess = state.isTestSuccess || (state.bannerMessage != null && state.bannerIsSuccess)
            AnimatedVisibility(
                visible = bannerText != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (bannerText != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isBannerSuccess) StatusSuccess.copy(alpha = 0.15f)
                                else StatusFailed.copy(alpha = 0.15f)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isBannerSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (isBannerSuccess) StatusSuccess else StatusFailed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = bannerText,
                            color = if (isBannerSuccess) StatusSuccess else StatusFailed,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step-by-step Meta Credentials Wizard
            MetaCredentialsWizard(
                state = state,
                onVerifyToken = { token ->
                    viewModel.verifyToken(token)
                },
                onFetchPhoneNumbers = {
                    viewModel.fetchPhoneNumbers()
                },
                onSelectPhoneNumber = { phone ->
                    viewModel.selectPhoneNumber(phone)
                },
                onManualPhoneNumberChange = { id ->
                    val updated = state.credentials.copy(phoneNumberId = id)
                    viewModel.saveCredentials(updated)
                },
                onBusinessIdChange = { id ->
                    viewModel.updateBusinessAccountId(id)
                },
                onWebhookTokenChange = { token ->
                    viewModel.updateWebhookToken(token)
                },
                onGenerateWebhookToken = {
                    viewModel.generateWebhookToken()
                },
                onOpenWebView = {
                    viewModel.showWebViewSheet()
                },
                onSaveAndTest = {
                    viewModel.saveAndTest()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal In-App WebView Bottom Sheet for Meta Graph API Explorer
    if (state.showWebViewSheet) {
        MetaTokenWebViewSheet(
            sheetState = sheetState,
            onTokenConfirmed = { token ->
                viewModel.onTokenCopiedFromWebView(token)
                viewModel.verifyToken(token)
            },
            onDismiss = {
                viewModel.hideWebViewSheet()
            }
        )
    }
}

@Composable
fun PermissionRow(
    title: String,
    subtitle: String,
    statusText: String,
    statusColor: Color,
    buttonText: String? = null,
    icon: ImageVector = Icons.Default.NotificationsActive,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BrandTextSecondary.copy(alpha = 0.1f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(statusColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.size(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = BrandTextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                color = BrandTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = statusText,
                color = statusColor,
                style = MaterialTheme.typography.labelSmall
            )
        }

        if (buttonText != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(buttonText, fontSize = 12.sp, color = BrandDarkBackground)
            }
        }
    }
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandAccent,
    unfocusedBorderColor = BrandTextSecondary.copy(alpha = 0.5f),
    focusedLabelColor = BrandAccent,
    unfocusedLabelColor = BrandTextSecondary,
    focusedTextColor = BrandTextPrimary,
    unfocusedTextColor = BrandTextPrimary,
    cursorColor = BrandAccent
)
