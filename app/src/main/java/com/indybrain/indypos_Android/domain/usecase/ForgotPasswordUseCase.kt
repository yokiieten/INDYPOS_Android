package com.indybrain.indypos_Android.domain.usecase

import com.indybrain.indypos_Android.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for forgot password
 */
class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Execute forgot password request
     * @param email User email address
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(email: String): Result<Unit> {
        // Validate input
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Email cannot be empty"))
        }
        
        // Validate email format
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        if (!emailRegex.toRegex().matches(email.trim())) {
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }
        
        // Call repository
        return authRepository.forgotPassword(email)
    }
}

