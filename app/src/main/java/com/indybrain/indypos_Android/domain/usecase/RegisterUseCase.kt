package com.indybrain.indypos_Android.domain.usecase

import com.indybrain.indypos_Android.domain.model.RegisterRequest
import com.indybrain.indypos_Android.domain.model.User
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for user registration
 */
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Execute registration with user data
     * @return Result containing User on success or error message
     */
    suspend operator fun invoke(request: RegisterRequest): Result<User> {
        // Basic validation
        if (request.username.isBlank()) {
            return Result.failure(IllegalArgumentException("Username cannot be empty"))
        }
        if (request.password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password cannot be empty"))
        }
        if (request.email.isBlank()) {
            return Result.failure(IllegalArgumentException("Email cannot be empty"))
        }
        
        // Call repository
        return authRepository.register(request)
    }
}

