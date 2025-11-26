package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "cart_addons",
    foreignKeys = [
        ForeignKey(
            entity = CartItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["cartItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CartAddonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cartItemId: Long,
    val addonId: String,
    val addonName: String,
    val addonPrice: Double,
    val addonGroupId: String,
    val addonGroupName: String
)

