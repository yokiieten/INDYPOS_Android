package com.indybrain.indypos_Android.domain.model

/**
 * Domain model for register request
 */
data class RegisterRequest(
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val password: String,
    val shopName: String,
    val shopDescription: String? = null,
    val shopImageUrl: String? = null,
    val birthDate: String? = null,
    val termOfUse: Boolean,
    val privacyPolicy: Boolean
)

