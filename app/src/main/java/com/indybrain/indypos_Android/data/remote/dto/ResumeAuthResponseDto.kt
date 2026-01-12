package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for resume authentication API response
 */
data class ResumeAuthResponseDto(
    @SerializedName("user")
    val user: LoginUserDto?,
    @SerializedName("token")
    val token: String?,
    @SerializedName("refresh_token")
    val refreshToken: String?,
    @SerializedName("expires_in")
    val expiresIn: Long?,
    @SerializedName("is_new_session")
    val isNewSession: Boolean?,
    @SerializedName("error")
    val error: String?
) {
    /**
     * Check if resume auth was successful
     */
    val isSuccess: Boolean
        get() = user != null && token != null && error == null
}
