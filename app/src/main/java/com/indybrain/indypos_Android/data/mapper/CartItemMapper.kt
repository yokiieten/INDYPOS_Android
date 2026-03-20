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
        fun resolveAddonEntity(groupId: String, addonId: String): AddonEntity? {
            addons[groupId]?.find { it.id == addonId }?.let { return it }
            // Cart เก็บ addonGroupId จากตอนเลือก — ใน Room บางที addon ยังไม่ sync หรือ addonGroupId บน entity ไม่ตรงกลุ่ม
            return addons.values.flatten().find { it.id == addonId }
        }

        // Group selected addons by groupId (ใช้ id จากแถว cart เพื่อให้ตรงกับ UI ของ product detail)
        val selectedAddonsMap = entity.cartAddons
            .groupBy { it.addonGroupId }
            .mapValues { (groupId, cartAddons) ->
                cartAddons.map { cartAddon ->
                    val addonEntity = resolveAddonEntity(groupId, cartAddon.addonId)
                    if (addonEntity != null) {
                        Addon(
                            id = addonEntity.id,
                            name = addonEntity.name,
                            price = addonEntity.price,
                            groupId = addonEntity.addonGroupId ?: groupId
                        )
                    } else {
                        // ไม่มีแถวใน Room (เช่น โหลดสินค้าจาก API แต่ยังไม่ได้บันทึก addons) — ใช้ snapshot จากตะกร้า
                        Addon(
                            id = cartAddon.addonId,
                            name = cartAddon.addonName,
                            price = cartAddon.addonPrice,
                            groupId = groupId
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

