package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

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

