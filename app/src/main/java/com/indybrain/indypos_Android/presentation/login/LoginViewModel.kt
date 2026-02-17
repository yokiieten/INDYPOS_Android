package com.indybrain.indypos_Android.presentation.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.domain.model.LoginRequest
import com.indybrain.indypos_Android.domain.usecase.LoginUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

/**
 * ViewModel for Login screen implementing MVI pattern with StateFlow
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val languageLocalDataSource: LanguageLocalDataSource,
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    /** Returns string in the user's selected language (respects language change in Settings) */
    private fun getLocalizedString(resId: Int): String {
        val localeCode = languageLocalDataSource.getLanguageLocale()
        val localizedContext = LocaleHelper.setLocale(context, localeCode)
        return localizedContext.getString(resId)
    }
    
    // UI State Flow
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    // MVI State Flow
    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state.asStateFlow()
    
    /**
     * Handle intents/actions from UI
     */
    fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateEmail -> {
                _uiState.update { it.copy(email = intent.email, errorMessage = null) }
            }
            
            is LoginIntent.UpdatePassword -> {
                _uiState.update { it.copy(password = intent.password, errorMessage = null) }
            }
            
            is LoginIntent.TogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
            
            is LoginIntent.Login -> {
                performLogin()
            }
            
            is LoginIntent.ClearError -> {
                _uiState.update { it.copy(errorMessage = null) }
                _state.value = LoginState.Idle
            }

            is LoginIntent.AcknowledgeSuccess -> {
                _uiState.update {
                    it.copy(
                        isLoginSuccess = false,
                        successMessage = null
                    )
                }
                _state.value = LoginState.Idle
            }
            
            is LoginIntent.ShowUnauthorizedError -> {
                // Show unauthorized error message (session expired)
                val message = getLocalizedString(R.string.api_error_unauthorized)
                _uiState.update {
                    it.copy(errorMessage = message)
                }
                _state.value = LoginState.Error(message)
            }
        }
    }
    
    /**
     * Perform login operation
     */
    private fun performLogin() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        
        if (email.isBlank() || password.isBlank()) {
            val message = getLocalizedString(R.string.login_error_fields_required)
            _uiState.update {
                it.copy(
                    errorMessage = message,
                    isLoading = false
                )
            }
            _state.value = LoginState.Error(message)
            return
        }
        
        // Update state to loading
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        _state.value = LoginState.Loading
        
        viewModelScope.launch {
            try {
                val request = LoginRequest(email = email, password = password)
                loginUseCase(request)
                    .onSuccess { user ->
                        // Login success - navigate to home immediately (no 4 API calls here)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoginSuccess = true,
                                successMessage = null,
                                user = user,
                                errorMessage = null
                            )
                        }
                        _state.value = LoginState.Success(user)
                    }
                    .onFailure { exception ->
                        val rawMessage = exception.message
                            ?: exception.cause?.message
                            ?: getLocalizedString(R.string.login_error_system_error)
                        val statusCode = (exception.cause as? HttpException)?.code()
                        val errorMessage = getLocalizedLoginErrorMessage(rawMessage, statusCode)
                            ?: rawMessage
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoginSuccess = false,
                                successMessage = null,
                                errorMessage = errorMessage
                            )
                        }
                        _state.value = LoginState.Error(errorMessage)
                    }
            } catch (e: Exception) {
                // Catch any unexpected exceptions to prevent app crash
                val rawMessage = e.message ?: e.cause?.message ?: getLocalizedString(R.string.login_error_system_error)
                val statusCode = (e.cause as? HttpException)?.code() ?: (e as? HttpException)?.code()
                val errorMessage = getLocalizedLoginErrorMessage(rawMessage, statusCode) ?: rawMessage
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoginSuccess = false,
                        successMessage = null,
                        errorMessage = errorMessage
                    )
                }
                _state.value = LoginState.Error(errorMessage)
            }
        }
    }

    /**
     * Maps raw error text and HTTP status code to localized login error messages.
     * Returns null if no mapping applies (caller should use raw message as fallback).
     */
    private fun getLocalizedLoginErrorMessage(
        errorText: String?,
        statusCode: Int?
    ): String? {
        // Check status code first - 401 typically means invalid credentials
        if (statusCode == 401) {
            return getLocalizedString(R.string.login_error_invalid_credentials)
        }

        // Check for connection/network errors
        val error = errorText?.lowercase() ?: return null
        if (error.contains("unable to resolve host") ||
            error.contains("timeout") ||
            error.contains("no address associated with hostname") ||
            error.contains("connection refused") ||
            error.contains("network is unreachable") ||
            error.contains("connection") ||
            error.contains("network")
        ) {
            return getLocalizedString(R.string.login_error_connection_failed)
        }

        // Check for various patterns of "invalid email or password" error
        if (error.contains("invalid") && error.contains("email") && error.contains("password")) {
            return getLocalizedString(R.string.login_error_invalid_credentials)
        }
        if (error.contains("invalid") && (error.contains("email") || error.contains("password"))) {
            return getLocalizedString(R.string.login_error_invalid_credentials)
        }
        if (error.contains("email") && error.contains("password") &&
            (error.contains("incorrect") || error.contains("wrong") || error.contains("invalid"))
        ) {
            return getLocalizedString(R.string.login_error_invalid_credentials)
        }
        if ((error.contains("email") || error.contains("password")) &&
            (error.contains("incorrect") || error.contains("wrong") || error.contains("invalid") || error.contains("not match"))
        ) {
            return getLocalizedString(R.string.login_error_invalid_credentials)
        }
        if (error.contains("invalid credentials")) {
            return getLocalizedString(R.string.login_error_invalid_credentials)
        }

        return null
    }
}

