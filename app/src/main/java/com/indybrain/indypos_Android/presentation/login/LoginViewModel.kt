package com.indybrain.indypos_Android.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.model.LoginRequest
import com.indybrain.indypos_Android.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Login screen implementing MVI pattern with StateFlow
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {
    
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
        }
    }
    
    /**
     * Perform login operation
     */
    private fun performLogin() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { 
                it.copy(
                    errorMessage = "Please fill in all fields",
                    isLoading = false
                )
            }
            _state.value = LoginState.Error("Please fill in all fields")
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
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoginSuccess = true,
                                successMessage = "เข้าสู่ระบบสำเร็จ",
                                user = user,
                                errorMessage = null
                            )
                        }
                        _state.value = LoginState.Success(user)
                    }
                    .onFailure { exception ->
                        val errorMessage = when {
                            exception.message != null -> exception.message!!
                            exception.cause?.message != null -> exception.cause!!.message!!
                            else -> "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง"
                        }
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
                val errorMessage = when {
                    e.message != null -> e.message!!
                    e.cause?.message != null -> e.cause!!.message!!
                    else -> "เกิดข้อผิดพลาดที่ไม่คาดคิด กรุณาลองใหม่อีกครั้ง"
                }
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
}

