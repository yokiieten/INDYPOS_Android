package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_addons",
    foreignKeys = [
        ForeignKey(
            entity = OrderItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OrderAddonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderItemId: String,
    val addonId: String,
    val addonName: String,
    val addonPrice: Double,
    val quantity: Int
)

