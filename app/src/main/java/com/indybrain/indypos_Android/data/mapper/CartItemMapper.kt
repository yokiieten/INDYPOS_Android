package com.indybrain.indypos_Android.data.mapper

import com.indybrain.indypos_Android.data.local.entity.AddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemWithAddons
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.domain.model.Addon
import com.indybrain.indypos_Android.domain.model.CartItem
import com.indybrain.indypos_Android.domain.model.Product
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartItemMapper @Inject constructor() {
    
    fun toDomain(
        entity: CartItemWithAddons,
        product: ProductEntity,
        addons: Map<String, List<AddonEntity>> // groupId -> List<AddonEntity>
    ): CartItem {
        // Group selected addons by groupId
        val selectedAddonsMap = entity.cartAddons
            .groupBy { it.addonGroupId }
            .mapValues { (groupId, cartAddons) ->
                cartAddons.mapNotNull { cartAddon ->
                    addons[groupId]?.find { it.id == cartAddon.addonId }
                        ?.let { addonEntity ->
                            Addon(
                                id = addonEntity.id,
                                name = addonEntity.name,
                                price = addonEntity.price,
                                groupId = addonEntity.addonGroupId
                            )
                        }
                }
            }
            .filterValues { it.isNotEmpty() }
        
        return CartItem(
            id = entity.cartItem.id,
            product = Product(
                id = product.id,
                name = product.name,
                price = product.price,
                imageUrl = entity.cartItem.productImageUrl ?: product.imageUrl, // Use snapshot if available
                stockQuantity = product.stockQuantity,
                isStockEnabled = product.isStockEnabled,
                selectedColorHex = entity.cartItem.productColorHex ?: product.selectedColorHex // Use snapshot if available
            ),
            quantity = entity.cartItem.quantity,
            selectedAddons = selectedAddonsMap,
            specialRequest = entity.cartItem.specialRequest,
            createdAt = entity.cartItem.createdAt
        )
    }
}

