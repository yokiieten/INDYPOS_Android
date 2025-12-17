package com.indybrain.indypos_Android.data.remote.api

import com.indybrain.indypos_Android.data.remote.dto.LoginResponseDto
import com.indybrain.indypos_Android.data.remote.dto.LogoutResponseDto
import com.indybrain.indypos_Android.data.remote.dto.UploadShopImageResponseDto
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PUT

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
    
    /**
     * Change password endpoint
     */
    @POST("protected/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequestDto): ChangePasswordResponseDto
    
    /**
     * Update shop description endpoint
     */
    @PATCH("protected/users/shop-description")
    suspend fun updateShopDescription(@Body request: UpdateShopDescriptionRequestDto): ShopUpdateResponseDto
    
    /**
     * Update shop name endpoint
     */
    @PATCH("protected/users/shop-name")
    suspend fun updateShopName(@Body request: UpdateShopNameRequestDto): ShopUpdateResponseDto

    /**
     * Upload / update shop image endpoint
     * PUT /protected/upload/shop-image (base URL already contains /api/v1)
     */
    @Multipart
    @PUT("protected/upload/shop-image")
    suspend fun uploadShopImage(
        @Part image: MultipartBody.Part
    ): UploadShopImageResponseDto
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

/**
 * Request DTO for change password
 */
data class ChangePasswordRequestDto(
    @SerializedName("old_password")
    val oldPassword: String,
    @SerializedName("new_password")
    val newPassword: String
)

/**
 * Response DTO for change password
 */
data class ChangePasswordResponseDto(
    val status: Int?,
    val message: String?,
    val data: Any?,
    val timestamp: String?
)

/**
 * Request DTO for updating shop description
 */
data class UpdateShopDescriptionRequestDto(
    @SerializedName("shop_description")
    val shopDescription: String
)

/**
 * Request DTO for updating shop name
 */
data class UpdateShopNameRequestDto(
    @SerializedName("shop_name")
    val shopName: String
)

/**
 * Response DTO for shop update operations
 */
data class ShopUpdateResponseDto(
    val status: Int?,
    val message: String?,
    val data: ShopUpdateDataDto?,
    val timestamp: String?
)

/**
 * Data DTO for shop update response
 */
data class ShopUpdateDataDto(
    @SerializedName("shop_name")
    val shopName: String?,
    @SerializedName("shop_description")
    val shopDescription: String?
)

