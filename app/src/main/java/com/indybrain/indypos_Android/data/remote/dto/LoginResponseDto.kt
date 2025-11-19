package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for login API response
 */
data class LoginResponseDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("name")
    val name: String? = null,
    
    @SerializedName("token")
    val token: String? = null
)

