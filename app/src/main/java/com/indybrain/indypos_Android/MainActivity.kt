package com.indybrain.indypos_Android

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.presentation.home.HomeScreen
import com.indybrain.indypos_Android.presentation.login.LoginScreen
import com.indybrain.indypos_Android.presentation.navigation.NavRoutes
import com.indybrain.indypos_Android.presentation.orderproduct.OrderProductScreen
import com.indybrain.indypos_Android.presentation.products.MainProductScreen
import com.indybrain.indypos_Android.presentation.products.ProductDetailScreen
import com.indybrain.indypos_Android.presentation.products.SearchProductScreen
import com.indybrain.indypos_Android.presentation.settings.LanguageSettingsScreen
import com.indybrain.indypos_Android.presentation.settings.AccountScreen
import com.indybrain.indypos_Android.presentation.settings.ChangePasswordScreen
import com.indybrain.indypos_Android.presentation.settings.OrderSettingsScreen
import com.indybrain.indypos_Android.presentation.splash.SplashScreen
import com.indybrain.indypos_Android.presentation.addongroupmanagement.AddonGroupManagementScreen
import com.indybrain.indypos_Android.presentation.addongroupmanagement.AddEditAddonGroupScreen
import com.indybrain.indypos_Android.presentation.addonmanagement.AddEditAddonScreen
import com.indybrain.indypos_Android.presentation.addonmanagement.AddOnManagementScreen
import com.indybrain.indypos_Android.presentation.categorymanagement.AddEditCategoryScreen
import com.indybrain.indypos_Android.presentation.categorymanagement.CategoryManagementScreen
import com.indybrain.indypos_Android.presentation.productmanagement.AddEditProductScreen
import com.indybrain.indypos_Android.presentation.productmanagement.ProductManagementScreen
import com.indybrain.indypos_Android.presentation.productedit.ProductEditScreen
import com.indybrain.indypos_Android.presentation.discount.DiscountScreen
import com.indybrain.indypos_Android.presentation.settings.OrderSettingsItem
import com.indybrain.indypos_Android.ui.theme.INDYPOS_AndroidTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var languageLocalDataSource: LanguageLocalDataSource
    
    override fun attachBaseContext(newBase: Context) {
        val localeCode = try {
            val prefs = newBase.getSharedPreferences("indypos_prefs", Context.MODE_PRIVATE)
            prefs.getInt("key_language_locale", 1054)
        } catch (e: Exception) {
            1054
        }
        val context = LocaleHelper.setLocale(newBase, localeCode)
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            INDYPOS_AndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var scannedBarcode by remember { mutableStateOf<String?>(null) }
                    
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.Splash.route
                    ) {
                        composable(NavRoutes.Splash.route) {
                            SplashScreen(
                                onNavigateToHome = {
                                    navController.navigate(NavRoutes.Home.route) {
                                        popUpTo(NavRoutes.Splash.route) {
                                            inclusive = true
                                        }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.navigate(NavRoutes.Login.route) {
                                        popUpTo(NavRoutes.Splash.route) {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }
                        
                        composable(NavRoutes.Login.route) {
                            LoginScreen(
                                onLoginSuccess = {
                                    // Navigate to home screen after successful login
                                    navController.navigate(NavRoutes.Home.route) {
                                        popUpTo(NavRoutes.Login.route) {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }
                        
                        composable(NavRoutes.Home.route) {
                            HomeScreen(
                                onNavigateToMainProduct = {
                                    navController.navigate(NavRoutes.MainProduct.route)
                                },
                                onLogoutSuccess = {
                                    // Navigate to login screen after logout
                                    navController.navigate(NavRoutes.Login.route) {
                                        popUpTo(NavRoutes.Home.route) {
                                            inclusive = true
                                        }
                                    }
                                },
                                onNavigateToLanguageSettings = {
                                    navController.navigate(NavRoutes.LanguageSettings.route)
                                },
                                onNavigateToAccountSettings = {
                                    navController.navigate(NavRoutes.AccountSettings.route)
                                },
                                onNavigateToChangePassword = {
                                    navController.navigate(NavRoutes.ChangePassword.route)
                                },
                                onNavigateToOrderSettings = {
                                    navController.navigate(NavRoutes.OrderSettings.route)
                                }
                            )
                        }
                        
                        composable(NavRoutes.MainProduct.route) {
                            MainProductScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onProductClick = { productId, productName, isInCart ->
                                    if (isInCart) {
                                        // Navigate to ProductEditScreen if product is in cart
                                        navController.navigate(NavRoutes.productEdit(productId, productName))
                                    } else {
                                        // Navigate to ProductDetailScreen if product is not in cart
                                        navController.navigate(NavRoutes.productDetail(productId))
                                    }
                                },
                                onCartClick = {
                                    navController.navigate(NavRoutes.OrderProduct.route)
                                },
                                onBarcodeScannerClick = {
                                    scannedBarcode = null // Clear previous barcode
                                    navController.navigate(NavRoutes.BarcodeScanner.route)
                                },
                                onSearchClick = {
                                    navController.navigate(NavRoutes.SearchProduct.route)
                                },
                                scannedBarcode = scannedBarcode
                            )
                        }
                        
                        composable(NavRoutes.SearchProduct.route) {
                            SearchProductScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onProductClick = { productId, productName, isInCart ->
                                    if (isInCart) {
                                        // Navigate to ProductEditScreen if product is in cart
                                        navController.navigate(NavRoutes.productEdit(productId, productName))
                                    } else {
                                        // Navigate to ProductDetailScreen if product is not in cart
                                        navController.navigate(NavRoutes.productDetail(productId))
                                    }
                                }
                            )
                        }
                        
                        composable(NavRoutes.BarcodeScanner.route) {
                            com.indybrain.indypos_Android.presentation.barcodescanner.BarcodeScannerScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onBarcodeScanned = { barcode ->
                                    // Set scanned barcode and navigate back to MainProduct
                                    scannedBarcode = barcode
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(
                            route = NavRoutes.PRODUCT_DETAIL_ROUTE,
                            arguments = listOf(navArgument("productId") {})
                        ) { backStackEntry ->
                            val productId = backStackEntry.arguments?.getString("productId") ?: ""
                            ProductDetailScreen(
                                productId = productId,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(NavRoutes.OrderProduct.route) {
                            val orderProductViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.indybrain.indypos_Android.presentation.orderproduct.OrderProductViewModel>()
                            OrderProductScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onAddMenuClick = {
                                    navController.navigate(NavRoutes.MainProduct.route) {
                                        popUpTo(NavRoutes.OrderProduct.route)
                                    }
                                },
                                onEditItemClick = { productId, productName ->
                                    navController.navigate(NavRoutes.productEdit(productId, productName))
                                },
                                onDiscountClick = {
                                    val subtotal = orderProductViewModel.calculateSubtotal()
                                    navController.navigate(NavRoutes.discount(subtotal))
                                },
                                onPlaceOrderClick = { totalAmount, subtotal, discount ->
                                    navController.navigate(NavRoutes.cashPayment(totalAmount, subtotal, discount))
                                },
                                viewModel = orderProductViewModel
                            )
                        }
                        
                        composable(
                            route = NavRoutes.DISCOUNT_ROUTE,
                            arguments = listOf(navArgument("subtotal") {})
                        ) { backStackEntry ->
                            val subtotalString = backStackEntry.arguments?.getString("subtotal") ?: "0.0"
                            val subtotal = subtotalString.toDoubleOrNull() ?: 0.0
                            val orderProductViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.indybrain.indypos_Android.presentation.orderproduct.OrderProductViewModel>()
                            
                            DiscountScreen(
                                subtotal = subtotal,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onCancel = {
                                    navController.popBackStack()
                                },
                                onDiscountSelected = { discount ->
                                    val discountAmount = when (discount.type) {
                                        com.indybrain.indypos_Android.presentation.discount.DiscountType.PERCENTAGE -> {
                                            (subtotal * discount.value / 100.0).coerceAtMost(subtotal)
                                        }
                                        com.indybrain.indypos_Android.presentation.discount.DiscountType.FIXED_AMOUNT -> {
                                            discount.value.coerceAtMost(subtotal)
                                        }
                                    }
                                    orderProductViewModel.setDiscountAmount(discountAmount)
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(
                            route = NavRoutes.CASH_PAYMENT_ROUTE,
                            arguments = listOf(
                                navArgument("totalAmount") {},
                                navArgument("subtotal") {},
                                navArgument("discount") {}
                            )
                        ) { backStackEntry ->
                            val totalAmountString = backStackEntry.arguments?.getString("totalAmount") ?: "0.0"
                            val subtotalString = backStackEntry.arguments?.getString("subtotal") ?: "0.0"
                            val discountString = backStackEntry.arguments?.getString("discount") ?: "0.0"
                            
                            val totalAmount = totalAmountString.toDoubleOrNull() ?: 0.0
                            val subtotal = subtotalString.toDoubleOrNull() ?: 0.0
                            val discount = discountString.toDoubleOrNull() ?: 0.0
                            
                            com.indybrain.indypos_Android.presentation.cashpayment.CashPaymentScreen(
                                totalAmount = totalAmount,
                                subtotal = subtotal,
                                discount = discount,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onPaymentComplete = { change ->
                                    // Navigate to order screen or home
                                    navController.navigate(NavRoutes.Home.route) {
                                        popUpTo(NavRoutes.OrderProduct.route) {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }
                        
                        composable(
                            route = NavRoutes.PRODUCT_EDIT_ROUTE,
                            arguments = listOf(
                                navArgument("productId") {},
                                navArgument("productName") { 
                                    nullable = true
                                    defaultValue = ""
                                }
                            )
                        ) { backStackEntry ->
                            val productId = backStackEntry.arguments?.getString("productId") ?: ""
                            val productName = try {
                                java.net.URLDecoder.decode(
                                    backStackEntry.arguments?.getString("productName") ?: "",
                                    "UTF-8"
                                )
                            } catch (e: Exception) {
                                backStackEntry.arguments?.getString("productName") ?: ""
                            }
                            ProductEditScreen(
                                productId = productId,
                                productName = productName,
                                onDismiss = {
                                    navController.popBackStack()
                                },
                                onAddAnother = {
                                    navController.navigate(NavRoutes.productDetail(productId)) {
                                        popUpTo(NavRoutes.PRODUCT_EDIT_ROUTE)
                                    }
                                },
                                onUpdateBasket = {
                                    navController.popBackStack()
                                },
                                onEditClick = { editProductId ->
                                    navController.navigate(NavRoutes.productDetail(editProductId)) {
                                        popUpTo(NavRoutes.PRODUCT_EDIT_ROUTE)
                                    }
                                }
                            )
                        }
                        
                        composable(NavRoutes.LanguageSettings.route) {
                            LanguageSettingsScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(NavRoutes.AccountSettings.route) {
                            AccountScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(NavRoutes.ChangePassword.route) {
                            ChangePasswordScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(NavRoutes.OrderSettings.route) {
                            OrderSettingsScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onOrderSettingsItemClick = { item ->
                                    when (item) {
                                        OrderSettingsItem.CategoryManagement -> {
                                            navController.navigate(NavRoutes.CategoryManagement.route)
                                        }
                                        OrderSettingsItem.ProductManagement -> {
                                            navController.navigate(NavRoutes.ProductManagement.route)
                                        }
                                        OrderSettingsItem.AddonGroupManagement -> {
                                            navController.navigate(NavRoutes.AddonGroupManagement.route)
                                        }
                                        OrderSettingsItem.AddonManagement -> {
                                            navController.navigate(NavRoutes.AddonManagement.route)
                                        }
                                        else -> {
                                            // TODO: Handle other order settings items
                                        }
                                    }
                                }
                            )
                        }
                        
                        composable(NavRoutes.CategoryManagement.route) {
                            CategoryManagementScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onAddCategoryClick = {
                                    navController.navigate(NavRoutes.addEditCategory(null))
                                },
                                onEditCategoryClick = { categoryId ->
                                    navController.navigate(NavRoutes.addEditCategory(categoryId))
                                }
                            )
                        }
                        
                        composable(NavRoutes.ProductManagement.route) {
                            ProductManagementScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onAddProductClick = {
                                    navController.navigate(NavRoutes.addEditProduct(null))
                                },
                                onEditProductClick = { productId ->
                                    navController.navigate(NavRoutes.addEditProduct(productId))
                                }
                            )
                        }
                        
                        composable(
                            route = NavRoutes.ADD_EDIT_PRODUCT_ROUTE,
                            arguments = listOf(navArgument("productId") { nullable = true })
                        ) { backStackEntry ->
                            val productId = backStackEntry.arguments?.getString("productId")
                            val actualProductId = if (productId == "null") null else productId
                            AddEditProductScreen(
                                productId = actualProductId,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onSaveSuccess = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(
                            route = NavRoutes.ADD_EDIT_CATEGORY_ROUTE,
                            arguments = listOf(navArgument("categoryId") { nullable = true })
                        ) { backStackEntry ->
                            val categoryId = backStackEntry.arguments?.getString("categoryId")
                            val actualCategoryId = if (categoryId == "null") null else categoryId
                            AddEditCategoryScreen(
                                categoryId = actualCategoryId,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onSaveSuccess = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(NavRoutes.AddonGroupManagement.route) {
                            AddonGroupManagementScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onAddAddonGroupClick = {
                                    navController.navigate(NavRoutes.addEditAddonGroup(null))
                                },
                                onEditAddonGroupClick = { addonGroupId ->
                                    navController.navigate(NavRoutes.addEditAddonGroup(addonGroupId))
                                }
                            )
                        }
                        
                        composable(NavRoutes.AddonManagement.route) {
                            AddOnManagementScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onAddAddonClick = {
                                    navController.navigate(NavRoutes.addEditAddon(null))
                                },
                                onEditAddonClick = { addonId ->
                                    navController.navigate(NavRoutes.addEditAddon(addonId))
                                }
                            )
                        }
                        
                        composable(
                            route = NavRoutes.ADD_EDIT_ADDON_ROUTE,
                            arguments = listOf(navArgument("addonId") { nullable = true })
                        ) { backStackEntry ->
                            val addonId = backStackEntry.arguments?.getString("addonId")
                            val actualAddonId = if (addonId == "null") null else addonId
                            AddEditAddonScreen(
                                addonId = actualAddonId,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onSaveSuccess = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(
                            route = NavRoutes.ADD_EDIT_ADDON_GROUP_ROUTE,
                            arguments = listOf(navArgument("addonGroupId") { nullable = true })
                        ) { backStackEntry ->
                            val addonGroupId = backStackEntry.arguments?.getString("addonGroupId")
                            val actualAddonGroupId = if (addonGroupId == "null") null else addonGroupId
                            AddEditAddonGroupScreen(
                                addonGroupId = actualAddonGroupId,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onSaveSuccess = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}