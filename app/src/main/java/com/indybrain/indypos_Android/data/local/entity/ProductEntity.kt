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
    val categoryId: String?, // Manual FK (not using @Relation)
    val price: Double,
    val costPrice: Double?,
    val imageUrl: String?,
    val unit: String?,
    val selectedUnit: String?,
    val selectedColorHex: String?,
    val productCode: String?,
    val skuCode: String?,
    val stockQuantity: Int?,
    val isActive: Boolean,
    val isSkuEnabled: Boolean?,
    val isStockEnabled: Boolean?,
    val hasAdditionalOptions: Boolean?,
    val popularityRank: Int?,
    val isDeletedLocally: Boolean = false,
    val isFromServer: Boolean = true,
    val isSynced: Boolean = true,
    val createdAt: Date,
    val updatedAt: Date,
    // Extra fields (not in Core Data but kept for compatibility)
    val description: String? = null,
    val userId: Int? = null,
    val minStockQuantity: Int? = null
)

