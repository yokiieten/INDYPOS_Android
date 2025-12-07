package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.indybrain.indypos_Android.data.local.converter.DateConverter
import java.util.Date

@Entity(tableName = "orders")
@TypeConverters(DateConverter::class)
data class OrderEntity(
    @PrimaryKey
    val id: String,
    val orderNumber: String,
    val orderDate: Date,
    val subtotal: Double,
    val discount: Double, // Core Data uses "discount" not "discountAmount"
    val total: Double,
    val paymentTypeRaw: Int, // Enum: 0=cash, 1=transfer, 2=card, 3=qrCode
    val statusRaw: Int, // Enum: 0=draft, 1=confirmed, 2=preparing, 3=ready, 4=delivered, 5=cancelled
    val isDeletedLocally: Boolean = false,
    val isFromServer: Boolean = true,
    val isSynced: Boolean = true,
    val updatedAt: Date,
    // Extra fields (not in Core Data but kept for compatibility)
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

