package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.domain.model.LoginRequest
import com.indybrain.indypos_Android.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations
 */
interface AuthRepository {
    /**
     * Login with email and password
     * @return Result containing User on success or error message
     */
    suspend fun login(request: LoginRequest): Result<User>
    
    /**
     * Get current logged in user
     */
    fun getCurrentUser(): Flow<User?>
    
    /**
     * Logout current user
     * @return Result indicating success or failure
     */
    suspend fun logout(): Result<Unit>
    
    /**
     * Check if user is logged in
     */
    suspend fun isLoggedIn(): Boolean
}

