package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateOrderRequestDto(
    @SerializedName("customer_name")
    val customerName: String,
    @SerializedName("customer_phone")
    val customerPhone: String,
    @SerializedName("customer_email")
    val customerEmail: String,
    @SerializedName("payment_type")
    val paymentType: Int,
    @SerializedName("discount_amount")
    val discountAmount: Double,
    @SerializedName("discount_percentage")
    val discountPercentage: Double,
    @SerializedName("tax_amount")
    val taxAmount: Double,
    @SerializedName("tax_percentage")
    val taxPercentage: Double,
    val notes: String,
    val items: List<OrderItemDto>
) {
    data class OrderItemDto(
        @SerializedName("product_id")
        val productId: String,
        val quantity: Int,
        @SerializedName("unit_cost")
        val unitCost: Double,
        @SerializedName("special_request")
        val specialRequest: String,
        val notes: String,
        @SerializedName("addon_groups")
        val addonGroups: List<AddonGroupDto>,
        val addons: List<Any>
    )
    
    data class AddonGroupDto(
        @SerializedName("addon_group_id")
        val addonGroupId: String,
        @SerializedName("selected_addons")
        val selectedAddons: List<SelectedAddonDto>
    )
    
    data class SelectedAddonDto(
        @SerializedName("addon_id")
        val addonId: String,
        val quantity: Int
    )
}

