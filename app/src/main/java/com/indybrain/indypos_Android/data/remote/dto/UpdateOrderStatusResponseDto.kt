package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UpdateOrderStatusResponseDto(
    val status: Int,
    val message: String,
    val data: OrderDto?,
    val timestamp: String?
)


