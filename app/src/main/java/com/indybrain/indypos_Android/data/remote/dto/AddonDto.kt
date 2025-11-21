package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

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

