package com.orderflow.autoresponder.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orderflow.autoresponder.R
import com.orderflow.autoresponder.presentation.rules.RulesViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    val colors = MaterialTheme.colorScheme
    LaunchedEffect(Unit) { viewModel.loadDashboardData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold,
                    color = colors.primary, style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = onNavigateToDiagnostics) {
                        Icon(Icons.Default.Info, "Diagnostics & status")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToRules,
                containerColor = colors.primary, contentColor = colors.onPrimary,
                shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.AddComment, "Manage reply rules")
            }
        },
        containerColor = colors.background
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
            .padding(bottom = 88.dp)) {
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp), color = colors.surfaceVariant) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.AutoAwesome, null, tint = colors.primary)
                    Text("Your conversations, on autopilot", style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant)
                }
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(onClick = onNavigateToRules, label = { Text("${state.activeRulesCount} active rules") })
                SuggestionChip(onClick = onNavigateToLogs, label = { Text("${state.todayRepliesCount} replies today") })
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HomeAvatar(Icons.Default.Forum)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Auto-reply", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(when {
                        !state.isAutoResponderEnabled -> "Paused"
                        !state.isNotificationAccessGranted -> "Permission required"
                        else -> "Active Â· Listening for messages"
                    }, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                Switch(state.isAutoResponderEnabled, { viewModel.toggleAutoResponder(it) },
                    modifier = Modifier.semantics { contentDescription = "Auto-reply" })
            }
            if (!state.isNotificationAccessGranted) {
                Surface(onClick = onNavigateToSettings,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    color = colors.errorContainer, shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Enable notification access", color = colors.onErrorContainer,
                            fontWeight = FontWeight.SemiBold)
                        Text("Tap to allow automatic replies", color = colors.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = colors.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Reply rules", Modifier.weight(1f), color = colors.primary,
                    style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onNavigateToRules) { Text("See all") }
            }
            when {
                rulesState.isLoading -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(20.dp))
                rulesState.rules.isEmpty() -> HomeRow("Create your first rule",
                    "Set a keyword and choose an automatic reply", Icons.Default.AddComment, onNavigateToRules)
                else -> rulesState.rules.take(3).forEach { rule ->
                    key(rule.id) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            HomeAvatar(Icons.Default.ChatBubble)
                            Column(Modifier.weight(1f).clickable(onClick = onNavigateToRules),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(rule.ruleName, style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(rule.messages.firstOrNull()?.message ?: "Keywords: ${rule.keywordsCsv}",
                                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Switch(rule.isActive, { rulesViewModel.toggleRule(rule.id, it) },
                                modifier = Modifier.semantics { contentDescription = "Enable rule: ${rule.ruleName}" })
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun HomeAvatar(icon: ImageVector) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun HomeRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        HomeAvatar(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
