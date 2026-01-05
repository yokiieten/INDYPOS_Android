package com.indybrain.indypos_Android.domain.usecase

import com.indybrain.indypos_Android.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for resetting password with token
 */
class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(token: String, newPassword: String): Result<Unit> {
        return authRepository.resetPassword(token, newPassword)
    }
}

