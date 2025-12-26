package com.indybrain.indypos_Android.presentation.register

import com.indybrain.indypos_Android.domain.model.User

/**
 * MVI State for Register screen
 */
sealed class RegisterState {
    /**
     * Initial/Idle state
     */
    data object Idle : RegisterState()
    
    /**
     * Loading state
     */
    data object Loading : RegisterState()
    
    /**
     * Success state with user data
     */
    data class Success(val user: User) : RegisterState()
    
    /**
     * Error state with error message
     */
    data class Error(val message: String) : RegisterState()
}

/**
 * UI State for Register screen (contains form fields)
 */
data class RegisterUiState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val gender: String = "",
    val birthDate: String = "",
    val shopName: String = "",
    val shopDescription: String = "",
    val termsAccepted: Boolean = false,
    val privacyAccepted: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistrationSuccess: Boolean = false,
    val successMessage: String? = null,
    val user: com.indybrain.indypos_Android.domain.model.User? = null
)

