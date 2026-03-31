package com.indybrain.indypos_Android

import android.app.Application
import android.content.Context
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import dagger.hilt.android.HiltAndroidApp
import net.posprinter.POSConnect

/**
 * Application class with Hilt setup
 */
@HiltAndroidApp
class IndyPosApplication : Application() {
    
    override fun attachBaseContext(base: Context) {
        val localeCode = LanguageLocalDataSource.readSavedLocaleCode(base)
        val context = LocaleHelper.setLocale(base, localeCode)
        super.attachBaseContext(context)
    }
    
    override fun onCreate() {
        super.onCreate()
        val localeCode = LanguageLocalDataSource.readSavedLocaleCode(this)
        LocaleHelper.setLocale(this, localeCode)
        
        // Initialize POS Printer SDK
        POSConnect.init(this)
    }
}

