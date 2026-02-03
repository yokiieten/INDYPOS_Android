package com.indybrain.indypos_Android.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.indybrain.indypos_Android.core.device.DeviceInfoProvider
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.core.utils.ImageUtils
import com.indybrain.indypos_Android.data.local.AuthLocalDataSource
import com.indybrain.indypos_Android.data.local.database.IndyPosDatabase
import com.indybrain.indypos_Android.data.remote.api.AuthApi
import com.indybrain.indypos_Android.data.remote.api.ChangePasswordRequestDto
import com.indybrain.indypos_Android.data.remote.api.ForgotPasswordRequestDto
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
        android.util.Log.d("AuthRepository", "logout() called - Stack trace: ${Thread.currentThread().stackTrace.joinToString("\n")}")
        return try {
            // Get device UUID for logout request
            val deviceInfo = try {
                deviceInfoProvider.getDeviceInfo()
            } catch (e: Exception) {
                // If we can't get device info, still clear local data and database
                localDataSource.clearUser()
                try {
                    database.clearAllData()
                    android.util.Log.d("AuthRepository", "All Room database data cleared successfully (device info error)")
                } catch (dbError: Exception) {
                    android.util.Log.e("AuthRepository", "Error clearing Room database: ${dbError.message}", dbError)
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
            // Clear all Room database data when user logs out
            // This ensures no user-specific data persists after logout
            try {
                database.clearAllData()
                android.util.Log.d("AuthRepository", "All Room database data cleared successfully")
            } catch (e: Exception) {
                // Log error but don't fail logout if database clear fails
                android.util.Log.e("AuthRepository", "Error clearing Room database: ${e.message}", e)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            // Clear local data even on error
            localDataSource.clearUser()
            // Clear all Room database data when user logs out
            // This ensures no user-specific data persists after logout
            try {
                database.clearAllData()
                android.util.Log.d("AuthRepository", "All Room database data cleared successfully (on error)")
            } catch (dbError: Exception) {
                // Log error but don't fail logout if database clear fails
                android.util.Log.e("AuthRepository", "Error clearing Room database: ${dbError.message}", dbError)
            }
            Result.failure(IllegalStateException("เกิดข้อผิดพลาดในการออกจากระบบ: ${e.message}", e))
        }
    }
    
    override suspend fun isLoggedIn(): Boolean {
        return localDataSource.isLoggedIn()
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
                Result.success(Unit)
            } else {
                val errorMessage = getLocalizedChangePasswordErrorMessage(
                    errorText = response.message,
                    statusCode = response.status
                ) ?: response.message ?: "เกิดข้อผิดพลาดในการเปลี่ยนรหัสผ่าน"
                Result.failure(IllegalStateException(errorMessage))
            }
        } catch (e: HttpException) {
            val errorMessage = parseChangePasswordErrorMessage(e.response()?.errorBody())
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
     * Get localized error message for change password errors
     * Similar to iOS implementation
     */
    private fun getLocalizedChangePasswordErrorMessage(
        errorText: String?,
        statusCode: Int? = null
    ): String? {
        val error = errorText?.lowercase() ?: return null
        
        // Check for "old password is incorrect" pattern
        if (error.contains("old password") && 
            (error.contains("incorrect") || error.contains("wrong") || 
             error.contains("invalid") || error.contains("not match"))) {
            return "รหัสผ่านเดิมไม่ถูกต้อง"
        }
        
        // Check for "password" and "incorrect" together
        if (error.contains("password") && 
            (error.contains("incorrect") || error.contains("wrong") || error.contains("invalid"))) {
            return "รหัสผ่านเดิมไม่ถูกต้อง"
        }
        
        return null
    }
    
    /**
     * Parse error message from HTTP error response body
     */
    private fun parseChangePasswordErrorMessage(errorBody: ResponseBody?): String {
        return try {
            if (errorBody == null) {
                return "เกิดข้อผิดพลาดในการเปลี่ยนรหัสผ่าน"
            }
            
            val errorJson = errorBody.string()
            if (errorJson.isBlank()) {
                return "เกิดข้อผิดพลาดในการเปลี่ยนรหัสผ่าน"
            }
            
            val errorResponse = gson.fromJson(errorJson, ChangePasswordErrorResponse::class.java)
            val errorMessage = errorResponse?.message?.takeIf { it.isNotBlank() } 
                ?: errorResponse?.error?.takeIf { it.isNotBlank() }
                ?: "เกิดข้อผิดพลาดในการเปลี่ยนรหัสผ่าน"
            
            // Apply localized error message logic
            getLocalizedChangePasswordErrorMessage(errorMessage) ?: errorMessage
        } catch (e: Exception) {
            "เกิดข้อผิดพลาดในการเปลี่ยนรหัสผ่าน"
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
                ownerId = userDto.ownerId,
                roleId = userDto.roleId,
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
                permissions = userDto.permissions,
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
                ownerId = this.user.ownerId,
                roleId = this.user.roleId,
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
                permissions = this.user.permissions,
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

