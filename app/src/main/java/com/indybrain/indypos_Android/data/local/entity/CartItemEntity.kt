package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.indybrain.indypos_Android.data.local.converter.DateConverter
import java.util.Date

@Entity(
    tableName = "cart_items",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL // Use SET_NULL instead of CASCADE to preserve cart items when product is deleted
            // This ensures cart items are NOT deleted when products are updated or removed from API
        )
    ]
)
@TypeConverters(DateConverter::class)
data class CartItemEntity(
    @PrimaryKey
    val id: String, // Changed from Long to String to match Core Data
    val productId: String?, // Many-to-One with ProductEntity
    val quantity: Int,
    val specialRequest: String?,
    val createdAt: Date,
    // Extra fields (snapshot data for display, not in Core Data relationships)
    val productName: String? = null,
    val productImageUrl: String? = null,
    val productColorHex: String? = null,
    val unitPrice: Double? = null
)

