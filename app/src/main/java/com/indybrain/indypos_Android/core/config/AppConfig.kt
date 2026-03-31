package com.indybrain.indypos_Android.core.config

import com.indybrain.indypos_Android.BuildConfig

/**
 * Application configuration utility class
 * Provides access to environment-specific configuration values from BuildConfig
 */
object AppConfig {
    /**
     * Base API URL for Retrofit endpoints
     */
    val baseApiUrl: String
        get() = BuildConfig.BASE_API_URL
    
    /**
     * Base URL for image resources (without /api/v1/)
     */
    val baseImageUrl: String
        get() = BuildConfig.BASE_IMAGE_URL
    
    /**
     * Environment name for debugging purposes
     */
    val environmentName: String
        get() = BuildConfig.ENVIRONMENT_NAME
    
    /**
     * Backoffice web origin only (no path); `/login` is appended in code.
     */
    val backofficeBaseUrl: String
        get() = BuildConfig.BACKOFFICE_BASE_URL
    
    /**
     * Full URL to Backoffice login (base from flavor + `/login`).
     */
    val backofficeLoginUrl: String
        get() = "${backofficeBaseUrl.trimEnd('/')}/login"
    
    /**
     * Builds a complete image URL from a relative image path
     * 
     * @param imagePath The relative image path (e.g., "/api/v1/files/product-images/image.jpg" or "image.jpg")
     * @return Complete image URL
     */
    fun buildImageUrl(imagePath: String): String {
        // Check if it's already an absolute URL
        if (imagePath.contains("://")) {
            return imagePath
        }
        
        // Ensure path starts with a single leading slash
        var path = imagePath
        if (!path.startsWith("/")) {
            path = "/$path"
        }
        
        // If path is already under the expected product-images route
        if (path.startsWith("/api/v1/files/product-images/")) {
            return "$baseImageUrl$path"
        }
        
        // Treat as a bare filename; place it under the product-images folder
        val cleanFile = if (path.startsWith("/")) {
            path.drop(1)
        } else {
            path
        }
        
        return "$baseImageUrl/api/v1/files/product-images/$cleanFile"
    }
}

