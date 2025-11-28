package com.indybrain.indypos_Android

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.indybrain.indypos_Android.presentation.settings.LanguageSettingsScreen
import com.indybrain.indypos_Android.presentation.settings.AccountScreen
import com.indybrain.indypos_Android.presentation.splash.SplashScreen
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
                                }
                            )
                        }
                        
                        composable(NavRoutes.MainProduct.route) {
                            MainProductScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onProductClick = { productId ->
                                    navController.navigate(NavRoutes.productDetail(productId))
                                },
                                onCartClick = {
                                    navController.navigate(NavRoutes.OrderProduct.route)
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
                            OrderProductScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onAddMenuClick = {
                                    navController.navigate(NavRoutes.MainProduct.route) {
                                        popUpTo(NavRoutes.OrderProduct.route)
                                    }
                                },
                                onEditItemClick = { itemId ->
                                    // TODO: Navigate to edit item screen or product detail
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
                    }
                }
            }
        }
    }
}