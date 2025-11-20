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

enum class OrderStatus {
    COMPLETED,  // เสร็จสิ้น
    CANCELLED,  // ยกเลิก
    FAILED      // ล้มเหลว
}

