package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.domain.model.LoginRequest
import com.indybrain.indypos_Android.domain.model.RegisterRequest
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
     * Register new user
     * @return Result containing User on success or error message
     */
    suspend fun register(request: RegisterRequest): Result<User>
    
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
    
    /**
     * Resume authentication with refresh token
     * @return Result containing User on success or error message
     */
    suspend fun resumeAuth(): Result<User>
    
    /**
     * Change password
     * @param oldPassword Current password
     * @param newPassword New password
     * @return Result indicating success or failure
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit>
    
    /**
     * Update shop description
     * @param description New shop description
     * @return Result containing updated User on success or error message
     */
    suspend fun updateShopDescription(description: String): Result<User>
    
    /**
     * Update shop name
     * @param shopName New shop name
     * @return Result containing updated User on success or error message
     */
    suspend fun updateShopName(shopName: String): Result<User>

    /**
     * Update shop image
     * @param imageUri URI of the image file to upload
     * @return Result containing updated User with new shopImageUrl
     */
    suspend fun updateShopImage(imageUri: android.net.Uri): Result<User>
    
    /**
     * Request password reset
     * @param email User email address
     * @return Result indicating success or failure
     */
    suspend fun forgotPassword(email: String): Result<Unit>
    
    /**
     * Verify reset password token
     * @param token Reset token from email link
     * @return Result containing userId if token is valid
     */
    suspend fun verifyResetPasswordToken(token: String): Result<Int?>
    
    /**
     * Reset password with token
     * @param token Reset token from email link
     * @param newPassword New password
     * @return Result indicating success or failure
     */
    suspend fun resetPassword(token: String, newPassword: String): Result<Unit>
    
    /**
     * Get employee list
     * Owner can see their own employees
     * Admin can see all employees or filter by ownerId
     * @param ownerId Optional owner ID (Admin only)
     * @return Result containing list of employees
     */
    suspend fun getEmployees(ownerId: Int? = null): Result<List<User>>

    /**
     * Create employee
     * @return Result containing created User on success
     */
    suspend fun createEmployee(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        password: String,
        roleId: Int? = null,
        permissions: List<String>? = null
    ): Result<User>

    /**
     * Update employee
     * @param employeeId ID of employee to update
     * @return Result containing updated User on success
     */
    suspend fun updateEmployee(
        employeeId: Int,
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        phone: String? = null,
        roleId: Int? = null,
        isActivated: Boolean? = null,
        permissions: List<String>? = null
    ): Result<User>
}

