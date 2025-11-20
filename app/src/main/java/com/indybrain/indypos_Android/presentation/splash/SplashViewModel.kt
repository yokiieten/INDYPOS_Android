package com.indybrain.indypos_Android.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Splash Screen
 * Checks if user is logged in and navigates accordingly
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            // เพิ่ม delay เล็กน้อยเพื่อให้เห็น splash screen
            delay(1500)
            
            try {
                val isLoggedIn = authRepository.isLoggedIn()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = isLoggedIn
                )
            } catch (e: Exception) {
                // ถ้าเกิด error ให้ไปหน้า login
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = false
                )
            }
        }
    }
}

