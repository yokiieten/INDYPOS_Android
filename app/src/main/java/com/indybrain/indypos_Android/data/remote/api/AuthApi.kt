package com.indybrain.indypos_Android.data.remote.api

import com.indybrain.indypos_Android.data.remote.dto.EmployeeListResponseDto
import com.indybrain.indypos_Android.data.remote.dto.EmployeeSingleResponseDto
import com.indybrain.indypos_Android.data.remote.dto.LoginResponseDto
import com.indybrain.indypos_Android.data.remote.dto.LogoutResponseDto
import com.indybrain.indypos_Android.data.remote.dto.ResumeAuthResponseDto
import com.indybrain.indypos_Android.data.remote.dto.UploadShopImageResponseDto
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

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
     * Register endpoint
     */
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): RegisterResponseDto
    
    /**
     * Resume authentication endpoint
     */
    @POST("auth/resume")
    suspend fun resumeAuth(@Body request: ResumeAuthRequestDto): ResumeAuthResponseDto
    
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
    
    /**
     * Forgot password endpoint
     */
    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequestDto): ForgotPasswordResponseDto
    
    /**
     * Verify reset password token endpoint
     */
    @POST("auth/reset-password/verify")
    suspend fun verifyResetPasswordToken(@Body request: VerifyResetPasswordTokenRequestDto): VerifyResetPasswordTokenResponseDto
    
    /**
     * Reset password endpoint
     */
    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequestDto): ResetPasswordResponseDto
    
    /**
     * Get employee list endpoint
     * Requires authentication and user.manage permission
     * Owner can see their own employees
     * Admin can see all employees or filter by owner_id
     */
    @GET("protected/employees")
    suspend fun getEmployees(
        @Query("owner_id") ownerId: Int? = null
    ): EmployeeListResponseDto

    /**
     * Create employee
     * POST /api/v1/protected/employees
     * Permission: user.manage (Owner or Admin)
     */
    @POST("protected/employees")
    suspend fun createEmployee(@Body request: CreateEmployeeRequestDto): EmployeeSingleResponseDto

    /**
     * Update employee
     * PUT /api/v1/protected/employees/:id
     * Permission: user.manage (Owner or Admin)
     */
    @PUT("protected/employees/{id}")
    suspend fun updateEmployee(
        @Path("id") employeeId: Int,
        @Body request: UpdateEmployeeRequestDto
    ): EmployeeSingleResponseDto

    /**
     * Delete employee
     * DELETE /api/v1/protected/employees/:id
     * Permission: user.manage (Owner or Admin)
     */
    @DELETE("protected/employees/{id}")
    suspend fun deleteEmployee(
        @Path("id") employeeId: Int
    ): EmployeeSingleResponseDto
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
 * Request DTO for resume authentication
 */
data class ResumeAuthRequestDto(
    @SerializedName("device_uuid")
    val deviceUuid: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
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

/**
 * Request DTO for register
 */
data class RegisterRequestDto(
    val username: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    val email: String,
    val phone: String,
    val password: String,
    @SerializedName("shop_name")
    val shopName: String,
    @SerializedName("shop_description")
    val shopDescription: String? = null,
    @SerializedName("shop_image_url")
    val shopImageUrl: String? = null,
    @SerializedName("birth_date")
    val birthDate: String? = null,
    @SerializedName("birth_date_locale")
    val birthDateLocale: String? = null,
    @SerializedName("term_of_use")
    val termOfUse: Boolean,
    @SerializedName("privacy_policy")
    val privacyPolicy: Boolean,
    @SerializedName("marketing_consent")
    val marketingConsent: Boolean = false,
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
 * Response DTO for register
 */
data class RegisterResponseDto(
    @SerializedName("is_success")
    val isSuccess: Boolean? = null,
    val user: RegisterUserDto? = null,
    val token: String? = null,
    @SerializedName("refresh_token")
    val refreshToken: String? = null,
    val error: String? = null,
    val message: String? = null
)

/**
 * User DTO for register response
 */
data class RegisterUserDto(
    val id: Int,
    val username: String? = null,
    @SerializedName("first_name")
    val firstName: String? = null,
    @SerializedName("last_name")
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @SerializedName("shop_name")
    val shopName: String? = null,
    @SerializedName("shop_description")
    val shopDescription: String? = null,
    @SerializedName("shop_image_url")
    val shopImageUrl: String? = null
)

/**
 * Request DTO for forgot password
 */
data class ForgotPasswordRequestDto(
    val email: String
)

/**
 * Response DTO for forgot password
 */
data class ForgotPasswordResponseDto(
    val status: Int? = null,
    val message: String? = null,
    val data: Any? = null,
    val timestamp: String? = null,
    val error: String? = null
)

/**
 * Request DTO for verify reset password token
 */
data class VerifyResetPasswordTokenRequestDto(
    val token: String
)

/**
 * Response DTO for verify reset password token
 */
data class VerifyResetPasswordTokenResponseDto(
    val status: Int? = null,
    val message: String? = null,
    val data: VerifyResetPasswordTokenDataDto? = null,
    val timestamp: String? = null,
    val error: String? = null
)

/**
 * Data DTO for verify reset password token response
 */
data class VerifyResetPasswordTokenDataDto(
    val valid: Boolean? = null,
    @SerializedName("userId")
    val userId: Int? = null
)

/**
 * Request DTO for reset password
 */
data class ResetPasswordRequestDto(
    val token: String,
    @SerializedName("new_password")
    val newPassword: String
)

/**
 * Response DTO for reset password
 */
data class ResetPasswordResponseDto(
    val status: Int? = null,
    val message: String? = null,
    val data: Any? = null,
    val timestamp: String? = null,
    val error: String? = null
)

/**
 * Request DTO for create employee
 * POST /api/v1/protected/employees
 */
data class CreateEmployeeRequestDto(
    val username: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    val email: String,
    val phone: String,
    val password: String,
    @SerializedName("role_id") val roleId: Int? = null,
    val permissions: List<String>? = null
)

/**
 * Request DTO for update employee
 * PUT /api/v1/protected/employees/:id
 * Send only fields to update
 */
data class UpdateEmployeeRequestDto(
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @SerializedName("role_id") val roleId: Int? = null,
    @SerializedName("is_activated") val isActivated: Boolean? = null,
    val permissions: List<String>? = null
)
