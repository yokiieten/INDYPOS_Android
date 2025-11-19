package com.indybrain.indypos_Android.domain.usecase

import com.indybrain.indypos_Android.domain.model.LoginRequest
import com.indybrain.indypos_Android.domain.model.User
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for user login
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Execute login with email and password
     * @return Result containing User on success or error message
     */
    suspend operator fun invoke(request: LoginRequest): Result<User> {
        // Validate input
        if (request.email.isBlank()) {
            return Result.failure(IllegalArgumentException("Email cannot be empty"))
        }
        if (request.password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password cannot be empty"))
        }
        
        // Call repository
        return authRepository.login(request)
    }
}

