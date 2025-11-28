package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for logout API response
 */
data class LogoutResponseDto(
    @SerializedName("status")
    val status: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: LogoutDataDto?,
    @SerializedName("timestamp")
    val timestamp: String?
)

data class LogoutDataDto(
    @SerializedName("device_uuid")
    val deviceUuid: String
)

