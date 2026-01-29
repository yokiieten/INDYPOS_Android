package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateEmployeeRequestDto(
    val username: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    val email: String,
    val phone: String,
    val password: String,
    @SerializedName("role_id")
    val roleId: Int
)

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
    @SerializedName("subscription_plan")
    val subscriptionPlan: String?,
    @SerializedName("max_devices")
    val maxDevices: Int?,
    @SerializedName("subscription_expires_at")
    val subscriptionExpiresAt: String?,
    @SerializedName("current_device_uuid")
    val currentDeviceUuid: String?,
    @SerializedName("term_of_use")
    val termOfUse: Boolean?,
    @SerializedName("privacy_policy")
    val privacyPolicy: Boolean?,
    @SerializedName("marketing_consent")
    val marketingConsent: Boolean?,
    @SerializedName("birth_date")
    val birthDate: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)
