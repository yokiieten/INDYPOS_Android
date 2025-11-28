package com.indybrain.indypos_Android.presentation.navigation

/**
 * Sealed class for navigation routes
 */
sealed class NavRoutes(val route: String) {
    data object Splash : NavRoutes("splash")
    data object Login : NavRoutes("login")
    data object Home : NavRoutes("home")
    data object MainProduct : NavRoutes("main_product")
    data object OrderProduct : NavRoutes("order_product")
    data object LanguageSettings : NavRoutes("language_settings")
    data object AccountSettings : NavRoutes("account_settings")
    data object ChangePassword : NavRoutes("change_password")
    data object OrderSettings : NavRoutes("order_settings")
    
    companion object {
        const val PRODUCT_DETAIL_ROUTE = "product_detail/{productId}"
        fun productDetail(productId: String) = "product_detail/$productId"
    }
}

