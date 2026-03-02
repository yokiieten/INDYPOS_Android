package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for delete multiple addon groups endpoint
 * Supports both full success and partial success (some deleted, some failed)
 */
data class DeleteAddonGroupsResponseDto(
    val status: Int,
    val message: String,
    @SerializedName("deleted_ids")
    val deletedIds: List<String>? = null,
    val count: Int = 0,
    val data: DeleteAddonGroupsDataDto? = null,
    @SerializedName("failed_deletions")
    val failedDeletions: List<String>? = null,
    val errors: List<String>? = null,
    val timestamp: String? = null
)

/**
 * Data payload for batch delete addon groups response
 */
data class DeleteAddonGroupsDataDto(
    @SerializedName("deleted_addon_groups")
    val deletedAddonGroups: List<DeletedAddonGroupSummaryDto> = emptyList(),
    @SerializedName("total_requested")
    val totalRequested: Int = 0,
    @SerializedName("total_deleted")
    val totalDeleted: Int = 0,
    @SerializedName("total_failed")
    val totalFailed: Int = 0
)

/**
 * Minimal addon group info in batch delete response
 */
data class DeletedAddonGroupSummaryDto(
    val id: String,
    val name: String? = null,
    @SerializedName("is_required")
    val isRequired: Boolean? = null,
    @SerializedName("is_single_selection")
    val isSingleSelection: Boolean? = null,
    @SerializedName("sort_order")
    val sortOrder: Int? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null
)
