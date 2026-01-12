package com.indybrain.indypos_Android.data.remote.interceptor

import android.util.Log
import com.google.gson.Gson
import com.indybrain.indypos_Android.core.error.UnauthorizedErrorHandler
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Interceptor that handles 401 Unauthorized responses
 * Similar to iOS Alamofire response validation
 */
class UnauthorizedInterceptor @Inject constructor(
    private val unauthorizedErrorHandler: UnauthorizedErrorHandler,
    private val gson: Gson
) : Interceptor {

    companion object {
        private const val TAG = "UnauthorizedInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Check for 401 status code
        if (response.code == 401) {
            Log.d(TAG, "🔍 401 Unauthorized detected on ${request.url}")

            // Parse response body to check for specific error messages
            val responseBody = response.peekBody(Long.MAX_VALUE).string()
            
            try {
                // Parse JSON to get error and message
                val errorResponse = gson.fromJson(responseBody, ErrorResponse::class.java)
                val serverError = errorResponse?.error?.lowercase()
                val serverMessage = errorResponse?.message?.lowercase()

                // Skip logout if error is "invalid email or password" (login error)
                // Similar to iOS: skip logout if "Device inactive" with "Unauthorized" message
                val isLoginError = serverError?.contains("invalid email or password") == true ||
                                  serverError?.contains("invalid credentials") == true ||
                                  serverMessage?.contains("invalid email or password") == true

                if (isLoginError) {
                    Log.d(TAG, "🔒 401 Login error detected - Skipping force logout")
                    // Return the response as-is to let the repository handle it
                    return response
                }

                // For all other 401 errors, force logout
                Log.d(TAG, "🔒 401 Unauthorized (non-login) - Triggering force logout")
                unauthorizedErrorHandler.handle401Error()

            } catch (e: Exception) {
                Log.e(TAG, "Error parsing 401 response: ${e.message}", e)
                // If we can't parse, assume it's a real unauthorized error and force logout
                unauthorizedErrorHandler.handle401Error()
            }
        }

        return response
    }

    /**
     * Data class for parsing error responses
     */
    private data class ErrorResponse(
        val error: String?,
        val message: String?,
        val statusCode: Int?
    )
}
