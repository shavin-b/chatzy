package com.orderflow.autoresponder.presentation.navigation

import android.content.Intent
import android.content.ActivityNotFoundException
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.orderflow.autoresponder.presentation.help.HelpAction
import com.orderflow.autoresponder.presentation.help.HelpScreen
import com.orderflow.autoresponder.presentation.help.HelpViewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.orderflow.autoresponder.domain.model.AutoReplyRule
import com.orderflow.autoresponder.presentation.customers.CustomersScreen
import com.orderflow.autoresponder.presentation.customers.CustomersViewModel
import com.orderflow.autoresponder.presentation.dashboard.DashboardScreen
import com.orderflow.autoresponder.presentation.dashboard.DashboardViewModel
import com.orderflow.autoresponder.presentation.logs.LogsScreen
import com.orderflow.autoresponder.presentation.logs.LogsViewModel
import com.orderflow.autoresponder.presentation.rules.AddEditRuleScreen
import com.orderflow.autoresponder.presentation.rules.RulesListScreen
import com.orderflow.autoresponder.presentation.rules.RulesViewModel
import com.orderflow.autoresponder.presentation.settings.DiagnosticsScreen
import com.orderflow.autoresponder.presentation.settings.DiagnosticsViewModel
import com.orderflow.autoresponder.presentation.settings.SettingsScreen
import com.orderflow.autoresponder.presentation.settings.SettingsViewModel

object Screen {
    const val DASHBOARD = "dashboard"
    const val RULES_LIST = "rules_list"
    const val ADD_EDIT_RULE = "add_edit_rule"
    const val LOGS = "logs"
    const val CUSTOMERS = "customers"
    const val SETTINGS = "settings"
    const val HELP = "help"
    const val DIAGNOSTICS = "diagnostics"
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    var editingRule by remember { mutableStateOf<AutoReplyRule?>(null) }

    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val tabs = listOf(
        Triple(Screen.DASHBOARD, "Home", Icons.Default.ChatBubble),
        Triple(Screen.RULES_LIST, "Rules", Icons.Default.AutoAwesome),
        Triple(Screen.CUSTOMERS, "Customers", Icons.Default.People),
        Triple(Screen.LOGS, "Activity", Icons.Default.History)
    )
    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(Screen.DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (tabs.any { it.first == currentRoute }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    tabs.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = { navigateToTab(route) },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            navController = navController,
            startDestination = Screen.DASHBOARD
        ) {
            composable(Screen.DASHBOARD) {
                val viewModel: DashboardViewModel = hiltViewModel()
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToRules = { navigateToTab(Screen.RULES_LIST) },
                    onNavigateToLogs = { navigateToTab(Screen.LOGS) },
                    onNavigateToCustomers = { navigateToTab(Screen.CUSTOMERS) },
                    onNavigateToSettings = { navController.navigate(Screen.SETTINGS) },
                    onNavigateToHelp = { navController.navigate(Screen.HELP) }
                )
            }

            composable(Screen.RULES_LIST) {
                val viewModel: RulesViewModel = hiltViewModel()
                RulesListScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onAddRule = {
                        editingRule = null
                        navController.navigate(Screen.ADD_EDIT_RULE)
                    },
                    onEditRule = { rule ->
                        editingRule = rule
                        navController.navigate(Screen.ADD_EDIT_RULE)
                    }
                )
            }

            composable(Screen.ADD_EDIT_RULE) {
                val viewModel: RulesViewModel = hiltViewModel()
                AddEditRuleScreen(
                    editingRule = editingRule,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.LOGS) {
                val viewModel: LogsViewModel = hiltViewModel()
                LogsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.CUSTOMERS) {
                val viewModel: CustomersViewModel = hiltViewModel()
                CustomersScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SETTINGS) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHelp = { navController.navigate(Screen.HELP) }
                )
            }

            composable(Screen.HELP) {
                val viewModel: HelpViewModel = hiltViewModel()
                HelpScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onAction = { action ->
                        when (action) {
                            HelpAction.SERVICE_STATUS -> navController.navigate(Screen.DIAGNOSTICS)
                            HelpAction.RULES -> navController.navigate(Screen.RULES_LIST)
                            HelpAction.SETTINGS -> navController.navigate(Screen.SETTINGS)
                            HelpAction.NOTIFICATION_ACCESS -> {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, "Open Android Settings and search for Notification Access.", Toast.LENGTH_LONG).show()
                                } catch (_: SecurityException) {
                                    Toast.makeText(context, "Open Android Settings and search for Notification Access.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
            }

            composable(Screen.DIAGNOSTICS) {
                val viewModel: DiagnosticsViewModel = hiltViewModel()
                DiagnosticsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
}
}
