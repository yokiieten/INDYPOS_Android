package com.indybrain.indypos_Android.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.indybrain.indypos_Android.core.config.AppConfig
import com.indybrain.indypos_Android.core.device.DeviceInfoProvider
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.core.utils.ImageUtils
import com.indybrain.indypos_Android.data.local.AuthLocalDataSource
import com.indybrain.indypos_Android.data.local.database.IndyPosDatabase
import com.indybrain.indypos_Android.data.remote.api.AuthApi
import com.indybrain.indypos_Android.data.remote.api.ChangePasswordDataDto
import com.indybrain.indypos_Android.data.remote.api.ChangePasswordRequestDto
import com.indybrain.indypos_Android.data.remote.api.ForgotPasswordRequestDto
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.data.remote.api.ResetPasswordRequestDto
import com.indybrain.indypos_Android.data.remote.api.VerifyResetPasswordTokenRequestDto
import com.indybrain.indypos_Android.data.remote.api.LoginRequestDto
import com.indybrain.indypos_Android.data.remote.api.LogoutRequestDto
import com.indybrain.indypos_Android.data.remote.api.RegisterRequestDto
import com.indybrain.indypos_Android.data.remote.api.RegisterResponseDto
import com.indybrain.indypos_Android.data.remote.api.ResumeAuthRequestDto
import com.indybrain.indypos_Android.data.remote.api.UpdateShopDescriptionRequestDto
import com.indybrain.indypos_Android.data.remote.api.UpdateShopNameRequestDto
import com.indybrain.indypos_Android.data.remote.dto.LoginResponseDto
import com.indybrain.indypos_Android.data.remote.dto.ResumeAuthResponseDto
import com.indybrain.indypos_Android.domain.model.LoginRequest
import com.indybrain.indypos_Android.domain.model.RegisterRequest
import com.indybrain.indypos_Android.domain.model.User
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import com.indybrain.indypos_Android.domain.repository.CartRepository
import com.indybrain.indypos_Android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.File
import java.util.Locale
import javax.inject.Inject

/**
 * Implementation of AuthRepository
 */
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val localDataSource: AuthLocalDataSource,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val cartRepository: CartRepository,
    private val database: IndyPosDatabase,
    private val gson: Gson,
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    @ApplicationContext private val context: Context
) : AuthRepository {
    
    override suspend fun login(request: LoginRequest): Result<User> {
        return try {
            // Get device info with error handling
            val deviceInfo = try {
                deviceInfoProvider.getDeviceInfo()
            } catch (e: Exception) {
                return Result.failure(IllegalStateException("ไม่สามารถอ่านข้อมูลอุปกรณ์ได้: ${e.message}", e))
            }
            
            // Call remote API
            val response = authApi.login(
                LoginRequestDto(
                    email = request.email,
                    password = request.password,
                    deviceUuid = deviceInfo.deviceUuid,
                    deviceName = deviceInfo.deviceName,
                    deviceType = deviceInfo.deviceType,
                    platform = deviceInfo.platform,
                    model = deviceInfo.model,
                    version = deviceInfo.version,
                    appVersion = deviceInfo.appVersion
                )
            )
            
            // Map DTO to domain model
            val user = try {
                response.toDomainModel()
            } catch (e: Exception) {
                return Result.failure(IllegalStateException("ไม่สามารถแปลงข้อมูลผู้ใช้ได้: ${e.message}", e))
            }
            
            // Save to local storage
            try {
                localDataSource.saveUser(user)
            } catch (e: Exception) {
                // Log error but don't fail login if save fails
                // User is still logged in, just not persisted
            }
            
            Result.success(user)
        } catch (e: HttpException) {
            val errorMessage = parseErrorMessage(e.response()?.errorBody())
            Result.failure(IllegalStateException(errorMessage, e))
        } catch (e: Exception) {
            // Handle any other exceptions (network, parsing, etc.)
            val errorMessage = when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้ กรุณาตรวจสอบการเชื่อมต่ออินเทอร์เน็ต"
                e.message?.contains("timeout", ignoreCase = true) == true -> "การเชื่อมต่อหมดเวลา กรุณาลองใหม่อีกครั้ง"
                e.message?.contains("No address associated with hostname", ignoreCase = true) == true -> "ไม่พบเซิร์ฟเวอร์ กรุณาตรวจสอบการเชื่อมต่อ"
                e.message?.contains("Connection refused", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้"
                e.message?.contains("Network is unreachable", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่ออินเทอร์เน็ตได้"
                else -> e.message ?: "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง"
            }
            Result.failure(IllegalStateException(errorMessage, e))
        }
    }
    
    override suspend fun register(request: RegisterRequest): Result<User> {
        return try {
            // Get device info with error handling
            val deviceInfo = try {
                deviceInfoProvider.getDeviceInfo()
            } catch (e: Exception) {
                return Result.failure(IllegalStateException("ไม่สามารถอ่านข้อมูลอุปกรณ์ได้: ${e.message}", e))
            }
            
            // Remove dashes from phone number
            val cleanPhoneNumber = request.phone.replace("-", "")
            
            // Get locale for birth_date_locale (only if birth_date is provided)
            val birthDateLocale = request.birthDate?.takeIf { it.isNotBlank() }?.let {
                Locale.getDefault().toLanguageTag()
            }
            
            // Call remote API
            val response = authApi.register(
                RegisterRequestDto(
                    username = request.username,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    email = request.email,
                    phone = cleanPhoneNumber,
                    password = request.password,
                    shopName = request.shopName,
                    shopDescription = request.shopDescription ?: "", // Send empty string if null (matching iOS)
                    shopImageUrl = request.shopImageUrl ?: "", // Send empty string if null (matching iOS)
                    gender = request.gender,
                    birthDate = request.birthDate,
                    birthDateLocale = birthDateLocale,
                    termOfUse = request.termOfUse,
                    privacyPolicy = request.privacyPolicy,
                    marketingConsent = false, // Always false (matching iOS)
                    deviceUuid = deviceInfo.deviceUuid,
                    deviceName = deviceInfo.deviceName,
                    deviceType = deviceInfo.deviceType,
                    platform = deviceInfo.platform,
                    model = deviceInfo.model,
                    version = deviceInfo.version,
                    appVersion = deviceInfo.appVersion
                )
            )
            
            // Check if registration was successful
            if (response.user == null || response.token == null ) {
                val errorText = response.error ?: response.message
                val errorMessage = getLocalizedRegisterErrorMessage(
                    errorText = errorText,
                    statusCode = null
                ) ?: errorText ?: "เกิดข้อผิดพลาดในการสมัครสมาชิก"
                return Result.failure(IllegalStateException(errorMessage))
            }
            
            // Map DTO to domain model
            val user = try {
                response.toDomainModel()
            } catch (e: Exception) {
                return Result.failure(IllegalStateException("ไม่สามารถแปลงข้อมูลผู้ใช้ได้: ${e.message}", e))
            }
            
            // Save to local storage
            try {
                localDataSource.saveUser(user)
            } catch (e: Exception) {
                // Log error but don't fail registration if save fails
            }
            
            Result.success(user)
        } catch (e: HttpException) {
            val errorText = parseErrorMessage(e.response()?.errorBody())
            val errorMessage = getLocalizedRegisterErrorMessage(
                errorText = errorText,
                statusCode = e.code()
            ) ?: errorText ?: "เกิดข้อผิดพลาดในการสมัครสมาชิก"
            Result.failure(IllegalStateException(errorMessage, e))
        } catch (e: Exception) {
            // Handle any other exceptions (network, parsing, etc.)
            // First try to get localized error message from exception message
            val localizedError = getLocalizedRegisterErrorMessage(
                errorText = e.message,
                statusCode = null
            )
            
            val errorMessage = localizedError ?: when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้ กรุณาตรวจสอบการเชื่อมต่ออินเทอร์เน็ต"
                e.message?.contains("timeout", ignoreCase = true) == true -> "การเชื่อมต่อหมดเวลา กรุณาลองใหม่อีกครั้ง"
                e.message?.contains("No address associated with hostname", ignoreCase = true) == true -> "ไม่พบเซิร์ฟเวอร์ กรุณาตรวจสอบการเชื่อมต่อ"
                e.message?.contains("Connection refused", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้"
                e.message?.contains("Network is unreachable", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่ออินเทอร์เน็ตได้"
                else -> e.message ?: "เกิดข้อผิดพลาดในการสมัครสมาชิก"
            }
            Result.failure(IllegalStateException(errorMessage, e))
        }
    }
    
    override fun getCurrentUser(): Flow<User?> = flow {
        emit(localDataSource.getUser())
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            // Get device UUID for logout request
            val deviceInfo = try {
                deviceInfoProvider.getDeviceInfo()
            } catch (e: Exception) {
                // If we can't get device info, still clear local data and database
                localDataSource.clearUser()
                try {
                    database.clearAllData()
                } catch (dbError: Exception) {
                    if (BuildConfig.DEBUG) android.util.Log.e("AuthRepository", "Error clearing Room database: ${dbError.message}", dbError)
                }
                return Result.failure(IllegalStateException("ไม่สามารถอ่านข้อมูลอุปกรณ์ได้: ${e.message}", e))
            }
            
            // Try to call logout API
            try {
                authApi.logout(
                    LogoutRequestDto(
                        deviceUuid = deviceInfo.deviceUuid
                    )
                )
            } catch (e: Exception) {
                // Even if API call fails, clear local data
                // This ensures user is logged out locally
            }
            
            // Clear local data regardless of API call result
            localDataSource.clearUser()
            try {
                database.clearAllData()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.e("AuthRepository", "Error clearing Room database: ${e.message}", e)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            // Clear local data even on error
            localDataSource.clearUser()
            try {
                database.clearAllData()
            } catch (dbError: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.e("AuthRepository", "Error clearing Room database: ${dbError.message}", dbError)
            }
            Result.failure(IllegalStateException("เกิดข้อผิดพลาดในการออกจากระบบ: ${e.message}", e))
        }
    }
    
    override suspend fun requestWebLoginMagicLinkUrl(): Result<String> {
        return try {
            val response = authApi.requestWebLoginToken()
            val token = response.data?.token?.takeIf { it.isNotBlank() }
            val statusOk = response.status == null || response.status == 200
            if (token != null && statusOk) {
                Result.success(AppConfig.buildBackofficeAutoLoginUrl(token))
            } else {
                val msg = response.error?.takeIf { it.isNotBlank() }
                    ?: response.message?.takeIf { it.isNotBlank() }
                    ?: "ไม่สามารถสร้างลิงก์เข้า Back office ได้"
                Result.failure(IllegalStateException(msg))
            }
        } catch (e: HttpException) {
            val errorMessage = parseErrorMessage(e.response()?.errorBody())
            Result.failure(IllegalStateException(errorMessage, e))
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้ กรุณาตรวจสอบการเชื่อมต่ออินเทอร์เน็ต"
                e.message?.contains("timeout", ignoreCase = true) == true -> "การเชื่อมต่อหมดเวลา กรุณาลองใหม่อีกครั้ง"
                e.message?.contains("No address associated with hostname", ignoreCase = true) == true -> "ไม่พบเซิร์ฟเวอร์ กรุณาตรวจสอบการเชื่อมต่อ"
                e.message?.contains("Connection refused", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้"
                e.message?.contains("Network is unreachable", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่ออินเทอร์เน็ตได้"
                else -> e.message ?: "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง"
            }
            Result.failure(IllegalStateException(errorMessage, e))
        }
    }
    
    override suspend fun isLoggedIn(): Boolean {
        return localDataSource.isLoggedIn()
    }
    
    override suspend fun clearSessionLocally() {
        try {
            localDataSource.clearUser()
            database.clearAllData()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("AuthRepository", "Error clearing session: ${e.message}", e)
        }
    }

    override suspend fun resumeAuth(): Result<User> {
        return try {
            // Check if we have a refresh token
            val refreshToken = localDataSource.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                return Result.failure(IllegalStateException("ไม่พบ refresh token"))
            }
            
            // Get device info with error handling
            val deviceInfo = try {
                deviceInfoProvider.getDeviceInfo()
            } catch (e: Exception) {
                return Result.failure(IllegalStateException("ไม่สามารถอ่านข้อมูลอุปกรณ์ได้: ${e.message}", e))
            }
            
            // Call resume auth API
            val response = authApi.resumeAuth(
                ResumeAuthRequestDto(
                    deviceUuid = deviceInfo.deviceUuid,
                    refreshToken = refreshToken,
                    platform = deviceInfo.platform,
                    model = deviceInfo.model,
                    version = deviceInfo.version,
                    appVersion = deviceInfo.appVersion
                )
            )
            
            if (response.isSuccess) {
                // Map DTO to domain model
                val user = try {
                    response.toDomainModel()
                } catch (e: Exception) {
                    return Result.failure(IllegalStateException("ไม่สามารถแปลงข้อมูลผู้ใช้ได้: ${e.message}", e))
                }
                
                // Save updated user data to local storage
                try {
                    localDataSource.saveUser(user)
                } catch (e: Exception) {
                    // Don't fail resume if save fails, user data is still valid
                }
                
                Result.success(user)
            } else {
                val errorMessage = response.error ?: "เกิดข้อผิดพลาดในการยืนยันตัวตน"
                Result.failure(IllegalStateException(errorMessage))
            }
        } catch (e: HttpException) {
            val errorMessage = parseErrorMessage(e.response()?.errorBody())
            Result.failure(IllegalStateException(errorMessage, e))
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้ กรุณาตรวจสอบการเชื่อมต่ออินเทอร์เน็ต"
                e.message?.contains("timeout", ignoreCase = true) == true -> "การเชื่อมต่อหมดเวลา กรุณาลองใหม่อีกครั้ง"
                e.message?.contains("No address associated with hostname", ignoreCase = true) == true -> "ไม่พบเซิร์ฟเวอร์ กรุณาตรวจสอบการเชื่อมต่อ"
                e.message?.contains("Connection refused", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้"
                e.message?.contains("Network is unreachable", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่ออินเทอร์เน็ตได้"
                else -> e.message ?: "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง"
            }
            Result.failure(IllegalStateException(errorMessage, e))
        }
    }
    
    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val response = authApi.changePassword(
                ChangePasswordRequestDto(
                    oldPassword = oldPassword,
                    newPassword = newPassword
                )
            )
            
            if (response.status == 200 || response.status == 201) {
                val data = response.data
                if (data != null && data.user != null && data.token != null && data.refreshToken != null) {
                    // Regular user: save new tokens and updated user
                    val updatedUser = data.toChangePasswordUser()
                    localDataSource.saveUser(updatedUser)
                    Result.success(Unit)
                } else {
                    // Admin: data is null — clear auth and require login again
                    localDataSource.clearUser()
                    try {
                        database.clearAllData()
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) android.util.Log.e("AuthRepository", "Error clearing database on admin password change: ${e.message}", e)
                    }
                    Result.success(Unit)
                }
            } else {
                val errorText = response.error ?: response.message
                val errorMessage = getLocalizedChangePasswordErrorMessage(
                    errorText = errorText,
                    statusCode = response.status
                ) ?: errorText ?: context.getString(R.string.settings_change_password_error_generic)
                Result.failure(IllegalStateException(errorMessage))
            }
        } catch (e: HttpException) {
            val errorMessage = parseChangePasswordErrorMessage(e.response()?.errorBody())
            Result.failure(IllegalStateException(errorMessage, e))
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้ กรุณาตรวจสอบการเชื่อมต่ออินเทอร์เน็ต"
                e.message?.contains("timeout", ignoreCase = true) == true -> "การเชื่อมต่อหมดเวลา กรุณาลองใหม่อีกครั้ง"
                else -> e.message ?: context.getString(R.string.settings_change_password_error_generic)
            }
            Result.failure(IllegalStateException(errorMessage, e))
        }
    }
    
    override suspend fun updateShopDescription(description: String): Result<User> {
        return try {
            val response = authApi.updateShopDescription(
                UpdateShopDescriptionRequestDto(shopDescription = description)
            )
            
            if (response.status == 200 || response.status == 201) {
                // Get current user
                val currentUser = localDataSource.getUser()
                if (currentUser != null) {
                    // Update user with new shop description and shop name from response
                    val updatedUser = currentUser.copy(
                        shopDescription = response.data?.shopDescription ?: description,
                        shopName = response.data?.shopName ?: currentUser.shopName
                    )
                    
                    // Save updated user to local storage
                    localDataSource.saveUser(updatedUser)
                    
                    Result.success(updatedUser)
                } else {
                    Result.failure(IllegalStateException("ไม่พบข้อมูลผู้ใช้"))
                }
            } else {
                val errorMessage = response.message ?: "เกิดข้อผิดพลาดในการอัปเดตรายละเอียดร้าน"
                Result.failure(IllegalStateException(errorMessage))
            }
        } catch (e: HttpException) {
            val errorMessage = parseErrorMessage(e.response()?.errorBody())
            Result.failure(IllegalStateException(errorMessage, e))
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้ กรุณาตรวจสอบการเชื่อมต่ออินเทอร์เน็ต"
                e.message?.contains("timeout", ignoreCase = true) == true -> "การเชื่อมต่อหมดเวลา กรุณาลองใหม่อีกครั้ง"
                else -> e.message ?: "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง"
            }
            Result.failure(IllegalStateException(errorMessage, e))
        }
    }
    
    override suspend fun updateShopName(shopName: String): Result<User> {
        return try {
            val response = authApi.updateShopName(
                UpdateShopNameRequestDto(shopName = shopName)
            )
            
            if (response.status == 200 || response.status == 201) {
                // Get current user
                val currentUser = localDataSource.getUser()
                if (currentUser != null) {
                    // Update user with new shop name and shop description from response
                    val updatedUser = currentUser.copy(
                        shopName = response.data?.shopName ?: shopName,
                        shopDescription = response.data?.shopDescription ?: currentUser.shopDescription
                    )
                    
                    // Save updated user to local storage
                    localDataSource.saveUser(updatedUser)
                    
                    Result.success(updatedUser)
                } else {
                    Result.failure(IllegalStateException("ไม่พบข้อมูลผู้ใช้"))
                }
            } else {
                val errorMessage = response.message ?: "เกิดข้อผิดพลาดในการอัปเดตชื่อร้าน"
                Result.failure(IllegalStateException(errorMessage))
            }
        } catch (e: HttpException) {
            val errorMessage = parseErrorMessage(e.response()?.errorBody())
            Result.failure(IllegalStateException(errorMessage, e))
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้ กรุณาตรวจสอบการเชื่อมต่ออินเทอร์เน็ต"
                e.message?.contains("timeout", ignoreCase = true) == true -> "การเชื่อมต่อหมดเวลา กรุณาลองใหม่อีกครั้ง"
                else -> e.message ?: "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง"
            }
            Result.failure(IllegalStateException(errorMessage, e))
        }
    }

    override suspend fun updateShopImage(imageUri: Uri): Result<User> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(IllegalStateException("กรุณาเชื่อมต่ออินเทอร์เน็ต"))
            }

            // Resize image to 384x216 before upload (16:9, balanced size for clarity and file size)
            val resizedBitmap = ImageUtils.resizeImage(
                imageUri = imageUri,
                targetWidth = 384,
                targetHeight = 216,
                context = context
            ) ?: return Result.failure(IllegalStateException("ไม่สามารถประมวลผลรูปภาพได้"))

            // Save resized bitmap to temporary file using JPEG with high quality
            val tempFile = File(context.cacheDir, "shop_image_${System.currentTimeMillis()}.jpg")
            val saved = ImageUtils.saveBitmapToFile(
                bitmap = resizedBitmap,
                file = tempFile,
                quality = 95  // High quality JPEG (95-100) for good balance
            )

            if (!saved) {
                resizedBitmap.recycle()
                return Result.failure(IllegalStateException("ไม่สามารถบันทึกไฟล์รูปภาพได้"))
            }

            val mediaType = "image/jpeg".toMediaTypeOrNull()
            val requestFile = tempFile.asRequestBody(mediaType)
            val body = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)

            // Upload image
            val response = authApi.uploadShopImage(body)

            // Clean up
            resizedBitmap.recycle()
            tempFile.delete()

            val imageUrl = response.file.url

            // Update local user with new shopImageUrl
            val currentUser = localDataSource.getUser()
                ?: return Result.failure(IllegalStateException("ไม่พบข้อมูลผู้ใช้"))

            val updatedUser = currentUser.copy(
                shopImageUrl = imageUrl
            )

            localDataSource.saveUser(updatedUser)

            Result.success(updatedUser)
        } catch (e: HttpException) {
            val errorMessage = parseErrorMessage(e.response()?.errorBody())
            Result.failure(IllegalStateException(errorMessage, e))
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                    "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้ กรุณาตรวจสอบการเชื่อมต่ออินเทอร์เน็ต"

                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "การเชื่อมต่อหมดเวลา กรุณาลองใหม่อีกครั้ง"

                else -> e.message ?: "เกิดข้อผิดพลาดในการอัปโหลดรูปภาพ"
            }
            Result.failure(IllegalStateException(errorMessage, e))
        }
    }
    
    override suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            val response = authApi.forgotPassword(
                ForgotPasswordRequestDto(email = email.trim())
            )
            
            // Check if status is 200 (success)
            if (response.status == 200) {
                Result.success(Unit)
            } else {
                val errorMessage = response.error ?: response.message ?: "เกิดข้อผิดพลาดในการส่งลิงก์รีเซ็ตรหัสผ่าน"
                Result.failure(IllegalStateException(errorMessage))
            }
        } catch (e: HttpException) {
            val errorMessage = parseErrorMessage(e.response()?.errorBody())
            Result.failure(IllegalStateException(errorMessage, e))
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้ กรุณาตรวจสอบการเชื่อมต่ออินเทอร์เน็ต"
                e.message?.contains("timeout", ignoreCase = true) == true -> "การเชื่อมต่อหมดเวลา กรุณาลองใหม่อีกครั้ง"
                e.message?.contains("No address associated with hostname", ignoreCase = true) == true -> "ไม่พบเซิร์ฟเวอร์ กรุณาตรวจสอบการเชื่อมต่อ"
                e.message?.contains("Connection refused", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้"
                e.message?.contains("Network is unreachable", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่ออินเทอร์เน็ตได้"
                else -> e.message ?: "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง"
            }
            Result.failure(IllegalStateException(errorMessage, e))
        }
    }
    
    override suspend fun verifyResetPasswordToken(token: String): Result<Int?> {
        return try {
            val response = authApi.verifyResetPasswordToken(
                VerifyResetPasswordTokenRequestDto(token = token.trim())
            )
            
            // Check if status is 200 and token is valid
            if (response.status == 200 && response.data?.valid == true) {
                Result.success(response.data.userId)
            } else {
                val errorMessage = response.error ?: response.message ?: "โทเค็นไม่ถูกต้องหรือหมดอายุแล้ว"
                Result.failure(IllegalStateException(errorMessage))
            }
        } catch (e: HttpException) {
            val errorMessage = parseErrorMessage(e.response()?.errorBody())
            Result.failure(IllegalStateException(errorMessage, e))
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้ กรุณาตรวจสอบการเชื่อมต่ออินเทอร์เน็ต"
                e.message?.contains("timeout", ignoreCase = true) == true -> "การเชื่อมต่อหมดเวลา กรุณาลองใหม่อีกครั้ง"
                e.message?.contains("No address associated with hostname", ignoreCase = true) == true -> "ไม่พบเซิร์ฟเวอร์ กรุณาตรวจสอบการเชื่อมต่อ"
                e.message?.contains("Connection refused", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้"
                e.message?.contains("Network is unreachable", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่ออินเทอร์เน็ตได้"
                else -> e.message ?: "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง"
            }
            Result.failure(IllegalStateException(errorMessage, e))
        }
    }
    
    override suspend fun resetPassword(token: String, newPassword: String): Result<Unit> {
        return try {
            val response = authApi.resetPassword(
                ResetPasswordRequestDto(
                    token = token.trim(),
                    newPassword = newPassword.trim()
                )
            )
            
            // Check if status is 200 (success)
            if (response.status == 200 || response.status == 201) {
                Result.success(Unit)
            } else {
                val errorMessage = response.error ?: response.message ?: "เกิดข้อผิดพลาดในการรีเซ็ตรหัสผ่าน"
                Result.failure(IllegalStateException(errorMessage))
            }
        } catch (e: HttpException) {
            val errorMessage = parseErrorMessage(e.response()?.errorBody())
            Result.failure(IllegalStateException(errorMessage, e))
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้ กรุณาตรวจสอบการเชื่อมต่ออินเทอร์เน็ต"
                e.message?.contains("timeout", ignoreCase = true) == true -> "การเชื่อมต่อหมดเวลา กรุณาลองใหม่อีกครั้ง"
                e.message?.contains("No address associated with hostname", ignoreCase = true) == true -> "ไม่พบเซิร์ฟเวอร์ กรุณาตรวจสอบการเชื่อมต่อ"
                e.message?.contains("Connection refused", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้"
                e.message?.contains("Network is unreachable", ignoreCase = true) == true -> "ไม่สามารถเชื่อมต่ออินเทอร์เน็ตได้"
                else -> e.message ?: "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง"
            }
            Result.failure(IllegalStateException(errorMessage, e))
        }
    }
    
    /**
     * Get localized error message for change password API errors.
     * Maps API error patterns to localized strings (supports Thai/English via device locale).
     */
    private fun getLocalizedChangePasswordErrorMessage(
        errorText: String?,
        statusCode: Int? = null
    ): String? {
        val error = errorText?.lowercase(Locale.ROOT) ?: return null
        
        // Check for "old password is incorrect" pattern
        if (error.contains("old password") &&
            (error.contains("incorrect") || error.contains("wrong") ||
                error.contains("invalid") || error.contains("not match"))) {
            return context.getString(R.string.settings_change_password_error_old_incorrect)
        }
        
        // Check for "current password" (alternative API wording)
        if (error.contains("current password") &&
            (error.contains("incorrect") || error.contains("wrong") ||
                error.contains("invalid") || error.contains("not match"))) {
            return context.getString(R.string.settings_change_password_error_old_incorrect)
        }
        
        // Check for "password" and "incorrect" together (generic password error)
        if (error.contains("password") &&
            (error.contains("incorrect") || error.contains("wrong") || error.contains("invalid"))) {
            return context.getString(R.string.settings_change_password_error_old_incorrect)
        }
        
        // Check for password mismatch (new vs confirm)
        if (error.contains("mismatch") || error.contains("do not match") || error.contains("not match")) {
            return context.getString(R.string.settings_change_password_error_mismatch)
        }
        
        // Check for weak password
        if (error.contains("at least") && error.contains("character")) {
            return context.getString(R.string.settings_change_password_error_weak)
        }
        
        // Check for "invalid request" (e.g. new password too short)
        if (error.contains("invalid request")) {
            return context.getString(R.string.settings_change_password_error_weak)
        }
        
        return null
    }
    
    /**
     * Parse error message from HTTP error response body (4xx/5xx from API).
     * Maps known error patterns to localized strings.
     */
    private fun parseChangePasswordErrorMessage(errorBody: ResponseBody?): String {
        return try {
            if (errorBody == null) {
                return context.getString(R.string.settings_change_password_error_generic)
            }
            
            val errorJson = errorBody.string()
            if (errorJson.isBlank()) {
                return context.getString(R.string.settings_change_password_error_generic)
            }
            
            val errorResponse = gson.fromJson(errorJson, ChangePasswordErrorResponse::class.java)
            val rawMessage = errorResponse?.message?.takeIf { it.isNotBlank() }
                ?: errorResponse?.error?.takeIf { it.isNotBlank() }
            
            when {
                rawMessage != null -> {
                    // Map known API error patterns to localized message
                    getLocalizedChangePasswordErrorMessage(rawMessage) ?: rawMessage
                }
                else -> context.getString(R.string.settings_change_password_error_generic)
            }
        } catch (e: Exception) {
            context.getString(R.string.settings_change_password_error_generic)
        }
    }
    
    /**
     * Get localized error message for register errors
     * Similar to iOS implementation
     */
    private fun getLocalizedRegisterErrorMessage(
        errorText: String?,
        statusCode: Int? = null
    ): String? {
        val error = errorText?.lowercase() ?: return null
        
        // Check for "email already exists" pattern
        if (error.contains("email") && 
            (error.contains("already exists") || error.contains("already registered") || 
             error.contains("already in use"))) {
            return "อีเมลนี้ถูกใช้งานแล้ว"
        }
        
        // Check for "phone number already exists" pattern
        if ((error.contains("phone") || error.contains("phone number")) && 
            (error.contains("already exists") || error.contains("already registered") || 
             error.contains("already in use"))) {
            return "เบอร์โทรศัพท์นี้ถูกใช้งานแล้ว"
        }
        
        // Check for "username already exists" pattern
        if (error.contains("username") && 
            (error.contains("already exists") || error.contains("already taken") || 
             error.contains("already in use"))) {
            return "ชื่อผู้ใช้นี้ถูกใช้งานแล้ว"
        }
        
        // Check for duplicate patterns
        if (error.contains("duplicate")) {
            if (error.contains("email")) {
                return "อีเมลนี้ถูกใช้งานแล้ว"
            }
            if (error.contains("phone")) {
                return "เบอร์โทรศัพท์นี้ถูกใช้งานแล้ว"
            }
            if (error.contains("username")) {
                return "ชื่อผู้ใช้นี้ถูกใช้งานแล้ว"
            }
        }
        
        return null
    }
    
    /**
     * Extension function to map RegisterResponseDto to domain model
     */
    private fun RegisterResponseDto.toDomainModel(): User {
        return try {
            val userDto = this.user ?: throw IllegalStateException("User data is null")
            User(
                id = userDto.id,
                username = userDto.username,
                firstName = userDto.firstName,
                lastName = userDto.lastName,
                email = userDto.email ?: "",
                phone = userDto.phone,
                role = null,
                shopName = userDto.shopName,
                shopDescription = userDto.shopDescription,
                shopImageUrl = userDto.shopImageUrl,
                subscriptionPlan = null,
                subscriptionExpiresAt = null,
                maxDevices = null,
                currentDeviceUuid = null,
                isActivated = null,
                termOfUse = null,
                privacyPolicy = null,
                marketingConsent = null,
                birthDate = null,
                createdAt = null,
                updatedAt = null,
                orderCount = null,
                token = this.token?.takeIf { it.isNotBlank() },
                refreshToken = this.refreshToken?.takeIf { it.isNotBlank() },
                expiresIn = null
            )
        } catch (e: Exception) {
            throw IllegalStateException("ไม่สามารถแปลงข้อมูลผู้ใช้ได้: ${e.message}", e)
        }
    }
    
    /**
     * Extension function to map ChangePasswordDataDto to domain User (with new tokens)
     */
    private fun ChangePasswordDataDto.toChangePasswordUser(): User {
        val userDto = this.user ?: throw IllegalStateException("User data is null")
        return User(
            id = userDto.id,
            username = userDto.username,
            firstName = userDto.firstName,
            lastName = userDto.lastName,
            email = userDto.email.ifBlank { "" },
            phone = userDto.phone,
            role = userDto.role,
            shopName = userDto.shopName,
            shopDescription = userDto.shopDescription,
            shopImageUrl = userDto.shopImageUrl,
            subscriptionPlan = userDto.subscriptionPlan,
            subscriptionExpiresAt = userDto.subscriptionExpiresAt,
            maxDevices = userDto.maxDevices,
            currentDeviceUuid = userDto.currentDeviceUuid,
            isActivated = userDto.isActivated,
            termOfUse = userDto.termOfUse,
            privacyPolicy = userDto.privacyPolicy,
            marketingConsent = userDto.marketingConsent,
            birthDate = userDto.birthDate,
            createdAt = userDto.createdAt,
            updatedAt = userDto.updatedAt,
            orderCount = userDto.orderCount,
            token = this.token?.ifBlank { null },
            refreshToken = this.refreshToken?.ifBlank { null },
            expiresIn = this.expiresIn
        )
    }
    
    /**
     * Extension function to map ResumeAuthResponseDto to domain model
     */
    private fun ResumeAuthResponseDto.toDomainModel(): User {
        return try {
            val userDto = this.user ?: throw IllegalStateException("User data is null")
            User(
                id = userDto.id,
                username = userDto.username,
                firstName = userDto.firstName,
                lastName = userDto.lastName,
                email = userDto.email.ifBlank { "" },
                phone = userDto.phone,
                role = userDto.role,
                shopName = userDto.shopName,
                shopDescription = userDto.shopDescription,
                shopImageUrl = userDto.shopImageUrl,
                subscriptionPlan = userDto.subscriptionPlan,
                subscriptionExpiresAt = userDto.subscriptionExpiresAt,
                maxDevices = userDto.maxDevices,
                currentDeviceUuid = userDto.currentDeviceUuid,
                isActivated = userDto.isActivated,
                termOfUse = userDto.termOfUse,
                privacyPolicy = userDto.privacyPolicy,
                marketingConsent = userDto.marketingConsent,
                birthDate = userDto.birthDate,
                createdAt = userDto.createdAt,
                updatedAt = userDto.updatedAt,
                orderCount = userDto.orderCount,
                token = this.token?.ifBlank { null },
                refreshToken = this.refreshToken?.ifBlank { null },
                expiresIn = this.expiresIn
            )
        } catch (e: Exception) {
            throw IllegalStateException("ไม่สามารถแปลงข้อมูลผู้ใช้ได้: ${e.message}", e)
        }
    }
    
    /**
     * Extension function to map DTO to domain model
     */
    private fun LoginResponseDto.toDomainModel(): User {
        return try {
            User(
                id = this.user.id,
                username = this.user.username,
                firstName = this.user.firstName,
                lastName = this.user.lastName,
                email = this.user.email.ifBlank { "" },
                phone = this.user.phone,
                role = this.user.role,
                shopName = this.user.shopName,
                shopDescription = this.user.shopDescription,
                shopImageUrl = this.user.shopImageUrl,
                subscriptionPlan = this.user.subscriptionPlan,
                subscriptionExpiresAt = this.user.subscriptionExpiresAt,
                maxDevices = this.user.maxDevices,
                currentDeviceUuid = this.user.currentDeviceUuid,
                isActivated = this.user.isActivated,
                termOfUse = this.user.termOfUse,
                privacyPolicy = this.user.privacyPolicy,
                marketingConsent = this.user.marketingConsent,
                birthDate = this.user.birthDate,
                createdAt = this.user.createdAt,
                updatedAt = this.user.updatedAt,
                orderCount = this.user.orderCount,
                token = this.token.ifBlank { null },
                refreshToken = this.refreshToken.ifBlank { null },
                expiresIn = this.expiresIn
            )
        } catch (e: Exception) {
            throw IllegalStateException("ไม่สามารถแปลงข้อมูลผู้ใช้ได้: ${e.message}", e)
        }
    }

    private fun parseErrorMessage(errorBody: ResponseBody?): String {
        return try {
            if (errorBody == null) {
                return "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง"
            }
            
            val errorJson = errorBody.string()
            if (errorJson.isBlank()) {
                return "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง"
            }
            
            val errorResponse = gson.fromJson(errorJson, LoginErrorResponse::class.java)
            errorResponse?.error?.takeIf { it.isNotBlank() } ?: "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง"
        } catch (e: Exception) {
            "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง"
        }
    }
}

private data class LoginErrorResponse(
    val error: String?
)

private data class ChangePasswordErrorResponse(
    val error: String?,
    val message: String?
)

