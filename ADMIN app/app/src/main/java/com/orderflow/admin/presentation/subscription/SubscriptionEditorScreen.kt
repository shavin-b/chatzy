package com.orderflow.admin.presentation.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orderflow.admin.core.common.DateUtils
import com.orderflow.admin.core.designsystem.components.AdminTopAppBar
import com.orderflow.admin.core.designsystem.components.GlassCard
import com.orderflow.admin.core.designsystem.theme.AccentBlue
import com.orderflow.admin.core.designsystem.theme.AccentIndigo
import com.orderflow.admin.core.designsystem.theme.BadgeActive
import com.orderflow.admin.core.designsystem.theme.VibrantAmber
import com.orderflow.admin.presentation.devicedetails.QuickAddButton

@Composable
fun SubscriptionEditorScreen(
    onBackClick: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actions = listOf("Extend", "Reduce", "Pause", "Resume", "Deactivate", "Reactivate", "Lifetime")

    Scaffold(
        topBar = {
            AdminTopAppBar(
                title = "Subscription Editor & Sync",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        if (uiState.device == null) {
            CircularProgressIndicator()
        } else {
            val device = uiState.device!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = device.businessName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Device ID: ${device.deviceId}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Current Expiry: ${DateUtils.formatDateOnly(device.subscriptionEnd)} (${device.daysRemaining} days remaining)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentBlue
                    )
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Select Action Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        actions.forEach { action ->
                            val selected = uiState.selectedAction == action
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.onActionSelected(action) },
                                label = { Text(action) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.selectedAction in listOf("Extend", "Reduce")) {
                        Text(
                            text = "Preset Extension Days",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickAddButton("7 Days", modifier = Modifier.weight(1f)) { viewModel.onCustomDaysChanged(7) }
                            QuickAddButton("30 Days", modifier = Modifier.weight(1f)) { viewModel.onCustomDaysChanged(30) }
                            QuickAddButton("90 Days", modifier = Modifier.weight(1f)) { viewModel.onCustomDaysChanged(90) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickAddButton("180 Days", modifier = Modifier.weight(1f)) { viewModel.onCustomDaysChanged(180) }
                            QuickAddButton("365 Days", modifier = Modifier.weight(1f)) { viewModel.onCustomDaysChanged(365) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = uiState.customDays.toString(),
                            onValueChange = { viewModel.onCustomDaysChanged(it.toIntOrNull() ?: 0) },
                            label = { Text("Custom Days") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.applySubscriptionUpdate() },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "COMMIT & INSTANT SYNC TO CLIENT",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccessDialog() },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = BadgeActive,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Subscription Updated & Synced!") },
            text = {
                Text("The subscription change has been recorded in Firestore and synced instantly to the client app.")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.dismissSuccessDialog()
                    onBackClick()
                }) {
                    Text("Done")
                }
            }
        )
    }
}
