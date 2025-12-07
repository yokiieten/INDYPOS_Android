package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.indybrain.indypos_Android.data.local.converter.DateConverter
import java.util.Date

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@TypeConverters(DateConverter::class)
data class OrderItemEntity(
    @PrimaryKey
    val id: String,
    val orderId: String, // Many-to-One with OrderEntity
    val productName: String, // Snapshot (not relationship)
    val productPrice: Double, // Snapshot (productPrice)
    val productUnitPrice: Double, // Snapshot (productUnitPrice)
    val quantity: Int,
    val totalPrice: Double,
    val addons: String?, // JSON string (snapshot of addons)
    val specialRequest: String?,
    // Extra fields (not in Core Data but kept for compatibility)
    val productId: String? = null,
    val productCode: String? = null,
    val unitCost: Double? = null,
    val notes: String? = null,
    val createdAt: Date? = null
)

