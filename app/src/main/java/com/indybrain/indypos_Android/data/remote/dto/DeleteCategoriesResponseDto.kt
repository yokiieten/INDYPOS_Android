package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for delete multiple categories endpoint
 * Supports both full success and partial success (some deleted, some failed)
 */
data class DeleteCategoriesResponseDto(
    val status: Int,
    val message: String,
    @SerializedName("deleted_ids")
    val deletedIds: List<String>? = null,
    val count: Int = 0,
    val data: DeleteCategoriesDataDto? = null,
    @SerializedName("failed_deletions")
    val failedDeletions: List<String>? = null,
    val errors: List<String>? = null,
    val timestamp: String? = null
)

/**
 * Data payload for batch delete categories response
 */
data class DeleteCategoriesDataDto(
    @SerializedName("deleted_categories")
    val deletedCategories: List<DeletedCategorySummaryDto> = emptyList(),
    @SerializedName("total_requested")
    val totalRequested: Int = 0,
    @SerializedName("total_deleted")
    val totalDeleted: Int = 0,
    @SerializedName("total_failed")
    val totalFailed: Int = 0
)

/**
 * Minimal category info in batch delete response
 */
data class DeletedCategorySummaryDto(
    val id: String,
    val name: String? = null,
    @SerializedName("sort_order")
    val sortOrder: Int? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null,
    @SerializedName("product_count")
    val productCount: Int? = null
)
