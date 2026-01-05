package com.indybrain.indypos_Android.presentation.resetpassword

/**
 * MVI State for Reset Password screen
 */
sealed class ResetPasswordState {
    /**
     * Initial/Idle state
     */
    data object Idle : ResetPasswordState()
    
    /**
     * Loading state
     */
    data object Loading : ResetPasswordState()
    
    /**
     * Success state
     */
    data class Success(val message: String) : ResetPasswordState()
    
    /**
     * Error state with error message
     */
    data class Error(val message: String) : ResetPasswordState()
}

/**
 * UI State for Reset Password screen
 */
data class ResetPasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isNewPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isVerifyingToken: Boolean = true,
    val isTokenVerified: Boolean = false,
    val userId: Int? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSuccess: Boolean = false,
    val shouldNavigateBack: Boolean = false
)

