package com.orderflow.admin.presentation.devicedetails

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.orderflow.admin.core.designsystem.components.StatusBadge
import com.orderflow.admin.core.designsystem.theme.AccentBlue
import com.orderflow.admin.core.designsystem.theme.AccentIndigo
import com.orderflow.admin.core.designsystem.theme.BadgeActive
import com.orderflow.admin.core.designsystem.theme.BadgeOffline
import com.orderflow.admin.core.designsystem.theme.VibrantAmber

@Composable
fun DeviceDetailsScreen(
    onBackClick: () -> Unit,
    onOpenSubscriptionEditor: (String) -> Unit,
    viewModel: DeviceDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            AdminTopAppBar(
                title = "Device Details & Control",
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading || uiState.device == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
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
                // Header Device Card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = device.businessName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Owner: ${device.userName}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        StatusBadge(status = device.status)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (device.isOnline) BadgeActive else BadgeOffline,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (device.isOnline) "Realtime Connected (Online)" else "Offline",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (device.isOnline) BadgeActive else BadgeOffline
                        )
                    }
                }

                // Subscription Management Card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Subscription Lifetime",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${device.daysRemaining} Days Remaining",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue
                            )
                        }
                        Button(
                            onClick = { onOpenSubscriptionEditor(device.deviceId) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Full Editor")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Quick Extensions:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickAddButton("+7 Days", modifier = Modifier.weight(1f)) { viewModel.quickExtendSubscription(7) }
                        QuickAddButton("+30 Days", modifier = Modifier.weight(1f)) { viewModel.quickExtendSubscription(30) }
                        QuickAddButton("+90 Days", modifier = Modifier.weight(1f)) { viewModel.quickExtendSubscription(90) }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickAddButton("+180 Days", modifier = Modifier.weight(1f)) { viewModel.quickExtendSubscription(180) }
                        QuickAddButton("+365 Days", modifier = Modifier.weight(1f)) { viewModel.quickExtendSubscription(365) }
                        QuickAddButton("Lifetime", color = VibrantAmber, modifier = Modifier.weight(1f)) { viewModel.setLifetimeSubscription() }
                    }
                }

                // Detailed Information Card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Technical Specification & Hardware",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow("Device ID", device.deviceId)
                    DetailRow("Phone Model", device.phoneModel)
                    DetailRow("Manufacturer", device.manufacturer)
                    DetailRow("Android OS", device.androidVersion)
                    DetailRow("App Version", device.appVersion)
                    DetailRow("Generated UUID (IMEI Alt)", device.generatedUuid)
                    DetailRow("Phone Number", device.phoneNumber)
                }

                // Timeline Dates Card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Registration & Sync History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow("Installation Date", DateUtils.formatDateOnly(device.installationDate))
                    DetailRow("Activation Date", DateUtils.formatDateOnly(device.activationDate))
                    DetailRow("Subscription Start", DateUtils.formatDateOnly(device.subscriptionStart))
                    DetailRow("Subscription End", DateUtils.formatDateOnly(device.subscriptionEnd))
                    DetailRow("Last Heartbeat Sync", DateUtils.formatTimestamp(device.lastSeen))
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun QuickAddButton(
    label: String,
    color: Color = AccentBlue,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
