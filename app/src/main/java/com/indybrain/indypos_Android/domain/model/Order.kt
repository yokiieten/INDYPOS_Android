package com.indybrain.indypos_Android.domain.model

import java.util.Date

/**
 * Order model representing an order in the system
 */
data class Order(
    val id: String,
    val orderId: String, // e.g., "ORD20251120001386"
    val createdAt: Date,
    val cancelledAt: Date? = null,
    val status: OrderStatus,
    val totalAmount: Double
)

enum class OrderStatus(val code: Int) {
    DRAFT(0),       // draft
    CONFIRMED(1),   // confirmed
    PREPARING(2),   // preparing
    READY(3),       // ready
    DELIVERED(4),   // delivered
    CANCELLED(5);   // cancelled
    
    companion object {
        fun fromCode(code: Int): OrderStatus {
            return entries.firstOrNull { it.code == code } ?: DRAFT
        }
    }
}

