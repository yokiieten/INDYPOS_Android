package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * SelectedAddonEntity represents selected addons for a cart item
 * Relationships:
 * - cartItem: Many-to-One with CartItemEntity
 * - addons: Many-to-Many with AddonEntity (via SelectedAddonJunctionEntity)
 */
@Entity(
    tableName = "selected_addons",
    foreignKeys = [
        ForeignKey(
            entity = CartItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["cartItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SelectedAddonEntity(
    @PrimaryKey
    val id: String,
    val cartItemId: String, // Many-to-One with CartItemEntity
    val groupId: String // Reference to AddonGroupEntity
)

