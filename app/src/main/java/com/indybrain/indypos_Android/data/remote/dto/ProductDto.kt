package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

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

