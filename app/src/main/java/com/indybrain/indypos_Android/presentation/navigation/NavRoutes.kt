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
    data object CategoryManagement : NavRoutes("category_management")
    data object ProductManagement : NavRoutes("product_management")
    data object AddEditProduct : NavRoutes("add_edit_product")
    data object AddonGroupManagement : NavRoutes("addon_group_management")
    data object AddonManagement : NavRoutes("addon_management")
    
    companion object {
        const val ADD_EDIT_PRODUCT_ROUTE = "add_edit_product/{productId}"
        fun addEditProduct(productId: String?) = if (productId != null) {
            "add_edit_product/$productId"
        } else {
            "add_edit_product/null"
        }
        const val PRODUCT_DETAIL_ROUTE = "product_detail/{productId}"
        fun productDetail(productId: String) = "product_detail/$productId"
        
        const val ADD_EDIT_CATEGORY_ROUTE = "add_edit_category/{categoryId}"
        fun addEditCategory(categoryId: String?) = if (categoryId != null) {
            "add_edit_category/$categoryId"
        } else {
            "add_edit_category/null"
        }
    }
}

