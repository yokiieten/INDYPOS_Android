package com.indybrain.indypos_Android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.launch
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.domain.repository.AuthRepository
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
import com.indybrain.indypos_Android.core.network.NetworkMonitor
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import javax.inject.Singleton
import javax.inject.Inject

/**
 * CompositionLocal exposing global online/offline state to all composables.
 *
 * Default is `true` so preview/unspecified cases behave as online.
 */
val LocalIsOnline = staticCompositionLocalOf { true }

/**
 * CompositionLocal for triggering UI recomposition when locale changes
 */
val LocalLocaleVersion = staticCompositionLocalOf { 0 }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var languageLocalDataSource: LanguageLocalDataSource
    
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var networkMonitor: NetworkMonitor
    
    // Flag to track if we received a new intent from onNewIntent
    private var hasNewIntent = false
    
    // Splash screen instance
    private var splashScreen: androidx.core.splashscreen.SplashScreen? = null
    
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
        // Install Splash Screen before super.onCreate() for Android 12+
        splashScreen = installSplashScreen()
        // ไม่ต้องบังคับให้ Splash ค้างตลอด ปล่อยให้ระบบจัดการเอง
        splashScreen?.setKeepOnScreenCondition { false }
        
        super.onCreate(savedInstanceState)
        
        // For Android 8-10, ensure locale is properly applied after onCreate
        if (android.os.Build.VERSION.SDK_INT in android.os.Build.VERSION_CODES.O..android.os.Build.VERSION_CODES.Q) {
            val localeCode = languageLocalDataSource.getLanguageLocale()
            val locale = LocaleHelper.getLocaleFromCode(localeCode)
            java.util.Locale.setDefault(locale)
        }
        
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
                    val isOnline by networkMonitor.isOnline.collectAsState()
                    var scannedBarcode by remember { mutableStateOf<String?>(null) }
                    var scannedBarcodeForProduct by remember { mutableStateOf<String?>(null) }
                    
                    // State for locale version to trigger recomposition when language changes
                    var localeVersion by remember { mutableStateOf(0) }
                    
                    // Function to update locale at runtime without recreating activity
                    val updateLocale: (Int) -> Unit = { localeCode ->
                        val locale = LocaleHelper.getLocaleFromCode(localeCode)
                        java.util.Locale.setDefault(locale)
                        
                        // Update configuration for the current context
                        val config = android.content.res.Configuration(resources.configuration)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            config.setLocale(locale)
                        } else {
                            @Suppress("DEPRECATION")
                            config.locale = locale
                        }
                        
                        // Update resources configuration
                        @Suppress("DEPRECATION")
                        resources.updateConfiguration(config, resources.displayMetrics)
                        
                        // Also update base context resources
                        val baseConfig = android.content.res.Configuration(baseContext.resources.configuration)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            baseConfig.setLocale(locale)
                        } else {
                            @Suppress("DEPRECATION")
                            baseConfig.locale = locale
                        }
                        @Suppress("DEPRECATION")
                        baseContext.resources.updateConfiguration(baseConfig, baseContext.resources.displayMetrics)
                        
                        // Trigger recomposition by updating version
                        localeVersion++
                    }
                    
                    // Control splash screen visibility
                    var isSplashScreenReady by remember { mutableStateOf(false) }
                    
                    // Close system splash screen when Compose splash screen is ready
                    LaunchedEffect(isSplashScreenReady) {
                        if (isSplashScreenReady) {
                            splashScreen?.setKeepOnScreenCondition { false }
                        }
                    }
                    
                    // Store current intent URI and timestamp to force updates
                    var currentIntentUri by remember { mutableStateOf(intent?.data?.toString()) }
                    var lastIntentTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
                    // Track the last processed intent URI (persist across config change เพื่อไม่กระพริบไป ResetPassword ตอนหมุนจอ)
                    var lastProcessedIntentUri by rememberSaveable { mutableStateOf<String?>(null) }
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
                    
                    // Track last resume time to prevent multiple calls
                    var lastResumeTime by remember { mutableStateOf(0L) }
                    
                    // Watch lifecycle to handle new intents from onNewIntent
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            // Handle deep link on resume
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
                    
                    // Separate effect for resume authentication - only on ON_RESUME
                    DisposableEffect(lifecycleOwner) {
                        val resumeObserver = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                val currentTime = System.currentTimeMillis()
                                // Prevent multiple calls within 1 second
                                if (currentTime - lastResumeTime > 1000) {
                                    lastResumeTime = currentTime
                                    resumeAuthentication(navController, coroutineScope)
                                }
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(resumeObserver)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(resumeObserver)
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
                    
                    // Track navigation intent extra
                    var navigateToExtra by remember { mutableStateOf<String?>(null) }
                    
                    // Handle navigation from notification (intent extra) - on create
                    LaunchedEffect(Unit) {
                        navigateToExtra = this@MainActivity.intent?.getStringExtra("navigate_to")
                    }
                    
                    // Watch lifecycle to handle navigation from notification when app is already open
                    DisposableEffect(lifecycleOwner) {
                        val navigationObserver = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                                // Check for navigation intent extra when app resumes
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(100)
                                    val newNavigateTo = this@MainActivity.intent?.getStringExtra("navigate_to")
                                    if (newNavigateTo != null && newNavigateTo != navigateToExtra) {
                                        navigateToExtra = newNavigateTo
                                    }
                                }
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(navigationObserver)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(navigationObserver)
                        }
                    }
                    
                    // Navigate when navigateToExtra is set
                    LaunchedEffect(navigateToExtra) {
                        if (navigateToExtra == "stock_management") {
                            // รอให้ navigation graph พร้อม
                            kotlinx.coroutines.delay(500)
                            // Navigate to Stock Management
                            try {
                                navController.navigate(NavRoutes.StockManagement.route) {
                                    // Pop to Home if it exists, otherwise clear all
                                    val homeRoute = NavRoutes.Home.route
                                    if (navController.graph.findNode(homeRoute) != null) {
                                        popUpTo(homeRoute) {
                                            inclusive = false
                                        }
                                    } else {
                                        popUpTo(0) {
                                            inclusive = true
                                        }
                                    }
                                    launchSingleTop = true
                                }
                                // Clear the extra to prevent re-navigation
                                navigateToExtra = null
                                this@MainActivity.intent?.removeExtra("navigate_to")
                            } catch (e: Exception) {
                                // Ignore navigation errors
                                navigateToExtra = null
                            }
                        }
                    }
                    CompositionLocalProvider(
                        LocalIsOnline provides isOnline,
                        LocalLocaleVersion provides localeVersion
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            NavHost(
                                navController = navController,
                                startDestination = NavRoutes.Splash.route
                            ) {
                        composable(NavRoutes.Splash.route) {
                            SplashScreen(
                                onSplashReady = { isSplashScreenReady = true },
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
                                    // วิธีที่ 2: กดกลับจาก MainProduct ให้กลับไป Home เสมอ
                                    navController.popBackStack(
                                        route = NavRoutes.Home.route,
                                        inclusive = false
                                    )
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
                                onProductManagementClick = {
                                    navController.navigate(NavRoutes.ProductManagement.route)
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
                                },
                                onLanguageChanged = { localeCode ->
                                    updateLocale(localeCode)
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

                        // Global offline overlay that blocks all interactions when there is no internet.
                        if (!isOnline) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    // Consume all clicks so underlying UI cannot be interacted with
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { /* no-op, just block */ }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ไม่มีการเชื่อมต่ออินเทอร์เน็ต",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
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
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(500)
                        // ถ้า login ค้างไว้ ไม่ navigate ไปหน้า Reset Password (ไม่มีอะไรเกิดขึ้น)
                        if (authRepository.isLoggedIn()) {
                            return@launch
                        }
                        val resetPasswordRoute = NavRoutes.resetPassword(token)
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
    
    /**
     * Resume authentication when app becomes active
     * Similar to iOS sceneDidBecomeActive
     */
    private fun resumeAuthentication(
        navController: androidx.navigation.NavController,
        coroutineScope: kotlinx.coroutines.CoroutineScope
    ) {
        coroutineScope.launch {
            // Add a small delay similar to iOS (0.33s)
            kotlinx.coroutines.delay(330)
            
            try {
                val result = authRepository.resumeAuth()
                result.onSuccess { user ->
                    // Navigate to contact admin if account is not activated
                    if (user.isActivated == false) {
                        // Navigate to ContactUs and clear back stack
                        navController.navigate(NavRoutes.ContactUs.route) {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    }
                }.onFailure {
                    // Silently fail - don't interrupt user experience
                    // This is normal if user is not logged in or has no refresh token
                }
            } catch (e: Exception) {
                // Silently fail - don't interrupt user experience
            }
        }
    }
}