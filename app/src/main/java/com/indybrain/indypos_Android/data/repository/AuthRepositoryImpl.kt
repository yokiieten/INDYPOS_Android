package com.indybrain.indypos_Android.data.repository

import com.indybrain.indypos_Android.data.local.AuthLocalDataSource
import com.indybrain.indypos_Android.data.remote.api.AuthApi
import com.indybrain.indypos_Android.data.remote.api.LoginRequestDto
import com.indybrain.indypos_Android.data.remote.dto.LoginResponseDto
import com.indybrain.indypos_Android.domain.model.LoginRequest
import com.indybrain.indypos_Android.domain.model.User
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Implementation of AuthRepository
 */
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val localDataSource: AuthLocalDataSource
) : AuthRepository {
    
    override suspend fun login(request: LoginRequest): Result<User> {
        return try {
            // Call remote API
            val response = authApi.login(
                LoginRequestDto(
                    email = request.email,
                    password = request.password
                )
            )
            
            // Map DTO to domain model
            val user = response.toDomainModel()
            
            // Save to local storage
            localDataSource.saveUser(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getCurrentUser(): Flow<User?> = flow {
        emit(localDataSource.getUser())
    }
    
    override suspend fun logout() {
        localDataSource.clearUser()
    }
    
    override suspend fun isLoggedIn(): Boolean {
        return localDataSource.isLoggedIn()
    }
    
    /**
     * Extension function to map DTO to domain model
     */
    private fun LoginResponseDto.toDomainModel(): User {
        return User(
            id = this.id,
            email = this.email,
            name = this.name,
            token = this.token
        )
    }
}

