package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response data for addons paginated API
 * GET /api/v1/protected/indypos/addons/paginated
 */
data class AddonsPaginatedDataDto(
    val addons: List<AddonDto>?,
    val pagination: AddonsPaginationDto?
)

/**
 * Pagination info for addons
 */
data class AddonsPaginationDto(
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

data class AddonDto(
    val id: String,
    val name: String,
    val price: Double,
    @SerializedName("sort_order")
    val sortOrder: Int,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

