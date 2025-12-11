package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for delete multiple products endpoint
 */
data class DeleteProductsResponseDto(
    val status: Int,
    val message: String,
    @SerializedName("deleted_ids")
    val deletedIds: List<String>,
    val count: Int,
    val timestamp: String?
)


