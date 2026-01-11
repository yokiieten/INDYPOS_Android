package com.indybrain.indypos_Android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.launch
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.presentation.forgotpassword.ForgotPasswordScreen
import com.indybrain.indypos_Android.presentation.resetpassword.ResetPasswordScreen
import com.indybrain.indypos_Android.presentation.home.HomeScreen
import com.indybrain.indypos_Android.presentation.login.LoginScreen
import com.indybrain.indypos_Android.presentation.register.RegisterScreen
import com.indybrain.indypos_Android.presentation.navigation.NavRoutes
import com.indybrain.indypos_Android.presentation.orderproduct.OrderProductScreen
import com.indybrain.indypos_Android.presentation.orderdetail.OrderDetailScreen
import com.indybrain.indypos_Android.presentation.products.MainProductScreen
import com.indybrain.indypos_Android.presentation.products.ProductDetailScreen
import com.indybrain.indypos_Android.presentation.products.SearchProductScreen
import com.indybrain.indypos_Android.presentation.settings.LanguageSettingsScreen
import com.indybrain.indypos_Android.presentation.settings.AccountScreen
import com.indybrain.indypos_Android.presentation.settings.ChangePasswordScreen
import com.indybrain.indypos_Android.presentation.settings.OrderSettingsScreen
import com.indybrain.indypos_Android.presentation.stockmanagement.StockManagementScreen
import com.indybrain.indypos_Android.presentation.splash.SplashScreen
import com.indybrain.indypos_Android.presentation.datamanagement.DataManagementScreen
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
import com.indybrain.indypos_Android.presentation.contactus.ContactUsScreen
import com.indybrain.indypos_Android.presentation.settings.OrderSettingsItem
import com.indybrain.indypos_Android.presentation.settings.printer.PrinterSettingsScreen
import com.indybrain.indypos_Android.presentation.settings.printer.BluetoothPrinterScanScreen
import com.indybrain.indypos_Android.ui.theme.INDYPOS_AndroidTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var languageLocalDataSource: LanguageLocalDataSource
    
    // Flag to track if we received a new intent from onNewIntent
    private var hasNewIntent = false
    
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
            INDYPOS_AndroidTheme(
                darkTheme = false,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val coroutineScope = rememberCoroutineScope()
                    var scannedBarcode by remember { mutableStateOf<String?>(null) }
                    var scannedBarcodeForProduct by remember { mutableStateOf<String?>(null) }
                    
                    // Store current intent URI and timestamp to force updates
                    var currentIntentUri by remember { mutableStateOf(intent?.data?.toString()) }
                    var lastIntentTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
                    // Track the last processed intent URI to prevent re-processing on resume
                    var lastProcessedIntentUri by remember { mutableStateOf<String?>(null) }
                    // Track if we have a new intent from onNewIntent that needs processing
                    var hasNewIntentToProcess by remember { mutableStateOf(false) }
                    
                    // Helper function to check if URI is a reset-password deep link
                    fun isResetPasswordDeepLink(uri: String?): Boolean {
                        if (uri == null) return false
                        return uri.contains("reset-password") && 
                               (uri.contains("indy-pos.com") || 
                                uri.contains("dev.indy-pos.com") || 
                                uri.contains("stg.indy-pos.com"))
                    }
                    
                    // Watch lifecycle to handle new intents from onNewIntent
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                                // On resume, check if intent has changed (from onNewIntent)
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(100)
                                    val newIntentUri = this@MainActivity.intent?.data?.toString()
                                    if (newIntentUri != null) {
                                        val isResetPassword = isResetPasswordDeepLink(newIntentUri)
                                        val isNewIntent = this@MainActivity.hasNewIntent
                                        
                                        // Reset the flag after checking
                                        this@MainActivity.hasNewIntent = false
                                        
                                        // Determine if we should update and process the deep link
                                        val shouldUpdate = when {
                                            // New intent from onNewIntent: always update
                                            isNewIntent -> {
                                                hasNewIntentToProcess = true
                                                true
                                            }
                                            // Reset-password on resume: only process if URI changed (new link clicked)
                                            isResetPassword -> newIntentUri != lastProcessedIntentUri
                                            // Other deep links: only process if URI changed
                                            else -> newIntentUri != currentIntentUri
                                        }
                                        
                                        if (shouldUpdate) {
                                            val currentTime = System.currentTimeMillis()
                                            currentIntentUri = newIntentUri
                                            lastIntentTimestamp = currentTime
                                        }
                                    }
                                }
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }
                    
                    // Handle deep link when intent URI or timestamp changes (both onCreate and onNewIntent)
                    LaunchedEffect(currentIntentUri, lastIntentTimestamp) {
                        if (currentIntentUri != null) {
                            val isResetPassword = isResetPasswordDeepLink(currentIntentUri)
                            
                            // Determine if we should process the deep link
                            val shouldProcess = when {
                                // New intent from onNewIntent: always process (even if same URI for reset-password)
                                hasNewIntentToProcess -> {
                                    hasNewIntentToProcess = false
                                    true
                                }
                                // Reset-password: only process if URI changed (new link clicked)
                                isResetPassword -> currentIntentUri != lastProcessedIntentUri
                                // Other deep links: only process if URI changed
                                else -> currentIntentUri != lastProcessedIntentUri
                            }
                            
                            if (shouldProcess) {
                                // Mark this deep link as processed before handling
                                lastProcessedIntentUri = currentIntentUri
                                handleDeepLink(this@MainActivity.intent, navController, coroutineScope)
                            }
                        }
                    }
                    
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
                                },
                                onForgotPasswordClick = {
                                    // Navigate to forgot password screen
                                    navController.navigate(NavRoutes.ForgotPassword.route)
                                },
                                onCreateAccountClick = {
                                    // Navigate to register screen
                                    navController.navigate(NavRoutes.Register.route)
                                }
                            )
                        }
                        
                        composable(NavRoutes.ForgotPassword.route) {
                            ForgotPasswordScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onSuccess = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(
                            route = NavRoutes.RESET_PASSWORD_ROUTE,
                            arguments = listOf(navArgument("token") {})
                        ) { backStackEntry ->
                            val token = backStackEntry.arguments?.getString("token") ?: ""
                            ResetPasswordScreen(
                                token = token,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onSuccess = {
                                    // Navigate to login screen after successful password reset
                                    navController.navigate(NavRoutes.Login.route) {
                                        popUpTo(NavRoutes.Splash.route) {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }
                        
                        composable(NavRoutes.Register.route) {
                            RegisterScreen(
                                onRegistrationSuccess = {
                                    // Navigate to home screen after successful registration
                                    navController.navigate(NavRoutes.Home.route) {
                                        popUpTo(NavRoutes.Register.route) {
                                            inclusive = true
                                        }
                                    }
                                },
                                onBackClick = {
                                    navController.popBackStack()
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
                                },
                                onNavigateToStockManagement = {
                                    navController.navigate(NavRoutes.StockManagement.route)
                                },
                                onNavigateToDataManagement = {
                                    navController.navigate(NavRoutes.DataManagement.route)
                                },
                                onNavigateToContactUs = {
                                    navController.navigate(NavRoutes.ContactUs.route)
                                },
                                onNavigateToReceiptSettings = {
                                    navController.navigate(NavRoutes.ReceiptSettings.route)
                                },
                                onNavigateToPrinterSettings = {
                                    navController.navigate(NavRoutes.PrinterSettings.route)
                                },
                                onNavigateToOrderDetail = { orderId ->
                                    navController.navigate(NavRoutes.orderDetail(orderId))
                                }
                            )
                        }
                        
                        composable(NavRoutes.MainProduct.route) {
                            MainProductScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onProductClick = { productId, productName, isInCart ->
                                    // Clear scannedBarcode before navigation to prevent re-trigger
                                    scannedBarcode = null
                                    if (isInCart) {
                                        // Navigate to ProductEditScreen if product is in cart
                                        navController.navigate(NavRoutes.productEdit(productId, productName))
                                    } else {
                                        // Navigate to ProductDetailScreen if product is not in cart
                                        navController.navigate(NavRoutes.productDetail(productId)) {
                                            // Ensure we can navigate back to MainProduct
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onProductClickFromScan = { productId, productName ->
                                    // Always navigate to ProductDetailScreen when scanned (even if in cart)
                                    scannedBarcode = null
                                    navController.navigate(NavRoutes.productDetail(productId)) {
                                        // Ensure we can navigate back to MainProduct
                                        launchSingleTop = true
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
                            var sourceScreen by remember { mutableStateOf<String?>(null) }
                            
                            // Determine which screen we came from by checking the back stack
                            LaunchedEffect(Unit) {
                                sourceScreen = navController.previousBackStackEntry?.destination?.route
                            }
                            
                            com.indybrain.indypos_Android.presentation.barcodescanner.BarcodeScannerScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onBarcodeScanned = { barcode ->
                                    // Set scanned barcode based on source screen
                                    if (sourceScreen?.contains("add_edit_product") == true) {
                                        scannedBarcodeForProduct = barcode
                                    } else {
                                        scannedBarcode = barcode
                                    }
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(
                            route = NavRoutes.PRODUCT_DETAIL_ROUTE,
                            arguments = listOf(
                                navArgument("productId") {},
                                navArgument("cartItemId") { nullable = true }
                            )
                        ) { backStackEntry ->
                            val productId = backStackEntry.arguments?.getString("productId") ?: ""
                            val rawCartItemId = backStackEntry.arguments?.getString("cartItemId")
                            val cartItemId = if (rawCartItemId == null || rawCartItemId == "null") null else rawCartItemId
                            // Clear scannedBarcode when entering ProductDetailScreen to prevent re-trigger
                            LaunchedEffect(Unit) {
                                scannedBarcode = null
                            }
                            ProductDetailScreen(
                                productId = productId,
                                cartItemId = cartItemId,
                                onBackClick = {
                                    // Clear scannedBarcode before going back
                                    scannedBarcode = null
                                    // Check if we can pop back, otherwise navigate to MainProduct
                                    if (!navController.popBackStack()) {
                                        navController.navigate(NavRoutes.MainProduct.route) {
                                            popUpTo(NavRoutes.Home.route) {
                                                inclusive = false
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        
                        composable(NavRoutes.OrderProduct.route) {
                            val orderProductViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.indybrain.indypos_Android.presentation.orderproduct.OrderProductViewModel>()
                            val coroutineScope = rememberCoroutineScope()
                            OrderProductScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onAddMenuClick = {
                                    navController.navigate(NavRoutes.MainProduct.route) {
                                        popUpTo(NavRoutes.OrderProduct.route)
                                    }
                                },
                                onEditItemClick = { productId, productName, cartItemIds ->
                                    // Navigate to ProductDetailScreen for editing specific cart item
                                    val targetCartItemId = cartItemIds.firstOrNull()
                                    navController.navigate(NavRoutes.productDetail(productId, targetCartItemId))
                                },
                                onDiscountClick = {
                                    val subtotal = orderProductViewModel.calculateSubtotal()
                                    navController.navigate(NavRoutes.discount(subtotal))
                                },
                                onPlaceOrderClick = { totalAmount, subtotal, discount ->
                                    navController.navigate(NavRoutes.cashPayment(totalAmount, subtotal, discount))
                                },
                                onOrderSuccess = { totalAmount ->
                                    // Navigate to order summary on success (for TRANSFER payment)
                                    navController.navigate(NavRoutes.orderSummary(totalAmount)) {
                                        popUpTo(NavRoutes.OrderProduct.route) {
                                            inclusive = true
                                        }
                                    }
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
                            // Get the same ViewModel instance from the parent route (OrderProduct)
                            // Use the parent backStackEntry to get the same ViewModel instance
                            val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                                navController.getBackStackEntry(NavRoutes.OrderProduct.route)
                            }
                            val orderProductViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.indybrain.indypos_Android.presentation.orderproduct.OrderProductViewModel>(
                                parentEntry
                            )
                            
                            DiscountScreen(
                                subtotal = subtotal,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onCancel = {
                                    navController.popBackStack()
                                },
                                onDiscountSelected = { discount ->
                                    orderProductViewModel.setDiscount(discount, subtotal)
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
                                    // Navigate to order summary screen
                                    navController.navigate(NavRoutes.orderSummary(change)) {
                                        popUpTo(NavRoutes.OrderProduct.route) {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }
                        
                        composable(
                            route = NavRoutes.ORDER_SUMMARY_ROUTE,
                            arguments = listOf(navArgument("totalAmount") {})
                        ) { backStackEntry ->
                            val totalAmountString = backStackEntry.arguments?.getString("totalAmount") ?: "0.0"
                            val totalAmount = totalAmountString.toDoubleOrNull() ?: 0.0
                            
                            com.indybrain.indypos_Android.presentation.ordersummary.OrderSummaryScreen(
                                totalAmount = totalAmount,
                                onAddOrderClick = {
                                    // Navigate back to MainProduct
                                    navController.navigate(NavRoutes.MainProduct.route) {
                                        // Pop all back stack until we reach MainProduct or Home
                                        popUpTo(NavRoutes.Home.route) {
                                            inclusive = false
                                        }
                                    }
                                }
                            )
                        }
                        
                        composable(
                            route = NavRoutes.ORDER_DETAIL_ROUTE,
                            arguments = listOf(navArgument("orderId") {})
                        ) { backStackEntry ->
                            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                            OrderDetailScreen(
                                orderId = orderId,
                                onBackClick = {
                                    navController.popBackStack()
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
                                    // Go to ProductDetail in "new item" mode (no pre-filled cart data)
                                    navController.navigate(NavRoutes.productDetail(productId, "new")) {
                                        popUpTo(NavRoutes.PRODUCT_EDIT_ROUTE)
                                    }
                                },
                                onUpdateBasket = {
                                    navController.popBackStack()
                                },
                                onEditClick = { editProductId, cartItemId ->
                                    navController.navigate(NavRoutes.productDetail(editProductId, cartItemId)) {
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
                        
                        composable(NavRoutes.StockManagement.route) {
                            StockManagementScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(NavRoutes.DataManagement.route) {
                            DataManagementScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(NavRoutes.ContactUs.route) {
                            ContactUsScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(NavRoutes.ReceiptSettings.route) {
                            com.indybrain.indypos_Android.presentation.settings.receipt.ReceiptSettingsScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(NavRoutes.PrinterSettings.route) {
                            PrinterSettingsScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onNavigateToBluetoothScan = {
                                    navController.navigate(NavRoutes.BluetoothPrinterScan.route)
                                }
                            )
                        }
                        
                        composable(NavRoutes.BluetoothPrinterScan.route) {
                            BluetoothPrinterScanScreen(
                                onBackClick = {
                                    navController.popBackStack()
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
                                    scannedBarcodeForProduct = null // Clear barcode when leaving
                                    navController.popBackStack()
                                },
                                onSaveSuccess = {
                                    scannedBarcodeForProduct = null // Clear barcode after save
                                    navController.popBackStack()
                                },
                                onBarcodeScannerClick = {
                                    scannedBarcodeForProduct = null // Clear previous barcode
                                    navController.navigate(NavRoutes.BarcodeScanner.route)
                                },
                                scannedBarcode = scannedBarcodeForProduct
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
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Mark that we received a new intent so lifecycle observer knows to process it
        hasNewIntent = true
        // The deep link will be processed by the lifecycle observer
        // when it detects the intent URI has changed
    }

    private fun handleDeepLink(
        intent: Intent?, 
        navController: androidx.navigation.NavController,
        coroutineScope: kotlinx.coroutines.CoroutineScope
    ) {
        val data: Uri? = intent?.data
        if (data != null) {
            val scheme = data.scheme
            val host = data.host
            val path = data.path
            
            // Handle reset password deep link
            // Format: https://indy-pos.com/reset-password?token=xxx
            // or: https://dev.indy-pos.com/reset-password?token=xxx
            // or: https://stg.indy-pos.com/reset-password?token=xxx
            if (scheme == "https" && 
                (host == "indy-pos.com" || host == "dev.indy-pos.com" || host == "stg.indy-pos.com") &&
                path?.contains("reset-password") == true) {
                
                val token = data.getQueryParameter("token")
                if (token != null && token.isNotEmpty()) {
                    // Navigate to reset password screen with token
                    // Use a small delay to ensure UI is ready
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(500)
                        // Always navigate to reset password screen, even if already there
                        // This allows handling the same link clicked again
                        val resetPasswordRoute = NavRoutes.resetPassword(token)
                        
                        // Navigate to reset password screen
                        // Clear back stack to Login so user can go back to login
                        navController.navigate(resetPasswordRoute) {
                            // Pop to Login if it exists, otherwise clear all
                            val loginRoute = NavRoutes.Login.route
                            if (navController.graph.findNode(loginRoute) != null) {
                                popUpTo(loginRoute) {
                                    inclusive = false
                                }
                            } else {
                                // If Login route doesn't exist in graph, clear all
                                popUpTo(0) {
                                    inclusive = true
                                }
                            }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }
}