package com.orderflow.autoresponder.presentation.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orderflow.autoresponder.R
import com.orderflow.autoresponder.domain.model.AutoReplyRule
import com.orderflow.autoresponder.presentation.rules.RulesViewModel

private val DashboardNavy = Color(0xFF101F46)
private val DashboardBlue = Color(0xFF2859DD)
private val DashboardMint = Color(0xFF69E4B7)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToRules: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    rulesViewModel: RulesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val rulesState by rulesViewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(Unit) { viewModel.loadDashboardData() }

    // This palette is local to Home; other screens retain their existing theme.
    val colors = if (isSystemInDarkTheme()) darkColorScheme(
        primary = Color(0xFFABC1FF), background = Color(0xFF0C1425),
        surface = Color(0xFF18243B), onBackground = Color(0xFFF0F4FF),
        onSurface = Color(0xFFF0F4FF), onSurfaceVariant = Color(0xFFBAC5DC)
    ) else lightColorScheme(
        primary = DashboardBlue, background = Color(0xFFF6F7FB),
        surface = Color.White, onBackground = DashboardNavy,
        onSurface = DashboardNavy, onSurfaceVariant = Color(0xFF596780)
    )
    MaterialTheme(colorScheme = colors) {
        Scaffold(containerColor = colors.background) { insets ->
            Column(
                Modifier.fillMaxSize().padding(insets)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(56.dp))
                    Text(stringResource(R.string.app_name), Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        uriHandler.openUri("https://gemini.google.com/share/d/12KS_GTgc4qC2B7qaxQkW5W5Jr4RIPFPr?usp=sharing")
                    }) { Icon(Icons.AutoMirrored.Filled.HelpOutline, "Help", tint = colors.onSurfaceVariant) }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = colors.onSurface)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(com.orderflow.autoresponder.core.util.AppConstants.TAGLINE,
                        style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                    Card(colors = CardDefaults.cardColors(containerColor = DashboardNavy),
                        shape = RoundedCornerShape(28.dp)) {
                        Row(Modifier.fillMaxWidth().padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Auto-reply", color = Color.White,
                                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(when {
                                    !state.isAutoResponderEnabled -> "Paused"
                                    !state.isNotificationAccessGranted -> "Enabled · Permission required"
                                    else -> "Active · Listening & replying"
                                }, color = if (state.isAutoResponderEnabled) DashboardMint else Color(0xFFD0D8EB),
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(checked = state.isAutoResponderEnabled,
                                onCheckedChange = { viewModel.toggleAutoResponder(it) },
                                modifier = Modifier.semantics { contentDescription = "Auto-reply" },
                                colors = dashboardSwitchColors())
                        }
                    }
                    if (!state.isNotificationAccessGranted) {
                        Card(onClick = onNavigateToSettings,
                            colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                            shape = RoundedCornerShape(20.dp)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Settings, null, tint = colors.onErrorContainer)
                                Column {
                                    Text("Notification Access Required", color = colors.onErrorContainer,
                                        style = MaterialTheme.typography.titleMedium)
                                    Text("Tap to enable auto-reply permission", color = colors.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Quick overview")
                    val fontScale = LocalDensity.current.fontScale
                    BoxWithConstraints {
                        if (maxWidth >= 320.dp && fontScale <= 1.3f) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.height(IntrinsicSize.Min)) {
                                StatCard("Active rules", state.activeRulesCount.toString(),
                                    Icons.Default.List, Modifier.weight(1f).fillMaxHeight())
                                StatCard("Replies today", state.todayRepliesCount.toString(),
                                    Icons.Default.Message, Modifier.weight(1f).fillMaxHeight())
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatCard("Active rules", state.activeRulesCount.toString(), Icons.Default.List)
                                StatCard("Replies today", state.todayRepliesCount.toString(), Icons.Default.Message)
                            }
                        }
                    }
                    ManagementRow("Customer Records", "${state.totalCustomersCount} total unique customers",
                        Icons.Default.People, onNavigateToCustomers)
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Reply rules")
                    ManagementRow("Auto-Reply Rules", "${state.activeRulesCount} active · ${state.totalRulesCount} total",
                        Icons.Default.List, onNavigateToRules)
                    when {
                        rulesState.isLoading -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.primary)
                        rulesState.rules.isEmpty() -> Text(
                            "No auto-reply rules created yet.\nOpen Auto-Reply Rules and tap '+' to create your first rule!",
                            style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp))
                        else -> rulesState.rules.take(3).forEach { rule ->
                            key(rule.id) {
                                DashboardRuleRow(rule) { rulesViewModel.toggleRule(rule.id, it) }
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Quick management")
                    ManagementRow("Message Logs", "View incoming & automated replies", Icons.Default.Message, onNavigateToLogs)
                    ManagementRow("Diagnostics & Status", "Check service & notification health", Icons.Default.Settings, onNavigateToDiagnostics)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun dashboardSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = DashboardNavy, checkedTrackColor = DashboardMint, checkedBorderColor = DashboardMint,
    uncheckedThumbColor = Color(0xFFF0F4FF), uncheckedTrackColor = Color(0xFF596780),
    uncheckedBorderColor = Color(0xFF8B99B3)
)

@Composable
private fun BlueIcon(icon: ImageVector) {
    Box(Modifier.clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)).padding(12.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BlueIcon(icon)
            Text(value, style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ManagementRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BlueIcon(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardRuleRow(rule: AutoReplyRule, onToggle: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BlueIcon(Icons.Default.Message)
                Text(rule.ruleName, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Switch(rule.isActive, onToggle,
                    modifier = Modifier.semantics { contentDescription = "Enable rule: ${rule.ruleName}" },
                    colors = dashboardSwitchColors())
            }
            Text("Keywords: ${rule.keywordsCsv}", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Match Type: ${rule.matchOption.name}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
