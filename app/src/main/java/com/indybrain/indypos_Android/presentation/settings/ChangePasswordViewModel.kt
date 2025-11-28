package com.indybrain.indypos_Android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Change Password screen
 */
@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()
    
    fun updateOldPassword(password: String) {
        _uiState.update { 
            it.copy(
                oldPassword = password,
                isFormValid = validateForm(
                    oldPassword = password,
                    newPassword = it.newPassword,
                    confirmPassword = it.confirmPassword
                )
            )
        }
    }
    
    fun updateNewPassword(password: String) {
        _uiState.update { 
            it.copy(
                newPassword = password,
                isFormValid = validateForm(
                    oldPassword = it.oldPassword,
                    newPassword = password,
                    confirmPassword = it.confirmPassword
                )
            )
        }
    }
    
    fun updateConfirmPassword(password: String) {
        _uiState.update { 
            it.copy(
                confirmPassword = password,
                isFormValid = validateForm(
                    oldPassword = it.oldPassword,
                    newPassword = it.newPassword,
                    confirmPassword = password
                )
            )
        }
    }
    
    fun toggleOldPasswordVisibility() {
        _uiState.update { it.copy(isOldPasswordVisible = !it.isOldPasswordVisible) }
    }
    
    fun toggleNewPasswordVisibility() {
        _uiState.update { it.copy(isNewPasswordVisible = !it.isNewPasswordVisible) }
    }
    
    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }
    
    fun changePassword() {
        val state = _uiState.value
        if (!state.isFormValid) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = authRepository.changePassword(
                oldPassword = state.oldPassword,
                newPassword = state.newPassword
            )
            
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        showSuccessDialog = true,
                        isSuccess = true
                    )
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการเปลี่ยนรหัสผ่าน"
                    )
                }
            }
        }
    }
    
    fun dismissSuccessDialog() {
        _uiState.update { 
            it.copy(
                showSuccessDialog = false,
                isSuccess = true // Trigger navigation
            )
        }
    }
    
    fun resetSuccess() {
        _uiState.update { 
            it.copy(
                isSuccess = false,
                oldPassword = "",
                newPassword = "",
                confirmPassword = ""
            )
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    private fun validateForm(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Boolean {
        return oldPassword.isNotBlank() &&
                newPassword.isNotBlank() &&
                confirmPassword.isNotBlank() &&
                newPassword.length >= 6 &&
                newPassword == confirmPassword
    }
}

/**
 * UI State for Change Password screen
 */
data class ChangePasswordUiState(
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isOldPasswordVisible: Boolean = false,
    val isNewPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isFormValid: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

