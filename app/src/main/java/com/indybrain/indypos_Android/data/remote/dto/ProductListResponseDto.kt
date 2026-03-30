package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs for Main Product List API response
 * GET /api/v1/protected/indypos/products/list
 */

data class ProductListResponseDto(
    val categories: List<CategoryListItemDto> = emptyList(),
    val products: List<ProductListItemDto> = emptyList()
)

/**
 * Response data for product search API
 * GET /api/v1/protected/indypos/products/search
 */
data class ProductsSearchPaginatedDataDto(
    val products: List<ProductListItemDto>? = null,
    val pagination: ProductsPaginationDto? = null
)

data class CategoryListItemDto(
    val id: String,
    val name: String,
    @SerializedName("sort_order")
    val sortOrder: Int = 0,
    @SerializedName("is_active")
    val isActive: Boolean = true
)

data class ProductListItemDto(
    val id: String,
    val name: String,
    val price: Double,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("selected_color_hex")
    val selectedColorHex: String? = null,
    @SerializedName("category_id")
    val categoryId: String? = null,
    @SerializedName("has_additional_options")
    val hasAdditionalOptions: Boolean? = null,
    @SerializedName("product_code")
    val productCode: String? = null,
    @SerializedName("sku_code")
    val skuCode: String? = null,
    @SerializedName("stock_quantity")
    val stockQuantity: Int? = null,
    @SerializedName("is_stock_enabled")
    val isStockEnabled: Boolean? = null,
    @SerializedName("popularity_rank")
    val popularityRank: Int? = null
)
