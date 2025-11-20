package com.indybrain.indypos_Android.data.repository

import com.google.gson.Gson
import com.indybrain.indypos_Android.core.device.DeviceInfoProvider
import com.indybrain.indypos_Android.data.local.AuthLocalDataSource
import com.indybrain.indypos_Android.data.remote.api.AuthApi
import com.indybrain.indypos_Android.data.remote.api.LoginRequestDto
import com.indybrain.indypos_Android.data.remote.dto.LoginResponseDto
import com.indybrain.indypos_Android.domain.model.LoginRequest
import com.indybrain.indypos_Android.domain.model.User
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Implementation of AuthRepository
 */
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val localDataSource: AuthLocalDataSource,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val gson: Gson
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
                subscriptionPlan = this.user.subscriptionPlan,
                maxDevices = this.user.maxDevices,
                currentDeviceUuid = this.user.currentDeviceUuid,
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

