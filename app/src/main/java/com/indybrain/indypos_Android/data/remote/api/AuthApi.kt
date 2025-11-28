package com.indybrain.indypos_Android.data.remote.api

import com.indybrain.indypos_Android.data.remote.dto.LoginResponseDto
import com.indybrain.indypos_Android.data.remote.dto.LogoutResponseDto
import com.google.gson.annotations.SerializedName
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
    
    /**
     * Logout endpoint
     */
    @POST("protected/auth/logout")
    suspend fun logout(@Body request: LogoutRequestDto): LogoutResponseDto
}

/**
 * Request DTO for login
 */
data class LoginRequestDto(
    val email: String,
    val password: String,
    @SerializedName("device_uuid")
    val deviceUuid: String,
    @SerializedName("device_name")
    val deviceName: String,
    @SerializedName("device_type")
    val deviceType: String,
    val platform: String,
    val model: String,
    val version: String,
    @SerializedName("app_version")
    val appVersion: String
)

/**
 * Request DTO for logout
 */
data class LogoutRequestDto(
    @SerializedName("device_uuid")
    val deviceUuid: String
)

