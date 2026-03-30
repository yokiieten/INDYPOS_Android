package com.indybrain.indypos_Android.data.local.entity

import java.util.Date

/**
 * Line item snapshot for an order (not persisted in Room; addons JSON lives in [addons]).
 */
data class OrderItemEntity(
    val id: String,
    val orderId: String,
    val productName: String,
    val productPrice: Double,
    val productUnitPrice: Double,
    val quantity: Int,
    val totalPrice: Double,
    val addons: String?,
    val specialRequest: String?,
    val productId: String? = null,
    val productCode: String? = null,
    val unitCost: Double? = null,
    val notes: String? = null,
    val createdAt: Date? = null
)
