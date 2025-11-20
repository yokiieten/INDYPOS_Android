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
    val shopDescription: String? = null,
    val shopImageUrl: String? = null,
    val subscriptionPlan: String? = null,
    val subscriptionExpiresAt: String? = null,
    val maxDevices: Int? = null,
    val currentDeviceUuid: String? = null,
    val isActivated: Boolean? = null,
    val termOfUse: Boolean? = null,
    val privacyPolicy: Boolean? = null,
    val marketingConsent: Boolean? = null,
    val birthDate: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val orderCount: Int? = null,
    val token: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Long? = null
)

