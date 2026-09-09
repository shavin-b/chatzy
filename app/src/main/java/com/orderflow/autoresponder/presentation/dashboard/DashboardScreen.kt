package com.orderflow.autoresponder.presentation.dashboard

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orderflow.autoresponder.presentation.components.AppTopBar
import com.orderflow.autoresponder.presentation.theme.BrandAccent
import com.orderflow.autoresponder.presentation.theme.BrandCardDark
import com.orderflow.autoresponder.presentation.theme.BrandDarkBackground
import com.orderflow.autoresponder.presentation.theme.BrandGreen
import com.orderflow.autoresponder.presentation.theme.BrandTextPrimary
import com.orderflow.autoresponder.presentation.theme.BrandTextSecondary
import com.orderflow.autoresponder.presentation.theme.StatusSuccess

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToRules: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { AppTopBar(title = "OrderFlow Automation") },
        containerColor = BrandDarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Master Switch Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BrandCardDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Responder Status",
                            style = MaterialTheme.typography.titleLarge,
                            color = BrandTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (state.isAutoResponderEnabled) "ACTIVE - Listening & Replying" else "PAUSED - Replies standard messages",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.isAutoResponderEnabled) StatusSuccess else BrandTextSecondary
                        )
                    }

                    Switch(
                        checked = state.isAutoResponderEnabled,
                        onCheckedChange = { viewModel.toggleAutoResponder(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BrandTextPrimary,
                            checkedTrackColor = BrandGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notification Access Status Card
            if (!state.isNotificationAccessGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    onClick = onNavigateToSettings
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        Column {
                            Text(
                                text = "Notification Access Required",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Tap to enable auto-reply permission",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Live Statistics",
                style = MaterialTheme.typography.titleLarge,
                color = BrandTextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stat Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Replies Today",
                    value = "${state.todayRepliesCount}",
                    color = BrandAccent
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Active Rules",
                    value = "${state.activeRulesCount}/${state.totalRulesCount}",
                    color = BrandGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            StatCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Total Unique Customers",
                value = "${state.totalCustomersCount}",
                color = StatusSuccess
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Quick Management",
                style = MaterialTheme.typography.titleLarge,
                color = BrandTextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            QuickActionButton(
                title = "Auto-Reply Rules",
                subtitle = "Manage keywords, delay & replies",
                icon = Icons.Default.List,
                onClick = onNavigateToRules
            )

            Spacer(modifier = Modifier.height(10.dp))

            QuickActionButton(
                title = "Message Logs",
                subtitle = "View incoming & automated replies",
                icon = Icons.Default.Message,
                onClick = onNavigateToLogs
            )

            Spacer(modifier = Modifier.height(10.dp))

            QuickActionButton(
                title = "Customer Records",
                subtitle = "Track contacts & conversation count",
                icon = Icons.Default.People,
                onClick = onNavigateToCustomers
            )

            Spacer(modifier = Modifier.height(10.dp))

            QuickActionButton(
                title = "Diagnostics & Status",
                subtitle = "Check service & notification health",
                icon = Icons.Default.Settings,
                onClick = onNavigateToDiagnostics
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BrandCardDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = BrandTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 26.sp,
                style = MaterialTheme.typography.headlineLarge,
                color = color
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = BrandCardDark),
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandGreen.copy(alpha = 0.2f))
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = BrandGreen
                )
            }
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandTextPrimary,
                    fontSize = 16.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
