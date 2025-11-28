package com.indybrain.indypos_Android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    /**
     * Handle logout button click
     * Checks internet connectivity first, then shows appropriate dialog
     */
    fun onLogoutClick() {
        if (!networkConnectivityChecker.isConnected()) {
            // No internet connection - show no internet dialog
            _uiState.update { it.copy(showNoInternetDialog = true) }
        } else {
            // Has internet - show confirmation dialog
            _uiState.update { it.copy(showLogoutConfirmationDialog = true) }
        }
    }
    
    /**
     * Confirm logout action
     */
    fun confirmLogout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, showLogoutConfirmationDialog = false) }
            
            val result = authRepository.logout()
            result.onSuccess {
                // Show success dialog instead of navigating immediately
                _uiState.update { 
                    it.copy(
                        isLoggingOut = false, 
                        showLogoutSuccessDialog = true
                    )
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoggingOut = false,
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการออกจากระบบ"
                    )
                }
            }
        }
    }
    
    /**
     * Cancel logout
     */
    fun cancelLogout() {
        _uiState.update { it.copy(showLogoutConfirmationDialog = false) }
    }
    
    /**
     * Dismiss no internet dialog
     */
    fun dismissNoInternetDialog() {
        _uiState.update { it.copy(showNoInternetDialog = false) }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Handle success dialog OK button click
     * This will trigger navigation to login screen
     */
    fun onLogoutSuccessDialogOk() {
        _uiState.update { 
            it.copy(
                showLogoutSuccessDialog = false,
                isLogoutSuccess = true
            )
        }
    }
    
    /**
     * Reset logout success state (after navigation)
     */
    fun resetLogoutSuccess() {
        _uiState.update { it.copy(isLogoutSuccess = false) }
    }
}

/**
 * UI State for Settings screen
 */
data class SettingsUiState(
    val showLogoutConfirmationDialog: Boolean = false,
    val showNoInternetDialog: Boolean = false,
    val showLogoutSuccessDialog: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isLogoutSuccess: Boolean = false,
    val errorMessage: String? = null
)

