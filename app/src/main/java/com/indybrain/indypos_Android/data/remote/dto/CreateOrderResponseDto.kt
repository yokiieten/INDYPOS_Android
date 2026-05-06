package com.indybrain.indypos_Android.data.remote.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Wraps POST /orders JSON. Explicit [SerializedName] matches [UnauthorizedInterceptor.ErrorResponse] —
 * R8 release builds otherwise rename Kotlin fields and Gson leaves [error]/[message] null so errors
 * fall through as English server text instead of [com.indybrain.indypos_Android.core.order.CreateOrderErrorMapper].
 */
@Keep
data class CreateOrderResponseDto(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?,
    @SerializedName("data") val data: OrderDataDto?
)

@Keep
data class OrderDataDto(
    @SerializedName("id") val id: String,
    @SerializedName("order_number")
    val orderNumber: String?,
    @SerializedName("order_date")
    val orderDate: String,
    @SerializedName("total") val total: Double
)


