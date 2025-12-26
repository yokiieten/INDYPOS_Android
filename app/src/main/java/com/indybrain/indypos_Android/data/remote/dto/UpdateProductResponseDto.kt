package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for update product endpoint
 * API returns: data { product: {...}, addon_group_ids: [...] }
 */
data class UpdateProductResponseDto(
    @SerializedName("addon_group_ids")
    val addonGroupIds: List<String>?,
    val product: ProductDto
)




