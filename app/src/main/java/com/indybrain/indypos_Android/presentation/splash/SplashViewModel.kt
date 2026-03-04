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
import retrofit2.HttpException
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
                val wasLoggedIn = authRepository.isLoggedIn()
                if (!wasLoggedIn) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = false
                    )
                    return@launch
                }

                // มี session อยู่ - เรียก resumeAuth เพื่อ refresh token ก่อน navigate ไป Home
                // ป้องกันกรณี token หมดอายุ แล้วเรียก API อื่นก่อนจะได้ token ใหม่ (race condition)
                val result = authRepository.resumeAuth()
                result
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true
                        )
                    }
                    .onFailure { error ->
                        // Clear เฉพาะเมื่อ auth ล้มเหลว (401, ไม่มี refresh token)
                        // ถ้าเป็น network error ให้ไป Home ไปก่อน (token อาจยังใช้ได้)
                        val isAuthFailure = error is HttpException && error.code() == 401 ||
                            error.message?.contains("ไม่พบ refresh token") == true
                        if (isAuthFailure) {
                            authRepository.clearSessionLocally()
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = !isAuthFailure
                        )
                    }
            } catch (e: Exception) {
                // ถ้าเกิด error ให้ไปหน้า login
                authRepository.clearSessionLocally()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = false
                )
            }
        }
    }
}

