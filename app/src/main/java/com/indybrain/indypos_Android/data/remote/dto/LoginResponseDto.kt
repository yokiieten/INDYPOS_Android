package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for login API response
 */
data class LoginResponseDto(
    @SerializedName("user")
    val user: LoginUserDto,
    @SerializedName("token")
    val token: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_in")
    val expiresIn: Long
)

data class LoginUserDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("username")
    val username: String? = null,
    @SerializedName("first_name")
    val firstName: String? = null,
    @SerializedName("last_name")
    val lastName: String? = null,
    @SerializedName("email")
    val email: String,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("role")
    val role: String? = null,
    @SerializedName("shop_name")
    val shopName: String? = null,
    @SerializedName("subscription_plan")
    val subscriptionPlan: String? = null,
    @SerializedName("max_devices")
    val maxDevices: Int? = null,
    @SerializedName("current_device_uuid")
    val currentDeviceUuid: String? = null
)

