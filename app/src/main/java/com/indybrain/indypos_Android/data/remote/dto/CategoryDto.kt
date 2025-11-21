package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

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

