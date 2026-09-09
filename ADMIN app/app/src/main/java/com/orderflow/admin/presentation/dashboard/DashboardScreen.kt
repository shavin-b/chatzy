package com.orderflow.admin.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orderflow.admin.core.designsystem.components.BarChart
import com.orderflow.admin.core.designsystem.components.BarChartData
import com.orderflow.admin.core.designsystem.components.GlassCard
import com.orderflow.admin.core.designsystem.components.PieChart
import com.orderflow.admin.core.designsystem.components.PieChartData
import com.orderflow.admin.core.designsystem.components.StatusBadge
import com.orderflow.admin.core.designsystem.theme.AccentBlue
import com.orderflow.admin.core.designsystem.theme.AccentIndigo
import com.orderflow.admin.core.designsystem.theme.BadgeActive
import com.orderflow.admin.core.designsystem.theme.BadgeExpired
import com.orderflow.admin.core.designsystem.theme.BadgeExpiring
import com.orderflow.admin.core.designsystem.theme.BadgeOffline
import com.orderflow.admin.domain.model.Device

@Composable
fun DashboardScreen(
    onNavigateToDeviceDetails: (String) -> Unit,
    onNavigateToDevicesList: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNotifications,
                containerColor = AccentIndigo,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Notifications, contentDescription = "Send Announcement")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Header & Greeting Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome back,",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = uiState.adminUser?.name ?: "Super Admin",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier
                            .size(48.dp)
                            .background(AccentIndigo.copy(alpha = 0.2f), shape = CircleShape)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = AccentBlue)
                    }
                }
            }

            // Top Stat Cards (2x3 Grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Total Devices",
                            value = uiState.stats.totalDevices.toString(),
                            icon = Icons.Default.Devices,
                            color = AccentBlue,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Online Devices",
                            value = uiState.stats.onlineDevices.toString(),
                            icon = Icons.Default.SignalCellularAlt,
                            color = BadgeActive,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Offline Devices",
                            value = uiState.stats.offlineDevices.toString(),
                            icon = Icons.Default.Devices,
                            color = BadgeOffline,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Expiring Soon",
                            value = uiState.stats.expiringSoonDevices.toString(),
                            icon = Icons.Default.Warning,
                            color = BadgeExpiring,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Expired",
                            value = uiState.stats.expiredDevices.toString(),
                            icon = Icons.Default.Warning,
                            color = BadgeExpired,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Active Today",
                            value = uiState.stats.activeTodayDevices.toString(),
                            icon = Icons.Default.Refresh,
                            color = AccentIndigo,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Charts Section
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Subscription Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PieChart(
                        data = listOf(
                            PieChartData("Active", (uiState.stats.totalDevices - uiState.stats.expiredDevices - uiState.stats.expiringSoonDevices).coerceAtLeast(0).toFloat(), BadgeActive),
                            PieChartData("Expiring", uiState.stats.expiringSoonDevices.toFloat(), BadgeExpiring),
                            PieChartData("Expired", uiState.stats.expiredDevices.toFloat(), BadgeExpired)
                        )
                    )
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Weekly Client Installations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BarChart(
                        data = listOf(
                            BarChartData("Mon", 12f),
                            BarChartData("Tue", 18f),
                            BarChartData("Wed", 25f),
                            BarChartData("Thu", 20f),
                            BarChartData("Fri", 32f),
                            BarChartData("Sat", 28f),
                            BarChartData("Sun", 40f)
                        )
                    )
                }
            }

            // Recent Devices Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Client Devices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onNavigateToDevicesList) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "See All")
                    }
                }
            }

            // Recent Devices List
            items(uiState.recentDevices) { device ->
                DashboardDeviceCard(
                    device = device,
                    onClick = { onNavigateToDeviceDetails(device.deviceId) }
                )
            }

            item { Spacer(modifier = Modifier.height(64.dp)) }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun DashboardDeviceCard(
    device: Device,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.businessName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${device.phoneModel} • ${device.phoneNumber}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Subscription: ${device.daysRemaining} days left",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentBlue
                )
            }
            StatusBadge(status = device.status)
        }
    }
}
