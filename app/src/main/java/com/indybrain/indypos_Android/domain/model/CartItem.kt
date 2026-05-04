package com.indybrain.indypos_Android.domain.model

import java.util.Date

/**
 * Domain model representing a Cart Item
 */
data class CartItem(
    val id: String,
    val product: Product,
    val quantity: Int,
    val selectedAddons: Map<String, List<Addon>>, // groupId -> List<Addon>
    val specialRequest: String?,
    val createdAt: Date?
)

/**
 * Domain model representing a Product
 */
data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String?,
    val stockQuantity: Int?,
    val isStockEnabled: Boolean?,
    val selectedColorHex: String? = null
)

/**
 * Domain model representing an Addon
 */
data class Addon(
    val id: String,
    val name: String,
    val price: Double,
    val groupId: String?
)

/**
 * Domain model representing a grouped cart item
 */
data class GroupedCartItem(
    val key: String, // same format as [CartItem.configurationKey]: "productId|specialRequest|groupId:addonIds|..."
    val items: List<CartItem>,
    val originalIndices: List<Int>, // indices ใน cart items list (for deletion)
    val totalQuantity: Int // sum ของ quantity ทั้งหมดใน group
)

