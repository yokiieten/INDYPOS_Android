package com.indybrain.indypos_Android.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import com.indybrain.indypos_Android.R

enum class HomeBottomDestination(
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val labelRes: Int
) {
    Home(Icons.Outlined.Home, Icons.Filled.Home, R.string.home_nav_home),
    Orders(Icons.Outlined.ShoppingCart, Icons.Filled.ShoppingCart, R.string.home_nav_orders),
    Charts(Icons.Outlined.Analytics, Icons.Outlined.Analytics, R.string.home_nav_charts),
    Settings(Icons.Outlined.Settings, Icons.Filled.Settings, R.string.home_nav_settings)
}

