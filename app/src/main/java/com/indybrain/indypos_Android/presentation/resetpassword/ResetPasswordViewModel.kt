package com.indybrain.indypos_Android.presentation.resetpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import com.indybrain.indypos_Android.domain.usecase.ResetPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * In-memory cache for verified reset-password tokens.
 * Survives ViewModel/Activity recreation (e.g. screen rotation) to prevent duplicate API calls.
 */
private object ResetPasswordVerifyCache {
    private val verifiedTokens = mutableMapOf<String, Int?>() // token -> userId
    private val verifyingTokens = mutableSetOf<String>()
    private val resetCompletedTokens = mutableSetOf<String>() // token already had reset success

    @Synchronized
    fun isVerified(token: String): Boolean = token in verifiedTokens

    @Synchronized
    fun getUserId(token: String): Int? = verifiedTokens[token]

    @Synchronized
    fun isVerifying(token: String): Boolean = token in verifyingTokens

    @Synchronized
    fun isResetCompleted(token: String): Boolean = token in resetCompletedTokens

    @Synchronized
    fun markVerifying(token: String) { verifyingTokens.add(token) }

    @Synchronized
    fun markVerified(token: String, userId: Int?) {
        verifyingTokens.remove(token)
        verifiedTokens[token] = userId
    }

    @Synchronized
    fun markVerifyFailed(token: String) {
        verifyingTokens.remove(token)
    }

    @Synchronized
    fun markResetCompleted(token: String) {
        verifiedTokens.remove(token)
        verifyingTokens.remove(token)
        resetCompletedTokens.add(token)
    }
}

/**
 * ViewModel for Reset Password screen implementing MVI pattern with StateFlow
 */
@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    // UI State Flow
    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()
    
    // MVI State Flow
    private val _state = MutableStateFlow<ResetPasswordState>(ResetPasswordState.Idle)
    val state: StateFlow<ResetPasswordState> = _state.asStateFlow()
    
    // Token from deep link
    private var resetToken: String? = null
    
    fun setTokenAndVerify(token: String) {
        if (token.isBlank()) return
        resetToken = token

        // Use cache to prevent duplicate API calls (survives ViewModel recreation on rotation)
        when {
            ResetPasswordVerifyCache.isResetCompleted(token) -> {
                // Reset already succeeded - skip API, navigate to login without showing popup again
                _uiState.update {
                    it.copy(
                        isVerifyingToken = false,
                        isTokenVerified = true,
                        isLoading = false,
                        errorMessage = null,
                        shouldNavigateToLoginOnRestore = true
                    )
                }
                return
            }
            ResetPasswordVerifyCache.isVerified(token) -> {
                val userId = ResetPasswordVerifyCache.getUserId(token)
                _uiState.update {
                    it.copy(
                        isVerifyingToken = false,
                        isTokenVerified = true,
                        userId = userId,
                        errorMessage = null
                    )
                }
                return
            }
            ResetPasswordVerifyCache.isVerifying(token) -> return
            _uiState.value.isSuccess -> return
            else -> verifyToken(token)
        }
    }
    
    private fun verifyToken(token: String) {
        ResetPasswordVerifyCache.markVerifying(token)
        _uiState.update { it.copy(isVerifyingToken = true, errorMessage = null) }

        viewModelScope.launch {
            val isLoggedIn = authRepository.isLoggedIn()
            try {
                authRepository.verifyResetPasswordToken(token)
                    .onSuccess { userId ->
                        if (isLoggedIn) {
                            ResetPasswordVerifyCache.markVerifyFailed(token)
                            // Req 3: Logged in + valid token -> don't show Reset Password, navigate back
                            _uiState.update {
                                it.copy(
                                    isVerifyingToken = false,
                                    isTokenVerified = false,
                                    shouldNavigateBack = true,
                                    errorMessage = null
                                )
                            }
                        } else {
                            ResetPasswordVerifyCache.markVerified(token, userId)
                            _uiState.update {
                                it.copy(
                                    isVerifyingToken = false,
                                    isTokenVerified = true,
                                    userId = userId,
                                    errorMessage = null
                                )
                            }
                        }
                    }
                    .onFailure {
                        ResetPasswordVerifyCache.markVerifyFailed(token)
                        if (isLoggedIn) {
                            // Req 2: Logged in + expired token -> no popup, navigate back silently
                            _uiState.update {
                                it.copy(
                                    isVerifyingToken = false,
                                    isTokenVerified = false,
                                    shouldNavigateBack = true,
                                    errorMessage = null
                                )
                            }
                        } else {
                            // Req 1: Not logged in + expired token -> show popup with localized text
                            val errorMessage = "reset_password_error_token_expired"
                            _uiState.update {
                                it.copy(
                                    isVerifyingToken = false,
                                    isTokenVerified = false,
                                    errorMessage = errorMessage,
                                    shouldNavigateBack = true
                                )
                            }
                            _state.value = ResetPasswordState.Error(errorMessage)
                        }
                    }
            } catch (e: Exception) {
                ResetPasswordVerifyCache.markVerifyFailed(token)
                if (isLoggedIn) {
                    // Req 2: Logged in + error -> no popup, navigate back silently
                    _uiState.update {
                        it.copy(
                            isVerifyingToken = false,
                            isTokenVerified = false,
                            shouldNavigateBack = true,
                            errorMessage = null
                        )
                    }
                } else {
                    // Req 1: Not logged in + error -> show popup
                    val errorMessage = "reset_password_error_token_expired"
                    _uiState.update {
                        it.copy(
                            isVerifyingToken = false,
                            isTokenVerified = false,
                            errorMessage = errorMessage,
                            shouldNavigateBack = true
                        )
                    }
                    _state.value = ResetPasswordState.Error(errorMessage)
                }
            }
        }
    }
    
    /**
     * Handle intents/actions from UI
     */
    fun handleIntent(intent: ResetPasswordIntent) {
        when (intent) {
            is ResetPasswordIntent.UpdateNewPassword -> {
                _uiState.update { it.copy(newPassword = intent.password, errorMessage = null) }
            }
            is ResetPasswordIntent.UpdateConfirmPassword -> {
                _uiState.update { it.copy(confirmPassword = intent.password, errorMessage = null) }
            }
            is ResetPasswordIntent.ToggleNewPasswordVisibility -> {
                _uiState.update { it.copy(isNewPasswordVisible = !it.isNewPasswordVisible) }
            }
            is ResetPasswordIntent.ToggleConfirmPasswordVisibility -> {
                _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
            }
            is ResetPasswordIntent.Submit -> {
                performSubmit()
            }
            is ResetPasswordIntent.ClearError -> {
                _uiState.update { 
                    it.copy(
                        errorMessage = null,
                        successMessage = null,
                        isSuccess = false,
                        shouldNavigateBack = false,
                        shouldNavigateToLoginOnRestore = false
                    ) 
                }
                _state.value = ResetPasswordState.Idle
            }
        }
    }
    
    /**
     * Perform submit operation
     */
    private fun performSubmit() {
        // Check if token is verified
        if (!_uiState.value.isTokenVerified) {
            val errorMessage = "reset_password_error_token_not_verified"
            _uiState.update { 
                it.copy(
                    errorMessage = errorMessage,
                    isLoading = false
                )
            }
            _state.value = ResetPasswordState.Error(errorMessage)
            return
        }
        
        val validation = validateInput()
        if (!validation.isValid) {
            _uiState.update { 
                it.copy(
                    errorMessage = validation.message,
                    isLoading = false
                )
            }
            _state.value = ResetPasswordState.Error(validation.message ?: "Invalid data")
            return
        }
        
        if (resetToken == null) {
            val errorMessage = "reset_password_error_token_missing"
            _uiState.update { 
                it.copy(
                    errorMessage = errorMessage,
                    isLoading = false
                )
            }
            _state.value = ResetPasswordState.Error(errorMessage)
            return
        }
        
        // Update state to loading
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        _state.value = ResetPasswordState.Loading
        
        viewModelScope.launch {
            try {
                val token = resetToken!!
                resetPasswordUseCase(token, _uiState.value.newPassword.trim())
                    .onSuccess {
                        ResetPasswordVerifyCache.markResetCompleted(token)
                        val successMessage = "reset_password_success_message"
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = true,
                                successMessage = successMessage,
                                errorMessage = null
                            )
                        }
                        _state.value = ResetPasswordState.Success(successMessage)
                    }
                    .onFailure { exception ->
                        val errorMessage = when {
                            exception.message != null -> exception.message!!
                            exception.cause?.message != null -> exception.cause!!.message!!
                            else -> "reset_password_error_generic"
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = false,
                                successMessage = null,
                                errorMessage = errorMessage
                            )
                        }
                        _state.value = ResetPasswordState.Error(errorMessage)
                    }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message != null -> e.message!!
                    e.cause?.message != null -> e.cause!!.message!!
                    else -> "reset_password_error_generic"
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        successMessage = null,
                        errorMessage = errorMessage
                    )
                }
                _state.value = ResetPasswordState.Error(errorMessage)
            }
        }
    }
    
    /**
     * Validate input fields
     */
    private fun validateInput(): ValidationResult {
        val state = _uiState.value
        
        // Validate new password
        val trimmedPassword = state.newPassword.trim()
        if (trimmedPassword.isEmpty()) {
            return ValidationResult(false, "reset_password_error_password_required")
        }
        if (trimmedPassword.length < 6) {
            return ValidationResult(false, "reset_password_error_password_too_short")
        }
        
        // Validate confirm password
        val trimmedConfirmPassword = state.confirmPassword.trim()
        if (trimmedConfirmPassword.isEmpty()) {
            return ValidationResult(false, "reset_password_error_confirm_password_required")
        }
        
        // Check if passwords match
        if (trimmedPassword != trimmedConfirmPassword) {
            return ValidationResult(false, "reset_password_error_passwords_not_match")
        }
        
        return ValidationResult(true, null)
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val message: String? // String resource key
    )
}

