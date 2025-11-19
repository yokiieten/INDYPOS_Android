package com.indybrain.indypos_Android.presentation.login

import com.indybrain.indypos_Android.domain.model.User

/**
 * MVI State for Login screen
 */
sealed class LoginState {
    /**
     * Initial/Idle state
     */
    data object Idle : LoginState()
    
    /**
     * Loading state
     */
    data object Loading : LoginState()
    
    /**
     * Success state with user data
     */
    data class Success(val user: User) : LoginState()
    
    /**
     * Error state with error message
     */
    data class Error(val message: String) : LoginState()
}

/**
 * UI State for Login screen (contains form fields)
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false,
    val user: User? = null
)

