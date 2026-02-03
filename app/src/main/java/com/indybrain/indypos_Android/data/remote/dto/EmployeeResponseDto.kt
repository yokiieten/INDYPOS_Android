package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.indybrain.indypos_Android.domain.model.User

/**
 * Response DTO for employee list endpoint
 */
data class EmployeeListResponseDto(
    val status: Int?,
    val message: String?,
    val data: List<EmployeeDto>?,
    val count: Int?,
    val timestamp: String?,
    val error: String?
)

/**
 * Employee DTO
 */
data class EmployeeDto(
    val id: Int?,
    val username: String?,
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?,
    val email: String?,
    val phone: String?,
    val role: String?,
    @SerializedName("role_id")
    val roleId: Int?,
    @SerializedName("role_name")
    val roleName: String?,
    val permissions: List<String>?,
    @SerializedName("role_permissions")
    val rolePermissions: List<String>?,
    @SerializedName("owner_id")
    val ownerId: Int?,
    @SerializedName("owner_name")
    val ownerName: String?,
    @SerializedName("owner_shop_name")
    val ownerShopName: String?,
    @SerializedName("shop_name")
    val shopName: String?,
    @SerializedName("shop_description")
    val shopDescription: String?,
    @SerializedName("shop_image_url")
    val shopImageUrl: String?,
    @SerializedName("is_activated")
    val isActivated: Boolean?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)

/**
 * Extension function to map EmployeeDto to User domain model
 */
fun EmployeeDto.toDomainModel(): User {
    return User(
        id = this.id ?: 0,
        username = this.username,
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email ?: "",
        phone = this.phone,
        role = this.role,
        roleId = this.roleId,
        roleName = this.roleName,
        ownerId = this.ownerId,
        ownerName = this.ownerName,
        ownerShopName = this.ownerShopName,
        shopName = this.shopName,
        shopDescription = this.shopDescription,
        shopImageUrl = this.shopImageUrl,
        isActivated = this.isActivated,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        permissions = this.permissions,
        rolePermissions = this.rolePermissions
    )
}
