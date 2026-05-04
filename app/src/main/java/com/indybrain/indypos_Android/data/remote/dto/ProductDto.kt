package com.indybrain.indypos_Android.data.remote.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Response data for products paginated API
 * GET /api/v1/protected/indypos/products/paginated
 */
data class ProductsPaginatedDataDto(
    val products: List<ProductDto>?,
    val pagination: ProductsPaginationDto?
)

/**
 * Pagination info for products
 */
data class ProductsPaginationDto(
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

@Keep
data class ProductDto(
    val id: String,
    val name: String,
    val description: String?,
    val price: Double,
    @SerializedName("cost_price")
    val costPrice: Double?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("category_id")
    val categoryId: String?,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("popularity_rank")
    val popularityRank: Int?,
    @SerializedName("product_code")
    val productCode: String?,
    val unit: String?,
    @SerializedName("sku_code")
    val skuCode: String?,
    @SerializedName("stock_quantity")
    val stockQuantity: Int?,
    @SerializedName("min_stock_quantity")
    val minStockQuantity: Int?,
    @SerializedName("selected_unit")
    val selectedUnit: String?,
    @SerializedName("selected_color_hex")
    val selectedColorHex: String?,
    @SerializedName("is_sku_enabled")
    val isSkuEnabled: Boolean?,
    @SerializedName("is_stock_enabled")
    val isStockEnabled: Boolean?,
    @SerializedName("has_additional_options")
    val hasAdditionalOptions: Boolean?,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    val category: CategoryDto?,
    @SerializedName("addon_groups")
    val addonGroups: List<AddonGroupDto>?
)

