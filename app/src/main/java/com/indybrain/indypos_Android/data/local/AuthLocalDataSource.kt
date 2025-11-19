package com.indybrain.indypos_Android.data.local

import android.content.SharedPreferences
import com.google.gson.Gson
import com.indybrain.indypos_Android.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for authentication data (SharedPreferences)
 */
@Singleton
class AuthLocalDataSource @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) {
    companion object {
        private const val KEY_USER = "key_user"
        private const val KEY_TOKEN = "key_token"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
    }
    
    /**
     * Save user data
     */
    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        sharedPreferences.edit()
            .putString(KEY_USER, userJson)
            .putString(KEY_TOKEN, user.token)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }
    
    /**
     * Get current user
     */
    fun getUser(): User? {
        val userJson = sharedPreferences.getString(KEY_USER, null)
        return if (userJson != null) {
            gson.fromJson(userJson, User::class.java)
        } else {
            null
        }
    }
    
    /**
     * Get auth token
     */
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    /**
     * Clear user data (logout)
     */
    fun clearUser() {
        sharedPreferences.edit()
            .remove(KEY_USER)
            .remove(KEY_TOKEN)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }
}

