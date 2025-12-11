package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for create product endpoint
 * The API returns data with nested product structure
 */
data class CreateProductResponseDto(
    @SerializedName("addon_group_ids")
    val addonGroupIds: List<String>?,
    val product: ProductDto
)


