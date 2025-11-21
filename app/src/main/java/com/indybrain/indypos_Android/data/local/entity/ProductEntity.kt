package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.indybrain.indypos_Android.data.local.converter.DateConverter
import java.util.Date

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
@TypeConverters(DateConverter::class)
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val price: Double,
    val costPrice: Double?,
    val imageUrl: String?,
    val categoryId: String?,
    val userId: Int,
    val popularityRank: Int?,
    val productCode: String?,
    val unit: String?,
    val skuCode: String?,
    val stockQuantity: Int?,
    val minStockQuantity: Int?,
    val selectedUnit: String?,
    val selectedColorHex: String?,
    val isSkuEnabled: Boolean?,
    val isStockEnabled: Boolean?,
    val hasAdditionalOptions: Boolean?,
    val isActive: Boolean,
    val createdAt: Date,
    val updatedAt: Date
)

