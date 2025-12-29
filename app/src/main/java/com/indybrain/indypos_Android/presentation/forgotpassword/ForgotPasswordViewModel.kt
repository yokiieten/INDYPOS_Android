package com.indybrain.indypos_Android.presentation.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.usecase.ForgotPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Forgot Password screen implementing MVI pattern with StateFlow
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : ViewModel() {
    
    // UI State Flow
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()
    
    // MVI State Flow
    private val _state = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()
    
    /**
     * Handle intents/actions from UI
     */
    fun handleIntent(intent: ForgotPasswordIntent) {
        when (intent) {
            is ForgotPasswordIntent.UpdateEmail -> {
                _uiState.update { it.copy(email = intent.email, errorMessage = null) }
            }
            is ForgotPasswordIntent.Submit -> {
                performSubmit()
            }
            is ForgotPasswordIntent.ClearError -> {
                _uiState.update { 
                    it.copy(
                        errorMessage = null,
                        successMessage = null,
                        isSuccess = false
                    ) 
                }
                _state.value = ForgotPasswordState.Idle
            }
        }
    }
    
    /**
     * Perform submit operation
     */
    private fun performSubmit() {
        val validation = validateInput()
        if (!validation.isValid) {
            _uiState.update { 
                it.copy(
                    errorMessage = validation.message,
                    isLoading = false
                )
            }
            _state.value = ForgotPasswordState.Error(validation.message ?: "Invalid data")
            return
        }
        
        // Update state to loading
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        _state.value = ForgotPasswordState.Loading
        
        viewModelScope.launch {
            try {
                forgotPasswordUseCase(_uiState.value.email.trim())
                    .onSuccess {
                        val successMessage = "forgot_password_success_message"
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = true,
                                successMessage = successMessage,
                                errorMessage = null
                            )
                        }
                        _state.value = ForgotPasswordState.Success(successMessage)
                    }
                    .onFailure { exception ->
                        val errorMessage = when {
                            exception.message != null -> exception.message!!
                            exception.cause?.message != null -> exception.cause!!.message!!
                            else -> "forgot_password_error_generic"
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = false,
                                successMessage = null,
                                errorMessage = errorMessage
                            )
                        }
                        _state.value = ForgotPasswordState.Error(errorMessage)
                    }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message != null -> e.message!!
                    e.cause?.message != null -> e.cause!!.message!!
                    else -> "forgot_password_error_generic"
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        successMessage = null,
                        errorMessage = errorMessage
                    )
                }
                _state.value = ForgotPasswordState.Error(errorMessage)
            }
        }
    }
    
    /**
     * Validate input fields
     */
    private fun validateInput(): ValidationResult {
        val state = _uiState.value
        
        // Validate email
        val trimmedEmail = state.email.trim()
        if (trimmedEmail.isEmpty()) {
            return ValidationResult(false, "forgot_password_error_email_required")
        }
        if (!isValidEmail(trimmedEmail)) {
            return ValidationResult(false, "forgot_password_error_email_invalid")
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Validation helper function
     */
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        return emailRegex.toRegex().matches(email)
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val message: String? // String resource key
    )
}

