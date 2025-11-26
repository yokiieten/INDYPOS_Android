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
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
@TypeConverters(DateConverter::class)
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: String?,
    val productName: String,
    val productImageUrl: String?,
    val productColorHex: String?,
    val unitPrice: Double,
    val quantity: Int,
    val specialRequest: String?,
    val createdAt: Date = Date()
)

