package com.orderflow.admin.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.orderflow.admin.core.designsystem.components.OrderFlowBottomNavBar
import com.orderflow.admin.presentation.analytics.AnalyticsScreen
import com.orderflow.admin.presentation.dashboard.DashboardScreen
import com.orderflow.admin.presentation.devicedetails.DeviceDetailsScreen
import com.orderflow.admin.presentation.devicelist.DeviceListScreen
import com.orderflow.admin.presentation.login.LoginScreen
import com.orderflow.admin.presentation.logs.LogsScreen
import com.orderflow.admin.presentation.notifications.NotificationsScreen
import com.orderflow.admin.presentation.profile.ProfileScreen
import com.orderflow.admin.presentation.settings.SettingsScreen
import com.orderflow.admin.presentation.splash.SplashScreen
import com.orderflow.admin.presentation.subscription.SubscriptionEditorScreen

@Composable
fun OrderFlowNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = listOf(
        Screen.Dashboard.route,
        Screen.DeviceList.route,
        Screen.Analytics.route,
        Screen.Notifications.route,
        Screen.Settings.route
    )

    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                OrderFlowBottomNavBar(
                    currentRoute = currentRoute,
                    onItemSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToDeviceDetails = { deviceId ->
                        navController.navigate(Screen.DeviceDetails.createRoute(deviceId))
                    },
                    onNavigateToDevicesList = {
                        navController.navigate(Screen.DeviceList.route)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    }
                )
            }

            composable(Screen.DeviceList.route) {
                DeviceListScreen(
                    onNavigateToDeviceDetails = { deviceId ->
                        navController.navigate(Screen.DeviceDetails.createRoute(deviceId))
                    }
                )
            }

            composable(
                route = Screen.DeviceDetails.route,
                arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
            ) {
                DeviceDetailsScreen(
                    onBackClick = { navController.popBackStack() },
                    onOpenSubscriptionEditor = { deviceId ->
                        navController.navigate(Screen.SubscriptionEditor.createRoute(deviceId))
                    }
                )
            }

            composable(
                route = Screen.SubscriptionEditor.route,
                arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
            ) {
                SubscriptionEditorScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen()
            }

            composable(Screen.Logs.route) {
                LogsScreen()
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToLogs = { navController.navigate(Screen.Logs.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onLoggedOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }
}
