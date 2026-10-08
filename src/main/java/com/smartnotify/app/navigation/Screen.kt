package com.smartnotify.app.navigation

sealed class Screen(val route: String, val title: String, val icon: String = "") {
    object Home : Screen("home", "Home", "🏠")
    object SessionSummary : Screen("session_summary", "Session", "📋")
    object Settings : Screen("settings", "Settings", "⚙️")
    
    // Sub-screens
    object AlertPreferences : Screen("alert_prefs", "Alert Preferences")
    object Privacy : Screen("privacy", "Privacy Policy")
    object About : Screen("about", "About SmartNotify")
    object DeveloperDiagnostics : Screen("dev_diagnostics", "Developer Diagnostics")
}
