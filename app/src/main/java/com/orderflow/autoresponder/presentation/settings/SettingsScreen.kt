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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReplyAll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import com.orderflow.autoresponder.core.util.AppConstants
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
    onNavigateBack: () -> Unit,
    onNavigateToHelp: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showNotificationDisclosure by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showClearDataConfirmDialog by remember { mutableStateOf(false) }
    var showDisconnectConfirmDialog by remember { mutableStateOf(false) }

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
            OutlinedButton(onClick = onNavigateToHelp, modifier = Modifier.fillMaxWidth()) {
                Text("Chatzy Help")
            }
            Text("Offline answers and troubleshooting", color = BrandTextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            DeviceEnrollmentPanel()
            Text(
                text = "Background Reliability & Setup",
                style = MaterialTheme.typography.titleLarge,
                color = BrandTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Designed to run reliably in the background, subject to Android and device power-management restrictions. To improve automatic replies while your phone is idle, keep Notification Access enabled and allow Chatzy to use background battery resources as recommended by your device.",
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
                    showNotificationDisclosure = true
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
            HorizontalDivider(color = BrandTextSecondary.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "About ${AppConstants.PRODUCT_NAME}", style = MaterialTheme.typography.titleLarge, color = BrandTextPrimary)
            Text(text = AppConstants.TAGLINE, style = MaterialTheme.typography.bodyMedium, color = BrandAccent)
            Text(text = AppConstants.COMPANY_NAME, style = MaterialTheme.typography.bodySmall, color = BrandTextSecondary)
            val installedVersion = remember {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
            }
            Text(text = "Version $installedVersion", style = MaterialTheme.typography.bodySmall, color = BrandTextSecondary)
            Text(text = if (AppConstants.SUPPORT_EMAIL.isBlank()) "Support contact: pending business configuration" else "Support: ${AppConstants.SUPPORT_EMAIL}", style = MaterialTheme.typography.bodySmall, color = BrandTextSecondary)
            Text(text = "Independent software. Chatzy is not affiliated with or endorsed by WhatsApp or Meta.", style = MaterialTheme.typography.bodySmall, color = BrandTextSecondary)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Privacy & Data Safety",
                style = MaterialTheme.typography.titleLarge,
                color = BrandTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Chatzy processes WhatsApp Business notification content on your device in Local Mode. Device registration, business profile, subscription status and command acknowledgements are transmitted to Firebase. Optional Cloud API replies transmit reply content and recipient details to Meta.",
                style = MaterialTheme.typography.bodySmall,
                color = BrandTextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            val privacyPolicyPlaceholderUrl = AppConstants.PRIVACY_POLICY_URL

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showPrivacyPolicyDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PrivacyTip, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("In-App Policy", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = {
                        try {
                            if (privacyPolicyPlaceholderUrl.isNotBlank()) {
                                uriHandler.openUri(privacyPolicyPlaceholderUrl)
                            } else {
                                showPrivacyPolicyDialog = true
                            }
                        } catch (_: Exception) {
                            // URL fallback
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (privacyPolicyPlaceholderUrl.isBlank()) "Policy URL pending" else "Public Policy URL", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = BrandTextSecondary.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Data & Device Management",
                style = MaterialTheme.typography.titleLarge,
                color = BrandTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Clear local activity logs or reset local device registration.",
                style = MaterialTheme.typography.bodySmall,
                color = BrandTextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showClearDataConfirmDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusFailed)
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Logs & Cache", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = { showDisconnectConfirmDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Disconnect Device", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
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

    // ── Dialog 1: Notification Access Disclosure ────────────────────────────
    if (showNotificationDisclosure) {
        AlertDialog(
            onDismissRequest = { showNotificationDisclosure = false },
            title = {
                Text(
                    text = "Notification Access Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandTextPrimary
                )
            },
            text = {
                Text(
                    text = "Chatzy uses Notification Access to detect incoming WhatsApp Business notifications and process them according to your configured automatic reply rules.\n\n" +
                            "How Notification Access is Used:\n" +
                            "• Identifies incoming WhatsApp messages and sender information.\n" +
                            "• Matches message text against your configured rules.\n" +
                            "• Sends your configured response through WhatsApp's notification reply function (RemoteInput).\n\n" +
                            "This access is strictly required for the automatic reply feature. Your notification content is processed locally on your device and is never sold or shared.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationDisclosure = false
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                ) {
                    Text("I Understand - Enable Access", color = BrandGreen)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNotificationDisclosure = false }
                ) {
                    Text("Not Now", color = BrandTextSecondary)
                }
            },
            containerColor = BrandDarkBackground
        )
    }

    // ── Dialog 2: Privacy Policy ─────────────────────────────────────────────
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = {
                Text(
                    text = "Privacy & Data Safety Policy",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandTextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "1. Local Auto Responder Processing\n" +
                                "Chatzy reads WhatsApp Business notifications locally on your device to trigger auto-replies according to your configured rules. Replies are dispatched through Android's notification reply action. Notification content is processed on-device and is NOT collected, sold, or shared for advertising.\n\n" +
                                "2. Firebase Cloud Communications\n" +
                                "The app syncs device registration details, FCM tokens, and status heartbeats with Firebase solely for device status tracking and remote administration.\n\n" +
                                "3. Meta WhatsApp Cloud API Architecture\n" +
                                "Chatzy includes optional Meta WhatsApp Cloud API integration. When enabled, reply text and recipient identifiers are sent to Meta. Firebase Analytics may process app usage and device information. Clearing local data or disconnecting does not delete cloud records. Contact SHAVIN SOLUTIONS (PVT) LTD to request cloud deletion once support details are configured.\n\n" +
                                "4. Published Privacy Policy\n" +
                                "${AppConstants.PRIVACY_POLICY_URL.ifBlank { "Published privacy policy URL: pending configuration" }}\n" +
                                "For privacy questions, contact the company using the support details shown in About when available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandTextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("Close", color = BrandAccent)
                }
            },
            containerColor = BrandDarkBackground
        )
    }

    // ── Dialog 3: Clear Data Confirmation ────────────────────────────────────
    if (showClearDataConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirmDialog = false },
            title = {
                Text("Clear Local Logs & Cache?", style = MaterialTheme.typography.titleMedium, color = BrandTextPrimary)
            },
            text = {
                Text(
                    "This will permanently delete local message logs and notification deduplication history from this device. Your auto-reply rules will remain intact.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataConfirmDialog = false
                        viewModel.clearLocalDataAndLogs()
                    }
                ) {
                    Text("Clear Data", color = StatusFailed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirmDialog = false }) {
                    Text("Cancel", color = BrandTextSecondary)
                }
            },
            containerColor = BrandDarkBackground
        )
    }

    // ── Dialog 4: Disconnect Device Confirmation ─────────────────────────────
    if (showDisconnectConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirmDialog = false },
            title = {
                Text("Disconnect Device Registration?", style = MaterialTheme.typography.titleMedium, color = BrandTextPrimary)
            },
            text = {
                Text(
                    "This will reset your local device registration ID and trigger a clean registration sync with Firebase. Local auto-reply rules will not be deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisconnectConfirmDialog = false
                        viewModel.disconnectDevice()
                    }
                ) {
                    Text("Disconnect", color = StatusFailed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirmDialog = false }) {
                    Text("Cancel", color = BrandTextSecondary)
                }
            },
            containerColor = BrandDarkBackground
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
