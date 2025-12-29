package com.indybrain.indypos_Android.presentation.forgotpassword

/**
 * MVI State for Forgot Password screen
 */
sealed class ForgotPasswordState {
    /**
     * Initial/Idle state
     */
    data object Idle : ForgotPasswordState()
    
    /**
     * Loading state
     */
    data object Loading : ForgotPasswordState()
    
    /**
     * Success state
     */
    data class Success(val message: String) : ForgotPasswordState()
    
    /**
     * Error state with error message
     */
    data class Error(val message: String) : ForgotPasswordState()
}

/**
 * UI State for Forgot Password screen
 */
data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSuccess: Boolean = false
)

