package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response data for categories paginated API
 */
data class CategoriesPaginatedDataDto(
    val categories: List<CategoryDto>?,
    val pagination: CategoriesPaginationDto?
)

/**
 * Pagination info for categories
 */
data class CategoriesPaginationDto(
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

data class CategoryDto(
    val id: String,
    val name: String,
    @SerializedName("sort_order")
    val sortOrder: Int,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("product_count")
    val productCount: Int?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

