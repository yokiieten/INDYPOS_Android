package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table for Many-to-Many relationship between ProductEntity and AddonGroupEntity
 */
@Entity(
    tableName = "product_addon_group_junction",
    primaryKeys = ["productId", "addonGroupId"],
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AddonGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["addonGroupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["addonGroupId"])
    ]
)
data class ProductAddonGroupJunctionEntity(
    val productId: String,
    val addonGroupId: String
)

