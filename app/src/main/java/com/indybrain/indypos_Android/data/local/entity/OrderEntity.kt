package com.indybrain.indypos_Android.data.local.entity

import java.util.Date

/**
 * In-memory / API snapshot model for an order (not persisted in Room).
 */
data class OrderEntity(
    val id: String,
    val orderNumber: String,
    val orderDate: Date,
    val subtotal: Double,
    val discount: Double,
    val total: Double,
    val paymentTypeRaw: Int,
    val statusRaw: Int,
    val isDeletedLocally: Boolean = false,
    val isFromServer: Boolean = true,
    val isSynced: Boolean = true,
    val updatedAt: Date,
    val userId: Int? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val customerEmail: String? = null,
    val discountAmount: Double? = null,
    val discountPercentage: Double? = null,
    val taxAmount: Double? = null,
    val taxPercentage: Double? = null,
    val paymentStatus: Int? = null,
    val notes: String? = null,
    val createdAt: Date? = null
)
