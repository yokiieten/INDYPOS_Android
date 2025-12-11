package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Statistics Data DTO
 */
data class StatisticsDataDto(
    @SerializedName("category_count")
    val categoryCount: Int?,
    @SerializedName("product_count")
    val productCount: Int?,
    @SerializedName("addon_group_count")
    val addonGroupCount: Int?,
    @SerializedName("addon_count")
    val addonCount: Int?,
    @SerializedName("order_count")
    val orderCount: Int?
)

