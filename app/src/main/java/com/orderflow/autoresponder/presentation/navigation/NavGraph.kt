package com.orderflow.autoresponder.presentation.navigation

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
    const val DIAGNOSTICS = "diagnostics"
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    var editingRule: AutoReplyRule? = null

    NavHost(
        navController = navController,
        startDestination = Screen.DASHBOARD
    ) {
        composable(Screen.DASHBOARD) {
            val viewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToRules = { navController.navigate(Screen.RULES_LIST) },
                onNavigateToLogs = { navController.navigate(Screen.LOGS) },
                onNavigateToCustomers = { navController.navigate(Screen.CUSTOMERS) },
                onNavigateToSettings = { navController.navigate(Screen.SETTINGS) },
                onNavigateToDiagnostics = { navController.navigate(Screen.DIAGNOSTICS) }
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
                onNavigateBack = { navController.popBackStack() }
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
