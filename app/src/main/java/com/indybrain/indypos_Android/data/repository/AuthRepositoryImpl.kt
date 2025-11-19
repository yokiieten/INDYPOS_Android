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
            // Call remote API
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
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
            val user = response.toDomainModel()
            
            // Save to local storage
            localDataSource.saveUser(user)
            
            Result.success(user)
        } catch (e: HttpException) {
            val errorMessage = parseErrorMessage(e.response()?.errorBody())
            Result.failure(IllegalStateException(errorMessage, e))
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
            id = this.user.id,
            username = this.user.username,
            firstName = this.user.firstName,
            lastName = this.user.lastName,
            email = this.user.email,
            phone = this.user.phone,
            role = this.user.role,
            shopName = this.user.shopName,
            subscriptionPlan = this.user.subscriptionPlan,
            maxDevices = this.user.maxDevices,
            currentDeviceUuid = this.user.currentDeviceUuid,
            token = this.token,
            refreshToken = this.refreshToken,
            expiresIn = this.expiresIn
        )
    }

    private fun parseErrorMessage(errorBody: ResponseBody?): String {
        return runCatching {
            val errorJson = errorBody?.charStream()
            val errorResponse = gson.fromJson(errorJson, LoginErrorResponse::class.java)
            errorResponse?.error?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง"
    }
}

private data class LoginErrorResponse(
    val error: String?
)

