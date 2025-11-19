package com.indybrain.indypos_Android.presentation.login

/**
 * MVI Intent/Action for Login screen
 * Represents user actions/events
 */
sealed class LoginIntent {
    /**
     * Update email field
     */
    data class UpdateEmail(val email: String) : LoginIntent()
    
    /**
     * Update password field
     */
    data class UpdatePassword(val password: String) : LoginIntent()
    
    /**
     * Perform login action
     */
    data object Login : LoginIntent()
    
    /**
     * Clear error message
     */
    data object ClearError : LoginIntent()
}

