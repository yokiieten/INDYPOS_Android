package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs for Today Sales API response
 * GET /api/v1/protected/indypos/home/today-sales
 */

data class TodaySalesResponseDto(
    @SerializedName("todays_sales")
    val todaysSales: Double = 0.0,
    @SerializedName("orders_today")
    val ordersToday: Int = 0,
    @SerializedName("top_product")
    val topProduct: TopProductDto? = null
)

data class TopProductDto(
    val name: String = "",
    val quantity: Int = 0,
    val amount: Double = 0.0
)
