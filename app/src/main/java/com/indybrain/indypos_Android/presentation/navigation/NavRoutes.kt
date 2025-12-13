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
    data object AddEditAddon : NavRoutes("add_edit_addon")
    data object AddEditAddonGroup : NavRoutes("add_edit_addon_group")
    data object ProductEdit : NavRoutes("product_edit")
    data object BarcodeScanner : NavRoutes("barcode_scanner")
    data object SearchProduct : NavRoutes("search_product")
    data object Discount : NavRoutes("discount")
    data object CashPayment : NavRoutes("cash_payment")
    data object OrderSummary : NavRoutes("order_summary")
    data object OrderDetail : NavRoutes("order_detail")
    data object StockManagement : NavRoutes("stock_management")
    data object DataManagement : NavRoutes("data_management")
    data object ContactUs : NavRoutes("contact_us")
    data object ReceiptSettings : NavRoutes("receipt_settings")
    data object PrinterSettings : NavRoutes("printer_settings")
    data object BluetoothPrinterScan : NavRoutes("bluetooth_printer_scan")
    
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
        
        const val ADD_EDIT_ADDON_ROUTE = "add_edit_addon/{addonId}"
        fun addEditAddon(addonId: String?) = if (addonId != null) {
            "add_edit_addon/$addonId"
        } else {
            "add_edit_addon/null"
        }
        
        const val ADD_EDIT_ADDON_GROUP_ROUTE = "add_edit_addon_group/{addonGroupId}"
        fun addEditAddonGroup(addonGroupId: String?) = if (addonGroupId != null) {
            "add_edit_addon_group/$addonGroupId"
        } else {
            "add_edit_addon_group/null"
        }
        
        const val PRODUCT_EDIT_ROUTE = "product_edit/{productId}/{productName}"
        fun productEdit(productId: String, productName: String = "") = 
            "product_edit/$productId/${java.net.URLEncoder.encode(productName, "UTF-8")}"
        
        const val DISCOUNT_ROUTE = "discount/{subtotal}"
        fun discount(subtotal: Double) = "discount/$subtotal"
        
        const val CASH_PAYMENT_ROUTE = "cash_payment/{totalAmount}/{subtotal}/{discount}"
        fun cashPayment(totalAmount: Double, subtotal: Double, discount: Double) = 
            "cash_payment/$totalAmount/$subtotal/$discount"
        
        const val ORDER_SUMMARY_ROUTE = "order_summary/{totalAmount}"
        fun orderSummary(totalAmount: Double) = "order_summary/$totalAmount"
        
        const val ORDER_DETAIL_ROUTE = "order_detail/{orderId}"
        fun orderDetail(orderId: String) = "order_detail/$orderId"
    }
}

