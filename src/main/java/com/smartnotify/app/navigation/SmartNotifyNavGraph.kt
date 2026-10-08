package com.smartnotify.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.smartnotify.app.ui.developer.DeveloperDiagnosticScreen
import com.smartnotify.app.ui.history.HistoryScreen
import com.smartnotify.app.ui.home.HomeScreen
import com.smartnotify.app.ui.settings.*
import com.smartnotify.app.ui.theme.CardBg
import com.smartnotify.app.ui.theme.PrimaryIndigo
import com.smartnotify.app.ui.theme.TextMuted
import com.smartnotify.app.viewmodel.*

@Composable
fun SmartNotifyMainApp() {
    val navController = rememberNavController()

    // Shared ViewModels
    val homeViewModel: HomeViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.SessionSummary,
        Screen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavItems.map { it.route }) {
                NavigationBar(
                    containerColor = CardBg,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Text(text = screen.icon, fontSize = 18.sp) },
                            label = { Text(text = screen.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryIndigo,
                                unselectedIconColor = TextMuted,
                                indicatorColor = PrimaryIndigo.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToSessionSummary = { navController.navigate(Screen.SessionSummary.route) }
                )
            }
            composable(Screen.SessionSummary.route) {
                HistoryScreen(viewModel = historyViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToAlertPrefs = { navController.navigate(Screen.AlertPreferences.route) },
                    onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) },
                    onNavigateToDeveloperDiagnostics = { navController.navigate(Screen.DeveloperDiagnostics.route) }
                )
            }
            composable(Screen.AlertPreferences.route) {
                AlertPreferencesScreen(viewModel = settingsViewModel)
            }
            composable(Screen.Privacy.route) {
                PrivacyScreen()
            }
            composable(Screen.About.route) {
                AboutScreen()
            }
            composable(Screen.DeveloperDiagnostics.route) {
                DeveloperDiagnosticScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
