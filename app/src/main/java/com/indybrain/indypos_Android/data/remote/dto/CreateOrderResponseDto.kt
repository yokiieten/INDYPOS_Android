package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateOrderResponseDto(
    val status: Int,
    val message: String?,
    val error: String?,
    val data: OrderDataDto?
)

data class OrderDataDto(
    val id: String,
    @SerializedName("order_number")
    val orderNumber: String?,
    @SerializedName("order_date")
    val orderDate: String,
    val total: Double
)


