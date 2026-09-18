package com.orderflow.autoresponder.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orderflow.autoresponder.data.repository.MetaPhoneNumber
import com.orderflow.autoresponder.domain.model.MetaCredentials
import com.orderflow.autoresponder.presentation.theme.BrandAccent
import com.orderflow.autoresponder.presentation.theme.BrandCardDark
import com.orderflow.autoresponder.presentation.theme.BrandGreen
import com.orderflow.autoresponder.presentation.theme.BrandSurfaceDark
import com.orderflow.autoresponder.presentation.theme.BrandTextPrimary
import com.orderflow.autoresponder.presentation.theme.BrandTextSecondary
import com.orderflow.autoresponder.presentation.theme.StatusFailed
import com.orderflow.autoresponder.presentation.theme.StatusSuccess
import com.orderflow.autoresponder.presentation.theme.StatusWarning

@Composable
fun MetaCredentialsWizard(
    state: SettingsUiState,
    onVerifyToken: (String) -> Unit,
    onFetchPhoneNumbers: () -> Unit,
    onSelectPhoneNumber: (MetaPhoneNumber) -> Unit,
    onManualPhoneNumberChange: (String) -> Unit,
    onBusinessIdChange: (String) -> Unit,
    onWebhookTokenChange: (String) -> Unit,
    onGenerateWebhookToken: () -> Unit,
    onOpenWebView: () -> Unit,
    onSaveAndTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tokenInput by remember(state.credentials.accessToken) {
        mutableStateOf(state.credentials.accessToken)
    }
    var phoneIdInput by remember(state.credentials.phoneNumberId) {
        mutableStateOf(state.credentials.phoneNumberId)
    }
    var businessIdInput by remember(state.credentials.businessAccountId) {
        mutableStateOf(state.credentials.businessAccountId)
    }
    var webhookTokenInput by remember(state.credentials.webhookVerifyToken) {
        mutableStateOf(state.credentials.webhookVerifyToken)
    }

    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- STEP 1: Permanent Access Token ---
        WizardStepCard(
            stepNumber = 1,
            title = "Permanent Access Token",
            subtitle = "Authenticate with Meta WhatsApp Cloud API",
            icon = Icons.Default.Key,
            isCompleted = state.tokenVerified || state.credentials.accessToken.isNotBlank(),
            isActive = true
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = {
                        tokenInput = it
                    },
                    label = { Text("Access Token (Bearer)") },
                    placeholder = { Text("EAAMUOZ...", color = BrandTextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = BrandTextPrimary
                    ),
                    colors = wizardTextFieldColors(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text ?: ""
                                if (clip.isNotBlank()) {
                                    tokenInput = clip.trim()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Paste",
                                tint = BrandAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )

                if (state.tokenError != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = StatusFailed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.tokenError,
                            color = StatusFailed,
                            fontSize = 12.sp
                        )
                    }
                }

                if (state.tokenVerified && state.verifiedAccount != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(StatusSuccess.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verified: ${state.verifiedAccount.accountName} (ID: ${state.verifiedAccount.userId})",
                            color = StatusSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenWebView,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Get Token ↗", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onVerifyToken(tokenInput.trim()) },
                        enabled = tokenInput.isNotBlank() && !state.isVerifyingToken,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isVerifyingToken) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = BrandTextPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Verify Token", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // --- STEP 2: Phone Number ID ---
        WizardStepCard(
            stepNumber = 2,
            title = "WhatsApp Phone Number ID",
            subtitle = "Select registered phone or enter ID manually",
            icon = Icons.Default.Phone,
            isCompleted = state.credentials.phoneNumberId.isNotBlank(),
            isActive = state.tokenVerified || state.credentials.accessToken.isNotBlank()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Auto-fetch from Meta API:",
                        fontSize = 12.sp,
                        color = BrandTextSecondary
                    )
                    Button(
                        onClick = onFetchPhoneNumbers,
                        enabled = !state.isFetchingPhoneNumbers,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isFetchingPhoneNumbers) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = BrandTextPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fetch Numbers", fontSize = 12.sp)
                        }
                    }
                }

                if (state.phoneNumberError != null) {
                    Text(
                        text = state.phoneNumberError,
                        color = StatusFailed,
                        fontSize = 12.sp
                    )
                }

                if (state.fetchedPhoneNumbers.isNotEmpty()) {
                    Text(
                        text = "Select your WhatsApp Business number:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandTextPrimary
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandSurfaceDark)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.fetchedPhoneNumbers.forEach { phone ->
                            val isSelected = phone.phoneNumberId == state.credentials.phoneNumberId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) BrandGreen.copy(alpha = 0.2f) else Color.Transparent
                                    )
                                    .clickable {
                                        onSelectPhoneNumber(phone)
                                        phoneIdInput = phone.phoneNumberId
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSelectPhoneNumber(phone)
                                        phoneIdInput = phone.phoneNumberId
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = BrandGreen,
                                        unselectedColor = BrandTextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${phone.displayPhoneNumber} (${phone.verifiedName})",
                                        color = BrandTextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "ID: ${phone.phoneNumberId} • ${if (phone.isVerified) "Verified" else "Unverified"}",
                                        color = if (phone.isVerified) StatusSuccess else StatusWarning,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = phoneIdInput,
                    onValueChange = {
                        phoneIdInput = it
                        onManualPhoneNumberChange(it)
                    },
                    label = { Text("Phone Number ID") },
                    placeholder = { Text("100293848192038", color = BrandTextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = wizardTextFieldColors(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = BrandTextPrimary
                    )
                )
            }
        }

        // --- STEP 3: Business Account ID ---
        WizardStepCard(
            stepNumber = 3,
            title = "WhatsApp Business Account ID (WABA)",
            subtitle = "Found in Meta Business Manager or fetched automatically",
            icon = Icons.Default.Storefront,
            isCompleted = state.credentials.businessAccountId.isNotBlank(),
            isActive = true
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = businessIdInput,
                    onValueChange = {
                        businessIdInput = it
                        onBusinessIdChange(it)
                    },
                    label = { Text("Business Account ID (WABA ID)") },
                    placeholder = { Text("109823749182374", color = BrandTextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = wizardTextFieldColors(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = BrandTextPrimary
                    )
                )
            }
        }

        // --- STEP 4: Webhook Verification Token ---
        WizardStepCard(
            stepNumber = 4,
            title = "Webhook Verification Token",
            subtitle = "Used to authenticate Meta webhook payloads",
            icon = Icons.Default.Security,
            isCompleted = state.credentials.webhookVerifyToken.isNotBlank(),
            isActive = true
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = webhookTokenInput,
                    onValueChange = {
                        webhookTokenInput = it
                        onWebhookTokenChange(it)
                    },
                    label = { Text("Webhook Verify Token") },
                    placeholder = { Text("orderflow_secure_token_...", color = BrandTextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = wizardTextFieldColors(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = BrandTextPrimary
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (webhookTokenInput.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(webhookTokenInput))
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = BrandAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )

                OutlinedButton(
                    onClick = {
                        onGenerateWebhookToken()
                        webhookTokenInput = state.credentials.webhookVerifyToken
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Random Secure Token", fontSize = 12.sp)
                }
            }
        }

        // --- Save & Test Combined CTA ---
        Button(
            onClick = onSaveAndTest,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            shape = RoundedCornerShape(12.dp),
            enabled = !state.isTestingConnection
        ) {
            if (state.isTestingConnection) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = BrandTextPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Testing Meta Connection...", fontSize = 15.sp)
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Test Connection", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WizardStepCard(
    stepNumber: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    isCompleted: Boolean,
    isActive: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BrandCardDark)
            .border(
                width = 1.dp,
                color = if (isCompleted) BrandGreen.copy(alpha = 0.6f) else BrandTextSecondary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                            .clip(CircleShape)
                        .background(
                            if (isCompleted) BrandGreen.copy(alpha = 0.2f) else BrandAccent.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "$stepNumber",
                            color = BrandAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = BrandTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = subtitle,
                        color = BrandTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
private fun wizardTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandAccent,
    unfocusedBorderColor = BrandTextSecondary.copy(alpha = 0.35f),
    focusedLabelColor = BrandAccent,
    unfocusedLabelColor = BrandTextSecondary,
    focusedTextColor = BrandTextPrimary,
    unfocusedTextColor = BrandTextPrimary,
    cursorColor = BrandAccent
)
