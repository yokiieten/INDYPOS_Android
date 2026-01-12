package com.indybrain.indypos_Android.core.error

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.indybrain.indypos_Android.MainActivity
import com.indybrain.indypos_Android.data.local.AuthLocalDataSource
import com.indybrain.indypos_Android.data.local.database.IndyPosDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handler for 401 Unauthorized errors
 * Centralized error handling similar to iOS implementation
 */
@Singleton
class UnauthorizedErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authLocalDataSource: AuthLocalDataSource,
    private val database: IndyPosDatabase
) {
    companion object {
        private const val TAG = "UnauthorizedErrorHandler"
    }

    private var isHandling = false
    
    // Create a coroutine scope for handling async operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Handle 401 Unauthorized error
     * This method:
     * 1. Clears all user data
     * 2. Navigates to login screen
     * 3. Shows error alert
     * 
     * Similar to iOS handleUnauthorizedError()
     */
    fun handle401Error() {
        // Prevent multiple simultaneous handling
        if (isHandling) {
            Log.d(TAG, "Already handling 401 error, skipping...")
            return
        }

        isHandling = true
        Log.d(TAG, "🔒 401 Unauthorized detected - Forcing logout")

        // Launch coroutine to handle async operations
        scope.launch {
            try {
                // 1. Clear all user data
                clearAllData()

                // 2. Navigate to login screen (on main thread)
                Handler(Looper.getMainLooper()).post {
                    try {
                        navigateToLogin()
                        
                        // 3. Show error alert (with delay to ensure login screen is ready)
                        Handler(Looper.getMainLooper()).postDelayed({
                            showErrorAlert()
                            isHandling = false
                        }, 500)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error navigating to login: ${e.message}", e)
                        isHandling = false
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error handling 401: ${e.message}", e)
                isHandling = false
            }
        }
    }

    /**
     * Clear all user data (SharedPreferences and Room database)
     * This is a suspend function that must be called from a coroutine
     */
    private suspend fun clearAllData() {
        try {
            // Clear SharedPreferences (synchronous operation)
            authLocalDataSource.clearUser()
            Log.d(TAG, "✅ Cleared user data from SharedPreferences")

            // Clear Room database (suspend function)
            database.clearAllData()
            Log.d(TAG, "✅ Cleared all Room database data")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing data: ${e.message}", e)
        }
    }

    /**
     * Navigate to login screen
     * Clears all back stack and starts fresh MainActivity with login screen
     */
    private fun navigateToLogin() {
        try {
            // Save flag to SharedPreferences to show error alert on login screen
            val prefs = context.getSharedPreferences("indypos_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("show_unauthorized_error", true)
                .apply()
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                       Intent.FLAG_ACTIVITY_CLEAR_TASK or 
                       Intent.FLAG_ACTIVITY_CLEAR_TOP
                // Add extra to indicate we should show login screen
                putExtra("NAVIGATE_TO_LOGIN", true)
                putExtra("SHOW_UNAUTHORIZED_ERROR", true)
            }
            context.startActivity(intent)
            Log.d(TAG, "✅ Navigated to login screen")
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to login: ${e.message}", e)
        }
    }

    /**
     * Show error alert dialog (called from login screen)
     * Similar to iOS UIAlertController
     */
    private fun showErrorAlert() {
        try {
            Log.d(TAG, "⚠️ Error alert will be shown on login screen via flag")
            // The error alert will be shown by LoginScreen when it checks the SharedPreferences flag
        } catch (e: Exception) {
            Log.e(TAG, "Error showing alert: ${e.message}", e)
        }
    }

    /**
     * Get localized string resource
     * Fallback to English if not found
     */
    private fun getLocalizedString(key: String): String {
        return try {
            val resId = context.resources.getIdentifier(key, "string", context.packageName)
            if (resId != 0) {
                context.getString(resId)
            } else {
                // Fallback values
                when (key) {
                    "home_error_title" -> "ข้อผิดพลาด"
                    "api_error_unauthorized" -> "เซสชันหมดอายุ กรุณาเข้าสู่ระบบอีกครั้ง"
                    "home_ok" -> "ตกลง"
                    else -> key
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting localized string: ${e.message}", e)
            key
        }
    }
}
