package com.indybrain.indypos_Android.presentation.navigation

/**
 * Sealed class for navigation routes
 */
sealed class NavRoutes(val route: String) {
    data object Login : NavRoutes("login")
    data object Home : NavRoutes("home")
}

