package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for delete multiple addons endpoint
 * Supports both full success and partial success (some deleted, some failed)
 */
data class DeleteAddonsResponseDto(
    val status: Int,
    val message: String,
    @SerializedName("deleted_ids")
    val deletedIds: List<String>? = null,
    val count: Int = 0,
    val data: DeleteAddonsDataDto? = null,
    @SerializedName("failed_deletions")
    val failedDeletions: List<String>? = null,
    val errors: List<String>? = null,
    val timestamp: String? = null
)

/**
 * Data payload for batch delete addons response
 */
data class DeleteAddonsDataDto(
    @SerializedName("deleted_addons")
    val deletedAddons: List<DeletedAddonSummaryDto> = emptyList(),
    @SerializedName("total_requested")
    val totalRequested: Int = 0,
    @SerializedName("total_deleted")
    val totalDeleted: Int = 0,
    @SerializedName("total_failed")
    val totalFailed: Int = 0
)

/**
 * Minimal addon info in batch delete response
 */
data class DeletedAddonSummaryDto(
    val id: String,
    val name: String? = null,
    val price: Double? = null
)
