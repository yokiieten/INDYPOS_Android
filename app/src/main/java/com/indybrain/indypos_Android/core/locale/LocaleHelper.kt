package com.indybrain.indypos_Android.core.locale

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale

/**
 * Helper class for managing app locale
 */
object LocaleHelper {
    
    /**
     * Get locale from locale code
     * 1033 = English (en)
     * 1054 = Thai (th)
     */
    fun getLocaleFromCode(localeCode: Int): Locale {
        return when (localeCode) {
            1033 -> Locale("en", "US") // English
            1054 -> Locale("th", "TH") // Thai
            else -> Locale("th", "TH") // Default to Thai
        }
    }
    
    /**
     * Get locale code from locale
     */
    fun getLocaleCode(locale: Locale): Int {
        return when (locale.language) {
            "en" -> 1033
            "th" -> 1054
            else -> 1054 // Default to Thai
        }
    }
    
    /**
     * Set locale for context
     */
    fun setLocale(context: Context, localeCode: Int): Context {
        val locale = getLocaleFromCode(localeCode)
        return updateResources(context, locale)
    }
    
    /**
     * Update resources with new locale
     */
    private fun updateResources(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            return context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }
    
    /**
     * Get current locale from context
     */
    fun getCurrentLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }
}

