package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName
import java.util.Date

data class OrdersResponseDto(
    val status: Int,
    val message: String,
    val data: List<OrderDto>?,
    val timestamp: String?
)

data class OrderDto(
    val id: String,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("order_number")
    val orderNumber: String,
    @SerializedName("order_date")
    val orderDate: String,
    @SerializedName("customer_name")
    val customerName: String?,
    @SerializedName("customer_phone")
    val customerPhone: String?,
    @SerializedName("customer_email")
    val customerEmail: String?,
    val subtotal: Double,
    @SerializedName("discount_amount")
    val discountAmount: Double,
    @SerializedName("discount_percentage")
    val discountPercentage: Double,
    @SerializedName("tax_amount")
    val taxAmount: Double,
    @SerializedName("tax_percentage")
    val taxPercentage: Double,
    val total: Double,
    @SerializedName("payment_type")
    val paymentType: Int,
    @SerializedName("payment_status")
    val paymentStatus: Int,
    @SerializedName("order_status")
    val orderStatus: Int,
    val notes: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    val items: List<OrderItemDto>?
)

data class OrderItemDto(
    val id: String,
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("product_id")
    val productId: String?,
    @SerializedName("product_name")
    val productName: String,
    @SerializedName("product_code")
    val productCode: String?,
    @SerializedName("unit_price")
    val unitPrice: Double,
    @SerializedName("unit_cost")
    val unitCost: Double,
    val quantity: Int,
    @SerializedName("total_price")
    val totalPrice: Double,
    @SerializedName("special_request")
    val specialRequest: String?,
    val notes: String?,
    @SerializedName("created_at")
    val createdAt: String,
    val addons: List<OrderAddonDto>?
)

data class OrderAddonDto(
    @SerializedName("order_item_id")
    val orderItemId: String,
    @SerializedName("addon_id")
    val addonId: String,
    @SerializedName("addon_name")
    val addonName: String,
    @SerializedName("addon_price")
    val addonPrice: Double,
    val quantity: Int
)

