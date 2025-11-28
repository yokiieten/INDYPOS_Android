package com.indybrain.indypos_Android.data.local

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for language/locale preference (SharedPreferences)
 * Locale codes:
 * - 1033 = English
 * - 1054 = Thai
 */
@Singleton
class LanguageLocalDataSource @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val KEY_LANGUAGE_LOCALE = "key_language_locale"
        private const val DEFAULT_LOCALE = 1054 // Thai as default
    }
    
    /**
     * Save language locale preference
     * @param localeCode Locale code (1033 for English, 1054 for Thai)
     */
    fun saveLanguageLocale(localeCode: Int) {
        sharedPreferences.edit()
            .putInt(KEY_LANGUAGE_LOCALE, localeCode)
            .apply()
    }
    
    /**
     * Get current language locale preference
     * @return Locale code (1033 for English, 1054 for Thai, default: 1054)
     */
    fun getLanguageLocale(): Int {
        return sharedPreferences.getInt(KEY_LANGUAGE_LOCALE, DEFAULT_LOCALE)
    }
    
    /**
     * Get current language option based on locale code
     * Similar to Swift function provided by user
     */
    fun getCurrentLanguageOption(): LanguageOption {
        return when (getLanguageLocale()) {
            1033 -> LanguageOption.English
            1054 -> LanguageOption.Thai
            else -> LanguageOption.Thai // Default to Thai
        }
    }
}

/**
 * Enum representing available language options
 */
enum class LanguageOption(val localeCode: Int, val displayName: String) {
    Thai(1054, "ภาษาไทย"),
    English(1033, "English")
}

