package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response data for addon groups paginated API
 * GET /api/v1/protected/indypos/addon-groups/paginated
 */
data class AddonGroupsPaginatedDataDto(
    @SerializedName("addon_groups")
    val addonGroups: List<AddonGroupDto>?,
    val pagination: AddonGroupsPaginationDto?
)

/**
 * Pagination info for addon groups
 */
data class AddonGroupsPaginationDto(
    @SerializedName("current_page")
    val currentPage: Int,
    val limit: Int,
    @SerializedName("total_count")
    val totalCount: Int,
    @SerializedName("total_pages")
    val totalPages: Int,
    @SerializedName("has_next")
    val hasNext: Boolean,
    @SerializedName("has_previous")
    val hasPrevious: Boolean
)

data class AddonGroupDto(
    val id: String,
    val name: String,
    @SerializedName("is_required")
    val isRequired: Boolean,
    @SerializedName("is_single_selection")
    val isSingleSelection: Boolean,
    @SerializedName("max_selection")
    val maxSelection: Int?,
    @SerializedName("min_selection")
    val minSelection: Int?,
    @SerializedName("sort_order")
    val sortOrder: Int,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    val addons: List<AddonDto>?
)

