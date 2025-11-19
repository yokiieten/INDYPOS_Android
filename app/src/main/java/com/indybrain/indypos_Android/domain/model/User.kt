package com.indybrain.indypos_Android.domain.model

/**
 * Domain model representing a User
 */
data class User(
    val id: String,
    val email: String,
    val name: String? = null,
    val token: String? = null
)

