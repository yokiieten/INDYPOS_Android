package com.indybrain.indypos_Android

import android.app.Application
import android.content.Context
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class with Hilt setup
 */
@HiltAndroidApp
class IndyPosApplication : Application() {
    
    override fun attachBaseContext(base: Context) {
        // Get saved locale or use default
        val localeCode = try {
            // We can't inject here, so we'll use SharedPreferences directly
            val prefs = base.getSharedPreferences("indypos_prefs", Context.MODE_PRIVATE)
            prefs.getInt("key_language_locale", 1054) // Default to Thai
        } catch (e: Exception) {
            1054 // Default to Thai
        }
        
        val context = LocaleHelper.setLocale(base, localeCode)
        super.attachBaseContext(context)
    }
    
    override fun onCreate() {
        super.onCreate()
        // Initialize locale on app start
        val localeCode = try {
            val prefs = getSharedPreferences("indypos_prefs", Context.MODE_PRIVATE)
            prefs.getInt("key_language_locale", 1054)
        } catch (e: Exception) {
            1054
        }
        LocaleHelper.setLocale(this, localeCode)
    }
}

