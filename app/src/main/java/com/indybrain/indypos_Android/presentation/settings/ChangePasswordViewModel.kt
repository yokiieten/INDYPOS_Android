package com.indybrain.indypos_Android.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val languageLocalDataSource: LanguageLocalDataSource,
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) : ViewModel() {

    /** Returns string in the user's selected language (respects language change in Settings) */
    private fun getLocalizedString(resId: Int): String {
        val localeCode = languageLocalDataSource.getLanguageLocale()
        val localizedContext = LocaleHelper.setLocale(context, localeCode)
        return localizedContext.getString(resId)
    }
    
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
        if (!state.isFormValid) {
            _uiState.update {
                it.copy(
                    errorMessage = getValidationErrorMessage(
                        oldPassword = it.oldPassword,
                        newPassword = it.newPassword,
                        confirmPassword = it.confirmPassword
                    )
                )
            }
            return
        }
        
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
                        showSuccessDialog = true
                    )
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: getLocalizedString(R.string.settings_change_password_error_generic)
                    )
                }
            }
        }
    }
    
    fun dismissSuccessDialog() {
        _uiState.update { 
            it.copy(
                showSuccessDialog = false,
                shouldNavigateBack = true
            )
        }
    }

    fun dismissSuccessDialogWithoutNavigate() {
        _uiState.update { it.copy(showSuccessDialog = false) }
    }
    
    fun resetSuccess() {
        _uiState.update { 
            it.copy(
                shouldNavigateBack = false,
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
    
    private fun getValidationErrorMessage(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ): String {
        return when {
            oldPassword.isBlank() ->
                getLocalizedString(R.string.settings_change_password_error_old_required)
            newPassword.isBlank() ->
                getLocalizedString(R.string.settings_change_password_error_new_required)
            confirmPassword.isBlank() || newPassword != confirmPassword ->
                getLocalizedString(R.string.settings_change_password_error_mismatch)
            newPassword.length < 6 ->
                getLocalizedString(R.string.settings_change_password_error_weak)
            else ->
                getLocalizedString(R.string.settings_change_password_error_generic)
        }
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
    val shouldNavigateBack: Boolean = false,
    val errorMessage: String? = null
)






