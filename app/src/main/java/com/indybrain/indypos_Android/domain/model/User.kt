package com.indybrain.indypos_Android.domain.model

/**
 * Domain model representing a User
 */
data class User(
    val id: Int,
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String,
    val phone: String? = null,
    val role: String? = null,
    val shopName: String? = null,
    val subscriptionPlan: String? = null,
    val maxDevices: Int? = null,
    val currentDeviceUuid: String? = null,
    val token: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Long? = null
)

