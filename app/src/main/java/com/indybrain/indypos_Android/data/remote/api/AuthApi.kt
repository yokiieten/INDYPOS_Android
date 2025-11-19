package com.indybrain.indypos_Android.data.remote.api

import com.indybrain.indypos_Android.data.remote.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API interface for authentication endpoints
 */
interface AuthApi {
    /**
     * Login endpoint
     */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto
}

/**
 * Request DTO for login
 */
data class LoginRequestDto(
    val email: String,
    val password: String
)

