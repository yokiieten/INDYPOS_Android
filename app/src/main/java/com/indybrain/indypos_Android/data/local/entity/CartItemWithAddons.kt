package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Relation class for Room to get CartItem with its Addons
 */
data class CartItemWithAddons(
    @Embedded val cartItem: CartItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "cartItemId"
    )
    val cartAddons: List<CartAddonEntity>
)


