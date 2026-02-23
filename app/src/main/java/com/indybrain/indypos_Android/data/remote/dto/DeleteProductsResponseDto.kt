package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for delete multiple products endpoint
 * Supports both full success and partial success (some deleted, some failed)
 */
data class DeleteProductsResponseDto(
    val status: Int,
    val message: String,
    @SerializedName("deleted_ids")
    val deletedIds: List<String>? = null,
    val count: Int = 0,
    val data: DeleteProductsDataDto? = null,
    @SerializedName("failed_deletions")
    val failedDeletions: List<String>? = null,
    val errors: List<String>? = null,
    val timestamp: String? = null
)

/**
 * Data payload for batch delete response
 */
data class DeleteProductsDataDto(
    @SerializedName("deleted_products")
    val deletedProducts: List<DeletedProductSummaryDto> = emptyList(),
    @SerializedName("total_requested")
    val totalRequested: Int = 0,
    @SerializedName("total_deleted")
    val totalDeleted: Int = 0,
    @SerializedName("total_failed")
    val totalFailed: Int = 0
)

/**
 * Minimal product info in batch delete response
 */
data class DeletedProductSummaryDto(
    val id: String,
    val name: String? = null,
    val price: Double? = null
)


